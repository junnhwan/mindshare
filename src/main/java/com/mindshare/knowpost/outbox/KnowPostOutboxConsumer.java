package com.mindshare.knowpost.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindshare.knowpost.id.SnowflakeIdGenerator;
import com.mindshare.search.index.SearchIndexService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Profile("!bootstrap-test")
public class KnowPostOutboxConsumer {
    private static final Logger log = LoggerFactory.getLogger(KnowPostOutboxConsumer.class);

    private final OutboxMapper outboxMapper;
    private final SearchIndexService searchIndexService;
    private final ObjectMapper objectMapper;
    private final SnowflakeIdGenerator idGenerator;

    public KnowPostOutboxConsumer(OutboxMapper outboxMapper, SearchIndexService searchIndexService,
                                   ObjectMapper objectMapper, SnowflakeIdGenerator idGenerator) {
        this.outboxMapper = outboxMapper;
        this.searchIndexService = searchIndexService;
        this.objectMapper = objectMapper;
        this.idGenerator = idGenerator;
    }

    public void writeKnowPostEvent(String eventType, long postId) {
        OutboxEvent event = OutboxEvent.builder()
                .id(idGenerator.nextId())
                .aggregateType("knowpost")
                .aggregateId(postId)
                .eventType(eventType)
                .payload("{\"entity\":\"knowpost\",\"op\":\"" + eventType + "\",\"id\":" + postId + "}")
                .build();
        outboxMapper.insert(event);
    }

    @Scheduled(fixedDelay = 2000L)
    @Transactional
    public void poll() {
        try {
            List<OutboxEvent> events = outboxMapper.pollUnprocessed(100);
            for (OutboxEvent event : events) {
                try {
                    if ("knowpost".equals(event.getAggregateType())) {
                        if ("upsert".equals(event.getEventType())) {
                            searchIndexService.upsertKnowPost(event.getAggregateId());
                        } else if ("delete".equals(event.getEventType())) {
                            searchIndexService.deleteKnowPost(event.getAggregateId());
                        }
                    }
                    outboxMapper.deleteById(event.getId());
                } catch (Exception e) {
                    log.warn("Failed to process outbox event {}: {}", event.getId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("Outbox poll failed: {}", e.getMessage());
        }
    }
}
