# 测试用例优先级映射规范

## 概述

本文档定义了测试用例优先级在前后端之间的映射关系，确保数据一致性和显示统一性。

## 优先级映射表

| 显示标签 | 数据库数值 | 描述 | 颜色标记 |
|---------|-----------|------|---------|
| P0 | 9-10 | 最高优先级（Critical） | 红色 (red) |
| P1 | 7-8 | 高优先级（High） | 橙色 (orange) |
| P2 | 4-6 | 中优先级（Medium） | 蓝色 (blue) |
| P3 | 0-3 | 低优先级（Low） | 灰色 (default) |

## 后端转换逻辑

### 数据库 Schema
```sql
priority INTEGER DEFAULT 5 CHECK (priority >= 0 AND priority <= 10)
```

### 字符串到数字的转换
后端 `TestCaseService.java` 的 `batchSaveTestCases` 方法中实现：

```java
if ("P0".equalsIgnoreCase(priorityStr)) {
    priority = 10;  // 最高优先级
} else if ("P1".equalsIgnoreCase(priorityStr)) {
    priority = 8;
} else if ("P2".equalsIgnoreCase(priorityStr)) {
    priority = 5;
} else if ("P3".equalsIgnoreCase(priorityStr)) {
    priority = 3;
}
```

## 前端处理逻辑

### 工具函数位置
`frontend/src/utils/priorityUtils.ts`

### 核心函数

#### 1. convertPriorityToLabel(priority: number): string
将数据库中的数字优先级转换为显示标签

**示例：**
```typescript
convertPriorityToLabel(10) // 返回 "P0"
convertPriorityToLabel(8)  // 返回 "P1"
convertPriorityToLabel(5)  // 返回 "P2"
convertPriorityToLabel(3)  // 返回 "P3"
```

#### 2. convertLabelToPriority(label: string): number
将优先级标签转换为数据库数字

**示例：**
```typescript
convertLabelToPriority("P0") // 返回 10
convertLabelToPriority("P1") // 返回 8
convertLabelToPriority("P2") // 返回 5
convertLabelToPriority("P3") // 返回 3
```

#### 3. getPriorityColor(priority: number | string): string
获取优先级对应的 Ant Design Tag 颜色

**示例：**
```typescript
getPriorityColor(10)   // 返回 "red"
getPriorityColor("P0") // 返回 "red"
getPriorityColor(5)    // 返回 "blue"
getPriorityColor("P2") // 返回 "blue"
```

## 数据流向

### AI 服务 → 后端 → 数据库
1. AI 服务生成测试用例，返回字符串格式优先级（P0, P1, P2, P3）
2. 前端调用 `/api/v1/test-cases/batch` 接口，传递字符串格式
3. 后端 `TestCaseService.batchSaveTestCases()` 将字符串转换为数字
4. 数据库存储数字格式（0-10）

### 数据库 → 后端 → 前端显示
1. 数据库返回数字格式优先级（0-10）
2. 后端传递数字给前端
3. 前端使用 `convertPriorityToLabel()` 转换为显示标签
4. 使用 `getPriorityColor()` 获取对应颜色
5. 在 UI 中显示为 P0-P3 标签

## 已更新的前端组件

以下组件已统一使用 `priorityUtils.ts` 工具函数：

1. `TestCaseList.jsx` - 测试用例列表页面
2. `SmartGenerate.tsx` - 智能生成测试用例页面
3. `TestCasePrioritization.tsx` - 测试用例优先级排序页面
4. `GenerationHistory.tsx` - 生成历史页面
5. `AITestCaseGeneration.jsx` - AI 测试用例生成页面

## 使用示例

### 在表格列中显示优先级
```jsx
{
  title: '优先级 (Priority)',
  dataIndex: 'priority',
  key: 'priority',
  render: (priority) => {
    const label = convertPriorityToLabel(priority)
    return <Tag color={getPriorityColor(priority)}>{label}</Tag>
  },
}
```

### 在详情页显示优先级
```jsx
<Tag color={getPriorityColor(testCase.priority)}>
  {convertPriorityToLabel(testCase.priority)}
</Tag>
```

## 测试验证

### 数据库中的值
```sql
SELECT id, title, priority FROM test_cases LIMIT 5;
```

| id | title | priority |
|----|-------|----------|
| ... | 登录测试 | 10 |
| ... | 搜索测试 | 8 |
| ... | 设置测试 | 5 |

### 前端显示结果
- priority = 10 → 显示 "P0" （红色）
- priority = 8 → 显示 "P1" （橙色）
- priority = 5 → 显示 "P2" （蓝色）

## 注意事项

1. **不要直接拼接**：避免使用 `P{priority}` 这种方式显示优先级
2. **统一使用工具函数**：所有需要显示优先级的地方都应使用 `priorityUtils.ts` 中的函数
3. **保持一致性**：确保前后端优先级映射关系一致
4. **新增组件**：如果添加新的显示测试用例的组件，必须使用统一的工具函数

## 相关文件

- 前端工具函数：`frontend/src/utils/priorityUtils.ts`
- 后端服务：`backend/src/main/java/com/synapsetest/testmanagement/service/TestCaseService.java`
- 数据库 Schema：`database/unified_schema.sql`
- 本文档：`docs/PRIORITY_MAPPING.md`

## 更新历史

- 2025-12-15: 创建优先级映射规范文档，修复所有前端页面的优先级显示问题
