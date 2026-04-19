package com.mindshare.knowpost.api.dto;

import jakarta.validation.constraints.NotBlank;

public record DescriptionSuggestRequest(
        @NotBlank String content
) {
}
