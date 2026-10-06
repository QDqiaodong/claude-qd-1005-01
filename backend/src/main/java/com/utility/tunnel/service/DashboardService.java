package com.utility.tunnel.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.utility.tunnel.mapper.FacilityMapper;
import com.utility.tunnel.mapper.TunnelSegmentMapper;

/**
 * 全廊体健康度看板。
 *
 * <p>健康分 = 舱段状态加权均值（运营 100 / 维护 60 / 停用 0），
 * 再按故障设备占比扣减（每个故障设备扣 20/设备总数）。</p>
 */
@Service
public class DashboardService {

    private final TunnelSegmentMapper segmentMapper;
    private final FacilityMapper facilityMapper;

    public DashboardService(TunnelSegmentMapper segmentMapper, FacilityMapper facilityMapper) {
        this.segmentMapper = segmentMapper;
        this.facilityMapper = facilityMapper;
    }

    public Map<String, Object> health() {
        int total = segmentMapper.countAll();
        int operating = segmentMapper.countByStatus("运营");
        int maintaining = segmentMapper.countByStatus("维护");
        int stopped = total - operating - maintaining;

        double segHealth = 100;
        if (total > 0) {
            segHealth = (operating * 100.0 + maintaining * 60.0) / total;
        }

        int totalFac = facilityMapper.countAll();
        int faulty = facilityMapper.countByStatus("故障");
        double score = segHealth;
        if (totalFac > 0) {
            score -= (faulty * 20.0) / totalFac;
        }
        if (score < 0) {
            score = 0;
        }
        if (score > 100) {
            score = 100;
        }

        Map<String, Object> m = new HashMap<>();
        m.put("score", (int) Math.round(score));
        m.put("totalSegments", total);
        m.put("operating", operating);
        m.put("maintaining", maintaining);
        m.put("stopped", stopped);
        m.put("totalFacilities", totalFac);
        m.put("faultyFacilities", faulty);
        return m;
    }
}
