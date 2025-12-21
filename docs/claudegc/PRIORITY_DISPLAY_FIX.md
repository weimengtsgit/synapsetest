# 优先级显示统一修复文档

## 问题描述

在测试用例列表等页面中，当数据库priority字段值为8时，被错误地显示为"P8"，而不是正确的"P1"。

## 优先级映射规则

### 数据库存储（0-10的整数）

- **P0 → 10**（最高优先级）
- **P1 → 8**
- **P2 → 5**
- **P3 → 3**（最低优先级）

### 显示转换规则

从数据库数字转换为显示标签：
- `9-10` → **P0**（最高优先级）
- `7-8` → **P1**
- `4-6` → **P2**
- `0-3` → **P3**（最低优先级）

## 根本原因

部分前端组件直接显示了从后端返回的priority数字值，而没有使用`convertPriorityToLabel()`函数进行转换。

## 修复内容

### 1. 已修复的组件

#### `/frontend/src/components/test-case/GenerationHistory.tsx`

**修复内容：**
- 导入`convertPriorityToLabel`工具函数
- 更新表格列的render函数，支持数字和字符串两种格式
- 更新详情Modal中的优先级显示

**修复代码：**

```typescript
// 导入
import { getPriorityColor, convertPriorityToLabel } from '../../utils/priorityUtils'

// 表格列渲染
{
  title: '优先级',
  dataIndex: 'priority',
  key: 'priority',
  width: 100,
  render: (priority: string | number) => {
    // 统一转换：支持数字（0-10）和字符串（P0-P3）两种格式
    const label = typeof priority === 'number' 
      ? convertPriorityToLabel(priority) 
      : priority
    return <Tag color={getPriorityColor(priority)}>{label}</Tag>
  },
}

// 详情Modal
<Tag color={getPriorityColor(selectedRecord.priority)}>
  {typeof selectedRecord.priority === 'number' 
    ? convertPriorityToLabel(selectedRecord.priority) 
    : selectedRecord.priority}
</Tag>
```

#### `/frontend/src/components/test-case/SmartGenerate.tsx`

**修复内容：**
- 导入`convertPriorityToLabel`工具函数
- 更新`renderPriorityTag`函数，支持数字和字符串两种格式

**修复代码：**

```typescript
// 导入
import { getPriorityColor, convertPriorityToLabel } from '../../utils/priorityUtils'

// 渲染函数
const renderPriorityTag = (priority: string | number) => {
  const label = typeof priority === 'number' 
    ? convertPriorityToLabel(priority) 
    : priority
  return <Tag color={getPriorityColor(priority)}>{label}</Tag>
}
```

### 2. 已验证正确的组件

以下组件已经正确使用了优先级转换逻辑，无需修改：

- ✅ `/frontend/src/components/test-case/TestCaseList.jsx`
- ✅ `/frontend/src/components/test-case/TestCasePrioritization.tsx`
- ✅ `/frontend/src/components/test-case/AITestCaseGeneration.jsx`
- ✅ `/frontend/src/components/test-case/TestCaseQualityAnalysis.tsx`（只显示统计数据）

## 工具函数说明

### `/frontend/src/utils/priorityUtils.ts`

提供了完整的优先级转换工具函数：

```typescript
/**
 * 将数据库中的数字优先级转换为显示标签（P0-P3）
 */
export function convertPriorityToLabel(priority: number | undefined | null): string {
  if (priority === undefined || priority === null) {
    return 'P2' // 默认值
  }

  if (priority >= 9) {
    return 'P0' // 最高优先级
  } else if (priority >= 7) {
    return 'P1'
  } else if (priority >= 4) {
    return 'P2'
  } else {
    return 'P3' // 最低优先级
  }
}

/**
 * 将优先级标签（P0-P3）转换为数据库数字
 */
export function convertLabelToPriority(label: string | undefined | null): number {
  // ...
}

/**
 * 获取优先级对应的颜色
 */
export function getPriorityColor(priority: number | string | undefined | null): string {
  // ...
}

/**
 * 获取优先级的数值（用于排序）
 */
export function getPriorityValue(priority: number | string | undefined | null): number {
  // ...
}
```

## 测试覆盖

已有完整的单元测试覆盖：`/frontend/src/utils/__tests__/priorityUtils.test.ts`

测试用例包括：
- ✅ 数字8 → P1（关键测试）
- ✅ 数字10 → P0
- ✅ 数字5 → P2
- ✅ 数字3 → P3
- ✅ 往返转换一致性测试
- ✅ 边界值测试
- ✅ 颜色映射测试

## 使用建议

### 对于新组件开发

1. **始终使用工具函数**：从`priorityUtils.ts`导入转换函数
2. **支持两种格式**：组件应该同时支持数字和字符串格式的priority
3. **类型定义清晰**：在TypeScript接口中明确定义priority类型

示例代码：

```typescript
import { convertPriorityToLabel, getPriorityColor } from '../../utils/priorityUtils'

// 渲染优先级标签（兼容两种格式）
const renderPriority = (priority: string | number) => {
  const label = typeof priority === 'number' 
    ? convertPriorityToLabel(priority) 
    : priority
  return <Tag color={getPriorityColor(priority)}>{label}</Tag>
}
```

### 对于数据流

1. **后端返回**：TestCaseResponse中priority为Integer（0-10）
2. **AI服务返回**：priority为String（P0/P1/P2/P3）
3. **前端展示**：统一使用`convertPriorityToLabel`转换为P0/P1/P2/P3标签
4. **保存到数据库**：使用`convertLabelToPriority`转换为数字（如果需要）

## 验证步骤

1. 启动前端应用
2. 访问"用例列表"页面
3. 检查数据库priority=8的测试用例，应显示为"P1"（橙色标签）
4. 访问"生成历史"页面，验证优先级显示正确
5. 访问"智能生成"页面，验证生成的用例优先级显示正确

## 相关文档

- [优先级设计文档](../DUAL_ID_DESIGN.md)
- [数据库Schema](../../database/unified_schema.sql)
- [后端优先级转换](../../backend/src/main/java/com/synapsetest/testmanagement/service/TestCaseService.java)
- [AI服务优先级映射](../../ai-service/data/mysql_client.py)

## 总结

本次修复确保了所有前端页面中的优先级显示逻辑统一，避免了"P8"这样的错误显示。通过使用统一的工具函数，保证了代码的可维护性和一致性。









