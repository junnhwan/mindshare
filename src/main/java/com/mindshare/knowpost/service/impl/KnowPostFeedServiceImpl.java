package com.mindshare.knowpost.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.mindshare.cache.config.CacheProperties;
import com.mindshare.counter.service.CounterService;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@Profile("!bootstrap-test")
public class KnowPostFeedServiceImpl implements KnowPostFeedService {

    private final KnowPostMapper knowPostMapper;
    private final ObjectMapper objectMapper;
    private final CacheProperties cacheProperties;
    private final StringRedisTemplate stringRedisTemplate;
    private final Cache<String, FeedPageResponse> feedPublicCache;
    private final Cache<String, FeedPageResponse> feedMineCache;
    private final CounterService counterService;
    private final ConcurrentMap<String, Object> publicFeedFlights = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Set<Long>> publicFeedPageItems = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, Set<String>> publicFeedItemPages = new ConcurrentHashMap<>();

    public KnowPostFeedServiceImpl(
            KnowPostMapper knowPostMapper,
            ObjectMapper objectMapper,
            CacheProperties cacheProperties,
            StringRedisTemplate stringRedisTemplate,
            CounterService counterService,
            @Qualifier("feedPublicCache") Cache<String, FeedPageResponse> feedPublicCache,
            @Qualifier("feedMineCache") Cache<String, FeedPageResponse> feedMineCache
    ) {
        this.knowPostMapper = knowPostMapper;
        this.objectMapper = objectMapper;
        this.cacheProperties = cacheProperties;
        this.stringRedisTemplate = stringRedisTemplate;
        this.counterService = counterService;
        this.feedPublicCache = feedPublicCache;
        this.feedMineCache = feedMineCache;
    }

    @Override
    public FeedPageResponse getPublicFeed(int page, int size, Long currentUserIdNullable) {
        int safePage = normalizePage(page);
        int safeSize = normalizeSize(size);
        String key = publicFeedKey(safePage, safeSize);

        FeedPageResponse local = feedPublicCache.getIfPresent(key);
        if (local != null) {
            return enrichResponse(local, currentUserIdNullable);
        }

        FeedPageResponse cached = readRedis(key);
        if (cached != null) {
            cachePublicFeed(key, cached);
            return enrichResponse(cached, currentUserIdNullable);
        }

        Object lock = publicFeedFlights.computeIfAbsent(key, ignored -> new Object());
        synchronized (lock) {
            try {
                FeedPageResponse localAgain = feedPublicCache.getIfPresent(key);
                if (localAgain != null) {
                    return enrichResponse(localAgain, currentUserIdNullable);
                }

                FeedPageResponse redisAgain = readRedis(key);
                if (redisAgain != null) {
                    cachePublicFeed(key, redisAgain);
                    return enrichResponse(redisAgain, currentUserIdNullable);
                }

                int offset = (safePage - 1) * safeSize;
                List<KnowPostFeedRow> rows = knowPostMapper.listFeedPublic(safeSize + 1, offset);
                boolean hasMore = rows.size() > safeSize;
                if (hasMore) {
                    rows = rows.subList(0, safeSize);
                }
                FeedPageResponse response = new FeedPageResponse(mapRows(rows), safePage, safeSize, hasMore);
                cachePublicFeed(key, response);
                return enrichResponse(response, currentUserIdNullable);
            } finally {
                publicFeedFlights.remove(key, lock);
            }
        }
    }

    @Override
    public FeedPageResponse getMyPublished(long creatorId, int page, int size) {
        int safePage = normalizePage(page);
        int safeSize = normalizeSize(size);
        String key = "feed:mine:" + creatorId + ":" + safePage + ":" + safeSize;

        FeedPageResponse local = feedMineCache.getIfPresent(key);
        if (local != null) {
            return enrichResponse(local, creatorId);
        }

        FeedPageResponse cached = readRedis(key);
        if (cached != null) {
            feedMineCache.put(key, cached);
            return enrichResponse(cached, creatorId);
        }

        int offset = (safePage - 1) * safeSize;
        List<KnowPostFeedRow> rows = knowPostMapper.listMyPublished(creatorId, safeSize + 1, offset);
        boolean hasMore = rows.size() > safeSize;
        if (hasMore) {
            rows = rows.subList(0, safeSize);
        }
        FeedPageResponse response = new FeedPageResponse(mapRows(rows), safePage, safeSize, hasMore);
        feedMineCache.put(key, response);
        writeRedis(key, response, cacheProperties.getL2().getMineCfg().getTtlSeconds());
        return enrichResponse(response, creatorId);
    }

    @Override
    public void invalidatePublicFeed() {
        feedPublicCache.invalidateAll();
        publicFeedPageItems.clear();
        publicFeedItemPages.clear();
        deleteRedisByPrefix("feed:public:");
    }

    @Override
    public void invalidatePublicFeedForPost(long postId) {
        Set<String> keysToInvalidate = new HashSet<>();
        keysToInvalidate.addAll(firstPublicPageKeys());
        Set<String> tracked = publicFeedItemPages.get(postId);
        if (tracked != null) {
            keysToInvalidate.addAll(tracked);
        }
        for (String key : keysToInvalidate) {
            invalidatePublicPage(key);
        }
        deleteRedisByPrefix("feed:public:1:");
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

    private String publicFeedKey(int page, int size) {
        return "feed:public:" + page + ":" + size;
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

    private void cachePublicFeed(String key, FeedPageResponse response) {
        feedPublicCache.put(key, response);
        trackPublicPage(key, response.items());
        writeRedis(key, response, cacheProperties.getL2().getPublicCfg().getTtlSeconds());
    }

    private FeedPageResponse enrichResponse(FeedPageResponse response, Long currentUserIdNullable) {
        List<String> entityIds = response.items().stream()
                .map(FeedItemResponse::id)
                .toList();
        Map<String, Map<String, Long>> countsBatch = counterService.getCountsBatch("knowpost", entityIds, List.of("like", "fav"));
        List<FeedItemResponse> enrichedItems = response.items().stream()
                .map(item -> {
                    Map<String, Long> counts = countsBatch.getOrDefault(item.id(), Map.of());
                    Boolean liked = currentUserIdNullable == null
                            ? null
                            : counterService.isLiked("knowpost", item.id(), currentUserIdNullable);
                    Boolean faved = currentUserIdNullable == null
                            ? null
                            : counterService.isFaved("knowpost", item.id(), currentUserIdNullable);
                    return new FeedItemResponse(
                            item.id(),
                            item.title(),
                            item.description(),
                            item.coverImage(),
                            item.tags(),
                            item.authorAvatar(),
                            item.authorNickname(),
                            item.tagJson(),
                            counts.getOrDefault("like", 0L),
                            counts.getOrDefault("fav", 0L),
                            liked,
                            faved,
                            item.isTop()
                    );
                })
                .toList();
        return new FeedPageResponse(enrichedItems, response.page(), response.size(), response.hasMore());
    }

    private void trackPublicPage(String key, List<FeedItemResponse> items) {
        removeTrackedPublicPage(key);
        Set<Long> itemIds = ConcurrentHashMap.newKeySet();
        for (FeedItemResponse item : items) {
            Long itemId = parseId(item.id());
            if (itemId == null) {
                continue;
            }
            itemIds.add(itemId);
            publicFeedItemPages.computeIfAbsent(itemId, ignored -> ConcurrentHashMap.newKeySet()).add(key);
        }
        if (!itemIds.isEmpty()) {
            publicFeedPageItems.put(key, itemIds);
        }
    }

    private void invalidatePublicPage(String key) {
        feedPublicCache.invalidate(key);
        removeTrackedPublicPage(key);
        deleteRedisKey(key);
    }

    private void removeTrackedPublicPage(String key) {
        Set<Long> previousItems = publicFeedPageItems.remove(key);
        if (previousItems == null) {
            return;
        }
        for (Long itemId : previousItems) {
            Set<String> pages = publicFeedItemPages.get(itemId);
            if (pages == null) {
                continue;
            }
            pages.remove(key);
            if (pages.isEmpty()) {
                publicFeedItemPages.remove(itemId, pages);
            }
        }
    }

    private Set<String> firstPublicPageKeys() {
        Set<String> keys = new HashSet<>();
        keys.addAll(feedPublicCache.asMap().keySet().stream()
                .filter(key -> key.startsWith("feed:public:1:"))
                .toList());
        keys.addAll(publicFeedPageItems.keySet().stream()
                .filter(key -> key.startsWith("feed:public:1:"))
                .toList());
        return keys;
    }

    private Long parseId(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private void deleteRedisKey(String key) {
        if (!cacheProperties.isRedisEnabled()) {
            return;
        }
        try {
            stringRedisTemplate.delete(key);
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
