package com.utility.tunnel.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.utility.tunnel.common.BizException;
import com.utility.tunnel.common.Dict;
import com.utility.tunnel.entity.Facility;
import com.utility.tunnel.entity.PatrolPoint;
import com.utility.tunnel.entity.RiskEvent;
import com.utility.tunnel.entity.RiskEventDetail;
import com.utility.tunnel.entity.RiskEventView;
import com.utility.tunnel.entity.TunnelSegment;
import com.utility.tunnel.mapper.FacilityMapper;
import com.utility.tunnel.mapper.PatrolPointMapper;
import com.utility.tunnel.mapper.RiskEventMapper;
import com.utility.tunnel.mapper.TunnelSegmentMapper;

/**
 * 巡检异常 / 风险事件闭环。
 *
 * <p>核心规则：
 * 1) 登记必须关联舱段、发现对象（巡检点 / 附属设备，且对象必须属于该舱段）、
 *    严重等级、描述与发现时间，新事件一律「待处置」；
 * 2) 状态流转：待处置 → 处理中 → 已解除（待处置可直接解除）；解除必须留处置说明、
 *    解除人与解除时间；
 * 3) 推进与解除都用「带状态条件的 UPDATE」落库：重复提交 / 并发时影响 0 行直接报错，
 *    不会留下半条状态；舱段恢复运营时按实时 COUNT 判定封控，
 *    同舱多条未解除事件必须全部解除后才放行，单条记录的重复解除不会提前放行。</p>
 */
@Service
public class RiskEventService {

    private final RiskEventMapper riskMapper;
    private final TunnelSegmentMapper segmentMapper;
    private final PatrolPointMapper patrolMapper;
    private final FacilityMapper facilityMapper;

    public RiskEventService(RiskEventMapper riskMapper,
                            TunnelSegmentMapper segmentMapper,
                            PatrolPointMapper patrolMapper,
                            FacilityMapper facilityMapper) {
        this.riskMapper = riskMapper;
        this.segmentMapper = segmentMapper;
        this.patrolMapper = patrolMapper;
        this.facilityMapper = facilityMapper;
    }

    public List<RiskEventView> list(Long segmentId, String status) {
        if (status != null && !status.isBlank() && !Dict.isValidRiskStatus(status)) {
            throw new BizException("事件状态不合法，应为：待处置 / 处理中 / 已解除");
        }
        List<RiskEventView> rows = riskMapper.findByFilter(segmentId, status);
        rows.forEach(this::fillLockReason);
        return rows;
    }

    /** 详情：事件 + 关联巡检点 / 设备的基本信息 */
    public RiskEventDetail detail(Long id) {
        RiskEventView v = riskMapper.findViewById(id);
        if (v == null) {
            throw new BizException("风险事件不存在：" + id);
        }
        fillLockReason(v);
        RiskEventDetail d = new RiskEventDetail();
        d.setEvent(v);
        if ("巡检点".equals(v.getSourceType())) {
            d.setPatrolPoint(patrolMapper.selectById(v.getSourceId()));
        } else if ("附属设备".equals(v.getSourceType())) {
            d.setFacility(facilityMapper.selectById(v.getSourceId()));
        }
        return d;
    }

    @Transactional
    public RiskEvent create(RiskEvent input) {
        if (input.getSegmentId() == null) {
            throw new BizException("必须关联舱段");
        }
        // 锁舱段行：与「恢复运营」互斥，避免恢复检查与风险登记并发导致封控判定失真
        TunnelSegment seg = segmentMapper.selectByIdForUpdate(input.getSegmentId());
        if (seg == null) {
            throw new BizException("关联舱段不存在：" + input.getSegmentId());
        }
        if (!Dict.isValidRiskSourceType(input.getSourceType())) {
            throw new BizException("发现对象类型不合法，应为：巡检点 / 附属设备");
        }
        if (input.getSourceId() == null) {
            throw new BizException("必须指定发现对象");
        }
        // 发现对象必须真实存在且属于关联舱段
        if ("巡检点".equals(input.getSourceType())) {
            PatrolPoint p = patrolMapper.selectById(input.getSourceId());
            if (p == null) {
                throw new BizException("发现对象（巡检点）不存在：" + input.getSourceId());
            }
            if (!input.getSegmentId().equals(p.getSegmentId())) {
                throw new BizException("巡检点 " + p.getPointCode() + " 不属于舱段 "
                        + seg.getSegCode() + "，请核对发现对象");
            }
        } else {
            Facility f = facilityMapper.selectById(input.getSourceId());
            if (f == null) {
                throw new BizException("发现对象（附属设备）不存在：" + input.getSourceId());
            }
            if (!input.getSegmentId().equals(f.getSegmentId())) {
                throw new BizException("设备（" + f.getFacilityType() + "）不属于舱段 "
                        + seg.getSegCode() + "，请核对发现对象");
            }
        }
        if (!Dict.isValidRiskSeverity(input.getSeverity())) {
            throw new BizException("严重等级不合法，应为：一般 / 较大 / 重大");
        }
        if (input.getDescription() == null || input.getDescription().isBlank()) {
            throw new BizException("风险描述不能为空");
        }
        if (input.getFoundAt() == null) {
            throw new BizException("发现时间不能为空");
        }
        RiskEvent e = new RiskEvent();
        e.setSegmentId(input.getSegmentId());
        e.setSourceType(input.getSourceType());
        e.setSourceId(input.getSourceId());
        e.setSeverity(input.getSeverity());
        e.setDescription(input.getDescription().trim());
        e.setFoundAt(input.getFoundAt());
        // 新登记一律「待处置」，之后只能经推进 / 解除流转，忽略客户端自带状态
        e.setStatus("待处置");
        riskMapper.insert(e);
        return e;
    }

    /** 推进：待处置 → 处理中（可顺带补处置说明） */
    @Transactional
    public RiskEvent advance(Long id, String handleNote) {
        RiskEvent e = requireEvent(id);
        if (!Dict.canTransferRisk(e.getStatus(), "处理中")) {
            throw new BizException("当前状态「" + e.getStatus() + "」不能推进为处理中");
        }
        String note = (handleNote == null || handleNote.isBlank()) ? null : handleNote.trim();
        if (riskMapper.advance(id, note) == 0) {
            // 并发下状态已被别人改动：影响 0 行，不落半条状态
            throw new BizException("事件状态已变化，请刷新后重试");
        }
        return requireEvent(id);
    }

    /** 解除：待处置 / 处理中 → 已解除，必须留处置说明与解除人，解除时间由服务端写入 */
    @Transactional
    public RiskEvent resolve(Long id, String handleNote, String resolvedBy) {
        RiskEvent e = requireEvent(id);
        if (!Dict.canTransferRisk(e.getStatus(), "已解除")) {
            throw new BizException("事件已解除，请勿重复提交");
        }
        if (handleNote == null || handleNote.isBlank()) {
            throw new BizException("解除风险必须填写处置说明");
        }
        if (resolvedBy == null || resolvedBy.isBlank()) {
            throw new BizException("解除风险必须填写解除人");
        }
        if (riskMapper.resolve(id, handleNote.trim(), resolvedBy.trim(), LocalDateTime.now()) == 0) {
            // 并发重复解除：影响 0 行，直接报错，不会重复放行
            throw new BizException("事件已解除，请勿重复提交");
        }
        return requireEvent(id);
    }

    private RiskEvent requireEvent(Long id) {
        RiskEvent e = riskMapper.selectById(id);
        if (e == null) {
            throw new BizException("风险事件不存在：" + id);
        }
        return e;
    }

    /** 未解除事件构成舱段的当前封控原因 */
    private void fillLockReason(RiskEventView v) {
        if (!"已解除".equals(v.getStatus())) {
            v.setLockReason("【" + v.getSeverity() + "】" + v.getDescription());
        }
    }
}
