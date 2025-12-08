# 快速开始指南: AI驱动测试任务管理系统

**Date**: 2025-11-10
**Feature**: AI驱动测试任务管理系统
**Branch**: 001-ai-testing-platform

## 1. 系统概述

AI驱动测试任务管理系统是一个智能化的测试管理平台，通过AI技术和智能调度解决测试效率低下、场景单一的问题。系统主要包含以下核心功能：

1. 智能测试任务调度
2. AI生成测试用例
3. 实时测试监控和质量追溯

## 2. 环境准备

### 2.1 系统要求
- 操作系统: Linux (推荐Ubuntu 20.04+) 或 macOS 12+
- 内存: 最少8GB RAM (推荐16GB+)
- 存储: 最少20GB可用空间
- Docker: 20.10+
- Docker Compose: 1.29+
- Node.js: 18+
- Python: 3.9+
- Java: 11+

### 2.2 依赖服务
系统需要以下依赖服务：
- PostgreSQL 13+
- MongoDB 5+
- Redis 6+
- Kafka 3+
- RabbitMQ 3.8+

## 3. 安装和部署

### 3.1 克隆代码库
```bash
git clone <repository-url>
cd synapsetest-qwen
```

### 3.2 配置环境变量
```bash
cp .env.example .env
# 编辑 .env 文件，配置数据库连接、API密钥等信息
```

### 3.3 启动依赖服务
```bash
docker-compose -f docker-compose.dependencies.yml up -d
```

### 3.4 构建和启动应用
```bash
# 构建后端服务
cd backend
./mvnw clean package
cd ..

# 构建前端应用
cd frontend
npm install
npm run build
cd ..

# 启动应用
docker-compose up -d
```

### 3.5 初始化数据库
```bash
# 运行数据库迁移脚本
docker-compose exec backend ./scripts/migrate.sh
```

## 4. 验证安装

### 4.1 检查服务状态
```bash
docker-compose ps
```

应该看到以下服务正在运行：
- backend
- frontend
- postgresql
- mongodb
- redis
- kafka
- rabbitmq

### 4.2 访问Web界面
打开浏览器访问 `http://localhost:3000`，应该能看到登录页面。

### 4.3 API健康检查
```bash
curl http://localhost:8080/health
```

应该返回类似以下的响应：
```json
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "redis": {"status": "UP"}
  }
}
```

## 5. 基本使用流程

### 5.1 创建测试任务
1. 登录系统
2. 导航到"测试任务"页面
3. 点击"创建任务"按钮
4. 填写任务信息（名称、环境、版本等）
5. 系统将自动推荐测试策略
6. 确认或调整推荐策略
7. 点击"启动任务"

### 5.2 使用AI生成测试用例
1. 导航到"测试用例"页面
2. 点击"AI生成"按钮
3. 输入需求文档或自然语言描述
4. 点击"生成"按钮
5. 系统将生成相关测试用例
6. 审核和调整生成的用例
7. 保存用例

### 5.3 查看测试结果
1. 导航到"监控仪表盘"页面
2. 查看实时测试进度和指标
3. 测试完成后，导航到"质量报告"页面
4. 查看详细的测试结果和分析

## 6. 故障排除

### 6.1 服务无法启动
- 检查端口是否被占用
- 检查环境变量配置是否正确
- 查看服务日志：`docker-compose logs <service-name>`

### 6.2 数据库连接失败
- 检查数据库服务是否正常运行
- 检查数据库连接配置是否正确
- 检查网络连接

### 6.3 AI功能无法使用
- 检查AI服务是否正常运行
- 检查模型文件是否存在
- 检查API密钥配置

## 7. 下一步

- 阅读完整的API文档了解所有可用接口
- 配置监控和告警
- 设置用户权限和角色
- 集成CI/CD流水线