package com.mindshare.counter.event;

import com.mindshare.counter.config.CounterProperties;
import com.mindshare.counter.schema.CounterKeys;
import com.mindshare.counter.schema.CounterSchema;
import com.mindshare.counter.service.UserCounterService;
import com.mindshare.knowpost.mapper.KnowPostMapper;
import com.mindshare.knowpost.model.KnowPost;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Profile("!bootstrap-test")
public class CounterAggregationConsumer {

    private final StringRedisTemplate redis;
    private final CounterProperties counterProperties;
    private final UserCounterService userCounterService;
    private final KnowPostMapper knowPostMapper;
    private final DefaultRedisScript<Long> incrScript;
    private final DefaultRedisScript<Long> decrScript;

    public CounterAggregationConsumer(
            StringRedisTemplate redis,
            CounterProperties counterProperties,
            UserCounterService userCounterService,
            KnowPostMapper knowPostMapper) {
        this.redis = redis;
        this.counterProperties = counterProperties;
        this.userCounterService = userCounterService;
        this.knowPostMapper = knowPostMapper;
        this.incrScript = new DefaultRedisScript<>();
        this.incrScript.setResultType(Long.class);
        this.incrScript.setScriptText(INCR_FIELD_LUA);
        this.decrScript = new DefaultRedisScript<>();
        this.decrScript.setResultType(Long.class);
        this.decrScript.setScriptText(DECR_FIELD_LUA);
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
            String aggKey = CounterKeys.aggKey(event.entityType(), event.entityId());
            redis.opsForHash().increment(aggKey, String.valueOf(event.idx()), event.delta());
        } catch (Exception ignored) {
        }
    }

    private void applyAuthorCounts(CounterEvent event) {
        Long postId = parseKnowPostId(event.entityType(), event.entityId());
        if (postId == null) return;
        KnowPost post = knowPostMapper.findById(postId);
        if (post == null || post.getCreatorId() == null) return;
        if ("like".equals(event.metric())) {
            userCounterService.incrementLikesReceived(post.getCreatorId(), event.delta());
        } else if ("fav".equals(event.metric())) {
            userCounterService.incrementFavsReceived(post.getCreatorId(), event.delta());
        }
    }

    @Scheduled(fixedDelay = 1000L)
    public void flush() {
        if (!counterProperties.isRedisEnabled()) {
            return;
        }
        Set<String> keys = redis.keys("agg:" + CounterSchema.SCHEMA_ID + ":*");
        if (keys == null || keys.isEmpty()) return;

        for (String aggKey : keys) {
            Map<Object, Object> entries = redis.opsForHash().entries(aggKey);
            if (entries.isEmpty()) continue;

            String[] parts = aggKey.split(":", 4);
            if (parts.length < 4) continue;

            String cntKey = CounterKeys.sdsKey(parts[2], parts[3]);

            for (Map.Entry<Object, Object> e : entries.entrySet()) {
                String field = String.valueOf(e.getKey());
                long delta;
                try {
                    delta = Long.parseLong(String.valueOf(e.getValue()));
                } catch (NumberFormatException nfe) {
                    continue;
                }
                if (delta == 0) continue;
                int idx;
                try {
                    idx = Integer.parseInt(field);
                } catch (NumberFormatException nfe) {
                    continue;
                }
                try {
                    redis.execute(incrScript, List.of(cntKey),
                            String.valueOf(CounterSchema.SCHEMA_LEN),
                            String.valueOf(CounterSchema.FIELD_SIZE),
                            String.valueOf(idx),
                            String.valueOf(delta));
                    redis.execute(decrScript, List.of(aggKey), field, String.valueOf(delta));
                } catch (Exception ex) {
                    // retry next cycle
                }
            }
            Long size = redis.opsForHash().size(aggKey);
            if (size != null && size == 0L) {
                redis.delete(aggKey);
            }
        }
    }

    private Long parseKnowPostId(String entityType, String entityId) {
        if (!"knowpost".equals(entityType)) return null;
        try {
            return Long.parseLong(entityId);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static final String INCR_FIELD_LUA = """
            local cntKey = KEYS[1]
            local schemaLen = tonumber(ARGV[1])
            local fieldSize = tonumber(ARGV[2])
            local idx = tonumber(ARGV[3])
            local delta = tonumber(ARGV[4])

            local function read32be(s, off)
              local b = {string.byte(s, off+1, off+4)}
              local n = 0
              for i=1,4 do n = n * 256 + b[i] end
              return n
            end

            local function write32be(n)
              local t = {}
              for i=4,1,-1 do t[i] = n % 256; n = math.floor(n/256) end
              return string.char(unpack(t))
            end

            local cnt = redis.call('GET', cntKey)
            if not cnt then cnt = string.rep(string.char(0), schemaLen * fieldSize) end
            local off = idx * fieldSize
            local v = read32be(cnt, off) + delta
            if v < 0 then v = 0 end
            local seg = write32be(v)
            cnt = string.sub(cnt, 1, off) .. seg .. string.sub(cnt, off+fieldSize+1)
            redis.call('SET', cntKey, cnt)
            return 1
            """;

    private static final String DECR_FIELD_LUA = """
            local key = KEYS[1]
            local field = ARGV[1]
            local delta = tonumber(ARGV[2])
            local v = redis.call('HINCRBY', key, field, -delta)
            if v == 0 then
                redis.call('HDEL', key, field)
            end
            return v
            """;
}
