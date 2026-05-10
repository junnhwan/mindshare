package com.mindshare.auth.verification;

import com.mindshare.auth.config.AuthProperties;
import com.mindshare.common.exception.BusinessException;
import com.mindshare.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;

@Service
public class VerificationService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AuthProperties authProperties;
    private final VerificationCodeStore codeStore;
    private final CodeSender codeSender;

    public VerificationService(
            AuthProperties authProperties,
            VerificationCodeStore codeStore,
            CodeSender codeSender
    ) {
        this.authProperties = authProperties;
        this.codeStore = codeStore;
        this.codeSender = codeSender;
    }

    public SendCodeResult sendCode(VerificationScene scene, String identifier) {
        if (scene == null || !StringUtils.hasText(identifier)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, ErrorCode.BAD_REQUEST.getDefaultMessage());
        }
        AuthProperties.Verification verification = authProperties.getVerification();
        String code = generateNumericCode(verification.getCodeLength());
        codeStore.saveCode(scene.name(), identifier, code, verification.getTtl(), verification.getMaxAttempts());
        codeSender.sendCode(scene, identifier, code, (int) verification.getTtl().toMinutes());
        return new SendCodeResult(identifier, scene, (int) verification.getTtl().toSeconds());
    }

    public VerificationCheckResult verify(VerificationScene scene, String identifier, String code) {
        if (scene == null || !StringUtils.hasText(identifier) || !StringUtils.hasText(code)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, ErrorCode.BAD_REQUEST.getDefaultMessage());
        }
        return codeStore.verify(scene.name(), identifier, code);
    }

    public void invalidate(VerificationScene scene, String identifier) {
        codeStore.invalidate(scene.name(), identifier);
    }

    private static String generateNumericCode(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int index = 0; index < length; index++) {
            builder.append(RANDOM.nextInt(10));
        }
        return builder.toString();
    }
}
