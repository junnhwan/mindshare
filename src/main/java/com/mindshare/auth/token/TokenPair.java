package com.mindshare.auth.token;

import java.time.Instant;

public record TokenPair(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        String refreshTokenId,
        Instant refreshTokenExpiresAt
) {
}
