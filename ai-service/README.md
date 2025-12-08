# AI Service - SynapseTest

AI驱动的测试任务管理系统 - AI服务模块

## 架构概览

AI Service是SynapseTest系统的核心AI引擎，提供以下功能：

1. **智能测试策略推荐** - 基于XGBoost + 规则引擎的测试策略推荐
2. **AI测试用例生成** - 基于RAG的测试用例自动生成
3. **风险评估** - 代码变更风险预测
4. **测试用例优化** - 语义去重和智能优先级排序

## 技术栈

- **框架**: FastAPI 0.104.1
- **AI模型**:
  - LLM: Qwen-7B-Chat (本地部署) 或 API调用
  - 推荐: XGBoost 2.0.2
  - 语义相似度: Sentence-BERT
- **数据库**:
  - MySQL (后端数据) - 与 Backend 共享
  - Qdrant/Milvus (向量数据库) - 用于语义搜索和 RAG
  - Redis (分布式缓存) - 可选，默认使用内存缓存
- **依赖管理**: requirements.txt

## 项目结构

```
ai-service/
├── api/                    # API路由层
│   ├── recommendation.py   # 推荐相关API
│   └── testcase.py        # 测试用例生成API
├── models/                 # AI模型层
│   ├── recommendation/    # 推荐模型
│   │   ├── strategy_recommender.py  # 策略推荐器
│   │   └── risk_predictor.py        # 风险预测
│   ├── llm/              # LLM模型
│   │   ├── base_model.py          # LLM基类
│   │   ├── qwen_model.py          # Qwen模型封装
│   │   └── rag_generator.py       # RAG生成器
│   └── optimization/     # 优化模型
│       ├── deduplicator.py        # 语义去重
│       └── prioritizer.py         # 优先级排序
├── services/              # 业务服务层
│   ├── recommendation_service.py
│   └── testcase_service.py
├── data/                  # 数据访问层
│   ├── mysql_client.py    # MySQL客户端
│   ├── qdrant_client.py   # Qdrant向量数据库
│   ├── milvus_client.py   # Milvus向量数据库
│   └── redis_client.py    # Redis缓存客户端
├── utils/                 # 工具类
│   ├── feature_extractor.py   # 特征提取
│   └── prompt_builder.py      # Prompt构建
├── config.py             # 配置管理
├── main.py              # 主应用入口
└── requirements.txt     # Python依赖

```

## API端点

### 1. 测试策略推荐

**POST** `/api/v1/ai/recommendation/strategy`

推荐最优测试策略（测试范围、环境、优先级等）

```json
{
  "task_id": "task-123",
  "context": {
    "code_change": {
      "changed_files_count": 5,
      "changed_lines_count": 200,
      "changed_modules": ["user-service"]
    },
    "historical": {
      "recent_pass_rate": 0.95,
      "avg_execution_time": 30
    },
    "business": {
      "business_priority": "P1",
      "module_importance": 0.8
    }
  }
}
```

### 2. AI测试用例生成

**POST** `/api/v1/ai/testcase/generate`

从需求文档生成测试用例

```json
{
  "requirement_text": "用户登录功能需求...",
  "module": "auth",
  "num_cases": 5,
  "include_edge_cases": true,
  "optimization": {
    "deduplicate": true,
    "prioritize": true
  }
}
```

### 3. 测试用例去重

**POST** `/api/v1/ai/testcase/optimize/deduplicate`

使用语义相似度去除重复用例

### 4. 测试用例优先级排序

**POST** `/api/v1/ai/testcase/optimize/prioritize`

基于多因素评分排序测试用例

## 配置说明

### 快速开始（推荐）

使用 `.env` 文件配置（最简单）：

```bash
# 1. 复制配置模板
cp env.example .env

# 2. 编辑配置文件，修改需要的值
vim .env
# 或者使用你喜欢的编辑器
nano .env

# 3. 启动服务（会自动加载 .env 文件）
python main.py
```

**注意**：
- ✅ `.env` 文件会被自动加载（无需手动 export）
- ✅ 如果没有 `.env` 文件，会使用默认值
- ✅ 系统环境变量优先级高于 `.env` 文件

### 环境变量详解

```bash
# LLM配置
LLM_PROVIDER=mock          # mock, local, api
QWEN_MODEL_PATH=/models/Qwen-7B-Chat
LLM_API_KEY=your-api-key
LLM_API_BASE=https://api.example.com

# 模型运行时
USE_GPU=false
MAX_GPU_MEMORY=16GB
MAX_TOKENS=2048
TEMPERATURE=0.7

# Redis缓存配置 (可选)
ENABLE_REDIS=false  # true: 使用Redis缓存, false: 使用内存缓存
REDIS_HOST=localhost
REDIS_PORT=6379
# REDIS_PASSWORD=your_password  # 如果Redis设置了密码
# REDIS_DB=0  # Redis数据库编号

# 应用配置
DEBUG=false
LOG_LEVEL=INFO
CORS_ORIGINS=*
```

## 运行方式

### 开发模式

```bash
cd ai-service

python -m venv .venv

source .venv/bin/activate

# 安装依赖
pip install -r requirements.txt

# 启动服务
python main.py

# 或使用uvicorn
uvicorn main:app --reload --host 0.0.0.0 --port 8000 --log-config logging_config.yaml
```

### 生产模式

```bash
uvicorn main:app --host 0.0.0.0 --port 8000 --workers 4
```

### Docker部署

```bash
docker build -t synapsetest-ai .
docker run -p 8000:8000 synapsetest-ai
```

## LLM Provider模式

### Mock模式（默认，用于开发测试）

```python
LLM_PROVIDER=mock
```

返回预定义的测试用例，无需真实LLM。

### Local模式（本地部署Qwen）

```python
LLM_PROVIDER=local
QWEN_MODEL_PATH=/models/Qwen-7B-Chat
USE_GPU=true
```

需要下载Qwen-7B-Chat模型文件。

### API模式（调用云端API）

```python
LLM_PROVIDER=api
LLM_API_KEY=your-key
LLM_API_BASE=https://dashscope.aliyuncs.com/compatible-mode/v1
```

使用阿里云通义千问API或其他OpenAI兼容API。

## 核心算法

### 1. 测试策略推荐

**算法**: XGBoost分类器 + 规则引擎

**特征**:

- 代码变更特征 (文件数、行数、复杂度)
- 历史特征 (通过率、执行时间、缺陷数)
- 业务特征 (优先级、重要性、紧急度)
- 环境特征 (资源、负载、稳定性)

**输出**:

- test_scope: SMOKE, CORE, FULL
- environment: DEV, STAGING, PROD
- priority: 1-10
- estimated_duration: 分钟
- confidence: 0-1

### 2. RAG测试用例生成

**流程**:

1. 解析需求文档
2. 检索相似历史用例 (RAG)
3. 加载企业测试标准
4. 构建上下文Prompt
5. LLM生成测试用例
6. 解析和验证
7. 语义去重
8. 优先级排序

**去重算法**: Sentence-BERT语义嵌入 + 余弦相似度 (阈值0.85)

**优先级评分**:

- 业务价值 (30%)
- 风险等级 (25%)
- 执行成本 (20%)
- 历史失败率 (15%)
- 覆盖率影响 (10%)

## 测试

```bash
# 运行单元测试
pytest tests/

# 测试覆盖率
pytest --cov=. tests/
```

## 监控和日志

日志输出格式:

```
2025-11-16 10:00:00 - ai_service - INFO - Starting SynapseTest AI Service v1.0.0
2025-11-16 10:00:01 - ai_service - INFO - LLM Provider: mock
2025-11-16 10:00:01 - ai_service - INFO - ✓ Redis connection established - Using Redis cache
```

## 性能优化

1. **缓存策略**: 
   - **Redis缓存** (推荐): 设置 `ENABLE_REDIS=true`，支持分布式缓存，TTL=5分钟
   - **内存缓存** (默认): 设置 `ENABLE_REDIS=false`，单机内存缓存，适合开发环境
   - 缓存内容：AI推荐结果、环境状态等
2. **批处理**: 支持批量生成测试用例
3. **异步处理**: FastAPI异步端点
4. **模型优化**: XGBoost模型量化，推理加速

## 限制和注意事项

1. **LLM输出质量**: 依赖模型质量和Prompt工程
2. **历史数据**: RAG需要足够的历史用例数据存储在向量数据库中
3. **资源需求**: 本地Qwen-7B需要至少16GB显存
4. **向量数据库**: 推荐使用 Qdrant 或 Milvus 用于语义搜索和RAG
5. **Redis缓存**: 可选，设置 `ENABLE_REDIS=false` (默认) 使用内存缓存

## 后续优化方向

1. [ ] 强化学习优化推荐策略
2. [ ] 支持更多LLM模型 (GPT-4, Claude等)
3. [ ] 向量数据库集成 (Milvus)
4. [ ] 测试用例质量评分模型
5. [ ] 多语言需求文档支持
6. [ ] 实时流式生成

## 联系方式

- 项目文档: `/docs`
- API文档: `/docs` (Swagger UI)
- 健康检查: `/health`

---

**版本**: 1.0.0
**最后更新**: 2025-11-16
