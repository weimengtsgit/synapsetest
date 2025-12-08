-- User Story 2 测试数据
DELETE FROM test_cases;
INSERT INTO test_cases (id, title, description, steps, expected_result, type, status, priority, tags, created_by, created_at, updated_at) VALUES
('case-001', '验证用户登录成功', '测试正常登录流程', '["打开登录页","输入用户名","输入密码","点击登录"]', '登录成功跳转首页', 'FUNCTIONAL', 'APPROVED', 8, '["login","smoke"]', 'qa_engineer', NOW(), NOW()),
('case-002', '验证密码错误提示', '测试密码错误场景', '["打开登录页","输入用户名","输入错误密码","点击登录"]', '显示密码错误提示', 'FUNCTIONAL', 'DRAFT', 7, '["login","negative"]', 'qa_engineer', NOW(), NOW()),
('case-with-json', 'JSON字段测试', '测试JSON序列化', '["步骤1","步骤2","步骤3"]', '预期结果', 'FUNCTIONAL', 'DRAFT', 5, '["test"]', 'tester', NOW(), NOW()),
('case-to-delete', '待删除用例', '用于测试删除功能', '["步骤"]', '结果', 'FUNCTIONAL', 'DRAFT', 3, '[]', 'tester', NOW(), NOW());
