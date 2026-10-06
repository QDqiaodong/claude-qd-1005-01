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
}
