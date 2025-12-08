# 缓存配置指南

本服务支持两种缓存模式：**Redis 分布式缓存**和**内存缓存**，可通过环境变量灵活切换。

## 缓存模式对比

| 特性 | Redis 缓存 | 内存缓存 |
|------|-----------|---------|
| **适用场景** | 生产环境、多实例部署 | 开发环境、单机部署 |
| **数据持久化** | ✓ 支持 | ✗ 进程重启后丢失 |
| **分布式共享** | ✓ 多实例共享 | ✗ 仅当前进程 |
| **配置复杂度** | 需要 Redis 服务 | 无需额外配置 |
| **性能** | 网络IO，略慢 | 内存访问，极快 |
| **推荐度** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ |

## 配置方式

### 方式一：使用内存缓存（默认）

适合开发环境，无需安装 Redis。

```bash
# .env 文件中设置或不设置（默认为 false）
ENABLE_REDIS=false
```

启动服务后会看到日志：
```
INFO - ℹ Using in-memory cache (Redis disabled by configuration)
```

### 方式二：使用 Redis 缓存（推荐生产环境）

#### 1. 安装并启动 Redis

**macOS:**
```bash
brew install redis
brew services start redis
```

**Linux (Ubuntu/Debian):**
```bash
sudo apt install redis-server
sudo systemctl start redis
```

**Docker:**
```bash
docker run -d --name redis -p 6379:6379 redis:7-alpine
```

#### 2. 配置环境变量

编辑 `.env` 文件：
```bash
# 启用 Redis
ENABLE_REDIS=true

# Redis 连接配置
REDIS_HOST=localhost
REDIS_PORT=6379
# REDIS_PASSWORD=your_password  # 如果设置了密码
REDIS_DB=0
```

#### 3. 启动服务

启动后会看到日志：
```
INFO - ✓ Redis connection established - Using Redis cache
```

## 缓存内容

系统会缓存以下内容以提升性能：

1. **AI 推荐结果**
   - 键格式：`recommendation:{context_hash}`
   - TTL：5分钟（可通过 `CACHE_TTL` 环境变量配置）
   - 作用：避免相同测试任务重复推理

2. **环境状态信息**
   - 键格式：`env_status`
   - 内容：dev/staging/prod 环境的可用性、负载、稳定性
   - 作用：智能推荐最优测试环境

## 缓存配置参数

```bash
# 缓存过期时间（秒），默认 300 秒（5分钟）
CACHE_TTL=300

# 内存缓存最大条目数，默认 1000
CACHE_MAX_SIZE=1000
```

## 故障转移

如果启用了 Redis 但连接失败，系统会**自动降级到内存缓存**：

```
WARNING - Failed to connect to Redis: Connection refused, using in-memory cache
INFO - ℹ Using in-memory cache (Redis connection failed)
```

这确保服务不会因为 Redis 不可用而中断。

## 监控和调试

### 查看 Redis 缓存内容

```bash
# 连接到 Redis
redis-cli

# 查看所有键
keys *

# 查看推荐缓存
keys recommendation:*

# 查看环境状态
get env_status

# 查看键的 TTL
ttl recommendation:abc123...
```

### 清除缓存

```bash
# 清除所有缓存
redis-cli FLUSHDB

# 清除特定键
redis-cli DEL recommendation:abc123...
```

## 最佳实践

1. **开发环境**：使用内存缓存（`ENABLE_REDIS=false`），简化配置
2. **生产环境**：使用 Redis 缓存（`ENABLE_REDIS=true`），支持多实例部署
3. **性能测试**：建议使用 Redis 以保证测试环境与生产一致
4. **缓存时间**：根据业务特点调整 `CACHE_TTL`，推荐值 300-600 秒

## 常见问题

### Q1: 如何验证 Redis 是否正常工作？

查看服务启动日志，寻找：
```
INFO - ✓ Redis connection established - Using Redis cache
```

### Q2: Redis 密码认证如何配置？

在 `.env` 文件中设置：
```bash
REDIS_PASSWORD=your_secure_password
```

### Q3: 缓存命中率如何查看？

未来版本会添加 Prometheus 指标，当前可通过日志观察：
```
INFO - Returning cached recommendation  # 缓存命中
```

### Q4: 如何完全禁用缓存？

目前不支持完全禁用缓存，但可以：
- 设置 `ENABLE_REDIS=false` 使用内存缓存
- 设置 `CACHE_TTL=1` 将缓存时间设为 1 秒（几乎等同于禁用）

## 相关配置文件

- `ai-service/config.py` - 配置类定义
- `ai-service/data/redis_client.py` - Redis 客户端实现
- `ai-service/env.example` - 配置示例模板
