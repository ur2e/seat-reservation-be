package com.kbsec.seatreservationbe.service;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.*;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.model.InvalidParameterException;
import com.amazonaws.services.rekognition.model.SearchFacesByImageRequest;
import com.amazonaws.services.rekognition.model.SearchFacesByImageResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RekognitionService {

    @Value("${aws.rekognition.collection}")
    String collection;

    @Value("${aws.dynamodb.table}")
    String dynamodbTable;

    private final AmazonRekognition rekognitionClient;
    private final AmazonDynamoDB dynamoDBClient;

    @Autowired
    public RekognitionService(AmazonRekognition rekognitionClient, AmazonDynamoDB dynamoDBClient) {
        this.rekognitionClient = rekognitionClient;
        this.dynamoDBClient = dynamoDBClient;
    }

    public String findEmployeeId(byte[] imageBytes) {
        SearchFacesByImageRequest request = new SearchFacesByImageRequest()
                .withCollectionId(collection)
                .withImage(new Image().withBytes(ByteBuffer.wrap(imageBytes)));

        // STEP 1. Rekognition에서 Face Id 가져온다.
        try {
            SearchFacesByImageResult result = rekognitionClient.searchFacesByImage(request);

            // STEP 2. 가져온 Face Id로 DynamoDB에서 사번을 찾는다.
            for (var match : result.getFaceMatches()) {
                String faceId = match.getFace().getFaceId();
                GetItemRequest getItemRequest = new GetItemRequest()
                        .withTableName(dynamodbTable)
                        .withKey(Map.of("RekognitionId", new AttributeValue(faceId)));
                GetItemResult itemResult = dynamoDBClient.getItem(getItemRequest);

                if (itemResult.getItem() != null) {
                    log.info("[findEmployeeId] EmployeeId : [{}]", itemResult.getItem().get("EmployeeId").getS());
                    return itemResult.getItem().get("EmployeeId").getS();
                }
            }
        } catch (InvalidParameterException e) {
            return "E400";
        } catch (Exception e) {
            return "An error occurred: " + e.getMessage();
        }

        log.info("[findEmployeeId] 찾은 faceId가 없음.");
        return null;
    }

    public List<Map<String, Object>> getAllDynamodbData(){
        // 테이블 객체 가져오기
        ScanRequest scanRequest = new ScanRequest()
                .withTableName(dynamodbTable);

        ScanResult result = dynamoDBClient.scan(scanRequest);
        return result.getItems().stream()
                .map(this::convertItem)
                .collect(Collectors.toList());
    }

    public void deleteAllItems() {
        ScanRequest scanRequest = new ScanRequest().withTableName(dynamodbTable);
        ScanResult scanResult = dynamoDBClient.scan(scanRequest);
        List<Map<String, AttributeValue>> items = scanResult.getItems();

        for (Map<String, AttributeValue> item : items) {
            Map<String, AttributeValue> key = new HashMap<>();
            key.put("RekognitionId", item.get("RekognitionId"));

            DeleteItemRequest deleteRequest = new DeleteItemRequest()
                    .withTableName(dynamodbTable)
                    .withKey(key);
            dynamoDBClient.deleteItem(deleteRequest);
        }

        log.info("All items deleted from table.");
    }

    public void deleteItemByEmployeeId(String employeeId) {
        ScanRequest scanRequest = new ScanRequest()
                .withTableName(dynamodbTable)
                .withFilterExpression("EmployeeId = :employeeId")
                .withExpressionAttributeValues(Map.of(":employeeId", new AttributeValue(employeeId)));

        ScanResult scanResult = dynamoDBClient.scan(scanRequest);
        List<Map<String, AttributeValue>> items = scanResult.getItems();

        for (Map<String, AttributeValue> item : items) {
            Map<String, AttributeValue> key = new HashMap<>();
            key.put("RekognitionId", item.get("RekognitionId")); // 파티션 키

            DeleteItemRequest deleteRequest = new DeleteItemRequest()
                    .withTableName(dynamodbTable)
                    .withKey(key);
            dynamoDBClient.deleteItem(deleteRequest);
        }

        log.info("Items with EmployeeId: {} deleted.", employeeId);
    }

    private Map<String, Object> convertItem(Map<String, AttributeValue> item) {
        Map<String, Object> convertedItem = new HashMap<>();
        for (Map.Entry<String, AttributeValue> entry : item.entrySet()) {
            String attributeName = entry.getKey();
            AttributeValue attributeValue = entry.getValue();

            if (attributeValue.getS() != null) {
                convertedItem.put(attributeName, attributeValue.getS());
            } else if (attributeValue.getN() != null) {
                convertedItem.put(attributeName, attributeValue.getN());
            } else if (attributeValue.getB() != null) {
                convertedItem.put(attributeName, attributeValue.getB().toString());
            } else if (attributeValue.getSS() != null) {
                convertedItem.put(attributeName, attributeValue.getSS());
            } else if (attributeValue.getNS() != null) {
                convertedItem.put(attributeName, attributeValue.getNS());
            } else if (attributeValue.getBS() != null) {
                convertedItem.put(attributeName, attributeValue.getBS().toString());
            } else if (attributeValue.getM() != null) {
                convertedItem.put(attributeName, convertItem(attributeValue.getM()));
            } else if (attributeValue.getL() != null) {
                convertedItem.put(attributeName, attributeValue.getL().stream()
                        .map(this::convertAttributeValue)
                        .collect(Collectors.toList()));
            } else if (attributeValue.getNULL() != null && attributeValue.getNULL()) {
                convertedItem.put(attributeName, null);
            } else if (attributeValue.getBOOL() != null) {
                convertedItem.put(attributeName, attributeValue.getBOOL());
            }
        }
        return convertedItem;
    }

    private Object convertAttributeValue(AttributeValue attributeValue) {
        if (attributeValue.getS() != null) {
            return attributeValue.getS();
        } else if (attributeValue.getN() != null) {
            return attributeValue.getN();
        } else if (attributeValue.getB() != null) {
            return attributeValue.getB().toString();
        } else if (attributeValue.getSS() != null) {
            return attributeValue.getSS();
        } else if (attributeValue.getNS() != null) {
            return attributeValue.getNS();
        } else if (attributeValue.getBS() != null) {
            return attributeValue.getBS().toString();
        } else if (attributeValue.getM() != null) {
            return convertItem(attributeValue.getM());
        } else if (attributeValue.getL() != null) {
            return attributeValue.getL().stream()
                    .map(this::convertAttributeValue)
                    .collect(Collectors.toList());
        } else if (attributeValue.getNULL() != null && attributeValue.getNULL()) {
            return null;
        } else if (attributeValue.getBOOL() != null) {
            return attributeValue.getBOOL();
        }
        return null;
    }
}
