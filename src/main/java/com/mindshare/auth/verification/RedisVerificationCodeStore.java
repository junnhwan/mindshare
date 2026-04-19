package com.mindshare.auth.verification;

import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

@Component
public class RedisVerificationCodeStore implements VerificationCodeStore {

    private static final String FIELD_CODE = "code";
    private static final String FIELD_MAX_ATTEMPTS = "maxAttempts";
    private static final String FIELD_ATTEMPTS = "attempts";

    private final StringRedisTemplate redisTemplate;

    public RedisVerificationCodeStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void saveCode(String scene, String identifier, String code, Duration ttl, int maxAttempts) {
        String key = buildKey(scene, identifier);
        HashOperations<String, String, String> ops = redisTemplate.opsForHash();
        ops.put(key, FIELD_CODE, code);
        ops.put(key, FIELD_MAX_ATTEMPTS, String.valueOf(maxAttempts));
        ops.put(key, FIELD_ATTEMPTS, "0");
        redisTemplate.expire(key, ttl);
    }

    @Override
    public VerificationCheckResult verify(String scene, String identifier, String code) {
        String key = buildKey(scene, identifier);
        HashOperations<String, String, String> ops = redisTemplate.opsForHash();
        Map<String, String> values = ops.entries(key);
        if (values == null || values.isEmpty()) {
            return new VerificationCheckResult(VerificationCodeStatus.NOT_FOUND, 0, 0);
        }

        String storedCode = values.get(FIELD_CODE);
        int maxAttempts = parseInt(values.get(FIELD_MAX_ATTEMPTS), 5);
        int attempts = parseInt(values.get(FIELD_ATTEMPTS), 0);

        if (attempts >= maxAttempts) {
            return new VerificationCheckResult(VerificationCodeStatus.TOO_MANY_ATTEMPTS, attempts, maxAttempts);
        }

        if (Objects.equals(storedCode, code)) {
            redisTemplate.delete(key);
            return new VerificationCheckResult(VerificationCodeStatus.SUCCESS, attempts, maxAttempts);
        }

        int updatedAttempts = attempts + 1;
        ops.put(key, FIELD_ATTEMPTS, String.valueOf(updatedAttempts));
        if (updatedAttempts > maxAttempts) {
            return new VerificationCheckResult(VerificationCodeStatus.TOO_MANY_ATTEMPTS, updatedAttempts, maxAttempts);
        }
        return new VerificationCheckResult(VerificationCodeStatus.MISMATCH, updatedAttempts, maxAttempts);
    }

    @Override
    public void invalidate(String scene, String identifier) {
        redisTemplate.delete(buildKey(scene, identifier));
    }

    private static String buildKey(String scene, String identifier) {
        return "auth:code:%s:%s".formatted(scene, identifier);
    }

    private static int parseInt(String value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }
}
