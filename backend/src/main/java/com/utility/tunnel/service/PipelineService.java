package com.utility.tunnel.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.utility.tunnel.common.BizException;
import com.utility.tunnel.common.Dict;
import com.utility.tunnel.entity.CapacityCheck;
import com.utility.tunnel.entity.Pipeline;
import com.utility.tunnel.entity.PipelineSlot;
import com.utility.tunnel.entity.RiskEventView;
import com.utility.tunnel.entity.SegmentCapacity;
import com.utility.tunnel.entity.TunnelSegment;
import com.utility.tunnel.mapper.PipelineMapper;
import com.utility.tunnel.mapper.RiskEventMapper;
import com.utility.tunnel.mapper.TunnelSegmentMapper;

/**
 * 入廊管线业务：舱段容量与规格兼容校核。
 *
 * <p>容量口径：舱段有「舱容 capacitySlots」（容量舱位上限），每条管线按
 * 「管线类型 × 压力级制」矩阵（{@link Dict#slotOf}）占用舱位——
 * 电力 2/1/1、给水 3/2/1、燃气 4/3/2、热力 2/1（热力不允许高压）、通信/排水固定 1。
 * 已用 / 剩余均实时遍历舱内管线计算，不维护计数器。</p>
 *
 * <p>入廊前必须依次通过同一条规则链（预检 {@link #check} 与落库 {@link #create} 共用）：
 * 规格兼容（带规则编号）→ 防爆隔离（电力/燃气双向禁同舱）→ 舱容不超限 →
 * 舱段未因未解除风险事件封控 → 舱段非「停用」。任何一条不满足，超容量或不兼容请求
 * 都不会落库；create 持舱段行锁执行，同一舱段并发提交两条管线时只有满足剩余容量与
 * 隔离规则的一条成功，另一条被拒且不会留下半条数据（单条 insert + 事务回滚）。</p>
 */
@Service
public class PipelineService {

    // ---------------- 规则编号与中文名（触发时明确报出是哪条规则） ----------------
    public static final String RULE_PRESSURE_REQUIRED = "PRESSURE_REQUIRED";
    public static final String RULE_PRESSURE_FORBIDDEN = "PRESSURE_FORBIDDEN";
    public static final String RULE_DRAIN_GRAVITY = "DRAIN_GRAVITY";
    public static final String RULE_HEAT_NO_HIGH = "HEAT_NO_HIGH";
    public static final String RULE_DIAMETER_REQUIRED = "DIAMETER_REQUIRED";
    public static final String RULE_DIAMETER_OVERSIZE = "DIAMETER_OVERSIZE";
    public static final String RULE_ISOLATION_GAS = "ISOLATION_GAS_WITH_POWER";
    public static final String RULE_ISOLATION_POWER = "ISOLATION_POWER_WITH_GAS";
    public static final String RULE_CAPACITY_OVERFLOW = "CAPACITY_OVERFLOW";
    public static final String RULE_SEGMENT_LOCKED = "SEGMENT_LOCKED_BY_RISK";
    public static final String RULE_SEGMENT_STOPPED = "SEGMENT_STOPPED";

    private static final Map<String, String> RULE_NAMES = Map.ofEntries(
            Map.entry(RULE_PRESSURE_REQUIRED, "压力管线必须标注压力级制"),
            Map.entry(RULE_PRESSURE_FORBIDDEN, "无压管线不得标注压力级制"),
            Map.entry(RULE_DRAIN_GRAVITY, "排水为重力流管线"),
            Map.entry(RULE_HEAT_NO_HIGH, "热力介质不得按高压敷设"),
            Map.entry(RULE_DIAMETER_REQUIRED, "管径必填且须为正数"),
            Map.entry(RULE_DIAMETER_OVERSIZE, "管径超出该类型允许上限"),
            Map.entry(RULE_ISOLATION_GAS, "防爆隔离：燃气禁与电力同舱"),
            Map.entry(RULE_ISOLATION_POWER, "防爆隔离：电力禁与燃气同舱"),
            Map.entry(RULE_CAPACITY_OVERFLOW, "舱段容量超限"),
            Map.entry(RULE_SEGMENT_LOCKED, "舱段风险封控"),
            Map.entry(RULE_SEGMENT_STOPPED, "舱段已停用"));

    private final PipelineMapper pipelineMapper;
    private final TunnelSegmentMapper segmentMapper;
    private final RiskEventMapper riskEventMapper;

    public PipelineService(PipelineMapper pipelineMapper,
                           TunnelSegmentMapper segmentMapper,
                           RiskEventMapper riskEventMapper) {
        this.pipelineMapper = pipelineMapper;
        this.segmentMapper = segmentMapper;
        this.riskEventMapper = riskEventMapper;
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

    /** 全部舱段用量（列表汇总），与各舱段已存管线实时一致 */
    public List<SegmentCapacity> capacities() {
        List<TunnelSegment> segments = segmentMapper.findAll();
        List<SegmentCapacity> result = new ArrayList<>(segments.size());
        for (TunnelSegment s : segments) {
            result.add(buildCapacity(s, pipelineMapper.findBySegmentId(s.getId())));
        }
        return result;
    }

    /** 指定舱段用量（表单提示：已用 / 剩余 / 逐条占用） */
    public SegmentCapacity capacity(Long segmentId) {
        TunnelSegment s = segmentMapper.selectById(segmentId);
        if (s == null) {
            throw new BizException("所属舱段不存在：" + segmentId);
        }
        return buildCapacity(s, pipelineMapper.findBySegmentId(segmentId));
    }

    /**
     * 提交前预检：不落库，返回是否兼容、被触发的规则编号与中文原因。
     * 供表单提示使用，规则链与 {@link #create} 完全相同（只读，不加行锁）。
     */
    public CapacityCheck check(Pipeline input) {
        Long segmentId = input.getSegmentId();
        if (segmentId == null) {
            return CapacityCheck.reject("SEGMENT_REQUIRED", "必须指定所属舱段",
                    "必须指定所属舱段", null, null);
        }
        TunnelSegment seg = segmentMapper.selectById(segmentId);
        if (seg == null) {
            return CapacityCheck.reject("SEGMENT_NOT_FOUND", "所属舱段不存在",
                    "所属舱段不存在：" + segmentId, null, null);
        }
        SegmentCapacity cap = buildCapacity(seg, pipelineMapper.findBySegmentId(segmentId));
        return evaluate(input, cap);
    }

    /**
     * 落库：持舱段行锁（FOR UPDATE）重算用量并走同一条规则链，
     * 使同舱并发提交串行化——后到者按先到者已占用的容量重新校核，
     * 超容量/不兼容即抛业务异常回滚，不会留下半条管线。
     */
    @Transactional
    public Pipeline create(Pipeline input) {
        if (input.getSegmentId() == null) {
            throw new BizException("必须指定所属舱段");
        }
        TunnelSegment seg = segmentMapper.selectByIdForUpdate(input.getSegmentId());
        if (seg == null) {
            throw new BizException("所属舱段不存在：" + input.getSegmentId());
        }
        SegmentCapacity cap = buildCapacity(seg, pipelineMapper.findBySegmentId(input.getSegmentId()));
        CapacityCheck result = evaluate(input, cap);
        if (!result.isCompatible()) {
            // 与预检同一文案，带规则编号：后端落库是最后一道闸
            throw new BizException(result.getMessage() + "（规则：" + result.getRuleCode()
                    + " " + result.getRuleName() + "）");
        }
        Pipeline p = new Pipeline();
        p.setSegmentId(input.getSegmentId());
        p.setLineType(input.getLineType());
        p.setDiameterMm(input.getDiameterMm());
        p.setPressureLevel(normalizePressure(input));
        p.setStatus(input.getStatus());
        pipelineMapper.insert(p);
        return p;
    }

    // ---------------- 规则链（预检 / 落库共用） ----------------

    /** 依次校核规格、隔离、容量、封控、舱段状态；命中第一条不兼容规则即返回。 */
    private CapacityCheck evaluate(Pipeline input, SegmentCapacity cap) {
        String lineType = input.getLineType();
        Integer diameter = input.getDiameterMm();
        String pressure = normalizePressure(input);

        // 字典合法性（不属于“规格兼容规则”，保持基础参数提示）
        if (!Dict.isValidLineType(lineType)) {
            return reject("LINE_TYPE_INVALID", "管线类型不合法",
                    "管线类型不合法，应为：电力 / 通信 / 给水 / 排水 / 热力 / 燃气", cap, null);
        }
        if (pressure != null && !Dict.isValidPressureLevel(pressure)) {
            return reject("PRESSURE_LEVEL_INVALID", "压力级制不合法",
                    "压力级制不合法，应为：高压 / 中压 / 低压", cap, null);
        }
        if (!Dict.isValidPipelineStatus(input.getStatus())) {
            return reject("PIPELINE_STATUS_INVALID", "管线状态不合法",
                    "管线状态不合法，应为：在用 / 检修", cap, null);
        }

        // ---- 规格兼容规则（提示文案只描述原因；规则编号由 create 统一追加、check 单独返回） ----
        if (diameter == null) {
            return reject(RULE_DIAMETER_REQUIRED, RULE_NAMES.get(RULE_DIAMETER_REQUIRED),
                    "管径必填且必须为正整数", cap, null);
        }
        if (diameter <= 0) {
            return reject(RULE_DIAMETER_REQUIRED, RULE_NAMES.get(RULE_DIAMETER_REQUIRED),
                    "管径必须大于 0", cap, null);
        }
        int max = Dict.maxDiameterMm(lineType);
        if (diameter > max) {
            return reject(RULE_DIAMETER_OVERSIZE, RULE_NAMES.get(RULE_DIAMETER_OVERSIZE),
                    lineType + "管线管径 " + diameter + "mm 超出该类型允许上限 " + max + "mm", cap, null);
        }
        if (Dict.isPressureRequired(lineType) && pressure == null) {
            return reject(RULE_PRESSURE_REQUIRED, RULE_NAMES.get(RULE_PRESSURE_REQUIRED),
                    lineType + "为压力管线，入廊必须标注高压/中压/低压压力级制", cap, null);
        }
        if (Dict.isPressureForbidden(lineType) && pressure != null) {
            // 通信为无压弱电管线、排水为重力流管线（DRAIN_GRAVITY），均不得标注压力级制
            if ("排水".equals(lineType)) {
                return reject(RULE_DRAIN_GRAVITY, RULE_NAMES.get(RULE_DRAIN_GRAVITY),
                        "排水为重力流管线，不得标注压力级制「" + pressure + "」", cap, null);
            }
            return reject(RULE_PRESSURE_FORBIDDEN, RULE_NAMES.get(RULE_PRESSURE_FORBIDDEN),
                    lineType + "为无压管线，不得标注压力级制「" + pressure + "」", cap, null);
        }
        // 矩阵兜底：类型×压力级制组合不存在（热力-高压 HEAT_NO_HIGH；绕过前端的非法组合）
        int requestSlots = Dict.slotOf(lineType, pressure);
        if (requestSlots <= 0) {
            if ("热力".equals(lineType) && "高压".equals(pressure)) {
                return reject(RULE_HEAT_NO_HIGH, RULE_NAMES.get(RULE_HEAT_NO_HIGH),
                        "热力介质（热水/蒸汽）不得按高压敷设，请改为中压/低压", cap, null);
            }
            return reject(RULE_PRESSURE_FORBIDDEN, RULE_NAMES.get(RULE_PRESSURE_FORBIDDEN),
                    lineType + "管线不允许「" + pressure + "」敷设", cap, null);
        }

        // ---- 防爆隔离（电力 ⇄ 燃气 双向） ----
        if ("燃气".equals(lineType) && cap.isHasPower()) {
            return reject(RULE_ISOLATION_GAS, RULE_NAMES.get(RULE_ISOLATION_GAS),
                    "舱段 " + cap.getSegCode() + " 已敷设电力管线，燃气须防爆隔离、分舱敷设，禁止同舱入廊",
                    cap, requestSlots);
        }
        if ("电力".equals(lineType) && cap.isHasGas()) {
            return reject(RULE_ISOLATION_POWER, RULE_NAMES.get(RULE_ISOLATION_POWER),
                    "舱段 " + cap.getSegCode() + " 已敷设燃气管线，电力须防爆隔离、分舱敷设，禁止同舱入廊",
                    cap, requestSlots);
        }

        // ---- 风险封控（未解除风险事件，容量功能不得绕过封控） ----
        if (cap.getUnresolvedRiskCount() != null && cap.getUnresolvedRiskCount() > 0) {
            String reason = cap.getLockReason() == null ? "" : "；当前封控原因：" + cap.getLockReason();
            return reject(RULE_SEGMENT_LOCKED, RULE_NAMES.get(RULE_SEGMENT_LOCKED),
                    "舱段 " + cap.getSegCode() + " 存在 " + cap.getUnresolvedRiskCount()
                            + " 条未解除风险事件，封控中，禁止新管线入廊" + reason, cap, requestSlots);
        }

        // ---- 舱段状态：停用禁止入廊（维护舱无未解除风险时允许检修性敷设） ----
        if ("停用".equals(cap.getStatus())) {
            return reject(RULE_SEGMENT_STOPPED, RULE_NAMES.get(RULE_SEGMENT_STOPPED),
                    "舱段 " + cap.getSegCode() + " 已停用，禁止新管线入廊", cap, requestSlots);
        }

        // ---- 容量上限 ----
        if (cap.getUsedSlots() + requestSlots > cap.getCapacitySlots()) {
            return reject(RULE_CAPACITY_OVERFLOW, RULE_NAMES.get(RULE_CAPACITY_OVERFLOW),
                    "舱段 " + cap.getSegCode() + " 舱容不足：舱容 " + cap.getCapacitySlots()
                            + "、已用 " + cap.getUsedSlots() + "、剩余 " + cap.getRemainSlots()
                            + "，本次管线需占用 " + requestSlots + " 舱位", cap, requestSlots);
        }

        return CapacityCheck.ok(cap, requestSlots);
    }

    // ---------------- 容量装配 ----------------

    /** 实时遍历舱内管线计算已用/剩余、防爆隔离标记、封控原因；不读任何计数器。 */
    private SegmentCapacity buildCapacity(TunnelSegment seg, List<Pipeline> pipelines) {
        SegmentCapacity c = new SegmentCapacity();
        c.setSegmentId(seg.getId());
        c.setSegCode(seg.getSegCode());
        c.setSegName(seg.getSegName());
        c.setStatus(seg.getStatus());
        c.setCapacitySlots(seg.getCapacitySlots());

        int used = 0;
        boolean hasPower = false;
        boolean hasGas = false;
        List<PipelineSlot> items = new ArrayList<>(pipelines.size());
        for (Pipeline p : pipelines) {
            int slots = Dict.slotOf(p.getLineType(), p.getPressureLevel());
            if (slots <= 0) {
                // 历史/异常数据按 1 舱位保守计入，保证容量汇总不为负且不把管线“算丢”
                slots = 1;
            }
            used += slots;
            hasPower |= "电力".equals(p.getLineType());
            hasGas |= "燃气".equals(p.getLineType());
            items.add(new PipelineSlot(p.getId(), p.getLineType(), p.getPressureLevel(), slots));
        }
        c.setUsedSlots(used);
        c.setRemainSlots(seg.getCapacitySlots() - used);
        c.setHasPower(hasPower);
        c.setHasGas(hasGas);
        c.setItems(items);

        int open = riskEventMapper.countUnresolvedBySegment(seg.getId());
        c.setUnresolvedRiskCount(open);
        if (open > 0) {
            RiskEventView latest = riskEventMapper.findLatestUnresolved(seg.getId());
            if (latest != null) {
                c.setLockReason("【" + latest.getSeverity() + "】" + latest.getDescription());
            }
        }
        return c;
    }

    /** 空白压力级制按无压（null）处理 */
    private String normalizePressure(Pipeline input) {
        String p = input.getPressureLevel();
        return (p == null || p.isBlank()) ? null : p.trim();
    }

    private CapacityCheck reject(String code, String ruleName, String message,
                                 SegmentCapacity cap, Integer requestSlots) {
        return CapacityCheck.reject(code, ruleName, message, cap, requestSlots);
    }
}
