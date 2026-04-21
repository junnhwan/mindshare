package com.mindshare.counter.api.dto;

import java.util.Map;

public record CountsResponse(
        String entityType,
        String entityId,
        Map<String, Long> counts
) {
}
