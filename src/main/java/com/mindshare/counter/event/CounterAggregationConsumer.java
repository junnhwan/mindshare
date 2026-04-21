package com.mindshare.counter.event;

import com.mindshare.counter.config.CounterProperties;
import com.mindshare.counter.schema.CounterKeys;
import com.mindshare.counter.schema.CounterSchema;
import com.mindshare.counter.service.UserCounterService;
import com.mindshare.knowpost.mapper.KnowPostMapper;
import com.mindshare.knowpost.model.KnowPost;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@Profile("!bootstrap-test")
public class CounterAggregationConsumer {

    private final StringRedisTemplate redisTemplate;
    private final CounterProperties counterProperties;
    private final UserCounterService userCounterService;
    private final KnowPostMapper knowPostMapper;

    public CounterAggregationConsumer(
            StringRedisTemplate redisTemplate,
            CounterProperties counterProperties,
            UserCounterService userCounterService,
            KnowPostMapper knowPostMapper
    ) {
        this.redisTemplate = redisTemplate;
        this.counterProperties = counterProperties;
        this.userCounterService = userCounterService;
        this.knowPostMapper = knowPostMapper;
    }

    @EventListener
    public void onEvent(CounterEvent event) {
        applyEntityCounts(event);
        applyAuthorCounts(event);
    }

    private void applyEntityCounts(CounterEvent event) {
        if (!counterProperties.isRedisEnabled()) {
            return;
        }
        try {
            redisTemplate.execute((RedisCallback<Void>) connection -> {
                byte[] raw = connection.stringCommands().get(entityKey(event));
                byte[] values = normalizeRaw(raw);
                int offset = event.idx() * CounterSchema.FIELD_SIZE;
                long current = readInt32Be(values, offset);
                writeInt32Be(values, offset, Math.max(0L, current + event.delta()));
                connection.stringCommands().set(entityKey(event), values);
                return null;
            });
        } catch (Exception ignored) {
        }
    }

    private void applyAuthorCounts(CounterEvent event) {
        Long knowPostId = parseKnowPostId(event.entityType(), event.entityId());
        if (knowPostId == null) {
            return;
        }
        KnowPost knowPost = knowPostMapper.findById(knowPostId);
        if (knowPost == null || knowPost.getCreatorId() == null) {
            return;
        }
        if ("like".equals(event.metric())) {
            userCounterService.incrementLikesReceived(knowPost.getCreatorId(), event.delta());
        } else if ("fav".equals(event.metric())) {
            userCounterService.incrementFavsReceived(knowPost.getCreatorId(), event.delta());
        }
    }

    private Long parseKnowPostId(String entityType, String entityId) {
        if (!"knowpost".equals(entityType)) {
            return null;
        }
        try {
            return Long.parseLong(entityId);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private byte[] entityKey(CounterEvent event) {
        return CounterKeys.sdsKey(event.entityType(), event.entityId()).getBytes(StandardCharsets.UTF_8);
    }

    private byte[] normalizeRaw(byte[] raw) {
        byte[] values = new byte[CounterSchema.SCHEMA_LEN * CounterSchema.FIELD_SIZE];
        if (raw != null) {
            System.arraycopy(raw, 0, values, 0, Math.min(raw.length, values.length));
        }
        return values;
    }

    private long readInt32Be(byte[] raw, int offset) {
        long value = 0L;
        for (int index = 0; index < CounterSchema.FIELD_SIZE; index++) {
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
}
