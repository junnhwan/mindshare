package com.mindshare.counter.api.dto;

import jakarta.validation.constraints.NotBlank;

public record ActionRequest(
        @NotBlank String entityType,
        @NotBlank String entityId
) {
}
