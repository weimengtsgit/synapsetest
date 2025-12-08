-- 测试数据初始化脚本 - Mapper层测试
-- 用于：TestTaskMapperTest, TestCaseMapperTest, MonitoringDataMapperTest

-- 清空已有数据（如果存在）
TRUNCATE TABLE test_tasks;
TRUNCATE TABLE test_cases;
TRUNCATE TABLE monitoring_data;

-- 插入测试任务数据
INSERT INTO test_tasks (id, task_name, test_scope, environment, version, created_by, status, priority, created_at, updated_at, deleted_at)
VALUES
(1, '冒烟测试-订单模块', 'SMOKE', 'DEV', 'v1.2.0', 'zhangsan', 'PENDING', 'HIGH', NOW(), NOW(), NULL),
(2, '核心功能测试-支付流程', 'CORE', 'TEST', 'v1.2.0', 'lisi', 'RUNNING', 'CRITICAL', NOW(), NOW(), NULL),
(3, '全量回归测试', 'FULL', 'STAGING', 'v1.2.0', 'zhangsan', 'PENDING', 'MEDIUM', NOW(), NOW(), NULL),
(4, '冒烟测试-用户管理', 'SMOKE', 'DEV', 'v1.3.0', 'wangwu', 'COMPLETED', 'HIGH', NOW(), NOW(), NULL);

-- 插入测试用例数据
INSERT INTO test_cases (id, case_name, module, priority, type, steps, expected_result, ai_generated, ai_confidence, created_by, status, created_at, updated_at, deleted_at)
VALUES
(1, '订单创建-正常流程', '订单管理', 'HIGH', 'FUNCTIONAL',
 '[{"step":"1","action":"选择商品","expected":"显示商品列表"},{"step":"2","action":"添加到购物车","expected":"购物车数量+1"},{"step":"3","action":"提交订单","expected":"订单创建成功"}]',
 '{"status_code":200,"order_id":"ORD123","message":"订单创建成功"}',
 true, 0.92, 'ai-system', 'ACTIVE', NOW(), NOW(), NULL),

(2, '订单取消-异常场景', '订单管理', 'MEDIUM', 'FUNCTIONAL',
 '[{"step":"1","action":"查询订单","expected":"显示订单详情"},{"step":"2","action":"点击取消","expected":"提示确认"}]',
 '{"status_code":200,"message":"订单已取消"}',
 true, 0.88, 'ai-system', 'ACTIVE', NOW(), NOW(), NULL),

(3, '支付流程-微信支付', '支付模块', 'CRITICAL', 'INTEGRATION',
 '[{"step":"1","action":"选择微信支付","expected":"跳转微信支付页面"},{"step":"2","action":"完成支付","expected":"支付成功回调"}]',
 '{"status_code":200,"payment_status":"SUCCESS"}',
 true, 0.95, 'ai-system', 'ACTIVE', NOW(), NOW(), NULL),

(4, '用户登录-手工用例', '用户认证', 'HIGH', 'FUNCTIONAL',
 '[{"step":"1","action":"输入用户名密码","expected":"验证通过"}]',
 '{"status_code":200}',
 false, NULL, 'manual-tester', 'ACTIVE', NOW(), NOW(), NULL);

-- 插入监控数据
INSERT INTO monitoring_data (id, task_id, metric_type, metric_value, unit, recorded_at, created_at)
VALUES
(1, 1, 'EXECUTION_TIME', 125.5, 'seconds', NOW(), NOW()),
(2, 1, 'PASS_RATE', 85.5, 'percent', NOW(), NOW()),
(3, 1, 'CPU_USAGE', 45.2, 'percent', NOW(), NOW()),
(4, 1, 'MEMORY_USAGE', 68.5, 'percent', NOW(), NOW()),
(5, 2, 'EXECUTION_TIME', 320.8, 'seconds', NOW(), NOW()),
(6, 2, 'PASS_RATE', 92.3, 'percent', NOW(), NOW()),
(7, 3, 'EXECUTION_TIME', 1580.2, 'seconds', NOW(), NOW()),
(8, 3, 'PASS_RATE', 78.9, 'percent', NOW(), NOW());

-- 设置自增ID的起始值
ALTER TABLE test_tasks AUTO_INCREMENT = 100;
ALTER TABLE test_cases AUTO_INCREMENT = 100;
ALTER TABLE monitoring_data AUTO_INCREMENT = 100;
