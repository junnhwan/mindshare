package com.mindshare.relation.mapper;

import lombok.Data;

import java.time.Instant;

@Data
public class RelationRow {
    private Long id;
    private Long fromUserId;
    private Long toUserId;
    private Integer relStatus;
    private Instant createdAt;
    private Instant updatedAt;
}
