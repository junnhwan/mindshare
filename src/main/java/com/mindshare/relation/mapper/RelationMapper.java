package com.mindshare.relation.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RelationMapper {

    void insertFollowing(@Param("id") long id, @Param("fromUserId") long fromUserId,
                         @Param("toUserId") long toUserId, @Param("now") String now);

    void insertFollower(@Param("id") long id, @Param("toUserId") long toUserId,
                        @Param("fromUserId") long fromUserId, @Param("now") String now);

    void cancelFollowing(@Param("fromUserId") long fromUserId, @Param("toUserId") long toUserId,
                         @Param("now") String now);

    void cancelFollower(@Param("toUserId") long toUserId, @Param("fromUserId") long fromUserId,
                        @Param("now") String now);

    boolean existsFollowing(@Param("fromUserId") long fromUserId, @Param("toUserId") long toUserId);

    RelationRow findByFollowing(@Param("fromUserId") long fromUserId, @Param("toUserId") long toUserId);

    List<Long> listFollowingIds(@Param("userId") long userId, @Param("limit") int limit,
                                @Param("offset") long offset);

    List<Long> listFollowerIds(@Param("userId") long userId, @Param("limit") int limit,
                               @Param("offset") long offset);

    List<Long> listFollowingIdsAfter(@Param("userId") long userId, @Param("after") long afterId,
                                     @Param("limit") int limit);

    List<Long> listFollowerIdsAfter(@Param("userId") long userId, @Param("after") long afterId,
                                    @Param("limit") int limit);

    long countFollowing(@Param("userId") long userId);

    long countFollowers(@Param("userId") long userId);
}
