package com.utility.tunnel.entity;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 入廊管线：敷设在某舱段内的市政管线。
 *
 * <p>lineType ∈ {电力, 通信, 给水, 排水, 热力, 燃气}；
 * pressureLevel ∈ {高压, 中压, 低压}：电力/给水/燃气/热力必须标注（热力不得为高压），
 * 通信/排水为无压管线必须为 null；
 * status ∈ {在用, 检修}。
 * 每条管线按「类型 × 压力级制」矩阵占用舱段舱位（见 Dict#slotOf）。</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class Pipeline {

    private Long id;

    /** 所属舱段 id */
    private Long segmentId;

    /** 管线类型：电力 / 通信 / 给水 / 排水 / 热力 / 燃气 */
    private String lineType;

    /** 管径（毫米） */
    private Integer diameterMm;

    /** 压力级制：高压 / 中压 / 低压 */
    private String pressureLevel;

    /** 在用 / 检修 */
    private String status;

    private LocalDateTime createdAt;
}
