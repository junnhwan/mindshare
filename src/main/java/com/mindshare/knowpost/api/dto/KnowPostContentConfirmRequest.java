package com.mindshare.knowpost.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record KnowPostContentConfirmRequest(
        @NotBlank String objectKey,
        @NotBlank String etag,
        @NotNull Long size,
        @NotBlank String sha256
) {
}
