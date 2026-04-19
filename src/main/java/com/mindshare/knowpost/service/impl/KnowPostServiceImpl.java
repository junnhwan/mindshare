package com.mindshare.knowpost.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.mindshare.cache.config.CacheProperties;
import com.mindshare.common.exception.BusinessException;
import com.mindshare.common.exception.ErrorCode;
import com.mindshare.knowpost.api.dto.DescriptionSuggestResponse;
import com.mindshare.knowpost.api.dto.FeedItemResponse;
import com.mindshare.knowpost.api.dto.FeedPageResponse;
import com.mindshare.knowpost.api.dto.KnowPostDetailResponse;
import com.mindshare.knowpost.id.SnowflakeIdGenerator;
import com.mindshare.knowpost.mapper.KnowPostMapper;
import com.mindshare.knowpost.model.KnowPost;
import com.mindshare.knowpost.model.KnowPostDetailRow;
import com.mindshare.knowpost.model.KnowPostFeedRow;
import com.mindshare.knowpost.service.KnowPostFeedService;
import com.mindshare.knowpost.service.KnowPostService;
import com.mindshare.storage.OssStorageService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Service
@Profile("!bootstrap-test")
public class KnowPostServiceImpl implements KnowPostService {

    private final KnowPostMapper knowPostMapper;
    private final SnowflakeIdGenerator idGenerator;
    private final ObjectMapper objectMapper;
    private final OssStorageService ossStorageService;
    private final KnowPostFeedService knowPostFeedService;
    private final CacheProperties cacheProperties;
    private final StringRedisTemplate stringRedisTemplate;
    private final Cache<String, KnowPostDetailResponse> knowPostDetailCache;

    public KnowPostServiceImpl(
            KnowPostMapper knowPostMapper,
            SnowflakeIdGenerator idGenerator,
            ObjectMapper objectMapper,
            OssStorageService ossStorageService,
            KnowPostFeedService knowPostFeedService,
            CacheProperties cacheProperties,
            StringRedisTemplate stringRedisTemplate,
            @Qualifier("knowPostDetailCache") Cache<String, KnowPostDetailResponse> knowPostDetailCache
    ) {
        this.knowPostMapper = knowPostMapper;
        this.idGenerator = idGenerator;
        this.objectMapper = objectMapper;
        this.ossStorageService = ossStorageService;
        this.knowPostFeedService = knowPostFeedService;
        this.cacheProperties = cacheProperties;
        this.stringRedisTemplate = stringRedisTemplate;
        this.knowPostDetailCache = knowPostDetailCache;
    }

    @Override
    @Transactional
    public long createDraft(long creatorId) {
        long id = idGenerator.nextId();
        Instant now = Instant.now();
        KnowPost post = new KnowPost();
        post.setId(id);
        post.setCreatorId(creatorId);
        post.setStatus("draft");
        post.setType("image_text");
        post.setVisible("public");
        post.setIsTop(false);
        post.setCreateTime(now);
        post.setUpdateTime(now);
        knowPostMapper.insertDraft(post);
        return id;
    }

    @Override
    @Transactional
    public void confirmContent(long creatorId, long id, String objectKey, String etag, Long size, String sha256) {
        KnowPost post = new KnowPost();
        post.setId(id);
        post.setCreatorId(creatorId);
        post.setContentObjectKey(objectKey);
        post.setContentEtag(etag);
        post.setContentSize(size);
        post.setContentSha256(sha256);
        post.setContentUrl(ossStorageService.publicUrl(objectKey));
        post.setUpdateTime(Instant.now());
        int updated = knowPostMapper.updateContent(post);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "draft not found or no permission");
        }
        invalidateCaches(creatorId, id);
    }

    @Override
    @Transactional
    public void updateMetadata(long creatorId,
                               long id,
                               String title,
                               Long tagId,
                               List<String> tags,
                               List<String> imgUrls,
                               String visible,
                               Boolean isTop,
                               String description) {
        KnowPost post = new KnowPost();
        post.setId(id);
        post.setCreatorId(creatorId);
        post.setTitle(title);
        post.setTagId(tagId);
        post.setTags(toJsonOrNull(tags));
        post.setImgUrls(toJsonOrNull(imgUrls));
        post.setVisible(visible);
        post.setIsTop(isTop);
        post.setDescription(description);
        post.setType("image_text");
        post.setUpdateTime(Instant.now());
        int updated = knowPostMapper.updateMetadata(post);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "draft not found or no permission");
        }
        invalidateCaches(creatorId, id);
    }

    @Override
    @Transactional
    public void publish(long creatorId, long id) {
        int updated = knowPostMapper.publish(id, creatorId);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "draft not found or no permission");
        }
        invalidateCaches(creatorId, id);
    }

    @Override
    @Transactional
    public void updateTop(long creatorId, long id, boolean isTop) {
        int updated = knowPostMapper.updateTop(id, creatorId, isTop);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "draft not found or no permission");
        }
        invalidateCaches(creatorId, id);
    }

    @Override
    @Transactional
    public void updateVisibility(long creatorId, long id, String visible) {
        if (!isValidVisible(visible)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "invalid visible value");
        }
        int updated = knowPostMapper.updateVisibility(id, creatorId, visible);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "draft not found or no permission");
        }
        invalidateCaches(creatorId, id);
    }

    @Override
    @Transactional
    public void delete(long creatorId, long id) {
        int updated = knowPostMapper.softDelete(id, creatorId);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "draft not found or no permission");
        }
        invalidateCaches(creatorId, id);
    }

    @Override
    @Transactional(readOnly = true)
    public KnowPostDetailResponse getDetail(long id, Long currentUserIdNullable) {
        String cacheKey = detailCacheKey(id);
        KnowPostDetailResponse cached = knowPostDetailCache.getIfPresent(cacheKey);
        if (cached != null) {
            return cached;
        }
        KnowPostDetailResponse redisCached = readDetailRedis(cacheKey);
        if (redisCached != null) {
            knowPostDetailCache.put(cacheKey, redisCached);
            return redisCached;
        }

        KnowPostDetailRow row = knowPostMapper.findDetailById(id);
        if (row == null || "deleted".equals(row.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "content not found");
        }
        boolean isPublic = "published".equals(row.getStatus()) && "public".equals(row.getVisible());
        boolean isOwner = currentUserIdNullable != null
                && row.getCreatorId() != null
                && currentUserIdNullable.equals(row.getCreatorId());
        if (!isPublic && !isOwner) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "no permission to view");
        }
        KnowPostDetailResponse response = new KnowPostDetailResponse(
                String.valueOf(row.getId()),
                row.getTitle(),
                row.getDescription(),
                row.getContentUrl(),
                parseStringArray(row.getImgUrls()),
                parseStringArray(row.getTags()),
                String.valueOf(row.getCreatorId()),
                row.getAuthorAvatar(),
                row.getAuthorNickname(),
                row.getAuthorTagJson(),
                0L,
                0L,
                null,
                null,
                row.getIsTop(),
                row.getVisible(),
                row.getType(),
                row.getPublishTime()
        );
        knowPostDetailCache.put(cacheKey, response);
        writeDetailRedis(cacheKey, response);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public FeedPageResponse getPublicFeed(int page, int size) {
        return knowPostFeedService.getPublicFeed(page, size, null);
    }

    @Override
    @Transactional(readOnly = true)
    public FeedPageResponse getMyPublished(long creatorId, int page, int size) {
        return knowPostFeedService.getMyPublished(creatorId, page, size);
    }

    @Override
    public DescriptionSuggestResponse suggestDescription(String content) {
        String normalized = content == null ? "" : content.trim().replaceAll("\\s+", " ");
        if (normalized.length() <= 120) {
            return new DescriptionSuggestResponse(normalized);
        }
        return new DescriptionSuggestResponse(normalized.substring(0, 117) + "...");
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

    private int normalizeSize(int size) {
        return Math.min(Math.max(size, 1), 50);
    }

    private int normalizePage(int page) {
        return Math.max(page, 1);
    }

    private String toJsonOrNull(List<String> values) {
        if (values == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "failed to serialize json");
        }
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

    private boolean isValidVisible(String visible) {
        return switch (visible) {
            case "public", "followers", "school", "private", "unlisted" -> true;
            default -> false;
        };
    }

    private void invalidateCaches(long creatorId, long postId) {
        invalidateDetailCache(postId);
        knowPostFeedService.invalidatePublicFeed();
        knowPostFeedService.invalidateMyPublished(creatorId);
    }

    private void invalidateDetailCache(long postId) {
        String key = detailCacheKey(postId);
        knowPostDetailCache.invalidate(key);
        if (!cacheProperties.isRedisEnabled()) {
            return;
        }
        try {
            stringRedisTemplate.delete(key);
        } catch (Exception ignored) {
        }
    }

    private String detailCacheKey(long id) {
        return "knowpost:detail:" + id;
    }

    private KnowPostDetailResponse readDetailRedis(String key) {
        if (!cacheProperties.isRedisEnabled()) {
            return null;
        }
        try {
            String json = stringRedisTemplate.opsForValue().get(key);
            if (json == null || json.isBlank()) {
                return null;
            }
            return objectMapper.readValue(json, KnowPostDetailResponse.class);
        } catch (Exception exception) {
            return null;
        }
    }

    private void writeDetailRedis(String key, KnowPostDetailResponse response) {
        if (!cacheProperties.isRedisEnabled()) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(response);
            stringRedisTemplate.opsForValue().set(key, json, cacheProperties.getDetailTtl());
        } catch (Exception ignored) {
        }
    }
}
