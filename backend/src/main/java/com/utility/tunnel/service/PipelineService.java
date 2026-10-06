package com.utility.tunnel.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.utility.tunnel.common.BizException;
import com.utility.tunnel.common.Dict;
import com.utility.tunnel.entity.Pipeline;
import com.utility.tunnel.mapper.PipelineMapper;
import com.utility.tunnel.mapper.TunnelSegmentMapper;

/**
 * 入廊管线业务。
 *
 * <p>核心规则：同舱段已敷设「电力」管线时，禁止再敷设「燃气」管线（防爆隔离，须分舱敷设），
 * 否则抛出中文业务异常。管径须为正数，压力级制与状态需在字典范围内。</p>
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

    @Transactional
    public Pipeline create(Pipeline input) {
        if (input.getSegmentId() == null) {
            throw new BizException("必须指定所属舱段");
        }
        if (segmentMapper.selectById(input.getSegmentId()) == null) {
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
        // 防爆隔离校验：同舱段已有电力管线时，禁止同舱敷设燃气
        if ("燃气".equals(input.getLineType())
                && pipelineMapper.countBySegmentAndType(input.getSegmentId(), "电力") > 0) {
            throw new BizException("同舱段已敷设电力管线，燃气须防爆隔离，禁止同舱敷设");
        }
        Pipeline p = new Pipeline();
        p.setSegmentId(input.getSegmentId());
        p.setLineType(input.getLineType());
        p.setDiameterMm(input.getDiameterMm());
        p.setPressureLevel(input.getPressureLevel());
        p.setStatus(input.getStatus());
        pipelineMapper.insert(p);
        return p;
    }
}
