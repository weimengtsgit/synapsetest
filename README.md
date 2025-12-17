# SynapseTest - AI驱动测试任务管理系统

<div align="center">

**面向中大型企业研发团队的智能化测试管理平台**

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Python](https://img.shields.io/badge/Python-3.9-blue.svg)](https://www.python.org/)
[![Node.js](https://img.shields.io/badge/Node.js-18.x-green.svg)](https://nodejs.org/)
[![Java](https://img.shields.io/badge/Java-11-orange.svg)](https://www.oracle.com/java/)

[文档](docs/) | [快速开始](#快速开始) | [功能特性](#核心功能) | [部署指南](部署说明文档.md)

</div>

---

## 📖 项目概述

SynapseTest 是一款基于人工智能技术的测试任务管理系统，通过智能化调度和全流程优化，为多版本、多场景测试需求提供高效、可靠的自动化测试解决方案。

### 核心价值

- **🚀 效率跃升 75%**：AI智能生成测试用例，大幅减少用例编写时间
- **🎯 智能决策**：基于代码变更和历史数据，智能推荐测试策略
- **🔍 智能去重 40%**：基于语义相似度自动识别重复用例，降低执行冗余
- **📊 质量保障**：实时监控、质量分析和端到端追溯能力
- **🤖 AI赋能**：RAG + LLM技术栈，提供企业级AI测试能力

---

## ✨ 核心功能

### 🎨 工作台
- 📊 数据概览：展示测试用例总数、任务执行情况、成功率等关键指标
- ⚡ 快捷入口：快速访问常用功能
- 📋 待办事项：显示待处理的测试任务和优化建议
- 📈 趋势图表：测试活动趋势、用例增长曲线等可视化

### 🧠 AI智能生成
- **智能用例生成**：基于需求文档AI生成结构化测试用例
- **RAG技术**：检索历史相似用例作为参考，提升生成质量
- **批量生成**：支持批量上传需求文档，快速建立用例库
- **生成历史**：完整追溯所有用例生成活动

**技术方案**：
```
需求文档 → RAG检索(Qdrant) → LLM生成(Qwen/DeepSeek) → 结构化用例
```

### 💡 智能推荐
- **策略推荐**：基于代码变更、历史数据和业务上下文，智能推荐测试策略
- **风险预测**：分析代码变更，预测高风险区域
- **环境推荐**：根据变更类型推荐最优测试环境
- **优先级排序**：多因子智能评分，确保关键用例优先执行

**技术方案**：
```
XGBoost模型 + 规则引擎 → 智能决策
```

### 🔧 用例优化
#### 智能去重
- 使用Sentence-BERT模型计算语义相似度
- 自动识别功能重复的用例
- 相似度阈值可调（默认85%）

#### 优先级排序
多因子加权评分：
- 业务价值 (30%)
- 风险等级 (25%)
- 执行成本 (20%)
- 历史失败率 (15%)
- 覆盖影响 (10%)

#### 质量分析
- 完整性分析：必填字段、步骤详细程度
- 覆盖度分析：功能点覆盖率、场景覆盖率
- 优先级分布：P0/P1/P2/P3合理性分析
- AI质量评分：描述清晰度、结构规范性

### 📋 用例管理
- 用例列表：多维度查询和筛选
- 用例详情：完整的用例信息和执行历史
- 批量操作：批量导出、删除、标记
- 版本管理：支持用例版本控制

### 🎯 任务管理
- 创建任务：关联测试用例，配置执行参数
- 任务列表：查看和管理所有测试任务
- 执行跟踪：实时跟踪任务执行状态
- AI辅助：自动推荐相关用例和测试策略

### 📊 监控分析
- 实时监控：测试执行的实时监控和追踪
- 质量报告：多维度测试质量分析报告
- 趋势分析：质量趋势预测和改进建议
- 端到端追溯：测试用例与需求、缺陷的完整关联

### ⚙️ 配置管理
- 测试版本：管理测试版本信息
- 测试环境：管理测试环境配置
- 用户权限：基于角色的访问控制(RBAC)

---

## 🏗️ 系统架构

### 整体架构

```
┌─────────────┐
│   前端层     │  React 18 + TypeScript + Ant Design Pro
└──────┬──────┘
       │ HTTP/WebSocket
┌──────┴──────┐
│   后端层     │  Spring Boot 3.0 + Spring Cloud
└──────┬──────┘
       │ HTTP/gRPC
┌──────┴──────┐
│  AI服务层   │  Python 3.9 + FastAPI + PyTorch
└──────┬──────┘
       │
┌──────┴──────────────────┐
│    数据存储层            │
├─────────┬────────┬──────┤
│  MySQL  │ Qdrant │ Redis│
└─────────┴────────┴──────┘
```

### 技术栈

#### 前端技术
- **框架**：React 18 + TypeScript
- **UI组件**：Ant Design Pro
- **状态管理**：Redux Toolkit
- **构建工具**：Vite 5.x
- **数据可视化**：ECharts

#### 后端技术
- **核心框架**：Spring Boot 3.0
- **编程语言**：Java 11
- **数据库**：MySQL 8.0
- **缓存**：Redis 7.x
- **构建工具**：Maven 3.8+

#### AI服务技术
| 技术 | 用途 |
|------|------|
| **FastAPI** | Web框架 |
| **PyTorch 2.0** | 深度学习框架 |
| **Transformers** | NLP模型库 |
| **Sentence-BERT** | 语义相似度计算 |
| **XGBoost** | 策略推荐 |
| **Qdrant** | 向量数据库 |
| **Qwen/DeepSeek** | 大语言模型 |

---

## 🎯 AI核心技术

### 1. RAG检索增强生成

```python
需求文本 → 向量化(Sentence-BERT) 
         ↓
    Qdrant语义检索 (Top-K相似用例)
         ↓
    构建Prompt (需求+历史用例+企业规范)
         ↓
    LLM生成 (Qwen-7B-Chat)
         ↓
    结构化用例输出
```

### 2. 语义去重

```python
用例集合 → Sentence-BERT编码(768维)
         ↓
    计算相似度矩阵(Cosine Similarity)
         ↓
    阈值过滤(0.85) + 聚类分组
         ↓
    从每组选择最优用例
```

### 3. 策略推荐

```python
特征提取 (模块复杂度、历史失败率、变更规模...)
         ↓
    XGBoost模型预测
         ↓
    规则引擎调整 (关键模块、热修复...)
         ↓
    策略推荐结果
```

---

## 🚀 快速开始

### 环境要求

| 组件 | 最低配置 | 推荐配置 |
|-----|---------|---------|
| CPU | 4核 | 8核+ |
| 内存 | 8GB | 16GB+ |
| 硬盘 | 50GB | 100GB+ SSD |
| GPU | 无 | NVIDIA GPU 8GB+ (可选) |

### 方式一：Docker Compose 部署（推荐）

```bash
# 1. 克隆项目
git clone https://github.com/your-org/synapsetest.git
cd synapsetest

# 2. 启动所有服务
docker-compose -f docker-compose.mysql-qdrant.yml up -d

# 3. 初始化向量数据库
docker exec synapse-ai-service python scripts/init_vector_db.py

# 4. 访问应用
# 前端: http://localhost
# 后端: http://localhost:8080
# AI服务: http://localhost:8000
# API文档: http://localhost:8000/docs
```

### 方式二：本地开发环境

#### 1. 启动依赖服务

```bash
# MySQL
docker run -d --name mysql-testdb \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=root123 \
  -e MYSQL_DATABASE=synapse_test \
  mysql:8.0

# Redis
docker run -d --name redis-cache \
  -p 6379:6379 \
  redis:7-alpine

# Qdrant
docker run -d --name qdrant-vectordb \
  -p 6333:6333 \
  -p 6334:6334 \
  qdrant/qdrant:v1.7.4
```

#### 2. 启动AI服务

```bash
cd ai-service

# 创建虚拟环境
python3.9 -m venv venv
source venv/bin/activate

# 安装依赖
pip install -r requirements.txt

# 配置环境变量
cp .env.example .env
# 编辑 .env 文件

# 启动服务
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

#### 3. 启动后端服务

```bash
cd backend

# 编译并启动
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

#### 4. 启动前端服务

```bash
cd frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

访问 http://localhost:5173

---

## 💼 应用场景

### 场景1：新项目快速建立用例库
**问题**：新项目启动，需要在短时间内建立完整的测试用例库

**解决方案**：
1. 批量上传需求文档（PRD、用户故事）
2. AI批量生成测试用例
3. 智能去重和质量分析
4. 人工审核后导入用例库

**效果**：1周内完成300+用例的建立（传统方式需要1个月）

### 场景2：版本迭代回归测试
**问题**：版本迭代频繁，需要快速确定回归测试范围

**解决方案**：
1. 输入代码变更信息
2. AI分析风险并推荐测试策略
3. 基于推荐结果创建测试任务
4. 智能排序确保关键用例优先执行

**效果**：测试范围决策时间从2小时缩短到10分钟

### 场景3：测试用例优化
**问题**：用例库经过多次迭代，存在大量重复和低质量用例

**解决方案**：
1. 使用智能去重功能识别重复用例
2. 使用优先级排序功能评估用例价值
3. 使用质量分析功能发现问题
4. 基于AI建议进行优化

**效果**：识别并清理40%的重复用例，用例执行时间减少30%

---

## 📊 价值收益

| 指标 | 传统方式 | SynapseTest | 提升 |
|-----|---------|------------|------|
| 用例编写时间 | 4小时/10用例 | 1小时/10用例 | **75% ↑** |
| 重复用例占比 | 40% | 5% | **87.5% ↓** |
| 测试策略决策时间 | 2小时 | 10分钟 | **91.7% ↓** |
| 缺陷发现率 | 60% | 85% | **41.7% ↑** |
| 测试执行效率 | 基准 | +40% | **40% ↑** |

---

## 📚 文档导航

- **[产品说明文档](产品说明文档.md)** - 详细的产品功能介绍和使用指南
- **[技术说明文档](技术说明文档.md)** - 系统架构、AI技术实现细节
- **[部署说明文档](部署说明文档.md)** - 完整的部署指南和配置说明
- **[产品方案](产品方案.md)** - 产品定位、架构设计和商业价值分析

---

## 🛠️ 配置说明

### AI服务配置

```bash
# ai-service/.env

# 数据库配置
MYSQL_HOST=localhost
MYSQL_PORT=3306
MYSQL_USER=synapse
MYSQL_PASSWORD=synapse123

# 向量数据库
VECTOR_DB_TYPE=qdrant
QDRANT_HOST=localhost
QDRANT_PORT=6333

# LLM配置
LLM_PROVIDER=qwen  # qwen / deepseek / openai
LLM_MODEL_PATH=models/llm/Qwen-7B-Chat
LLM_DEVICE=auto    # auto / cuda / cpu
```

### 后端配置

```yaml
# backend/src/main/resources/application.yml

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/synapse_test
    username: synapse
    password: synapse123
  
  redis:
    host: localhost
    port: 6379

ai-service:
  url: http://localhost:8000
```

---

## 🔍 常见问题

### Q1: AI生成的用例质量如何保证？
A: 系统采用RAG技术，基于企业历史数据生成用例，同时提供人工审核环节。实测质量评分可达B+以上。

### Q2: 支持哪些需求文档格式？
A: 支持纯文本、Markdown、Word（.docx）、PDF等常见格式。

### Q3: 智能去重的准确率如何？
A: 基于Sentence-BERT语义分析，准确率超过90%。用户可调整相似度阈值以适应不同场景。

### Q4: 是否支持私有化部署？
A: 完全支持。系统采用Docker容器化，可一键部署到企业私有云或本地服务器。

### Q5: 需要GPU吗？
A: GPU是可选的。CPU模式可正常运行，但响应速度较慢。推荐使用GPU加速AI推理（NVIDIA GPU 8GB+）。

---

## 🔧 性能优化

### AI服务优化
- 模型量化：使用INT8/4-bit量化减少内存占用
- 批处理：批量处理请求提高吞吐量
- GPU加速：使用CUDA加速推理
- 缓存：Redis缓存生成结果

### 数据库优化
- 索引优化：为高频查询字段添加索引
- 连接池：配置合理的连接池大小
- HNSW索引：Qdrant向量检索加速

---

## 📈 系统监控

### 健康检查

```bash
# 检查所有服务状态
./scripts/health-check.sh

# 预期输出：
# MySQL: ✓ 正常
# Redis: ✓ 正常
# Qdrant: ✓ 正常
# AI服务: ✓ 正常
# 后端服务: ✓ 正常
# 前端服务: ✓ 正常
```

### 资源监控

```bash
# 查看容器资源使用
docker stats

# 查看服务日志
docker-compose logs -f
```

---

## 🤝 贡献指南

欢迎贡献代码、报告问题或提出建议！

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

---

## 📄 开源协议

本项目采用 [MIT License](LICENSE) 开源协议。

---

## 📮 联系我们

- **邮箱**：support@synapsetest.com
- **官网**：https://www.synapsetest.com
- **在线客服**：工作日 9:00-18:00

---

## 🌟 Star History

如果这个项目对您有帮助，请给我们一个 ⭐️ Star！

---

<div align="center">

**SynapseTest - 让AI重新定义软件测试**

Made with ❤️ by SynapseTest Team

</div>
