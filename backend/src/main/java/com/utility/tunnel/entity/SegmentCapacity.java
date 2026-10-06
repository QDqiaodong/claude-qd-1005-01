package com.utility.tunnel.entity;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 舱段用量（容量汇总）：舱容、已用舱位、剩余舱位及逐条管线的占用明细。
 *
 * <p>已用 / 剩余均由后端实时遍历该舱段管线计算，不维护任何计数器，
 * 因此与已存管线始终一致；lockReason 带出当前未解除风险事件构成的封控原因。</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class SegmentCapacity {

    /** 舱段 id */
    private Long segmentId;

    /** 舱段编码 */
    private String segCode;

    /** 舱段名称 */
    private String segName;

    /** 舱段状态：运营 / 维护 / 停用 */
    private String status;

    /** 舱容（容量舱位上限） */
    private Integer capacitySlots;

    /** 已用舱位 */
    private Integer usedSlots;

    /** 剩余舱位 = 舱容 - 已用 */
    private Integer remainSlots;

    /** 舱内是否已有电力管线（防爆隔离展示用） */
    private boolean hasPower;

    /** 舱内是否已有燃气管线（防爆隔离展示用） */
    private boolean hasGas;

    /** 未解除风险事件数；&gt;0 表示该舱段处于封控 */
    private Integer unresolvedRiskCount;

    /** 当前封控原因（取最近一条未解除事件），未封控为 null */
    private String lockReason;

    /** 逐条管线占用明细 */
    private List<PipelineSlot> items = new ArrayList<>();
}
