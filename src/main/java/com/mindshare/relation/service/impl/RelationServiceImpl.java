package com.mindshare.relation.service.impl;

import com.mindshare.counter.service.UserCounterService;
import com.mindshare.knowpost.id.SnowflakeIdGenerator;
import com.mindshare.relation.mapper.RelationMapper;
import com.mindshare.relation.mapper.RelationRow;
import com.mindshare.relation.service.RelationService;
import com.mindshare.user.mapper.UserMapper;
import com.mindshare.user.domain.User;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Profile("!bootstrap-test")
public class RelationServiceImpl implements RelationService {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneOffset.UTC);

    private final RelationMapper relationMapper;
    private final SnowflakeIdGenerator idGenerator;
    private final UserCounterService userCounterService;
    private final UserMapper userMapper;

    public RelationServiceImpl(RelationMapper relationMapper, SnowflakeIdGenerator idGenerator,
                                UserCounterService userCounterService, UserMapper userMapper) {
        this.relationMapper = relationMapper;
        this.idGenerator = idGenerator;
        this.userCounterService = userCounterService;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public boolean follow(long fromUserId, long toUserId) {
        if (fromUserId == toUserId) return false;
        RelationRow existing = relationMapper.findByFollowing(fromUserId, toUserId);
        String now = DT_FMT.format(Instant.now());
        if (existing != null) {
            if (existing.getRelStatus() != null && existing.getRelStatus() == 1) return false;
            relationMapper.cancelFollowing(fromUserId, toUserId, now);
            relationMapper.cancelFollower(toUserId, fromUserId, now);
        }
        long id = idGenerator.nextId();
        relationMapper.insertFollowing(id, fromUserId, toUserId, now);
        relationMapper.insertFollower(id, toUserId, fromUserId, now);
        userCounterService.incrementFollowings(fromUserId, 1);
        userCounterService.incrementFollowers(toUserId, 1);
        return true;
    }

    @Override
    @Transactional
    public boolean unfollow(long fromUserId, long toUserId) {
        if (fromUserId == toUserId) return false;
        RelationRow existing = relationMapper.findByFollowing(fromUserId, toUserId);
        if (existing == null || existing.getRelStatus() == null || existing.getRelStatus() != 1) return false;
        String now = DT_FMT.format(Instant.now());
        relationMapper.cancelFollowing(fromUserId, toUserId, now);
        relationMapper.cancelFollower(toUserId, fromUserId, now);
        userCounterService.incrementFollowings(fromUserId, -1);
        userCounterService.incrementFollowers(toUserId, -1);
        return true;
    }

    @Override
    public String relationStatus(long fromUserId, long toUserId) {
        if (fromUserId == toUserId) return "self";
        boolean iFollow = relationMapper.existsFollowing(fromUserId, toUserId);
        boolean theyFollow = relationMapper.existsFollowing(toUserId, fromUserId);
        if (iFollow && theyFollow) return "mutual";
        if (iFollow) return "following";
        if (theyFollow) return "followedBy";
        return "none";
    }

    @Override
    public List<Long> listFollowingIds(long userId, int page, int size) {
        int offset = (Math.max(page, 1) - 1) * Math.min(Math.max(size, 1), 50);
        return relationMapper.listFollowingIds(userId, Math.min(Math.max(size, 1), 50), offset);
    }

    @Override
    public List<Long> listFollowerIds(long userId, int page, int size) {
        int offset = (Math.max(page, 1) - 1) * Math.min(Math.max(size, 1), 50);
        return relationMapper.listFollowerIds(userId, Math.min(Math.max(size, 1), 50), offset);
    }

    @Override
    public long countFollowing(long userId) {
        return relationMapper.countFollowing(userId);
    }

    @Override
    public long countFollowers(long userId) {
        return relationMapper.countFollowers(userId);
    }
}
