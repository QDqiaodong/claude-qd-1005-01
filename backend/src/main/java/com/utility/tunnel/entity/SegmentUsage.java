package com.utility.tunnel.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 舱段容量用量汇总。
 *
 * <p>由 pipeline 表实时聚合（SUM(occupancy)），不落库、不维护计数器，
 * 因此刷新后汇总与已存管线天然一致；剩余容量 = 舱段容量 - 已用容量。</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class SegmentUsage {

    private Long segmentId;

    private String segCode;

    private String segName;

    /** 舱段总容量（容量单位） */
    private Integer capacity;

    /** 舱内管线条数 */
    private Integer pipelineCount;

    /** 已用容量 = 舱内管线占用之和 */
    private Integer used;

    /** 剩余容量 = capacity - used */
    private Integer remaining;
}
