package com.mindshare.counter.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("!bootstrap-test")
public class CounterEventProducer {

    private final ApplicationEventPublisher applicationEventPublisher;

    public CounterEventProducer(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public void publish(CounterEvent event) {
        applicationEventPublisher.publishEvent(event);
    }
}
