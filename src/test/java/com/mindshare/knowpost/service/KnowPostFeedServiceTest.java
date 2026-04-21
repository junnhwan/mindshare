package com.mindshare.knowpost.service;

import com.mindshare.common.exception.BusinessException;
import com.mindshare.common.exception.ErrorCode;
import com.mindshare.knowpost.api.dto.FeedPageResponse;
import com.mindshare.knowpost.api.dto.KnowPostDetailResponse;
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
class KnowPostFeedServiceTest {

    @Autowired
    private KnowPostService knowPostService;

    @Autowired
    private KnowPostFeedService knowPostFeedService;

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
                """, 8201L, "author@example.com", "hash", "Author", "https://cdn.example.com/author.png", "[\"java\"]");
    }

    @Test
    void shouldCachePublicAndMineFeedsUntilServiceInvalidation() {
        long id = createPublishedPost("Original Title");

        FeedPageResponse publicFeed = knowPostFeedService.getPublicFeed(1, 20, null);
        FeedPageResponse mineFeed = knowPostFeedService.getMyPublished(8201L, 1, 20);
        assertThat(publicFeed.items().getFirst().title()).isEqualTo("Original Title");
        assertThat(mineFeed.items().getFirst().title()).isEqualTo("Original Title");

        jdbcTemplate.update("UPDATE know_posts SET title = ?, update_time = CURRENT_TIMESTAMP WHERE id = ?", "Direct DB Title", id);

        assertThat(knowPostFeedService.getPublicFeed(1, 20, null).items().getFirst().title()).isEqualTo("Original Title");
        assertThat(knowPostFeedService.getMyPublished(8201L, 1, 20).items().getFirst().title()).isEqualTo("Original Title");

        knowPostService.updateMetadata(
                8201L,
                id,
                "Service Updated Title",
                1L,
                List.of("java"),
                List.of("https://cdn.example.com/cover.png"),
                "public",
                true,
                "updated"
        );

        assertThat(knowPostFeedService.getPublicFeed(1, 20, null).items().getFirst().title()).isEqualTo("Service Updated Title");
        assertThat(knowPostFeedService.getMyPublished(8201L, 1, 20).items().getFirst().title()).isEqualTo("Service Updated Title");
    }

    @Test
    void shouldKeepUnrelatedPublicFeedPagesCachedWhenAnotherPostUpdates() {
        long oldestId = createPublishedPost("Page One");
        createPublishedPost("Page Two");
        long newestId = createPublishedPost("Page Three");

        FeedPageResponse thirdPage = knowPostFeedService.getPublicFeed(3, 1, null);
        assertThat(thirdPage.items()).hasSize(1);
        assertThat(thirdPage.items().getFirst().title()).isEqualTo("Page One");

        jdbcTemplate.update("UPDATE know_posts SET title = ?, update_time = CURRENT_TIMESTAMP WHERE id = ?", "Direct DB Page One", oldestId);

        knowPostService.updateMetadata(
                8201L,
                newestId,
                "Page Three Updated",
                1L,
                List.of("java"),
                List.of("https://cdn.example.com/cover.png"),
                "public",
                false,
                "updated"
        );

        FeedPageResponse cachedThirdPage = knowPostFeedService.getPublicFeed(3, 1, null);
        assertThat(cachedThirdPage.items()).hasSize(1);
        assertThat(cachedThirdPage.items().getFirst().title()).isEqualTo("Page One");

        FeedPageResponse refreshedFirstPage = knowPostFeedService.getPublicFeed(1, 1, null);
        assertThat(refreshedFirstPage.items()).hasSize(1);
        assertThat(refreshedFirstPage.items().getFirst().title()).isEqualTo("Page Three Updated");
    }

    @Test
    void shouldInvalidateDetailCacheAfterPublishAndDelete() {
        long id = knowPostService.createDraft(8201L);
        knowPostService.confirmContent(8201L, id, "posts/" + id + "/content.md", "etag-1", 128L, "sha-1");
        knowPostService.updateMetadata(
                8201L,
                id,
                "Draft Title",
                1L,
                List.of("java"),
                List.of("https://cdn.example.com/draft.png"),
                "public",
                false,
                "draft description"
        );

        KnowPostDetailResponse cachedDraft = knowPostService.getDetail(id, 8201L);
        assertThat(cachedDraft.title()).isEqualTo("Draft Title");

        jdbcTemplate.update("UPDATE know_posts SET title = ?, update_time = CURRENT_TIMESTAMP WHERE id = ?", "Changed While Cached", id);
        assertThat(knowPostService.getDetail(id, 8201L).title()).isEqualTo("Draft Title");

        knowPostService.publish(8201L, id);
        assertThat(knowPostService.getDetail(id, null).title()).isEqualTo("Changed While Cached");

        knowPostService.delete(8201L, id);

        assertThatThrownBy(() -> knowPostService.getDetail(id, 8201L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.BAD_REQUEST);
    }

    private long createPublishedPost(String title) {
        long id = knowPostService.createDraft(8201L);
        knowPostService.confirmContent(8201L, id, "posts/" + id + "/content.md", "etag-1", 128L, "sha-1");
        knowPostService.updateMetadata(
                8201L,
                id,
                title,
                1L,
                List.of("java"),
                List.of("https://cdn.example.com/cover.png"),
                "public",
                false,
                "published"
        );
        knowPostService.publish(8201L, id);
        return id;
    }
}
