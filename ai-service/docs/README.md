# AI Service 文档中心

## 📚 文档导航

### 🚀 快速开始
- **[快速开始指南](./MYSQL_MILVUS_QUICKSTART.md)** - 3分钟快速部署和体验

### 📘 完整文档
- **[架构总结](./FINAL_ARCHITECTURE_SUMMARY.md)** - 系统架构和设计说明
- **[部署指南](./MYSQL_MILVUS_DEPLOYMENT.md)** - 生产环境完整部署文档
- **[AI功能模块详细设计方案](./AI功能模块详细设计方案.md)** - AI功能模块的详细设计

### 🎨 产品设计文档（新增）
- **[产品方案](./产品方案.md)** - 完整的产品策划方案（定位、功能、商业模式）
- **[前端UI设计方案](./前端UI设计方案.md)** - 详细的UI设计规范和页面说明
- **[UI原型演示](./UI原型演示.html)** - 可交互的HTML原型（浏览器打开）

### 🧪 测试文档
- **[测试文档](./test/README_TESTING.md)** - 测试相关文档

---

## 🏗️ 系统架构

```
MySQL + Milvus 双库架构
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Frontend (React/Vue)
        ↓
Backend Service (Spring Boot)
        ↓
    MySQL ←────────── AI Service (FastAPI)
        ↑                     ↓
        │              Milvus (向量数据库)
        │              ├─ etcd (元数据)
        │              └─ MinIO (对象存储)
        │                     ↓
        └──────────────  RAG Manager
                             ↓
                   统一的企业级架构

核心特点：
✅ MySQL: 关系数据库（AI + Backend 共享）
✅ Milvus: 企业级向量数据库（十亿级检索）
✅ 统一数据源（无需同步）
✅ 高性能（<10ms 向量检索）
✅ 易部署（Docker Compose 一键启动）
```

---

## 🎯 核心功能

### 1. 智能测试用例生成
- 基于需求文档自动生成测试用例
- RAG（检索增强生成）技术
- 历史用例参考和学习

### 2. 语义相似度检索
- Milvus 向量数据库
- 毫秒级检索（<10ms）
- 支持十亿级向量规模

### 3. 测试策略推荐
- 基于代码变更分析
- 风险评估和优先级排序
- 智能测试范围推荐

---

## 🎨 产品设计文档

### 查看产品方案
打开 **[产品方案.md](./产品方案.md)** 了解：
- 🎯 **产品定位**：AI智能测试平台的核心价值主张
- 👥 **目标用户**：测试工程师、测试经理、开发工程师等
- 🏗️ **功能架构**：用例生成、策略推荐、质量分析等核心模块
- 📊 **商业模式**：定价策略和收入预测
- 🗺️ **产品路线图**：MVP到企业版的演进计划
- 📈 **成功指标**：效率提升70%+的目标

### 体验UI原型
**方式1：直接在浏览器打开**
```bash
# macOS/Linux
open docs/UI原型演示.html

# Windows
start docs/UI原型演示.html
```

**方式2：使用本地服务器**
```bash
cd docs
python3 -m http.server 8080
# 访问 http://localhost:8080/UI原型演示.html
```

### 原型功能亮点
- ✅ **工作台**：数据概览、快速操作、任务跟踪
- ✅ **智能生成**：三步向导、实时进度、结果展示
- ✅ **批量生成**：多需求并行处理
- ✅ **策略推荐**：风险评估、详细建议
- ✅ **用例管理**：搜索、筛选、批量操作
- ✅ **用例优化**：智能去重、优先级排序

### API接口映射
| 前端功能 | API接口 | 文件 |
|---------|---------|------|
| 生成测试用例 | POST /testcase/generate | api/testcase.py:64 |
| 批量生成 | POST /testcase/generate/batch | api/testcase.py:90 |
| 策略推荐 | POST /recommendation/strategy | api/recommendation.py:72 |
| 用例去重 | POST /testcase/optimize/deduplicate | api/testcase.py:142 |
| 用例排序 | POST /testcase/optimize/prioritize | api/testcase.py:169 |

---

## 💻 技术栈

### 后端框架
- **FastAPI** - 高性能 Python Web 框架
- **Uvicorn** - ASGI 服务器

### 数据库
- **MySQL 8.0+** - 关系数据库（与 Backend 共享）
- **Milvus 2.3+** - 企业级向量数据库
- **etcd** - Milvus 元数据存储
- **MinIO** - Milvus 对象存储

### AI/ML
- **Sentence Transformers** - 文本嵌入模型
- **LLM** - 大语言模型（支持多种提供商）

### 其他
- **Redis** - 缓存（可选）
- **Docker** - 容器化部署

---

## 🚀 快速开始

### 使用 Docker Compose（推荐）

```bash
# 1. 启动所有服务
docker-compose -f docker-compose.mysql-milvus.yml up -d

# 2. 验证部署
curl http://localhost:8001/health/rag

# 3. 查看日志
docker-compose -f docker-compose.mysql-milvus.yml logs -f ai-service
```

### 本地开发

```bash
# 1. 安装依赖
pip install -r requirements.txt

# 2. 配置环境变量
export DATABASE_TYPE=mysql
export VECTOR_DB_TYPE=milvus
export MYSQL_URI=mysql://root:password@localhost:3306/synapsetest
export MILVUS_HOST=localhost
export MILVUS_PORT=19530

# 3. 启动服务
uvicorn main:app --reload --port 8001
```

---

## 📖 API 文档

服务启动后访问：
- Swagger UI: http://localhost:8001/docs
- ReDoc: http://localhost:8001/redoc

### 主要 API 端点

#### 测试用例生成
```bash
POST /api/v1/ai/testcase/generate
```

#### 语义检索
```bash
POST /api/v1/ai/testcase/search
```

#### 测试策略推荐
```bash
POST /api/v1/ai/recommendation/strategy
```

#### 健康检查
```bash
GET /health/rag
```

---

## 🔧 配置说明

### 环境变量

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `DATABASE_TYPE` | 数据库类型 | `mysql` |
| `MYSQL_URI` | MySQL 连接字符串 | `mysql://root:password@localhost:3306/synapsetest` |
| `VECTOR_DB_TYPE` | 向量数据库类型 | `milvus` |
| `MILVUS_HOST` | Milvus 主机地址 | `localhost` |
| `MILVUS_PORT` | Milvus 端口 | `19530` |
| `LLM_PROVIDER` | LLM 提供商 | `mock` |

### 数据库配置

#### MySQL
- 字符集：`utf8mb4`
- 排序规则：`utf8mb4_unicode_ci`
- JSON 字段支持：✅

#### Milvus
- 向量维度：384
- 索引类型：`IVF_FLAT`
- 距离度量：`L2`

---

## 📊 性能指标

| 操作 | 性能 |
|------|------|
| 向量检索（100万条） | < 10ms |
| 数据库查询 | < 20ms |
| 端到端响应 | < 50ms |
| 并发支持 | 1000+ QPS |

---

## 🛠️ 开发指南

### 项目结构

```
ai-service/
├── api/               # API 路由
│   ├── testcase.py   # 测试用例相关 API
│   └── recommendation.py  # 推荐相关 API
├── data/             # 数据访问层
│   ├── mysql_client.py    # MySQL 客户端
│   ├── milvus_client.py   # Milvus 客户端
│   ├── db_factory.py      # 数据库工厂
│   ├── vector_db_factory.py  # 向量数据库工厂
│   └── rag_manager.py     # RAG 管理器
├── models/           # AI 模型
│   ├── llm/          # 大语言模型
│   ├── optimization/ # 优化算法
│   └── recommendation/  # 推荐模型
├── services/         # 业务逻辑层
├── utils/            # 工具函数
├── tests/            # 测试文件
├── docs/             # 文档
├── config.py         # 配置文件
├── main.py           # 应用入口
└── requirements.txt  # 依赖列表
```

### 添加新功能

1. 在 `api/` 目录创建新的路由文件
2. 在 `services/` 目录实现业务逻辑
3. 在 `models/` 目录添加 AI 模型（如需要）
4. 更新 `tests/` 添加测试用例
5. 更新文档

---

## 🧪 测试

### 运行测试

```bash
# 运行所有测试
pytest

# 运行特定测试文件
pytest tests/test_rag_manager.py

# 查看覆盖率
pytest --cov=./ --cov-report=html
```

### 测试环境配置

参考 [测试文档](./test/README_TESTING.md)

---

## 📈 监控和运维

### 健康检查

```bash
# AI Service 健康检查
curl http://localhost:8001/health/rag

# 查看统计信息
curl http://localhost:8001/api/v1/ai/stats
```

### 日志

```bash
# Docker 日志
docker-compose logs -f ai-service

# 本地日志
tail -f logs/ai-service.log
```

### 备份

```bash
# MySQL 备份
mysqldump -u root -p synapsetest > backup.sql

# Milvus 数据自动持久化到 MinIO
```

---

## 🤝 贡献指南

1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

---

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](../../LICENSE) 文件了解详情

---

## 📮 联系方式

- 项目地址: https://github.com/your-repo/synapsetest-qwen
- Issue 追踪: https://github.com/your-repo/synapsetest-qwen/issues

---

## 🙏 致谢

- [FastAPI](https://fastapi.tiangolo.com/) - 现代化 Web 框架
- [Milvus](https://milvus.io/) - 开源向量数据库
- [Sentence Transformers](https://www.sbert.net/) - 文本嵌入模型
- [MySQL](https://www.mysql.com/) - 关系数据库

---

**💡 提示**: 首次使用请先阅读 [快速开始指南](./MYSQL_MILVUS_QUICKSTART.md)

