package com.mindshare.knowpost.service;

import com.mindshare.common.exception.BusinessException;
import com.mindshare.common.exception.ErrorCode;
import com.mindshare.knowpost.api.dto.KnowPostDetailResponse;
import com.mindshare.knowpost.mapper.KnowPostMapper;
import com.mindshare.knowpost.model.KnowPost;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class KnowPostServiceTest {

    @Autowired
    private KnowPostService knowPostService;

    @Autowired
    private KnowPostMapper knowPostMapper;

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
                """, 8001L, "author@example.com", "hash", "Author", "https://cdn.example.com/author.png", "[\"java\"]");
        jdbcTemplate.update("""
                INSERT INTO users (id, email, password_hash, nickname, avatar, tags_json, create_time, update_time)
                VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP())
                """, 8002L, "other@example.com", "hash", "Other", "https://cdn.example.com/other.png", "[\"search\"]");
    }

    @Test
    void shouldCreatePublishAndLoadDetail() {
        long id = knowPostService.createDraft(8001L);

        knowPostService.confirmContent(8001L, id, "posts/" + id + "/content.md", "etag-1", 128L, "sha-1");
        knowPostService.updateMetadata(
                8001L,
                id,
                "MindShare Replica",
                3L,
                List.of("java", "spring"),
                List.of("https://cdn.example.com/cover.png"),
                "public",
                true,
                "Resume friendly backend replica"
        );
        knowPostService.publish(8001L, id);

        KnowPost stored = knowPostMapper.findById(id);
        assertThat(stored.getStatus()).isEqualTo("published");
        assertThat(stored.getContentObjectKey()).isEqualTo("posts/" + id + "/content.md");
        assertThat(stored.getContentUrl()).contains("posts/" + id + "/content.md");
        assertThat(stored.getTitle()).isEqualTo("MindShare Replica");
        assertThat(stored.getTags()).isEqualTo("[\"java\",\"spring\"]");
        assertThat(stored.getImgUrls()).isEqualTo("[\"https://cdn.example.com/cover.png\"]");

        KnowPostDetailResponse detail = knowPostService.getDetail(id, null);
        assertThat(detail.id()).isEqualTo(String.valueOf(id));
        assertThat(detail.title()).isEqualTo("MindShare Replica");
        assertThat(detail.description()).isEqualTo("Resume friendly backend replica");
        assertThat(detail.contentUrl()).contains("posts/" + id + "/content.md");
        assertThat(detail.images()).containsExactly("https://cdn.example.com/cover.png");
        assertThat(detail.tags()).containsExactly("java", "spring");
        assertThat(detail.authorId()).isEqualTo("8001");
        assertThat(detail.authorNickname()).isEqualTo("Author");
        assertThat(detail.isTop()).isTrue();
        assertThat(detail.visible()).isEqualTo("public");
    }

    @Test
    void shouldSoftDeleteKnowPost() {
        long id = knowPostService.createDraft(8001L);
        knowPostService.publish(8001L, id);

        knowPostService.delete(8001L, id);

        assertThat(knowPostMapper.findById(id).getStatus()).isEqualTo("deleted");
        assertThatThrownBy(() -> knowPostService.getDetail(id, 8001L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.BAD_REQUEST);
    }
}
