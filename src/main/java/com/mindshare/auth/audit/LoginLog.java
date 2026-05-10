package com.mindshare.auth.audit;

import lombok.Data;

import java.time.Instant;

@Data
public class LoginLog {

    private Long id;
    private Long userId;
    private String identifier;
    private String channel;
    private String ip;
    private String userAgent;
    private String result;
    private Instant createTime;
}
