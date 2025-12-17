-- ============================================
-- 数据库迁移脚本: 添加任务-测试用例关联表
-- ============================================
-- 版本: v2.1
-- 日期: 2024-12-16
-- 用途: 实现测试任务与测试用例的关联关系
-- 作者: Claude AI Assistant
-- ============================================

USE synapsetest;

-- 创建任务-测试用例关联表
DROP TABLE IF EXISTS task_test_cases;
CREATE TABLE task_test_cases (
    task_id CHAR(36) NOT NULL COMMENT '任务ID',
    test_case_id CHAR(36) NOT NULL COMMENT '测试用例ID',
    execution_order INTEGER COMMENT '执行顺序',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    PRIMARY KEY (task_id, test_case_id),
    
    INDEX idx_task_id (task_id),
    INDEX idx_test_case_id (test_case_id),
    INDEX idx_execution_order (execution_order),
    
    FOREIGN KEY (task_id) REFERENCES test_tasks(id) ON DELETE CASCADE,
    FOREIGN KEY (test_case_id) REFERENCES test_cases(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务-测试用例关联表';

-- 验证表创建
SELECT CONCAT('✅ task_test_cases 表创建成功') AS status;

-- 显示表结构
DESCRIBE task_test_cases;

-- 显示索引
SHOW INDEX FROM task_test_cases;

-- 验证外键约束
SELECT 
    TABLE_NAME,
    COLUMN_NAME,
    CONSTRAINT_NAME,
    REFERENCED_TABLE_NAME,
    REFERENCED_COLUMN_NAME
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = 'synapsetest'
AND TABLE_NAME = 'task_test_cases'
AND REFERENCED_TABLE_NAME IS NOT NULL;

SELECT '✅ task_test_cases 表迁移完成!' AS status;
