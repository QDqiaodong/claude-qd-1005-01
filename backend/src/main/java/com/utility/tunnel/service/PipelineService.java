package com.utility.tunnel.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.utility.tunnel.common.BizException;
import com.utility.tunnel.common.Dict;
import com.utility.tunnel.entity.Pipeline;
import com.utility.tunnel.entity.SegmentUsage;
import com.utility.tunnel.entity.TunnelSegment;
import com.utility.tunnel.mapper.PipelineMapper;
import com.utility.tunnel.mapper.TunnelSegmentMapper;

/**
 * 入廊管线业务。
 *
 * <p>核心规则（均为命名规则，触发时异常信息带规则名）：</p>
 * <ul>
 *   <li>「防爆隔离」：同舱段电力与燃气须分舱敷设（双向），已有其一时禁止另一条入廊；</li>
 *   <li>「压力级制适配」：给水 / 热力 / 燃气为压力管线必须填写压力级制；排水为重力流不得使用高压；</li>
 *   <li>「舱段容量」：本次敷设占用（按管线类型 + 压力级制计算）不得超过舱段剩余容量，
 *       已用容量 = 舱内管线占用之和，实时 SUM 自 pipeline 表。</li>
 * </ul>
 *
 * <p>落库在舱段行锁（FOR UPDATE）内完成：同舱段并发敷设被串行化，后到者按最新
 * 已用容量重新判定，超容量或不兼容的请求整体回滚，不会留下半条数据；
 * 事务隔离用 READ_COMMITTED，保证锁等待结束后读到的是最新已提交的占用。</p>
 */
@Service
public class PipelineService {

    private final PipelineMapper pipelineMapper;
    private final TunnelSegmentMapper segmentMapper;

    public PipelineService(PipelineMapper pipelineMapper, TunnelSegmentMapper segmentMapper) {
        this.pipelineMapper = pipelineMapper;
        this.segmentMapper = segmentMapper;
    }

    public List<Pipeline> list(Long segmentId) {
        if (segmentId != null) {
            return pipelineMapper.findBySegmentId(segmentId);
        }
        return pipelineMapper.findAll();
    }

    public Pipeline get(Long id) {
        Pipeline p = pipelineMapper.selectById(id);
        if (p == null) {
            throw new BizException("管线不存在：" + id);
        }
        return p;
    }

    /** 舱段容量用量汇总：实时聚合 pipeline 表，刷新后与已存管线一致 */
    public List<SegmentUsage> usage(Long segmentId) {
        List<SegmentUsage> rows = pipelineMapper.findUsage(segmentId);
        rows.forEach(u -> u.setRemaining(u.getCapacity() - u.getUsed()));
        return rows;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Pipeline create(Pipeline input) {
        if (input.getSegmentId() == null) {
            throw new BizException("必须指定所属舱段");
        }
        // 行锁舱段：容量汇总、隔离校验、写入都在锁内完成，同舱段并发敷设串行判定
        TunnelSegment seg = segmentMapper.selectByIdForUpdate(input.getSegmentId());
        if (seg == null) {
            throw new BizException("所属舱段不存在：" + input.getSegmentId());
        }
        if (input.getLineType() == null || !Dict.isValidLineType(input.getLineType())) {
            throw new BizException("管线类型不合法，应为：电力 / 通信 / 给水 / 排水 / 热力 / 燃气");
        }
        if (input.getDiameterMm() != null && input.getDiameterMm() <= 0) {
            throw new BizException("管径必须大于 0");
        }
        if (input.getPressureLevel() != null && !input.getPressureLevel().isBlank()
                && !Dict.isValidPressureLevel(input.getPressureLevel())) {
            throw new BizException("压力级制不合法，应为：高压 / 中压 / 低压");
        }
        if (input.getStatus() == null || !Dict.isValidPipelineStatus(input.getStatus())) {
            throw new BizException("管线状态不合法，应为：在用 / 检修");
        }
        // 规则「压力级制适配」：压力管线必填压力级制；排水（重力流）不得使用高压
        if (Dict.PRESSURE_REQUIRED_TYPES.contains(input.getLineType())
                && (input.getPressureLevel() == null || input.getPressureLevel().isBlank())) {
            throw new BizException("触发规则「压力级制适配」：" + input.getLineType()
                    + "为压力管线，必须填写压力级制（高压 / 中压 / 低压）");
        }
        if ("排水".equals(input.getLineType()) && "高压".equals(input.getPressureLevel())) {
            throw new BizException("触发规则「压力级制适配」：排水为重力流管线，不得使用高压");
        }
        // 规则「防爆隔离」：同舱段电力与燃气须分舱敷设（双向判定）
        if ("燃气".equals(input.getLineType())
                && pipelineMapper.countBySegmentAndType(input.getSegmentId(), "电力") > 0) {
            throw new BizException("触发规则「防爆隔离」：同舱段已敷设电力管线，燃气须分舱敷设，禁止同舱敷设");
        }
        if ("电力".equals(input.getLineType())
                && pipelineMapper.countBySegmentAndType(input.getSegmentId(), "燃气") > 0) {
            throw new BizException("触发规则「防爆隔离」：同舱段已敷设燃气管线，电力须分舱敷设，禁止同舱敷设");
        }
        // 规则「舱段容量」：本次占用不得超过剩余容量（已用容量在锁内实时汇总）
        int occupancy = Dict.pipelineOccupancy(input.getLineType(), input.getPressureLevel());
        int used = pipelineMapper.sumOccupancyBySegment(input.getSegmentId());
        int remaining = seg.getCapacity() - used;
        if (occupancy > remaining) {
            throw new BizException("触发规则「舱段容量」：舱段 " + seg.getSegCode()
                    + " 剩余容量 " + remaining + "，本次敷设需占用 " + occupancy
                    + "（已用 " + used + " / 总容量 " + seg.getCapacity() + "），超容量禁止入廊");
        }
        Pipeline p = new Pipeline();
        p.setSegmentId(input.getSegmentId());
        p.setLineType(input.getLineType());
        p.setDiameterMm(input.getDiameterMm());
        p.setPressureLevel(input.getPressureLevel());
        p.setOccupancy(occupancy);
        p.setStatus(input.getStatus());
        pipelineMapper.insert(p);
        return p;
    }
}
