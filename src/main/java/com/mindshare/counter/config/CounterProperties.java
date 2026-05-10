package com.mindshare.counter.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "mindshare.counter")
public class CounterProperties {

    private boolean redisEnabled = true;
    private Rebuild rebuild = new Rebuild();

    @Data
    public static class Rebuild {
        private long lockTtlMs = 5000;
        private int ratePermits = 3;
        private int rateWindowSeconds = 10;
        private long backoffBaseMs = 500;
        private long backoffMaxMs = 30000;
    }
}
