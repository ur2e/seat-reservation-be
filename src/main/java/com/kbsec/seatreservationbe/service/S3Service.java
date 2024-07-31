package com.kbsec.seatreservationbe.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@Service
public class S3Service {

    private final AmazonS3 amazonS3;
    private final String bucket;

    @Autowired
    public S3Service(AmazonS3 amazonS3, @Value("${cloud.aws.s3.bucket}") String bucket) {
        this.amazonS3 = amazonS3;
        this.bucket = bucket;
    }

    public void uploadFile(MultipartFile file, Map<String, String> metadata) throws IOException {
        try {
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentType(file.getContentType());
            objectMetadata.setContentLength(file.getSize());
            metadata.forEach(objectMetadata::addUserMetadata);

            // capture_를 사원 번호로 대체합니다.
            String filename = file.getOriginalFilename().replaceFirst("capture", metadata.get("EmployeeId"));

            PutObjectRequest request = new PutObjectRequest(bucket, "index/" + filename, file.getInputStream(), objectMetadata);
            PutObjectResult response = amazonS3.putObject(request);

            System.out.println("File uploaded successfully: " + response.getETag());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getCurrentDateTime() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        return LocalDateTime.now().format(formatter);
    }
}
