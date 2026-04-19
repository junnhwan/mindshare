package com.mindshare.storage;

import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.mindshare.common.exception.BusinessException;
import com.mindshare.common.exception.ErrorCode;
import com.mindshare.storage.config.OssProperties;
import org.springframework.stereotype.Service;

import java.net.URL;
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
