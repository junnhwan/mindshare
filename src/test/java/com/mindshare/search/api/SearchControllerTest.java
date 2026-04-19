package com.mindshare.search.api;

import com.mindshare.knowpost.service.KnowPostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private KnowPostService knowPostService;

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
                """, 8401L, "author@example.com", "hash", "Author", "https://cdn.example.com/author.png", "[\"java\"]");
    }

    @Test
    void shouldSearchAndSuggestThroughApi() throws Exception {
        long id = knowPostService.createDraft(8401L);
        knowPostService.confirmContent(8401L, id, "posts/" + id + "/content.md", "etag-1", 128L, "sha-1");
        knowPostService.updateMetadata(
                8401L,
                id,
                "Controller Search Gamma",
                1L,
                List.of("java", "search"),
                List.of("https://cdn.example.com/cover.png"),
                "public",
                false,
                "controller gamma backend"
        );
        knowPostService.publish(8401L, id);

        mockMvc.perform(get("/api/v1/search").queryParam("q", "Gamma"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(String.valueOf(id)))
                .andExpect(jsonPath("$.items[0].title").value("Controller Search Gamma"));

        mockMvc.perform(get("/api/v1/search/suggest").queryParam("prefix", "Controller Search G"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0]").value("Controller Search Gamma"));
    }
}
