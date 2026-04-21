package com.mindshare.counter.service;

import java.util.Map;

public interface UserCounterService {

    void incrementFollowings(long userId, int delta);

    void incrementFollowers(long userId, int delta);

    void incrementPosts(long userId, int delta);

    void incrementLikesReceived(long userId, int delta);

    void incrementFavsReceived(long userId, int delta);

    void rebuildAllCounters(long userId);

    Map<String, Long> getCounts(long userId);
}
