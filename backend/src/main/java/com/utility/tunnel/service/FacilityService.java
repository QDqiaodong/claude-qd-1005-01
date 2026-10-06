package com.utility.tunnel.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.utility.tunnel.common.BizException;
import com.utility.tunnel.common.Dict;
import com.utility.tunnel.entity.Facility;
import com.utility.tunnel.mapper.FacilityMapper;
import com.utility.tunnel.mapper.TunnelSegmentMapper;

/**
 * 附属设备业务。
 *
 * <p>核心规则：设备报故障将 status 置为「故障」；修复后恢复为「正常」。
 * 设备类型与状态需在字典范围内。</p>
 */
@Service
public class FacilityService {

    private final FacilityMapper facilityMapper;
    private final TunnelSegmentMapper segmentMapper;

    public FacilityService(FacilityMapper facilityMapper, TunnelSegmentMapper segmentMapper) {
        this.facilityMapper = facilityMapper;
        this.segmentMapper = segmentMapper;
    }

    public List<Facility> list(Long segmentId) {
        if (segmentId != null) {
            return facilityMapper.findBySegmentId(segmentId);
        }
        return facilityMapper.findAll();
    }

    public Facility get(Long id) {
        Facility f = facilityMapper.selectById(id);
        if (f == null) {
            throw new BizException("设备不存在：" + id);
        }
        return f;
    }

    @Transactional
    public Facility create(Facility input) {
        if (input.getSegmentId() == null) {
            throw new BizException("必须指定所属舱段");
        }
        if (segmentMapper.selectById(input.getSegmentId()) == null) {
            throw new BizException("所属舱段不存在：" + input.getSegmentId());
        }
        if (input.getFacilityType() == null || !Dict.isValidFacilityType(input.getFacilityType())) {
            throw new BizException("设备类型不合法，应为：风机 / 排水泵 / 照明 / 监控");
        }
        if (input.getStatus() == null || !Dict.isValidFacilityStatus(input.getStatus())) {
            throw new BizException("设备状态不合法，应为：正常 / 故障");
        }
        Facility f = new Facility();
        f.setSegmentId(input.getSegmentId());
        f.setFacilityType(input.getFacilityType());
        f.setStatus(input.getStatus());
        facilityMapper.insert(f);
        return f;
    }

    /** 报故障 */
    @Transactional
    public Facility reportFault(Long id) {
        Facility f = get(id);
        f.setStatus("故障");
        facilityMapper.updateStatus(f.getId(), "故障");
        return f;
    }

    /** 修复恢复 */
    @Transactional
    public Facility restore(Long id) {
        Facility f = get(id);
        f.setStatus("正常");
        facilityMapper.updateStatus(f.getId(), "正常");
        return f;
    }
}
