# 智能生成测试用例保存功能修复总结

> **重要更新**: 本项目已实现双ID设计，详见 [双ID设计文档](docs/DUAL_ID_DESIGN.md)

## 问题描述

在前端"智能生成"功能中，生成测试用例后点击"保存到库"时，保存到MySQL数据库`test_cases`表的数据存在以下问题：

1. **模块字段错误**：用例所属模块应该使用`module`字段，实际使用了`related_requirement`字段
2. **测试步骤格式错误**：应该为JSON对象数组格式，实际为字符串数组格式
3. **优先级数据错误**：前端显示P0，但数据库存储为10
4. **缺少前置条件**：未正确存储到`preconditions`字段
5. **缺少优先级分数**：未存储到`quality_score`字段
6. **缺少AI标记字段**：`ai_generated`和`ai_confidence`字段未设置

## 修复内容

### 1. 前端修复 (SmartGenerate.tsx)

#### 1.1 移除错误的优先级转换
- **删除**：`convertPriority`函数（原来将P0转换为10，P1转换为8等）
- **原因**：后端现在支持直接处理P0/P1/P2/P3字符串格式

#### 1.2 修复测试步骤格式
```typescript
// 修改前：返回字符串数组 ["1. action | 预期: expected", ...]
// 修改后：返回JSON对象字符串数组 ["{\"step\":1,...}", "{\"step\":2,...}", ...]
const convertSteps = (steps: TestStep[]): string[] => {
  return steps.map(step => JSON.stringify(step))
}
```

#### 1.3 完善保存数据映射
```typescript
const selectedTestCases = generatedData.testcases
  .filter((_, idx) => selectedCases.has(idx))
  .map((testcase) => ({
    title: testcase.name,
    description: `${testcase.type} - ${testcase.tags.join(', ')}`,
    steps: convertSteps(testcase.steps),  // JSON对象字符串数组
    expectedResult: testcase.steps.length > 0
      ? testcase.steps[testcase.steps.length - 1].expected
      : '测试通过',
    priority: testcase.priority,  // 保持P0/P1/P2/P3格式
    type: convertType(testcase.type),
    tags: testcase.tags,
    module: generatedData.metadata.module,  // 正确的模块字段
    preconditions: testcase.preconditions,  // 前置条件
    quality_score: testcase.priority_score || 0.0,  // 质量评分
    ai_generated: true,  // AI生成标记
    ai_confidence: testcase.priority_score || 0.85,  // AI置信度
  }))
```

### 2. 后端修复 (TestCaseService.java)

#### 2.1 修复模块字段Bug
```java
// 修改前：
testCase.setRelatedRequirement((String) tcMap.getOrDefault("module", ""));

// 修改后：
testCase.setRelatedRequirement((String) tcMap.get("related_requirement"));
```

#### 2.2 支持P0/P1/P2/P3优先级格式
```java
// 新增优先级映射
if ("P0".equalsIgnoreCase(priorityStr)) {
    priority = 10;  // P0 = 最高优先级
} else if ("P1".equalsIgnoreCase(priorityStr)) {
    priority = 8;
} else if ("P2".equalsIgnoreCase(priorityStr)) {
    priority = 5;
} else if ("P3".equalsIgnoreCase(priorityStr)) {
    priority = 3;
}
```

#### 2.3 添加新字段处理
```java
// 质量评分
Object qualityScoreObj = tcMap.get("quality_score");
if (qualityScoreObj != null) {
    testCase.setQualityScore(((Number) qualityScoreObj).floatValue());
}

// AI生成标记
Object aiGeneratedObj = tcMap.get("ai_generated");
if (aiGeneratedObj instanceof Boolean) {
    testCase.setAiGenerated((Boolean) aiGeneratedObj);
}

// AI置信度
Object aiConfidenceObj = tcMap.get("ai_confidence");
if (aiConfidenceObj != null) {
    testCase.setAiConfidence(((Number) aiConfidenceObj).floatValue());
}
```

### 3. 新增StepsTypeHandler

创建了专门的类型处理器`StepsTypeHandler.java`来正确处理测试步骤的序列化：

```java
/**
 * 将List<String>转换为JSON数组时：
 * - 检查每个元素是否为JSON对象字符串（以"{"开头）
 * - 如果是，直接添加到数组中（不转义）
 * - 如果不是，正常转义后添加
 * 
 * 结果：[{"step":1,"action":"...","expected":"..."},...]
 * 而不是：["{\"step\":1,...}","{\"step\":2,...}"]
 */
```

### 4. 更新MyBatis Mapper (TestCaseMapper.xml)

#### 4.1 更新ResultMap
添加了新字段的映射：
- `module`
- `preconditions` (使用JsonTypeHandler)
- `quality_score`
- `ai_generated`
- `ai_confidence`

#### 4.2 更新SQL语句
- INSERT语句：添加了所有新字段
- UPDATE语句：添加了所有新字段
- steps字段使用`StepsTypeHandler`代替`JsonTypeHandler`

## 数据格式对比

### 测试步骤格式

#### 修改前（错误）：
```json
["1. 在登录页面点击'微信登录'按钮 | 预期: 调起微信授权界面", 
 "2. 确认授权登录 | 预期: 授权成功，系统创建/绑定账户并跳转至首页"]
```

#### 修改后（正确）：
```json
[
  {"step": 1, "action": "在登录页面点击'微信登录'按钮", "expected": "调起微信授权界面"},
  {"step": 2, "action": "确认授权登录", "expected": "授权成功，系统创建/绑定账户并跳转至首页"}
]
```

### 优先级处理

#### 修改前：
- 前端：P0 → 转换为数字10
- 后端：接收数字10，存储10
- 问题：P0不应该直接映射为10

#### 修改后：
- 前端：P0 → 发送字符串"P0"
- 后端：接收"P0"，根据映射规则转换为10
- 后端映射：P0→10, P1→8, P2→5, P3→3

### 完整字段映射

| 前端字段 | 后端字段 | 数据库字段 | 数据类型 | 说明 |
|---------|---------|-----------|---------|------|
| testcase.name | title | title | VARCHAR | 用例名称 |
| - | description | description | TEXT | 用例描述 |
| convertSteps(testcase.steps) | steps | steps | JSON | 测试步骤（JSON数组） |
| last step.expected | expectedResult | expected_result | TEXT | 预期结果 |
| testcase.priority | priority | priority | INTEGER | 优先级（0-10） |
| convertType(testcase.type) | type | type | VARCHAR | 测试类型 |
| testcase.tags | tags | tags | JSON | 标签 |
| metadata.module | module | module | VARCHAR | 所属模块 |
| - | relatedRequirement | related_requirement | VARCHAR | 关联需求 |
| testcase.preconditions | preconditions | preconditions | JSON | 前置条件 |
| testcase.priority_score | qualityScore | quality_score | FLOAT | 质量评分 |
| true | aiGenerated | ai_generated | BOOLEAN | AI生成标记 |
| testcase.priority_score | aiConfidence | ai_confidence | FLOAT | AI置信度 |

## AI字段说明

### ai_generated
- **含义**：标识该测试用例是否由AI生成
- **类型**：BOOLEAN
- **值**：对于智能生成的用例，设置为`true`
- **用途**：用于筛选和统计AI生成的用例，进行质量分析

### ai_confidence
- **含义**：AI生成该用例的置信度
- **类型**：FLOAT (0.0-1.0)
- **值**：使用测试用例的`priority_score`作为置信度
- **用途**：
  - 评估AI生成用例的质量
  - 可用于过滤低质量用例（如筛选confidence > 0.8的用例）
  - 用于AI模型的反馈和改进

## 测试验证

修复后需要验证以下内容：

1. ✅ `module`字段正确存储模块名称（如"用户认证模块"）
2. ✅ `related_requirement`字段为空或存储正确的关联需求
3. ✅ `steps`字段存储为JSON数组对象格式
4. ✅ `priority`字段正确映射（P0→10, P1→8, P2→5, P3→3）
5. ✅ `preconditions`字段存储前置条件数组
6. ✅ `quality_score`字段存储优先级分数
7. ✅ `ai_generated`字段为`true`
8. ✅ `ai_confidence`字段存储置信度值

## 文件修改清单

1. **前端**：
   - `/frontend/src/components/test-case/SmartGenerate.tsx`

2. **后端**：
   - `/backend/src/main/java/com/synapsetest/testmanagement/service/TestCaseService.java`
   - `/backend/src/main/java/com/synapsetest/testmanagement/service/CaseNumberGenerator.java` (新增)
   - `/backend/src/main/java/com/synapsetest/testmanagement/model/TestCase.java`
   - `/backend/src/main/java/com/synapsetest/testmanagement/controller/TestCaseController.java`
   - `/backend/src/main/java/com/synapsetest/testmanagement/config/StepsTypeHandler.java` (新增)
   - `/backend/src/main/resources/mapper/TestCaseMapper.xml`

3. **数据库**：
   - `/database/unified_schema.sql`
   - `/database/migrations/add_case_number_field.sql` (新增)

4. **文档**：
   - `/docs/DUAL_ID_DESIGN.md` (新增)

## 注意事项

1. **向后兼容性**：StepsTypeHandler既支持新格式（JSON对象数组），也支持旧格式（字符串数组）
2. **优先级映射**：P0为最高优先级（10），P3为最低（3）
3. **AI置信度**：目前使用priority_score作为confidence的值，后续可根据需要调整
4. **数据迁移**：如果数据库中已有旧格式的步骤数据，可能需要数据迁移脚本

---

## 双ID设计说明（重要更新）

### 问题与解决方案

**问题**：前端显示的`TC001`与数据库中的UUID不一致

**原因**：
- `TC001`只是前端临时显示编号（基于数组索引）
- 数据库使用UUID作为主键（如`f1a8593c-7b39-423c-8e0d-d37f3ac55059`）

**解决方案**：实现双ID设计
- **技术主键(id)**: UUID格式，保证全局唯一性和分布式友好
- **业务编号(case_number)**: 人类友好格式，便于记忆和交流

### 新增功能

#### 1. 数据库字段

```sql
CREATE TABLE test_cases (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),           -- 技术主键
    case_number VARCHAR(20) UNIQUE NOT NULL,            -- 业务编号
    ...
);
```

#### 2. 业务编号格式

```
TC{yyyyMMdd}{seq}
```

示例：
- `TC20231215001` - 2023年12月15日第1个用例
- `TC20231215002` - 2023年12月15日第2个用例
- `TC20231216001` - 2023年12月16日第1个用例（新一天，序号重置）

#### 3. 自动生成器

后端服务`CaseNumberGenerator`自动生成业务编号：
- 线程安全设计
- 日期变化时自动重置序号
- 支持批量生成

#### 4. API响应变更

**修改前**：
```json
{
  "saved_count": 3,
  "saved_ids": ["uuid1", "uuid2", "uuid3"]
}
```

**修改后**：
```json
{
  "saved_count": 3,
  "saved_cases": [
    {"id": "uuid1", "case_number": "TC20231215001"},
    {"id": "uuid2", "case_number": "TC20231215002"},
    {"id": "uuid3", "case_number": "TC20231215003"}
  ]
}
```

#### 5. 前端显示优化

保存成功后显示真实的业务编号：
```
成功保存 3 条测试用例到数据库！
用例编号: TC20231215001, TC20231215002, TC20231215003
```

### 使用场景

1. **日常沟通**
   - ✅ "TC20231215001这个用例执行失败了"（简短易记）
   - ❌ "f1a8593c-7b39-423c-8e0d-d37f3ac55059这个用例..."（不便交流）

2. **API调用**
   ```bash
   # 使用UUID（主键）
   GET /api/v1/test-cases/f1a8593c-7b39-423c-8e0d-d37f3ac55059
   
   # 使用业务编号（可选）
   GET /api/v1/test-cases/by-number/TC20231215001
   ```

3. **数据库查询**
   ```sql
   -- 用户筛选：使用业务编号
   SELECT * FROM test_cases WHERE case_number = 'TC20231215001';
   
   -- 表关联：使用UUID
   SELECT tc.*, te.* FROM test_cases tc
   JOIN test_executions te ON tc.id = te.test_case_id;
   ```

### 数据迁移

对于现有数据库，使用迁移脚本：
```bash
mysql -u username -p database_name < database/migrations/add_case_number_field.sql
```

### 详细文档

完整的设计说明、最佳实践和常见问题，请参考：
📖 [双ID设计文档](docs/DUAL_ID_DESIGN.md)
