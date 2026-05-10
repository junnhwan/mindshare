package com.mindshare.counter.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Profile("!bootstrap-test")
public class CounterEventProducer {
    private static final Logger log = LoggerFactory.getLogger(CounterEventProducer.class);

    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public CounterEventProducer(
            ApplicationEventPublisher eventPublisher,
            ObjectMapper objectMapper,
            @Autowired(required = false) KafkaTemplate<String, String> kafkaTemplate) {
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(CounterEvent event) {
        if (kafkaTemplate != null) {
            try {
                String payload = objectMapper.writeValueAsString(event);
                kafkaTemplate.send(CounterTopics.EVENTS, payload);
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize counter event", e);
            }
        }
        eventPublisher.publishEvent(event);
    }
}
