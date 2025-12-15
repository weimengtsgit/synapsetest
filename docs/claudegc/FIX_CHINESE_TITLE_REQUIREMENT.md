# 修复：测试用例生成时Title必须全中文

## 问题描述

在智能生成测试用例时，部分生成的用例title出现了英文，不符合中文标题的要求。

## 根本原因分析

在 `ai-service/utils/prompt_builder.py` 文件中，虽然AI生成提示词(prompt)提供了中文示例，但**没有明确强调name字段必须使用中文**。这导致LLM在生成测试用例时，有时会使用英文来命名测试用例。

主要问题点：
1. `TESTCASE_GENERATION_PROMPT` - 主测试用例生成prompt
2. `build_edge_case_prompt` - 边界用例生成prompt  
3. `build_api_test_prompt` - API测试用例生成prompt

这三个prompt模板中，虽然提供了中文示例，但缺少对"必须使用中文"的明确要求。

## 解决方案

在所有测试用例生成的prompt中，明确添加"name字段必须使用中文"的要求：

### 1. 主测试用例生成Prompt (TESTCASE_GENERATION_PROMPT)

**修改位置**：`ai-service/utils/prompt_builder.py` 第24-54行

**修改内容**：
- 在"输出格式要求"部分，将第1点修改为：
  ```
  1. 用例名称（name）：简明扼要描述测试目标，**必须使用中文**
  ```

- 在JSON格式示例中，明确标注：
  ```json
  "name": "用例名称（必须中文）"
  ```

- 在"注意事项"部分，添加第一条规则：
  ```
  - **name字段必须使用中文，禁止使用英文**
  ```

### 2. 边界用例生成Prompt (build_edge_case_prompt)

**修改位置**：`ai-service/utils/prompt_builder.py` 第143-167行

**修改内容**：
- 在JSON格式示例中：
  ```json
  "name": "测试用例名称（必须中文）"
  ```

- 在"注意"部分，添加第一条：
  ```
  - **name字段必须使用中文，禁止使用英文**
  ```

### 3. API测试用例生成Prompt (build_api_test_prompt)

**修改位置**：`ai-service/utils/prompt_builder.py` 第191-192行

**修改内容**：
- 添加"重要"部分：
  ```
  【重要】
  - 测试用例的name字段必须使用中文，禁止使用英文
  ```

## 文件修改清单

| 文件路径 | 修改说明 |
|---------|---------|
| `ai-service/utils/prompt_builder.py` | 在所有prompt模板中明确要求name字段使用中文 |

## 验证方法

1. **重启AI服务**（已自动完成）：
   - AI服务使用了 `--reload` 参数，已自动检测文件更改并重新加载

2. **测试生成新用例**：
   ```bash
   # 使用智能生成功能生成新的测试用例
   # 检查生成的测试用例的title/name字段是否全部为中文
   ```

3. **检查示例**：
   - 在前端"智能生成"页面输入需求文档
   - 点击"生成测试用例"
   - 查看生成的用例，确认title全部为中文

## 影响范围

- ✅ **新生成的测试用例**：将严格遵守中文title要求
- ❌ **已存在的测试用例**：不受影响（历史数据保持不变）

## 注意事项

1. **历史数据**：数据库中已存在的英文title测试用例不会自动更新，需要手动修改或重新生成
2. **LLM限制**：虽然prompt明确要求使用中文，但LLM仍可能偶尔生成英文title，建议在前端添加验证逻辑
3. **服务重启**：由于使用了 `--reload` 模式，服务已自动重新加载，无需手动重启

## 后续优化建议

### 1. 前端验证（推荐）
在前端接收到生成的测试用例后，添加验证逻辑：

```javascript
// 检查title是否包含英文字母
function validateChineseTitle(title) {
  const englishPattern = /[a-zA-Z]/;
  if (englishPattern.test(title)) {
    console.warn(`测试用例标题包含英文: ${title}`);
    return false;
  }
  return true;
}
```

### 2. 后端后处理（备选）
在 `ai-service/models/llm/rag_generator.py` 的 `_validate_testcases` 方法中添加验证：

```python
def _validate_testcases(self, testcases: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
    validated = []
    for tc in testcases:
        # 检查name是否包含英文
        name = tc.get('name', '')
        if re.search(r'[a-zA-Z]', name):
            logger.warning(f"测试用例名称包含英文，将跳过: {name}")
            continue
        # ... 其他验证逻辑
```

### 3. 监控和报警
在生成日志中添加监控，当检测到英文title时发出警告。

## 测试结果

✅ 修改已完成
✅ AI服务已自动重新加载
⏳ 等待用户测试验证新生成的用例是否全部为中文

---

## 第二次修复（2025-12-15 - 增强版）

### 问题反馈
用户反馈：即使在prompt中明确要求使用中文，LLM仍然偶尔生成英文title。

### 根本原因
仅依靠prompt约束不够可靠，LLM有时不会严格遵循指令。需要在后端添加强制验证逻辑。

### 增强解决方案

#### 1. 后端强制验证（核心）

**文件**: `ai-service/models/llm/rag_generator.py`

**修改1**: 添加re模块导入
```python
import re
```

**修改2**: 在`_validate_testcases`方法中添加中文验证逻辑（第346行之后）
```python
# Validate that name is in Chinese (no English letters)
name = tc.get('name', '')
if re.search(r'[a-zA-Z]', name):
    logger.warning(f"⚠️  跳过包含英文的测试用例名称: {name}")
    continue
```

**效果**: 
- 任何包含英文字母的测试用例name将被自动过滤
- 只返回name为全中文的测试用例
- 在日志中记录被过滤的用例

#### 2. 进一步强化Prompt要求

**文件**: `ai-service/utils/prompt_builder.py`

**修改内容**:
1. 在所有三个prompt模板开头添加醒目的强制要求
2. 在JSON示例中使用更明确的说明："全中文，严禁英文"
3. 在注意事项中使用警告符号❌和强调语言
4. 明确告知违反规则的后果："违反此规则的用例将被丢弃！"

**具体位置**:
- `TESTCASE_GENERATION_PROMPT`: 第10-13行（新增），第39-57行（修改）
- `build_edge_case_prompt`: 第135-138行（新增），第158-172行（修改）
- `build_api_test_prompt`: 第181-184行（新增），第202-213行（新增）

### 技术方案总结

采用**双重保障机制**：

1. **前置约束（Prompt强化）**
   - 在prompt开头和多处重复强调中文要求
   - 使用醒目符号（⚠️、❌）提高LLM的注意力
   - 明确告知违规后果

2. **后置验证（代码强制）** ⭐ 核心
   - 使用正则表达式检测英文字母
   - 强制过滤包含英文的测试用例
   - 确保100%返回中文title

### 文件修改清单

| 文件路径 | 修改说明 | 影响范围 |
|---------|---------|---------|
| `ai-service/models/llm/rag_generator.py` | 添加re导入和中文验证逻辑 | 所有测试用例生成 |
| `ai-service/utils/prompt_builder.py` | 强化三个prompt模板的中文要求 | 主用例/边界用例/API用例 |

### 验证方法

1. **检查AI服务日志**：
```bash
# 查看AI服务终端输出
# 如果有英文title被生成，会看到警告：
# ⚠️  跳过包含英文的测试用例名称: xxx
```

2. **前端测试**：
- 使用"智能生成"功能生成测试用例
- 检查所有返回的测试用例name字段
- 应该全部为中文，不含任何英文字母

3. **日志验证**：
```bash
# 在ai-service目录查看日志
grep "跳过包含英文" logs/*.log
```

### 预期效果

- ✅ **100%保证**：返回给前端的测试用例name字段全部为中文
- ✅ **自动过滤**：包含英文的用例自动被过滤，不会返回
- ✅ **可追溯**：日志中记录所有被过滤的英文title
- ✅ **无需重启**：AI服务使用--reload模式，自动检测变更

### 注意事项

1. **可能的用例数量减少**：
   - 如果LLM生成了英文title的用例，这些用例会被过滤
   - 实际返回的用例数量可能少于请求的数量
   - 这是预期行为，确保质量

2. **LLM优化**：
   - 强化的prompt应该显著减少LLM生成英文title的概率
   - 但仍然保留后端验证作为最后一道防线

3. **监控建议**：
   - 定期检查日志，看是否频繁出现英文title被过滤
   - 如果频繁出现，可能需要进一步优化prompt或考虑更换LLM模型

### 测试结果

✅ 代码修改完成
✅ 双重保障机制已实施
✅ AI服务将自动重新加载
⏳ 等待用户验证效果

---

**首次修复时间**: 2025-12-15
**增强修复时间**: 2025-12-15（同日）
**修复人**: Claude AI Assistant
**优先级**: P1（用户体验相关）
**状态**: 已实施双重保障机制
