-- 完整集成测试数据初始化脚本
-- 用于：TestCaseController, MonitoringController, ReportController集成测试

-- 禁用外键检查，允许删除表
SET FOREIGN_KEY_CHECKS = 0;

-- 先删除可能存在的表和约束
DROP TABLE IF EXISTS quality_report_test_result;
DROP TABLE IF EXISTS quality_report_risk_assessment;
DROP TABLE IF EXISTS quality_report;
DROP TABLE IF EXISTS monitoring_data;
DROP TABLE IF EXISTS test_cases;

-- 重新启用外键检查
SET FOREIGN_KEY_CHECKS = 1;

-- 创建表（如果不存在）
CREATE TABLE test_cases (
    id VARCHAR(36) PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    steps JSON NOT NULL,
    expected_result TEXT,
    type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    priority INTEGER DEFAULT 0,
    tags JSON,
    related_requirement VARCHAR(100),
    created_by VARCHAR(100) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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

CREATE TABLE quality_report (
    id VARCHAR(36) PRIMARY KEY,
    task_id VARCHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    summary TEXT,
    generated_at DATETIME NOT NULL,
    status VARCHAR(20) NOT NULL,
    defect_stats JSON,
    performance_metrics JSON,
    INDEX idx_task_id (task_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quality_report_test_result (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_id VARCHAR(36) NOT NULL,
    test_case_id VARCHAR(36) NOT NULL,
    test_case_name VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    execution_time BIGINT,
    error TEXT,
    screenshot VARCHAR(500),
    executed_at DATETIME,
    INDEX idx_report_id (report_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quality_report_risk_assessment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_id VARCHAR(36) NOT NULL,
    overall_risk VARCHAR(20) NOT NULL,
    risk_score DOUBLE DEFAULT 0.0,
    high_risk_modules JSON,
    recommendations JSON,
    module_risk_scores JSON,
    INDEX idx_report_id (report_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ====================================
-- 测试用例数据 (test_cases)
-- ====================================
INSERT INTO test_cases (
    id, title, description, steps, expected_result,
    type, status, priority, tags,
    related_requirement, created_by, created_at, updated_at
) VALUES
-- 正常测试用例
('tc-001', '用户登录-正常流程', '验证用户使用正确凭证登录',
 '["打开登录页面","输入用户名: test@example.com","输入密码: ******","点击登录按钮"]',
 '登录成功，跳转到首页',
 'FUNCTIONAL', 'APPROVED', 8, '["login","smoke"]',
 'REQ-001', 'zhangsan', NOW(), NOW()),

('tc-002', '用户登录-密码错误', '验证密码错误时的提示',
 '["打开登录页面","输入用户名: test@example.com","输入错误密码","点击登录按钮"]',
 '显示"密码错误"提示，登录失败',
 'FUNCTIONAL', 'APPROVED', 7, '["login","negative"]',
 'REQ-001', 'zhangsan', NOW(), NOW()),

('tc-003', '订单创建-正常流程', '验证用户创建订单',
 '["选择商品","添加到购物车","填写收货地址","选择支付方式","提交订单"]',
 '订单创建成功，生成订单号',
 'FUNCTIONAL', 'APPROVED', 9, '["order","critical"]',
 'REQ-002', 'lisi', NOW(), NOW()),

('tc-004', '支付功能-微信支付', '验证微信支付流程',
 '["选择微信支付","扫码支付","确认支付"]',
 '支付成功，订单状态更新为已支付',
 'FUNCTIONAL', 'DRAFT', 8, '["payment","integration"]',
 'REQ-003', 'lisi', NOW(), NOW()),

('tc-005', '性能测试-并发登录', '验证系统并发处理能力',
 '["模拟100个用户同时登录","记录响应时间"]',
 '所有请求成功，平均响应时间<2秒',
 'PERFORMANCE', 'APPROVED', 6, '["performance","load"]',
 'REQ-004', 'wangwu', NOW(), NOW()),

('tc-006', 'API接口-创建用户', '验证创建用户API',
 '["发送POST请求到/api/users","请求体包含用户信息"]',
 '返回201状态码，用户创建成功',
 'API', 'DRAFT', 7, '["api","backend"]',
 'REQ-005', 'zhangsan', NOW(), NOW()),

-- 待删除的测试用例
('tc-to-delete', '待删除用例', '用于测试删除功能',
 '["测试步骤"]',
 '预期结果',
 'FUNCTIONAL', 'DRAFT', 3, '[]',
 NULL, 'tester', NOW(), NOW());

-- ====================================
-- 监控数据 (monitoring_data)
-- ====================================
INSERT INTO monitoring_data (
    id, task_id, status, progress,
    total_cases, executed_cases, passed_cases, failed_cases, skipped_cases,
    start_time, estimated_end_time, actual_end_time,
    environment, version,
    resource_usage, performance_metrics,
    timestamp
) VALUES
-- 运行中的任务
('mon-001', 'task-001', 'RUNNING', 65,
 100, 65, 60, 5, 0,
 DATE_SUB(NOW(), INTERVAL 30 MINUTE), DATE_ADD(NOW(), INTERVAL 15 MINUTE), NULL,
 'DEV', 'v2.1.0',
 '{"cpu": 45.5, "memory": 1024, "disk": 50}', '{"avgResponseTime": 250, "maxResponseTime": 800}',
 NOW()),

('mon-002', 'task-002', 'RUNNING', 30,
 50, 15, 14, 1, 0,
 DATE_SUB(NOW(), INTERVAL 10 MINUTE), DATE_ADD(NOW(), INTERVAL 20 MINUTE), NULL,
 'STAGING', 'v2.1.0',
 '{"cpu": 30.2, "memory": 512, "disk": 25}', '{"avgResponseTime": 180, "maxResponseTime": 500}',
 NOW()),

-- 已完成的任务
('mon-003', 'task-003', 'COMPLETED', 100,
 80, 80, 78, 2, 0,
 DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_SUB(NOW(), INTERVAL 1 HOUR),
 'DEV', 'v2.0.0',
 '{"cpu": 50.0, "memory": 768, "disk": 40}', '{"avgResponseTime": 200, "maxResponseTime": 600}',
 DATE_SUB(NOW(), INTERVAL 1 HOUR)),

('mon-004', 'task-004', 'COMPLETED', 100,
 120, 120, 110, 10, 0,
 DATE_SUB(NOW(), INTERVAL 3 HOUR), DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_SUB(NOW(), INTERVAL 2 HOUR),
 'PROD', 'v2.1.0',
 '{"cpu": 60.0, "memory": 2048, "disk": 80}', '{"avgResponseTime": 300, "maxResponseTime": 1000}',
 DATE_SUB(NOW(), INTERVAL 2 HOUR)),

-- 失败的任务
('mon-005', 'task-005', 'FAILED', 45,
 100, 45, 35, 10, 0,
 DATE_SUB(NOW(), INTERVAL 1 HOUR), NULL, DATE_SUB(NOW(), INTERVAL 30 MINUTE),
 'DEV', 'v2.0.0',
 '{"cpu": 55.0, "memory": 1024, "disk": 60}', '{"avgResponseTime": 350, "maxResponseTime": 1200}',
 DATE_SUB(NOW(), INTERVAL 30 MINUTE)),

-- 待执行的任务
('mon-006', 'task-006', 'PENDING', 0,
 60, 0, 0, 0, 0,
 NULL, NULL, NULL,
 'STAGING', 'v2.1.0',
 NULL, NULL,
 NOW());

-- ====================================
-- 质量报告数据 (quality_report)
-- ====================================
INSERT INTO quality_report (
    id, task_id, name, summary, generated_at, status, defect_stats, performance_metrics
) VALUES
('report-001', 'task-003', '测试任务task-003质量报告',
 '测试执行顺利，通过率97.5%',
 DATE_SUB(NOW(), INTERVAL 1 HOUR),
 'COMPLETED',
 '{"critical": 0, "high": 0, "medium": 2, "low": 0}',
 '{"avgResponseTime": 200, "maxResponseTime": 600, "minResponseTime": 50}'),

('report-002', 'task-004', '测试任务task-004质量报告',
 '生产环境测试，发现10个问题',
 DATE_SUB(NOW(), INTERVAL 2 HOUR),
 'COMPLETED',
 '{"critical": 0, "high": 2, "medium": 5, "low": 3}',
 '{"avgResponseTime": 300, "maxResponseTime": 1000, "minResponseTime": 80}'),

('report-003', 'task-005', '测试任务task-005质量报告',
 '测试中断，执行未完成',
 DATE_SUB(NOW(), INTERVAL 30 MINUTE),
 'GENERATING',
 '{"critical": 1, "high": 3, "medium": 6, "low": 0}',
 '{"avgResponseTime": 350, "maxResponseTime": 1200, "minResponseTime": 100}');

-- ====================================
-- 测试结果数据 (quality_report_test_result)
-- ====================================
INSERT INTO quality_report_test_result (
    report_id, test_case_id, test_case_name, status, execution_time, error, screenshot, executed_at
) VALUES
-- report-001 的测试结果
('report-001', 'tc-001', '用户登录-正常流程', 'PASSED', 1250, NULL, NULL, DATE_SUB(NOW(), INTERVAL 1 HOUR)),
('report-001', 'tc-002', '用户登录-密码错误', 'PASSED', 980, NULL, NULL, DATE_SUB(NOW(), INTERVAL 1 HOUR)),
('report-001', 'tc-003', '订单创建-正常流程', 'FAILED', 2500, '数据库连接超时', '/screenshots/tc-003-fail.png', DATE_SUB(NOW(), INTERVAL 1 HOUR)),

-- report-002 的测试结果
('report-002', 'tc-001', '用户登录-正常流程', 'PASSED', 1100, NULL, NULL, DATE_SUB(NOW(), INTERVAL 2 HOUR)),
('report-002', 'tc-004', '支付功能-微信支付', 'FAILED', 3200, '支付接口返回500', '/screenshots/tc-004-fail.png', DATE_SUB(NOW(), INTERVAL 2 HOUR)),
('report-002', 'tc-005', '性能测试-并发登录', 'PASSED', 5600, NULL, NULL, DATE_SUB(NOW(), INTERVAL 2 HOUR)),

-- report-003 的测试结果
('report-003', 'tc-001', '用户登录-正常流程', 'PASSED', 1300, NULL, NULL, DATE_SUB(NOW(), INTERVAL 30 MINUTE)),
('report-003', 'tc-002', '用户登录-密码错误', 'BLOCKED', 0, '环境不可用', NULL, DATE_SUB(NOW(), INTERVAL 30 MINUTE));

-- ====================================
-- 风险评估数据 (quality_report_risk_assessment)
-- ====================================
INSERT INTO quality_report_risk_assessment (
    report_id, overall_risk, risk_score, high_risk_modules, recommendations, module_risk_scores
) VALUES
('report-001', 'LOW', 0.15,
 '[]',
 '["建议修复2个失败用例", "增加边界场景测试"]',
 '{"login": 0.1, "order": 0.2, "payment": 0.1}'),

('report-002', 'MEDIUM', 0.45,
 '["payment", "order"]',
 '["优先修复高优先级缺陷", "进行性能优化", "加强支付模块测试"]',
 '{"login": 0.2, "order": 0.5, "payment": 0.7}'),

('report-003', 'HIGH', 0.75,
 '["login", "payment", "order"]',
 '["分析失败原因", "重新执行测试", "修复环境问题"]',
 '{"login": 0.8, "order": 0.7, "payment": 0.8}');
