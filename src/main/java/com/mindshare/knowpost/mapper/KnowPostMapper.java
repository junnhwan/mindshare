package com.mindshare.knowpost.mapper;

import com.mindshare.knowpost.model.KnowPost;
import com.mindshare.knowpost.model.KnowPostDetailRow;
import com.mindshare.knowpost.model.KnowPostFeedRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface KnowPostMapper {

    void insertDraft(KnowPost post);

    KnowPost findById(@Param("id") Long id);

    int updateContent(KnowPost post);

    int updateMetadata(KnowPost post);

    int publish(@Param("id") Long id, @Param("creatorId") Long creatorId);

    List<KnowPostFeedRow> listFeedPublic(@Param("limit") int limit, @Param("offset") int offset);

    List<KnowPostFeedRow> listMyPublished(@Param("creatorId") long creatorId,
                                          @Param("limit") int limit,
                                          @Param("offset") int offset);

    int updateTop(@Param("id") Long id, @Param("creatorId") Long creatorId, @Param("isTop") Boolean isTop);

    int updateVisibility(@Param("id") Long id, @Param("creatorId") Long creatorId, @Param("visible") String visible);

    int softDelete(@Param("id") Long id, @Param("creatorId") Long creatorId);

    KnowPostDetailRow findDetailById(@Param("id") Long id);

    long countMyPublished(@Param("creatorId") long creatorId);

    List<Long> listMyPublishedIds(@Param("creatorId") long creatorId);
}
