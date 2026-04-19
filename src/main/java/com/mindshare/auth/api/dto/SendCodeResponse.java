package com.mindshare.auth.api.dto;

import com.mindshare.auth.verification.VerificationScene;

public record SendCodeResponse(
        String identifier,
        VerificationScene scene,
        int expireSeconds
) {
}
