package com.mindshare.counter.event;

import com.mindshare.counter.service.CounterService;
import com.mindshare.knowpost.service.KnowPostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
class CounterEventFlowTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private KnowPostService knowPostService;

    @Autowired
    private CounterService counterService;

    @SpyBean
    private CounterAggregationConsumer counterAggregationConsumer;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM know_posts");
        jdbcTemplate.update("DELETE FROM login_logs");
        jdbcTemplate.update("DELETE FROM users");
        jdbcTemplate.update("""
                INSERT INTO users (id, email, password_hash, nickname, avatar, tags_json, create_time, update_time)
                VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP())
                """, 9401L, "author@example.com", "hash", "Author", "https://cdn.example.com/author.png", "[\"java\"]");
        jdbcTemplate.update("""
                INSERT INTO users (id, email, password_hash, nickname, avatar, tags_json, create_time, update_time)
                VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP())
                """, 9402L, "reader@example.com", "hash", "Reader", "https://cdn.example.com/reader.png", "[\"search\"]");
    }

    @Test
    void shouldPublishCounterEventToAggregationConsumer() {
        long id = publish("Event Counter Example");

        assertThat(counterService.like("knowpost", String.valueOf(id), 9402L)).isTrue();

        ArgumentCaptor<CounterEvent> captor = ArgumentCaptor.forClass(CounterEvent.class);
        verify(counterAggregationConsumer, atLeastOnce()).onEvent(captor.capture());

        assertThat(captor.getAllValues()).anySatisfy(event -> {
            assertThat(event.entityType()).isEqualTo("knowpost");
            assertThat(event.entityId()).isEqualTo(String.valueOf(id));
            assertThat(event.metric()).isEqualTo("like");
            assertThat(event.delta()).isEqualTo(1);
        });
    }

    private long publish(String title) {
        long id = knowPostService.createDraft(9401L);
        knowPostService.confirmContent(9401L, id, "posts/" + id + "/content.md", "etag-1", 128L, "sha-1");
        knowPostService.updateMetadata(
                9401L,
                id,
                title,
                1L,
                List.of("java"),
                List.of("https://cdn.example.com/cover.png"),
                "public",
                false,
                "published"
        );
        knowPostService.publish(9401L, id);
        return id;
    }
}
