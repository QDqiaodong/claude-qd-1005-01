package com.utility.tunnel.entity;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 廊体舱段：综合管廊被划分的舱体段落。
 *
 * <p>断面形状 shape ∈ {圆形, 矩形, 马蹄形}；状态 status ∈ {运营, 维护, 停用}；
 * capacitySlots 为舱容（容量舱位上限），入廊管线按类型×压力级制占用舱位，
 * 已用不得超过舱容。</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class TunnelSegment {

    private Long id;

    /** 舱段编码，唯一 */
    private String segCode;

    /** 舱段名称 */
    private String segName;

    /** 断面形状：圆形 / 矩形 / 马蹄形 */
    private String shape;

    /** 舱段长度（米），必须大于 0 */
    private Double lengthM;

    /** 舱容（容量舱位）：该舱段允许敷设管线占用的舱位总量，必须为正整数 */
    private Integer capacitySlots;

    /** 运营 / 维护 / 停用 */
    private String status;

    private LocalDateTime createdAt;
}
