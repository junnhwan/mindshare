package com.mindshare.user.mapper;

import com.mindshare.user.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserMapperTest {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldInsertAndQueryUserByPhoneEmailAndId() {
        User user = new User();
        user.setPhone("13800138000");
        user.setEmail("alice@example.com");
        user.setPasswordHash("hashed-password");
        user.setNickname("Alice");
        user.setAvatar("https://cdn.example.com/avatar.png");
        user.setBio("bio");
        user.setGender("female");
        user.setBirthday(LocalDate.of(1998, 1, 1));
        user.setSchool("Tongji");
        user.setTagsJson("[\"java\"]");
        user.setCreateTime(Instant.parse("2026-04-19T00:00:00Z"));
        user.setUpdateTime(Instant.parse("2026-04-19T00:00:00Z"));

        userMapper.insert(user);

        assertThat(user.getId()).isNotNull();
        assertThat(userMapper.findByPhone("13800138000")).isNotNull();
        assertThat(userMapper.findByEmail("alice@example.com")).isNotNull();
        assertThat(userMapper.findById(user.getId()).getNickname()).isEqualTo("Alice");
    }

    @Test
    void shouldUpdatePasswordAndProfileFields() {
        jdbcTemplate.update("""
                INSERT INTO users (id, phone, email, password_hash, nickname, create_time, update_time)
                VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP())
                """, 1001L, "13900139000", "bob@example.com", "old-hash", "Bob");

        userMapper.updatePassword(1001L, "new-hash");

        User profilePatch = new User();
        profilePatch.setId(1001L);
        profilePatch.setNickname("Bobby");
        profilePatch.setBio("updated");
        profilePatch.setSchool("MindShare University");
        profilePatch.setAvatar("https://cdn.example.com/bob.png");
        userMapper.updateProfile(profilePatch);

        User updated = userMapper.findById(1001L);
        assertThat(updated.getPasswordHash()).isEqualTo("new-hash");
        assertThat(updated.getNickname()).isEqualTo("Bobby");
        assertThat(updated.getBio()).isEqualTo("updated");
        assertThat(updated.getSchool()).isEqualTo("MindShare University");
    }
}
