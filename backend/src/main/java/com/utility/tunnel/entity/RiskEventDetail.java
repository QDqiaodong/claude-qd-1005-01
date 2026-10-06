package com.utility.tunnel.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 风险事件详情：事件视图 + 关联发现对象（巡检点 / 附属设备）的基本信息。
 *
 * <p>patrolPoint 与 facility 按 sourceType 二选一有值。</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class RiskEventDetail {

    private RiskEventView event;

    /** 来源为「巡检点」时有值 */
    private PatrolPoint patrolPoint;

    /** 来源为「附属设备」时有值 */
    private Facility facility;
}
