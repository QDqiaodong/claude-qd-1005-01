package com.utility.tunnel.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.utility.tunnel.entity.PatrolPoint;

@Mapper
public interface PatrolPointMapper {

    int insert(PatrolPoint patrolPoint);

    List<PatrolPoint> findBySegmentId(@Param("segmentId") Long segmentId);

    PatrolPoint selectById(@Param("id") Long id);

    List<PatrolPoint> findAll();

    int updateLastPatrolAt(@Param("id") Long id,
                           @Param("lastPatrolAt") LocalDateTime lastPatrolAt);
}
