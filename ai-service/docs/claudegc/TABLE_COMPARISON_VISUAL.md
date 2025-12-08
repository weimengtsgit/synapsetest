# 📊 Backend 与 AI Service 表结构对比与统一

## 对比总览

### 统一前 - 存在的问题 ❌

```
┌─────────────────────────────────────────────────────────────────┐
│                        Backend Service                           │
├─────────────────────────────────────────────────────────────────┤
│  Table: test_cases                                              │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ id                CHAR(36) PRIMARY KEY (UUID)            │  │
│  │ title             VARCHAR(200)                           │  │
│  │ description       TEXT                                   │  │
│  │ steps             JSON                                   │  │
│  │ expected_result   TEXT                                   │  │
│  │ priority          INTEGER (0-10)                         │  │
│  │ type              VARCHAR(20) [FUNCTIONAL, PERFORMANCE]  │  │
│  │ status            VARCHAR(20) [DRAFT, APPROVED, ...]    │  │
│  │ tags              JSON                                   │  │
│  │ related_requirement VARCHAR(200)                         │  │
│  │ created_by        VARCHAR(100)                           │  │
│  │ created_at        TIMESTAMP                              │  │
│  │ updated_at        TIMESTAMP                              │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              ❌
                         数据不互通
                              ❌
┌─────────────────────────────────────────────────────────────────┐
│                        AI Service (旧)                          │
├─────────────────────────────────────────────────────────────────┤
│  Table: historical_testcases                                    │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ id                INT AUTO_INCREMENT PRIMARY KEY         │  │
│  │ testcase_id       VARCHAR(100) UNIQUE                    │  │
│  │ name              VARCHAR(500)         ❌ 应该是 title   │  │
│  │ module            VARCHAR(100)         ❌ Backend 没有    │  │
│  │ priority          ENUM('P0','P1','P2','P3') ❌ 格式不同  │  │
│  │ type              VARCHAR(50) [功能测试, ...] ❌ 中文     │  │
│  │ description       TEXT                                   │  │
│  │ steps             JSON                                   │  │
│  │ preconditions     JSON                   ❌ Backend 没有  │  │
│  │ expected_result   TEXT                                   │  │
│  │ tags              JSON                                   │  │
│  │ status            VARCHAR(20) [active, ...] ❌ 值不同    │  │
│  │ created_at        TIMESTAMP                              │  │
│  │ updated_at        TIMESTAMP                              │  │
│  │                   ❌ 缺少 created_by                     │  │
│  │                   ❌ 缺少 related_requirement            │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

### 统一后 - 完美解决 ✅

```
┌─────────────────────────────────────────────────────────────────┐
│                     Backend Service                              │
│                            ↓                                     │
│                    直接操作 test_cases 表                         │
│                            ↓                                     │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │              test_cases (共享表)                         │  │
│  │ ────────────────────────────────────────────────────────  │  │
│  │ id                CHAR(36) UUID                          │  │
│  │ title             VARCHAR(200)                           │  │
│  │ description       TEXT                                   │  │
│  │ steps             JSON                                   │  │
│  │ expected_result   TEXT                                   │  │
│  │ priority          INTEGER (0-10)                         │  │
│  │ type              VARCHAR(20) [FUNCTIONAL, PERFORMANCE]  │  │
│  │ status            VARCHAR(20) [DRAFT, APPROVED, ...]    │  │
│  │ tags              JSON                                   │  │
│  │ related_requirement VARCHAR(200)                         │  │
│  │ created_by        VARCHAR(100)                           │  │
│  │ created_at        TIMESTAMP                              │  │
│  │ updated_at        TIMESTAMP                              │  │
│  └──────────────────────────────────────────────────────────┘  │
│                            ↑                                     │
│                    通过映射层操作 test_cases 表                  │
│                            ↑                                     │
└────────────────────────────┼─────────────────────────────────────┘
                             │
┌────────────────────────────┴─────────────────────────────────────┐
│                     AI Service (新)                              │
│                                                                  │
│  mysql_client_unified_backend.py (映射层)                        │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ 字段映射方法:                                             │  │
│  │ • _map_priority_to_backend()    P1 → 7                  │  │
│  │ • _map_type_to_backend()        功能测试 → FUNCTIONAL    │  │
│  │ • _map_status_to_backend()      active → APPROVED       │  │
│  │ • save_testcase()               保存到 test_cases        │  │
│  │ • get_testcase_by_id()          从 test_cases 查询      │  │
│  │ • module → 添加到 tags                                   │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                  │
│  AI 专用表 (不影响 Backend):                                     │
│  • testcase_generation_history (生成历史)                       │
│  • recommendation_history (推荐历史)                            │
│  • company_standards (测试标准)                                 │
└──────────────────────────────────────────────────────────────────┘
```

## 字段映射详解

### 1. 优先级映射 (Priority)

```
AI Service (P0-P3)  →  Backend (0-10)
─────────────────────────────────────
      P0            →      10
      P1            →       7
      P2            →       5
      P3            →       3

反向映射:
Backend (0-10)  →  AI Service (P0-P3)
─────────────────────────────────────
    9-10        →      P0
    7-8         →      P1
    4-6         →      P2
    0-3         →      P3
```

### 2. 类型映射 (Type)

```
AI Service (中文)  →  Backend (英文)
─────────────────────────────────────
    功能测试       →    FUNCTIONAL
    性能测试       →    PERFORMANCE
    安全测试       →    SECURITY

反向映射:
Backend (英文)  →  AI Service (中文)
─────────────────────────────────────
  FUNCTIONAL    →    功能测试
  PERFORMANCE   →    性能测试
  SECURITY      →    安全测试
```

### 3. 状态映射 (Status)

```
AI Service      →  Backend
────────────────────────────
   active       →  APPROVED
   draft        →  DRAFT
   deleted      →  DEPRECATED
   deprecated   →  DEPRECATED

反向映射:
Backend      →  AI Service
────────────────────────────
 APPROVED    →  active
 DRAFT       →  draft
 DEPRECATED  →  deprecated
```

### 4. 特殊字段处理

| AI Service 字段 | Backend 处理方式 | 说明 |
|----------------|-----------------|------|
| `name` | 映射为 `title` | 字段名转换 |
| `module` | 添加到 `tags` 数组 | Backend 没有 module 字段 |
| `preconditions` | 可存入 `description` | Backend 没有 preconditions 字段 |
| - | 自动添加 `created_by = 'ai-service'` | AI Service 标识 |

## 数据流示例

### 示例 1: AI Service 生成测试用例

```python
# AI Service 代码
testcase_data = {
    'name': '用户登录-正常流程',
    'priority': 'P1',
    'type': '功能测试',
    'status': 'active',
    'module': '用户管理',
    'tags': ['login', 'smoke'],
    'steps': ['打开登录页', '输入凭证', '点击登录'],
    'expected_result': '登录成功'
}

testcase_id = mysql_client.save_testcase(testcase_data)
# 返回: 'a1b2c3d4-...'
```

**实际保存到 Backend test_cases 表的数据**:
```sql
INSERT INTO test_cases VALUES (
    'a1b2c3d4-...',                    -- id (UUID)
    '用户登录-正常流程',                -- title
    '',                                -- description
    '["打开登录页", "输入凭证", ...]',  -- steps (JSON)
    '登录成功',                         -- expected_result
    7,                                 -- priority (P1 → 7)
    'FUNCTIONAL',                      -- type (功能测试 → FUNCTIONAL)
    'APPROVED',                        -- status (active → APPROVED)
    '["login", "smoke", "用户管理"]',  -- tags (添加了 module)
    '',                                -- related_requirement
    'ai-service',                      -- created_by
    NOW(),                             -- created_at
    NOW()                              -- updated_at
);
```

### 示例 2: Backend 查询测试用例

```java
// Backend Java 代码
TestCase testCase = testCaseMapper.selectById("a1b2c3d4-...");

// 查询结果:
// {
//   "id": "a1b2c3d4-...",
//   "title": "用户登录-正常流程",
//   "priority": 7,
//   "type": "FUNCTIONAL",
//   "status": "APPROVED",
//   "tags": ["login", "smoke", "用户管理"],
//   "expectedResult": "登录成功",
//   "createdBy": "ai-service"
// }
```

### 示例 3: AI Service 查询测试用例

```python
# AI Service 查询
testcase = mysql_client.get_testcase_by_id("a1b2c3d4-...")

# 查询结果 (自动反向映射):
# {
#   "id": "a1b2c3d4-...",
#   "title": "用户登录-正常流程",
#   "name": "用户登录-正常流程",        # 映射 title → name
#   "priority": 7,
#   "priority_level": "P1",            # 映射 7 → P1
#   "type": "FUNCTIONAL",
#   "test_type": "功能测试",            # 映射 FUNCTIONAL → 功能测试
#   "status": "APPROVED",
#   "tags": ["login", "smoke", "用户管理"],
#   "expected_result": "登录成功",
#   "created_by": "ai-service"
# }
```

## 修复的 Backend 内部问题

### 问题: expected_result 字段名不一致

**Schema.sql** 使用 `expected_result` (单数):
```sql
CREATE TABLE test_cases (
    ...
    expected_result TEXT,
    ...
);
```

**TestCaseMapper.xml** 原来使用 `expected_results` (复数) ❌:
```xml
<result property="expectedResult" column="expected_results"/>
INSERT INTO test_cases(..., expected_results, ...)
UPDATE test_cases SET expected_results = ...
```

**修复后** 统一使用 `expected_result` (单数) ✅:
```xml
<result property="expectedResult" column="expected_result"/>
INSERT INTO test_cases(..., expected_result, ...)
UPDATE test_cases SET expected_result = ...
```

## 完整的表结构对比

| 字段 | Backend test_cases | AI Service historical_testcases | 映射方式 |
|-----|-------------------|--------------------------------|----------|
| **主键** | `id CHAR(36)` UUID | `id INT AUTO_INCREMENT` | ✅ AI Service 生成 UUID |
| **外部ID** | - | `testcase_id VARCHAR(100)` | ❌ 不再使用 |
| **标题** | `title VARCHAR(200)` | `name VARCHAR(500)` | ✅ name → title |
| **描述** | `description TEXT` | `description TEXT` | ✅ 直接映射 |
| **步骤** | `steps JSON` | `steps JSON` | ✅ 直接映射 |
| **预期结果** | `expected_result TEXT` | `expected_result TEXT` | ✅ 直接映射 |
| **优先级** | `priority INTEGER (0-10)` | `priority ENUM('P0','P1','P2','P3')` | ✅ P1 → 7 |
| **类型** | `type VARCHAR(20)` EN | `type VARCHAR(50)` CN | ✅ 功能测试 → FUNCTIONAL |
| **状态** | `status VARCHAR(20)` | `status VARCHAR(20)` | ✅ active → APPROVED |
| **标签** | `tags JSON` | `tags JSON` | ✅ 直接映射 + module |
| **模块** | - | `module VARCHAR(100)` | ✅ 添加到 tags |
| **前置条件** | - | `preconditions JSON` | ⚠️ 可存入 description |
| **关联需求** | `related_requirement VARCHAR(200)` | - | ✅ AI Service 可传入 |
| **创建者** | `created_by VARCHAR(100)` | - | ✅ 默认 'ai-service' |
| **创建时间** | `created_at TIMESTAMP` | `created_at TIMESTAMP` | ✅ 自动生成 |
| **更新时间** | `updated_at TIMESTAMP` | `updated_at TIMESTAMP` | ✅ 自动更新 |

## 总结

### ✅ 解决的问题

1. **表名统一**: 都使用 `test_cases` 表
2. **字段一致**: 修复了 Backend 内部的 `expected_result` 字段名问题
3. **数据互通**: AI Service 和 Backend 共享同一数据源
4. **自动映射**: 字段格式差异通过映射层自动转换
5. **向后兼容**: AI Service 代码无需大幅修改

### 📊 效果对比

| 指标 | 统一前 | 统一后 |
|-----|-------|-------|
| 数据一致性 | ❌ 两套数据 | ✅ 完全一致 |
| 字段兼容性 | ❌ 格式不同 | ✅ 自动映射 |
| 实时同步 | ❌ 需要同步机制 | ✅ 直接共享 |
| 维护成本 | ❌ 高 | ✅ 低 |
| 数据孤岛 | ❌ 存在 | ✅ 消除 |

### 🎯 核心价值

通过统一数据库表和字段映射，实现了 Backend 和 AI Service 对测试用例操作的完全一致性，消除了数据孤岛，提高了系统的可维护性和可靠性。✅

