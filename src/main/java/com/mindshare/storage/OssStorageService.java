package com.mindshare.storage;

import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.aliyun.oss.model.PutObjectRequest;
import com.mindshare.common.exception.BusinessException;
import com.mindshare.common.exception.ErrorCode;
import com.mindshare.storage.config.OssProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.util.Date;

@Service
public class OssStorageService {

    private final OssProperties properties;

    public OssStorageService(OssProperties properties) {
        this.properties = properties;
    }

    public String generatePresignedPutUrl(String objectKey, String contentType, int expiresInSeconds) {
        ensureConfigured();
        OSS client = new OSSClientBuilder().build(
                properties.getEndpoint(),
                properties.getAccessKeyId(),
                properties.getAccessKeySecret()
        );
        try {
            Date expiration = new Date(System.currentTimeMillis() + expiresInSeconds * 1000L);
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
                    properties.getBucket(),
                    objectKey,
                    HttpMethod.PUT
            );
            request.setExpiration(expiration);
            if (contentType != null && !contentType.isBlank()) {
                request.setContentType(contentType);
            }
            URL url = client.generatePresignedUrl(request);
            return url.toString();
        } finally {
            client.shutdown();
        }
    }

    public String uploadAvatar(long userId, MultipartFile file) {
        ensureConfigured();
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }
        String objectKey = properties.getBasePath() + "/avatars/" + userId + "-" + Instant.now().toEpochMilli() + extension;

        OSS client = new OSSClientBuilder().build(
                properties.getEndpoint(),
                properties.getAccessKeyId(),
                properties.getAccessKeySecret()
        );
        try {
            PutObjectRequest request = new PutObjectRequest(properties.getBucket(), objectKey, file.getInputStream());
            client.putObject(request);
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "failed to read avatar file");
        } finally {
            client.shutdown();
        }
        return publicUrl(objectKey);
    }

    public String publicUrl(String objectKey) {
        if (properties.getPublicDomain() != null && !properties.getPublicDomain().isBlank()) {
            return properties.getPublicDomain().replaceAll("/$", "") + "/" + objectKey;
        }
        return "https://" + properties.getBucket() + "." + properties.getEndpoint().replaceFirst("^https?://", "") + "/" + objectKey;
    }

    public int getPresignExpireSeconds() {
        return properties.getPresignExpireSeconds();
    }

    private void ensureConfigured() {
        if (isBlank(properties.getEndpoint())
                || isBlank(properties.getAccessKeyId())
                || isBlank(properties.getAccessKeySecret())
                || isBlank(properties.getBucket())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "oss is not configured");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
