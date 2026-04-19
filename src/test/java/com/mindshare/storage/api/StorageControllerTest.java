package com.mindshare.storage.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StorageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS know_posts (
                    id BIGINT PRIMARY KEY,
                    creator_id BIGINT NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    type VARCHAR(32) NOT NULL,
                    visible VARCHAR(32) NOT NULL,
                    is_top BOOLEAN NOT NULL,
                    title VARCHAR(255),
                    description VARCHAR(512),
                    tags TEXT,
                    img_urls TEXT,
                    content_object_key VARCHAR(512),
                    content_etag VARCHAR(255),
                    content_size BIGINT,
                    content_sha256 VARCHAR(128),
                    content_url VARCHAR(512),
                    publish_time TIMESTAMP,
                    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """);
        jdbcTemplate.update("DELETE FROM know_posts");
        jdbcTemplate.update("DELETE FROM login_logs");
        jdbcTemplate.update("DELETE FROM users");
        jdbcTemplate.update("""
                INSERT INTO users (id, email, password_hash, nickname, create_time, update_time)
                VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP())
                """, 6001L, "owner@example.com", "hash", "Owner");
        jdbcTemplate.update("""
                INSERT INTO users (id, email, password_hash, nickname, create_time, update_time)
                VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP())
                """, 6002L, "other@example.com", "hash", "Other");
        jdbcTemplate.update("""
                INSERT INTO know_posts (id, creator_id, status, type, visible, is_top, create_time, update_time)
                VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP())
                """, 7001L, 6001L, "draft", "image_text", "public", false);
    }

    @Test
    void shouldPresignContentForOwnedDraft() throws Exception {
        mockMvc.perform(post("/api/v1/storage/presign")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject("6001").claim("uid", 6001L)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "scene": "knowpost_content",
                                  "postId": "7001",
                                  "contentType": "text/markdown"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.objectKey").value("posts/7001/content.md"))
                .andExpect(jsonPath("$.headers['Content-Type']").value("text/markdown"))
                .andExpect(jsonPath("$.expiresIn").value(600))
                .andExpect(jsonPath("$.putUrl", containsString("posts/7001/content.md")));
    }

    @Test
    void shouldNormalizeImageExtForPresign() throws Exception {
        mockMvc.perform(post("/api/v1/storage/presign")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject("6001").claim("uid", 6001L)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "scene": "knowpost_image",
                                  "postId": "7001",
                                  "contentType": "image/png",
                                  "ext": "png"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.objectKey")
                        .value(matchesPattern("posts/7001/images/\\d{8}/[a-f0-9]{8}\\.png")));
    }

    @Test
    void shouldRejectPresignForNonOwner() throws Exception {
        mockMvc.perform(post("/api/v1/storage/presign")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject("6002").claim("uid", 6002L)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "scene": "knowpost_content",
                                  "postId": "7001",
                                  "contentType": "text/markdown"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }
}
