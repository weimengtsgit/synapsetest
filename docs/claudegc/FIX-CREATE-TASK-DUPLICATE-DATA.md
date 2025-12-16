# 创建任务页面重复数据修复

## 问题描述

### 问题1：测试环境下拉列表展示重复数据
- **现象**：接口返回3条环境信息，但页面显示了6条
- **原因**：代码中同时包含了硬编码的选项和API返回的数据

### 问题2：页面重复请求接口
- **现象**：测试环境接口和测试版本接口各被调用2次
- **原因**：React.StrictMode在开发模式下会故意执行组件两次（包括useEffect）

## 根本原因分析

### 原因1：重复数据源
在 `frontend/src/components/test-task/CreateTestTask.jsx` 第112-121行：

```jsx
<Select placeholder="Select test environment">
  {/* 硬编码的3个选项 */}
  <Option value="DEV">开发环境 (DEV)</Option>
  <Option value="STAGING">预发环境 (STAGING)</Option>
  <Option value="PROD">生产环境 (PROD)</Option>
  
  {/* 从API返回的数据（可能也是这3个） */}
  {environments.map((env) => (
    <Option key={env.id} value={env.name}>
      {env.name}
    </Option>
  ))}
</Select>
```

如果API返回的数据恰好也是 DEV、STAGING、PROD，就会导致重复显示。

### 原因2：React.StrictMode
在 `frontend/src/main.tsx` 第13行开启了 `<React.StrictMode>`：

```tsx
ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <Provider store={store}>
      ...
    </Provider>
  </React.StrictMode>,
)
```

在**开发模式**下，React.StrictMode会：
- 故意执行组件两次
- 故意执行useEffect两次
- 目的是帮助开发者发现潜在的副作用问题

**注意**：在生产环境中，StrictMode不会导致重复渲染。

## 修复方案

### 修复1：移除硬编码选项
只使用从API返回的数据，移除硬编码的选项：

```jsx
<Select placeholder="Select test environment">
  {environments.map((env) => (
    <Option key={env.id} value={env.name}>
      {env.name}
    </Option>
  ))}
</Select>
```

### 修复2：防止重复请求
使用 `useRef` 防止在StrictMode下的重复请求：

```jsx
import React, { useState, useEffect, useRef } from 'react'

const CreateTestTask = () => {
  // ... other state
  const hasFetchedData = useRef(false)

  useEffect(() => {
    // Prevent duplicate API calls in React StrictMode
    if (!hasFetchedData.current) {
      hasFetchedData.current = true
      loadInitialData()
    }
  }, [])
}
```

## 修改文件

- `frontend/src/components/test-task/CreateTestTask.jsx`
  - 第1行：添加 `useRef` 导入
  - 第20行：添加 `hasFetchedData` ref
  - 第22-28行：修改 useEffect 逻辑，防止重复请求
  - 第112-124行：移除硬编码的测试环境选项

## 测试验证

修复后应验证：
1. ✅ 测试环境下拉列表只显示3条数据（不再重复）
2. ✅ 在开发模式下，接口只被调用1次（不再重复请求）
3. ✅ 测试版本下拉列表正常显示
4. ✅ 表单提交功能正常工作

## 影响范围

- **影响文件**：`frontend/src/components/test-task/CreateTestTask.jsx`
- **影响功能**：创建测试任务页面
- **向后兼容性**：✅ 无破坏性变更
- **数据库影响**：无

## 备注

### 关于React.StrictMode
- StrictMode在开发模式下的重复渲染是**有意的设计**，不是bug
- 它帮助开发者发现：
  - 不安全的生命周期方法
  - 过时的API使用
  - 意外的副作用
- 在生产构建中，StrictMode不会造成任何性能问题
- 建议**保留** StrictMode，使用 useRef 等方式处理副作用

### 为什么不移除StrictMode？
移除StrictMode虽然可以避免重复请求，但会失去：
- 副作用检测能力
- 未来React并发模式的兼容性
- 代码质量保障

因此，推荐的做法是使用 useRef 等技术来正确处理副作用，而不是移除StrictMode。

## 修复日期
2024-12-16

## 修复人员
AI Assistant
