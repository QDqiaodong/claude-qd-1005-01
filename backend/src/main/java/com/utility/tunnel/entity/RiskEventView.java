package com.utility.tunnel.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 风险事件列表视图：事件本体 + 舱段与发现对象的展示字段 + 当前封控原因。
 *
 * <p>lockReason 仅在该事件未解除（待处置/处理中）时有值，
 * 即该事件当前对所属舱段构成的封控原因。</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class RiskEventView extends RiskEvent {

    /** 关联舱段编码 */
    private String segCode;

    /** 关联舱段名称 */
    private String segName;

    /** 发现对象名称：点位编码（巡检点）或设备类型（附属设备） */
    private String sourceName;

    /** 当前封控原因：未解除时为「【严重等级】描述」，已解除为 null */
    private String lockReason;
}
