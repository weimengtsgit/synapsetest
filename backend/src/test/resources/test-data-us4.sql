-- US4-质量报告生成和管理测试数据
-- 先创建表结构，再插入测试数据

-- 测试任务表由test-schema.sql创建，这里不再重复创建

-- 测试用例表由test-schema.sql创建，这里不再重复创建

-- 创建质量报告表
CREATE TABLE IF NOT EXISTS quality_report (
    id VARCHAR(36) PRIMARY KEY,
    task_id VARCHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    summary CLOB,
    generated_at TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL,
    defect_stats CLOB,
    performance_metrics CLOB
);

-- 创建测试结果表
CREATE TABLE IF NOT EXISTS quality_report_test_result (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_id VARCHAR(36) NOT NULL,
    test_case_id VARCHAR(36) NOT NULL,
    test_case_name VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    execution_time BIGINT,
    error CLOB,
    screenshot VARCHAR(500),
    executed_at TIMESTAMP
);

-- 创建风险评估表
CREATE TABLE IF NOT EXISTS quality_report_risk_assessment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_id VARCHAR(36) NOT NULL UNIQUE,
    overall_risk VARCHAR(20) NOT NULL,
    risk_score DOUBLE DEFAULT 0.0,
    high_risk_modules CLOB,
    recommendations CLOB,
    module_risk_scores CLOB
);

-- 创建监控数据表
CREATE TABLE IF NOT EXISTS monitoring_data (
    id BIGINT PRIMARY KEY,
    task_id BIGINT,
    metric_type VARCHAR(100),
    metric_value DOUBLE,
    unit VARCHAR(50),
    collection_time TIMESTAMP,
    environment VARCHAR(50),
    status VARCHAR(50)
);

-- 清空已有数据
DELETE FROM quality_report_risk_assessment WHERE 1=1;
DELETE FROM quality_report_test_result WHERE 1=1;
DELETE FROM quality_report WHERE 1=1;
DELETE FROM monitoring_data WHERE 1=1;
DELETE FROM test_cases WHERE 1=1;
DELETE FROM test_tasks WHERE 1=1;

-- 插入测试任务数据 (使用test-schema.sql的列名)
INSERT INTO test_tasks (id, name, test_scope, environment, version, created_by, status, priority, created_at, updated_at)
VALUES
('1001', '测试任务-质量验证', 'FULL', 'PRODUCTION', '1.0.0', 'lisi', 'COMPLETED', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('1002', '测试任务-回归测试', 'PARTIAL', 'STAGING', '1.0.1', 'wangwu', 'RUNNING', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 插入测试用例数据 (使用test-schema.sql的列名)
INSERT INTO test_cases (id, title, description, steps, expected_result, type, priority, status, created_by, created_at, updated_at)
VALUES
('2001', '登录验证测试', '验证用户登录功能', '[{"step": "打开登录页面"}, {"step": "输入用户名密码"}, {"step": "点击登录按钮"}]', '成功登录系统', 'FUNCTIONAL', 0, 'ACTIVE', 'zhangsan', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('2002', '数据查询性能测试', '测试系统查询性能', '[{"step": "输入查询条件"}, {"step": "执行查询"}, {"step": "记录响应时间"}]', '响应时间<3秒', 'PERFORMANCE', 0, 'ACTIVE', 'zhangsan', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('2003', '权限管理测试', '测试用户权限管理功能', '[{"step": "访问权限设置"}, {"step": "修改用户权限"}, {"step": "验证权限变更"}]', '权限修改成功', 'FUNCTIONAL', 0, 'ACTIVE', 'lisi', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 插入质量报告数据
INSERT INTO quality_report (id, task_id, name, summary, generated_at, status, defect_stats, performance_metrics)
VALUES
('report-001', '1001', '质量验证报告', '系统质量良好，通过率95%', CURRENT_TIMESTAMP, 'COMPLETED', '{"critical": 0, "high": 1, "medium": 2, "low": 3}', '{"avgResponseTime": 250, "throughput": 1000}'),
('report-002', '1002', '回归测试报告', '发现部分功能缺陷', CURRENT_TIMESTAMP, 'GENERATING', '{"critical": 1, "high": 2, "medium": 5, "low": 8}', '{"avgResponseTime": 420, "throughput": 800}');

-- 监控数据由test-data-us3.sql提供，不在此重复插入