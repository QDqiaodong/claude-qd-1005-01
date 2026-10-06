package com.utility.tunnel.common;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 船舶修造船厂词典与业务规则参数表：船台船坞类型与状态流转、船舶类型、船东类型、
 * 修造合同类型与状态流转、分段类型与状态流转、起重设备类型、搭载搭载状态、
 * 报验项目、报验结论、船检机构、遗留项严重度与状态，以及各类阈值。
 *
 * <p>规则集中在此，便于出题时被要求调整或扩展。</p>
 */
public final class Dict {

    private Dict() {
    }

    // ---------------- 船台船坞类型 ----------------
    public static final List<String> BERTH_TYPES = List.of("SLIPWAY", "DRYDOCK", "WHARF");

    private static final Map<String, String> BERTH_TYPE_NAME = Map.of(
            "SLIPWAY", "船台",
            "DRYDOCK", "船坞",
            "WHARF", "舾装码头");

    // ---------------- 船台船坞状态：空闲 → 占用 → 检修 → 空闲 ----------------
    public static final List<String> BERTH_STATUSES = List.of("IDLE", "OCCUPIED", "MAINTENANCE");

    private static final Map<String, String> BERTH_STATUS_NAME = Map.of(
            "IDLE", "空闲",
            "OCCUPIED", "占用",
            "MAINTENANCE", "检修");

    /** 允许的状态流转（含出坞/让位：占用 → 空闲） */
    private static final Map<String, List<String>> BERTH_NEXT = Map.of(
            "IDLE", List.of("OCCUPIED", "MAINTENANCE"),
            "OCCUPIED", List.of("MAINTENANCE", "IDLE"),
            "MAINTENANCE", List.of("IDLE"));

    // ---------------- 船舶类型 ----------------
    public static final List<String> VESSEL_TYPES = List.of(
            "BULK", "TANKER", "CONTAINER", "PASSENGER", "ENGINEERING");

    private static final Map<String, String> VESSEL_TYPE_NAME = Map.of(
            "BULK", "散货船",
            "TANKER", "油轮",
            "CONTAINER", "集装箱船",
            "PASSENGER", "客滚船",
            "ENGINEERING", "工程船");

    public static final List<String> VESSEL_STATUSES = List.of("IN_YARD", "LAUNCHED", "DELIVERED");

    private static final Map<String, String> VESSEL_STATUS_NAME = Map.of(
            "IN_YARD", "在厂",
            "LAUNCHED", "已下水",
            "DELIVERED", "已交付");

    // ---------------- 船东 ----------------
    public static final List<String> OWNER_TYPES = List.of("STATE", "PRIVATE", "FOREIGN");

    private static final Map<String, String> OWNER_TYPE_NAME = Map.of(
            "STATE", "国有航运",
            "PRIVATE", "民营船东",
            "FOREIGN", "境外船东");

    public static final List<String> COOP_LEVELS = List.of("STRATEGIC", "KEY", "NORMAL");

    private static final Map<String, String> COOP_LEVEL_NAME = Map.of(
            "STRATEGIC", "战略客户",
            "KEY", "重点客户",
            "NORMAL", "一般客户");

    // ---------------- 修造合同 ----------------
    public static final List<String> CONTRACT_TYPES = List.of("NEW_BUILD", "REPAIR", "CONVERSION");

    private static final Map<String, String> CONTRACT_TYPE_NAME = Map.of(
            "NEW_BUILD", "新船建造",
            "REPAIR", "坞检修理",
            "CONVERSION", "改装加长");

    /** 草拟 → 已签 → 履行中 → 已结算，逐级推进，不可跳级 */
    public static final List<String> CONTRACT_FLOW = List.of("DRAFT", "SIGNED", "PERFORMING", "SETTLED");

    private static final Map<String, String> CONTRACT_STATUS_NAME = Map.of(
            "DRAFT", "草拟",
            "SIGNED", "已签",
            "PERFORMING", "履行中",
            "SETTLED", "已结算");

    // ---------------- 分段 ----------------
    public static final List<String> BLOCK_TYPES = List.of(
            "BOTTOM", "SIDE", "DECK", "BOW", "STERN", "SUPER");

    private static final Map<String, String> BLOCK_TYPE_NAME = Map.of(
            "BOTTOM", "底部分段",
            "SIDE", "舷侧分段",
            "DECK", "甲板分段",
            "BOW", "艏部分段",
            "STERN", "艉部分段",
            "SUPER", "上建分段");

    /** 预制 → 涂装 → 搭载 → 合拢 → 交验，逐级推进，不可跳级 */
    public static final List<String> BLOCK_FLOW = List.of("PREFAB", "PAINT", "ERECTION", "CLOSURE", "SURVEY");

    private static final Map<String, String> BLOCK_STATUS_NAME = Map.of(
            "PREFAB", "预制",
            "PAINT", "涂装",
            "ERECTION", "搭载",
            "CLOSURE", "合拢",
            "SURVEY", "交验");

    // ---------------- 起重设备 ----------------
    public static final List<String> CRANE_TYPES = List.of("GANTRY", "TOWER", "CRAWLER", "FLOATING");

    private static final Map<String, String> CRANE_TYPE_NAME = Map.of(
            "GANTRY", "龙门吊",
            "TOWER", "塔吊",
            "CRAWLER", "履带吊",
            "FLOATING", "浮吊");

    public static final List<String> CRANE_STATUSES = List.of("AVAILABLE", "MAINTAINING", "RETIRED");

    private static final Map<String, String> CRANE_STATUS_NAME = Map.of(
            "AVAILABLE", "可用",
            "MAINTAINING", "检修",
            "RETIRED", "停用");

    // ---------------- 搭载任务 ----------------
    public static final List<String> ERECTION_STATUSES = List.of("PLANNED", "READY", "DONE");

    private static final Map<String, String> ERECTION_STATUS_NAME = Map.of(
            "PLANNED", "已排程",
            "READY", "待吊装",
            "DONE", "已完成");

    private static final Map<String, List<String>> ERECTION_NEXT = Map.of(
            "PLANNED", List.of("READY"),
            "READY", List.of("DONE", "PLANNED"),
            "DONE", List.of());

    // ---------------- 报验 ----------------
    public static final List<String> SURVEY_ITEMS = List.of(
            "WELD", "DIMENSION", "COATING", "OUTFITTING", "TIGHTNESS");

    private static final Map<String, String> SURVEY_ITEM_NAME = Map.of(
            "WELD", "焊缝探伤",
            "DIMENSION", "主尺度",
            "COATING", "涂装膜厚",
            "OUTFITTING", "舾装完整性",
            "TIGHTNESS", "舱室密性");

    /** 结论只能三选一：合格 / 让步 / 不合格 */
    public static final List<String> SURVEY_RESULTS = List.of("PASS", "CONCESSION", "FAIL");

    private static final Map<String, String> SURVEY_RESULT_NAME = Map.of(
            "PASS", "合格",
            "CONCESSION", "让步",
            "FAIL", "不合格");

    public static final List<String> AUTHORITIES = List.of("CCS", "ABS", "LR", "DNV", "BV", "OWNER");

    private static final Map<String, String> AUTHORITY_NAME = Map.of(
            "CCS", "中国船级社",
            "ABS", "美国船级社",
            "LR", "英国劳氏",
            "DNV", "挪威船级社",
            "BV", "法国必维",
            "OWNER", "船东代表");

    // ---------------- 遗留项 ----------------
    public static final List<String> PUNCH_SEVERITIES = List.of("MINOR", "MAJOR", "CRITICAL");

    private static final Map<String, String> PUNCH_SEVERITY_NAME = Map.of(
            "MINOR", "一般",
            "MAJOR", "重要",
            "CRITICAL", "严重");

    /** 开启 → 整改中 → 已闭环 */
    public static final List<String> PUNCH_STATUSES = List.of("OPEN", "RECTIFYING", "CLOSED");

    private static final Map<String, String> PUNCH_STATUS_NAME = Map.of(
            "OPEN", "开启",
            "RECTIFYING", "整改中",
            "CLOSED", "已闭环");

    private static final Map<String, List<String>> PUNCH_NEXT = Map.of(
            "OPEN", List.of("RECTIFYING", "CLOSED"),
            "RECTIFYING", List.of("CLOSED"),
            "CLOSED", List.of());

    // ---------------- 业务阈值 ----------------
    /** 单个分段重量上限（吨） */
    public static final BigDecimal MAX_BLOCK_WEIGHT = BigDecimal.valueOf(1200);

    /** 单次吊装重量上限（吨），超过任何一台在用设备都拦 */
    public static final BigDecimal MAX_LIFT_WEIGHT = BigDecimal.valueOf(1500);

    /** 六级及以上风力禁止吊装 */
    public static final int MAX_WIND_LEVEL = 6;

    /** 分段长度/宽度余量：船台尺度须比船舶尺度至少富余这么多（米） */
    public static final BigDecimal BERTH_MARGIN_M = BigDecimal.valueOf(0);

    // ---------------- 综合管廊：附属设备 / 管线 / 舱段 ----------------
    public static final List<String> FACILITY_TYPES = List.of("风机", "排水泵", "照明", "监控");
    public static final List<String> FACILITY_STATUSES = List.of("正常", "故障");
    public static final List<String> LINE_TYPES = List.of("电力", "通信", "给水", "排水", "热力", "燃气");
    public static final List<String> PRESSURE_LEVELS = List.of("高压", "中压", "低压");
    public static final List<String> PIPELINE_STATUSES = List.of("在用", "检修");
    public static final List<String> SEGMENT_SHAPES = List.of("圆形", "矩形", "马蹄形");
    public static final List<String> SEGMENT_STATUSES = List.of("运营", "维护", "停用");

    // ---------------- 综合管廊：舱段容量与管线规格兼容规则 ----------------
    /** 新建舱段默认舱容（容量舱位），单舱段舱容必须为正数 */
    public static final int DEFAULT_SEGMENT_CAPACITY = 8;

    /** 无压力级制时（通信 / 排水固定占 1 舱位）的占用矩阵键 */
    private static final String NO_PRESSURE = "-";

    /**
     * 管线「类型 × 压力级制 → 占用舱位」矩阵。
     * 通信、排水为无压管线，固定占用 1 舱位；其余按压力级制分级：
     * 电力 2/1/1，给水 3/2/1，热力 2/1（热力不允许高压），燃气 4/3/2。
     */
    private static final Map<String, Map<String, Integer>> SLOT_MATRIX = Map.of(
            "电力", Map.of("高压", 2, "中压", 1, "低压", 1),
            "通信", Map.of(NO_PRESSURE, 1),
            "给水", Map.of("高压", 3, "中压", 2, "低压", 1),
            "排水", Map.of(NO_PRESSURE, 1),
            "热力", Map.of("中压", 2, "低压", 1),
            "燃气", Map.of("高压", 4, "中压", 3, "低压", 2));

    /** 必须标注压力级制的压力管线：电力 / 给水 / 燃气 / 热力（热力仅不可为高压） */
    public static final List<String> PRESSURE_REQUIRED_TYPES = List.of("电力", "给水", "燃气", "热力");

    /** 无压管线，禁止填写压力级制：通信 / 排水 */
    public static final List<String> PRESSURE_FORBIDDEN_TYPES = List.of("通信", "排水");

    /** 各类型允许的最大管径（毫米），超出即规格不兼容 */
    private static final Map<String, Integer> MAX_DIAMETER_MM = Map.of(
            "电力", 1600,
            "通信", 400,
            "给水", 1200,
            "排水", 2000,
            "热力", 1000,
            "燃气", 800);

    // ---------------- 综合管廊：巡检异常 / 风险事件 ----------------
    /** 发现对象类型：风险事件只能从巡检点或附属设备登记 */
    public static final List<String> RISK_SOURCE_TYPES = List.of("巡检点", "附属设备");

    public static final List<String> RISK_SEVERITIES = List.of("一般", "较大", "重大");

    /** 待处置 → 处理中 → 已解除（待处置也可直接解除） */
    public static final List<String> RISK_STATUSES = List.of("待处置", "处理中", "已解除");

    private static final Map<String, List<String>> RISK_NEXT = Map.of(
            "待处置", List.of("处理中", "已解除"),
            "处理中", List.of("已解除"),
            "已解除", List.of());

    // ---------------- 取值方法 ----------------

    public static boolean isValidBerthType(String type) {
        return type != null && BERTH_TYPE_NAME.containsKey(type);
    }

    public static String berthTypeName(String type) {
        return type == null ? "" : BERTH_TYPE_NAME.getOrDefault(type, type);
    }

    public static boolean isValidBerthStatus(String status) {
        return status != null && BERTH_STATUS_NAME.containsKey(status);
    }

    public static String berthStatusName(String status) {
        return status == null ? "" : BERTH_STATUS_NAME.getOrDefault(status, status);
    }

    /** 船台状态流转：空闲 → 占用 → 检修 → 空闲 */
    public static boolean canTransferBerth(String from, String to) {
        if (from == null || to == null) {
            return false;
        }
        return BERTH_NEXT.getOrDefault(from, List.of()).contains(to);
    }

    public static boolean isValidVesselType(String type) {
        return type != null && VESSEL_TYPE_NAME.containsKey(type);
    }

    public static String vesselTypeName(String type) {
        return type == null ? "" : VESSEL_TYPE_NAME.getOrDefault(type, type);
    }

    public static boolean isValidVesselStatus(String status) {
        return status != null && VESSEL_STATUS_NAME.containsKey(status);
    }

    public static String vesselStatusName(String status) {
        return status == null ? "" : VESSEL_STATUS_NAME.getOrDefault(status, status);
    }

    public static boolean isValidOwnerType(String type) {
        return type != null && OWNER_TYPE_NAME.containsKey(type);
    }

    public static String ownerTypeName(String type) {
        return type == null ? "" : OWNER_TYPE_NAME.getOrDefault(type, type);
    }

    public static boolean isValidCoopLevel(String level) {
        return level != null && COOP_LEVEL_NAME.containsKey(level);
    }

    public static String coopLevelName(String level) {
        return level == null ? "" : COOP_LEVEL_NAME.getOrDefault(level, level);
    }

    public static boolean isValidContractType(String type) {
        return type != null && CONTRACT_TYPE_NAME.containsKey(type);
    }

    public static String contractTypeName(String type) {
        return type == null ? "" : CONTRACT_TYPE_NAME.getOrDefault(type, type);
    }

    public static boolean isValidContractStatus(String status) {
        return status != null && CONTRACT_STATUS_NAME.containsKey(status);
    }

    public static String contractStatusName(String status) {
        return status == null ? "" : CONTRACT_STATUS_NAME.getOrDefault(status, status);
    }

    /** 合同状态只能逐级推进：草拟 → 已签 → 履行中 → 已结算 */
    public static boolean canTransferContract(String from, String to) {
        int i = CONTRACT_FLOW.indexOf(from);
        return i >= 0 && i + 1 < CONTRACT_FLOW.size() && CONTRACT_FLOW.get(i + 1).equals(to);
    }

    public static boolean isValidBlockType(String type) {
        return type != null && BLOCK_TYPE_NAME.containsKey(type);
    }

    public static String blockTypeName(String type) {
        return type == null ? "" : BLOCK_TYPE_NAME.getOrDefault(type, type);
    }

    public static boolean isValidBlockStatus(String status) {
        return status != null && BLOCK_STATUS_NAME.containsKey(status);
    }

    public static String blockStatusName(String status) {
        return status == null ? "" : BLOCK_STATUS_NAME.getOrDefault(status, status);
    }

    public static int blockStatusIndex(String status) {
        return BLOCK_FLOW.indexOf(status);
    }

    /** 分段状态只能逐级推进：预制 → 涂装 → 搭载 → 合拢 → 交验 */
    public static boolean canTransferBlock(String from, String to) {
        int i = BLOCK_FLOW.indexOf(from);
        return i >= 0 && i + 1 < BLOCK_FLOW.size() && BLOCK_FLOW.get(i + 1).equals(to);
    }

    public static boolean isValidCraneType(String type) {
        return type != null && CRANE_TYPE_NAME.containsKey(type);
    }

    public static String craneTypeName(String type) {
        return type == null ? "" : CRANE_TYPE_NAME.getOrDefault(type, type);
    }

    public static boolean isValidCraneStatus(String status) {
        return status != null && CRANE_STATUS_NAME.containsKey(status);
    }

    public static String craneStatusName(String status) {
        return status == null ? "" : CRANE_STATUS_NAME.getOrDefault(status, status);
    }

    public static boolean isValidErectionStatus(String status) {
        return status != null && ERECTION_STATUS_NAME.containsKey(status);
    }

    public static String erectionStatusName(String status) {
        return status == null ? "" : ERECTION_STATUS_NAME.getOrDefault(status, status);
    }

    public static boolean canTransferErection(String from, String to) {
        return ERECTION_NEXT.getOrDefault(from, List.of()).contains(to);
    }

    public static boolean isValidSurveyItem(String item) {
        return item != null && SURVEY_ITEM_NAME.containsKey(item);
    }

    public static String surveyItemName(String item) {
        return item == null ? "" : SURVEY_ITEM_NAME.getOrDefault(item, item);
    }

    public static boolean isValidSurveyResult(String result) {
        return result != null && SURVEY_RESULT_NAME.containsKey(result);
    }

    public static String surveyResultName(String result) {
        return result == null ? "" : SURVEY_RESULT_NAME.getOrDefault(result, result);
    }

    public static boolean isValidAuthority(String authority) {
        return authority != null && AUTHORITY_NAME.containsKey(authority);
    }

    public static String authorityName(String authority) {
        return authority == null ? "" : AUTHORITY_NAME.getOrDefault(authority, authority);
    }

    public static boolean isValidPunchSeverity(String severity) {
        return severity != null && PUNCH_SEVERITY_NAME.containsKey(severity);
    }

    public static String punchSeverityName(String severity) {
        return severity == null ? "" : PUNCH_SEVERITY_NAME.getOrDefault(severity, severity);
    }

    public static boolean isValidPunchStatus(String status) {
        return status != null && PUNCH_STATUS_NAME.containsKey(status);
    }

    public static String punchStatusName(String status) {
        return status == null ? "" : PUNCH_STATUS_NAME.getOrDefault(status, status);
    }

    public static boolean canTransferPunch(String from, String to) {
        return PUNCH_NEXT.getOrDefault(from, List.of()).contains(to);
    }

    // ---------------- 综合管廊取值方法 ----------------

    public static boolean isValidFacilityType(String type) {
        return type != null && !type.isBlank() && FACILITY_TYPES.contains(type);
    }

    public static boolean isValidFacilityStatus(String status) {
        return status != null && !status.isBlank() && FACILITY_STATUSES.contains(status);
    }

    public static boolean isValidLineType(String type) {
        return type != null && !type.isBlank() && LINE_TYPES.contains(type);
    }

    public static boolean isValidPressureLevel(String level) {
        return level != null && !level.isBlank() && PRESSURE_LEVELS.contains(level);
    }

    /** 该类型管线是否必须标注压力级制（电力 / 给水 / 燃气） */
    public static boolean isPressureRequired(String lineType) {
        return PRESSURE_REQUIRED_TYPES.contains(lineType);
    }

    /** 该类型管线是否为无压管线、禁止填写压力级制（通信 / 排水 / 热力） */
    public static boolean isPressureForbidden(String lineType) {
        return PRESSURE_FORBIDDEN_TYPES.contains(lineType);
    }

    /**
     * 按「管线类型 × 压力级制」计算占用舱位。
     *
     * @return 占用舱位；类型/压力级制组合不允许（如热力-高压）时返回 -1
     */
    public static int slotOf(String lineType, String pressureLevel) {
        Map<String, Integer> byLevel = SLOT_MATRIX.get(lineType);
        if (byLevel == null) {
            return -1;
        }
        String key = isPressureForbidden(lineType) ? NO_PRESSURE : pressureLevel;
        Integer slots = byLevel.get(key);
        return slots == null ? -1 : slots;
    }

    /** 该类型管线允许的最大管径（毫米） */
    public static int maxDiameterMm(String lineType) {
        return MAX_DIAMETER_MM.getOrDefault(lineType, Integer.MAX_VALUE);
    }

    /** 舱容合法：非 null 且为正整数 */
    public static boolean isValidCapacity(Integer capacity) {
        return capacity != null && capacity > 0;
    }

    public static boolean isValidPipelineStatus(String status) {
        return status != null && !status.isBlank() && PIPELINE_STATUSES.contains(status);
    }

    public static boolean isValidSegmentShape(String shape) {
        return shape != null && !shape.isBlank() && SEGMENT_SHAPES.contains(shape);
    }

    public static boolean isValidSegmentStatus(String status) {
        return status != null && !status.isBlank() && SEGMENT_STATUSES.contains(status);
    }

    public static boolean isValidRiskSourceType(String sourceType) {
        return sourceType != null && !sourceType.isBlank() && RISK_SOURCE_TYPES.contains(sourceType);
    }

    public static boolean isValidRiskSeverity(String severity) {
        return severity != null && !severity.isBlank() && RISK_SEVERITIES.contains(severity);
    }

    public static boolean isValidRiskStatus(String status) {
        return status != null && !status.isBlank() && RISK_STATUSES.contains(status);
    }

    /** 风险事件状态流转：待处置 → 处理中 → 已解除（待处置可直接解除，已解除为终态） */
    public static boolean canTransferRisk(String from, String to) {
        if (from == null || to == null) {
            return false;
        }
        return RISK_NEXT.getOrDefault(from, List.of()).contains(to);
    }
}
