package com.utility.tunnel.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.utility.tunnel.common.BizException;
import com.utility.tunnel.common.Dict;
import com.utility.tunnel.entity.RiskEventView;
import com.utility.tunnel.entity.SegmentWithChildren;
import com.utility.tunnel.entity.TunnelSegment;
import com.utility.tunnel.mapper.RiskEventMapper;
import com.utility.tunnel.mapper.TunnelSegmentMapper;

/**
 * 廊体舱段业务。
 *
 * <p>核心规则：新建舱段长度 lengthM 必须大于 0、舱容 capacitySlots（容量舱位上限）
 * 必须为正整数；断面形状与状态需在字典范围内。
 * 状态恢复闭环：舱段存在未解除风险事件时处于封控状态，
 * 不能从「维护/停用」恢复为「运营」。</p>
 */
@Service
public class TunnelSegmentService {

    private final TunnelSegmentMapper segmentMapper;
    private final RiskEventMapper riskEventMapper;

    public TunnelSegmentService(TunnelSegmentMapper segmentMapper, RiskEventMapper riskEventMapper) {
        this.segmentMapper = segmentMapper;
        this.riskEventMapper = riskEventMapper;
    }

    public List<TunnelSegment> list(String status) {
        if (status != null && !status.isBlank()) {
            return segmentMapper.findByStatus(status);
        }
        return segmentMapper.findAll();
    }

    public TunnelSegment get(Long id) {
        TunnelSegment s = segmentMapper.selectById(id);
        if (s == null) {
            throw new BizException("舱段不存在：" + id);
        }
        return s;
    }

    /** 一条 SQL 带出舱段及其全部子对象（入廊管线 / 巡检点 / 附属设备） */
    public SegmentWithChildren withChildren(Long id) {
        SegmentWithChildren s = segmentMapper.selectWithChildren(id);
        if (s == null) {
            throw new BizException("舱段不存在：" + id);
        }
        return s;
    }

    @Transactional
    public TunnelSegment create(TunnelSegment input) {
        if (input.getSegCode() == null || input.getSegCode().isBlank()) {
            throw new BizException("舱段编码不能为空");
        }
        if (input.getLengthM() == null || input.getLengthM() <= 0) {
            throw new BizException("舱段长度必须大于 0");
        }
        if (!Dict.isValidCapacity(input.getCapacitySlots())) {
            throw new BizException("舱容（容量舱位）必须为正整数");
        }
        if (input.getShape() == null || !Dict.isValidSegmentShape(input.getShape())) {
            throw new BizException("断面形状不合法，应为：圆形 / 矩形 / 马蹄形");
        }
        if (input.getStatus() == null || !Dict.isValidSegmentStatus(input.getStatus())) {
            throw new BizException("舱段状态不合法，应为：运营 / 维护 / 停用");
        }
        TunnelSegment s = new TunnelSegment();
        s.setSegCode(input.getSegCode().trim());
        s.setSegName(input.getSegName() == null ? "" : input.getSegName().trim());
        s.setShape(input.getShape());
        s.setLengthM(input.getLengthM());
        s.setCapacitySlots(input.getCapacitySlots());
        s.setStatus(input.getStatus());
        segmentMapper.insert(s);
        return s;
    }

    /**
     * 舱段状态变更。
     *
     * <p>封控闭环：恢复为「运营」前按实时 COUNT 校验本舱段无未解除风险事件，
     * 否则抛中文业务异常并附当前封控原因。行锁（FOR UPDATE）使恢复与风险登记互斥；
     * 封控判定不依赖任何计数器，同舱多条未解除事件必须全部解除才放行，
     * 单条事件的重复解除提交不会提前放行。</p>
     */
    @Transactional
    public TunnelSegment changeStatus(Long id, String target) {
        if (target == null || !Dict.isValidSegmentStatus(target)) {
            throw new BizException("目标状态不合法，应为：运营 / 维护 / 停用");
        }
        TunnelSegment s = segmentMapper.selectByIdForUpdate(id);
        if (s == null) {
            throw new BizException("舱段不存在：" + id);
        }
        if (target.equals(s.getStatus())) {
            throw new BizException("舱段已处于「" + target + "」状态");
        }
        if ("运营".equals(target)) {
            int open = riskEventMapper.countUnresolvedBySegment(id);
            if (open > 0) {
                RiskEventView latest = riskEventMapper.findLatestUnresolved(id);
                String reason = latest == null ? ""
                        : "；当前封控原因：【" + latest.getSeverity() + "】" + latest.getDescription();
                throw new BizException("舱段存在 " + open + " 条未解除风险事件，封控中，不能恢复运营" + reason);
            }
        }
        segmentMapper.updateStatus(id, target);
        s.setStatus(target);
        return s;
    }
}
