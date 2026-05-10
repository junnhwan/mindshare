package com.mindshare.knowpost.outbox;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OutboxMapper {

    void insert(OutboxEvent event);

    List<OutboxEvent> pollUnprocessed(@Param("limit") int limit);

    void deleteById(@Param("id") long id);
}
