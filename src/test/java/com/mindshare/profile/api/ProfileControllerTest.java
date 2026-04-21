package com.mindshare.profile.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;

import com.mindshare.storage.OssStorageService;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private OssStorageService ossStorageService;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM know_posts");
        jdbcTemplate.update("DELETE FROM login_logs");
        jdbcTemplate.update("DELETE FROM users");
        jdbcTemplate.update("""
                INSERT INTO users (id, phone, email, password_hash, nickname, avatar, bio, tags_json, school, gender, create_time, update_time)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP())
                """, 5001L, "13800000001", "profile@example.com", "hash", "Profile User", "https://cdn.example.com/a.png",
                "before", "[\"java\"]", "Tongji", "UNKNOWN");
    }

    @Test
    void shouldGetCurrentProfile() throws Exception {
        mockMvc.perform(get("/api/v1/profile")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject("5001").claim("uid", 5001L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5001))
                .andExpect(jsonPath("$.nickname").value("Profile User"))
                .andExpect(jsonPath("$.email").value("profile@example.com"));
    }

    @Test
    void shouldPatchCurrentProfile() throws Exception {
        mockMvc.perform(patch("/api/v1/profile")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject("5001").claim("uid", 5001L)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "Updated User",
                                  "bio": "after",
                                  "gender": "FEMALE",
                                  "school": "MindShare University",
                                  "tagJson": "[\\"spring\\",\\"search\\"]"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("Updated User"))
                .andExpect(jsonPath("$.bio").value("after"))
                .andExpect(jsonPath("$.gender").value("FEMALE"))
                .andExpect(jsonPath("$.school").value("MindShare University"))
                .andExpect(jsonPath("$.tagJson").value("[\"spring\",\"search\"]"));
    }

    @Test
    void shouldUploadAvatarAndUpdateProfile() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", "avatar".getBytes());
        given(ossStorageService.uploadAvatar(eq(5001L), any())).willReturn("https://cdn.example.com/avatar-new.png");

        mockMvc.perform(multipart("/api/v1/profile/avatar")
                        .file(file)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject("5001").claim("uid", 5001L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5001))
                .andExpect(jsonPath("$.avatar").value("https://cdn.example.com/avatar-new.png"));
    }
}
