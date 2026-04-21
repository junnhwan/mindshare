package com.mindshare.counter.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CounterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM know_posts");
        jdbcTemplate.update("DELETE FROM login_logs");
        jdbcTemplate.update("DELETE FROM users");
        jdbcTemplate.update("""
                INSERT INTO users (id, email, password_hash, nickname, avatar, tags_json, create_time, update_time)
                VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP())
                """, 9101L, "counter@example.com", "hash", "Counter User", "https://cdn.example.com/counter.png", "[\"java\"]");
    }

    @Test
    void shouldToggleLikeAndFavAndReturnCounts() throws Exception {
        String payload = """
                {
                  "entityType": "knowpost",
                  "entityId": "post-1"
                }
                """;

        mockMvc.perform(post("/api/v1/action/like")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject("9101").claim("uid", 9101L)))
                        .contentType(APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.changed").value(true))
                .andExpect(jsonPath("$.liked").value(true));

        mockMvc.perform(post("/api/v1/action/like")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject("9101").claim("uid", 9101L)))
                        .contentType(APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.changed").value(false))
                .andExpect(jsonPath("$.liked").value(true));

        mockMvc.perform(post("/api/v1/action/fav")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject("9101").claim("uid", 9101L)))
                        .contentType(APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.changed").value(true))
                .andExpect(jsonPath("$.faved").value(true));

        mockMvc.perform(get("/api/v1/counter/knowpost/post-1").queryParam("metrics", "like,fav"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entityType").value("knowpost"))
                .andExpect(jsonPath("$.entityId").value("post-1"))
                .andExpect(jsonPath("$.counts.like").value(1))
                .andExpect(jsonPath("$.counts.fav").value(1));

        mockMvc.perform(post("/api/v1/action/unlike")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject("9101").claim("uid", 9101L)))
                        .contentType(APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.changed").value(true))
                .andExpect(jsonPath("$.liked").value(false));

        mockMvc.perform(post("/api/v1/action/unfav")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject("9101").claim("uid", 9101L)))
                        .contentType(APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.changed").value(true))
                .andExpect(jsonPath("$.faved").value(false));

        mockMvc.perform(get("/api/v1/counter/knowpost/post-1").queryParam("metrics", "like,fav"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.counts.like").value(0))
                .andExpect(jsonPath("$.counts.fav").value(0));
    }
}
