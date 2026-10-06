SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET character_set_client = utf8mb4;
SET character_set_connection = utf8mb4;
SET character_set_results = utf8mb4;

-- ---------------------------------------------------------------------
-- 综合管廊（地下城市管廊）· 廊体舱段 / 入廊管线 / 巡检点 / 附属设备 / 风险事件
-- 库名 utility_tunnel；application.yml 里 ddl-auto: none，表结构全部由本文件在 MySQL 镜像构建期导入。
-- 注意：mysql 容器是 UTC，种子数据里的时间一律写 UTC_TIMESTAMP() + INTERVAL 8 HOUR。

CREATE DATABASE IF NOT EXISTS utility_tunnel
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE utility_tunnel;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS risk_event;
DROP TABLE IF EXISTS facility;
DROP TABLE IF EXISTS patrol_point;
DROP TABLE IF EXISTS pipeline;
DROP TABLE IF EXISTS tunnel_segment;
SET FOREIGN_KEY_CHECKS = 1;

-- ---------------- 廊体舱段 ----------------
CREATE TABLE tunnel_segment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    seg_code VARCHAR(32) NOT NULL UNIQUE,
    seg_name VARCHAR(64),
    shape VARCHAR(16),
    length_m DECIMAL(10,2) NOT NULL,
    capacity_slots INT NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------- 入廊管线 ----------------
CREATE TABLE pipeline (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    segment_id BIGINT NOT NULL,
    line_type VARCHAR(16) NOT NULL,
    diameter_mm INT,
    pressure_level VARCHAR(16),
    status VARCHAR(16) NOT NULL,
    created_at DATETIME,
    KEY idx_pipeline_segment (segment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------- 巡检点 ----------------
CREATE TABLE patrol_point (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    segment_id BIGINT NOT NULL,
    point_code VARCHAR(32) NOT NULL,
    location VARCHAR(128),
    last_patrol_at DATETIME,
    created_at DATETIME,
    KEY idx_patrol_segment (segment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------- 附属设备 ----------------
CREATE TABLE facility (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    segment_id BIGINT NOT NULL,
    facility_type VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at DATETIME,
    KEY idx_facility_segment (segment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------- 巡检异常 / 风险事件 ----------------
CREATE TABLE risk_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    segment_id BIGINT NOT NULL,
    source_type VARCHAR(16) NOT NULL,
    source_id BIGINT NOT NULL,
    severity VARCHAR(16) NOT NULL,
    description VARCHAR(512) NOT NULL,
    found_at DATETIME NOT NULL,
    status VARCHAR(16) NOT NULL,
    handle_note VARCHAR(512),
    resolved_by VARCHAR(64),
    resolved_at DATETIME,
    created_at DATETIME,
    KEY idx_risk_segment (segment_id),
    KEY idx_risk_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =====================================================================
-- 种子数据（17 行，覆盖全部五个模块；演示「电力 + 燃气 防爆隔离」与「风险封控」规则场景）
-- =====================================================================

INSERT INTO tunnel_segment (id, seg_code, seg_name, shape, length_m, capacity_slots, status, created_at) VALUES
(1, 'SEG-01', '中央综合舱·北段', '矩形', 1200.00, 6, '运营', UTC_TIMESTAMP() + INTERVAL 8 HOUR - INTERVAL 90 DAY),
(2, 'SEG-02', '中央综合舱·南段', '圆形',  980.00,  4, '运营', UTC_TIMESTAMP() + INTERVAL 8 HOUR - INTERVAL 88 DAY),
(3, 'SEG-03', '电力舱',          '矩形',  760.00, 8, '维护', UTC_TIMESTAMP() + INTERVAL 8 HOUR - INTERVAL 80 DAY),
(4, 'SEG-04', '燃气舱',          '马蹄形', 540.00,  8, '停用', UTC_TIMESTAMP() + INTERVAL 8 HOUR - INTERVAL 60 DAY);

-- 占用舱位按「类型 × 压力级制」计算：SEG-01 电力高压2 + 通信1 = 3/6；SEG-02 排水1 = 1/4。
-- 通信 / 排水为无压管线，pressure_level 必须为 NULL（规格兼容规则）。
INSERT INTO pipeline (id, segment_id, line_type, diameter_mm, pressure_level, status, created_at) VALUES
(1, 1, '电力', 300, '高压', '在用', UTC_TIMESTAMP() + INTERVAL 8 HOUR - INTERVAL 70 DAY),
(2, 1, '通信', 100, NULL, '在用', UTC_TIMESTAMP() + INTERVAL 8 HOUR - INTERVAL 68 DAY),
(3, 2, '排水', 500, NULL, '在用', UTC_TIMESTAMP() + INTERVAL 8 HOUR - INTERVAL 66 DAY),
(4, 4, '燃气', 200, '中压', '在用', UTC_TIMESTAMP() + INTERVAL 8 HOUR - INTERVAL 55 DAY);

INSERT INTO patrol_point (id, segment_id, point_code, location, last_patrol_at, created_at) VALUES
(1, 1, 'P-01', '桩号 K0+100', UTC_TIMESTAMP() + INTERVAL 8 HOUR - INTERVAL 2 DAY, UTC_TIMESTAMP() + INTERVAL 8 HOUR - INTERVAL 70 DAY),
(2, 2, 'P-02', '桩号 K1+200', UTC_TIMESTAMP() + INTERVAL 8 HOUR - INTERVAL 5 DAY, UTC_TIMESTAMP() + INTERVAL 8 HOUR - INTERVAL 66 DAY),
(3, 3, 'P-03', '桩号 K2+000', UTC_TIMESTAMP() + INTERVAL 8 HOUR - INTERVAL 9 DAY, UTC_TIMESTAMP() + INTERVAL 8 HOUR - INTERVAL 80 DAY);

INSERT INTO facility (id, segment_id, facility_type, status, created_at) VALUES
(1, 1, '风机',   '正常', UTC_TIMESTAMP() + INTERVAL 8 HOUR - INTERVAL 70 DAY),
(2, 1, '排水泵', '故障', UTC_TIMESTAMP() + INTERVAL 8 HOUR - INTERVAL 70 DAY),
(3, 4, '监控',   '故障', UTC_TIMESTAMP() + INTERVAL 8 HOUR - INTERVAL 55 DAY);

-- 风险事件：SEG-01 一条已解除；SEG-03（维护）处理中、SEG-04（停用）待处置 —— 解除前两舱段封控
INSERT INTO risk_event (id, segment_id, source_type, source_id, severity, description, found_at,
                        status, handle_note, resolved_by, resolved_at, created_at) VALUES
(1, 1, '巡检点',   1, '一般', 'K0+100 处照明回路异响，光强波动', UTC_TIMESTAMP() + INTERVAL 8 HOUR - INTERVAL 6 DAY,
    '已解除', '现场复位回路并复测正常', '张伟', UTC_TIMESTAMP() + INTERVAL 8 HOUR - INTERVAL 5 DAY,
    UTC_TIMESTAMP() + INTERVAL 8 HOUR - INTERVAL 6 DAY),
(2, 3, '巡检点',   3, '较大', '电力舱电缆接头温升偏高，需停电复紧', UTC_TIMESTAMP() + INTERVAL 8 HOUR - INTERVAL 2 DAY,
    '处理中', '已安排检修班组进场', NULL, NULL,
    UTC_TIMESTAMP() + INTERVAL 8 HOUR - INTERVAL 2 DAY),
(3, 4, '附属设备', 3, '重大', '燃气舱监控设备故障，存在监测盲区', UTC_TIMESTAMP() + INTERVAL 8 HOUR - INTERVAL 1 DAY,
    '待处置', NULL, NULL, NULL,
    UTC_TIMESTAMP() + INTERVAL 8 HOUR - INTERVAL 1 DAY);
