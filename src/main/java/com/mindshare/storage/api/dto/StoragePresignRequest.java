package com.mindshare.storage.api.dto;

import jakarta.validation.constraints.NotBlank;

public record StoragePresignRequest(
        @NotBlank String scene,
        @NotBlank String postId,
        @NotBlank String contentType,
        String ext
) {
}
