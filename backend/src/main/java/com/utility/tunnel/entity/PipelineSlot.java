package com.utility.tunnel.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 单条已敷管线的容量占用行（舱段容量明细）。
 *
 * <p>占用舱位 slots 由「管线类型 × 压力级制」矩阵（{@link com.utility.tunnel.common.Dict#slotOf}）
 * 统一计算，前端表单提示、列表汇总与后端落库共用同一份规则。</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class PipelineSlot {

    /** 管线 id */
    private Long id;

    /** 管线类型 */
    private String lineType;

    /** 压力级制（无压管线为 null） */
    private String pressureLevel;

    /** 占用舱位 */
    private Integer slots;

    public PipelineSlot(Long id, String lineType, String pressureLevel, Integer slots) {
        this.id = id;
        this.lineType = lineType;
        this.pressureLevel = pressureLevel;
        this.slots = slots;
    }
}
