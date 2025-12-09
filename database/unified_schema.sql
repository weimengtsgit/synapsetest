-- ============================================
-- SynapseTest 统一数据库Schema
-- ============================================
-- 版本: v2.0
-- 日期: 2025-12-09
-- 用途: Backend和AI-Service统一数据库表结构
-- 合并来源:
--   - backend/src/main/resources/schema.sql
--   - ai-service/scripts/init_mysql.sql
-- ============================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS synapsetest CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE synapsetest;

-- ============================================
-- 核心业务表 (Backend原有)
-- ============================================

-- 1. 测试任务表
DROP TABLE IF EXISTS test_tasks;
CREATE TABLE test_tasks (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '任务ID',
    name VARCHAR(100) NOT NULL COMMENT '任务名称',
    description TEXT COMMENT '任务描述',
    environment VARCHAR(50) NOT NULL COMMENT '测试环境',
    version VARCHAR(50) NOT NULL COMMENT '测试版本',
    test_scope VARCHAR(50) COMMENT '测试范围',
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETED', 'CANCELLED')) COMMENT '状态',
    priority INTEGER DEFAULT 0 CHECK (priority >= 0 AND priority <= 10) COMMENT '优先级(0-10)',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by VARCHAR(100) NOT NULL COMMENT '创建人',

    INDEX idx_status (status),
    INDEX idx_environment (environment),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='测试任务表';

-- 2. 测试用例表 (Backend + AI-Service合并版本)
DROP TABLE IF EXISTS test_cases;
CREATE TABLE test_cases (
    -- 基础字段 (Backend)
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '用例ID',
    title VARCHAR(200) NOT NULL COMMENT '用例标题',
    description TEXT COMMENT '用例描述',
    steps JSON NOT NULL COMMENT '测试步骤',
    expected_result TEXT COMMENT '预期结果',

    -- 分类字段 (Backend + AI-Service)
    module VARCHAR(100) COMMENT '所属模块',
    type VARCHAR(20) NOT NULL DEFAULT 'FUNCTIONAL' CHECK (type IN ('FUNCTIONAL', 'PERFORMANCE', 'SECURITY')) COMMENT '测试类型',
    priority INTEGER DEFAULT 5 CHECK (priority >= 0 AND priority <= 10) COMMENT '优先级(0-10)',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'APPROVED', 'DEPRECATED')) COMMENT '状态',
    tags JSON COMMENT '标签',
    related_requirement VARCHAR(200) COMMENT '关联需求',

    -- AI增强字段 (AI-Service)
    preconditions JSON COMMENT '前置条件',
    quality_score FLOAT DEFAULT 0.0 COMMENT 'AI质量评分(0.0-1.0)',
    ai_generated BOOLEAN DEFAULT FALSE COMMENT '是否AI生成',
    ai_confidence FLOAT DEFAULT 0.0 COMMENT 'AI置信度(0.0-1.0)',

    -- 审计字段
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by VARCHAR(100) NOT NULL COMMENT '创建人',

    -- 索引
    INDEX idx_module (module),
    INDEX idx_type (type),
    INDEX idx_priority (priority),
    INDEX idx_status (status),
    INDEX idx_ai_generated (ai_generated),
    INDEX idx_quality_score (quality_score),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='测试用例表(Backend+AI统一)';

-- 3. 测试环境表
DROP TABLE IF EXISTS test_environments;
CREATE TABLE test_environments (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '环境ID',
    name VARCHAR(100) NOT NULL UNIQUE COMMENT '环境名称',
    description TEXT COMMENT '环境描述',
    url VARCHAR(500) COMMENT '环境URL',
    config JSON COMMENT '配置信息',
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE' CHECK (status IN ('AVAILABLE', 'MAINTENANCE', 'UNAVAILABLE')) COMMENT '状态',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_status (status),
    INDEX idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='测试环境表';

-- 4. 测试版本表
DROP TABLE IF EXISTS test_versions;
CREATE TABLE test_versions (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '版本ID',
    name VARCHAR(100) NOT NULL COMMENT '版本名称',
    description TEXT COMMENT '版本描述',
    product_version VARCHAR(50) NOT NULL COMMENT '产品版本号',
    release_date DATE COMMENT '发布日期',
    config JSON COMMENT '配置信息',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_product_version (product_version),
    INDEX idx_release_date (release_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='测试版本表';

-- 5. 资源池表
DROP TABLE IF EXISTS resource_pools;
CREATE TABLE resource_pools (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '资源池ID',
    name VARCHAR(100) NOT NULL UNIQUE COMMENT '资源池名称',
    description TEXT COMMENT '资源池描述',
    type VARCHAR(20) NOT NULL CHECK (type IN ('VM', 'CONTAINER', 'DEVICE')) COMMENT '资源类型',
    capacity INTEGER NOT NULL CHECK (capacity > 0) COMMENT '总容量',
    allocated INTEGER DEFAULT 0 CHECK (allocated >= 0) COMMENT '已分配',
    location VARCHAR(200) COMMENT '位置',
    config JSON COMMENT '配置信息',
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE' CHECK (status IN ('AVAILABLE', 'MAINTENANCE', 'UNAVAILABLE')) COMMENT '状态',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_type (type),
    INDEX idx_status (status),

    -- 表级别约束: allocated不能超过capacity
    CONSTRAINT chk_allocated_capacity CHECK (allocated <= capacity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资源池表';

-- ============================================
-- AI相关表 (Backend + AI-Service合并)
-- ============================================

-- 6. AI模型表
DROP TABLE IF EXISTS ai_models;
CREATE TABLE ai_models (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '模型ID',
    name VARCHAR(100) NOT NULL COMMENT '模型名称',
    version VARCHAR(50) NOT NULL COMMENT '模型版本',
    description TEXT COMMENT '模型描述',
    file_path VARCHAR(500) COMMENT '模型文件路径',
    security_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (security_status IN ('PENDING', 'IN_REVIEW', 'APPROVED', 'REJECTED')) COMMENT '安全状态',
    vulnerability_scan_result TEXT COMMENT '漏洞扫描结果',
    last_scan_time TIMESTAMP COMMENT '最后扫描时间',
    compliance_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (compliance_status IN ('COMPLIANT', 'NON_COMPLIANT', 'PENDING')) COMMENT '合规状态',
    metrics JSON COMMENT '模型性能指标',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_name_version (name, version),
    INDEX idx_security_status (security_status),
    INDEX idx_compliance_status (compliance_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI模型表';

-- 7. 测试用例生成历史表 (AI-Service)
DROP TABLE IF EXISTS testcase_generation_history;
CREATE TABLE testcase_generation_history (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '记录ID',
    request_id VARCHAR(50) UNIQUE COMMENT '请求ID(兼容旧版)',
    user_id VARCHAR(50) COMMENT '用户ID',
    requirement_text TEXT COMMENT '需求文本',
    module VARCHAR(100) COMMENT '模块名称',
    num_cases_requested INT COMMENT '请求生成数量',
    num_cases_generated INT COMMENT '实际生成数量',
    generation_time_ms BIGINT COMMENT '生成耗时(毫秒)',
    llm_provider VARCHAR(50) COMMENT 'LLM提供商',
    model_name VARCHAR(100) COMMENT '模型名称',
    success BOOLEAN DEFAULT TRUE COMMENT '是否成功',
    error_message TEXT COMMENT '错误信息',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    INDEX idx_user_id (user_id),
    INDEX idx_module (module),
    INDEX idx_request_id (request_id),
    INDEX idx_created_at (created_at),
    INDEX idx_success (success)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='测试用例生成历史';

-- 8. 策略推荐历史表 (AI-Service)
DROP TABLE IF EXISTS recommendation_history;
CREATE TABLE recommendation_history (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '记录ID',
    task_id VARCHAR(50) COMMENT '任务ID',
    environment_id CHAR(36) COMMENT '环境ID',
    version_id CHAR(36) COMMENT '版本ID',
    recommendation JSON NOT NULL COMMENT '推荐结果',
    risk_assessment JSON COMMENT '风险评估',
    context JSON COMMENT '上下文信息',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    INDEX idx_task_id (task_id),
    INDEX idx_environment_id (environment_id),
    INDEX idx_version_id (version_id),
    INDEX idx_created_at (created_at),

    FOREIGN KEY (environment_id) REFERENCES test_environments(id) ON DELETE SET NULL,
    FOREIGN KEY (version_id) REFERENCES test_versions(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='策略推荐历史';

-- 9. 用户反馈表 (AI-Service)
DROP TABLE IF EXISTS user_feedback;
CREATE TABLE user_feedback (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '反馈ID',
    request_id VARCHAR(50) COMMENT '生成请求ID',
    rating INT CHECK (rating >= 1 AND rating <= 5) COMMENT '评分(1-5)',
    feedback_type VARCHAR(20) DEFAULT 'GENERATION' CHECK (feedback_type IN ('GENERATION', 'RECOMMENDATION', 'QUALITY')) COMMENT '反馈类型',
    comments TEXT COMMENT '评论',
    accepted_cases JSON COMMENT '接受的用例ID列表',
    rejected_cases JSON COMMENT '拒绝的用例ID列表',
    user_id VARCHAR(50) COMMENT '用户ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    INDEX idx_request_id (request_id),
    INDEX idx_rating (rating),
    INDEX idx_feedback_type (feedback_type),
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at),

    FOREIGN KEY (request_id) REFERENCES testcase_generation_history(request_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户反馈表';

-- 10. 公司标准表 (AI-Service)
DROP TABLE IF EXISTS company_standards;
CREATE TABLE company_standards (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '标准ID',
    name VARCHAR(200) NOT NULL COMMENT '标准名称',
    description TEXT COMMENT '标准描述',
    category VARCHAR(50) COMMENT '标准类别',
    standard_data JSON NOT NULL COMMENT '标准内容(JSON格式)',
    version VARCHAR(50) DEFAULT '1.0' COMMENT '版本号',
    status VARCHAR(20) DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'DEPRECATED', 'DRAFT')) COMMENT '状态',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_category (category),
    INDEX idx_status (status),
    INDEX idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公司测试标准';

-- ============================================
-- 监控和报告表 (Backend原有)
-- ============================================

-- 11. 监控数据表
DROP TABLE IF EXISTS monitoring_data;
CREATE TABLE monitoring_data (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '监控数据ID',
    task_id CHAR(36) NOT NULL COMMENT '关联的测试任务ID',
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED', 'CANCELLED')) COMMENT '状态',
    progress INTEGER DEFAULT 0 CHECK (progress >= 0 AND progress <= 100) COMMENT '进度百分比',
    executed_cases INTEGER DEFAULT 0 CHECK (executed_cases >= 0) COMMENT '已执行用例数',
    total_cases INTEGER DEFAULT 0 CHECK (total_cases >= 0) COMMENT '总用例数',
    passed_cases INTEGER DEFAULT 0 CHECK (passed_cases >= 0) COMMENT '通过用例数',
    failed_cases INTEGER DEFAULT 0 CHECK (failed_cases >= 0) COMMENT '失败用例数',
    skipped_cases INTEGER DEFAULT 0 CHECK (skipped_cases >= 0) COMMENT '跳过用例数',
    start_time TIMESTAMP COMMENT '开始时间',
    estimated_end_time TIMESTAMP COMMENT '预计结束时间',
    actual_end_time TIMESTAMP COMMENT '实际结束时间',
    resource_usage JSON COMMENT '资源使用情况',
    performance_metrics JSON COMMENT '性能指标',
    timestamp TIMESTAMP COMMENT '数据时间戳',
    environment VARCHAR(50) COMMENT '环境信息',
    version VARCHAR(50) COMMENT '版本信息',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_task_id (task_id),
    INDEX idx_status (status),
    INDEX idx_timestamp (timestamp),

    FOREIGN KEY (task_id) REFERENCES test_tasks(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='监控数据表';

-- 12. 质量报告表
DROP TABLE IF EXISTS quality_reports;
CREATE TABLE quality_reports (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '报告ID',
    task_id CHAR(36) NOT NULL COMMENT '关联的测试任务ID',
    name VARCHAR(200) NOT NULL COMMENT '报告名称',
    summary TEXT COMMENT '报告摘要',
    test_results JSON COMMENT '测试结果',
    defect_stats JSON COMMENT '缺陷统计',
    performance_metrics JSON COMMENT '性能指标',
    risk_assessment JSON COMMENT '风险评估',
    generated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
    status VARCHAR(20) NOT NULL DEFAULT 'GENERATING' CHECK (status IN ('GENERATING', 'COMPLETED', 'ARCHIVED')) COMMENT '状态',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_task_id (task_id),
    INDEX idx_status (status),
    INDEX idx_generated_at (generated_at),

    FOREIGN KEY (task_id) REFERENCES test_tasks(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='质量报告表';

-- 13. 质量报告测试结果表
DROP TABLE IF EXISTS quality_report_test_results;
CREATE TABLE quality_report_test_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    report_id CHAR(36) NOT NULL COMMENT '关联的质量报告ID',
    test_case_id CHAR(36) NOT NULL COMMENT '测试用例ID',
    test_case_name VARCHAR(255) NOT NULL COMMENT '测试用例名称',
    status VARCHAR(20) NOT NULL CHECK (status IN ('PASSED', 'FAILED', 'SKIPPED', 'BLOCKED')) COMMENT '执行状态',
    execution_time BIGINT COMMENT '执行时间（毫秒）',
    error TEXT COMMENT '错误信息',
    screenshot VARCHAR(500) COMMENT '截图路径',
    executed_at DATETIME COMMENT '执行时间',

    INDEX idx_report_id (report_id),
    INDEX idx_test_case_id (test_case_id),
    INDEX idx_status (status),

    FOREIGN KEY (report_id) REFERENCES quality_reports(id) ON DELETE CASCADE,
    FOREIGN KEY (test_case_id) REFERENCES test_cases(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='质量报告测试结果表';

-- 14. 质量报告风险评估表
DROP TABLE IF EXISTS quality_report_risk_assessments;
CREATE TABLE quality_report_risk_assessments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    report_id CHAR(36) NOT NULL UNIQUE COMMENT '关联的质量报告ID',
    overall_risk VARCHAR(20) NOT NULL CHECK (overall_risk IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')) COMMENT '总体风险等级',
    risk_score DOUBLE DEFAULT 0.0 CHECK (risk_score >= 0.0 AND risk_score <= 1.0) COMMENT '风险分数',
    high_risk_modules JSON COMMENT '高风险模块列表',
    recommendations JSON COMMENT '改进建议列表',
    module_risk_scores JSON COMMENT '各模块风险分数',

    INDEX idx_report_id (report_id),
    INDEX idx_overall_risk (overall_risk),

    FOREIGN KEY (report_id) REFERENCES quality_reports(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='质量报告风险评估表';

-- ============================================
-- 初始化数据
-- ============================================

-- 插入默认AI模型
INSERT INTO ai_models (id, name, version, description, security_status, compliance_status, created_at, updated_at)
VALUES (UUID(), 'Default Test Case Generator', '1.0.0', '默认测试用例生成模型', 'APPROVED', 'COMPLIANT', NOW(), NOW());

-- 插入测试环境
INSERT INTO test_environments (id, name, description, status, created_at, updated_at)
VALUES
    (UUID(), 'DEV', '开发环境', 'AVAILABLE', NOW(), NOW()),
    (UUID(), 'STAGING', '预发环境', 'AVAILABLE', NOW(), NOW()),
    (UUID(), 'PROD', '生产环境', 'AVAILABLE', NOW(), NOW());

-- ============================================
-- 测试预置数据 (用于开发和测试)
-- ============================================

-- 插入测试用例数据 (已转换为新表结构)
INSERT INTO test_cases (id, title, description, steps, expected_result, priority, type, status, tags, module, preconditions, quality_score, ai_generated, ai_confidence, created_at, created_by) VALUES
('TC001',
 '手机号+验证码正常登录',
 '测试用户通过手机号和验证码进行正常登录流程',
 '[
   {"step":1,"action":"打开登录页面","expected":"页面正常显示"},
   {"step":2,"action":"输入手机号13800138000","expected":"手机号格式正确"},
   {"step":3,"action":"点击获取验证码","expected":"收到验证码"},
   {"step":4,"action":"输入验证码并登录","expected":"登录成功"}
 ]',
 '用户成功登录系统,跳转到首页',
 10,  -- P0 -> 10
 'FUNCTIONAL',
 'APPROVED',
 '["登录", "认证", "验证码"]',
 '用户认证',
 '["用户已注册", "系统正常运行"]',
 0.95,
 FALSE,
 0.0,
 NOW(),
 'admin'),

('TC002',
 '微信第三方登录成功',
 '测试用户通过微信进行第三方登录',
 '[
   {"step":1,"action":"点击微信登录按钮","expected":"跳转微信授权页"},
   {"step":2,"action":"确认授权","expected":"自动登录成功"}
 ]',
 '用户通过微信授权成功登录系统',
 10,  -- P0 -> 10
 'FUNCTIONAL',
 'APPROVED',
 '["登录", "第三方登录", "微信"]',
 '用户认证',
 '["用户已有微信账号", "微信授权正常"]',
 0.92,
 FALSE,
 0.0,
 NOW(),
 'admin'),

('TC003',
 '支付宝支付成功',
 '测试用户使用支付宝完成支付流程',
 '[
   {"step":1,"action":"选择支付宝支付","expected":"跳转支付宝页面"},
   {"step":2,"action":"确认支付","expected":"支付成功并跳转"},
   {"step":3,"action":"查看订单状态","expected":"订单状态为已支付"}
 ]',
 '支付成功,订单状态更新为已支付',
 10,  -- P0 -> 10
 'FUNCTIONAL',
 'APPROVED',
 '["支付", "支付宝"]',
 '支付系统',
 '["用户已登录", "订单已创建", "支付宝账户余额充足"]',
 0.98,
 FALSE,
 0.0,
 NOW(),
 'admin'),

('TC004',
 '订单创建成功',
 '测试用户从购物车创建订单的完整流程',
 '[
   {"step":1,"action":"进入购物车","expected":"显示商品列表"},
   {"step":2,"action":"点击结算","expected":"进入订单确认页"},
   {"step":3,"action":"确认订单信息","expected":"订单信息正确"},
   {"step":4,"action":"提交订单","expected":"订单创建成功"}
 ]',
 '订单创建成功,显示订单详情页',
 7,   -- P1 -> 7
 'FUNCTIONAL',
 'APPROVED',
 '["订单", "创建"]',
 '订单中心',
 '["用户已登录", "购物车有商品"]',
 0.88,
 FALSE,
 0.0,
 NOW(),
 'admin'),

('TC005',
 '商品搜索精确匹配',
 '测试商品搜索功能的精确匹配能力',
 '[
   {"step":1,"action":"输入商品关键词","expected":"搜索框显示关键词"},
   {"step":2,"action":"点击搜索按钮","expected":"展示搜索结果"},
   {"step":3,"action":"查看搜索结果","expected":"结果精确匹配关键词"}
 ]',
 '搜索结果准确匹配用户输入的关键词',
 7,   -- P1 -> 7
 'FUNCTIONAL',
 'APPROVED',
 '["搜索", "商品"]',
 '搜索引擎',
 '["系统正常运行", "商品数据已加载"]',
 0.85,
 FALSE,
 0.0,
 NOW(),
 'admin'),

('TC006',
 '修改个人资料成功',
 '测试用户修改个人资料的功能',
 '[
   {"step":1,"action":"进入个人资料页面","expected":"显示当前资料"},
   {"step":2,"action":"修改昵称和头像","expected":"修改成功"},
   {"step":3,"action":"保存修改","expected":"提示保存成功"}
 ]',
 '个人资料修改成功并保存',
 5,   -- P2 -> 5
 'FUNCTIONAL',
 'APPROVED',
 '["用户", "资料"]',
 '用户中心',
 '["用户已登录"]',
 0.90,
 FALSE,
 0.0,
 NOW(),
 'admin'),

('TC007',
 '订单取消成功',
 '测试用户取消未支付订单的功能',
 '[
   {"step":1,"action":"进入订单列表","expected":"显示订单"},
   {"step":2,"action":"点击取消订单","expected":"弹出确认框"},
   {"step":3,"action":"确认取消","expected":"订单状态变为已取消"}
 ]',
 '订单成功取消,状态更新为已取消',
 7,   -- P1 -> 7
 'FUNCTIONAL',
 'APPROVED',
 '["订单", "取消"]',
 '订单中心',
 '["用户已登录", "订单已创建未支付"]',
 0.92,
 FALSE,
 0.0,
 NOW(),
 'admin'),

('TC008',
 '商品加入购物车',
 '测试用户将商品加入购物车的功能',
 '[
   {"step":1,"action":"选择商品规格","expected":"规格选中"},
   {"step":2,"action":"点击加入购物车","expected":"提示添加成功"},
   {"step":3,"action":"查看购物车","expected":"购物车中有该商品"}
 ]',
 '商品成功添加到购物车',
 7,   -- P1 -> 7
 'FUNCTIONAL',
 'APPROVED',
 '["购物车", "商品"]',
 '购物车',
 '["用户已登录", "商品详情页已打开"]',
 0.93,
 FALSE,
 0.0,
 NOW(),
 'admin'),

('TC009',
 '支付失败重试',
 '测试支付失败后的重试机制',
 '[
   {"step":1,"action":"选择支付宝支付","expected":"跳转支付宝"},
   {"step":2,"action":"确认支付","expected":"支付失败提示"},
   {"step":3,"action":"点击重试","expected":"返回支付选择页"}
 ]',
 '支付失败后可以重新选择支付方式',
 7,   -- P1 -> 7
 'FUNCTIONAL',
 'APPROVED',
 '["支付", "重试"]',
 '支付系统',
 '["用户已登录", "订单已创建", "支付余额不足"]',
 0.87,
 FALSE,
 0.0,
 NOW(),
 'admin'),

('TC010',
 '登录失败3次锁定',
 '测试账户安全机制:登录失败3次后锁定账户',
 '[
   {"step":1,"action":"输入错误密码登录(第1次)","expected":"提示密码错误"},
   {"step":2,"action":"输入错误密码登录(第2次)","expected":"提示密码错误"},
   {"step":3,"action":"输入错误密码登录(第3次)","expected":"账户被锁定30分钟"}
 ]',
 '连续3次登录失败后,账户被锁定30分钟',
 10,  -- P0 -> 10
 'SECURITY',
 'APPROVED',
 '["登录", "安全", "锁定"]',
 '用户认证',
 '["用户已注册"]',
 0.96,
 FALSE,
 0.0,
 NOW(),
 'admin');

-- 插入用例生成历史数据
INSERT INTO testcase_generation_history (request_id, user_id, requirement_text, module, num_cases_requested, num_cases_generated, generation_time_ms, llm_provider, model_name, success, created_at) VALUES
('req_abc123',
 'user001',
 '用户登录功能需求：\n1. 用户可以通过手机号+验证码登录\n2. 支持微信、支付宝第三方登录\n3. 登录失败3次后锁定账户30分钟',
 '用户认证',
 10,
 10,
 23500,  -- 23.5秒 -> 23500毫秒
 'qwen-api',
 'qwen-plus',
 TRUE,
 NOW()),

('req_def456',
 'user001',
 '在线支付功能需求：\n1. 支持支付宝、微信、银行卡支付\n2. 支付金额范围: 0.01元-50000元\n3. 支付失败后支持重试',
 '支付系统',
 15,
 14,
 35200,  -- 35.2秒 -> 35200毫秒
 'qwen-api',
 'qwen-plus',
 TRUE,
 NOW()),

('req_ghi789',
 'user002',
 '订单管理功能需求：\n1. 创建订单\n2. 修改订单\n3. 取消订单\n4. 订单查询',
 '订单中心',
 12,
 12,
 28800,  -- 28.8秒 -> 28800毫秒
 'qwen-api',
 'qwen-plus',
 TRUE,
 NOW()),

('req_jkl012',
 'user002',
 '商品搜索功能需求：\n1. 支持关键词搜索\n2. 支持分类筛选\n3. 支持价格排序',
 '搜索引擎',
 8,
 8,
 18600,  -- 18.6秒 -> 18600毫秒
 'qwen-api',
 'qwen-plus',
 TRUE,
 NOW()),

('req_mno345',
 'user003',
 '购物车功能需求：\n1. 添加商品到购物车\n2. 修改商品数量\n3. 删除商品\n4. 清空购物车',
 '购物车',
 10,
 9,
 22300,  -- 22.3秒 -> 22300毫秒
 'qwen-api',
 'qwen-plus',
 TRUE,
 NOW());

-- 插入用户反馈数据
INSERT INTO user_feedback (id, request_id, rating, feedback_type, comments, accepted_cases, rejected_cases, user_id, created_at) VALUES
(UUID(),
 'req_abc123',
 5,
 'GENERATION',
 '生成的用例非常完整,覆盖了所有场景,步骤清晰,直接可用!',
 '["TC001", "TC002", "TC003", "TC004", "TC005"]',
 '[]',
 'user001',
 NOW()),

(UUID(),
 'req_def456',
 4,
 'GENERATION',
 '大部分用例质量不错,个别用例需要微调',
 '["TC001", "TC002", "TC003", "TC004"]',
 '["TC008"]',
 'user001',
 NOW()),

(UUID(),
 'req_ghi789',
 3,
 'GENERATION',
 '部分用例还可以,但有些用例步骤不够详细,需要修改',
 '["TC001", "TC002"]',
 '["TC005", "TC008"]',
 'user002',
 NOW());

-- ============================================
-- 数据验证
-- ============================================

SELECT '=== 统一数据库Schema创建完成 ===' AS status;
SELECT CONCAT('数据库名称: synapsetest') AS info;
SELECT CONCAT('总共创建 ', COUNT(*), ' 张表') AS info
FROM information_schema.tables
WHERE table_schema = 'synapsetest';

-- 显示所有表
SELECT table_name, table_comment
FROM information_schema.tables
WHERE table_schema = 'synapsetest'
ORDER BY table_name;

SELECT '✅ 统一Schema初始化完成!' AS status;
