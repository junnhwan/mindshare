package com.mindshare.search.api.dto;

import com.mindshare.knowpost.api.dto.FeedItemResponse;

import java.util.List;

public record SearchResponse(
        List<FeedItemResponse> items,
        String nextAfter,
        boolean hasMore
) {
}
