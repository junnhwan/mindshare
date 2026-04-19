package com.mindshare.knowpost.api.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

public record KnowPostPatchRequest(
        String title,
        Long tagId,
        @Size(max = 20) List<String> tags,
        @Size(max = 20) List<String> imgUrls,
        String visible,
        Boolean isTop,
        String description
) {
}
