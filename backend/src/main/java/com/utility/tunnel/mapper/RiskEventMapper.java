package com.utility.tunnel.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.utility.tunnel.entity.RiskEvent;
import com.utility.tunnel.entity.RiskEventView;

@Mapper
public interface RiskEventMapper {

    int insert(RiskEvent event);

    RiskEvent selectById(@Param("id") Long id);

    /** 列表视图：按舱段 / 状态筛选，带舱段编码与发现对象名称 */
    List<RiskEventView> findByFilter(@Param("segmentId") Long segmentId,
                                     @Param("status") String status);

    RiskEventView findViewById(@Param("id") Long id);

    /** 未解除（待处置/处理中）事件数：舱段恢复运营的封控判定依据 */
    int countUnresolvedBySegment(@Param("segmentId") Long segmentId);

    /** 最近一条未解除事件：用于拼当前封控原因 */
    RiskEventView findLatestUnresolved(@Param("segmentId") Long segmentId);

    /** 推进：仅当当前状态为「待处置」才置为「处理中」（带状态条件，防并发/重复提交） */
    int advance(@Param("id") Long id, @Param("handleNote") String handleNote);

    /** 解除：仅当当前状态为「待处置/处理中」才置为「已解除」（带状态条件，重复解除影响 0 行） */
    int resolve(@Param("id") Long id,
                @Param("handleNote") String handleNote,
                @Param("resolvedBy") String resolvedBy,
                @Param("resolvedAt") LocalDateTime resolvedAt);
}
