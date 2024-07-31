package com.kbsec.seatreservationbe.service;

import com.kbsec.seatreservationbe.entity.Reservation;
import com.kbsec.seatreservationbe.repository.ReservationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReservationService {
    private final ReservationRepository reservationRepository;

    public ReservationService(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    public List<Integer> getReservation() {
        List<Reservation> reservationsList = reservationRepository.findAll();

        List<Integer> seatList = new ArrayList<>();

        for (Reservation r : reservationsList) {
            seatList.add(r.getSeat());
        }

        return seatList;
    }

    public void saveReservation(String employeeId, int seat){
        Reservation reservation = Reservation.builder()
                .employeeId(employeeId)
                .seat(seat)
                .status("reserved")
                .reservedDateTime(LocalDateTime.now())
                .build();

        reservationRepository.save(reservation);
    }

    public void deleteReservation(String employeeId){
        Reservation reservation = findReservation(employeeId);
        reservationRepository.delete(reservation);
    }

    public Reservation findReservation(String employeeId) {
        return reservationRepository.findById(employeeId).orElse(null);
    }

    public boolean existReservation(String employeeId) {
        Reservation reservation = findReservation(employeeId);

        return reservation != null;
    }
}


