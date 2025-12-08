-- 清理脚本 - 在每个测试方法执行后清空数据
-- 用于所有Mapper层测试

-- 按正确顺序删除数据（先子表，后父表）
DELETE FROM task_test_cases;
DELETE FROM monitoring_data;
DELETE FROM quality_reports;
DELETE FROM test_cases;
DELETE FROM test_tasks;
DELETE FROM test_environments;
DELETE FROM test_versions;
DELETE FROM resource_pools;
DELETE FROM ai_models;
-- 禁用外键检查
SET FOREIGN_KEY_CHECKS = 0;

-- 删除测试表（按照外键依赖顺序）
DROP TABLE IF EXISTS quality_report_test_result;
DROP TABLE IF EXISTS quality_report_risk_assessment;
DROP TABLE IF EXISTS quality_report;
DROP TABLE IF EXISTS monitoring_data;
DROP TABLE IF EXISTS test_cases;

-- 重新启用外键检查
SET FOREIGN_KEY_CHECKS = 1;
