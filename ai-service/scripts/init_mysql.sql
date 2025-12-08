-- ============================================
-- AI智能测试平台 - MySQL数据库初始化脚本
-- ============================================
-- 版本: v1.0
-- 创建日期: 2025-12-04
-- 用途: 为API接口测试预置数据
-- ============================================

-- 1. 创建数据库
-- DROP DATABASE IF EXISTS synapsetest;
CREATE DATABASE IF NOT EXISTS synapsetest CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE synapsetest;

-- ============================================
-- 2. 创建表结构
-- ============================================

-- 表1: test_cases (测试用例表)
DROP TABLE IF EXISTS test_cases;
CREATE TABLE test_cases (
  id VARCHAR(50) PRIMARY KEY COMMENT '用例ID',
  name VARCHAR(200) NOT NULL COMMENT '用例名称',
  module VARCHAR(100) COMMENT '所属模块',
  priority ENUM('P0', 'P1', 'P2', 'P3') COMMENT '优先级',
  type VARCHAR(50) COMMENT '测试类型',
  preconditions JSON COMMENT '前置条件',
  steps JSON COMMENT '测试步骤',
  tags JSON COMMENT '标签',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  created_by VARCHAR(50) COMMENT '创建人',
  quality_score FLOAT COMMENT '质量评分',
  INDEX idx_module (module),
  INDEX idx_priority (priority),
  INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='测试用例表';

-- 表2: generation_history (用例生成历史表)
DROP TABLE IF EXISTS generation_history;
CREATE TABLE generation_history (
  request_id VARCHAR(50) PRIMARY KEY COMMENT '请求ID',
  user_id VARCHAR(50) COMMENT '用户ID',
  requirement_text TEXT COMMENT '需求文本',
  module VARCHAR(100) COMMENT '模块名称',
  num_cases INT COMMENT '请求生成数量',
  generated_count INT COMMENT '实际生成数量',
  generation_time FLOAT COMMENT '生成耗时(秒)',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX idx_user_id (user_id),
  INDEX idx_module (module),
  INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用例生成历史表';

-- 表3: recommendation_history (策略推荐历史表)
DROP TABLE IF EXISTS recommendation_history;
CREATE TABLE recommendation_history (
  id INT AUTO_INCREMENT PRIMARY KEY COMMENT '自增ID',
  task_id VARCHAR(50) COMMENT '任务ID',
  recommendation JSON COMMENT '推荐结果',
  risk_assessment JSON COMMENT '风险评估',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX idx_task_id (task_id),
  INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='策略推荐历史表';

-- 表4: user_feedback (用户反馈表)
DROP TABLE IF EXISTS user_feedback;
CREATE TABLE user_feedback (
  id INT AUTO_INCREMENT PRIMARY KEY COMMENT '自增ID',
  request_id VARCHAR(50) COMMENT '生成请求ID',
  rating INT COMMENT '评分(1-5)',
  comments TEXT COMMENT '评论',
  accepted_cases JSON COMMENT '接受的用例',
  rejected_cases JSON COMMENT '拒绝的用例',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX idx_request_id (request_id),
  INDEX idx_rating (rating),
  INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户反馈表';

-- ============================================
-- 3. 插入预置数据
-- ============================================

-- 3.1 插入测试用例数据
INSERT INTO test_cases (id, name, module, priority, type, preconditions, steps, tags, created_at, created_by, quality_score) VALUES
('TC001',
 '手机号+验证码正常登录',
 '用户认证',
 'P0',
 '功能测试',
 '["用户已注册", "系统正常运行"]',
 '[
   {"step":1,"action":"打开登录页面","expected":"页面正常显示"},
   {"step":2,"action":"输入手机号13800138000","expected":"手机号格式正确"},
   {"step":3,"action":"点击获取验证码","expected":"收到验证码"},
   {"step":4,"action":"输入验证码并登录","expected":"登录成功"}
 ]',
 '["登录", "认证", "验证码"]',
 NOW(),
 'admin',
 0.95),

('TC002',
 '微信第三方登录成功',
 '用户认证',
 'P0',
 '功能测试',
 '["用户已有微信账号", "微信授权正常"]',
 '[
   {"step":1,"action":"点击微信登录按钮","expected":"跳转微信授权页"},
   {"step":2,"action":"确认授权","expected":"自动登录成功"}
 ]',
 '["登录", "第三方登录", "微信"]',
 NOW(),
 'admin',
 0.92),

('TC003',
 '支付宝支付成功',
 '支付系统',
 'P0',
 '功能测试',
 '["用户已登录", "订单已创建", "支付宝账户余额充足"]',
 '[
   {"step":1,"action":"选择支付宝支付","expected":"跳转支付宝页面"},
   {"step":2,"action":"确认支付","expected":"支付成功并跳转"},
   {"step":3,"action":"查看订单状态","expected":"订单状态为已支付"}
 ]',
 '["支付", "支付宝"]',
 NOW(),
 'admin',
 0.98),

('TC004',
 '订单创建成功',
 '订单中心',
 'P1',
 '功能测试',
 '["用户已登录", "购物车有商品"]',
 '[
   {"step":1,"action":"进入购物车","expected":"显示商品列表"},
   {"step":2,"action":"点击结算","expected":"进入订单确认页"},
   {"step":3,"action":"确认订单信息","expected":"订单信息正确"},
   {"step":4,"action":"提交订单","expected":"订单创建成功"}
 ]',
 '["订单", "创建"]',
 NOW(),
 'admin',
 0.88),

('TC005',
 '商品搜索精确匹配',
 '搜索引擎',
 'P1',
 '功能测试',
 '["系统正常运行", "商品数据已加载"]',
 '[
   {"step":1,"action":"输入商品关键词","expected":"搜索框显示关键词"},
   {"step":2,"action":"点击搜索按钮","expected":"展示搜索结果"},
   {"step":3,"action":"查看搜索结果","expected":"结果精确匹配关键词"}
 ]',
 '["搜索", "商品"]',
 NOW(),
 'admin',
 0.85),

('TC006',
 '修改个人资料成功',
 '用户中心',
 'P2',
 '功能测试',
 '["用户已登录"]',
 '[
   {"step":1,"action":"进入个人资料页面","expected":"显示当前资料"},
   {"step":2,"action":"修改昵称和头像","expected":"修改成功"},
   {"step":3,"action":"保存修改","expected":"提示保存成功"}
 ]',
 '["用户", "资料"]',
 NOW(),
 'admin',
 0.90),

('TC007',
 '订单取消成功',
 '订单中心',
 'P1',
 '功能测试',
 '["用户已登录", "订单已创建未支付"]',
 '[
   {"step":1,"action":"进入订单列表","expected":"显示订单"},
   {"step":2,"action":"点击取消订单","expected":"弹出确认框"},
   {"step":3,"action":"确认取消","expected":"订单状态变为已取消"}
 ]',
 '["订单", "取消"]',
 NOW(),
 'admin',
 0.92),

('TC008',
 '商品加入购物车',
 '购物车',
 'P1',
 '功能测试',
 '["用户已登录", "商品详情页已打开"]',
 '[
   {"step":1,"action":"选择商品规格","expected":"规格选中"},
   {"step":2,"action":"点击加入购物车","expected":"提示添加成功"},
   {"step":3,"action":"查看购物车","expected":"购物车中有该商品"}
 ]',
 '["购物车", "商品"]',
 NOW(),
 'admin',
 0.93),

('TC009',
 '支付失败重试',
 '支付系统',
 'P1',
 '功能测试',
 '["用户已登录", "订单已创建", "支付余额不足"]',
 '[
   {"step":1,"action":"选择支付宝支付","expected":"跳转支付宝"},
   {"step":2,"action":"确认支付","expected":"支付失败提示"},
   {"step":3,"action":"点击重试","expected":"返回支付选择页"}
 ]',
 '["支付", "重试"]',
 NOW(),
 'admin',
 0.87),

('TC010',
 '登录失败3次锁定',
 '用户认证',
 'P0',
 '功能测试',
 '["用户已注册"]',
 '[
   {"step":1,"action":"输入错误密码登录(第1次)","expected":"提示密码错误"},
   {"step":2,"action":"输入错误密码登录(第2次)","expected":"提示密码错误"},
   {"step":3,"action":"输入错误密码登录(第3次)","expected":"账户被锁定30分钟"}
 ]',
 '["登录", "安全", "锁定"]',
 NOW(),
 'admin',
 0.96);

-- 3.2 插入用例生成历史数据
INSERT INTO generation_history (request_id, user_id, requirement_text, module, num_cases, generated_count, generation_time, created_at) VALUES
('req_abc123',
 'user001',
 '用户登录功能需求：\n1. 用户可以通过手机号+验证码登录\n2. 支持微信、支付宝第三方登录\n3. 登录失败3次后锁定账户30分钟',
 '用户认证',
 10,
 10,
 23.5,
 NOW()),

('req_def456',
 'user001',
 '在线支付功能需求：\n1. 支持支付宝、微信、银行卡支付\n2. 支付金额范围: 0.01元-50000元\n3. 支付失败后支持重试',
 '支付系统',
 15,
 14,
 35.2,
 NOW()),

('req_ghi789',
 'user002',
 '订单管理功能需求：\n1. 创建订单\n2. 修改订单\n3. 取消订单\n4. 订单查询',
 '订单中心',
 12,
 12,
 28.8,
 NOW()),

('req_jkl012',
 'user002',
 '商品搜索功能需求：\n1. 支持关键词搜索\n2. 支持分类筛选\n3. 支持价格排序',
 '搜索引擎',
 8,
 8,
 18.6,
 NOW()),

('req_mno345',
 'user003',
 '购物车功能需求：\n1. 添加商品到购物车\n2. 修改商品数量\n3. 删除商品\n4. 清空购物车',
 '购物车',
 10,
 9,
 22.3,
 NOW());

-- 3.3 插入策略推荐历史数据
INSERT INTO recommendation_history (task_id, recommendation, risk_assessment, created_at) VALUES
('TASK-2024-001',
 '{
   "test_scope": "FULL",
   "environment": "STAGING",
   "priority": 9,
   "estimated_duration": 135,
   "resource_requirement": 5,
   "confidence": 0.87
 }',
 '{
   "risk_level": "HIGH",
   "risk_factors": [
     "支付模块历史缺陷率18%,高于平均值",
     "代码复杂度上升20%",
     "测试覆盖率下降5%"
   ]
 }',
 NOW()),

('TASK-2024-002',
 '{
   "test_scope": "SMOKE",
   "environment": "DEV",
   "priority": 3,
   "estimated_duration": 25,
   "resource_requirement": 1,
   "confidence": 0.92
 }',
 '{
   "risk_level": "LOW",
   "risk_factors": []
 }',
 NOW()),

('TASK-2024-003',
 '{
   "test_scope": "CORE",
   "environment": "STAGING",
   "priority": 6,
   "estimated_duration": 75,
   "resource_requirement": 3,
   "confidence": 0.85
 }',
 '{
   "risk_level": "MEDIUM",
   "risk_factors": [
     "用户模块变更较大",
     "历史通过率92%"
   ]
 }',
 NOW());

-- 3.4 插入用户反馈数据
INSERT INTO user_feedback (request_id, rating, comments, accepted_cases, rejected_cases, created_at) VALUES
('req_abc123',
 5,
 '生成的用例非常完整,覆盖了所有场景,步骤清晰,直接可用!',
 '["TC001", "TC002", "TC003", "TC004", "TC005"]',
 '[]',
 NOW()),

('req_def456',
 4,
 '大部分用例质量不错,个别用例需要微调',
 '["TC001", "TC002", "TC003", "TC004"]',
 '["TC008"]',
 NOW()),

('req_ghi789',
 3,
 '部分用例还可以,但有些用例步骤不够详细,需要修改',
 '["TC001", "TC002"]',
 '["TC005", "TC008"]',
 NOW());

-- ============================================
-- 4. 数据验证
-- ============================================

-- 验证数据插入成功
SELECT '=== 数据验证 ===' AS info;
SELECT CONCAT('test_cases 表: ', COUNT(*), ' 条记录') AS result FROM test_cases;
SELECT CONCAT('generation_history 表: ', COUNT(*), ' 条记录') AS result FROM generation_history;
SELECT CONCAT('recommendation_history 表: ', COUNT(*), ' 条记录') AS result FROM recommendation_history;
SELECT CONCAT('user_feedback 表: ', COUNT(*), ' 条记录') AS result FROM user_feedback;

-- 显示部分数据
SELECT '=== 测试用例示例 ===' AS info;
SELECT id, name, module, priority FROM test_cases LIMIT 5;

SELECT '=== 生成历史示例 ===' AS info;
SELECT request_id, module, num_cases, generated_count, ROUND(generation_time, 2) as time_sec FROM generation_history LIMIT 3;

SELECT '=== 推荐历史示例 ===' AS info;
SELECT task_id, JSON_EXTRACT(recommendation, '$.test_scope') as test_scope, JSON_EXTRACT(risk_assessment, '$.risk_level') as risk_level FROM recommendation_history LIMIT 3;

-- ============================================
-- 5. 完成提示
-- ============================================
SELECT '✅ MySQL数据库初始化完成!' AS status;
SELECT 'ℹ️ 数据库名称: synapsetest' AS info;
SELECT CONCAT('ℹ️ 总共创建 ',
  (SELECT COUNT(*) FROM test_cases) +
  (SELECT COUNT(*) FROM generation_history) +
  (SELECT COUNT(*) FROM recommendation_history) +
  (SELECT COUNT(*) FROM user_feedback),
  ' 条记录') AS info;
