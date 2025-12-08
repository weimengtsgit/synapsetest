# 快速开始指南

本指南帮助你在本地快速启动 SynapseTest 系统的各个组件。

## 前置要求

- Docker 20.10+
- Python 3.13+ (用于 AI Service)
- Java 11+ (用于 Backend)
- Node.js 18+ (用于 Frontend)
- 至少 8GB 可用内存

## 1️⃣ 启动数据库服务

### MySQL (必需)

```bash
# 启动 MySQL
./start_mysql.sh

# 默认配置：
# - 端口: 3306
# - Root 密码: synapsetest123
# - 数据库: synapsetest
```

**详细文档**: [MYSQL_SETUP.md](./MYSQL_SETUP.md)

### 向量数据库 (二选一)

#### 选项 A: Qdrant (推荐 Mac)

```bash
# 启动 Qdrant
./start_qdrant.sh

# 服务地址:
# - REST API: http://localhost:6333
# - Web UI: http://localhost:6333/dashboard
```

#### 选项 B: Milvus

```bash
# 启动 Milvus
./start_milvus.sh

# 服务地址:
# - gRPC: localhost:19530
# - REST API: http://localhost:9091
```

## 2️⃣ 启动 Backend 服务

```bash
cd backend

# 方式一：使用 Maven
./mvnw spring-boot:run

# 方式二：使用脚本
./run.sh

# 服务地址: http://localhost:8000
# API 文档: http://localhost:8000/swagger-ui.html
```

**配置文件**: `backend/src/main/resources/application.yml`

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/synapsetest
    username: root
    password: synapsetest123
```

## 3️⃣ 启动 AI Service

```bash
cd ai-service

# 1. 创建配置文件
cp env.example .env

# 2. 编辑配置（可选）
vim .env

# 3. 安装依赖
pip install -r requirements.txt

# 4. 启动服务
python main.py

# 服务地址: http://localhost:8080
# API 文档: http://localhost:8080/docs
# 健康检查: http://localhost:8080/health
```

**关键配置** (`.env`):

```bash
# 向量数据库选择
VECTOR_DB_TYPE=qdrant  # 或 milvus

# Qdrant 配置
QDRANT_HOST=localhost
QDRANT_PORT=6333

# MySQL 配置
MYSQL_HOST=localhost
MYSQL_PORT=3306
MYSQL_USER=root
MYSQL_PASSWORD=synapsetest123
MYSQL_DATABASE=synapsetest

# Redis 缓存（可选）
ENABLE_REDIS=false  # 默认使用内存缓存
```

## 4️⃣ 启动 Frontend

```bash
cd frontend

# 1. 安装依赖
npm install

# 2. 启动开发服务器
npm run dev

# 访问地址: http://localhost:5173
```

## 🎯 验证服务状态

### 检查所有服务

```bash
# MySQL
docker ps | grep mysql-synapsetest
mysql -h 127.0.0.1 -P 3306 -uroot -psynapsetest123 -e "SELECT 1"

# Qdrant (如果使用)
curl http://localhost:6333/health

# Backend
curl http://localhost:8000/actuator/health

# AI Service
curl http://localhost:8080/health

# Frontend
curl http://localhost:5173
```

### 查看服务日志

```bash
# MySQL
docker logs -f mysql-synapsetest

# Qdrant
docker logs -f qdrant-standalone

# Backend
tail -f backend/logs/application.log

# AI Service
# 在启动终端查看

# Frontend
# 在启动终端查看
```

## 🚀 完整启动流程示例

```bash
# 1. 克隆项目
git clone <repo-url>
cd synapsetest-qwen

# 2. 启动数据库
./start_mysql.sh
./start_qdrant.sh  # 或 ./start_milvus.sh

# 3. 启动后端服务（在新终端）
cd backend && ./run.sh

# 4. 启动 AI 服务（在新终端）
cd ai-service
cp env.example .env
pip install -r requirements.txt
python main.py

# 5. 启动前端（在新终端）
cd frontend
npm install
npm run dev

# 6. 访问系统
open http://localhost:5173
```

## 🛑 停止服务

```bash
# 停止 MySQL
./stop_mysql.sh

# 停止 Qdrant
docker stop qdrant-standalone

# 停止 Milvus
docker stop milvus-standalone milvus-minio milvus-etcd

# 停止 Backend/AI Service/Frontend
# 在各自终端按 Ctrl+C
```

## 📝 常见问题

### 1. 端口冲突

如果端口已被占用，修改配置：

```bash
# MySQL
export MYSQL_PORT=3307
./start_mysql.sh

# Backend - 修改 application.yml
server:
  port: 8001

# AI Service - 修改 .env 或启动命令
uvicorn main:app --port 8081

# Frontend - 修改 vite.config.ts 或使用
npm run dev -- --port 5174
```

### 2. 数据库连接失败

```bash
# 检查 MySQL 是否启动
docker ps | grep mysql-synapsetest

# 测试连接
mysql -h 127.0.0.1 -P 3306 -uroot -psynapsetest123 -e "SELECT 1"

# 查看日志
docker logs mysql-synapsetest
```

### 3. AI Service 依赖安装失败

```bash
# Python 版本要求: 3.13+
python --version

# 升级 pip
pip install --upgrade pip

# 使用国内镜像加速
pip install -r requirements.txt -i https://pypi.tuna.tsinghua.edu.cn/simple
```

### 4. 向量数据库连接失败

```bash
# 检查 Qdrant
curl http://localhost:6333/health

# 检查 Milvus
docker ps | grep milvus

# AI Service 配置检查
cat ai-service/.env | grep VECTOR_DB_TYPE
```

## 🔧 开发工具

### API 测试

```bash
# Backend API
curl http://localhost:8000/api/test-tasks

# AI Service API
curl http://localhost:8080/api/v1/ai/testcase/generate \
  -X POST \
  -H "Content-Type: application/json" \
  -d '{"requirement_text": "用户登录功能", "num_cases": 3}'
```

### 数据库管理

```bash
# MySQL 命令行
docker exec -it mysql-synapsetest mysql -uroot -psynapsetest123 synapsetest

# 或使用 GUI 工具
# - MySQL Workbench
# - DBeaver
# - DataGrip
```

### 监控和调试

```bash
# Backend Actuator
open http://localhost:8000/actuator

# AI Service Swagger
open http://localhost:8080/docs

# Qdrant Dashboard
open http://localhost:6333/dashboard
```

## 📚 相关文档

- [MySQL 启动指南](./MYSQL_SETUP.md)
- [Backend 文档](./backend/docs/)
- [AI Service 文档](./ai-service/README.md)
- [Frontend 文档](./frontend/README.md)
- [项目架构](./README.md)

## 💡 提示

1. **开发环境推荐配置**
   - 使用 Qdrant (Mac 友好)
   - Redis 禁用（使用内存缓存）
   - LLM Provider 设为 mock

2. **数据持久化**
   - 所有 Docker 数据在 `./volumes/` 目录
   - 删除该目录会清空所有数据

3. **快速重置**
   ```bash
   # 停止所有服务
   docker stop $(docker ps -aq)
   
   # 删除所有数据
   rm -rf ./volumes/
   
   # 重新启动
   ./start_mysql.sh
   ./start_qdrant.sh
   ```

---

**需要帮助？** 查看各组件的详细文档或提交 Issue。

**最后更新**: 2024-12-02
