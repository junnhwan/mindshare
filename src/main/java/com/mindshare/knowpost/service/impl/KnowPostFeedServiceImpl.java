package com.mindshare.knowpost.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.mindshare.cache.config.CacheProperties;
import com.mindshare.knowpost.api.dto.FeedItemResponse;
import com.mindshare.knowpost.api.dto.FeedPageResponse;
import com.mindshare.knowpost.mapper.KnowPostMapper;
import com.mindshare.knowpost.model.KnowPostFeedRow;
import com.mindshare.knowpost.service.KnowPostFeedService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Profile("!bootstrap-test")
public class KnowPostFeedServiceImpl implements KnowPostFeedService {

    private final KnowPostMapper knowPostMapper;
    private final ObjectMapper objectMapper;
    private final CacheProperties cacheProperties;
    private final StringRedisTemplate stringRedisTemplate;
    private final Cache<String, FeedPageResponse> feedPublicCache;
    private final Cache<String, FeedPageResponse> feedMineCache;

    public KnowPostFeedServiceImpl(
            KnowPostMapper knowPostMapper,
            ObjectMapper objectMapper,
            CacheProperties cacheProperties,
            StringRedisTemplate stringRedisTemplate,
            @Qualifier("feedPublicCache") Cache<String, FeedPageResponse> feedPublicCache,
            @Qualifier("feedMineCache") Cache<String, FeedPageResponse> feedMineCache
    ) {
        this.knowPostMapper = knowPostMapper;
        this.objectMapper = objectMapper;
        this.cacheProperties = cacheProperties;
        this.stringRedisTemplate = stringRedisTemplate;
        this.feedPublicCache = feedPublicCache;
        this.feedMineCache = feedMineCache;
    }

    @Override
    public FeedPageResponse getPublicFeed(int page, int size, Long currentUserIdNullable) {
        int safePage = normalizePage(page);
        int safeSize = normalizeSize(size);
        String key = "feed:public:" + safePage + ":" + safeSize;

        FeedPageResponse local = feedPublicCache.getIfPresent(key);
        if (local != null) {
            return local;
        }

        FeedPageResponse cached = readRedis(key);
        if (cached != null) {
            feedPublicCache.put(key, cached);
            return cached;
        }

        int offset = (safePage - 1) * safeSize;
        List<KnowPostFeedRow> rows = knowPostMapper.listFeedPublic(safeSize + 1, offset);
        boolean hasMore = rows.size() > safeSize;
        if (hasMore) {
            rows = rows.subList(0, safeSize);
        }
        FeedPageResponse response = new FeedPageResponse(mapRows(rows), safePage, safeSize, hasMore);
        feedPublicCache.put(key, response);
        writeRedis(key, response, cacheProperties.getPublicFeedTtl().getSeconds());
        return response;
    }

    @Override
    public FeedPageResponse getMyPublished(long creatorId, int page, int size) {
        int safePage = normalizePage(page);
        int safeSize = normalizeSize(size);
        String key = "feed:mine:" + creatorId + ":" + safePage + ":" + safeSize;

        FeedPageResponse local = feedMineCache.getIfPresent(key);
        if (local != null) {
            return local;
        }

        FeedPageResponse cached = readRedis(key);
        if (cached != null) {
            feedMineCache.put(key, cached);
            return cached;
        }

        int offset = (safePage - 1) * safeSize;
        List<KnowPostFeedRow> rows = knowPostMapper.listMyPublished(creatorId, safeSize + 1, offset);
        boolean hasMore = rows.size() > safeSize;
        if (hasMore) {
            rows = rows.subList(0, safeSize);
        }
        FeedPageResponse response = new FeedPageResponse(mapRows(rows), safePage, safeSize, hasMore);
        feedMineCache.put(key, response);
        writeRedis(key, response, cacheProperties.getMyFeedTtl().getSeconds());
        return response;
    }

    @Override
    public void invalidatePublicFeed() {
        feedPublicCache.invalidateAll();
        deleteRedisByPrefix("feed:public:");
    }

    @Override
    public void invalidateMyPublished(long creatorId) {
        List<String> localKeys = new ArrayList<>(feedMineCache.asMap().keySet());
        for (String key : localKeys) {
            if (key.startsWith("feed:mine:" + creatorId + ":")) {
                feedMineCache.invalidate(key);
            }
        }
        deleteRedisByPrefix("feed:mine:" + creatorId + ":");
    }

    private List<FeedItemResponse> mapRows(List<KnowPostFeedRow> rows) {
        return rows.stream()
                .map(row -> {
                    List<String> images = parseStringArray(row.getImgUrls());
                    return new FeedItemResponse(
                            String.valueOf(row.getId()),
                            row.getTitle(),
                            row.getDescription(),
                            images.isEmpty() ? null : images.get(0),
                            parseStringArray(row.getTags()),
                            row.getAuthorAvatar(),
                            row.getAuthorNickname(),
                            row.getAuthorTagJson(),
                            0L,
                            0L,
                            null,
                            null,
                            row.getIsTop()
                    );
                })
                .toList();
    }

    private List<String> parseStringArray(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception exception) {
            return Collections.emptyList();
        }
    }

    private int normalizePage(int page) {
        return Math.max(page, 1);
    }

    private int normalizeSize(int size) {
        return Math.min(Math.max(size, 1), 50);
    }

    private FeedPageResponse readRedis(String key) {
        if (!cacheProperties.isRedisEnabled()) {
            return null;
        }
        try {
            String json = stringRedisTemplate.opsForValue().get(key);
            if (json == null || json.isBlank()) {
                return null;
            }
            return objectMapper.readValue(json, FeedPageResponse.class);
        } catch (Exception exception) {
            return null;
        }
    }

    private void writeRedis(String key, FeedPageResponse response, long ttlSeconds) {
        if (!cacheProperties.isRedisEnabled()) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(response);
            stringRedisTemplate.opsForValue().set(key, json, java.time.Duration.ofSeconds(ttlSeconds));
        } catch (Exception ignored) {
        }
    }

    private void deleteRedisByPrefix(String prefix) {
        if (!cacheProperties.isRedisEnabled()) {
            return;
        }
        try {
            var keys = stringRedisTemplate.keys(prefix + "*");
            if (keys != null && !keys.isEmpty()) {
                stringRedisTemplate.delete(keys);
            }
        } catch (Exception ignored) {
        }
    }
}
