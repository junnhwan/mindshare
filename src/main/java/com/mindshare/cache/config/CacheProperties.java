package com.mindshare.cache.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Data
@Component
@ConfigurationProperties(prefix = "mindshare.cache")
public class CacheProperties {

    private boolean redisEnabled = true;
    private long feedMaximumSize = 128;
    private long detailMaximumSize = 256;
    private Duration publicFeedTtl = Duration.ofMinutes(5);
    private Duration myFeedTtl = Duration.ofMinutes(3);
    private Duration detailTtl = Duration.ofMinutes(5);
}
