-- Test Schema for MySQL Database

-- Disable foreign key checks to allow dropping tables
SET FOREIGN_KEY_CHECKS = 0;

-- Drop tables if exists
DROP TABLE IF EXISTS quality_report_risk_assessment;
DROP TABLE IF EXISTS quality_report_test_result;
DROP TABLE IF EXISTS quality_report;
DROP TABLE IF EXISTS monitoring_data;
DROP TABLE IF EXISTS test_cases;
DROP TABLE IF EXISTS test_tasks;

-- Re-enable foreign key checks
SET FOREIGN_KEY_CHECKS = 1;

-- Create test_cases table
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
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create test_tasks table
CREATE TABLE test_tasks (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    environment VARCHAR(50) NOT NULL,
    version VARCHAR(50) NOT NULL,
    test_scope VARCHAR(50),
    status VARCHAR(20) NOT NULL,
    priority INTEGER DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create monitoring_data table (aligned with main schema.sql)
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
    INDEX idx_status (status),
    INDEX idx_environment (environment)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create quality_report table
CREATE TABLE quality_report (
    id VARCHAR(36) PRIMARY KEY,
    task_id VARCHAR(36) NOT NULL,
    report_name VARCHAR(255) NOT NULL,
    total_cases INT DEFAULT 0,
    passed_cases INT DEFAULT 0,
    failed_cases INT DEFAULT 0,
    skipped_cases INT DEFAULT 0,
    pass_rate DOUBLE DEFAULT 0.0,
    execution_time BIGINT DEFAULT 0,
    environment VARCHAR(100),
    version VARCHAR(50),
    summary TEXT,
    recommendations TEXT,
    created_by VARCHAR(100),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_task_id (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;