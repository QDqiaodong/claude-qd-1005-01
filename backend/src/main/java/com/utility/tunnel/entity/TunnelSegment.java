package com.utility.tunnel.entity;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 廊体舱段：综合管廊被划分的舱体段落。
 *
 * <p>断面形状 shape ∈ {圆形, 矩形, 马蹄形}；状态 status ∈ {运营, 维护, 停用}。</p>
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

    /** 舱段容量（容量单位），必须大于 0；已用容量 = 舱内管线占用之和 */
    private Integer capacity;

    /** 运营 / 维护 / 停用 */
    private String status;

    private LocalDateTime createdAt;
}
