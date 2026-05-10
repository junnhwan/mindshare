package com.mindshare.counter.service.impl;

import com.mindshare.counter.config.CounterProperties;
import com.mindshare.counter.event.CounterEvent;
import com.mindshare.counter.event.CounterEventProducer;
import com.mindshare.counter.schema.BitmapShard;
import com.mindshare.counter.schema.CounterKeys;
import com.mindshare.counter.schema.CounterSchema;
import com.mindshare.counter.service.CounterService;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateType;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
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
    private final RedissonClient redissonClient;
    private final ConcurrentMap<String, Set<Long>> likeFacts = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Set<Long>> favFacts = new ConcurrentHashMap<>();

    public CounterServiceImpl(
            StringRedisTemplate redisTemplate,
            CounterProperties counterProperties,
            CounterEventProducer counterEventProducer,
            @Autowired(required = false) RedissonClient redissonClient) {
        this.redisTemplate = redisTemplate;
        this.counterProperties = counterProperties;
        this.counterEventProducer = counterEventProducer;
        this.redissonClient = redissonClient;
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
        String sdsKey = CounterKeys.sdsKey(entityType, entityId);
        int expectedLen = CounterSchema.SCHEMA_LEN * CounterSchema.FIELD_SIZE;

        if (isRedisEnabled()) {
            byte[] raw = getRaw(sdsKey);
            if (raw != null && raw.length == expectedLen) {
                return decodeCounts(raw, safeMetrics);
            }
            Map<String, Long> result = new LinkedHashMap<>();
            triggerRebuild(entityType, entityId, safeMetrics, result, sdsKey, expectedLen);
            return result;
        }
        return getCountsInMemory(entityType, entityId, safeMetrics);
    }

    @Override
    public Map<String, Map<String, Long>> getCountsBatch(String entityType, List<String> entityIds, List<String> metrics) {
        Map<String, Map<String, Long>> result = new LinkedHashMap<>();
        List<String> safeMetrics = normalizeMetrics(metrics);
        if (entityIds == null || entityIds.isEmpty()) return result;

        if (isRedisEnabled()) {
            List<String> keys = entityIds.stream()
                    .map(eid -> CounterKeys.sdsKey(entityType, eid))
                    .toList();
            List<Object> raws = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                for (String key : keys) connection.stringCommands().get(key.getBytes(StandardCharsets.UTF_8));
                return null;
            });
            int expectedLen = CounterSchema.SCHEMA_LEN * CounterSchema.FIELD_SIZE;
            for (int i = 0; i < entityIds.size(); i++) {
                byte[] raw = (i < raws.size() && raws.get(i) instanceof byte[] bytes) ? bytes : null;
                if (raw != null && raw.length == expectedLen) {
                    result.put(entityIds.get(i), decodeCounts(raw, safeMetrics));
                } else {
                    Map<String, Long> m = new LinkedHashMap<>();
                    for (String name : safeMetrics) m.put(name, 0L);
                    result.put(entityIds.get(i), m);
                }
            }
        } else {
            for (String eid : entityIds) {
                result.put(eid, getCountsInMemory(entityType, eid, safeMetrics));
            }
        }
        return result;
    }

    @Override
    public boolean isLiked(String entityType, String entityId, long userId) {
        return isSet("like", entityType, entityId, userId);
    }

    @Override
    public boolean isFaved(String entityType, String entityId, long userId) {
        return isSet("fav", entityType, entityId, userId);
    }

    private void triggerRebuild(String entityType, String entityId, List<String> metrics, Map<String, Long> result,
                                String sdsKey, int expectedLen) {
        if (redissonClient == null) {
            for (String m : metrics) result.put(m, 0L);
            return;
        }
        CounterProperties.Rebuild rb = counterProperties.getRebuild();
        if (inBackoff(entityType, entityId)) {
            for (String m : metrics) result.put(m, 0L);
            return;
        }
        if (!allowedByRateLimiter(entityType, entityId)) {
            escalateBackoff(entityType, entityId);
            for (String m : metrics) result.put(m, 0L);
            return;
        }
        String lockKey = String.format("lock:sds-rebuild:%s:%s", entityType, entityId);
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;
        try {
            locked = lock.tryLock(0L, java.util.concurrent.TimeUnit.MILLISECONDS);
            if (!locked) {
                escalateBackoff(entityType, entityId);
                for (String m : metrics) result.put(m, 0L);
                return;
            }
            byte[] newSds = new byte[expectedLen];
            List<String> rebuildFields = new ArrayList<>();
            for (String m : metrics) {
                Integer idx = CounterSchema.NAME_TO_IDX.get(m);
                if (idx == null) continue;
                long sum = bitCountShardsPipelined(m, entityType, entityId);
                writeInt32BE(newSds, idx * CounterSchema.FIELD_SIZE, sum);
                result.put(m, sum);
                rebuildFields.add(String.valueOf(idx));
            }
            setRaw(sdsKey, newSds);
            if (!rebuildFields.isEmpty()) {
                String aggKey = CounterKeys.aggKey(entityType, entityId);
                redisTemplate.opsForHash().delete(aggKey, rebuildFields.toArray());
            }
            resetBackoff(entityType, entityId);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            escalateBackoff(entityType, entityId);
            for (String m : metrics) result.put(m, 0L);
        } finally {
            if (locked) {
                try { lock.unlock(); } catch (Exception ignore) {}
            }
        }
    }

    private boolean inBackoff(String entityType, String entityId) {
        if (redissonClient == null) return false;
        String bKey = String.format("backoff:sds-rebuild:until:%s:%s", entityType, entityId);
        RBucket<Long> bucket = redissonClient.getBucket(bKey);
        Long until = bucket.get();
        return until != null && System.currentTimeMillis() < until;
    }

    private void escalateBackoff(String entityType, String entityId) {
        if (redissonClient == null) return;
        String eKey = String.format("backoff:sds-rebuild:exp:%s:%s", entityType, entityId);
        String uKey = String.format("backoff:sds-rebuild:until:%s:%s", entityType, entityId);
        RBucket<Integer> expB = redissonClient.getBucket(eKey);
        RBucket<Long> untilB = redissonClient.getBucket(uKey);
        Integer exp = expB.get();
        int nextExp = Math.min(exp == null ? 0 : exp + 1, 10);
        CounterProperties.Rebuild rb = counterProperties.getRebuild();
        long delay = Math.min(rb.getBackoffBaseMs() * (1L << nextExp), rb.getBackoffMaxMs());
        long until = System.currentTimeMillis() + delay;
        expB.set(nextExp);
        untilB.set(until, Duration.ofMillis(delay + 1000));
    }

    private void resetBackoff(String entityType, String entityId) {
        if (redissonClient == null) return;
        String eKey = String.format("backoff:sds-rebuild:exp:%s:%s", entityType, entityId);
        String uKey = String.format("backoff:sds-rebuild:until:%s:%s", entityType, entityId);
        try { redissonClient.getBucket(eKey).delete(); } catch (Exception ignore) {}
        try { redissonClient.getBucket(uKey).delete(); } catch (Exception ignore) {}
    }

    private boolean allowedByRateLimiter(String entityType, String entityId) {
        if (redissonClient == null) return false;
        String rlKey = String.format("rl:sds-rebuild:%s:%s", entityType, entityId);
        RRateLimiter limiter = redissonClient.getRateLimiter(rlKey);
        CounterProperties.Rebuild rb = counterProperties.getRebuild();
        limiter.trySetRate(RateType.OVERALL, rb.getRatePermits(), Duration.ofSeconds(rb.getRateWindowSeconds()));
        return limiter.tryAcquire(1);
    }

    private long bitCountShardsPipelined(String metric, String etype, String eid) {
        String pattern = String.format("bm:%s:%s:%s:*", metric, etype, eid);
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys == null || keys.isEmpty()) return 0L;
        List<Object> res = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (String k : keys) connection.stringCommands().bitCount(k.getBytes(StandardCharsets.UTF_8));
            return null;
        });
        long sum = 0L;
        if (res != null) {
            for (Object o : res) {
                if (o instanceof Number n) sum += n.longValue();
            }
        }
        return sum;
    }

    private byte[] getRaw(String key) {
        return redisTemplate.execute((RedisCallback<byte[]>) connection ->
                connection.stringCommands().get(key.getBytes(StandardCharsets.UTF_8)));
    }

    private void setRaw(String key, byte[] val) {
        redisTemplate.execute((RedisCallback<Void>) connection -> {
            connection.stringCommands().set(key.getBytes(StandardCharsets.UTF_8), val);
            return null;
        });
    }

    private boolean toggle(String entityType, String entityId, long userId, String metric, int idx, boolean add) {
        if (!isRedisEnabled()) {
            boolean changed = toggleInMemory(metric, entityType, entityId, userId, add);
            if (changed) publishEvent(entityType, entityId, metric, idx, userId, add ? 1 : -1);
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
            if (updated) publishEvent(entityType, entityId, metric, idx, userId, add ? 1 : -1);
            return updated;
        } catch (Exception e) {
            boolean changed = toggleInMemory(metric, entityType, entityId, userId, add);
            if (changed) publishEvent(entityType, entityId, metric, idx, userId, add ? 1 : -1);
            return changed;
        }
    }

    private boolean toggleInMemory(String metric, String entityType, String entityId, long userId, boolean add) {
        Set<Long> users = metricFacts(metric).computeIfAbsent(entityKey(entityType, entityId), ignored -> ConcurrentHashMap.newKeySet());
        return add ? users.add(userId) : users.remove(userId);
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
                            CounterKeys.bitmapKey(metric, entityType, entityId, chunk).getBytes(StandardCharsets.UTF_8), bit));
            return Boolean.TRUE.equals(value);
        } catch (Exception e) {
            return metricFacts(metric).getOrDefault(entityKey(entityType, entityId), Set.of()).contains(userId);
        }
    }

    private Map<String, Long> getCountsInMemory(String entityType, String entityId, List<String> metrics) {
        Map<String, Long> counts = new LinkedHashMap<>();
        String key = entityKey(entityType, entityId);
        for (String m : metrics) {
            long value = switch (m) {
                case "like" -> likeFacts.getOrDefault(key, Set.of()).size();
                case "fav" -> favFacts.getOrDefault(key, Set.of()).size();
                default -> 0L;
            };
            counts.put(m, value);
        }
        return counts;
    }

    private Map<String, Long> decodeCounts(byte[] raw, List<String> metrics) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String metric : metrics) {
            Integer idx = CounterSchema.NAME_TO_IDX.get(metric);
            if (idx == null) continue;
            counts.put(metric, raw == null ? 0L : readInt32BE(raw, idx * CounterSchema.FIELD_SIZE));
        }
        return counts;
    }

    private long readInt32BE(byte[] buf, int off) {
        long n = 0;
        for (int i = 0; i < 4; i++) n = (n << 8) | (buf[off + i] & 0xFFL);
        return n;
    }

    private void writeInt32BE(byte[] buf, int off, long val) {
        long n = Math.max(0, Math.min(val, 0xFFFF_FFFFL));
        buf[off] = (byte) ((n >>> 24) & 0xFF);
        buf[off + 1] = (byte) ((n >>> 16) & 0xFF);
        buf[off + 2] = (byte) ((n >>> 8) & 0xFF);
        buf[off + 3] = (byte) (n & 0xFF);
    }

    private List<String> normalizeMetrics(List<String> metrics) {
        if (metrics == null || metrics.isEmpty()) return new ArrayList<>(CounterSchema.SUPPORTED_METRICS);
        return metrics.stream().filter(CounterSchema.SUPPORTED_METRICS::contains).distinct().toList();
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
