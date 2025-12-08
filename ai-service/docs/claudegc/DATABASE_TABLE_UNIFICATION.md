# 📊 Backend 与 AI Service 数据库表统一方案

## 🎯 核心问题

**用户需求**：Backend 服务和 AI Service 都操作测试用例，需要确保两个服务对 MySQL 数据库表操作一致。

**解决方案**：✅ AI Service 使用 Backend 的 `test_cases` 表，实现数据统一。

---

## 📋 表结构对比

### Backend Service 的表结构（标准）

```sql
-- Backend 使用的表名: test_cases
CREATE TABLE test_cases (
    id CHAR(36) PRIMARY KEY,              -- UUID 格式
    title VARCHAR(200) NOT NULL,          -- 测试用例标题
    description TEXT,                     -- 描述
    steps JSON NOT NULL,                  -- 测试步骤（JSON）
    expected_result TEXT,                 -- 预期结果
    priority INTEGER DEFAULT 0,           -- 优先级 0-10
    type VARCHAR(20) NOT NULL,            -- FUNCTIONAL, PERFORMANCE, SECURITY
    status VARCHAR(20) NOT NULL,          -- DRAFT, APPROVED, DEPRECATED
    tags JSON,                            -- 标签（JSON）
    related_requirement VARCHAR(200),     -- 关联需求
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL      -- 创建人
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### AI Service 的旧表结构（已废弃）

```sql
-- AI Service 之前使用: historical_testcases (已删除)
CREATE TABLE historical_testcases (
    id INT AUTO_INCREMENT PRIMARY KEY,    -- ❌ 不同！
    testcase_id VARCHAR(100),             -- ❌ 额外字段
    name VARCHAR(500),                    -- ❌ 应该是 title
    module VARCHAR(100),                  -- ❌ Backend 没有
    priority ENUM('P0','P1','P2','P3'),  -- ❌ 格式不同
    type VARCHAR(50),                     -- ❌ 长度不同
    description TEXT,
    steps JSON,
    preconditions JSON,                   -- ❌ Backend 没有
    expected_result TEXT,
    tags JSON,
    status VARCHAR(20),                   -- ❌ 值不同
    created_at TIMESTAMP,
    updated_at TIMESTAMP
    -- ❌ 缺少 created_by
);
```

---

## ✅ 统一后的方案

### 表使用策略

| 表名 | 用途 | 使用方 | 说明 |
|------|------|--------|------|
| `test_cases` | **主测试用例表** | Backend + AI Service | **共享表** |
| `testcase_generation_history` | AI 生成历史 | AI Service | AI 专用 |
| `recommendation_history` | AI 推荐历史 | AI Service | AI 专用 |
| `company_standards` | 测试标准 | AI Service | AI 专用 |

---

## 🔄 字段映射关系

### AI Service → Backend 字段映射

| AI Service 字段 | Backend 字段 | 类型 | 映射规则 |
|----------------|-------------|------|---------|
| `name` | `title` | VARCHAR | 直接映射 |
| `priority` (P0-P3) | `priority` (0-10) | INTEGER | P0→10, P1→7, P2→5, P3→3 |
| `type` (中文) | `type` (英文) | VARCHAR | 功能测试→FUNCTIONAL, 性能测试→PERFORMANCE |
| `status` | `status` | VARCHAR | active→APPROVED, deleted→DEPRECATED |
| `expected_result` | `expected_result` | TEXT | 直接映射 |
| `steps` | `steps` | JSON | 直接映射 |
| `tags` | `tags` | JSON | 直接映射 |
| `module` | - | - | ⚠️ Backend 没有，可用 tags 代替 |
| `preconditions` | - | - | ⚠️ Backend 没有，可存入 description |
| - | `created_by` | VARCHAR | AI Service 添加默认值 'ai-service' |
| - | `related_requirement` | VARCHAR | AI Service 可选传入 |

---

## 💻 代码实现

### 保存测试用例（AI Service）

```python
def save_testcase(self, testcase_data: Dict[str, Any]) -> Optional[str]:
    """
    Save to Backend's test_cases table
    
    AI Service format → Backend format
    """
    import uuid
    
    testcase_id = str(uuid.uuid4())
    
    # Field mapping
    title = testcase_data.get('name') or testcase_data.get('title')
    priority = self._map_priority(testcase_data.get('priority', 'P2'))
    test_type = self._map_type(testcase_data.get('type', '功能测试'))
    status = self._map_status(testcase_data.get('status', 'active'))
    
    cursor.execute("""
        INSERT INTO test_cases
        (id, title, description, steps, expected_result, priority, 
         type, status, tags, related_requirement, created_by)
        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
    """, (
        testcase_id,
        title,
        testcase_data.get('description'),
        json.dumps(testcase_data.get('steps', [])),
        testcase_data.get('expected_result'),
        priority,
        test_type,
        status,
        json.dumps(testcase_data.get('tags', [])),
        testcase_data.get('related_requirement'),
        'ai-service'
    ))
    
    return testcase_id
```

### 查询测试用例（Backend Service）

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

## 🔄 数据流程

### 完整的数据流（统一表）

```
1. Frontend 请求生成测试用例
   ↓
2. Backend 调用 AI Service
   ↓
3. AI Service:
   a. Milvus 语义检索（历史相似用例）
   b. MySQL 查询完整数据（test_cases 表）
   c. LLM 生成新用例
   d. MySQL 保存到 test_cases 表 ✅
   e. Milvus 索引向量 ✅
   ↓
4. AI Service 返回结果给 Backend
   ↓
5. Backend 直接从 test_cases 表查询 ✅
   ↓
6. Frontend 展示（完美一致）✅
```

---

## 📊 统一前后对比

### 统一前（有问题）

```
AI Service                    Backend Service
    ↓                              ↓
historical_testcases          test_cases
    ↓                              ↓
  不同的表！                    不同的表！
    ↓                              ↓
  数据不一致                    无法共享
```

**问题**：
- ❌ 表名不同
- ❌ 字段不同
- ❌ 数据孤岛
- ❌ 需要同步机制

### 统一后（完美）

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

**优势**：
- ✅ 共享同一张表
- ✅ 字段完全一致
- ✅ 数据实时同步
- ✅ 无需同步机制

---

## 🗄️ 完整数据库架构

### 共享表（Backend 管理，AI Service 使用）

| 表名 | 用途 | 创建者 | 使用者 |
|------|------|--------|--------|
| `test_cases` | 主测试用例表 | Backend | **Backend + AI** |
| `test_tasks` | 测试任务 | Backend | Backend |
| `test_environments` | 测试环境 | Backend | Backend |
| `quality_reports` | 质量报告 | Backend | Backend |
| `monitoring_data` | 监控数据 | Backend | Backend |

### AI 专用表（AI Service 管理）

| 表名 | 用途 | 使用者 |
|------|------|--------|
| `testcase_generation_history` | AI 生成历史 | AI Service |
| `recommendation_history` | AI 推荐历史 | AI Service |
| `company_standards` | 测试标准 | AI Service |

---

## 🔧 字段映射工具类

### Priority 映射

```python
# AI Service priority format: P0, P1, P2, P3
# Backend priority format: 0-10 integer

PRIORITY_MAPPING = {
    'P0': 10,  # 最高优先级
    'P1': 7,   # 高优先级
    'P2': 5,   # 中优先级
    'P3': 3    # 低优先级
}

# Reverse mapping
PRIORITY_REVERSE = {
    10: 'P0', 9: 'P0',
    8: 'P1', 7: 'P1',
    6: 'P2', 5: 'P2', 4: 'P2',
    3: 'P3', 2: 'P3', 1: 'P3', 0: 'P3'
}
```

### Type 映射

```python
# AI Service type format: Chinese
# Backend type format: English enum

TYPE_MAPPING = {
    '功能测试': 'FUNCTIONAL',
    '性能测试': 'PERFORMANCE',
    '安全测试': 'SECURITY'
}

TYPE_REVERSE = {
    'FUNCTIONAL': '功能测试',
    'PERFORMANCE': '性能测试',
    'SECURITY': '安全测试'
}
```

### Status 映射

```python
# AI Service status format: active, inactive, deleted
# Backend status format: DRAFT, APPROVED, DEPRECATED

STATUS_MAPPING = {
    'active': 'APPROVED',
    'draft': 'DRAFT',
    'deleted': 'DEPRECATED',
    'inactive': 'DEPRECATED'
}

STATUS_REVERSE = {
    'DRAFT': 'draft',
    'APPROVED': 'active',
    'DEPRECATED': 'deleted'
}
```

---

## 🧪 测试验证

### 测试 1：AI Service 保存

```python
from data.rag_manager import rag_manager

# AI Service 格式
testcase = {
    "name": "用户登录-正常流程",
    "priority": "P0",
    "type": "功能测试",
    "description": "验证用户登录",
    "steps": [
        {"step": 1, "action": "输入用户名", "expected": "显示"}
    ],
    "tags": ["登录"]
}

# 保存到 test_cases 表
testcase_id = rag_manager.add_testcase(testcase)
print(f"✅ AI Service 已保存: {testcase_id}")
```

### 测试 2：Backend 查询

```java
// Backend Service 查询
@Autowired
private TestCaseMapper testCaseMapper;

// 直接查询 AI Service 保存的数据
TestCase testCase = testCaseMapper.selectById(testcase_id);
System.out.println("✅ Backend 查询成功: " + testCase.getTitle());
```

### 测试 3：数据一致性验证

```python
from data.mysql_client import mysql_client
import pymysql

# 连接数据库
conn = pymysql.connect(
    host='localhost',
    user='root',
    password='password',
    database='synapsetest'
)

cursor = conn.cursor()

# 查询 AI Service 保存的数据
cursor.execute("SELECT * FROM test_cases WHERE created_by = 'ai-service'")
ai_cases = cursor.fetchall()

print(f"✅ AI Service 保存的测试用例: {len(ai_cases)} 条")

# 查询 Backend 保存的数据
cursor.execute("SELECT * FROM test_cases WHERE created_by != 'ai-service'")
backend_cases = cursor.fetchall()

print(f"✅ Backend 保存的测试用例: {len(backend_cases)} 条")

# 验证表结构
cursor.execute("DESCRIBE test_cases")
columns = cursor.fetchall()

print("\n✅ 表结构验证:")
for col in columns:
    print(f"  {col[0]}: {col[1]}")
```

---

## 🎯 数据统一策略

### 策略 1：字段兼容

```python
# AI Service 读取时自动转换
def normalize_testcase(tc: Dict) -> Dict:
    """Normalize Backend format to AI Service format"""
    return {
        'id': tc.get('id'),
        'name': tc.get('title'),           # title → name
        'title': tc.get('title'),          # 保留原字段
        'description': tc.get('description'),
        'steps': tc.get('steps'),
        'expected_result': tc.get('expected_result'),
        'priority': reverse_map_priority(tc.get('priority')),  # 0-10 → P0-P3
        'type': reverse_map_type(tc.get('type')),              # EN → CN
        'status': reverse_map_status(tc.get('status')),        # EN → CN
        'tags': tc.get('tags'),
        'created_by': tc.get('created_by'),
        'created_at': tc.get('created_at'),
        'updated_at': tc.get('updated_at')
    }
```

### 策略 2：写入时转换

```python
# AI Service 保存时自动转换
def save_testcase(self, testcase_data: Dict) -> str:
    """Convert AI Service format to Backend format"""
    
    # Generate UUID (Backend format)
    testcase_id = str(uuid.uuid4())
    
    # Map fields
    backend_data = {
        'id': testcase_id,
        'title': testcase_data.get('name'),         # name → title
        'priority': map_priority(testcase_data.get('priority')),  # P0-P3 → 0-10
        'type': map_type(testcase_data.get('type')),              # CN → EN
        'status': 'DRAFT',                          # Default status
        'created_by': 'ai-service'                  # Identify source
    }
    
    # Insert to test_cases table
    cursor.execute("INSERT INTO test_cases (...) VALUES (...)")
    return testcase_id
```

---

## 📊 数据兼容性处理

### Module 字段处理

Backend 没有 `module` 字段，使用以下策略：

```python
# 方案 1：使用 tags 存储 module 信息
tags = testcase_data.get('tags', [])
module = testcase_data.get('module')
if module:
    tags.append(f"module:{module}")

# 方案 2：使用 related_requirement 存储
related_requirement = f"MODULE:{testcase_data.get('module')}"

# 方案 3：查询时根据 type 和 tags 推断
# AI Service 检索时可以根据 tags 筛选
```

### Preconditions 字段处理

Backend 没有独立的 `preconditions` 字段：

```python
# 合并到 description 中
description = testcase_data.get('description', '')
preconditions = testcase_data.get('preconditions', [])

if preconditions:
    description = f"前置条件：\n" + "\n".join(preconditions) + f"\n\n{description}"
```

---

## 🎮 使用示例

### AI Service 生成并保存

```python
from services.testcase_service import TestCaseGenerationService

service = TestCaseGenerationService()

# AI 生成
result = service.generate_testcases({
    'requirement_text': '用户登录功能',
    'module': 'login',
    'num_cases': 5
})

# 自动保存到 Backend 的 test_cases 表
print(f"✅ 生成 {result['total_unique']} 个测试用例")
print(f"✅ 已保存到 test_cases 表")
```

### Backend 直接查询

```java
// Backend Service
@Service
public class TestCaseService {
    
    @Autowired
    private TestCaseMapper mapper;
    
    // 查询所有测试用例（包括 AI 生成的）
    public List<TestCase> getAllTestCases() {
        return mapper.selectAll();
    }
    
    // 区分 AI 生成的用例
    public List<TestCase> getAIGeneratedTestCases() {
        return mapper.selectByCreatedBy("ai-service");
    }
    
    // 区分人工创建的用例
    public List<TestCase> getManualTestCases() {
        return mapper.selectNotCreatedBy("ai-service");
    }
}
```

### Frontend 查询（通过 Backend）

```javascript
// Frontend 调用 Backend API
async function getTestCases() {
  // Backend 从 test_cases 表查询
  // 包括 AI 生成的和人工创建的
  const response = await fetch('/api/testcases');
  const testcases = await response.json();
  
  // 可以根据 created_by 字段区分来源
  const aiCases = testcases.filter(tc => tc.created_by === 'ai-service');
  const manualCases = testcases.filter(tc => tc.created_by !== 'ai-service');
  
  console.log(`AI 生成: ${aiCases.length} 条`);
  console.log(`人工创建: ${manualCases.length} 条`);
}
```

---

## ✅ 统一带来的好处

### 1. 数据一致性

```
✅ 单一数据源（test_cases 表）
✅ 实时同步（无需额外机制）
✅ 字段统一（自动映射）
✅ 格式统一（双向转换）
```

### 2. 开发效率

```
✅ Backend 无需修改（使用现有表）
✅ AI Service 自动适配（字段映射）
✅ Frontend 统一查询（通过 Backend）
✅ 减少重复开发
```

### 3. 运维简化

```
✅ 单一表结构
✅ 统一备份策略
✅ 统一监控指标
✅ 降低维护成本
```

---

## 🔍 验证清单

- [x] Backend 使用 `test_cases` 表
- [x] AI Service 使用 `test_cases` 表
- [x] 字段映射逻辑实现
- [x] 优先级转换实现
- [x] 类型转换实现
- [x] 状态转换实现
- [x] JSON 字段处理
- [x] 删除旧的 `historical_testcases` 表定义
- [ ] 端到端测试
- [ ] 数据一致性验证

---

## 📝 SQL 迁移脚本（如果需要）

### 从旧表迁移到新表

```sql
-- 如果之前使用了 historical_testcases，迁移数据
INSERT INTO test_cases (
    id, title, description, steps, expected_result, 
    priority, type, status, tags, created_by
)
SELECT 
    UUID() as id,
    name as title,
    description,
    steps,
    expected_result,
    CASE 
        WHEN priority = 'P0' THEN 10
        WHEN priority = 'P1' THEN 7
        WHEN priority = 'P2' THEN 5
        WHEN priority = 'P3' THEN 3
        ELSE 5
    END as priority,
    CASE 
        WHEN type = '功能测试' THEN 'FUNCTIONAL'
        WHEN type = '性能测试' THEN 'PERFORMANCE'
        WHEN type = '安全测试' THEN 'SECURITY'
        ELSE 'FUNCTIONAL'
    END as type,
    CASE 
        WHEN status = 'active' THEN 'APPROVED'
        WHEN status = 'deleted' THEN 'DEPRECATED'
        ELSE 'DRAFT'
    END as status,
    tags,
    'ai-service' as created_by
FROM historical_testcases
WHERE status != 'deleted';

-- 迁移后删除旧表
DROP TABLE IF EXISTS historical_testcases;
```

---

## 🎯 总结

### 统一方案核心

```
1️⃣ 共享表：test_cases
   - Backend 创建和管理
   - AI Service 读写
   
2️⃣ 字段映射：自动转换
   - AI Service → Backend 格式
   - Backend → AI Service 格式
   
3️⃣ 数据标识：created_by
   - 'ai-service': AI 生成
   - 其他: 人工创建
   
4️⃣ 专用表：AI 历史数据
   - testcase_generation_history
   - recommendation_history
   - company_standards
```

### 架构优势

```
✅ 数据统一：单一数据源
✅ 实时同步：无需同步机制
✅ 完美集成：Backend + AI Service
✅ 字段兼容：自动映射转换
✅ 易于维护：标准化架构
```

---

**🎉 数据库表统一完成！Backend 和 AI Service 现在完美共享 test_cases 表！**

