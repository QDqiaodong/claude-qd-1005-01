package com.utility.tunnel.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.utility.tunnel.entity.Pipeline;

@Mapper
public interface PipelineMapper {

    int insert(Pipeline pipeline);

    List<Pipeline> findAll();

    List<Pipeline> findBySegmentId(@Param("segmentId") Long segmentId);

    Pipeline selectById(@Param("id") Long id);

    /** 统计某舱段内指定类型的管线数量（用于燃气/电力防爆隔离校验） */
    int countBySegmentAndType(@Param("segmentId") Long segmentId,
                              @Param("lineType") String lineType);
}
