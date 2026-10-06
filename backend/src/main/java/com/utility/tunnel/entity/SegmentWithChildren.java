package com.utility.tunnel.entity;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 舱段 + 其下全部子对象的聚合视图。
 *
 * <p>由一条带三张子表 LEFT JOIN 的 SQL 经嵌套 &lt;collection&gt; resultMap 一次性带出：
 * 入廊管线 pipelines / 巡检点 patrolPoints / 附属设备 facilities。</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class SegmentWithChildren {

    private Long id;
    private String segCode;
    private String segName;
    private String shape;
    private Double lengthM;
    private Integer capacitySlots;
    private String status;

    private List<Pipeline> pipelines = new ArrayList<>();
    private List<PatrolPoint> patrolPoints = new ArrayList<>();
    private List<Facility> facilities = new ArrayList<>();
}
