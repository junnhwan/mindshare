package com.mindshare.counter.service.impl;

import com.mindshare.counter.config.CounterProperties;
import com.mindshare.counter.event.CounterEvent;
import com.mindshare.counter.event.CounterEventProducer;
import com.mindshare.counter.schema.BitmapShard;
import com.mindshare.counter.schema.CounterKeys;
import com.mindshare.counter.schema.CounterSchema;
import com.mindshare.counter.service.CounterService;
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
            local offset = tonumber(ARGV[1])
            local op = ARGV[2]
            
            local prev = redis.call('GETBIT', bmKey, offset)
            if op == 'add' then
              if prev == 1 then return 0 end
              redis.call('SETBIT', bmKey, offset, 1)
            elseif op == 'remove' then
              if prev == 0 then return 0 end
              redis.call('SETBIT', bmKey, offset, 0)
            else
              return -1
            end
            return 1
            """;

    private final StringRedisTemplate redisTemplate;
    private final CounterProperties counterProperties;
    private final CounterEventProducer counterEventProducer;
    private final DefaultRedisScript<Long> toggleScript;
    private final ConcurrentMap<String, Set<Long>> likeFacts = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Set<Long>> favFacts = new ConcurrentHashMap<>();

    public CounterServiceImpl(
            StringRedisTemplate redisTemplate,
            CounterProperties counterProperties,
            CounterEventProducer counterEventProducer
    ) {
        this.redisTemplate = redisTemplate;
        this.counterProperties = counterProperties;
        this.counterEventProducer = counterEventProducer;
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
                publishEvent(entityType, entityId, metric, idx, userId, add ? 1 : -1);
            }
            return changed;
        }
        long chunk = BitmapShard.chunkOf(userId);
        long bit = BitmapShard.bitOf(userId);
        try {
            Long changed = redisTemplate.execute(
                    toggleScript,
                    List.of(CounterKeys.bitmapKey(metric, entityType, entityId, chunk)),
                    String.valueOf(bit),
                    add ? "add" : "remove"
            );
            boolean updated = changed != null && changed == 1L;
            if (updated) {
                publishEvent(entityType, entityId, metric, idx, userId, add ? 1 : -1);
            }
            return updated;
        } catch (Exception exception) {
            boolean changed = toggleInMemory(metric, entityType, entityId, userId, add);
            if (changed) {
                publishEvent(entityType, entityId, metric, idx, userId, add ? 1 : -1);
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

    private void publishEvent(String entityType, String entityId, String metric, int idx, long userId, int delta) {
        counterEventProducer.publish(CounterEvent.of(entityType, entityId, metric, idx, userId, delta));
    }
}
