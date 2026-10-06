package com.utility.tunnel.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 「本次拟入廊管线」的容量与规格兼容校核结果。
 *
 * <p>表单提示（提交前预检）与后端落库共用同一条规则链：
 * compatible=true 表示可入廊；false 时 ruleCode 给出被触发的规则编号，
 * message 给出中文原因，前端据此禁用提交按钮并说明是哪条规则。</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class CapacityCheck {

    /** 是否兼容（容量、规格、隔离、封控、舱段状态全部通过才为 true） */
    private boolean compatible;

    /** 被触发的规则编号，如 CAPACITY_OVERFLOW / ISOLATION_GAS_WITH_POWER */
    private String ruleCode;

    /** 规则中文名（用于页面展示“触发了哪条规则”） */
    private String ruleName;

    /** 中文不兼容原因 / 通过提示 */
    private String message;

    /** 本次拟占用舱位（规格不兼容无法计算时为 null） */
    private Integer requestSlots;

    /** 校核时刻的舱段用量 */
    private SegmentCapacity capacity;

    public static CapacityCheck ok(SegmentCapacity capacity, Integer requestSlots) {
        CapacityCheck c = new CapacityCheck();
        c.setCompatible(true);
        c.setRuleCode("OK");
        c.setRuleName("校核通过");
        c.setMessage("校核通过：本次占用 " + requestSlots + " 舱位，入廊后已用 "
                + (capacity.getUsedSlots() + requestSlots) + "/" + capacity.getCapacitySlots());
        c.setRequestSlots(requestSlots);
        c.setCapacity(capacity);
        return c;
    }

    public static CapacityCheck reject(String ruleCode, String ruleName, String message,
                                       SegmentCapacity capacity, Integer requestSlots) {
        CapacityCheck c = new CapacityCheck();
        c.setCompatible(false);
        c.setRuleCode(ruleCode);
        c.setRuleName(ruleName);
        c.setMessage(message);
        c.setRequestSlots(requestSlots);
        c.setCapacity(capacity);
        return c;
    }
}
