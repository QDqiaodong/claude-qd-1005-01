package com.utility.tunnel.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.utility.tunnel.common.BizException;
import com.utility.tunnel.entity.PatrolPoint;
import com.utility.tunnel.mapper.PatrolPointMapper;
import com.utility.tunnel.mapper.TunnelSegmentMapper;

/**
 * 巡检点业务。
 *
 * <p>核心规则：巡检登记需登记点位编码与所属舱段；登记时刷新 lastPatrolAt 为当前时间。</p>
 */
@Service
public class PatrolPointService {

    private final PatrolPointMapper patrolMapper;
    private final TunnelSegmentMapper segmentMapper;

    public PatrolPointService(PatrolPointMapper patrolMapper, TunnelSegmentMapper segmentMapper) {
        this.patrolMapper = patrolMapper;
        this.segmentMapper = segmentMapper;
    }

    public List<PatrolPoint> list(Long segmentId) {
        if (segmentId != null) {
            return patrolMapper.findBySegmentId(segmentId);
        }
        return patrolMapper.findAll();
    }

    public PatrolPoint get(Long id) {
        PatrolPoint p = patrolMapper.selectById(id);
        if (p == null) {
            throw new BizException("巡检点不存在：" + id);
        }
        return p;
    }

    @Transactional
    public PatrolPoint create(PatrolPoint input) {
        if (input.getSegmentId() == null) {
            throw new BizException("必须指定所属舱段");
        }
        if (segmentMapper.selectById(input.getSegmentId()) == null) {
            throw new BizException("所属舱段不存在：" + input.getSegmentId());
        }
        if (input.getPointCode() == null || input.getPointCode().isBlank()) {
            throw new BizException("点位编码不能为空");
        }
        PatrolPoint p = new PatrolPoint();
        p.setSegmentId(input.getSegmentId());
        p.setPointCode(input.getPointCode().trim());
        p.setLocation(input.getLocation() == null ? "" : input.getLocation().trim());
        p.setLastPatrolAt(input.getLastPatrolAt());
        patrolMapper.insert(p);
        return p;
    }

    /** 巡检登记：刷新最近巡检时间为当前时间 */
    @Transactional
    public PatrolPoint register(Long id) {
        PatrolPoint p = get(id);
        p.setLastPatrolAt(LocalDateTime.now());
        patrolMapper.updateLastPatrolAt(p.getId(), p.getLastPatrolAt());
        return p;
    }
}
