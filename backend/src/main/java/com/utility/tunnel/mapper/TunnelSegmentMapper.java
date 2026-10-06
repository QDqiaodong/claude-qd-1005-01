package com.utility.tunnel.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.utility.tunnel.entity.SegmentWithChildren;
import com.utility.tunnel.entity.TunnelSegment;

@Mapper
public interface TunnelSegmentMapper {

    int insert(TunnelSegment segment);

    List<TunnelSegment> findAll();

    List<TunnelSegment> findByStatus(@Param("status") String status);

    TunnelSegment selectById(@Param("id") Long id);

    /** 行锁读取：状态变更 / 风险登记时与并发事务互斥 */
    TunnelSegment selectByIdForUpdate(@Param("id") Long id);

    int updateStatus(@Param("id") Long id, @Param("status") String status);

    SegmentWithChildren selectWithChildren(@Param("id") Long id);

    int countAll();

    int countByStatus(@Param("status") String status);
}
