# 测试任务与测试用例关联功能 - 使用指南

## 🎯 功能概述

现在创建测试任务时，系统会**自动匹配并关联相关的测试用例**，让任务真正具备可执行性！

### ✅ 主要特性

1. **智能匹配** - 根据模块、测试范围、优先级自动匹配用例
2. **预览功能** - 创建前预览将要关联的用例
3. **执行顺序** - 自动设置用例执行顺序
4. **详情展示** - 任务详情页显示所有关联的用例

## 📖 用户操作指南

### 1. 创建测试任务（增强版）

#### 步骤 1: 填写基本信息
```
- 任务名称: 例如"订单模块冒烟测试"
- 测试环境: DEV / STAGING / PROD
- 测试版本: v1.0.0
- 测试模块: 用户认证、订单中心、支付系统等（可多选）
```

#### 步骤 2: 预览匹配的测试用例（新功能！）
1. 填写完模块等必填字段后
2. 点击"预览匹配的测试用例"按钮
3. 系统会显示一个表格，展示所有将要关联的用例：
   - 用例编号（如 TC20231215001）
   - 用例标题
   - 所属模块
   - 优先级（P0-P10，带颜色标签）
   - 类型（FUNCTIONAL/PERFORMANCE/SECURITY）
   - 状态（APPROVED/DRAFT）

#### 步骤 3: 填写代码变更信息
```
- 变更文件数: 10
- 变更行数: 200
- 是否热修复: 否
- 是否核心模块: 是
```

#### 步骤 4: 提交创建
点击"创建任务"按钮，系统会：
1. 创建任务记录
2. 自动匹配相关测试用例
3. 建立任务-用例关联关系
4. 设置执行顺序
5. 返回成功消息，显示关联的用例数量

### 2. 查看任务详情（新页面！）

访问任务详情页面可以看到：

#### 任务基本信息
- 任务ID、名称、状态、优先级
- 测试环境、版本、范围
- **用例总数**（新增）
- 创建人、创建时间、更新时间

#### AI 推荐信息
- 推荐的环境、版本、测试范围
- 置信度评分
- 推理说明

#### 关联的测试用例列表（新增！）
完整的表格展示，包括：
- **执行顺序**（支持排序）
- 用例编号、标题、模块
- 优先级、类型、状态
- 支持分页浏览

### 3. 任务列表增强

现在任务列表中的每个任务都会显示**关联的用例数量**，让您一目了然。

## 🔧 开发者指南

### 数据库准备

如果您的数据库中还没有 `task_test_cases` 表，请执行迁移脚本：

```bash
# 方式1: 使用迁移脚本
mysql -u root -p synapsetest < database/migrations/add_task_test_cases_table.sql

# 方式2: 重新初始化完整schema
mysql -u root -p < database/unified_schema.sql
```

### API 使用示例

#### 1. 预览匹配的测试用例（不创建任务）

```bash
curl -X POST http://localhost:8080/api/v1/test-tasks/preview-test-cases \
  -H "Content-Type: application/json" \
  -d '{
    "taskName": "订单模块测试",
    "environment": "DEV",
    "version": "v1.0.0",
    "modules": ["订单中心", "支付系统"],
    "codeChangeInfo": {
      "changed_files_count": 10,
      "changed_lines_count": 200,
      "is_hotfix": false,
      "is_critical_module": true
    }
  }'
```

**响应示例**:
```json
{
  "success": true,
  "message": "Matched 8 test cases",
  "data": [
    {
      "id": "uuid-001",
      "caseNumber": "TC20231215004",
      "title": "订单创建成功",
      "module": "订单中心",
      "priority": 7,
      "type": "FUNCTIONAL",
      "status": "APPROVED"
    },
    // ... 更多用例
  ]
}
```

#### 2. 创建任务（自动关联用例）

```bash
curl -X POST http://localhost:8080/api/v1/test-tasks \
  -H "Content-Type: application/json" \
  -H "X-User-Id: zhangsan" \
  -d '{
    "taskName": "订单模块测试",
    "environment": "DEV",
    "version": "v1.0.0",
    "modules": ["订单中心", "支付系统"],
    "codeChangeInfo": {
      "changed_files_count": 10,
      "changed_lines_count": 200,
      "is_hotfix": false,
      "is_critical_module": true
    }
  }'
```

**响应示例**:
```json
{
  "success": true,
  "message": "Test task created successfully",
  "data": {
    "id": "task-uuid-xxx",
    "name": "订单模块测试",
    "status": "PENDING",
    "environment": "DEV",
    "version": "v1.0.0",
    "testScope": "CORE",
    "totalTestCases": 8,
    "testCases": [
      {
        "id": "uuid-001",
        "caseNumber": "TC20231215004",
        "title": "订单创建成功",
        "module": "订单中心",
        "priority": 7,
        "type": "FUNCTIONAL",
        "status": "APPROVED",
        "executionOrder": 1
      },
      // ... 更多用例
    ],
    "recommendation": {
      "recommendedEnvironment": "DEV",
      "recommendedVersion": "v1.0.0",
      "recommendedScope": "CORE",
      "confidenceScore": 0.85,
      "reasoning": "基于代码变更量和核心模块标记，推荐执行核心回归测试"
    }
  }
}
```

#### 3. 获取任务详情（包含用例列表）

```bash
curl http://localhost:8080/api/v1/test-tasks/task-uuid-xxx
```

#### 4. 单独获取任务关联的用例

```bash
curl http://localhost:8080/api/v1/test-tasks/task-uuid-xxx/test-cases
```

**响应示例**:
```json
{
  "success": true,
  "message": "Success",
  "data": [
    {
      "id": "uuid-001",
      "caseNumber": "TC20231215004",
      "title": "订单创建成功",
      "description": "测试用户从购物车创建订单的完整流程",
      "steps": [...],
      "expectedResult": "订单创建成功,显示订单详情页",
      "module": "订单中心",
      "priority": 7,
      "type": "FUNCTIONAL",
      "status": "APPROVED"
    },
    // ... 更多用例
  ]
}
```

## 🎨 智能匹配策略

### 匹配规则

系统会根据以下规则智能匹配测试用例：

#### 1. 按模块匹配
从用户选择的模块中查找所有**已批准**（APPROVED）的测试用例

#### 2. 按测试范围过滤

根据 AI 推荐的测试范围进行过滤：

| 测试范围 | 优先级过滤 | 数量限制 | 适用场景 |
|---------|-----------|---------|---------|
| **SMOKE**（冒烟测试） | P0 (≥8) | 最多20个 | 快速验证核心功能 |
| **CORE**（核心回归） | P0-P1 (≥5) | 最多50个 | 核心功能全面测试 |
| **FULL**（全量回归） | 所有优先级 | 无限制 | 发版前完整测试 |

#### 3. 去重处理
如果同一用例在多个模块中出现，保留优先级最高的那个

#### 4. 按优先级排序
最终结果按优先级从高到低排序（P0 → P1 → P2...）

#### 5. 设置执行顺序
按排序结果设置 `executionOrder`（1, 2, 3...）

### 示例场景

#### 场景 1: 冒烟测试
```
输入:
- 模块: ["用户认证", "订单中心"]
- 代码变更: 小规模变更
- AI推荐: SMOKE

输出:
- 匹配策略: 只选择 P0 高优先级用例
- 预期结果: 5-10个核心用例
```

#### 场景 2: 核心回归
```
输入:
- 模块: ["用户认证", "订单中心", "支付系统"]
- 代码变更: 中等规模变更，核心模块
- AI推荐: CORE

输出:
- 匹配策略: 选择 P0-P1 用例
- 预期结果: 20-40个用例
```

#### 场景 3: 全量回归
```
输入:
- 模块: ["所有模块"]
- 代码变更: 大规模变更或发版
- AI推荐: FULL

输出:
- 匹配策略: 所有已批准的用例
- 预期结果: 所有用例（可能上百个）
```

## 📊 数据结构

### task_test_cases 表

| 字段 | 类型 | 说明 |
|-----|------|------|
| task_id | CHAR(36) | 任务ID（主键1） |
| test_case_id | CHAR(36) | 用例ID（主键2） |
| execution_order | INTEGER | 执行顺序 |
| created_at | TIMESTAMP | 创建时间 |

**外键约束**:
- `task_id` → `test_tasks(id)` ON DELETE CASCADE
- `test_case_id` → `test_cases(id)` ON DELETE CASCADE

**索引**:
- PRIMARY KEY (task_id, test_case_id)
- INDEX idx_task_id
- INDEX idx_test_case_id
- INDEX idx_execution_order

## 🚀 前端集成

### React 组件使用

#### 1. CreateTestTask 组件

```jsx
import CreateTestTask from './components/test-task/CreateTestTask'

// 在路由中使用
<Route path="/test-tasks/create" element={<CreateTestTask />} />
```

**新增功能**:
- ✅ 预览测试用例按钮
- ✅ 匹配用例表格显示
- ✅ 创建成功显示用例数量

#### 2. TestTaskDetail 组件（新增）

```jsx
import TestTaskDetail from './components/test-task/TestTaskDetail'

// 在路由中使用
<Route path="/test-tasks/:id" element={<TestTaskDetail />} />
```

**显示内容**:
- ✅ 任务基本信息
- ✅ AI 推荐信息
- ✅ 关联的测试用例列表
- ✅ 返回按钮

### Service API 调用

```javascript
import testTaskService from './services/testTaskService'

// 预览测试用例
const testCases = await testTaskService.previewTestCases({
  taskName: "测试任务",
  environment: "DEV",
  version: "v1.0.0",
  modules: ["用户认证"],
  codeChangeInfo: {...}
})

// 创建任务（自动关联用例）
const result = await testTaskService.createTestTask({...})
console.log(`Created task with ${result.data.totalTestCases} test cases`)

// 获取任务详情（包含用例列表）
const taskDetail = await testTaskService.getTestTaskById(taskId)

// 获取任务关联的用例
const testCases = await testTaskService.getTestCasesByTaskId(taskId)
```

## ⚡ 性能优化

### 数量限制

为避免性能问题，系统对匹配的用例数量进行了限制：

- **SMOKE**: 最多 20 个用例
- **CORE**: 最多 50 个用例
- **FULL**: 无限制（但建议不超过 200 个）

### 批量操作

- 使用 MyBatis 批量插入提高性能
- 一次性插入所有关联记录
- 事务保证原子性

### 缓存策略

建议在以下场景使用缓存：
- 模块列表（较少变化）
- 已批准的用例列表（按模块缓存）
- AI 推荐结果（相同输入可缓存）

## 🐛 常见问题

### Q1: 为什么预览的用例数量很少？

**A**: 这是正常的！系统会根据测试范围智能过滤：
- SMOKE 测试只选 P0 高优先级用例
- 确保每个模块至少有几个核心用例
- 如果需要更多用例，可以选择 CORE 或 FULL 范围

### Q2: 为什么有些模块没有匹配到用例？

**A**: 可能的原因：
1. 该模块确实没有已批准的测试用例
2. 用例的 `module` 字段为空或不匹配
3. 所有用例都是 DRAFT 状态（未批准）

**解决方案**:
- 检查测试用例表，确认模块名称
- 确保用例状态为 APPROVED
- 先创建该模块的测试用例

### Q3: 如何手动调整关联的用例？

**A**: 当前版本（Phase 1）暂不支持手动选择，系统完全自动匹配。Phase 2 将支持：
- 手动勾选/取消用例
- 拖拽调整执行顺序
- 批量添加/移除用例

### Q4: 任务创建后可以修改关联的用例吗？

**A**: 当前版本创建后不支持修改。建议：
- 创建前使用预览功能确认
- 如果需要调整，取消当前任务，重新创建
- Phase 2 将支持关联用例的增删改

### Q5: 删除任务会删除关联的用例吗？

**A**: 不会！
- 删除任务是**软删除**（标记为 CANCELLED）
- 用例关联记录会保留（审计需要）
- 测试用例本身不会被删除

## 📈 数据统计

使用新功能后，您可以：

```sql
-- 查看任务关联的用例数量分布
SELECT 
    t.name,
    t.test_scope,
    COUNT(ttc.test_case_id) as case_count
FROM test_tasks t
LEFT JOIN task_test_cases ttc ON t.id = ttc.task_id
WHERE t.status != 'CANCELLED'
GROUP BY t.id, t.name, t.test_scope
ORDER BY case_count DESC;

-- 查看最常被关联的测试用例
SELECT 
    tc.case_number,
    tc.title,
    tc.module,
    COUNT(ttc.task_id) as task_count
FROM test_cases tc
LEFT JOIN task_test_cases ttc ON tc.id = ttc.test_case_id
GROUP BY tc.id, tc.case_number, tc.title, tc.module
ORDER BY task_count DESC
LIMIT 10;

-- 查看各测试范围的平均用例数
SELECT 
    t.test_scope,
    AVG(case_count) as avg_cases,
    MIN(case_count) as min_cases,
    MAX(case_count) as max_cases
FROM (
    SELECT 
        t.id,
        t.test_scope,
        COUNT(ttc.test_case_id) as case_count
    FROM test_tasks t
    LEFT JOIN task_test_cases ttc ON t.id = ttc.task_id
    WHERE t.status != 'CANCELLED'
    GROUP BY t.id, t.test_scope
) AS stats
GROUP BY test_scope;
```

## 🎯 最佳实践

### 1. 模块管理
- 保持模块命名一致性（用例和任务使用相同名称）
- 建议使用中文模块名（如"用户认证"而非"auth"）
- 定期整理和合并相似模块

### 2. 用例优先级
- P0 (8-10): 核心功能，必须每次都测
- P1 (5-7): 重要功能，核心回归必测
- P2 (0-4): 一般功能，全量回归时测

### 3. 测试范围选择
- **SMOKE**: 发版后快速验证，15-30分钟
- **CORE**: 日常回归测试，1-2小时
- **FULL**: 发版前完整测试，4-8小时

### 4. 代码变更信息
准确填写代码变更信息可以让 AI 推荐更准确：
- 变更文件数: 影响测试范围
- 变更行数: 影响测试深度
- 是否热修复: 影响测试紧急度
- 是否核心模块: 影响测试广度

## 🔮 未来规划

### Phase 2: 用户体验优化（规划中）
- [ ] 手动选择/取消测试用例
- [ ] 拖拽调整执行顺序
- [ ] 批量操作用例
- [ ] 自定义过滤条件

### Phase 3: AI 智能推荐（规划中）
- [ ] 基于代码变更分析推荐相关用例
- [ ] 基于历史执行结果推荐（失败率高的优先）
- [ ] 学习用户选择偏好
- [ ] 推荐理由和置信度展示

## 📞 技术支持

如有问题或建议，请查看：
- 设计文档: `docs/claudegc/TASK_TESTCASE_ASSOCIATION_TODO.md`
- 实施文档: `docs/claudegc/TASK_TESTCASE_IMPLEMENTATION_COMPLETE.md`
- 数据库迁移: `database/migrations/add_task_test_cases_table.sql`

---

**文档版本**: v1.0  
**创建日期**: 2024-12-16  
**作者**: Claude AI Assistant  
**状态**: ✅ 功能已上线，可正常使用
