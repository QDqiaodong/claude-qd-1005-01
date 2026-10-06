package com.utility.tunnel.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.utility.tunnel.entity.Pipeline;
import com.utility.tunnel.entity.SegmentUsage;

@Mapper
public interface PipelineMapper {

    int insert(Pipeline pipeline);

    List<Pipeline> findAll();

    List<Pipeline> findBySegmentId(@Param("segmentId") Long segmentId);

    Pipeline selectById(@Param("id") Long id);

    /** 统计某舱段内指定类型的管线数量（用于燃气/电力防爆隔离校验） */
    int countBySegmentAndType(@Param("segmentId") Long segmentId,
                              @Param("lineType") String lineType);

    /** 舱段已用容量 = 舱内管线占用之和（须在舱段行锁内调用，配合容量校核） */
    int sumOccupancyBySegment(@Param("segmentId") Long segmentId);

    /** 舱段容量用量汇总：按舱段聚合一览（segmentId 为空时查全部舱段） */
    List<SegmentUsage> findUsage(@Param("segmentId") Long segmentId);
}
