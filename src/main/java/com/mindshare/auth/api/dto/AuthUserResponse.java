package com.mindshare.auth.api.dto;

import java.time.LocalDate;

public record AuthUserResponse(
        Long id,
        String nickname,
        String avatar,
        String phone,
        String email,
        String zgId,
        String gender,
        LocalDate birthday,
        String school,
        String bio,
        String tagsJson
) {
}
