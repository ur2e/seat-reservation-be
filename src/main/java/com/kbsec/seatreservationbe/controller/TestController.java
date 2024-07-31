package com.kbsec.seatreservationbe.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.kbsec.seatreservationbe.service.RekognitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class TestController {

    private final RekognitionService rekognitionService;
    private final ObjectMapper objectMapper;

    @Autowired
    public TestController(RekognitionService rekognitionService) {
        this.rekognitionService = rekognitionService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT); // JSON 포맷팅 설정
    }

    @GetMapping("/getall")
    public String scanTable() throws JsonProcessingException {
        List<Map<String, Object>> items = rekognitionService.getAllDynamodbData();
        return objectMapper.writeValueAsString(items); // 포맷팅된 JSON 반환
    }

    @GetMapping("/deleteall")
    public void deleteAllItems() {
        rekognitionService.deleteAllItems();
    }

    @GetMapping("/delete")
    public void deleteItemByEmployeeId(@RequestParam String id) {
        rekognitionService.deleteItemByEmployeeId(id);
    }
}
