package com.mindshare.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "auth")
public class AuthProperties {

    private Jwt jwt = new Jwt();
    private Verification verification = new Verification();

    @Data
    public static class Jwt {

        private String issuer = "mindshare";
        private Duration accessTokenTtl = Duration.ofMinutes(30);
        private Duration refreshTokenTtl = Duration.ofDays(14);
        private Resource publicKeyLocation;
        private Resource privateKeyLocation;
    }

    @Data
    public static class Verification {

        private int codeLength = 6;
        private Duration ttl = Duration.ofMinutes(5);
        private int maxAttempts = 5;
    }
}
