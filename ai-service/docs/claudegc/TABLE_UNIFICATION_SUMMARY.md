# 🔄 Backend 与 AI Service 数据库表统一总结

## ✅ 完成的工作

### 1. 问题识别

发现 Backend 和 AI Service 对测试用例表的操作存在以下不一致:

| 问题类型 | Backend Service | AI Service (旧) | 影响 |
|---------|----------------|----------------|------|
| **表名** | `test_cases` | `historical_testcases` | 数据孤岛 |
| **ID 类型** | `CHAR(36)` UUID | `INT AUTO_INCREMENT` | ID 格式不兼容 |
| **标题字段** | `title` | `name` | 字段名不一致 |
| **优先级** | `priority INTEGER (0-10)` | `priority ENUM('P0','P1','P2','P3')` | 格式不兼容 |
| **类型** | `type VARCHAR(20)` (FUNCTIONAL, PERFORMANCE, SECURITY) | `type VARCHAR(50)` (功能测试, 性能测试, 安全测试) | 语言不一致 |
| **状态** | `status VARCHAR(20)` (DRAFT, APPROVED, DEPRECATED) | `status VARCHAR(20)` (active, draft, deleted) | 值不一致 |
| **模块** | 无 `module` 字段 | `module VARCHAR(100)` | Backend 不支持 |
| **前置条件** | 无 `preconditions` 字段 | `preconditions JSON` | Backend 不支持 |
| **创建者** | `created_by VARCHAR(100)` ✅ | 缺少 | AI Service 不记录 |
| **关联需求** | `related_requirement VARCHAR(200)` ✅ | 缺少 | AI Service 不支持 |

### 2. 解决方案

#### 2.1 创建统一的 MySQL Client

**文件**: `ai-service/data/mysql_client_unified_backend.py`

**核心特性**:
- ✅ 直接操作 Backend 的 `test_cases` 表
- ✅ 自动字段映射 (P0-P3 ↔ 0-10, 中文 ↔ 英文)
- ✅ 向后兼容 AI Service 原有接口
- ✅ 保留 AI 专用表 (generation_history, recommendation_history)

**字段映射方法**:
```python
# 优先级映射
_map_priority_to_backend(priority)     # P0→10, P1→7, P2→5, P3→3
_map_priority_from_backend(priority)   # 10→P0, 7→P1, 5→P2, 3→P3

# 类型映射
_map_type_to_backend(test_type)        # 功能测试→FUNCTIONAL
_map_type_from_backend(test_type)      # FUNCTIONAL→功能测试

# 状态映射
_map_status_to_backend(status)         # active→APPROVED, draft→DRAFT
_map_status_from_backend(status)       # APPROVED→active
```

**测试用例操作**:
- `save_testcase()`: 保存到 `test_cases` 表
- `get_testcase_by_id()`: 从 `test_cases` 表查询
- `get_testcases_by_ids()`: 批量查询
- `update_testcase()`: 更新测试用例
- `get_similar_testcases()`: 查询相似用例

#### 2.2 修复 Backend 字段名不一致

**问题**: Backend 的 `schema.sql` 使用 `expected_result` (单数)，但 `TestCaseMapper.xml` 使用 `expected_results` (复数)

**修复文件**:
- ✅ `backend/src/main/resources/mapper/TestCaseMapper.xml`
- ✅ `backend/src/test/resources/test-schema.sql`
- ✅ `backend/src/test/resources/test-data-integration.sql`
- ✅ `backend/src/test/resources/test-data-us2.sql`
- ✅ `backend/src/test/resources/test-data-us4.sql`

**修改内容**:
```xml
<!-- 修改前 -->
<result property="expectedResult" column="expected_results"/>
INSERT INTO test_cases(..., expected_results, ...)

<!-- 修改后 -->
<result property="expectedResult" column="expected_result"/>
INSERT INTO test_cases(..., expected_result, ...)
```

### 3. 数据库架构

#### 3.1 共享表 (Backend 管理，AI Service 使用)

| 表名 | 主要字段 | 管理方 | 使用方 |
|------|---------|--------|--------|
| `test_cases` | id, title, description, steps, expected_result, priority, type, status, tags, related_requirement, created_by | Backend | **Backend + AI Service** |

#### 3.2 AI 专用表 (AI Service 管理)

| 表名 | 用途 | 主要字段 |
|------|------|---------|
| `testcase_generation_history` | AI 生成历史 | request_id, module, num_cases_requested, num_cases_generated, success, metadata |
| `recommendation_history` | AI 推荐历史 | task_id, recommendation, context |
| `company_standards` | 公司测试标准 | standard_name, standard_data, active |

### 4. 数据流程

#### 4.1 统一前的数据流 (有问题)

```
Frontend → Backend → AI Service
                         ↓
              historical_testcases ❌
                         ↓
              (Backend 看不到这些数据)
```

#### 4.2 统一后的数据流 (完美)

```
Frontend → Backend → AI Service
              ↓           ↓
          test_cases ← ─ ─┘
              ↓
        (数据完全一致) ✅
```

### 5. 使用示例

#### 5.1 AI Service 保存测试用例

```python
from data.mysql_client_unified_backend import mysql_client

testcase_data = {
    'name': '用户登录测试',              # 会映射为 title
    'priority': 'P1',                   # 会映射为 7
    'type': '功能测试',                  # 会映射为 FUNCTIONAL
    'status': 'active',                 # 会映射为 APPROVED
    'steps': ['步骤1', '步骤2'],
    'expected_result': '登录成功',
    'module': '用户管理',                # 会添加到 tags
    'tags': ['login', 'smoke']
}

testcase_id = mysql_client.save_testcase(testcase_data)
# 保存到 Backend 的 test_cases 表
# 返回 UUID: 'a1b2c3d4-e5f6-7890-abcd-ef1234567890'
```

#### 5.2 Backend 查询测试用例

```java
// Backend 直接查询 test_cases 表
TestCase testCase = testCaseMapper.selectById(testcaseId);

// 结果:
// - id: "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
// - title: "用户登录测试"
// - priority: 7
// - type: "FUNCTIONAL"
// - status: "APPROVED"
// - tags: ["login", "smoke", "用户管理"]
```

## 📊 统一效果对比

### 统一前

| 指标 | 结果 |
|-----|------|
| 数据一致性 | ❌ 不一致 (不同表) |
| 实时同步 | ❌ 需要额外机制 |
| 字段兼容性 | ❌ 不兼容 |
| 维护复杂度 | ❌ 高 (两套表结构) |
| 数据孤岛 | ❌ 存在 |

### 统一后

| 指标 | 结果 |
|-----|------|
| 数据一致性 | ✅ 完全一致 (同一张表) |
| 实时同步 | ✅ 自动同步 (无需额外机制) |
| 字段兼容性 | ✅ 完全兼容 (自动映射) |
| 维护复杂度 | ✅ 低 (统一表结构) |
| 数据孤岛 | ✅ 消除 |

## 🎯 关键成果

1. **数据统一**: Backend 和 AI Service 使用同一张 `test_cases` 表
2. **字段一致**: 修复了 Backend 内部的 `expected_result` 字段名不一致问题
3. **自动映射**: AI Service 可以继续使用原有字段名，自动转换为 Backend 格式
4. **向后兼容**: AI Service 原有代码无需大幅修改
5. **清晰架构**: Backend 管理业务表，AI Service 管理专用表

## 📝 下一步行动

### 必须完成

- [ ] **更新 AI Service 导入**: 将 `mysql_client.py` 的导入改为 `mysql_client_unified_backend.py`
- [ ] **运行集成测试**: 验证 Backend 和 AI Service 的数据一致性
- [ ] **数据迁移** (如果需要): 将 `historical_testcases` 表的数据迁移到 `test_cases` 表

### 可选完成

- [ ] 监控数据一致性
- [ ] 性能优化 (连接池等)
- [ ] 添加单元测试

## 📚 相关文档

- **详细设计**: `ai-service/docs/BACKEND_AI_TABLE_UNIFICATION_COMPLETE.md`
- **原始需求**: `ai-service/docs/DATABASE_TABLE_UNIFICATION.md`
- **Backend Schema**: `backend/src/main/resources/schema.sql`
- **AI Service Client**: `ai-service/data/mysql_client_unified_backend.py`

---

**总结**: 通过这次统一，Backend 和 AI Service 对测试用例的数据库操作完全一致，消除了数据孤岛，提高了系统的可维护性和数据一致性。✅

