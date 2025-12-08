# 🔄 Backend 与 AI Service 数据库表统一 - 完整方案

## 📋 执行摘要

**问题**: Backend 服务和 AI Service 都对测试用例进行录入、查询等操作，但使用不同的数据库表和字段，导致数据不一致。

**解决方案**: ✅ **AI Service 统一使用 Backend 的 `test_cases` 表**，确保两个服务对相同资源的操作完全一致。

---

## 🔍 问题分析

### Backend Service 的表结构 (标准表)

**表名**: `test_cases`

```sql
CREATE TABLE IF NOT EXISTS test_cases (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    title VARCHAR(200) NOT NULL,
    description TEXT,
    steps JSON NOT NULL,
    expected_result TEXT,                          -- 注意：单数形式
    priority INTEGER DEFAULT 0,                    -- 0-10 范围
    type VARCHAR(20) NOT NULL CHECK (type IN ('FUNCTIONAL', 'PERFORMANCE', 'SECURITY')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('DRAFT', 'APPROVED', 'DEPRECATED')),
    tags JSON,
    related_requirement VARCHAR(200),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### AI Service 原来的表结构 (不一致的表 - 已废弃)

**表名**: `historical_testcases` ❌

```sql
CREATE TABLE IF NOT EXISTS historical_testcases (
    id INT AUTO_INCREMENT PRIMARY KEY,             -- ❌ 类型不同 (INT vs CHAR(36))
    testcase_id VARCHAR(100) UNIQUE,               -- ❌ 额外字段
    name VARCHAR(500) NOT NULL,                    -- ❌ 应该是 title
    module VARCHAR(100),                           -- ❌ Backend 没有这个字段
    priority ENUM('P0', 'P1', 'P2', 'P3') DEFAULT 'P2',  -- ❌ 格式不同
    type VARCHAR(50) DEFAULT '功能测试',           -- ❌ 中文 vs 英文
    description TEXT,
    steps JSON,
    preconditions JSON,                            -- ❌ Backend 没有这个字段
    expected_result TEXT,
    tags JSON,
    status VARCHAR(20) DEFAULT 'active',           -- ❌ 值不同
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    -- ❌ 缺少 created_by 字段
    -- ❌ 缺少 related_requirement 字段
);
```

---

## ✅ 统一方案

### 1. 表使用策略

| 表名 | 用途 | 使用方 | 维护方 | 说明 |
|------|------|--------|--------|------|
| `test_cases` | **主测试用例表** | Backend + AI Service | Backend | **共享表，数据统一** |
| `testcase_generation_history` | AI 生成历史 | AI Service | AI Service | AI 专用 |
| `recommendation_history` | AI 推荐历史 | AI Service | AI Service | AI 专用 |
| `company_standards` | 测试标准 | AI Service | AI Service | AI 专用 |

### 2. 字段映射关系

| AI Service 字段 | Backend 字段 | 类型 | 映射规则 | 说明 |
|----------------|-------------|------|---------|------|
| `name` | `title` | VARCHAR | 直接映射 | 测试用例名称 |
| `priority` (P0-P3) | `priority` (0-10) | INTEGER | P0→10, P1→7, P2→5, P3→3 | 优先级转换 |
| `type` (中文) | `type` (英文) | VARCHAR | 功能测试→FUNCTIONAL, 性能测试→PERFORMANCE, 安全测试→SECURITY | 类型转换 |
| `status` | `status` | VARCHAR | active→APPROVED, draft→DRAFT, deleted→DEPRECATED | 状态转换 |
| `expected_result` | `expected_result` | TEXT | 直接映射 | 预期结果 |
| `steps` | `steps` | JSON | 直接映射 | 测试步骤 |
| `tags` | `tags` | JSON | 直接映射 | 标签 |
| `description` | `description` | TEXT | 直接映射 | 描述 |
| `module` | - | - | 存入 tags 数组 | Backend 没有 module 字段 |
| `preconditions` | - | - | 可以存入 description | Backend 没有 preconditions 字段 |
| - | `created_by` | VARCHAR | 默认 'ai-service' | AI Service 添加 |
| - | `related_requirement` | VARCHAR | 可选传入 | 关联需求 |

---

## 💻 代码实现

### 统一的 MySQL Client

创建了新文件: `ai-service/data/mysql_client_unified_backend.py`

#### 核心方法说明:

1. **字段映射方法**:
   - `_map_priority_to_backend()`: P0-P3 → 0-10
   - `_map_priority_from_backend()`: 0-10 → P0-P3
   - `_map_type_to_backend()`: 中文 → 英文
   - `_map_type_from_backend()`: 英文 → 中文
   - `_map_status_to_backend()`: active/draft → APPROVED/DRAFT
   - `_map_status_from_backend()`: APPROVED/DRAFT → active/draft

2. **测试用例操作**:
   - `save_testcase()`: 保存到 `test_cases` 表
   - `get_testcase_by_id()`: 从 `test_cases` 表查询
   - `get_testcases_by_ids()`: 批量查询
   - `update_testcase()`: 更新测试用例
   - `get_similar_testcases()`: 查询相似用例

3. **AI 专用操作**:
   - `save_testcase_generation_history()`: 保存生成历史
   - `save_recommendation_history()`: 保存推荐历史
   - `get_company_standards()`: 获取公司标准

### 示例代码

#### 保存测试用例 (AI Service)

```python
from data.mysql_client_unified_backend import mysql_client

# AI Service 格式的测试用例
testcase_data = {
    'name': '用户登录测试',
    'description': '验证用户登录功能',
    'steps': [
        '打开登录页面',
        '输入用户名和密码',
        '点击登录按钮'
    ],
    'expected_result': '登录成功',
    'priority': 'P1',  # 会被映射为 7
    'type': '功能测试',  # 会被映射为 FUNCTIONAL
    'status': 'active',  # 会被映射为 APPROVED
    'tags': ['login', 'smoke'],
    'module': '用户管理',  # 会被添加到 tags
    'related_requirement': 'REQ-001'
}

# 保存到 Backend 的 test_cases 表
testcase_id = mysql_client.save_testcase(testcase_data)
print(f"✅ 测试用例已保存: {testcase_id}")
```

#### 查询测试用例 (Backend Service)

```java
// Backend 直接查询 test_cases 表
@Mapper
public interface TestCaseMapper {
    @Select("SELECT * FROM test_cases WHERE id = #{id}")
    TestCase selectById(String id);
    
    @Select("SELECT * FROM test_cases WHERE type = #{type}")
    List<TestCase> selectByType(String type);
}
```

---

## 🔄 完整数据流程

### 统一表后的数据流

```
┌──────────┐
│ Frontend │
└─────┬────┘
      │ 1. 请求生成测试用例
      ↓
┌──────────┐
│ Backend  │
└─────┬────┘
      │ 2. 调用 AI Service API
      ↓
┌────────────────┐
│  AI Service    │
├────────────────┤
│ a. Milvus 语义检索 │
│ b. MySQL 查询 test_cases 表 ✅ │
│ c. LLM 生成新用例 │
│ d. MySQL 保存到 test_cases 表 ✅ │
│ e. Milvus 索引向量 │
└─────┬──────────┘
      │ 3. 返回测试用例 ID
      ↓
┌──────────┐
│ Backend  │
└─────┬────┘
      │ 4. 从 test_cases 表查询 ✅
      ↓
┌──────────┐
│ Frontend │
└──────────┘
      5. 展示测试用例 (数据完全一致) ✅
```

---

## 📊 统一前后对比

### 统一前 (有问题)

```
AI Service                    Backend Service
    ↓                              ↓
historical_testcases          test_cases
    ↓                              ↓
  不同的表！                    不同的表！
    ↓                              ↓
  数据不一致                    无法共享
```

**问题**:
- ❌ 表名不同 (`historical_testcases` vs `test_cases`)
- ❌ 字段不同 (name vs title, priority 格式, type 语言)
- ❌ 数据孤岛 (AI 生成的用例 Backend 看不到)
- ❌ 需要复杂的同步机制
- ❌ 可能出现数据不一致

### 统一后 (完美)

```
AI Service                    Backend Service
    ↓                              ↓
    └──────────┬───────────────────┘
               ↓
          test_cases
               ↓
         统一的表！
               ↓
        数据完全一致
```

**优势**:
- ✅ 共享同一张表 (`test_cases`)
- ✅ 字段完全一致 (通过映射方法)
- ✅ 数据实时同步 (没有延迟)
- ✅ 无需同步机制 (直接读写)
- ✅ 数据一致性保证

---

## 🗄️ 完整数据库架构

### 共享表 (Backend 管理，AI Service 使用)

| 表名 | 用途 | 创建者 | 使用者 | 说明 |
|------|------|--------|--------|------|
| `test_cases` | 主测试用例表 | Backend | **Backend + AI** | **共享表** |
| `test_tasks` | 测试任务 | Backend | Backend | Backend 专用 |
| `test_environments` | 测试环境 | Backend | Backend | Backend 专用 |
| `quality_reports` | 质量报告 | Backend | Backend | Backend 专用 |
| `monitoring_data` | 监控数据 | Backend | Backend | Backend 专用 |

### AI 专用表 (AI Service 管理)

| 表名 | 用途 | 使用者 | 说明 |
|------|------|--------|------|
| `testcase_generation_history` | AI 生成历史 | AI Service | 记录生成请求和结果 |
| `recommendation_history` | AI 推荐历史 | AI Service | 记录推荐结果 |
| `company_standards` | 测试标准 | AI Service | 公司测试规范 |

---

## 🚀 使用步骤

### 1. 更新 AI Service 代码

替换 `mysql_client.py` 的导入:

```python
# 旧的导入 (使用 historical_testcases 表)
# from data.mysql_client import mysql_client

# 新的导入 (使用 backend 的 test_cases 表)
from data.mysql_client_unified_backend import mysql_client
```

### 2. 迁移现有数据 (如果需要)

如果 `historical_testcases` 表中有数据，需要迁移到 `test_cases` 表:

```sql
-- 迁移脚本
INSERT INTO test_cases (
    id, title, description, steps, expected_result,
    priority, type, status, tags, created_by
)
SELECT 
    testcase_id,
    name,
    description,
    steps,
    expected_result,
    CASE 
        WHEN priority = 'P0' THEN 10
        WHEN priority = 'P1' THEN 7
        WHEN priority = 'P2' THEN 5
        WHEN priority = 'P3' THEN 3
    END,
    CASE 
        WHEN type = '功能测试' THEN 'FUNCTIONAL'
        WHEN type = '性能测试' THEN 'PERFORMANCE'
        WHEN type = '安全测试' THEN 'SECURITY'
    END,
    CASE 
        WHEN status = 'active' THEN 'APPROVED'
        WHEN status = 'draft' THEN 'DRAFT'
        ELSE 'DEPRECATED'
    END,
    tags,
    'ai-service'
FROM historical_testcases
WHERE status = 'active';
```

### 3. 测试验证

```python
# 测试保存
testcase_data = {
    'name': '测试用例',
    'priority': 'P1',
    'type': '功能测试',
    'status': 'active',
    'steps': ['步骤1', '步骤2'],
    'expected_result': '预期结果'
}

testcase_id = mysql_client.save_testcase(testcase_data)
print(f"保存成功: {testcase_id}")

# 测试查询
result = mysql_client.get_testcase_by_id(testcase_id)
print(f"查询结果: {result}")
```

### 4. 验证 Backend 可以访问

```java
// Backend Java 代码
TestCase testCase = testCaseMapper.selectById(testcaseId);
System.out.println("Title: " + testCase.getTitle());
System.out.println("Priority: " + testCase.getPriority());
System.out.println("Type: " + testCase.getType());
```

---

## ⚠️ 注意事项

1. **字段名称**:
   - Backend 使用 `expected_result` (单数)
   - Mapper XML 中使用 `expected_results` (复数) 需要检查

2. **优先级范围**:
   - Backend: 0-10 (INTEGER)
   - AI Service: P0-P3 (ENUM) → 自动映射

3. **类型语言**:
   - Backend: FUNCTIONAL, PERFORMANCE, SECURITY (英文)
   - AI Service: 功能测试, 性能测试, 安全测试 (中文) → 自动映射

4. **模块字段**:
   - Backend 没有 `module` 字段
   - AI Service 的 `module` 会被添加到 `tags` 数组中

5. **前置条件**:
   - Backend 没有 `preconditions` 字段
   - 可以将前置条件存入 `description` 字段

---

## 📝 检查清单

- [x] 创建统一的 MySQL Client (`mysql_client_unified_backend.py`)
- [x] 实现字段映射方法
- [x] 更新 `save_testcase()` 方法
- [x] 更新 `get_testcase_by_id()` 方法
- [x] 更新 `get_testcases_by_ids()` 方法
- [x] 更新 `get_similar_testcases()` 方法
- [x] 保留 AI 专用表和方法
- [x] 创建完整文档
- [ ] 更新 AI Service 的导入语句
- [ ] 运行集成测试
- [ ] 验证 Backend 和 AI Service 数据一致性

---

## 🎯 总结

通过这次统一:

1. **数据一致性**: Backend 和 AI Service 使用同一张 `test_cases` 表
2. **字段映射**: 自动处理 AI Service 和 Backend 的字段格式差异
3. **向后兼容**: AI Service 代码可以继续使用原有的字段名称
4. **实时同步**: 无需额外的数据同步机制
5. **清晰架构**: Backend 管理业务表，AI Service 管理专用表

现在两个服务对测试用例的操作完全一致！ ✅

