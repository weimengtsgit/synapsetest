-- ============================================
-- 数据库迁移脚本: 添加case_number字段
-- 作者: AI Assistant
-- 日期: 2024-12-15
-- 目的: 实现双ID设计，添加业务友好的用例编号
-- ============================================

-- 步骤1: 添加case_number字段（先允许NULL）
ALTER TABLE test_cases 
ADD COLUMN case_number VARCHAR(20) COMMENT '业务编号(如TC20231215001)' 
AFTER id;

-- 步骤2: 为现有数据生成case_number
-- 注意：这里使用简单的序列生成，实际使用时可能需要根据created_at调整日期部分
SET @row_number = 0;
SET @today = DATE_FORMAT(NOW(), '%Y%m%d');

UPDATE test_cases 
SET case_number = CONCAT('TC', @today, LPAD((@row_number := @row_number + 1), 3, '0'))
WHERE case_number IS NULL
ORDER BY created_at;

-- 步骤3: 添加NOT NULL约束和UNIQUE约束
ALTER TABLE test_cases 
MODIFY COLUMN case_number VARCHAR(20) NOT NULL COMMENT '业务编号(如TC20231215001)';

ALTER TABLE test_cases 
ADD UNIQUE INDEX idx_case_number (case_number);

-- 验证迁移结果
SELECT 
    COUNT(*) as total_records,
    COUNT(DISTINCT case_number) as unique_case_numbers,
    MIN(case_number) as first_case_number,
    MAX(case_number) as last_case_number
FROM test_cases;

-- 显示示例数据
SELECT id, case_number, title, created_at 
FROM test_cases 
ORDER BY created_at 
LIMIT 10;

-- ============================================
-- 迁移完成说明
-- ============================================
-- 1. case_number字段已添加到test_cases表
-- 2. 现有数据已自动生成case_number
-- 3. 新增数据将由后端CaseNumberGenerator自动生成
-- 4. case_number格式: TC{yyyyMMdd}{seq} (例如: TC20231215001)
-- ============================================
