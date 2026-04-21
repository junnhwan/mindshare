package com.mindshare.counter.service.impl;

import com.mindshare.counter.config.CounterProperties;
import com.mindshare.counter.schema.BitmapShard;
import com.mindshare.counter.schema.CounterKeys;
import com.mindshare.counter.schema.CounterSchema;
import com.mindshare.counter.service.CounterService;
import com.mindshare.counter.service.UserCounterService;
import com.mindshare.knowpost.mapper.KnowPostMapper;
import com.mindshare.knowpost.model.KnowPost;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@Profile("!bootstrap-test")
public class CounterServiceImpl implements CounterService {

    private static final String TOGGLE_LUA = """
            local bmKey = KEYS[1]
            local sdsKey = KEYS[2]
            local offset = tonumber(ARGV[1])
            local op = ARGV[2]
            local idx = tonumber(ARGV[3])
            local schemaLen = tonumber(ARGV[4])
            local fieldSize = tonumber(ARGV[5])
            
            local function read32be(s, off)
              local b1, b2, b3, b4 = string.byte(s, off + 1, off + 4)
              return ((b1 or 0) * 16777216) + ((b2 or 0) * 65536) + ((b3 or 0) * 256) + (b4 or 0)
            end
            
            local function write32be(n)
              local value = math.max(0, n)
              local b1 = math.floor(value / 16777216) % 256
              local b2 = math.floor(value / 65536) % 256
              local b3 = math.floor(value / 256) % 256
              local b4 = value % 256
              return string.char(b1, b2, b3, b4)
            end
            
            local prev = redis.call('GETBIT', bmKey, offset)
            local delta = 0
            if op == 'add' then
              if prev == 1 then return 0 end
              redis.call('SETBIT', bmKey, offset, 1)
              delta = 1
            elseif op == 'remove' then
              if prev == 0 then return 0 end
              redis.call('SETBIT', bmKey, offset, 0)
              delta = -1
            else
              return -1
            end
            
            local len = schemaLen * fieldSize
            local sds = redis.call('GET', sdsKey)
            if not sds then
              sds = string.rep(string.char(0), len)
            end
            
            local off = idx * fieldSize
            local current = read32be(sds, off)
            local next = current + delta
            if next < 0 then next = 0 end
            local seg = write32be(next)
            sds = string.sub(sds, 1, off) .. seg .. string.sub(sds, off + fieldSize + 1)
            redis.call('SET', sdsKey, sds)
            return 1
            """;

    private final StringRedisTemplate redisTemplate;
    private final CounterProperties counterProperties;
    private final KnowPostMapper knowPostMapper;
    private final ObjectProvider<UserCounterService> userCounterServiceProvider;
    private final DefaultRedisScript<Long> toggleScript;
    private final ConcurrentMap<String, Set<Long>> likeFacts = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Set<Long>> favFacts = new ConcurrentHashMap<>();

    public CounterServiceImpl(
            StringRedisTemplate redisTemplate,
            CounterProperties counterProperties,
            KnowPostMapper knowPostMapper,
            ObjectProvider<UserCounterService> userCounterServiceProvider
    ) {
        this.redisTemplate = redisTemplate;
        this.counterProperties = counterProperties;
        this.knowPostMapper = knowPostMapper;
        this.userCounterServiceProvider = userCounterServiceProvider;
        this.toggleScript = new DefaultRedisScript<>();
        this.toggleScript.setResultType(Long.class);
        this.toggleScript.setScriptText(TOGGLE_LUA);
    }

    @Override
    public boolean like(String entityType, String entityId, long userId) {
        return toggle(entityType, entityId, userId, "like", CounterSchema.IDX_LIKE, true);
    }

    @Override
    public boolean unlike(String entityType, String entityId, long userId) {
        return toggle(entityType, entityId, userId, "like", CounterSchema.IDX_LIKE, false);
    }

    @Override
    public boolean fav(String entityType, String entityId, long userId) {
        return toggle(entityType, entityId, userId, "fav", CounterSchema.IDX_FAV, true);
    }

    @Override
    public boolean unfav(String entityType, String entityId, long userId) {
        return toggle(entityType, entityId, userId, "fav", CounterSchema.IDX_FAV, false);
    }

    @Override
    public Map<String, Long> getCounts(String entityType, String entityId, List<String> metrics) {
        List<String> safeMetrics = normalizeMetrics(metrics);
        if (!isRedisEnabled()) {
            return getCountsInMemory(entityType, entityId, safeMetrics);
        }
        try {
            byte[] raw = redisTemplate.execute((RedisCallback<byte[]>) connection ->
                    connection.stringCommands().get(CounterKeys.sdsKey(entityType, entityId).getBytes(StandardCharsets.UTF_8)));
            return decodeCounts(raw, safeMetrics);
        } catch (Exception exception) {
            return getCountsInMemory(entityType, entityId, safeMetrics);
        }
    }

    @Override
    public Map<String, Map<String, Long>> getCountsBatch(String entityType, List<String> entityIds, List<String> metrics) {
        Map<String, Map<String, Long>> result = new LinkedHashMap<>();
        List<String> safeMetrics = normalizeMetrics(metrics);
        if (entityIds == null || entityIds.isEmpty()) {
            return result;
        }
        if (!isRedisEnabled()) {
            for (String entityId : entityIds) {
                result.put(entityId, getCountsInMemory(entityType, entityId, safeMetrics));
            }
            return result;
        }
        try {
            List<String> keys = entityIds.stream()
                    .map(entityId -> CounterKeys.sdsKey(entityType, entityId))
                    .toList();
            List<Object> raws = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                for (String key : keys) {
                    connection.stringCommands().get(key.getBytes(StandardCharsets.UTF_8));
                }
                return null;
            });
            for (int index = 0; index < entityIds.size(); index++) {
                byte[] raw = index < raws.size() && raws.get(index) instanceof byte[] bytes ? bytes : null;
                result.put(entityIds.get(index), decodeCounts(raw, safeMetrics));
            }
            return result;
        } catch (Exception exception) {
            for (String entityId : entityIds) {
                result.put(entityId, getCountsInMemory(entityType, entityId, safeMetrics));
            }
            return result;
        }
    }

    @Override
    public boolean isLiked(String entityType, String entityId, long userId) {
        return isSet("like", entityType, entityId, userId);
    }

    @Override
    public boolean isFaved(String entityType, String entityId, long userId) {
        return isSet("fav", entityType, entityId, userId);
    }

    private boolean toggle(String entityType, String entityId, long userId, String metric, int idx, boolean add) {
        if (!isRedisEnabled()) {
            boolean changed = toggleInMemory(metric, entityType, entityId, userId, add);
            if (changed) {
                updateAuthorCounters(metric, entityId, add);
            }
            return changed;
        }
        long chunk = BitmapShard.chunkOf(userId);
        long bit = BitmapShard.bitOf(userId);
        try {
            Long changed = redisTemplate.execute(
                    toggleScript,
                    List.of(
                            CounterKeys.bitmapKey(metric, entityType, entityId, chunk),
                            CounterKeys.sdsKey(entityType, entityId)
                    ),
                    String.valueOf(bit),
                    add ? "add" : "remove",
                    String.valueOf(idx),
                    String.valueOf(CounterSchema.SCHEMA_LEN),
                    String.valueOf(CounterSchema.FIELD_SIZE)
            );
            boolean updated = changed != null && changed == 1L;
            if (updated) {
                updateAuthorCounters(metric, entityId, add);
            }
            return updated;
        } catch (Exception exception) {
            boolean changed = toggleInMemory(metric, entityType, entityId, userId, add);
            if (changed) {
                updateAuthorCounters(metric, entityId, add);
            }
            return changed;
        }
    }

    private boolean toggleInMemory(String metric, String entityType, String entityId, long userId, boolean add) {
        Set<Long> users = metricFacts(metric).computeIfAbsent(entityKey(entityType, entityId), ignored -> ConcurrentHashMap.newKeySet());
        if (add) {
            return users.add(userId);
        }
        return users.remove(userId);
    }

    private boolean isSet(String metric, String entityType, String entityId, long userId) {
        if (!isRedisEnabled()) {
            return metricFacts(metric).getOrDefault(entityKey(entityType, entityId), Set.of()).contains(userId);
        }
        long chunk = BitmapShard.chunkOf(userId);
        long bit = BitmapShard.bitOf(userId);
        try {
            Boolean value = redisTemplate.execute((RedisCallback<Boolean>) connection ->
                    connection.stringCommands().getBit(
                            CounterKeys.bitmapKey(metric, entityType, entityId, chunk).getBytes(StandardCharsets.UTF_8),
                            bit
                    ));
            return Boolean.TRUE.equals(value);
        } catch (Exception exception) {
            return metricFacts(metric).getOrDefault(entityKey(entityType, entityId), Set.of()).contains(userId);
        }
    }

    private Map<String, Long> getCountsInMemory(String entityType, String entityId, List<String> metrics) {
        Map<String, Long> counts = new LinkedHashMap<>();
        String key = entityKey(entityType, entityId);
        for (String metric : metrics) {
            long value = switch (metric) {
                case "like" -> likeFacts.getOrDefault(key, Set.of()).size();
                case "fav" -> favFacts.getOrDefault(key, Set.of()).size();
                default -> 0L;
            };
            counts.put(metric, value);
        }
        return counts;
    }

    private Map<String, Long> decodeCounts(byte[] raw, List<String> metrics) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String metric : metrics) {
            Integer idx = CounterSchema.NAME_TO_IDX.get(metric);
            if (idx == null) {
                continue;
            }
            long value = raw == null ? 0L : readInt32Be(raw, idx * CounterSchema.FIELD_SIZE);
            counts.put(metric, value);
        }
        return counts;
    }

    private long readInt32Be(byte[] raw, int offset) {
        if (raw.length < offset + CounterSchema.FIELD_SIZE) {
            return 0L;
        }
        long value = 0L;
        for (int index = 0; index < CounterSchema.FIELD_SIZE; index++) {
            value = (value << 8) | (raw[offset + index] & 0xFFL);
        }
        return value;
    }

    private List<String> normalizeMetrics(List<String> metrics) {
        if (metrics == null || metrics.isEmpty()) {
            return new ArrayList<>(CounterSchema.SUPPORTED_METRICS);
        }
        return metrics.stream()
                .filter(CounterSchema.SUPPORTED_METRICS::contains)
                .distinct()
                .toList();
    }

    private ConcurrentMap<String, Set<Long>> metricFacts(String metric) {
        return "fav".equals(metric) ? favFacts : likeFacts;
    }

    private String entityKey(String entityType, String entityId) {
        return entityType + ":" + entityId;
    }

    private boolean isRedisEnabled() {
        return counterProperties.isRedisEnabled();
    }

    private void updateAuthorCounters(String metric, String entityId, boolean add) {
        UserCounterService userCounterService = userCounterServiceProvider.getIfAvailable();
        if (userCounterService == null) {
            return;
        }
        Long knowPostId = parseKnowPostId(entityId);
        if (knowPostId == null) {
            return;
        }
        KnowPost knowPost = knowPostMapper.findById(knowPostId);
        if (knowPost == null || knowPost.getCreatorId() == null) {
            return;
        }
        int delta = add ? 1 : -1;
        if ("like".equals(metric)) {
            userCounterService.incrementLikesReceived(knowPost.getCreatorId(), delta);
        } else if ("fav".equals(metric)) {
            userCounterService.incrementFavsReceived(knowPost.getCreatorId(), delta);
        }
    }

    private Long parseKnowPostId(String entityId) {
        try {
            return Long.parseLong(entityId);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
