package com.mindshare.auth.verification;

public record SendCodeResult(
        String identifier,
        VerificationScene scene,
        int expireSeconds
) {
}
