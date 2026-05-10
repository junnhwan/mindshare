package com.mindshare.knowpost.model;

import lombok.Data;

import java.time.Instant;

@Data
public class KnowPostFeedRow {

    private Long id;
    private String title;
    private String description;
    private String tags;
    private String imgUrls;
    private String authorAvatar;
    private String authorNickname;
    private String authorTagJson;
    private Instant publishTime;
    private Boolean isTop;
}
