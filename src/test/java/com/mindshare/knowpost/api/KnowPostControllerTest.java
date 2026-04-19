package com.mindshare.knowpost.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class KnowPostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
                """, 8101L, "author@example.com", "hash", "Author", "https://cdn.example.com/author.png", "[\"java\"]");
    }

    @Test
    void shouldCreatePublishAndQueryKnowPostFlow() throws Exception {
        long id = createDraft();

        mockMvc.perform(post("/api/v1/knowposts/{id}/content/confirm", id)
                        .with(jwt(8101L))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "objectKey": "posts/%s/content.md",
                                  "etag": "etag-1",
                                  "size": 128,
                                  "sha256": "sha-1"
                                }
                                """.formatted(id)))
                .andExpect(status().isNoContent());

        mockMvc.perform(patch("/api/v1/knowposts/{id}", id)
                        .with(jwt(8101L))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "MindShare Replica",
                                  "tagId": 1,
                                  "tags": ["java", "spring"],
                                  "imgUrls": ["https://cdn.example.com/cover.png"],
                                  "visible": "public",
                                  "isTop": true,
                                  "description": "Resume friendly backend replica"
                                }
                                """))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/knowposts/{id}/publish", id).with(jwt(8101L)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/knowposts/feed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(String.valueOf(id)))
                .andExpect(jsonPath("$.items[0].title").value("MindShare Replica"));

        mockMvc.perform(get("/api/v1/knowposts/mine")
                        .with(jwt(8101L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(String.valueOf(id)))
                .andExpect(jsonPath("$.items[0].isTop").value(true));

        mockMvc.perform(get("/api/v1/knowposts/detail/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(String.valueOf(id)))
                .andExpect(jsonPath("$.title").value("MindShare Replica"))
                .andExpect(jsonPath("$.authorNickname").value("Author"));
    }

    @Test
    void shouldPatchVisibilityTopSuggestAndDelete() throws Exception {
        long id = createDraft();

        mockMvc.perform(patch("/api/v1/knowposts/{id}/top", id)
                        .with(jwt(8101L))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "isTop": true
                                }
                                """))
                .andExpect(status().isNoContent());

        mockMvc.perform(patch("/api/v1/knowposts/{id}/visibility", id)
                        .with(jwt(8101L))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "visible": "private"
                                }
                                """))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/knowposts/description/suggest")
                        .with(jwt(8101L))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "This backend reimplements auth profile storage knowpost feed and search with a resume friendly scope for interviews."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").exists())
                .andExpect(jsonPath("$.description", not(blankOrNullString())));

        mockMvc.perform(delete("/api/v1/knowposts/{id}", id).with(jwt(8101L)))
                .andExpect(status().isNoContent());
    }

    private long createDraft() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/knowposts/drafts").with(jwt(8101L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isString())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return Long.parseLong(json.get("id").asText());
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwt(long userId) {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(jwt -> jwt.subject(String.valueOf(userId)).claim("uid", userId));
    }
}
