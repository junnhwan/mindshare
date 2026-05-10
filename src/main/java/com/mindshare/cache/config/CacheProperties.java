package com.mindshare.cache.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "mindshare.cache")
public class CacheProperties {

    private boolean redisEnabled = true;
    private L2 l2 = new L2();
    private Hotkey hotkey = new Hotkey();

    @Data
    public static class L2 {
        private PublicCfg publicCfg = new PublicCfg();
        private MineCfg mineCfg = new MineCfg();
        private DetailCfg detailCfg = new DetailCfg();
    }

    @Data
    public static class PublicCfg {
        private int ttlSeconds = 300;
        private long maxSize = 128;
    }

    @Data
    public static class MineCfg {
        private int ttlSeconds = 180;
        private long maxSize = 128;
    }

    @Data
    public static class DetailCfg {
        private int ttlSeconds = 300;
        private long maxSize = 256;
    }

    @Data
    public static class Hotkey {
        private int windowSeconds = 60;
        private int segmentSeconds = 10;
        private int levelLow = 10;
        private int levelMedium = 50;
        private int levelHigh = 100;
        private int extendLowSeconds = 30;
        private int extendMediumSeconds = 120;
        private int extendHighSeconds = 300;
    }
}
