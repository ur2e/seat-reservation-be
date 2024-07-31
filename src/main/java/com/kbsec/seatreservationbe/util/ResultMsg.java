package com.kbsec.seatreservationbe.util;

import lombok.Getter;

@Getter
public enum ResultMsg {
    /*
    * S0 : 회원 등록 요청
    * S1 : 자리예약 요청
    * S2 : 퇴실 요청
    * S3 : 공통
    * */
    S000("S000%s님 등록 완료되었습니다.", "회원등록요청 - 성공", 1),

    S100("S100🔊  %s님 안녕하세요. %d번 좌석 예약되었습니다.", "좌석예약요청 - 성공", 2),
    S101("S101이미 %d번 좌석을 이용 중입니다.", "자리예약요청 - 이미 선택한 좌석이 없음", 1),

    //코드,좌석번호|메세지
    S200("S200%d|%s님 오늘도 수고하셨습니다😄 %d번 좌석 퇴실 처리되었습니다.", "퇴실요청 - 성공", 3),
    S201("S201먼저 좌석을 선택해주세요.", "퇴실요청 - 선택한 좌석이 없음", 0),

    S301("S301등록되지 않은 사용자입니다. 등록을 진행해주세요.", "자리예약요청/퇴실 - 존재하지않는 사원 얼굴임", 0),
    S302("S302카메라 정면을 응시해주세요.", "자리예약요청/퇴실 - 사진에 얼굴이 없음.", 0);

    private final String msg;
    private final String explain;
    private final int paramCnt;

    ResultMsg(String msg, String explain, int paramCnt) {
        this.msg = msg;
        this.explain = explain;
        this.paramCnt = paramCnt;
    }
}
