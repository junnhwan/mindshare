package com.mindshare.knowpost.api.dto;

import jakarta.validation.constraints.NotNull;

public record KnowPostTopPatchRequest(
        @NotNull Boolean isTop
) {
}
