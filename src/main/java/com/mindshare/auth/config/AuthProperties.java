package com.mindshare.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

import java.time.Duration;

@ConfigurationProperties(prefix = "auth")
public class AuthProperties {

    private Jwt jwt = new Jwt();
    private Verification verification = new Verification();

    public Jwt getJwt() {
        return jwt;
    }

    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }

    public Verification getVerification() {
        return verification;
    }

    public void setVerification(Verification verification) {
        this.verification = verification;
    }

    public static class Jwt {

        private String issuer = "mindshare";
        private Duration accessTokenTtl = Duration.ofMinutes(30);
        private Duration refreshTokenTtl = Duration.ofDays(14);
        private Resource publicKeyLocation;
        private Resource privateKeyLocation;

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        public Duration getAccessTokenTtl() {
            return accessTokenTtl;
        }

        public void setAccessTokenTtl(Duration accessTokenTtl) {
            this.accessTokenTtl = accessTokenTtl;
        }

        public Duration getRefreshTokenTtl() {
            return refreshTokenTtl;
        }

        public void setRefreshTokenTtl(Duration refreshTokenTtl) {
            this.refreshTokenTtl = refreshTokenTtl;
        }

        public Resource getPublicKeyLocation() {
            return publicKeyLocation;
        }

        public void setPublicKeyLocation(Resource publicKeyLocation) {
            this.publicKeyLocation = publicKeyLocation;
        }

        public Resource getPrivateKeyLocation() {
            return privateKeyLocation;
        }

        public void setPrivateKeyLocation(Resource privateKeyLocation) {
            this.privateKeyLocation = privateKeyLocation;
        }
    }

    public static class Verification {

        private int codeLength = 6;
        private Duration ttl = Duration.ofMinutes(5);
        private int maxAttempts = 5;

        public int getCodeLength() {
            return codeLength;
        }

        public void setCodeLength(int codeLength) {
            this.codeLength = codeLength;
        }

        public Duration getTtl() {
            return ttl;
        }

        public void setTtl(Duration ttl) {
            this.ttl = ttl;
        }

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }
    }
}
