package com.mindshare.counter.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "mindshare.counter")
public class CounterProperties {

    private boolean redisEnabled = true;
}
