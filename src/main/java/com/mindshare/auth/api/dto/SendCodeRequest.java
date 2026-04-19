package com.mindshare.auth.api.dto;

import com.mindshare.auth.model.IdentifierType;
import com.mindshare.auth.verification.VerificationScene;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SendCodeRequest(
        @NotNull VerificationScene scene,
        @NotNull IdentifierType identifierType,
        @NotBlank String identifier
) {
}
