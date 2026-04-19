package com.mindshare.auth.config;

import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public final class PemUtils {

    private PemUtils() {
    }

    public static RSAPublicKey readPublicKey(Resource resource) {
        try {
            String pem = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            String content = pem.replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] decoded = Base64.getDecoder().decode(content);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(keySpec);
        } catch (IOException | GeneralSecurityException exception) {
            throw new IllegalStateException("Failed to read RSA public key", exception);
        }
    }

    public static RSAPrivateKey readPrivateKey(Resource resource) {
        try {
            String pem = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            String content = pem.replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] decoded = Base64.getDecoder().decode(content);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
            return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(keySpec);
        } catch (IOException | GeneralSecurityException exception) {
            throw new IllegalStateException("Failed to read RSA private key", exception);
        }
    }
}
