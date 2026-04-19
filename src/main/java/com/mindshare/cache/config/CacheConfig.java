package com.mindshare.cache.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mindshare.knowpost.api.dto.FeedPageResponse;
import com.mindshare.knowpost.api.dto.KnowPostDetailResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class CacheConfig {

    @Bean(name = "feedPublicCache")
    public Cache<String, FeedPageResponse> feedPublicCache(CacheProperties properties) {
        return Caffeine.newBuilder()
                .maximumSize(properties.getFeedMaximumSize())
                .expireAfterWrite(properties.getPublicFeedTtl())
                .build();
    }

    @Bean(name = "feedMineCache")
    public Cache<String, FeedPageResponse> feedMineCache(CacheProperties properties) {
        return Caffeine.newBuilder()
                .maximumSize(properties.getFeedMaximumSize())
                .expireAfterWrite(properties.getMyFeedTtl())
                .build();
    }

    @Bean(name = "knowPostDetailCache")
    public Cache<String, KnowPostDetailResponse> knowPostDetailCache(CacheProperties properties) {
        return Caffeine.newBuilder()
                .maximumSize(properties.getDetailMaximumSize())
                .expireAfterWrite(properties.getDetailTtl())
                .build();
    }
}
