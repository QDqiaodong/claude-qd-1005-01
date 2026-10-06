package com.utility.tunnel.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.utility.tunnel.entity.Facility;

@Mapper
public interface FacilityMapper {

    int insert(Facility facility);

    List<Facility> findBySegmentId(@Param("segmentId") Long segmentId);

    Facility selectById(@Param("id") Long id);

    List<Facility> findAll();

    int updateStatus(@Param("id") Long id, @Param("status") String status);

    int countAll();

    int countByStatus(@Param("status") String status);
}
