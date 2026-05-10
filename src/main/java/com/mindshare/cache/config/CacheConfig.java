package com.mindshare.cache.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mindshare.knowpost.api.dto.FeedPageResponse;
import com.mindshare.knowpost.api.dto.KnowPostDetailResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration(proxyBeanMethods = false)
public class CacheConfig {

    @Bean(name = "feedPublicCache")
    public Cache<String, FeedPageResponse> feedPublicCache(CacheProperties props) {
        return Caffeine.newBuilder()
                .maximumSize(props.getL2().getPublicCfg().getMaxSize())
                .expireAfterWrite(Duration.ofSeconds(props.getL2().getPublicCfg().getTtlSeconds()))
                .build();
    }

    @Bean(name = "feedMineCache")
    public Cache<String, FeedPageResponse> feedMineCache(CacheProperties props) {
        return Caffeine.newBuilder()
                .maximumSize(props.getL2().getMineCfg().getMaxSize())
                .expireAfterWrite(Duration.ofSeconds(props.getL2().getMineCfg().getTtlSeconds()))
                .build();
    }

    @Bean(name = "knowPostDetailCache")
    public Cache<String, KnowPostDetailResponse> knowPostDetailCache(CacheProperties props) {
        return Caffeine.newBuilder()
                .maximumSize(props.getL2().getDetailCfg().getMaxSize())
                .expireAfterWrite(Duration.ofSeconds(props.getL2().getDetailCfg().getTtlSeconds()))
                .build();
    }
}
