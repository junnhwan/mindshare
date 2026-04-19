package com.mindshare.profile.api.dto;

import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ProfilePatchRequest(
        @Size(min = 1, max = 64) String nickname,
        @Size(max = 512) String bio,
        @Pattern(regexp = "(?i)MALE|FEMALE|OTHER|UNKNOWN") String gender,
        @PastOrPresent LocalDate birthday,
        @Size(max = 128) String school,
        String tagJson,
        String avatar
) {
}
