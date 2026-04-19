package com.mindshare.auth.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindshare.auth.token.RefreshTokenStore;
import com.mindshare.auth.verification.CodeSender;
import com.mindshare.auth.verification.VerificationCheckResult;
import com.mindshare.auth.verification.VerificationCodeStatus;
import com.mindshare.auth.verification.VerificationCodeStore;
import com.mindshare.auth.verification.VerificationScene;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CapturingCodeSender capturingCodeSender;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM login_logs");
        jdbcTemplate.update("DELETE FROM users");
        capturingCodeSender.clear();
    }

    @Test
    void shouldRegisterSuccessfully() throws Exception {
        mockMvc.perform(post("/api/v1/auth/send-code")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "scene": "REGISTER",
                                  "identifierType": "EMAIL",
                                  "identifier": "alice@example.com"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expireSeconds").value(300));

        String code = capturingCodeSender.lastCode("REGISTER", "alice@example.com");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "identifierType", "EMAIL",
                                "identifier", "alice@example.com",
                                "code", code,
                                "password", "Strong123",
                                "nickname", "Alice"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.id").isNumber())
                .andExpect(jsonPath("$.user.nickname").value("Alice"))
                .andExpect(jsonPath("$.token.accessToken").isString())
                .andExpect(jsonPath("$.token.refreshToken").isString());
    }

    @Test
    void shouldLoginByPasswordSuccessfully() throws Exception {
        jdbcTemplate.update("""
                INSERT INTO users (id, email, password_hash, nickname, create_time, update_time)
                VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP())
                """, 3001L, "login@example.com", passwordEncoder.encode("Strong123"), "Login User");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "identifierType": "EMAIL",
                                  "identifier": "login@example.com",
                                  "password": "Strong123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.id").value(3001))
                .andExpect(jsonPath("$.token.accessToken").isString())
                .andExpect(jsonPath("$.token.refreshToken").isString());
    }

    @Test
    void shouldRejectInvalidRefreshToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/token/refresh")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "not-a-token"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }

    @Test
    void shouldRejectMeWithoutAccessToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @TestConfiguration
    static class TestAuthConfig {

        @Bean
        @Primary
        VerificationCodeStore verificationCodeStore() {
            return new InMemoryVerificationCodeStore();
        }

        @Bean
        @Primary
        CapturingCodeSender capturingCodeSender() {
            return new CapturingCodeSender();
        }

        @Bean
        @Primary
        RefreshTokenStore refreshTokenStore() {
            return new InMemoryRefreshTokenStore();
        }
    }

    static class CapturingCodeSender implements CodeSender {
        private final Map<String, String> codes = new ConcurrentHashMap<>();

        @Override
        public void sendCode(VerificationScene scene, String identifier, String code, int expireMinutes) {
            codes.put(scene.name() + ":" + identifier, code);
        }

        String lastCode(String scene, String identifier) {
            return codes.get(scene + ":" + identifier);
        }

        void clear() {
            codes.clear();
        }
    }

    static class InMemoryVerificationCodeStore implements VerificationCodeStore {
        private final Map<String, Record> store = new ConcurrentHashMap<>();

        @Override
        public void saveCode(String scene, String identifier, String code, Duration ttl, int maxAttempts) {
            store.put(scene + ":" + identifier, new Record(code, maxAttempts, 0));
        }

        @Override
        public VerificationCheckResult verify(String scene, String identifier, String code) {
            String key = scene + ":" + identifier;
            Record record = store.get(key);
            if (record == null) {
                return new VerificationCheckResult(VerificationCodeStatus.NOT_FOUND, 0, 0);
            }
            if (record.attempts >= record.maxAttempts) {
                return new VerificationCheckResult(VerificationCodeStatus.TOO_MANY_ATTEMPTS, record.attempts, record.maxAttempts);
            }
            if (record.code.equals(code)) {
                store.remove(key);
                return new VerificationCheckResult(VerificationCodeStatus.SUCCESS, record.attempts, record.maxAttempts);
            }
            Record updated = new Record(record.code, record.maxAttempts, record.attempts + 1);
            store.put(key, updated);
            if (updated.attempts > updated.maxAttempts) {
                return new VerificationCheckResult(VerificationCodeStatus.TOO_MANY_ATTEMPTS, updated.attempts, updated.maxAttempts);
            }
            return new VerificationCheckResult(VerificationCodeStatus.MISMATCH, updated.attempts, updated.maxAttempts);
        }

        @Override
        public void invalidate(String scene, String identifier) {
            store.remove(scene + ":" + identifier);
        }

        private record Record(String code, int maxAttempts, int attempts) {
        }
    }

    static class InMemoryRefreshTokenStore implements RefreshTokenStore {
        private final Map<Long, Set<String>> tokens = new ConcurrentHashMap<>();

        @Override
        public void storeToken(long userId, String tokenId, Duration ttl) {
            tokens.computeIfAbsent(userId, key -> ConcurrentHashMap.newKeySet()).add(tokenId);
        }

        @Override
        public boolean isTokenValid(long userId, String tokenId) {
            return tokens.getOrDefault(userId, Set.of()).contains(tokenId);
        }

        @Override
        public void revokeToken(long userId, String tokenId) {
            tokens.getOrDefault(userId, Set.of()).remove(tokenId);
        }

        @Override
        public void revokeAll(long userId) {
            tokens.remove(userId);
        }
    }
}
