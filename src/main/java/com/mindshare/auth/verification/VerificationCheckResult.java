package com.mindshare.auth.verification;

public record VerificationCheckResult(
        VerificationCodeStatus status,
        int attempts,
        int maxAttempts
) {
    public boolean success() {
        return status == VerificationCodeStatus.SUCCESS;
    }
}
