package com.mindshare.knowpost.service;

import com.mindshare.knowpost.api.dto.FeedPageResponse;

public interface KnowPostFeedService {

    FeedPageResponse getPublicFeed(int page, int size, Long currentUserIdNullable);

    FeedPageResponse getMyPublished(long creatorId, int page, int size);

    void invalidatePublicFeed();

    void invalidatePublicFeedForPost(long postId);

    void invalidateMyPublished(long creatorId);
}
