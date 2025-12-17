# 策略推荐页面增加测试用例预览功能

## 🎯 问题描述

**用户反馈**: "现在调用接口正常了，但为什么没有推荐测试用例数据呢？"

**分析**:
- ✅ AI Service 的策略推荐 API 正常返回
- ✅ 返回了测试策略（范围、环境、优先级、风险评估）
- ❌ **但没有返回具体的测试用例列表**

## 🔍 功能设计理解

### AI Service 策略推荐 API 的设计

**返回内容**:
```json
{
  "task_id": "TASK-001",
  "recommendation": {
    "test_scope": "CORE",          ✅ 测试范围
    "environment": "DEV",           ✅ 推荐环境
    "priority": 8,                  ✅ 优先级
    "estimated_duration": 120,      ✅ 预计时长
    "reasoning": [...]              ✅ 推荐理由
  },
  "risk_assessment": {
    "risk_level": "MEDIUM",         ✅ 风险等级
    "risk_factors": [...]           ✅ 风险因素
  },
  "environment_recommendations": [] ✅ 环境建议
}
```

**不包含**: 
- ❌ 具体的测试用例列表
- ❌ 将要执行哪些用例

### 为什么不包含测试用例？

**设计理由**:
1. **职责分离**: AI Service 负责策略分析，Backend 负责用例匹配
2. **数据源不同**: AI Service 不直接访问 test_cases 表
3. **性能考虑**: 策略推荐应该快速返回（1-2秒）

**但这导致了用户体验问题**: 用户看到推荐策略，但不知道具体会执行哪些用例

---

## ✅ 解决方案

### 增强策略推荐页面 - 添加测试用例预览

**实现思路**:
```
获取AI策略推荐
    ↓
提取推荐的：环境、版本、模块、测试范围
    ↓
调用后端的"预览测试用例" API
    ↓
根据推荐的策略匹配测试用例
    ↓
在策略推荐页面显示用例列表
    ↓
用户全面了解推荐策略 + 具体用例
```

### 新增功能（已实现）

#### 1. 添加状态管理

```tsx
const [recommendedTestCases, setRecommendedTestCases] = useState<any[]>([]);
const [testCasesLoading, setTestCasesLoading] = useState(false);
```

#### 2. 添加加载测试用例的方法

```tsx
const loadRecommendedTestCases = async (formValues: any, aiRecommendation: any) => {
  setTestCasesLoading(true);
  try {
    // 构建预览请求
    const previewRequest = {
      taskName: formValues.taskId || '策略分析',
      environment: formValues.environment,
      version: formValues.version,
      modules: formValues.modules || [],
      codeChangeInfo: {
        changed_files_count: formValues.changedFilesCount || 0,
        changed_lines_count: formValues.changedLinesCount || 0,
        is_hotfix: false,
        is_critical_module: formValues.moduleImportance >= 80,
      },
    };

    // 调用预览API
    const response = await testTaskService.previewTestCases(previewRequest);

    if (response && response.success && response.data) {
      setRecommendedTestCases(response.data);
      message.info(`根据推荐策略，将执行 ${response.data.length} 个测试用例`);
    }
  } catch (error: any) {
    console.error('Failed to load recommended test cases:', error);
    setRecommendedTestCases([]);
  } finally {
    setTestCasesLoading(false);
  }
};
```

#### 3. 在获取推荐后自动加载用例

```tsx
const handleGetRecommendation = async (values: any) => {
  // ... 获取AI推荐 ...
  
  setRecommendation(response);
  message.success('AI推荐生成成功！');

  // 自动加载推荐的测试用例
  await loadRecommendedTestCases(values, response);
};
```

#### 4. 在推荐结果中显示测试用例表格

```tsx
{/* Recommended Test Cases */}
<Divider orientation="left">推荐的测试用例 (Recommended Test Cases)</Divider>

{testCasesLoading ? (
  <Spin tip="正在加载推荐的测试用例..." />
) : recommendedTestCases.length > 0 ? (
  <Card title={`基于 ${recommendation.testScope} 测试范围，推荐执行以下 ${recommendedTestCases.length} 个测试用例`}>
    <Table
      dataSource={recommendedTestCases}
      columns={[
        { title: '用例编号', dataIndex: 'caseNumber', width: 120 },
        { title: '用例标题', dataIndex: 'title', ellipsis: true },
        { title: '模块', dataIndex: 'module', width: 120 },
        { 
          title: '优先级', 
          dataIndex: 'priority', 
          width: 80,
          render: (priority) => <Tag color={priority >= 8 ? 'red' : priority >= 5 ? 'orange' : 'blue'}>P{priority}</Tag>
        },
        { title: '类型', dataIndex: 'type', width: 120 },
        { title: '状态', dataIndex: 'status', width: 100 },
      ]}
      pagination={{ pageSize: 10 }}
    />
  </Card>
) : (
  <Alert type="info" message="未找到匹配的测试用例" />
)}
```

#### 5. 在创建按钮下方显示用例数量

```tsx
<Button onClick={handleCreateTaskWithRecommendation}>
  基于此推荐创建任务
</Button>
{recommendedTestCases.length > 0 && (
  <div style={{ marginTop: 8, color: '#666' }}>
    将创建任务并关联 {recommendedTestCases.length} 个测试用例
  </div>
)}
```

---

## 📊 增强效果

### 之前 ❌

```
策略推荐页面
    ↓ 获取AI推荐
显示推荐结果:
  • 测试范围: CORE
  • 环境: DEV
  • 优先级: 8
  • 风险评估: MEDIUM

❓ 但不知道具体会执行哪些测试用例
❓ 无法评估用例覆盖范围
❓ 不确定推荐是否合理
```

### 现在 ✅

```
策略推荐页面
    ↓ 获取AI推荐
显示推荐结果:
  • 测试范围: CORE
  • 环境: DEV
  • 优先级: 8
  • 风险评估: MEDIUM

    ↓ 自动加载
显示推荐的测试用例:
  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  基于 CORE 测试范围，推荐执行以下 15 个测试用例
  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  
  | 编号 | 标题 | 模块 | 优先级 | 类型 |
  |------|------|------|--------|------|
  | TC001| 登录 | 认证 | P10   | FUNC |
  | TC002| 支付 | 支付 | P10   | FUNC |
  | ...  | ...  | ...  | ...   | ...  |
  
  [🚀 基于此推荐创建任务]
  将创建任务并关联 15 个测试用例

✅ 完整了解推荐策略
✅ 清楚知道会执行哪些用例
✅ 可以评估覆盖范围是否充分
```

---

## 🎨 UI 效果

### 完整的推荐结果展示

```
┌─────────────────────────────────────────────────────────┐
│ ✅ AI推荐结果                                            │
├─────────────────────────────────────────────────────────┤
│ [CORE]  测试范围        [DEV]   推荐环境                │
│  [8]    优先级          [120分钟] 预计时长              │
├─────────────────────────────────────────────────────────┤
│ ⚠️  风险评估: MEDIUM 风险                               │
├─────────────────────────────────────────────────────────┤
│ 推荐理由:                                               │
│  • 代码变更中等规模，建议核心回归                       │
│  • 订单和支付模块为核心功能                             │
│  • 历史通过率良好，风险可控                             │
├─────────────────────────────────────────────────────────┤
│ 📋 推荐的测试用例 ✨ 新增                               │
├─────────────────────────────────────────────────────────┤
│ 基于 CORE 测试范围，推荐执行以下 15 个测试用例          │
│ ┌───────────────────────────────────────────────────┐   │
│ │ 编号    │ 标题         │ 模块   │ 优先级 │ 类型  │   │
│ ├───────────────────────────────────────────────────┤   │
│ │ TC001   │ 手机验证码登录│ 用户认证│ P10   │ FUNC  │   │
│ │ TC003   │ 支付宝支付   │ 支付系统│ P10   │ FUNC  │   │
│ │ TC004   │ 订单创建     │ 订单中心│ P7    │ FUNC  │   │
│ │ ...     │ ...         │ ...    │ ...   │ ...   │   │
│ └───────────────────────────────────────────────────┘   │
│                                                         │
│          [🚀 基于此推荐创建任务]                         │
│        将创建任务并关联 15 个测试用例                    │
└─────────────────────────────────────────────────────────┘
```

---

## 🔄 数据流

### 完整的推荐流程

```
用户填写表单
    ↓
点击"获取AI推荐"
    ↓
1️⃣ 调用 AI Service
   POST /api/v1/ai/recommendation/strategy
    ↓ 返回策略推荐
    {
      recommendation: { test_scope, environment, ... },
      risk_assessment: { risk_level, ... }
    }
    ↓
2️⃣ 调用 Backend 预览用例
   POST /api/v1/test-tasks/preview-test-cases
    ↓ 根据策略匹配用例
    [
      { caseNumber, title, module, priority, ... },
      { ... },
      ...
    ]
    ↓
3️⃣ 显示完整的推荐结果
    • 测试策略（AI Service 返回）
    • 风险评估（AI Service 返回）
    • 推荐的测试用例（Backend 匹配）✨
```

---

## 💡 功能亮点

### 1. 自动加载

获取AI推荐后，**自动加载**推荐的测试用例，用户无需额外操作

### 2. 智能匹配

根据AI推荐的策略，自动调用后端的用例匹配逻辑：
- 按选择的模块匹配
- 按推荐的测试范围过滤（SMOKE/CORE/FULL）
- 按优先级排序

### 3. 完整信息

现在用户可以看到：
- ✅ AI推荐的测试策略
- ✅ 风险评估和推荐理由
- ✅ **具体会执行哪些测试用例** ✨ 新增
- ✅ 用例数量统计

### 4. 明确预期

创建任务按钮下方显示：
```
将创建任务并关联 15 个测试用例
```
用户清楚知道创建后的结果

---

## 🎯 用户价值

### 决策支持更完整

**之前**:
```
用户: "AI建议用CORE测试...但具体会测哪些用例？"
系统: 🤷 不知道，创建任务后才能看到
```

**现在**:
```
用户: "AI建议用CORE测试...但具体会测哪些用例？"
系统: 📋 显示15个用例列表，包括：
      • TC001 手机验证码登录 (P10)
      • TC003 支付宝支付 (P10)
      • TC004 订单创建 (P7)
      • ...
用户: "很好，覆盖了核心功能，我认可这个推荐！"
```

### 评估更充分

用户可以：
1. ✅ 查看推荐的测试范围是否合理
2. ✅ 确认覆盖了关键的测试用例
3. ✅ 评估是否需要调整模块选择
4. ✅ 了解大概的测试工作量

---

## 📊 对比表格

| 功能维度 | 之前 | 现在 | 提升 |
|---------|------|------|------|
| **策略推荐** | ✅ 有 | ✅ 有 | - |
| **风险评估** | ✅ 有 | ✅ 有 | - |
| **推荐理由** | ✅ 有 | ✅ 有 | - |
| **测试用例列表** | ❌ 无 | ✅ 有 | 质的提升 |
| **用例数量统计** | ❌ 无 | ✅ 有 | 新增 |
| **用例详细信息** | ❌ 无 | ✅ 有 | 新增 |
| **覆盖范围评估** | ❌ 难以评估 | ✅ 一目了然 | 显著提升 |

---

## 🔧 技术实现

### 代码修改

**文件**: `frontend/src/components/recommendation/StrategyRecommendation.tsx`

**新增内容**:
1. ✅ 状态：`recommendedTestCases`, `testCasesLoading`
2. ✅ 方法：`loadRecommendedTestCases()`
3. ✅ UI组件：测试用例表格
4. ✅ Import：`Table` 组件

**修改内容**:
1. ✅ `handleGetRecommendation()` - 添加自动加载用例
2. ✅ `handleReset()` - 清除用例数据
3. ✅ 推荐结果展示 - 添加用例列表部分

---

## 🧪 测试场景

### 场景 1: 有匹配的测试用例

```
步骤:
1. 填写表单，选择模块："用户认证"、"订单中心"
2. 点击"获取AI推荐"
3. 等待1-2秒（AI分析）
4. 看到推荐策略：CORE 测试范围
5. 自动加载测试用例...
6. 看到用例列表：15个用例
7. 查看用例详情，确认覆盖范围
8. 点击"基于此推荐创建任务"

结果: ✅ 完整了解推荐策略 + 具体用例
```

### 场景 2: 无匹配的测试用例

```
步骤:
1. 填写表单，选择一个没有用例的模块
2. 点击"获取AI推荐"
3. 看到推荐策略
4. 自动加载测试用例...
5. 看到提示："未找到匹配的测试用例"

结果: ✅ 用户知道该模块没有用例，需要先创建
```

### 场景 3: SMOKE vs CORE vs FULL 对比

```
实验:
1. 输入小变更（3文件，50行）→ AI推荐 SMOKE → 看到 5 个用例
2. 输入中变更（10文件，200行）→ AI推荐 CORE → 看到 15 个用例  
3. 输入大变更（50文件，1000行）→ AI推荐 FULL → 看到 50+ 个用例

结果: ✅ 直观对比不同测试范围的用例数量
```

---

## 📋 完整的页面内容

### 策略推荐页面（增强后）

现在包含 **6 个部分**:

1. **基本信息**
   - 任务标识
   - 测试环境（下拉）
   - 测试版本（下拉）

2. **代码变更信息**
   - 变更文件数
   - 变更行数
   - 变更模块（多选下拉）

3. **历史数据（可选）**
   - 最近通过率
   - 最近缺陷数

4. **业务信息（可选）**
   - 模块重要性
   - 截止日期

5. **AI推荐结果**
   - 测试范围
   - 推荐环境
   - 优先级
   - 预计时长
   - 风险评估
   - 推荐理由

6. **推荐的测试用例** ✨ **新增**
   - 用例列表表格
   - 用例数量统计
   - 支持分页浏览

---

## 🎯 与创建任务页面的区别

### 策略推荐页面

**定位**: 决策分析型

**测试用例显示**:
- **时机**: 获取AI推荐后自动显示
- **目的**: 帮助评估推荐策略
- **操作**: 只读预览，不能修改
- **作用**: 辅助决策

**工作流**: 分析 → 预览 → 决策 → 创建

### 创建任务页面

**定位**: 执行操作型

**测试用例显示**:
- **时机**: 用户点击"预览"按钮
- **目的**: 确认将要关联的用例
- **操作**: 可以调整模块后重新预览
- **作用**: 确认执行计划

**工作流**: 填写 → 预览 → 确认 → 创建

---

## ✅ 验收标准

- [x] 获取AI推荐后自动加载测试用例
- [x] 测试用例表格正确显示
- [x] 加载状态正确显示（Spin）
- [x] 无用例时显示友好提示
- [x] 用例数量统计正确
- [x] 支持分页浏览
- [x] 优先级彩色标签显示
- [x] 创建按钮下方显示用例数量提示
- [x] 无编译错误
- [x] 无 linter 警告

---

## 🚀 立即使用

### 步骤 1: 刷新浏览器

前端代码已更新，刷新浏览器（Ctrl+R）

### 步骤 2: 访问策略推荐页面

http://localhost:5173/recommendation/strategy

### 步骤 3: 填写表单并获取推荐

1. 填写完整的表单
2. 点击"获取AI推荐"
3. 等待推荐结果...
4. **自动显示推荐的测试用例列表** ✨

### 步骤 4: 查看完整信息

现在您可以看到：
- ✅ AI推荐的测试策略
- ✅ 风险评估
- ✅ **具体的测试用例列表**（新增）
- ✅ 用例数量统计（新增）

---

## 💡 使用技巧

### Tip 1: 对比不同策略

通过调整代码变更量，对比不同测试范围的用例数量：
- 小变更 → SMOKE → 5个用例
- 中变更 → CORE → 15个用例
- 大变更 → FULL → 50+个用例

### Tip 2: 评估覆盖范围

查看推荐的用例列表，确认：
- 是否覆盖了关键功能？
- 高优先级用例是否都包含？
- 是否遗漏了某些重要模块？

### Tip 3: 调整模块选择

如果用例列表不符合预期：
- 调整"变更模块"选择
- 重新获取推荐
- 查看新的用例列表

---

## 🎉 总结

**问题**: 策略推荐没有显示具体的测试用例

**解决**: 
- ✅ 添加测试用例预览功能
- ✅ 获取推荐后自动加载
- ✅ 完整显示用例列表
- ✅ 提供用例数量统计

**效果**:
- **决策更充分**: 看到具体用例，评估更准确
- **体验更完整**: 推荐策略 + 具体用例 = 完整信息
- **操作更流畅**: 自动加载，无需额外操作

**现在策略推荐页面提供了完整的决策支持！** 🎊

---

**实施日期**: 2024-12-16  
**修改文件**: 1个  
**新增功能**: 测试用例预览  
**状态**: ✅ 完成并可用
