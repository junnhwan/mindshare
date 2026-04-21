package com.mindshare.search.service;

import com.mindshare.counter.service.CounterService;
import com.mindshare.knowpost.service.KnowPostService;
import com.mindshare.search.api.dto.SearchResponse;
import com.mindshare.search.api.dto.SuggestResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class SearchServiceTest {

    @Autowired
    private KnowPostService knowPostService;

    @Autowired
    private SearchService searchService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CounterService counterService;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM know_posts");
        jdbcTemplate.update("DELETE FROM login_logs");
        jdbcTemplate.update("DELETE FROM users");
        jdbcTemplate.update("""
                INSERT INTO users (id, email, password_hash, nickname, avatar, tags_json, create_time, update_time)
                VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP())
                """, 8301L, "author@example.com", "hash", "Author", "https://cdn.example.com/author.png", "[\"java\"]");
    }

    @Test
    void shouldSearchSuggestAndSyncAfterUpdateDelete() {
        long id = publish("Search Seed Alpha", "search alpha backend", List.of("java", "search"));

        SearchResponse searchAlpha = searchService.search("Alpha", 20, null, null, null);
        assertThat(searchAlpha.items()).hasSize(1);
        assertThat(searchAlpha.items().getFirst().id()).isEqualTo(String.valueOf(id));
        assertThat(searchAlpha.items().getFirst().title()).isEqualTo("Search Seed Alpha");

        SuggestResponse suggestAlpha = searchService.suggest("Search Seed A", 10);
        assertThat(suggestAlpha.items()).contains("Search Seed Alpha");

        knowPostService.updateMetadata(
                8301L,
                id,
                "Search Seed Beta",
                1L,
                List.of("java", "search"),
                List.of("https://cdn.example.com/cover.png"),
                "public",
                false,
                "search beta backend"
        );

        SearchResponse searchBeta = searchService.search("Beta", 20, null, null, null);
        assertThat(searchBeta.items()).hasSize(1);
        assertThat(searchBeta.items().getFirst().title()).isEqualTo("Search Seed Beta");

        knowPostService.delete(8301L, id);

        SearchResponse deleted = searchService.search("Beta", 20, null, null, null);
        assertThat(deleted.items()).isEmpty();
    }

    @Test
    void shouldSupportCursorLikeAfterToken() {
        publish("Cursor Example First", "cursor search first", List.of("java"));
        publish("Cursor Example Second", "cursor search second", List.of("java"));

        SearchResponse firstPage = searchService.search("Cursor Example", 1, null, null, null);
        assertThat(firstPage.items()).hasSize(1);
        assertThat(firstPage.nextAfter()).isNotBlank();
        assertThat(firstPage.hasMore()).isTrue();

        SearchResponse secondPage = searchService.search("Cursor Example", 1, null, firstPage.nextAfter(), null);
        assertThat(secondPage.items()).hasSize(1);
        assertThat(secondPage.items().getFirst().title()).isNotEqualTo(firstPage.items().getFirst().title());
    }

    @Test
    void shouldKeepCursorStableAfterNewHeadDocumentInserted() {
        long firstId = publish("Stable Cursor First", "cursor stable first", List.of("java"));
        publish("Stable Cursor Second", "cursor stable second", List.of("java"));

        SearchResponse firstPage = searchService.search("Stable Cursor", 1, null, null, null);
        assertThat(firstPage.items()).hasSize(1);
        assertThat(firstPage.nextAfter()).isNotBlank();

        publish("Stable Cursor Latest", "cursor stable latest", List.of("java"));

        SearchResponse secondPage = searchService.search("Stable Cursor", 1, null, firstPage.nextAfter(), null);
        assertThat(secondPage.items()).hasSize(1);
        assertThat(secondPage.items().getFirst().id()).isEqualTo(String.valueOf(firstId));
        assertThat(secondPage.items().getFirst().id()).isNotEqualTo(firstPage.items().getFirst().id());
    }

    @Test
    void shouldSearchPublishedKnowpostByRealContentBody() throws Exception {
        Path contentFile = Files.createTempFile("mindshare-search-body", ".md");
        Files.writeString(contentFile, "这是一段只存在于正文里的关键字：向量检索种子词");

        long id = knowPostService.createDraft(8301L);
        knowPostService.confirmContent(8301L, id, "posts/" + id + "/content.md", "etag-1", 128L, "sha-1");
        jdbcTemplate.update("UPDATE know_posts SET content_url = ? WHERE id = ?", contentFile.toUri().toString(), id);
        knowPostService.updateMetadata(
                8301L,
                id,
                "Body Search Example",
                1L,
                List.of("java", "search"),
                List.of("https://cdn.example.com/cover.png"),
                "public",
                false,
                "这里没有目标关键词"
        );
        knowPostService.publish(8301L, id);

        SearchResponse response = searchService.search("向量检索种子词", 20, null, null, null);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().id()).isEqualTo(String.valueOf(id));
        assertThat(response.items().getFirst().description()).contains("向量检索种子词");
    }

    @Test
    void shouldReturnRealInteractionStateInSearchResults() {
        long id = publish("Counter Search Example", "counter aware search", List.of("java", "search"));

        assertThat(counterService.like("knowpost", String.valueOf(id), 8301L)).isTrue();
        assertThat(counterService.fav("knowpost", String.valueOf(id), 8301L)).isTrue();

        SearchResponse response = searchService.search("Counter Search", 20, null, null, 8301L);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().likeCount()).isEqualTo(1L);
        assertThat(response.items().getFirst().favoriteCount()).isEqualTo(1L);
        assertThat(response.items().getFirst().liked()).isTrue();
        assertThat(response.items().getFirst().faved()).isTrue();
    }

    private long publish(String title, String description, List<String> tags) {
        long id = knowPostService.createDraft(8301L);
        knowPostService.confirmContent(8301L, id, "posts/" + id + "/content.md", "etag-1", 128L, "sha-1");
        knowPostService.updateMetadata(
                8301L,
                id,
                title,
                1L,
                tags,
                List.of("https://cdn.example.com/cover.png"),
                "public",
                false,
                description
        );
        knowPostService.publish(8301L, id);
        return id;
    }
}
