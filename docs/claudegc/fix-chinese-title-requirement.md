# 修复：智能生成测试用例标题必须全中文

## 问题描述

用户反馈在使用AI智能生成测试用例功能时，生成的测试用例标题（title/name）中出现了英文，但要求应该全部使用中文。

## 问题分析

经过分析，发现问题涉及以下几个层面：

### 1. AI Prompt层面 ✅
- **原状态**：prompt中已有中文要求，但强调不够明显
- **问题**：某些LLM模型可能忽略了这些要求

### 2. AI验证层面 ✅
- **原状态**：已有验证逻辑跳过包含英文的测试用例
- **位置**：`ai-service/models/llm/rag_generator.py` 第345-349行
- **状态**：正常工作

### 3. 后端接收层面 ❌ **问题所在**
- **问题**：后端 `batchSaveTestCases` 方法只检查 `title` 和 `case_name` 字段
- **根源**：AI生成的字段名是 `name`，但后端没有读取这个字段
- **结果**：即使AI生成了中文的 `name`，也无法正确保存到数据库的 `title` 字段

### 4. 前端显示层面
- **行为**：优先显示 `title` 字段，其次是 `name`
- **位置**：`frontend/src/components/test-case/TestCaseOptimization.tsx` 第475行

## 解决方案

### 1. 强化AI Prompt（增强约束）

**文件**：`ai-service/utils/prompt_builder.py`

**修改内容**：
- 在prompt开头添加更醒目的警告和示例
- 使用🚨表情符号强调重要性
- 提供正确✅和错误❌的对比示例
- 在多个位置重复强调要求
- 更新JSON示例，使用实际的中文名称

**具体修改**：
```python
【最高优先级要求 - 必须严格遵守】
🚨 测试用例的name（名称）字段必须100%使用中文，不得包含任何英文字母（a-z, A-Z）！
🚨 包含英文的测试用例将被系统自动丢弃，不会被保存！
🚨 示例：
   ✅ 正确："验证用户登录功能"
   ✅ 正确："测试订单支付流程"
   ❌ 错误："Test Login"（包含英文）
   ❌ 错误："验证Login功能"（包含英文Login）
   ❌ 错误："testcase-001"（包含英文）
```

### 2. 修复后端字段映射（核心修复）

**文件**：`backend/src/main/java/com/synapsetest/testmanagement/service/TestCaseService.java`

**问题**：原代码只检查 `title` 和 `case_name`
```java
// 原代码
String title = (String) tcMap.get("title");
if (title == null) {
    title = (String) tcMap.get("case_name");
}
```

**修复**：添加对 `name` 字段的支持
```java
// 修复后的代码
String title = (String) tcMap.get("title");
if (title == null) {
    title = (String) tcMap.get("name");  // AI生成的字段
}
if (title == null) {
    title = (String) tcMap.get("case_name");
}

// 验证：如果包含英文则记录警告
if (title != null && title.matches(".*[a-zA-Z]+.*")) {
    log.warn("⚠️  Test case title contains English letters (should be Chinese only): {}", title);
}
```

**优先级顺序**：`title` > `name` > `case_name`

### 3. 添加后端验证（双重保障）

在后端 `batchSaveTestCases` 方法中添加验证逻辑：
- 如果标题包含英文字母，记录警告日志
- 便于追踪和调试问题

### 4. 修复Fallback生成逻辑（保持一致性）

**文件**：`backend/src/main/java/com/synapsetest/testmanagement/service/AITestCaseGenerationService.java`

**问题**：当AI服务不可用时，使用本地fallback生成，但生成的内容包含英文：
```java
// 原代码 - 英文场景
scenarios.add("User login with valid credentials");
scenarios.add("User registration with valid data");

// 原代码 - 英文步骤
steps.add("Navigate to login page");
steps.add("Enter username in username field");

// 原代码 - 英文标题
testCase.setTitle(String.format("TC%03d: %s", index, scenario));
```

**修复**：将所有fallback生成的内容改为中文
```java
// 修复后 - 中文场景
scenarios.add("验证用户使用有效凭证登录");
scenarios.add("验证用户使用有效数据注册");

// 修复后 - 中文步骤
steps.add("导航到登录页面");
steps.add("在用户名字段输入用户名");

// 修复后 - 中文标题
testCase.setTitle(String.format("测试用例%03d：%s", index, scenario));
```

**影响范围**：
- `analyzeInputScenarios` 方法：场景描述改为中文
- `generateTestSteps` 方法：测试步骤改为中文
- `generateExpectedResults` 方法：预期结果改为中文
- `generateTestCaseForScenario` 方法：标题和描述改为中文

## 修改文件清单

1. ✅ `ai-service/utils/prompt_builder.py` - 强化prompt约束
2. ✅ `backend/src/main/java/com/synapsetest/testmanagement/service/TestCaseService.java` - 修复字段映射，添加验证
3. ✅ `backend/src/main/java/com/synapsetest/testmanagement/service/AITestCaseGenerationService.java` - 修复fallback生成逻辑，使用中文

## 测试建议

### 1. 单元测试
```python
# 测试AI生成的用例name字段是否为纯中文
def test_chinese_only_name():
    result = generator.generate(requirement_text="测试需求", module="测试模块")
    for case in result['testcases']:
        assert not re.search(r'[a-zA-Z]', case['name']), f"测试用例名称包含英文: {case['name']}"
```

### 2. 集成测试
1. 使用AI生成测试用例
2. 检查生成的JSON中的 `name` 字段
3. 验证后端是否正确将 `name` 映射到 `title`
4. 在前端查看显示的标题是否为纯中文

### 3. 手动测试步骤
1. 访问AI测试用例生成页面
2. 输入需求文档
3. 点击"智能生成"
4. 查看生成的测试用例标题
5. 确认所有标题都是纯中文

## 预期效果

修复后：
- ✅ AI生成的测试用例 `name` 字段必须为纯中文
- ✅ 包含英文的用例会被AI验证层自动过滤
- ✅ 后端正确读取 `name` 字段并映射到 `title`
- ✅ 后端会记录包含英文的标题警告
- ✅ 前端显示的标题全部为中文

## 注意事项

1. **LLM模型选择**：某些LLM模型（特别是非中文优化的模型）可能仍然会忽略prompt要求，建议使用中文优化的模型（如Qwen、ChatGLM等）

2. **历史数据**：已存在的包含英文标题的测试用例不会被自动修改，需要手动清理

3. **多语言支持**：如果未来需要支持多语言，需要修改验证逻辑，添加语言参数

## 维护建议

1. 定期检查日志中的警告信息，识别是否有包含英文的标题
2. 如果发现大量警告，考虑调整LLM模型或进一步强化prompt
3. 考虑在前端添加客户端验证，在用户保存前提示标题包含英文

## 相关代码位置

- AI Prompt定义：`ai-service/utils/prompt_builder.py` 第10-70行
- AI验证逻辑：`ai-service/models/llm/rag_generator.py` 第345-349行
- 后端字段映射：`backend/src/main/java/com/synapsetest/testmanagement/service/TestCaseService.java` 第214-229行
- 后端Fallback生成：`backend/src/main/java/com/synapsetest/testmanagement/service/AITestCaseGenerationService.java` 第226-340行
- 前端显示逻辑：`frontend/src/components/test-case/TestCaseOptimization.tsx` 第475行

## 完成检查清单

- ✅ 强化AI Prompt，添加多处醒目的中文要求提示
- ✅ 提供正确✅和错误❌的对比示例
- ✅ 更新JSON示例，使用实际的中文名称
- ✅ 修复后端字段映射，支持AI生成的`name`字段
- ✅ 添加后端验证逻辑，记录包含英文的标题警告
- ✅ 修复Fallback生成逻辑，全部改为中文
- ✅ 更新所有三个prompt函数（生成、边界、API）
- ✅ 创建详细的修复文档

## 版本信息

- 修复日期：2025-12-15
- 修复分支：001-ai-testing-platform-reform
- 相关Issue：智能生成测试用例时title需要全中文
- 修改文件数：3个（1个Python文件，2个Java文件）
