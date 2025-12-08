-- User Story 1 测试数据
INSERT INTO test_tasks (id, name, description, environment, version, test_scope, status, priority, created_by, created_at, updated_at) VALUES
('task-001', '登录模块冒烟测试', '验证登录功能', 'DEV', 'v2.1.0', 'SMOKE', 'PENDING', 8, 'zhangsan', NOW(), NOW()),
('task-002', '支付流程回归测试', '支付功能回归', 'STAGING', 'v2.1.0', 'CORE_REGRESSION', 'RUNNING', 9, 'lisi', NOW(), NOW()),
('task-pending', '待启动任务', '等待启动', 'DEV', 'v2.0.0', 'SMOKE', 'PENDING', 5, 'wangwu', NOW(), NOW());

INSERT INTO test_environments (id, name, url, status, created_at, updated_at) VALUES
('env-dev', 'DEV环境', 'http://dev.example.com', 'AVAILABLE', NOW(), NOW()),
('env-test', 'TEST环境', 'http://test.example.com', 'AVAILABLE', NOW(), NOW()),
('env-staging', 'STAGING环境', 'http://staging.example.com', 'AVAILABLE', NOW(), NOW());

INSERT INTO test_versions (id, name, product_version, release_date, created_at, updated_at) VALUES
('ver-200', 'v2.0.0', '2.0.0', '2025-10-01', NOW(), NOW()),
('ver-210', 'v2.1.0', '2.1.0', '2025-11-01', NOW(), NOW());

INSERT INTO resource_pools (id, name, type, capacity, allocated, status, created_at, updated_at) VALUES
('pool-001', 'VM资源池', 'VM', 100, 45, 'AVAILABLE', NOW(), NOW());
