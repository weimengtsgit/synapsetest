# LLM模型配置指南

本文档介绍如何在 AI Service 中配置和使用 Qwen 和 DeepSeek 大模型。

## 支持的模型提供者

AI Service 支持以下 LLM 提供者：

| 提供者 | 说明 | 使用场景 |
|-------|------|---------|
| `mock` | 模拟模型，返回预定义的测试用例 | 开发和测试 |
| `qwen-local` | 本地运行的通义千问模型 | 私有部署，数据安全要求高 |
| `qwen-api` | 通义千问 API 服务 | 快速部署，无需本地资源 |
| `deepseek-local` | 本地运行的 DeepSeek 模型 | 私有部署，代码生成场景 |
| `deepseek-api` | DeepSeek API 服务 | 快速部署，代码生成场景 |

## 快速开始

### 1. 使用模拟模型（默认）

无需额外配置，适合快速测试：

```bash
# .env 文件
LLM_PROVIDER=mock
```

### 2. 使用 Qwen 本地模型

#### 前置要求
- 已下载 Qwen 模型文件（如 Qwen-7B-Chat）
- 安装了必要的依赖：`transformers`, `torch`
- （可选）CUDA 环境用于 GPU 加速

#### 配置步骤

```bash
# .env 文件
LLM_PROVIDER=qwen-local
QWEN_MODEL_PATH=/path/to/Qwen-7B-Chat
USE_GPU=true  # 如果有 GPU
MAX_GPU_MEMORY=16GB
```

### 3. 使用 Qwen API

#### 前置要求
- 申请通义千问 API Key
- 安装 `openai` 包

#### 配置步骤

```bash
# .env 文件
LLM_PROVIDER=qwen-api
LLM_API_KEY=your_qwen_api_key_here
LLM_API_BASE=https://dashscope.aliyuncs.com/compatible-mode/v1
LLM_MODEL_NAME=qwen-plus  # 或 qwen-turbo, qwen-max
```

### 4. 使用 DeepSeek 本地模型

#### 前置要求
- 已下载 DeepSeek 模型文件（如 deepseek-coder-6.7b-instruct）
- 安装了必要的依赖：`transformers`, `torch`
- （可选）CUDA 环境用于 GPU 加速

#### 配置步骤

```bash
# .env 文件
LLM_PROVIDER=deepseek-local
DEEPSEEK_MODEL_PATH=/path/to/deepseek-coder-6.7b-instruct
DEEPSEEK_MODEL_NAME=deepseek-coder
USE_GPU=true  # 如果有 GPU
MAX_GPU_MEMORY=16GB
```

### 5. 使用 DeepSeek API

#### 前置要求
- 申请 DeepSeek API Key
- 安装 `openai` 包

#### 配置步骤

```bash
# .env 文件
LLM_PROVIDER=deepseek-api
LLM_API_KEY=your_deepseek_api_key_here
LLM_API_BASE=https://api.deepseek.com/v1
LLM_MODEL_NAME=deepseek-chat  # 或 deepseek-coder
```

## 模型生成参数配置

所有模型都支持以下生成参数：

```bash
# 最大生成token数
MAX_TOKENS=2048

# 采样温度 (0-1)，值越高输出越随机
TEMPERATURE=0.7

# Top-p 采样参数 (0-1)
TOP_P=0.9
```

## 完整配置示例

### 示例 1: 使用 Qwen 本地模型

```bash
# LLM配置
LLM_PROVIDER=qwen-local
QWEN_MODEL_PATH=/models/Qwen-7B-Chat

# GPU配置
USE_GPU=true
MAX_GPU_MEMORY=16GB

# 生成参数
MAX_TOKENS=2048
TEMPERATURE=0.7
TOP_P=0.9
```

### 示例 2: 使用 DeepSeek API

```bash
# LLM配置
LLM_PROVIDER=deepseek-api
LLM_API_KEY=sk-xxxxxxxxxxxxxxxxxxxxx
LLM_API_BASE=https://api.deepseek.com/v1
LLM_MODEL_NAME=deepseek-chat

# 生成参数
MAX_TOKENS=2048
TEMPERATURE=0.7
TOP_P=0.9
```

## 模型切换

通过修改 `.env` 文件中的 `LLM_PROVIDER` 环境变量即可轻松切换模型：

```bash
# 从 Mock 切换到 Qwen 本地模型
LLM_PROVIDER=qwen-local

# 从 Qwen 本地模型切换到 DeepSeek API
LLM_PROVIDER=deepseek-api
```

修改后需要重启服务才能生效。

## 依赖安装

### 使用本地模型

```bash
pip install transformers torch
# GPU 支持
pip install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cu118
```

### 使用 API

```bash
pip install openai
```

## 模型选择建议

### Qwen vs DeepSeek

| 特性 | Qwen | DeepSeek |
|------|------|----------|
| **通用对话** | ✅ 优秀 | ✅ 良好 |
| **代码生成** | ✅ 良好 | ✅ 优秀 |
| **测试用例生成** | ✅ 推荐 | ✅ 推荐 |
| **中文支持** | ✅ 优秀 | ✅ 优秀 |
| **模型大小** | 7B-72B | 1.3B-67B |

### 本地部署 vs API

| 考虑因素 | 本地部署 | API |
|---------|---------|-----|
| **数据隐私** | ✅ 高 | ⚠️ 中等 |
| **成本** | 💰 一次性硬件投入 | 💰 按使用量付费 |
| **性能** | 🚀 取决于硬件 | 🚀 稳定可靠 |
| **维护** | 🔧 需要自行维护 | 🔧 无需维护 |
| **可扩展性** | ⚡ 受限于硬件 | ⚡ 弹性扩展 |

## 常见问题

### Q: 如何验证模型配置是否正确？

A: 启动服务后，查看日志输出：

```
INFO: Using LLM provider: qwen-local
INFO: Loading Qwen model from /models/Qwen-7B-Chat
INFO: Qwen model loaded successfully
```

### Q: GPU 内存不足怎么办？

A: 可以尝试以下方法：
1. 使用更小的模型（如 Qwen-7B 而非 Qwen-14B）
2. 设置 `USE_GPU=false` 使用 CPU（速度会变慢）
3. 调整 `MAX_GPU_MEMORY` 参数

### Q: 如何选择合适的模型？

A: 建议：
- **开发测试**: 使用 `mock` 模型
- **生产环境（高隐私要求）**: 使用本地模型
- **生产环境（快速部署）**: 使用 API 服务
- **代码相关测试**: 优先考虑 DeepSeek
- **通用测试用例**: Qwen 和 DeepSeek 都可以

## 代码示例

### 直接使用模型

```python
from models.llm.qwen_model import create_llm_model
from config import ai_config

# 创建模型实例
llm = create_llm_model(
    provider=ai_config.LLM_PROVIDER,
    model_path=ai_config.QWEN_MODEL_PATH,
    api_key=ai_config.LLM_API_KEY,
    api_base=ai_config.LLM_API_BASE
)

# 生成文本
response = llm.generate(
    prompt="请生成一个登录功能的测试用例",
    max_tokens=2048,
    temperature=0.7
)

print(response)
```

### 在 RAG 生成器中使用

模型会自动根据配置初始化：

```python
from models.llm.rag_generator import RAGTestCaseGenerator

# 自动使用 .env 中配置的模型
generator = RAGTestCaseGenerator()

# 生成测试用例
test_cases = generator.generate(
    requirement="用户登录功能",
    project_id=1
)
```

## 更新日志

- 2025-12-03: 添加 Qwen 和 DeepSeek 模型支持
- 2025-12-03: 支持通过环境变量切换模型
- 2025-12-03: 添加本地模型和 API 模型支持
