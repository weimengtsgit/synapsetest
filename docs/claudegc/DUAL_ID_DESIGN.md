# 测试用例双ID设计方案

## 概述

为了兼顾技术实现的灵活性和业务使用的友好性，测试用例系统采用了**双ID设计**：
- **技术主键 (id)**: UUID格式，保证全局唯一性
- **业务编号 (case_number)**: 人类友好的格式，便于记忆和交流

## 设计动机

### 为什么需要两个ID？

1. **UUID的优势**
   - 全局唯一，无冲突
   - 分布式环境友好
   - 无需中央序列管理
   - 行业标准做法

2. **UUID的劣势**
   - 不便于人类记忆和交流（如`f1a8593c-7b39-423c-8e0d-d37f3ac55059`）
   - 在日常沟通中难以引用

3. **业务编号的价值**
   - 简短易记（如`TC20231215001`）
   - 包含时间信息，便于追溯
   - 符合测试团队的习惯

## 数据库设计

### 表结构

```sql
CREATE TABLE test_cases (
    -- 技术主键
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '用例ID(技术主键)',
    
    -- 业务编号
    case_number VARCHAR(20) UNIQUE NOT NULL COMMENT '业务编号(如TC20231215001)',
    
    -- 其他字段...
    title VARCHAR(200) NOT NULL COMMENT '用例标题',
    ...
    
    -- 索引
    INDEX idx_case_number (case_number)
);
```

### 字段说明

| 字段 | 类型 | 说明 | 示例 |
|-----|------|------|------|
| id | CHAR(36) | 技术主键，UUID格式，数据库自动生成 | `f1a8593c-7b39-423c-8e0d-d37f3ac55059` |
| case_number | VARCHAR(20) | 业务编号，后端服务生成，包含日期和序号 | `TC20231215001` |

## 业务编号生成规则

### 格式定义

```
TC{yyyyMMdd}{seq}
```

- **TC**: 固定前缀，表示Test Case
- **yyyyMMdd**: 8位日期（年月日）
- **seq**: 3位序列号（001-999），每天重置

### 生成示例

```
TC20231215001  # 2023年12月15日的第1个用例
TC20231215002  # 2023年12月15日的第2个用例
...
TC20231215999  # 2023年12月15日的第999个用例
TC20231216001  # 2023年12月16日的第1个用例（序号重置）
```

### 生成器实现

位置：`backend/src/main/java/com/synapsetest/testmanagement/service/CaseNumberGenerator.java`

核心特性：
- **线程安全**: 使用`synchronized`和`AtomicInteger`
- **自动重置**: 日期变化时序列号自动重置为1
- **批量生成**: 支持一次生成多个编号

```java
@Service
public class CaseNumberGenerator {
    public synchronized String generateNextCaseNumber() {
        String today = LocalDate.now().format(DATE_FORMATTER);
        
        // 日期变化时重置计数器
        if (!today.equals(currentDate)) {
            currentDate = today;
            sequenceCounter.set(0);
        }
        
        int sequence = sequenceCounter.incrementAndGet();
        return String.format("%s%s%03d", PREFIX, today, sequence);
    }
}
```

## API接口变更

### 批量保存接口

**请求**: `POST /api/v1/test-cases/batch`

```json
{
  "test_cases": [
    {
      "title": "用户登录测试",
      "description": "...",
      "steps": [...],
      ...
    }
  ]
}
```

**响应**: 

```json
{
  "saved_count": 3,
  "saved_cases": [
    {
      "id": "f1a8593c-7b39-423c-8e0d-d37f3ac55059",
      "case_number": "TC20231215001"
    },
    {
      "id": "a2b9483d-6c28-412d-9f1e-e48g4bd66160",
      "case_number": "TC20231215002"
    },
    {
      "id": "b3c0594e-7d39-523e-0g2f-f59h5ce77271",
      "case_number": "TC20231215003"
    }
  ]
}
```

**变更说明**：
- 返回值从`saved_ids`改为`saved_cases`
- 每个保存的用例返回`id`和`case_number`

## 前端实现

### 智能生成组件

位置：`frontend/src/components/test-case/SmartGenerate.tsx`

#### 显示逻辑

1. **生成阶段**: 显示临时编号
   ```typescript
   // 临时编号，基于数组索引
   TC{String(index + 1).padStart(3, '0')}  // TC001, TC002, ...
   ```

2. **保存成功**: 显示真实编号
   ```typescript
   const caseNumbers = savedCases.map(c => c.case_number).join(', ')
   message.success(`成功保存！用例编号: ${caseNumbers}`)
   // 显示: "成功保存！用例编号: TC20231215001, TC20231215002, TC20231215003"
   ```

### 用例列表组件

在用例管理、执行等页面，应优先显示`case_number`而非UUID。

```typescript
// 推荐做法
<div>用例编号: {testCase.case_number}</div>
<div>内部ID: {testCase.id.substring(0, 8)}...</div>  // 可选显示
```

## 使用场景

### 场景1：日常沟通

**团队成员A**: "TC20231215001这个用例执行失败了"  
**团队成员B**: "我看看，是登录用例吧？"

✅ 业务编号简短易记，便于口头和文字交流

### 场景2：API调用

```bash
# 使用技术ID查询（推荐）
GET /api/v1/test-cases/f1a8593c-7b39-423c-8e0d-d37f3ac55059

# 使用业务编号查询（可选实现）
GET /api/v1/test-cases/by-number/TC20231215001
```

### 场景3：数据分析

```sql
-- 按业务编号排序和筛选
SELECT case_number, title, status 
FROM test_cases 
WHERE case_number LIKE 'TC20231215%'
ORDER BY case_number;

-- 技术ID用于关联查询
SELECT tc.*, te.* 
FROM test_cases tc
JOIN test_executions te ON tc.id = te.test_case_id;
```

## 数据迁移

### 新系统

使用`database/unified_schema.sql`直接创建表结构。

### 现有系统

使用迁移脚本`database/migrations/add_case_number_field.sql`：

```bash
mysql -u username -p database_name < database/migrations/add_case_number_field.sql
```

迁移步骤：
1. 添加`case_number`字段
2. 为现有数据生成编号
3. 添加唯一约束和索引
4. 验证迁移结果

## 最佳实践

### 1. API设计

- ✅ **推荐**: 使用UUID作为RESTful API的资源标识符
  ```
  GET /api/v1/test-cases/{uuid}
  ```
- ✅ **可选**: 提供基于业务编号的查询端点
  ```
  GET /api/v1/test-cases/by-number/{case_number}
  ```

### 2. 前端显示

- ✅ **推荐**: 优先显示`case_number`给用户
- ✅ **推荐**: 在详情页或调试模式下显示UUID
- ❌ **避免**: 在用户界面直接显示完整UUID

### 3. 数据库操作

- ✅ **推荐**: 表关联使用UUID（`id`字段）
- ✅ **推荐**: 用户筛选和排序使用`case_number`
- ✅ **推荐**: 创建`case_number`的索引以提高查询性能

### 4. 日志和监控

```java
// ✅ 推荐：同时记录两个ID
log.info("Executing test case: {} ({})", caseNumber, id);

// ❌ 避免：只记录UUID
log.info("Executing test case: {}", id);
```

## 性能考虑

### 索引策略

```sql
-- 主键索引（自动）
PRIMARY KEY (id)

-- 业务编号唯一索引
UNIQUE INDEX idx_case_number (case_number)

-- 其他常用查询索引
INDEX idx_module (module)
INDEX idx_created_at (created_at)
```

### 查询优化

```sql
-- ✅ 高效：使用主键
SELECT * FROM test_cases WHERE id = 'uuid-value';

-- ✅ 高效：使用唯一索引
SELECT * FROM test_cases WHERE case_number = 'TC20231215001';

-- ⚠️ 注意：模糊查询可能影响性能
SELECT * FROM test_cases WHERE case_number LIKE 'TC2023%';
```

## 扩展性

### 自定义编号格式

如需更改编号格式，修改`CaseNumberGenerator`即可：

```java
// 当前格式: TC20231215001
// 可选格式示例:
// - TC-2023-1215-001  (带分隔符)
// - CASE20231215001   (不同前缀)
// - TC2312150001      (年份简写)
// - TC001             (不含日期，全局递增)
```

### 多项目支持

如需支持多项目，可在编号中添加项目标识：

```java
// 格式: {ProjectCode}{yyyyMMdd}{seq}
// 示例: WEB20231215001, APP20231215001
public String generateNextCaseNumber(String projectCode) {
    // ...
}
```

## 常见问题

### Q1: 为什么不直接使用TC001、TC002这样的简单编号？

**A**: 简单递增编号在分布式环境下难以保证唯一性，且无法传达时间信息。当前格式包含日期，便于追溯和管理。

### Q2: 如果一天生成超过999个用例怎么办？

**A**: 可以修改`CaseNumberGenerator`增加序列号位数（如4位、5位）。或者调整格式为全局递增（不按天重置）。

### Q3: case_number能否手动指定？

**A**: 当前设计为自动生成。如需手动指定，可在`batchSaveTestCases`方法中添加逻辑：
```java
String caseNumber = (String) tcMap.get("case_number");
if (caseNumber == null || caseNumber.isEmpty()) {
    caseNumber = caseNumberGenerator.generateNextCaseNumber();
}
```

### Q4: 旧数据的case_number如何生成？

**A**: 使用迁移脚本自动生成。如需保留特定格式，可手动更新：
```sql
UPDATE test_cases 
SET case_number = 'TC-LEGACY-001' 
WHERE id = 'old-uuid';
```

## 总结

双ID设计兼顾了技术实现和业务需求：
- **UUID** 提供稳定、可靠的技术基础
- **case_number** 提供友好、直观的业务标识

这种设计模式在现代系统中广泛应用（如订单号、物流单号等），是一个经过验证的最佳实践。

## 相关文件

- 数据库Schema: `database/unified_schema.sql`
- 迁移脚本: `database/migrations/add_case_number_field.sql`
- 生成器服务: `backend/src/main/java/.../CaseNumberGenerator.java`
- TestCase服务: `backend/src/main/java/.../TestCaseService.java`
- MyBatis配置: `backend/src/main/resources/mapper/TestCaseMapper.xml`
- 前端组件: `frontend/src/components/test-case/SmartGenerate.tsx`
- 修复总结: `FIXES_SUMMARY.md`
