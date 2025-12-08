# 日志配置说明

## 问题

启动 AI Service 后，在 terminal 控制台看不到 API 请求和响应的日志输出。

## 原因

### 1. Uvicorn 日志系统覆盖

当使用 `uvicorn` 启动 FastAPI 应用时，uvicorn 有自己的日志配置系统，会覆盖 `logging.basicConfig()` 的配置。

**默认行为**:
- Uvicorn 默认只显示 WARNING 及以上级别的日志
- 应用中的 `logger.info()` 不会显示在控制台

### 2. 日志级别配置

即使在 `.env` 中设置了 `LOG_LEVEL=INFO`，如果 uvicorn 没有配置相应的日志级别，仍然看不到 INFO 级别的日志。

### 3. Logger 继承问题

不同模块的 logger (如 `api.testcase`) 可能没有正确继承 root logger 的配置。

## 解决方案

### ✅ 方案 1: 代码配置（已实施）

**main.py** 中的改进：

1. **强制覆盖日志配置**
   ```python
   logging.basicConfig(
       level=getattr(logging, settings.log_level),
       format=log_format,
       datefmt=date_format,
       handlers=handlers,
       force=True  # 强制覆盖已存在的配置
   )
   ```

2. **配置 Uvicorn logger**
   ```python
   # 设置 uvicorn 相关 logger 的级别
   logging.getLogger("uvicorn").setLevel(getattr(logging, settings.log_level))
   logging.getLogger("uvicorn.access").setLevel(getattr(logging, settings.log_level))
   logging.getLogger("uvicorn.error").setLevel(getattr(logging, settings.log_level))
   ```

3. **Uvicorn 启动配置**
   ```python
   uvicorn.run(
       "main:app",
       host="0.0.0.0",
       port=8000,
       reload=True,
       log_level=settings.log_level.lower(),  # 传递日志级别
       log_config=None  # 禁用默认配置
   )
   ```

### ✅ 方案 2: 使用日志配置文件

**创建 logging_config.yaml**（已创建）

**启动命令**:
```bash
# 使用自定义日志配置
uvicorn main:app --log-config logging_config.yaml

# 或指定日志级别
uvicorn main:app --log-level info
```

### ✅ 方案 3: 命令行参数

**最简单的方法** - 启动时指定日志级别：

```bash
# 方法 1: 使用 --log-level
uvicorn main:app --log-level info

# 方法 2: 使用 --log-config
uvicorn main:app --log-config logging_config.yaml

# 方法 3: 使用环境变量
LOG_LEVEL=DEBUG uvicorn main:app
```

## 启动方式对比

### 方式 1: Python 直接运行（推荐用于开发）

```bash
python main.py
```

**优点**:
- ✅ 使用代码中的日志配置
- ✅ 支持 `force=True` 强制覆盖
- ✅ 自动读取 `.env` 中的 `LOG_LEVEL`

**效果**:
```
2025-12-03 10:30:15 - __main__ - INFO - Starting SynapseTest AI Service v1.0.0
2025-12-03 10:30:15 - __main__ - INFO - LLM Provider: qwen-api
2025-12-03 10:30:16 - api.testcase - INFO - [REQUEST] POST /testcase/generate
{
  "requirement_text": "用户登录功能",
  "module": "auth",
  "num_cases": 5
}
2025-12-03 10:30:20 - api.testcase - INFO - [RESPONSE] POST /testcase/generate - Status: SUCCESS, Time: 4.23s
```

---

### 方式 2: Uvicorn 命令行（生产环境）

```bash
# 需要指定日志级别
uvicorn main:app --host 0.0.0.0 --port 8000 --log-level info
```

**优点**:
- ✅ 更灵活的进程管理
- ✅ 支持多 worker
- ✅ 更好的性能

**缺点**:
- ⚠️ 需要显式指定 `--log-level`
- ⚠️ 或使用 `--log-config` 指定配置文件

**效果**:
```
INFO:     Started server process [12345]
INFO:     Waiting for application startup.
2025-12-03 10:30:15 - __main__ - INFO - Starting SynapseTest AI Service v1.0.0
INFO:     Application startup complete.
INFO:     Uvicorn running on http://0.0.0.0:8000 (Press CTRL+C to quit)
2025-12-03 10:30:16 - api.testcase - INFO - [REQUEST] POST /testcase/generate
```

---

### 方式 3: 使用配置文件

```bash
uvicorn main:app --log-config logging_config.yaml
```

**优点**:
- ✅ 最灵活的配置
- ✅ 可以分别配置不同模块的日志级别
- ✅ 支持不同的日志格式

**logging_config.yaml** 示例：
```yaml
loggers:
  api:
    level: INFO      # API 层日志
  services:
    level: INFO      # 服务层日志
  models:
    level: DEBUG     # 模型层显示更详细的日志
```

## 常见问题

### Q1: 为什么有时看不到日志？

**原因**: Uvicorn 默认只显示 WARNING 及以上级别

**解决**:
```bash
# 启动时指定日志级别
uvicorn main:app --log-level info
```

### Q2: 日志输出格式不统一

**原因**: Uvicorn 和应用使用不同的日志格式器

**解决**: 使用统一的日志配置文件 `logging_config.yaml`

### Q3: 某些模块的日志看不到

**原因**: 模块 logger 级别设置不正确

**解决**: 检查并设置特定模块的日志级别
```python
logging.getLogger("api").setLevel(logging.INFO)
logging.getLogger("services").setLevel(logging.INFO)
logging.getLogger("models").setLevel(logging.INFO)
```

### Q4: 日志文件没有生成

**原因**: logs 目录不存在

**解决**: main.py 已经自动创建了
```python
log_dir = Path(__file__).parent / 'logs'
log_dir.mkdir(exist_ok=True)
```

## 推荐配置

### 开发环境

```bash
# 直接运行，查看详细日志
LOG_LEVEL=DEBUG python main.py
```

### 测试环境

```bash
# 标准日志级别
LOG_LEVEL=INFO uvicorn main:app --log-level info
```

### 生产环境

```bash
# 使用配置文件 + 多 worker
uvicorn main:app \
  --host 0.0.0.0 \
  --port 8000 \
  --workers 4 \
  --log-config logging_config.yaml
```

## 验证日志配置

### 测试 1: 启动日志

启动服务后应该看到：
```
INFO - Starting SynapseTest AI Service v1.0.0
INFO - LLM Provider: qwen-api
INFO - Debug Mode: False
INFO - Log Level: INFO
INFO - Log File: /path/to/logs/ai-service.log
```

### 测试 2: API 请求日志

发送请求：
```bash
curl -X POST "http://localhost:8000/api/v1/ai/testcase/generate" \
  -H "Content-Type: application/json" \
  -d '{
    "requirement_text": "用户登录功能",
    "module": "auth",
    "num_cases": 5
  }'
```

应该看到：
```
INFO - [REQUEST] POST /testcase/generate
{
  "requirement_text": "用户登录功能",
  "module": "auth",
  "num_cases": 5
}
INFO - [RESPONSE] POST /testcase/generate - Status: SUCCESS, Time: 4.23s
```

### 测试 3: 检查日志文件

```bash
# 查看日志文件
tail -f logs/ai-service.log

# 查看访问日志
tail -f logs/access.log
```

## 日志级别说明

| 级别 | 用途 | 输出内容 |
|------|------|---------|
| DEBUG | 开发调试 | 所有详细信息，包括变量值、中间结果 |
| INFO | 正常运行 | 请求/响应、关键操作、状态变化 |
| WARNING | 警告信息 | 非关键错误、降级操作 |
| ERROR | 错误信息 | 异常、失败的操作 |
| CRITICAL | 严重错误 | 系统级错误、服务不可用 |

## 环境变量配置

在 `.env` 中设置：

```env
# 日志级别: DEBUG, INFO, WARNING, ERROR, CRITICAL
LOG_LEVEL=INFO

# 调试模式（显示更多信息）
DEBUG=false
```

## 总结

### 问题
- ❌ 控制台看不到 API 日志
- ❌ Uvicorn 覆盖了应用日志配置

### 解决方案
- ✅ 添加 `force=True` 强制覆盖配置
- ✅ 配置 Uvicorn logger 级别
- ✅ 启动时传递 `log_level` 参数
- ✅ 创建 `logging_config.yaml` 配置文件

### 推荐启动方式

**开发环境**:
```bash
python main.py
```

**生产环境**:
```bash
uvicorn main:app --log-config logging_config.yaml
```

---

更新日期: 2025-12-03
