package com.mindshare.counter.service;

import com.mindshare.knowpost.service.KnowPostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class UserCounterServiceTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private KnowPostService knowPostService;

    @Autowired
    private CounterService counterService;

    @Autowired
    private UserCounterService userCounterService;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM know_posts");
        jdbcTemplate.update("DELETE FROM login_logs");
        jdbcTemplate.update("DELETE FROM users");
        jdbcTemplate.update("""
                INSERT INTO users (id, email, password_hash, nickname, avatar, tags_json, create_time, update_time)
                VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP())
                """, 9201L, "author@example.com", "hash", "Author", "https://cdn.example.com/author.png", "[\"java\"]");
        jdbcTemplate.update("""
                INSERT INTO users (id, email, password_hash, nickname, avatar, tags_json, create_time, update_time)
                VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP())
                """, 9202L, "reader@example.com", "hash", "Reader", "https://cdn.example.com/reader.png", "[\"search\"]");
    }

    @Test
    void shouldTrackPublishedPostsAndReceivedInteractions() {
        long firstId = publish(9201L, "Counter First");
        long secondId = publish(9201L, "Counter Second");

        assertThat(userCounterService.getCounts(9201L))
                .containsEntry("posts", 2L)
                .containsEntry("likesReceived", 0L)
                .containsEntry("favsReceived", 0L);

        assertThat(counterService.like("knowpost", String.valueOf(firstId), 9202L)).isTrue();
        assertThat(counterService.fav("knowpost", String.valueOf(secondId), 9202L)).isTrue();

        assertThat(userCounterService.getCounts(9201L))
                .containsEntry("posts", 2L)
                .containsEntry("likesReceived", 1L)
                .containsEntry("favsReceived", 1L);

        knowPostService.delete(9201L, secondId);

        assertThat(userCounterService.getCounts(9201L))
                .containsEntry("posts", 1L)
                .containsEntry("likesReceived", 1L)
                .containsEntry("favsReceived", 1L);
    }

    @Test
    void shouldRebuildUserCountersFromFacts() {
        insertUser(9301L, "author-rebuild@example.com", "Author Rebuild");
        insertUser(9302L, "reader-rebuild@example.com", "Reader Rebuild");

        long firstId = publish(9301L, "Rebuild First");
        publish(9301L, "Rebuild Second");
        assertThat(counterService.like("knowpost", String.valueOf(firstId), 9302L)).isTrue();
        assertThat(counterService.fav("knowpost", String.valueOf(firstId), 9302L)).isTrue();

        userCounterService.incrementPosts(9301L, 9);
        userCounterService.incrementLikesReceived(9301L, 7);
        userCounterService.incrementFavsReceived(9301L, 5);

        Map<String, Long> broken = userCounterService.getCounts(9301L);
        assertThat(broken.get("posts")).isEqualTo(11L);
        assertThat(broken.get("likesReceived")).isEqualTo(8L);
        assertThat(broken.get("favsReceived")).isEqualTo(6L);

        userCounterService.rebuildAllCounters(9301L);

        assertThat(userCounterService.getCounts(9301L))
                .containsEntry("followings", 0L)
                .containsEntry("followers", 0L)
                .containsEntry("posts", 2L)
                .containsEntry("likesReceived", 1L)
                .containsEntry("favsReceived", 1L);
    }

    private long publish(long creatorId, String title) {
        long id = knowPostService.createDraft(creatorId);
        knowPostService.confirmContent(creatorId, id, "posts/" + id + "/content.md", "etag-1", 128L, "sha-1");
        knowPostService.updateMetadata(
                creatorId,
                id,
                title,
                1L,
                List.of("java"),
                List.of("https://cdn.example.com/cover.png"),
                "public",
                false,
                "published"
        );
        knowPostService.publish(creatorId, id);
        return id;
    }

    private void insertUser(long userId, String email, String nickname) {
        jdbcTemplate.update("""
                INSERT INTO users (id, email, password_hash, nickname, avatar, tags_json, create_time, update_time)
                VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP())
                """, userId, email, "hash", nickname, "https://cdn.example.com/" + userId + ".png", "[\"java\"]");
    }
}
