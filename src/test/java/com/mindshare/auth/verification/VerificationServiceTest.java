package com.mindshare.auth.verification;

import com.mindshare.auth.config.AuthProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class VerificationServiceTest {

    @Test
    void shouldSendCodeAndVerifySuccessfully() {
        InMemoryVerificationCodeStore codeStore = new InMemoryVerificationCodeStore();
        CapturingCodeSender codeSender = new CapturingCodeSender();
        VerificationService verificationService = new VerificationService(authProperties(), codeStore, codeSender);

        SendCodeResult result = verificationService.sendCode(VerificationScene.REGISTER, "alice@example.com");

        assertThat(result.identifier()).isEqualTo("alice@example.com");
        assertThat(result.scene()).isEqualTo(VerificationScene.REGISTER);
        assertThat(result.expireSeconds()).isEqualTo(300);
        assertThat(codeSender.lastCode).hasSize(6);

        VerificationCheckResult checkResult = verificationService.verify(
                VerificationScene.REGISTER,
                "alice@example.com",
                codeSender.lastCode
        );

        assertThat(checkResult.success()).isTrue();
        assertThat(codeStore.records).doesNotContainKey("REGISTER:alice@example.com");
    }

    @Test
    void shouldRejectAfterTooManyAttempts() {
        InMemoryVerificationCodeStore codeStore = new InMemoryVerificationCodeStore();
        CapturingCodeSender codeSender = new CapturingCodeSender();
        VerificationService verificationService = new VerificationService(authProperties(), codeStore, codeSender);

        verificationService.sendCode(VerificationScene.LOGIN, "13800138000");

        VerificationCheckResult first = verificationService.verify(VerificationScene.LOGIN, "13800138000", "000000");
        VerificationCheckResult second = verificationService.verify(VerificationScene.LOGIN, "13800138000", "111111");
        VerificationCheckResult third = verificationService.verify(VerificationScene.LOGIN, "13800138000", "222222");

        assertThat(first.status()).isEqualTo(VerificationCodeStatus.MISMATCH);
        assertThat(second.status()).isEqualTo(VerificationCodeStatus.MISMATCH);
        assertThat(third.status()).isEqualTo(VerificationCodeStatus.TOO_MANY_ATTEMPTS);
    }

    private AuthProperties authProperties() {
        AuthProperties properties = new AuthProperties();
        AuthProperties.Verification verification = new AuthProperties.Verification();
        verification.setCodeLength(6);
        verification.setTtl(Duration.ofMinutes(5));
        verification.setMaxAttempts(2);
        properties.setVerification(verification);
        return properties;
    }

    private static final class CapturingCodeSender implements CodeSender {
        private String lastCode;

        @Override
        public void sendCode(VerificationScene scene, String identifier, String code, int expireMinutes) {
            this.lastCode = code;
        }
    }

    private static final class InMemoryVerificationCodeStore implements VerificationCodeStore {
        private final Map<String, VerificationRecord> records = new HashMap<>();

        @Override
        public void saveCode(String scene, String identifier, String code, Duration ttl, int maxAttempts) {
            records.put(scene + ":" + identifier, new VerificationRecord(code, maxAttempts));
        }

        @Override
        public VerificationCheckResult verify(String scene, String identifier, String code) {
            VerificationRecord record = records.get(scene + ":" + identifier);
            if (record == null) {
                return new VerificationCheckResult(VerificationCodeStatus.NOT_FOUND, 0, 0);
            }
            if (record.attempts >= record.maxAttempts) {
                return new VerificationCheckResult(VerificationCodeStatus.TOO_MANY_ATTEMPTS, record.attempts, record.maxAttempts);
            }
            if (record.code.equals(code)) {
                records.remove(scene + ":" + identifier);
                return new VerificationCheckResult(VerificationCodeStatus.SUCCESS, record.attempts, record.maxAttempts);
            }
            record.attempts++;
            if (record.attempts > record.maxAttempts) {
                return new VerificationCheckResult(VerificationCodeStatus.TOO_MANY_ATTEMPTS, record.attempts, record.maxAttempts);
            }
            return new VerificationCheckResult(VerificationCodeStatus.MISMATCH, record.attempts, record.maxAttempts);
        }

        @Override
        public void invalidate(String scene, String identifier) {
            records.remove(scene + ":" + identifier);
        }

        private static final class VerificationRecord {
            private final String code;
            private final int maxAttempts;
            private int attempts;

            private VerificationRecord(String code, int maxAttempts) {
                this.code = code;
                this.maxAttempts = maxAttempts;
            }
        }
    }
}
