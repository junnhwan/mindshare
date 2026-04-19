package com.mindshare.auth.token;

import com.mindshare.user.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("bootstrap-test")
class JwtServiceTest {

    @Autowired
    private JwtService jwtService;

    @Test
    void shouldIssueAndDecodeAccessAndRefreshTokens() {
        User user = new User();
        user.setId(1001L);
        user.setEmail("jwt@example.com");
        user.setNickname("Jwt User");

        TokenPair pair = jwtService.issueTokenPair(user);

        assertThat(pair.accessToken()).isNotBlank();
        assertThat(pair.refreshToken()).isNotBlank();
        assertThat(pair.tokenType()).isEqualTo("Bearer");
        assertThat(pair.expiresIn()).isPositive();

        Jwt accessJwt = jwtService.decode(pair.accessToken());
        Jwt refreshJwt = jwtService.decode(pair.refreshToken());

        assertThat(jwtService.extractUserId(accessJwt)).isEqualTo(1001L);
        assertThat(jwtService.extractUserId(refreshJwt)).isEqualTo(1001L);
        assertThat(jwtService.extractTokenType(accessJwt)).isEqualTo("access");
        assertThat(jwtService.extractTokenType(refreshJwt)).isEqualTo("refresh");
        assertThat(jwtService.extractTokenId(refreshJwt)).isNotBlank();
        assertThat(accessJwt.getClaimAsString("nickname")).isEqualTo("Jwt User");
        assertThat(refreshJwt.getClaimAsString("nickname")).isNull();
    }
}
