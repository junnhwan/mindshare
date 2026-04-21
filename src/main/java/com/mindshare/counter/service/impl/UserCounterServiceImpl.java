package com.mindshare.counter.service.impl;

import com.mindshare.counter.config.CounterProperties;
import com.mindshare.counter.schema.UserCounterKeys;
import com.mindshare.counter.service.CounterService;
import com.mindshare.counter.service.UserCounterService;
import com.mindshare.knowpost.mapper.KnowPostMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@Profile("!bootstrap-test")
public class UserCounterServiceImpl implements UserCounterService {

    private static final int FIELD_SIZE = 4;
    private static final int FIELD_COUNT = 5;
    private static final int IDX_FOLLOWINGS = 0;
    private static final int IDX_FOLLOWERS = 1;
    private static final int IDX_POSTS = 2;
    private static final int IDX_LIKES_RECEIVED = 3;
    private static final int IDX_FAVS_RECEIVED = 4;
    private static final List<String> FIELD_NAMES = List.of(
            "followings",
            "followers",
            "posts",
            "likesReceived",
            "favsReceived"
    );

    private final StringRedisTemplate redisTemplate;
    private final CounterProperties counterProperties;
    private final KnowPostMapper knowPostMapper;
    private final ObjectProvider<CounterService> counterServiceProvider;
    private final ConcurrentMap<Long, long[]> inMemoryCounters = new ConcurrentHashMap<>();

    public UserCounterServiceImpl(
            StringRedisTemplate redisTemplate,
            CounterProperties counterProperties,
            KnowPostMapper knowPostMapper,
            ObjectProvider<CounterService> counterServiceProvider
    ) {
        this.redisTemplate = redisTemplate;
        this.counterProperties = counterProperties;
        this.knowPostMapper = knowPostMapper;
        this.counterServiceProvider = counterServiceProvider;
    }

    @Override
    public void incrementFollowings(long userId, int delta) {
        increment(userId, IDX_FOLLOWINGS, delta);
    }

    @Override
    public void incrementFollowers(long userId, int delta) {
        increment(userId, IDX_FOLLOWERS, delta);
    }

    @Override
    public void incrementPosts(long userId, int delta) {
        increment(userId, IDX_POSTS, delta);
    }

    @Override
    public void incrementLikesReceived(long userId, int delta) {
        increment(userId, IDX_LIKES_RECEIVED, delta);
    }

    @Override
    public void incrementFavsReceived(long userId, int delta) {
        increment(userId, IDX_FAVS_RECEIVED, delta);
    }

    @Override
    public void rebuildAllCounters(long userId) {
        long[] snapshot = new long[FIELD_COUNT];
        snapshot[IDX_FOLLOWINGS] = 0L;
        snapshot[IDX_FOLLOWERS] = 0L;
        snapshot[IDX_POSTS] = knowPostMapper.countMyPublished(userId);

        CounterService counterService = counterServiceProvider.getIfAvailable();
        List<Long> publishedIds = knowPostMapper.listMyPublishedIds(userId);
        if (counterService != null && publishedIds != null && !publishedIds.isEmpty()) {
            List<String> entityIds = publishedIds.stream().map(String::valueOf).toList();
            Map<String, Map<String, Long>> countsBatch = counterService.getCountsBatch("knowpost", entityIds, List.of("like", "fav"));
            for (String entityId : entityIds) {
                Map<String, Long> counts = countsBatch.get(entityId);
                if (counts == null) {
                    continue;
                }
                snapshot[IDX_LIKES_RECEIVED] += counts.getOrDefault("like", 0L);
                snapshot[IDX_FAVS_RECEIVED] += counts.getOrDefault("fav", 0L);
            }
        }

        writeAll(userId, snapshot);
    }

    @Override
    public Map<String, Long> getCounts(long userId) {
        long[] snapshot = readAll(userId);
        Map<String, Long> counts = new LinkedHashMap<>();
        for (int index = 0; index < FIELD_NAMES.size(); index++) {
            counts.put(FIELD_NAMES.get(index), snapshot[index]);
        }
        return counts;
    }

    private void increment(long userId, int index, int delta) {
        if (!isRedisEnabled()) {
            long[] values = inMemoryCounters.computeIfAbsent(userId, ignored -> new long[FIELD_COUNT]);
            values[index] = Math.max(0L, values[index] + delta);
            return;
        }
        try {
            redisTemplate.execute((RedisCallback<Void>) connection -> {
                byte[] raw = connection.stringCommands().get(key(userId));
                byte[] values = normalizeRaw(raw);
                long current = readInt32Be(values, index * FIELD_SIZE);
                writeInt32Be(values, index * FIELD_SIZE, Math.max(0L, current + delta));
                connection.stringCommands().set(key(userId), values);
                return null;
            });
        } catch (Exception exception) {
            long[] values = inMemoryCounters.computeIfAbsent(userId, ignored -> new long[FIELD_COUNT]);
            values[index] = Math.max(0L, values[index] + delta);
        }
    }

    private long[] readAll(long userId) {
        if (!isRedisEnabled()) {
            return inMemoryCounters.computeIfAbsent(userId, ignored -> new long[FIELD_COUNT]).clone();
        }
        try {
            byte[] raw = redisTemplate.execute((RedisCallback<byte[]>) connection -> connection.stringCommands().get(key(userId)));
            byte[] values = normalizeRaw(raw);
            long[] snapshot = new long[FIELD_COUNT];
            for (int index = 0; index < FIELD_COUNT; index++) {
                snapshot[index] = readInt32Be(values, index * FIELD_SIZE);
            }
            return snapshot;
        } catch (Exception exception) {
            return inMemoryCounters.computeIfAbsent(userId, ignored -> new long[FIELD_COUNT]).clone();
        }
    }

    private void writeAll(long userId, long[] snapshot) {
        if (!isRedisEnabled()) {
            inMemoryCounters.put(userId, snapshot.clone());
            return;
        }
        try {
            byte[] raw = new byte[FIELD_COUNT * FIELD_SIZE];
            for (int index = 0; index < FIELD_COUNT; index++) {
                writeInt32Be(raw, index * FIELD_SIZE, snapshot[index]);
            }
            redisTemplate.execute((RedisCallback<Void>) connection -> {
                connection.stringCommands().set(key(userId), raw);
                return null;
            });
        } catch (Exception exception) {
            inMemoryCounters.put(userId, snapshot.clone());
        }
    }

    private byte[] normalizeRaw(byte[] raw) {
        byte[] values = new byte[FIELD_COUNT * FIELD_SIZE];
        if (raw != null) {
            System.arraycopy(raw, 0, values, 0, Math.min(raw.length, values.length));
        }
        return values;
    }

    private long readInt32Be(byte[] raw, int offset) {
        long value = 0L;
        for (int index = 0; index < FIELD_SIZE; index++) {
            value = (value << 8) | (raw[offset + index] & 0xFFL);
        }
        return value;
    }

    private void writeInt32Be(byte[] raw, int offset, long value) {
        long safe = Math.max(0L, Math.min(value, 0xFFFF_FFFFL));
        raw[offset] = (byte) ((safe >>> 24) & 0xFF);
        raw[offset + 1] = (byte) ((safe >>> 16) & 0xFF);
        raw[offset + 2] = (byte) ((safe >>> 8) & 0xFF);
        raw[offset + 3] = (byte) (safe & 0xFF);
    }

    private byte[] key(long userId) {
        return UserCounterKeys.sdsKey(userId).getBytes(StandardCharsets.UTF_8);
    }

    private boolean isRedisEnabled() {
        return counterProperties.isRedisEnabled();
    }
}
