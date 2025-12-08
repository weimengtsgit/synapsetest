-- 测试数据初始化脚本 - US3 监控和可视化测试
-- 用于：MonitoringControllerIntegrationTest

-- Disable foreign key checks
SET FOREIGN_KEY_CHECKS = 0;

-- Drop table if exists
DROP TABLE IF EXISTS monitoring_data;

-- Re-enable foreign key checks
SET FOREIGN_KEY_CHECKS = 1;

-- Create monitoring_data table
CREATE TABLE monitoring_data (
    id VARCHAR(36) PRIMARY KEY,
    task_id VARCHAR(36) NOT NULL,
    status VARCHAR(20) NOT NULL,
    progress INT DEFAULT 0,
    executed_cases INT DEFAULT 0,
    total_cases INT DEFAULT 0,
    passed_cases INT DEFAULT 0,
    failed_cases INT DEFAULT 0,
    skipped_cases INT DEFAULT 0,
    start_time DATETIME,
    estimated_end_time DATETIME,
    actual_end_time DATETIME,
    resource_usage JSON,
    performance_metrics JSON,
    timestamp DATETIME NOT NULL,
    environment VARCHAR(100),
    version VARCHAR(50),
    INDEX idx_task_id (task_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 插入测试监控数据
INSERT INTO monitoring_data (
    id, task_id, status, progress, total_cases, executed_cases,
    passed_cases, failed_cases, skipped_cases,
    start_time, estimated_end_time, actual_end_time,
    environment, version, resource_usage, performance_metrics, timestamp
) VALUES
-- 运行中的任务
('mon-001', 'task-001', 'RUNNING', 65, 100, 65, 60, 5, 0,
 DATE_SUB(NOW(), INTERVAL 30 MINUTE), DATE_ADD(NOW(), INTERVAL 15 MINUTE), NULL,
 'DEV', 'v2.1.0',
 '{"cpu": 45.5, "memory": 1024, "disk": 50}', '{"avgResponseTime": 250, "maxResponseTime": 800}',
 NOW()),

('mon-002', 'task-002', 'RUNNING', 30, 50, 15, 14, 1, 0,
 DATE_SUB(NOW(), INTERVAL 10 MINUTE), DATE_ADD(NOW(), INTERVAL 20 MINUTE), NULL,
 'STAGING', 'v2.1.0',
 '{"cpu": 30.2, "memory": 512, "disk": 25}', '{"avgResponseTime": 180, "maxResponseTime": 500}',
 NOW()),

-- 已完成的任务
('mon-003', 'task-003', 'COMPLETED', 100, 80, 80, 78, 2, 0,
 DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_SUB(NOW(), INTERVAL 1 HOUR),
 'DEV', 'v2.0.0',
 '{"cpu": 50.0, "memory": 768, "disk": 40}', '{"avgResponseTime": 200, "maxResponseTime": 600}',
 DATE_SUB(NOW(), INTERVAL 1 HOUR)),

('mon-004', 'task-004', 'COMPLETED', 100, 120, 120, 110, 10, 0,
 DATE_SUB(NOW(), INTERVAL 3 HOUR), DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_SUB(NOW(), INTERVAL 2 HOUR),
 'PROD', 'v2.1.0',
 '{"cpu": 60.0, "memory": 2048, "disk": 80}', '{"avgResponseTime": 300, "maxResponseTime": 1000}',
 DATE_SUB(NOW(), INTERVAL 2 HOUR)),

-- 失败的任务
('mon-005', 'task-005', 'FAILED', 45, 100, 45, 35, 10, 0,
 DATE_SUB(NOW(), INTERVAL 1 HOUR), NULL, DATE_SUB(NOW(), INTERVAL 30 MINUTE),
 'DEV', 'v2.0.0',
 '{"cpu": 55.0, "memory": 1024, "disk": 60}', '{"avgResponseTime": 350, "maxResponseTime": 1200}',
 DATE_SUB(NOW(), INTERVAL 30 MINUTE)),

-- 待执行的任务
('mon-006', 'task-006', 'PENDING', 0, 60, 0, 0, 0, 0,
 NULL, NULL, NULL,
 'STAGING', 'v2.1.0', NULL, NULL,
 NOW());
