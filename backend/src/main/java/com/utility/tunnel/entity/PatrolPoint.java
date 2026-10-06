package com.utility.tunnel.entity;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 巡检点：舱段上布设的日常巡检位置。
 *
 * <p>巡检登记会更新 lastPatrolAt。</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class PatrolPoint {

    private Long id;

    /** 所属舱段 id */
    private Long segmentId;

    /** 点位编码 */
    private String pointCode;

    /** 位置描述 */
    private String location;

    /** 最近一次巡检时间 */
    private LocalDateTime lastPatrolAt;

    private LocalDateTime createdAt;
}
