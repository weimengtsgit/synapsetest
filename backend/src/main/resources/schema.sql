-- MySQL Schema for Test Management System

-- Create database
CREATE DATABASE IF NOT EXISTS test_management DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE test_management;

-- 1. 监控数据表 - 存储测试任务的实时监控指标
CREATE TABLE IF NOT EXISTS monitoring_data (
    id VARCHAR(36) NOT NULL PRIMARY KEY COMMENT '唯一标识符',
    task_id VARCHAR(36) NOT NULL COMMENT '关联的测试任务ID',
    status VARCHAR(20) NOT NULL COMMENT '状态：PENDING, RUNNING, COMPLETED, FAILED, CANCELLED',
    progress INT DEFAULT 0 COMMENT '进度百分比 (0-100)',
    executed_cases INT DEFAULT 0 COMMENT '已执行的测试用例数',
    total_cases INT DEFAULT 0 COMMENT '总测试用例数',
    passed_cases INT DEFAULT 0 COMMENT '通过的测试用例数',
    failed_cases INT DEFAULT 0 COMMENT '失败的测试用例数',
    skipped_cases INT DEFAULT 0 COMMENT '跳过的测试用例数',
    start_time DATETIME COMMENT '开始时间',
    estimated_end_time DATETIME COMMENT '预计结束时间',
    actual_end_time DATETIME COMMENT '实际结束时间',
    resource_usage JSON COMMENT '资源使用情况，存储CPU、内存等数据',
    performance_metrics JSON COMMENT '性能指标，存储响应时间、吞吐量等数据',
    timestamp DATETIME NOT NULL COMMENT '数据时间戳',
    environment VARCHAR(100) COMMENT '环境信息',
    version VARCHAR(50) COMMENT '版本信息',
    INDEX idx_task_id (task_id),
    INDEX idx_status (status),
    INDEX idx_environment (environment),
    INDEX idx_timestamp (timestamp)
-- Test Tasks Table
CREATE TABLE IF NOT EXISTS test_tasks (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    environment VARCHAR(50) NOT NULL,
    version VARCHAR(50) NOT NULL,
    test_scope VARCHAR(50),
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETED', 'CANCELLED')),
    priority INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL,
    CONSTRAINT chk_priority CHECK (priority >= 0 AND priority <= 10)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Test Cases Table
CREATE TABLE IF NOT EXISTS test_cases (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    title VARCHAR(200) NOT NULL,
    description TEXT,
    steps JSON NOT NULL,
    expected_result TEXT,
    priority INTEGER DEFAULT 0,
    type VARCHAR(20) NOT NULL CHECK (type IN ('FUNCTIONAL', 'PERFORMANCE', 'SECURITY')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('DRAFT', 'APPROVED', 'DEPRECATED')),
    tags JSON,
    related_requirement VARCHAR(200),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. 质量报告表 - 存储测试执行的质量分析报告
CREATE TABLE IF NOT EXISTS quality_report (
    id VARCHAR(36) NOT NULL PRIMARY KEY COMMENT '唯一标识符',
    task_id VARCHAR(36) NOT NULL COMMENT '关联的测试任务ID',
    name VARCHAR(255) NOT NULL COMMENT '报告名称',
    summary TEXT COMMENT '报告摘要',
    generated_at DATETIME NOT NULL COMMENT '生成时间',
    status VARCHAR(20) NOT NULL COMMENT '状态：GENERATING, COMPLETED, ARCHIVED',
    defect_stats JSON COMMENT '缺陷统计，按严重程度分类',
    performance_metrics JSON COMMENT '性能指标汇总',
    INDEX idx_task_id (task_id),
    INDEX idx_status (status),
    INDEX idx_generated_at (generated_at)
-- Test Environments Table
CREATE TABLE IF NOT EXISTS test_environments (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    url VARCHAR(500),
    config JSON,
    status VARCHAR(20) NOT NULL CHECK (status IN ('AVAILABLE', 'MAINTENANCE', 'UNAVAILABLE')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. 测试结果表 - 存储质量报告中的测试结果详情
CREATE TABLE IF NOT EXISTS quality_report_test_result (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    report_id VARCHAR(36) NOT NULL COMMENT '关联的质量报告ID',
    test_case_id VARCHAR(36) NOT NULL COMMENT '测试用例ID',
    test_case_name VARCHAR(255) NOT NULL COMMENT '测试用例名称',
    status VARCHAR(20) NOT NULL COMMENT '执行状态：PASSED, FAILED, SKIPPED, BLOCKED',
    execution_time BIGINT COMMENT '执行时间（毫秒）',
    error TEXT COMMENT '错误信息',
    screenshot VARCHAR(500) COMMENT '截图路径',
    executed_at DATETIME COMMENT '执行时间',
    INDEX idx_report_id (report_id),
    INDEX idx_test_case_id (test_case_id),
    INDEX idx_status (status),
    FOREIGN KEY (report_id) REFERENCES quality_report(id) ON DELETE CASCADE
-- Test Versions Table
CREATE TABLE IF NOT EXISTS test_versions (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    product_version VARCHAR(50) NOT NULL,
    release_date DATE,
    config JSON,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. 风险评估表 - 存储质量报告中的风险评估信息
CREATE TABLE IF NOT EXISTS quality_report_risk_assessment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    report_id VARCHAR(36) NOT NULL COMMENT '关联的质量报告ID',
    overall_risk VARCHAR(20) NOT NULL COMMENT '总体风险等级：LOW, MEDIUM, HIGH, CRITICAL',
    risk_score DOUBLE DEFAULT 0.0 COMMENT '风险分数 (0.0-1.0)',
    high_risk_modules JSON COMMENT '高风险模块列表',
    recommendations JSON COMMENT '改进建议列表',
    module_risk_scores JSON COMMENT '各模块风险分数映射',
    INDEX idx_report_id (report_id),
    INDEX idx_overall_risk (overall_risk),
    UNIQUE KEY uk_report_id (report_id),
    FOREIGN KEY (report_id) REFERENCES quality_report(id) ON DELETE CASCADE
-- Resource Pools Table
CREATE TABLE IF NOT EXISTS resource_pools (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    type VARCHAR(20) NOT NULL CHECK (type IN ('VM', 'CONTAINER', 'DEVICE')),
    capacity INTEGER NOT NULL CHECK (capacity > 0),
    allocated INTEGER DEFAULT 0 CHECK (allocated >= 0),
    location VARCHAR(200),
    config JSON,
    status VARCHAR(20) NOT NULL CHECK (status IN ('AVAILABLE', 'MAINTENANCE', 'UNAVAILABLE')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_capacity_allocated CHECK (allocated <= capacity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- AI Models Table
CREATE TABLE IF NOT EXISTS ai_models (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    name VARCHAR(100) NOT NULL,
    version VARCHAR(50) NOT NULL,
    description TEXT,
    file_path VARCHAR(500),
    security_status VARCHAR(20) NOT NULL CHECK (security_status IN ('PENDING', 'IN_REVIEW', 'APPROVED', 'REJECTED')),
    vulnerability_scan_result TEXT,
    last_scan_time TIMESTAMP,
    compliance_status VARCHAR(20) NOT NULL CHECK (compliance_status IN ('COMPLIANT', 'NON_COMPLIANT', 'PENDING')),
    metrics JSON,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Monitoring Data Table
CREATE TABLE IF NOT EXISTS monitoring_data (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    task_id CHAR(36) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED', 'CANCELLED')),
    progress INTEGER CHECK (progress >= 0 AND progress <= 100),
    executed_cases INTEGER CHECK (executed_cases >= 0),
    total_cases INTEGER CHECK (total_cases >= 0),
    passed_cases INTEGER CHECK (passed_cases >= 0),
    failed_cases INTEGER CHECK (failed_cases >= 0),
    skipped_cases INTEGER CHECK (skipped_cases >= 0),
    start_time TIMESTAMP,
    estimated_end_time TIMESTAMP,
    actual_end_time TIMESTAMP,
    resource_usage JSON,
    performance_metrics JSON,
    timestamp TIMESTAMP,
    environment VARCHAR(50),
    version VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (task_id) REFERENCES test_tasks(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. AI模型表 - 存储AI模型信息
CREATE TABLE IF NOT EXISTS ai_model (
    id VARCHAR(36) NOT NULL PRIMARY KEY COMMENT '唯一标识符',
    name VARCHAR(255) NOT NULL COMMENT '模型名称',
    version VARCHAR(50) NOT NULL COMMENT '模型版本',
    description TEXT COMMENT '模型描述',
    file_path VARCHAR(500) COMMENT '模型文件路径',
    security_status VARCHAR(20) DEFAULT 'PENDING' COMMENT '安全状态：PENDING, IN_REVIEW, APPROVED, REJECTED',
    vulnerability_scan_result TEXT COMMENT '漏洞扫描结果',
    last_scan_time DATETIME COMMENT '最后扫描时间',
    compliance_status VARCHAR(20) DEFAULT 'PENDING' COMMENT '合规状态：COMPLIANT, NON_COMPLIANT, PENDING',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    metrics JSON COMMENT '模型性能指标',
    INDEX idx_name_version (name, version),
    INDEX idx_security_status (security_status),
    INDEX idx_compliance_status (compliance_status)
-- Quality Reports Table
CREATE TABLE IF NOT EXISTS quality_reports (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    task_id CHAR(36) NOT NULL,
    name VARCHAR(200) NOT NULL,
    summary TEXT,
    test_results JSON,
    defect_stats JSON,
    performance_metrics JSON,
    risk_assessment JSON,
    generated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL CHECK (status IN ('GENERATING', 'COMPLETED', 'ARCHIVED')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (task_id) REFERENCES test_tasks(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 为支持UUID生成
DELIMITER //
CREATE FUNCTION generate_uuid_v4() RETURNS CHAR(36)
BEGIN
    SET @uuid := UUID();
    RETURN @uuid;
END//
DELIMITER ;

-- 初始数据插入示例（可选）
INSERT INTO ai_model (id, name, version, description, security_status, compliance_status, created_at, updated_at)
SELECT generate_uuid_v4(), 'Default Test Model', '1.0.0', '默认测试用例生成模型', 'APPROVED', 'COMPLIANT', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM ai_model WHERE name = 'Default Test Model' AND version = '1.0.0');
-- Create indexes for performance
CREATE INDEX idx_test_tasks_status ON test_tasks(status);
CREATE INDEX idx_test_tasks_created_at ON test_tasks(created_at);
CREATE INDEX idx_test_cases_type ON test_cases(type);
CREATE INDEX idx_test_cases_status ON test_cases(status);
CREATE INDEX idx_quality_reports_task_id ON quality_reports(task_id);
CREATE INDEX idx_quality_reports_status ON quality_reports(status);
CREATE INDEX idx_quality_reports_generated_at ON quality_reports(generated_at);
CREATE INDEX idx_test_environments_status ON test_environments(status);
CREATE INDEX idx_resource_pools_status ON resource_pools(status);
CREATE INDEX idx_ai_models_security_status ON ai_models(security_status);
CREATE INDEX idx_ai_models_compliance_status ON ai_models(compliance_status);
CREATE INDEX idx_ai_models_name ON ai_models(name);
CREATE INDEX idx_monitoring_data_task_id ON monitoring_data(task_id);
CREATE INDEX idx_monitoring_data_status ON monitoring_data(status);
CREATE INDEX idx_monitoring_data_timestamp ON monitoring_data(timestamp);
