package com.kbsec.seatreservationbe.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class ApiGatewayService {

    private final RestTemplate restTemplate = new RestTemplate();

    //TODO : action은 Enum으로 처리 (좌석 예약, 퇴실)
    public String sendRequest(String name, String email, int seat, String action) {
        StringBuilder sbUrl = new StringBuilder();
        sbUrl.append("https://6oh7s5cq90.execute-api.ap-northeast-2.amazonaws.com/seat-noti");

        if (action.equals("exit")){
            sbUrl.append("/exit");
        } else if (action.equals("reserve")) {
            sbUrl.append("/reserve");
        }

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(sbUrl.toString())
                .queryParam("name", name)
                .queryParam("email", email)
                .queryParam("seat", seat);

        String uriString = builder.encode().toUriString(); // URI 인코딩을 명시적으로 설정

        return restTemplate.getForObject(uriString, String.class);
    }

    public void sendSNSSubscribeRequest(String name, String email) {
        StringBuilder sbUrl = new StringBuilder();
        sbUrl.append("https://6oh7s5cq90.execute-api.ap-northeast-2.amazonaws.com/seat-noti/subscribe");

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(sbUrl.toString())
                .queryParam("name", name)
                .queryParam("email", email);

        String uriString = builder.encode().toUriString(); // URI 인코딩을 명시적으로 설정

        restTemplate.getForObject(uriString, String.class);
    }
}
