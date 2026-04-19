package com.mindshare.auth.api.dto;

import com.mindshare.auth.model.IdentifierType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LoginRequest(
        @NotNull IdentifierType identifierType,
        @NotBlank String identifier,
        String password,
        String code
) {
}
