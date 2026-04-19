package com.mindshare.auth.api.dto;

import com.mindshare.auth.model.IdentifierType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterRequest(
        @NotNull IdentifierType identifierType,
        @NotBlank String identifier,
        @NotBlank String code,
        @NotBlank String password,
        String nickname
) {
}
