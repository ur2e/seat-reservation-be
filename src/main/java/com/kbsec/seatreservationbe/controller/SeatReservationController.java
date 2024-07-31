package com.kbsec.seatreservationbe.controller;

import com.kbsec.seatreservationbe.entity.Employee;
import com.kbsec.seatreservationbe.entity.Reservation;
import com.kbsec.seatreservationbe.service.*;
import com.kbsec.seatreservationbe.util.ResultMsg;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("")
@Slf4j
public class SeatReservationController {

    @Autowired
    private S3Service s3Service;

    @Autowired
    private RekognitionService rekognitionService;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private ApiGatewayService apiGatewayService;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(
            @RequestParam("file") MultipartFile file,
            @RequestParam("name") String fullName,
            @RequestParam("id") String employeeId,
            @RequestParam("email") String email) {
        try {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("EmployeeId", employeeId);

            // 1. 이미지를 S3에 저장한다 -> 트리거 : Rekognition / DynamoDB에 사용자 이미지 등록됨
            s3Service.uploadFile(file, metadata);

            log.info("[signup] s3 upload file {} {}", employeeId, fullName);

            // 2. 이름, 사번, 이메일주소, 휴대폰 번호는 RDS에 저장한다.
            employeeService.saveEmployee(employeeId, fullName, email);
            log.info("[signup] save data to RDS {} {}", employeeId, fullName);

            // 3. SNS 이메일 구독을 위한 리퀘스트 전송
            apiGatewayService.sendSNSSubscribeRequest(fullName, email);
            log.info("[signup] call SNS req {} {}", employeeId, fullName);

            log.info("[signup] employeeId [{}], name [{}] 등록 완료", employeeId, fullName);

            String msg = String.format(ResultMsg.S000.getMsg(),fullName);

            return ResponseEntity.ok(msg);
        } catch (IOException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/reserve")
    public ResponseEntity<String> reserveSeat(@RequestParam("file") MultipartFile file,
                                                   @RequestParam("seat") int seat) {
        try {
            byte[] imageBytes = file.getBytes();

            // 1. 사번을 찾는다.
            String employeeId = rekognitionService.findEmployeeId(imageBytes);
            // 1-1. 등록된 얼굴이 아니라면 에러 처리
            if (employeeId == null) {
                log.info("[reserve] 등록된 얼굴이 아님");
                return ResponseEntity.ok(ResultMsg.S301.getMsg());
            } else if (employeeId.equals("E400")) {
                log.info("[reserve] 카메라에 얼굴 사진이 없음");
                return ResponseEntity.ok(ResultMsg.S302.getMsg());
            }

            // 2. 예약 정보를 찾는다.
            if (reservationService.existReservation(employeeId)) {
                // 2-1. 이미 예약된 좌석이 있으면 에러 처리
                Reservation reservation = reservationService.findReservation(employeeId);
                String msg = String.format(ResultMsg.S101.getMsg(), reservation.getSeat());

                log.info("[reserve] employeeId [{}] 이미 예약한 좌석이 있음.", employeeId);
                return ResponseEntity.ok(msg);
            }

            // 3. 사원 정보를 조회한다.
            Employee employee = employeeService.getEmployee(employeeId);
            log.info("yji {}}", employeeId);

            // 4. 예약 정보를 추가한다.
            reservationService.saveReservation(employeeId, seat);
            String msg = String.format(ResultMsg.S100.getMsg(), employee.getName(), seat);

            log.info("[reserve] employeeId [{}] seat[{}] 예약 완료", employeeId, seat);

            // 5. SNS 이메일 전송 - 좌석 예약 완료
            apiGatewayService.sendRequest(employee.getName(), employee.getEmail(), seat,"reserve");

            return ResponseEntity.ok(msg);
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Error processing image");
        }
    }

    @PostMapping("/exit")
    public ResponseEntity<String> checkoutSeat(@RequestParam("file") MultipartFile file) {
        try {
            byte[] imageBytes = file.getBytes();
            // 1. 사번을 찾는다.
            String employeeId = rekognitionService.findEmployeeId(imageBytes);
            // 1-1. 등록된 얼굴이 아니라면 에러 처리
            if (employeeId == null) {
                log.info("[exit] 등록된 얼굴이 아님");
                return ResponseEntity.ok(ResultMsg.S301.getMsg());
            } else if (employeeId.equals("E400")) {
                log.info("[exit] 카메라에 얼굴 사진이 없음");
                return ResponseEntity.ok(ResultMsg.S302.getMsg());
            }


            // 2. 예약 정보를 찾는다.
            if (!reservationService.existReservation(employeeId)) {
                // 2-1. 예약 정보 없으면 에러 메세지
                log.info("[exit] employeeId [{}] 좌석예약정보 EMPTY", employeeId);
                return ResponseEntity.ok(ResultMsg.S201.getMsg());
            }

            // 3. 좌석번호를 가져오기위해 예약을 조회한다.
            Reservation reservation = reservationService.findReservation(employeeId);
            Employee employee = employeeService.getEmployee(employeeId);

            // 4. 예약정보를 지운다.
            reservationService.deleteReservation(employeeId);

            // 5. SNS 이메일 전송 - 퇴실 완료
            apiGatewayService.sendRequest(employee.getName(), employee.getEmail(), reservation.getSeat(), "exit");

            log.info("[exit] employeeId [{}] seat[{}] 좌석예약정보 DELETE ", employeeId, reservation.getSeat());

            String msg = String.format(ResultMsg.S200.getMsg(), reservation.getSeat(),employee.getName(), reservation.getSeat());
            return ResponseEntity.ok(msg);
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Error processing image");
        }
    }

    @GetMapping("/get-rsv")
    public ResponseEntity<List<Integer>> checkoutSeat() {
        return ResponseEntity.ok(reservationService.getReservation());
    }
}
