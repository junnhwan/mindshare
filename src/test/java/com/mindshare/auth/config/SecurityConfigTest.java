package com.mindshare.auth.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("bootstrap-test")
class SecurityConfigTest {

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private SecurityFilterChain securityFilterChain;

    @Test
    void shouldLoadJwtBeansAndSecurityFilterChain() {
        assertThat(jwtEncoder).isNotNull();
        assertThat(jwtDecoder).isNotNull();
        assertThat(securityFilterChain).isNotNull();
    }
}
