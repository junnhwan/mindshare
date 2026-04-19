package com.mindshare.cache.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "mindshare.cache")
public class CacheProperties {

    private boolean redisEnabled = true;
    private long feedMaximumSize = 128;
    private long detailMaximumSize = 256;
    private Duration publicFeedTtl = Duration.ofMinutes(5);
    private Duration myFeedTtl = Duration.ofMinutes(3);
    private Duration detailTtl = Duration.ofMinutes(5);

    public boolean isRedisEnabled() {
        return redisEnabled;
    }

    public void setRedisEnabled(boolean redisEnabled) {
        this.redisEnabled = redisEnabled;
    }

    public long getFeedMaximumSize() {
        return feedMaximumSize;
    }

    public void setFeedMaximumSize(long feedMaximumSize) {
        this.feedMaximumSize = feedMaximumSize;
    }

    public long getDetailMaximumSize() {
        return detailMaximumSize;
    }

    public void setDetailMaximumSize(long detailMaximumSize) {
        this.detailMaximumSize = detailMaximumSize;
    }

    public Duration getPublicFeedTtl() {
        return publicFeedTtl;
    }

    public void setPublicFeedTtl(Duration publicFeedTtl) {
        this.publicFeedTtl = publicFeedTtl;
    }

    public Duration getMyFeedTtl() {
        return myFeedTtl;
    }

    public void setMyFeedTtl(Duration myFeedTtl) {
        this.myFeedTtl = myFeedTtl;
    }

    public Duration getDetailTtl() {
        return detailTtl;
    }

    public void setDetailTtl(Duration detailTtl) {
        this.detailTtl = detailTtl;
    }
}
