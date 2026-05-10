package com.mindshare.relation.service;

import com.mindshare.profile.api.dto.ProfileResponse;

import java.util.List;

public interface RelationService {

    boolean follow(long fromUserId, long toUserId);

    boolean unfollow(long fromUserId, long toUserId);

    String relationStatus(long fromUserId, long toUserId);

    List<Long> listFollowingIds(long userId, int page, int size);

    List<Long> listFollowerIds(long userId, int page, int size);

    long countFollowing(long userId);

    long countFollowers(long userId);
}
