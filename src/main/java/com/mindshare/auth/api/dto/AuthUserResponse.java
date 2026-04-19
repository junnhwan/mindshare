package com.mindshare.auth.api.dto;

public record AuthUserResponse(
        Long id,
        String nickname,
        String avatar,
        String phone,
        String email
) {
}
