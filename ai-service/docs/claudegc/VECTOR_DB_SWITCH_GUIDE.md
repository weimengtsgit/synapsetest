# 向量数据库切换指南

本指南介绍如何在 Milvus 和 Qdrant 之间切换向量数据库。

## 概述

项目现在支持两种向量数据库：

| 特性 | Qdrant | Milvus |
|------|--------|--------|
| **Mac兼容性** | ✅ 优秀 (原生ARM64) | ⚠️ 一般 (需要模拟) |
| **部署复杂度** | ✅ 简单 (单容器) | ⚠️ 复杂 (需要etcd+MinIO) |
| **内存模式** | ✅ 支持 | ❌ 不支持 |
| **性能** | ✅ 优秀 | ✅ 优秀 |
| **企业级特性** | ✅ 完整 | ✅ 完整 |
| **推荐场景** | Mac开发、快速部署 | 大规模生产环境 |

## 快速开始

### 方案1: Qdrant (推荐用于Mac)

#### 1. 启动Qdrant服务

```bash
# 使用Podman启动Qdrant
bash start_qdrant.sh

# 或使用Docker Compose
docker-compose -f docker-compose.mysql-qdrant.yml up -d
```

#### 2. 配置环境变量

```bash
export VECTOR_DB_TYPE=qdrant
export QDRANT_HOST=localhost
export QDRANT_PORT=6333
export QDRANT_MODE=server  # 或 'memory' 用于开发
```

#### 3. 安装依赖

```bash
cd ai-service
pip install qdrant-client==1.7.0
```

#### 4. 启动AI服务

```bash
python main.py
```

#### 5. 验证

访问 Qdrant Web UI：http://localhost:6333/dashboard

```bash
# 测试连接
curl http://localhost:6333/healthz
```

### 方案2: Milvus (用于生产环境)

#### 1. 启动Milvus服务

```bash
# 使用Docker Compose (推荐)
docker-compose -f docker-compose.mysql-milvus.yml up -d

# 或使用Podman (Mac可能不稳定)
bash start_milvus.sh
```

#### 2. 配置环境变量

```bash
export VECTOR_DB_TYPE=milvus
export MILVUS_HOST=localhost
export MILVUS_PORT=19530
```

#### 3. 安装依赖

```bash
cd ai-service
pip install pymilvus==2.3.4
```

#### 4. 启动AI服务

```bash
python main.py
```

## 详细配置

### Qdrant 配置选项

```python
# config.py 或环境变量

# 向量数据库类型
VECTOR_DB_TYPE=qdrant

# Qdrant服务器模式
QDRANT_HOST=localhost
QDRANT_PORT=6333
QDRANT_GRPC_PORT=6334
QDRANT_MODE=server  # 'server' 或 'memory'

# 内存模式 (无需启动服务器，用于测试)
QDRANT_MODE=memory
```

### Milvus 配置选项

```python
# config.py 或环境变量

# 向量数据库类型
VECTOR_DB_TYPE=milvus

# Milvus服务器配置
MILVUS_HOST=localhost
MILVUS_PORT=19530
```

## 开发环境配置

### Mac 开发 (推荐Qdrant)

```bash
# .env 文件
VECTOR_DB_TYPE=qdrant
QDRANT_HOST=localhost
QDRANT_PORT=6333
QDRANT_MODE=server

# 或使用内存模式 (无需启动服务)
QDRANT_MODE=memory
```

### Linux/生产环境 (可选Milvus)

```bash
# .env 文件
VECTOR_DB_TYPE=milvus
MILVUS_HOST=milvus-server
MILVUS_PORT=19530
```

## 数据迁移

如果需要从 Milvus 迁移到 Qdrant（或反向）：

### 1. 导出数据

```python
# 使用旧的向量数据库客户端
from data.milvus_client import milvus_client

# 获取所有测试用例
# 这需要从MySQL获取，因为向量数据库只存储embedding
```

### 2. 切换配置

```bash
# 修改 VECTOR_DB_TYPE
export VECTOR_DB_TYPE=qdrant
```

### 3. 重新导入

```python
# 使用新的向量数据库客户端
from data.vector_db_factory import vector_db_client

# 批量导入测试用例
vector_db_client.add_testcases_batch(testcases)
```

### 自动迁移脚本

```bash
# 创建迁移脚本
cd ai-service
python -c "
from data.mysql_client_unified import mysql_client
from data.vector_db_factory import vector_db_client

# 获取所有测试用例
testcases = mysql_client.get_all_testcases()

# 批量导入到新的向量数据库
count = vector_db_client.add_testcases_batch(testcases)
print(f'Migrated {count} test cases')
"
```

## 性能对比

### Qdrant

- ✅ 启动速度快 (~5秒)
- ✅ 内存占用低 (~100MB)
- ✅ Mac上稳定运行
- ✅ 支持内存模式

### Milvus

- ⚠️ 启动速度慢 (~30秒)
- ⚠️ 内存占用高 (~500MB+)
- ⚠️ Mac上可能不稳定
- ✅ 大规模数据性能更好

## 故障排查

### Qdrant 问题

#### 1. 连接失败

```bash
# 检查服务状态
podman ps | grep qdrant
curl http://localhost:6333/healthz

# 查看日志
podman logs qdrant-standalone
```

#### 2. 端口占用

```bash
# 检查端口
lsof -i :6333
lsof -i :6334

# 修改端口配置
export QDRANT_PORT=6335
```

### Milvus 问题

#### 1. Mac上启动失败

**解决方案**：切换到Qdrant

```bash
export VECTOR_DB_TYPE=qdrant
bash start_qdrant.sh
```

#### 2. ARM64兼容性问题

Milvus在Apple Silicon Mac上可能遇到兼容性问题，推荐使用Qdrant。

## API兼容性

两个向量数据库实现了相同的接口，代码无需修改：

```python
from data.vector_db_factory import vector_db_client

# 所有操作保持一致
vector_db_client.add_testcase(testcase_id, testcase_data)
vector_db_client.search_similar(query_text, top_k=5)
vector_db_client.delete_testcase(testcase_id)
```

## 最佳实践

### 开发环境

```bash
# 使用Qdrant内存模式，无需启动服务
export VECTOR_DB_TYPE=qdrant
export QDRANT_MODE=memory
```

### 测试环境

```bash
# 使用Qdrant服务器模式
export VECTOR_DB_TYPE=qdrant
export QDRANT_MODE=server
bash start_qdrant.sh
```

### 生产环境

```bash
# 使用Docker Compose部署
docker-compose -f docker-compose.mysql-qdrant.yml up -d

# 或使用Milvus (如果需要超大规模)
docker-compose -f docker-compose.mysql-milvus.yml up -d
```

## 总结

- **Mac用户**: 强烈推荐使用 **Qdrant**
- **Linux生产环境**: 两者都可以，Qdrant更简单，Milvus更适合超大规模
- **快速开发**: 使用Qdrant的内存模式
- **切换简单**: 只需修改 `VECTOR_DB_TYPE` 环境变量

## 相关资源

- [Qdrant官方文档](https://qdrant.tech/documentation/)
- [Milvus官方文档](https://milvus.io/docs)
- [项目README](../README.md)

