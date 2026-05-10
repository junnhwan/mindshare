package com.mindshare.storage.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "oss")
public class OssProperties {

    private String endpoint = "https://oss-cn-hangzhou.aliyuncs.com";
    private String accessKeyId = "test-access-key-id";
    private String accessKeySecret = "test-access-key-secret";
    private String bucket = "mindshare-dev";
    private String publicDomain;
    private String basePath = "mindshare";
    private int presignExpireSeconds = 600;
}
