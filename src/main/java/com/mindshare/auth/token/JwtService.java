package com.mindshare.auth.token;

import com.mindshare.auth.config.AuthProperties;
import com.mindshare.user.domain.User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class JwtService {

    private static final String CLAIM_TOKEN_TYPE = "token_type";
    private static final String CLAIM_USER_ID = "uid";

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final AuthProperties authProperties;
    private final Clock clock = Clock.systemUTC();

    public JwtService(JwtEncoder jwtEncoder, JwtDecoder jwtDecoder, AuthProperties authProperties) {
        this.jwtEncoder = jwtEncoder;
        this.jwtDecoder = jwtDecoder;
        this.authProperties = authProperties;
    }

    public TokenPair issueTokenPair(User user) {
        Instant issuedAt = Instant.now(clock);
        Instant accessExpiresAt = issuedAt.plus(authProperties.getJwt().getAccessTokenTtl());
        Instant refreshExpiresAt = issuedAt.plus(authProperties.getJwt().getRefreshTokenTtl());
        String refreshTokenId = UUID.randomUUID().toString();

        String accessToken = encodeAccessToken(user, issuedAt, accessExpiresAt);
        String refreshToken = encodeRefreshToken(user, issuedAt, refreshExpiresAt, refreshTokenId);
        return new TokenPair(
                accessToken,
                refreshToken,
                "Bearer",
                authProperties.getJwt().getAccessTokenTtl().toSeconds(),
                refreshTokenId,
                refreshExpiresAt
        );
    }

    public Jwt decode(String token) {
        return jwtDecoder.decode(token);
    }

    public long extractUserId(Jwt jwt) {
        Object claim = jwt.getClaim(CLAIM_USER_ID);
        if (claim instanceof Number number) {
            return number.longValue();
        }
        if (claim instanceof String text) {
            return Long.parseLong(text);
        }
        return Long.parseLong(jwt.getSubject());
    }

    public String extractTokenType(Jwt jwt) {
        return jwt.getClaimAsString(CLAIM_TOKEN_TYPE);
    }

    public String extractTokenId(Jwt jwt) {
        return jwt.getId();
    }

    private String encodeAccessToken(User user, Instant issuedAt, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(authProperties.getJwt().getIssuer())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(String.valueOf(user.getId()))
                .id(UUID.randomUUID().toString())
                .claim(CLAIM_TOKEN_TYPE, "access")
                .claim(CLAIM_USER_ID, user.getId())
                .claim("nickname", user.getNickname())
                .claim("email", user.getEmail())
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    private String encodeRefreshToken(User user, Instant issuedAt, Instant expiresAt, String tokenId) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(authProperties.getJwt().getIssuer())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(String.valueOf(user.getId()))
                .id(tokenId)
                .claim(CLAIM_TOKEN_TYPE, "refresh")
                .claim(CLAIM_USER_ID, user.getId())
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}
