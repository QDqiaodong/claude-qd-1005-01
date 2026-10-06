package com.utility.tunnel.entity;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 附属设备：保障管廊运营的机电设备。
 *
 * <p>facilityType ∈ {风机, 排水泵, 照明, 监控}；status ∈ {正常, 故障}。</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class Facility {

    private Long id;

    /** 所属舱段 id */
    private Long segmentId;

    /** 设备类型：风机 / 排水泵 / 照明 / 监控 */
    private String facilityType;

    /** 正常 / 故障 */
    private String status;

    private LocalDateTime createdAt;
}
