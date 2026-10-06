package com.utility.tunnel.entity;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 巡检异常 / 风险事件：值守人员从巡检点或附属设备登记的舱段风险。
 *
 * <p>状态流转：待处置 → 处理中 → 已解除（待处置也可直接解除，已解除为终态）。
 * 存在未解除事件的舱段处于封控状态，不能从「维护/停用」恢复为「运营」。</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class RiskEvent {

    private Long id;

    /** 关联舱段 id */
    private Long segmentId;

    /** 发现对象类型：巡检点 / 附属设备 */
    private String sourceType;

    /** 发现对象 id（patrol_point.id 或 facility.id） */
    private Long sourceId;

    /** 严重等级：一般 / 较大 / 重大 */
    private String severity;

    /** 风险描述 */
    private String description;

    /** 发现时间 */
    private LocalDateTime foundAt;

    /** 待处置 / 处理中 / 已解除 */
    private String status;

    /** 处置说明 */
    private String handleNote;

    /** 解除人（解除时必填） */
    private String resolvedBy;

    /** 解除时间（解除时由服务端写入） */
    private LocalDateTime resolvedAt;

    private LocalDateTime createdAt;
}
