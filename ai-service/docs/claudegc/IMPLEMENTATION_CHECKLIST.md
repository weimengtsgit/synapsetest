# ✅ 数据库表统一 - 实施检查清单

## 📋 已完成的工作

### ✅ 1. 问题分析与设计

- [x] 分析 Backend 和 AI Service 的表结构差异
- [x] 设计字段映射方案
- [x] 确定共享表和专用表策略

### ✅ 2. 代码实现

- [x] 创建统一的 MySQL Client: `ai-service/data/mysql_client_unified_backend.py`
- [x] 实现字段映射方法 (priority, type, status)
- [x] 实现测试用例 CRUD 操作
- [x] 保留 AI 专用表操作

### ✅ 3. 修复 Backend 字段名不一致

- [x] 修复 `backend/src/main/resources/mapper/TestCaseMapper.xml`
- [x] 修复 `backend/src/test/resources/test-schema.sql`
- [x] 修复 `backend/src/test/resources/test-data-integration.sql`
- [x] 修复 `backend/src/test/resources/test-data-us2.sql`
- [x] 修复 `backend/src/test/resources/test-data-us4.sql`

**修复内容**: `expected_results` (复数) → `expected_result` (单数)

### ✅ 4. 文档编写

- [x] 完整设计文档: `ai-service/docs/BACKEND_AI_TABLE_UNIFICATION_COMPLETE.md`
- [x] 实施总结文档: `ai-service/docs/TABLE_UNIFICATION_SUMMARY.md`
- [x] 可视化对比文档: `ai-service/docs/TABLE_COMPARISON_VISUAL.md`
- [x] 本检查清单: `ai-service/docs/IMPLEMENTATION_CHECKLIST.md`

---

## 🚀 待实施步骤

### 📍 Step 1: 更新 AI Service 代码

**优先级**: 🔴 高

**文件修改**:

#### 1.1 更新 RAG Manager 导入

**文件**: `ai-service/data/rag_manager.py`

```python
# 查找以下代码:
from data.mysql_client import mysql_client

# 替换为:
from data.mysql_client_unified_backend import mysql_client
```

#### 1.2 更新服务层导入 (如果有直接使用)

**检查以下文件**:
- `ai-service/services/testcase_service.py`
- `ai-service/services/recommendation_service.py`
- `ai-service/models/llm/rag_generator.py`

**替换代码**:
```python
# 旧的导入
from data.mysql_client import mysql_client

# 新的导入
from data.mysql_client_unified_backend import mysql_client
```

#### 1.3 更新 API 层 (如果有直接使用)

**检查以下文件**:
- `ai-service/api/testcase.py`
- `ai-service/api/recommendation.py`

---

### 📍 Step 2: 测试验证

**优先级**: 🔴 高

#### 2.1 单元测试

**命令**:
```bash
cd ai-service
pytest tests/models/test_semantic_deduplicator.py -v
pytest tests/models/test_strategy_recommender.py -v
```

**预期结果**: 所有测试通过

#### 2.2 集成测试 - AI Service

**测试脚本**: 创建 `ai-service/tests/test_mysql_unified.py`

```python
import pytest
from data.mysql_client_unified_backend import mysql_client

def test_save_and_get_testcase():
    """测试保存和查询测试用例"""
    # 保存测试用例
    testcase_data = {
        'name': '测试-统一表验证',
        'priority': 'P1',
        'type': '功能测试',
        'status': 'active',
        'module': '测试模块',
        'tags': ['test'],
        'steps': ['步骤1', '步骤2'],
        'expected_result': '测试通过'
    }
    
    testcase_id = mysql_client.save_testcase(testcase_data)
    assert testcase_id is not None
    
    # 查询测试用例
    result = mysql_client.get_testcase_by_id(testcase_id)
    assert result is not None
    assert result['title'] == '测试-统一表验证'
    assert result['priority'] == 7  # P1 → 7
    assert result['type'] == 'FUNCTIONAL'  # 功能测试 → FUNCTIONAL
    assert result['status'] == 'APPROVED'  # active → APPROVED
    assert '测试模块' in result['tags']  # module 添加到 tags

def test_field_mapping():
    """测试字段映射"""
    # 测试优先级映射
    assert mysql_client._map_priority_to_backend('P0') == 10
    assert mysql_client._map_priority_to_backend('P1') == 7
    assert mysql_client._map_priority_to_backend('P2') == 5
    assert mysql_client._map_priority_to_backend('P3') == 3
    
    # 测试类型映射
    assert mysql_client._map_type_to_backend('功能测试') == 'FUNCTIONAL'
    assert mysql_client._map_type_to_backend('性能测试') == 'PERFORMANCE'
    assert mysql_client._map_type_to_backend('安全测试') == 'SECURITY'
    
    # 测试状态映射
    assert mysql_client._map_status_to_backend('active') == 'APPROVED'
    assert mysql_client._map_status_to_backend('draft') == 'DRAFT'
```

**运行测试**:
```bash
pytest ai-service/tests/test_mysql_unified.py -v
```

#### 2.3 集成测试 - Backend 访问

**测试脚本**: 创建 `backend/src/test/java/.../TestCaseUnificationTest.java`

```java
@Test
public void testAIServiceGeneratedTestCase() {
    // AI Service 应该已经生成了一个测试用例
    // Backend 应该能够查询到它
    
    List<TestCase> testCases = testCaseMapper.selectByCreatedBy("ai-service");
    assertFalse(testCases.isEmpty());
    
    TestCase testCase = testCases.get(0);
    assertNotNull(testCase.getId());
    assertNotNull(testCase.getTitle());
    assertTrue(testCase.getPriority() >= 0 && testCase.getPriority() <= 10);
    assertTrue(Arrays.asList("FUNCTIONAL", "PERFORMANCE", "SECURITY")
        .contains(testCase.getType()));
}
```

#### 2.4 端到端测试

**测试流程**:
1. Frontend 请求生成测试用例
2. Backend 调用 AI Service API
3. AI Service 保存到 `test_cases` 表
4. Backend 查询 `test_cases` 表
5. Frontend 展示测试用例

**验证点**:
- [ ] AI Service 能成功保存测试用例
- [ ] Backend 能查询到 AI Service 保存的用例
- [ ] 数据字段完全一致 (priority, type, status)
- [ ] 没有字段缺失或格式错误

---

### 📍 Step 3: 数据迁移 (如果需要)

**优先级**: 🟡 中

**检查是否需要迁移**:
```sql
-- 检查旧表是否有数据
SELECT COUNT(*) FROM historical_testcases WHERE status = 'active';
```

**如果有数据，执行迁移**:

#### 3.1 创建迁移脚本

**文件**: `ai-service/scripts/migrate_to_unified_table.sql`

```sql
-- 数据迁移脚本
-- 将 historical_testcases 迁移到 test_cases

START TRANSACTION;

-- 插入数据 (带字段映射)
INSERT INTO test_cases (
    id, 
    title, 
    description, 
    steps, 
    expected_result,
    priority, 
    type, 
    status, 
    tags, 
    created_by,
    created_at,
    updated_at
)
SELECT 
    -- 生成 UUID (如果 testcase_id 不是 UUID 格式)
    IF(testcase_id REGEXP '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$',
       testcase_id,
       UUID()),
    
    -- name → title
    name,
    description,
    steps,
    expected_result,
    
    -- priority 映射: P0→10, P1→7, P2→5, P3→3
    CASE priority
        WHEN 'P0' THEN 10
        WHEN 'P1' THEN 7
        WHEN 'P2' THEN 5
        WHEN 'P3' THEN 3
        ELSE 5
    END,
    
    -- type 映射: 中文 → 英文
    CASE type
        WHEN '功能测试' THEN 'FUNCTIONAL'
        WHEN '性能测试' THEN 'PERFORMANCE'
        WHEN '安全测试' THEN 'SECURITY'
        ELSE 'FUNCTIONAL'
    END,
    
    -- status 映射: active→APPROVED, draft→DRAFT
    CASE status
        WHEN 'active' THEN 'APPROVED'
        WHEN 'draft' THEN 'DRAFT'
        ELSE 'DEPRECATED'
    END,
    
    -- tags: 添加 module 到 tags (如果有)
    CASE 
        WHEN module IS NOT NULL AND tags IS NOT NULL THEN
            JSON_MERGE_PRESERVE(tags, JSON_ARRAY(module))
        WHEN module IS NOT NULL THEN
            JSON_ARRAY(module)
        ELSE tags
    END,
    
    -- created_by
    'ai-service-migrated',
    
    created_at,
    updated_at
FROM historical_testcases
WHERE status = 'active'
  AND NOT EXISTS (
      SELECT 1 FROM test_cases 
      WHERE test_cases.title = historical_testcases.name
  );

-- 检查迁移结果
SELECT 
    '迁移前记录数' AS type,
    COUNT(*) AS count
FROM historical_testcases
WHERE status = 'active'

UNION ALL

SELECT 
    '迁移后记录数' AS type,
    COUNT(*) AS count
FROM test_cases
WHERE created_by IN ('ai-service', 'ai-service-migrated');

-- 如果检查无误，提交事务
COMMIT;

-- 如果有问题，回滚
-- ROLLBACK;
```

#### 3.2 执行迁移

```bash
# 备份数据库
mysqldump -u root -p test_management > backup_before_migration.sql

# 执行迁移
mysql -u root -p test_management < ai-service/scripts/migrate_to_unified_table.sql

# 验证迁移结果
mysql -u root -p test_management -e "
SELECT 
    'historical_testcases' AS table_name,
    COUNT(*) AS count
FROM historical_testcases
WHERE status = 'active'

UNION ALL

SELECT 
    'test_cases (ai-service)' AS table_name,
    COUNT(*) AS count
FROM test_cases
WHERE created_by LIKE 'ai-service%';
"
```

#### 3.3 迁移后验证

- [ ] 迁移数据量正确
- [ ] 字段映射正确 (priority, type, status)
- [ ] tags 包含原来的 module
- [ ] 没有重复数据

---

### 📍 Step 4: 部署与监控

**优先级**: 🟢 中

#### 4.1 更新配置

**检查 AI Service 配置**:
- [ ] MySQL 连接配置正确
- [ ] 数据库名称一致
- [ ] 权限配置正确

#### 4.2 滚动部署

```bash
# 1. 部署 AI Service (新版本)
kubectl apply -f k8s/ai-service-deployment.yaml

# 2. 验证 AI Service 健康检查
kubectl get pods -l app=ai-service

# 3. 测试 AI Service API
curl -X POST http://ai-service/api/testcase/generate \
  -H "Content-Type: application/json" \
  -d '{"module": "测试模块", "num_cases": 1}'

# 4. 验证 Backend 能查询到新数据
curl http://backend/api/test-cases?createdBy=ai-service
```

#### 4.3 监控指标

**关键指标**:
- [ ] AI Service 测试用例保存成功率
- [ ] Backend 查询 AI Service 生成用例的成功率
- [ ] 数据一致性检查 (字段格式)
- [ ] MySQL 查询性能

**监控脚本** (可选):
```python
# ai-service/scripts/monitor_data_consistency.py
import mysql.connector
import logging

def check_data_consistency():
    """检查数据一致性"""
    conn = mysql.connector.connect(...)
    cursor = conn.cursor()
    
    # 检查 AI Service 生成的测试用例
    cursor.execute("""
        SELECT 
            COUNT(*) as total,
            SUM(CASE WHEN priority < 0 OR priority > 10 THEN 1 ELSE 0 END) as invalid_priority,
            SUM(CASE WHEN type NOT IN ('FUNCTIONAL', 'PERFORMANCE', 'SECURITY') THEN 1 ELSE 0 END) as invalid_type,
            SUM(CASE WHEN status NOT IN ('DRAFT', 'APPROVED', 'DEPRECATED') THEN 1 ELSE 0 END) as invalid_status
        FROM test_cases
        WHERE created_by = 'ai-service'
    """)
    
    result = cursor.fetchone()
    total, invalid_priority, invalid_type, invalid_status = result
    
    if invalid_priority > 0 or invalid_type > 0 or invalid_status > 0:
        logging.error(f"数据一致性检查失败: invalid_priority={invalid_priority}, invalid_type={invalid_type}, invalid_status={invalid_status}")
        return False
    
    logging.info(f"数据一致性检查通过: total={total}")
    return True

if __name__ == '__main__':
    check_data_consistency()
```

---

## 🔍 验证检查清单

### 功能验证

- [ ] AI Service 能成功保存测试用例到 `test_cases` 表
- [ ] Backend 能查询到 AI Service 保存的测试用例
- [ ] 字段映射正确:
  - [ ] Priority: P1 → 7
  - [ ] Type: 功能测试 → FUNCTIONAL
  - [ ] Status: active → APPROVED
- [ ] Module 正确添加到 tags
- [ ] UUID 格式正确
- [ ] created_by 字段为 'ai-service'

### 性能验证

- [ ] 保存操作响应时间 < 500ms
- [ ] 查询操作响应时间 < 200ms
- [ ] 批量查询性能正常

### 兼容性验证

- [ ] Backend Java 代码能正确解析 AI Service 保存的数据
- [ ] AI Service Python 代码能正确解析 Backend 保存的数据
- [ ] JSON 字段 (steps, tags) 解析正常

---

## 📞 问题排查

### 常见问题

#### 问题 1: 字段名不匹配

**症状**: Backend 查询报错 `Unknown column 'expected_results'`

**解决方案**: 确保已修复所有 Backend mapper 和 SQL 文件中的字段名

**检查命令**:
```bash
grep -r "expected_results" backend/src/
```

#### 问题 2: 优先级超出范围

**症状**: 插入数据时报错 `CHECK constraint failed`

**解决方案**: 检查映射方法是否正确

**验证代码**:
```python
assert mysql_client._map_priority_to_backend('P0') == 10
assert mysql_client._map_priority_to_backend('P1') == 7
```

#### 问题 3: 类型值不匹配

**症状**: 插入数据时报错 `CHECK constraint failed: type IN (...)`

**解决方案**: 确保类型映射正确

**验证代码**:
```python
assert mysql_client._map_type_to_backend('功能测试') == 'FUNCTIONAL'
```

#### 问题 4: JSON 字段解析失败

**症状**: Backend 查询后 steps 或 tags 为空

**解决方案**: 检查 JSON 序列化格式

**验证代码**:
```python
import json
steps = ['步骤1', '步骤2']
steps_json = json.dumps(steps, ensure_ascii=False)
print(steps_json)  # 应该是: ["步骤1", "步骤2"]
```

---

## 📚 参考文档

- [完整设计文档](./BACKEND_AI_TABLE_UNIFICATION_COMPLETE.md)
- [实施总结](./TABLE_UNIFICATION_SUMMARY.md)
- [可视化对比](./TABLE_COMPARISON_VISUAL.md)
- [Backend Schema](../../backend/src/main/resources/schema.sql)
- [统一的 MySQL Client](../data/mysql_client_unified_backend.py)

---

## ✅ 完成标准

### 最小可行标准 (MVP)

- [x] 统一的 MySQL Client 实现完成
- [x] Backend 字段名修复完成
- [x] 文档编写完成
- [ ] AI Service 代码更新完成
- [ ] 单元测试通过
- [ ] 集成测试通过
- [ ] 至少一个端到端测试通过

### 完整标准

- [ ] 所有单元测试通过
- [ ] 所有集成测试通过
- [ ] 端到端测试通过
- [ ] 数据迁移完成 (如果需要)
- [ ] 生产环境部署完成
- [ ] 监控指标正常
- [ ] 文档更新完成

---

## 📝 下一步行动

1. **立即执行**:
   - [ ] 更新 AI Service 导入语句
   - [ ] 运行单元测试

2. **短期计划** (1-2 天):
   - [ ] 运行集成测试
   - [ ] 执行端到端测试
   - [ ] 数据迁移 (如果需要)

3. **中期计划** (1 周):
   - [ ] 生产环境部署
   - [ ] 监控数据一致性
   - [ ] 性能优化

---

**更新时间**: 2025-11-30
**负责人**: AI Service 团队
**优先级**: 🔴 高

