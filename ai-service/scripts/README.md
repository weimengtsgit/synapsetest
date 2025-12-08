# 数据初始化脚本使用指南

本目录包含AI智能测试平台的数据初始化脚本,用于为API接口测试预置数据。

---

## ⚠️ 重要更新说明 (2025-12-04)

**脚本已更新以匹配ai-service实际配置!**

| 配置项 | 修正前 (错误) | 修正后 (正确) | 状态 |
|--------|-------------|-------------|------|
| Collection名称 | historical_testcases | **testcases** | ✅ 已修复 |
| 向量维度 | 768 | **384** | ✅ 已修复 |
| Embedding模型 | paraphrase-multilingual-mpnet-base-v2 | **paraphrase-multilingual-MiniLM-L12-v2** | ✅ 已修复 |
| 数据持久化 | 初始化脚本支持 | **ai-service已启用持久化** | ✅ 已修复 |

**🎉 所有配置问题已100%修复!**

**如果您之前运行过旧版脚本,请:**
1. (可选) 删除旧数据: `rm -rf ai-service/data/qdrant_storage/`
2. 运行验证脚本: `python verify_qdrant_config.py`
3. 重新运行: `python init_vector_db_qdrant.py`

---

## 📁 脚本文件

1. **init_mysql.sql** - MySQL数据库初始化脚本
2. **init_vector_db_qdrant.py** - Qdrant向量数据库初始化脚本 (内存模式,推荐用于开发)
3. **init_vector_db_milvus.py** - Milvus向量数据库初始化脚本 (服务器模式,可选)

---

## 🚀 快速开始

### 前置条件

#### 1. MySQL数据库
```bash
# 确保MySQL服务已启动
mysql --version

# 或使用Docker
docker run -d \
  --name mysql \
  -e MYSQL_ROOT_PASSWORD=synapsetest123 \
  -p 3306:3306 \
  mysql:8.0
```

#### 2. Python依赖

```bash
# 安装基础依赖
pip install sentence-transformers

# 选择向量数据库依赖 (二选一)

# 方式1: Qdrant (推荐,用于开发环境)
pip install qdrant-client

# 方式2: Milvus (可选,用于生产环境)
pip install pymilvus
```

---

## 📝 步骤1: 初始化MySQL数据库

### 方式1: 直接执行SQL文件

```bash
# 进入脚本目录
cd ai-service/scripts

# 执行初始化脚本
mysql -u root -p < init_mysql.sql

# 或指定主机和密码
mysql -h localhost -u root -psynapsetest123 < init_mysql.sql
```

### 方式2: 在MySQL客户端中执行

```bash
# 1. 登录MySQL
mysql -u root -psynapsetest123

# 2. 执行脚本
mysql> source /path/to/ai-service/scripts/init_mysql.sql;

# 3. 验证数据
mysql> USE synapsetest;
mysql> SHOW TABLES;
mysql> SELECT COUNT(*) FROM test_cases;
```

### 预期结果

执行成功后应看到:
- ✅ 创建了4张表: `test_cases`, `generation_history`, `recommendation_history`, `user_feedback`
- ✅ 插入了示例数据:
  - test_cases: 10条
  - generation_history: 5条
  - recommendation_history: 3条
  - user_feedback: 3条

---

## 🔮 步骤2: 初始化向量数据库

### ⚠️ 重要配置说明

**在执行初始化前,请先了解以下关键配置差异:**

1. **Collection名称**:
   - ai-service实际使用: `testcases`
   - 初始化脚本已对齐: `testcases`

2. **向量维度与模型**:
   - ai-service实际使用: 384维 + `paraphrase-multilingual-MiniLM-L12-v2`
   - 初始化脚本已对齐: 384维 + `paraphrase-multilingual-MiniLM-L12-v2`

3. **✅ 数据持久化状态**:
   - **ai-service已启用持久化** (`QdrantSDK(path=storage_path)`)
   - **数据在ai-service重启后会保留!** ✅
   - 存储路径: `ai-service/data/qdrant_storage/`
   - 初始化脚本和ai-service使用相同存储路径 ✅

---

### ✅ Qdrant数据持久化已启用

**状态**: ✅ 已完成 (2025-12-04)

ai-service已修改为启用持久化模式,无需再手动修改。当前配置:

```python
# ai-service/data/qdrant_client.py (第74-80行)
if ai_config.QDRANT_MODE == 'memory':
    logger.info("Initializing Qdrant in memory mode with persistence")
    from pathlib import Path
    storage_path = Path(__file__).parent.parent / "data" / "qdrant_storage"
    storage_path.mkdir(parents=True, exist_ok=True)
    self._client = QdrantSDK(path=str(storage_path))
    logger.info(f"Qdrant storage path: {storage_path}")
```

**当前优势**:
- ✅ 数据持久化,重启后仍然存在
- ✅ 无需外部Qdrant服务器
- ✅ 初始化脚本和ai-service使用相同存储路径
- ✅ 开发和生产环境数据一致

---

### 🎯 方式1: Qdrant (推荐,用于开发环境)

**特点**:
- ✅ 内存模式,无需外部服务器
- ✅ 启动快速,使用简单
- ✅ 支持持久化存储
- ✅ 完美适配 `.env` 配置: `VECTOR_DB_TYPE=qdrant`, `QDRANT_MODE=memory`

#### 执行脚本

```bash
# 进入脚本目录
cd ai-service/scripts

# 赋予执行权限
chmod +x init_vector_db_qdrant.py

# 执行脚本
python init_vector_db_qdrant.py

# 或直接运行
./init_vector_db_qdrant.py
```

#### 预期输出

```
============================================================
  检查依赖
============================================================
✓ qdrant-client 版本: 1.x.x
✓ sentence-transformers 已安装

============================================================
  创建Qdrant客户端
============================================================
✓ Qdrant客户端创建成功 (内存模式 + 持久化)
  存储路径: /path/to/ai-service/data/qdrant_storage

============================================================
  创建Collection
============================================================
✓ 成功创建Collection: testcases
  向量维度: 384
  距离度量: COSINE

============================================================
  加载Embedding模型
============================================================
正在加载模型: paraphrase-multilingual-MiniLM-L12-v2
✓ 模型加载成功

============================================================
  生成Embeddings
============================================================
正在生成 15 条文本的embeddings...
100%|████████████████████| 15/15 [00:01<00:00,  8.45it/s]
✓ Embeddings生成成功
  数量: 15
  维度: 384
  耗时: 1.78秒

============================================================
  插入数据
============================================================
正在插入 15 条数据...
✓ 数据插入成功
  插入数量: 15

============================================================
  验证数据
============================================================
✓ Collection统计:
  总记录数: 15
  向量维度: 384

  示例数据 (前3条):
    TC001: 手机号+验证码正常登录 [用户认证] (P0)
    TC002: 微信第三方登录 [用户认证] (P0)
    TC003: 支付宝支付成功 [支付系统] (P0)

============================================================
  测试搜索功能
============================================================
测试查询: '用户登录功能测试'
✓ 搜索成功,返回top 3结果:
  1. 手机号+验证码正常登录 [用户认证]
     相似度: 0.8234, ID: TC001
  2. 登录失败3次锁定 [用户认证]
     相似度: 0.7891, ID: TC010
  3. 微信第三方登录 [用户认证]
     相似度: 0.7456, ID: TC002

============================================================
  初始化完成
============================================================

✅ Qdrant向量数据库初始化成功!
```

#### 数据持久化

数据会自动保存到: `ai-service/data/qdrant_storage/`
- 重启服务后数据仍然存在
- 如需清空数据,删除该目录即可

---

### 🎯 方式2: Milvus (可选,用于生产环境)

**特点**:
- ✅ 高性能,适合大规模数据
- ✅ 支持分布式部署
- ⚠️ 需要运行外部Milvus服务器

#### 前置条件: 启动Milvus

```bash
# 使用Docker Compose启动Milvus
# 参考: https://milvus.io/docs/install_standalone-docker.md

# 下载docker-compose配置
wget https://github.com/milvus-io/milvus/releases/download/v2.3.0/milvus-standalone-docker-compose.yml -O docker-compose.yml

# 启动Milvus
docker-compose up -d

# 检查Milvus是否运行
docker ps | grep milvus
```

#### 执行脚本

```bash
# 进入脚本目录
cd ai-service/scripts

# 执行脚本
python init_vector_db_milvus.py
```

#### 修改 .env 配置

如果使用Milvus,需要修改 `ai-service/.env`:
```bash
# 修改向量数据库类型
VECTOR_DB_TYPE=milvus

# 配置Milvus连接
MILVUS_HOST=localhost
MILVUS_PORT=19530
```

---

## ✅ 验证数据初始化成功

### 验证MySQL

```bash
mysql -u root -psynapsetest123 -e "
USE synapsetest;
SELECT '=== 表统计 ===' AS info;
SELECT
  'test_cases' AS table_name, COUNT(*) AS count FROM test_cases
UNION ALL
SELECT
  'generation_history' AS table_name, COUNT(*) AS count FROM generation_history
UNION ALL
SELECT
  'recommendation_history' AS table_name, COUNT(*) AS count FROM recommendation_history
UNION ALL
SELECT
  'user_feedback' AS table_name, COUNT(*) AS count FROM user_feedback;
"
```

预期输出:
```
+------------------------+-------+
| table_name             | count |
+------------------------+-------+
| test_cases             |    10 |
| generation_history     |     5 |
| recommendation_history |     3 |
| user_feedback          |     3 |
+------------------------+-------+
```

### 验证Qdrant

```python
# test_qdrant.py
from qdrant_client import QdrantClient
from pathlib import Path

# 连接 (使用持久化路径)
storage_path = Path(__file__).parent.parent / "data" / "qdrant_storage"
client = QdrantClient(path=str(storage_path))

# 获取Collection信息
collection_info = client.get_collection("testcases")  # ⚠️ 使用正确的collection名称
print(f"总记录数: {collection_info.points_count}")
print(f"向量维度: {collection_info.config.params.vectors.size}")

# 查询示例
results = client.scroll(
    collection_name="testcases",  # ⚠️ 使用正确的collection名称
    limit=5,
    with_payload=True
)

print(f"\nP0优先级用例:")
for point in results[0]:
    if point.payload['priority'] == 'P0':
        print(f"  {point.payload['test_case_id']}: {point.payload['name']}")
```

执行:
```bash
python test_qdrant.py
```

### 验证Milvus (如果使用)

```python
# test_milvus.py
from pymilvus import connections, Collection

# 连接
connections.connect("default", host="localhost", port="19530")

# 获取Collection
collection = Collection("historical_testcases")

# 统计
print(f"总记录数: {collection.num_entities}")

# 查询示例
results = collection.query(
    expr="priority == 'P0'",
    output_fields=["id", "name", "module"]
)

print(f"\nP0优先级用例数量: {len(results)}")
for r in results[:3]:
    print(f"  {r['id']}: {r['name']}")
```

---

## 🔧 常见问题

### 问题1: MySQL连接失败

**错误**: `ERROR 2002 (HY000): Can't connect to local MySQL server`

**解决**:
```bash
# 检查MySQL是否运行
systemctl status mysql
# 或
brew services list | grep mysql

# 启动MySQL
systemctl start mysql
# 或
brew services start mysql
```

### 问题2: Qdrant数据丢失

**原因**: 使用了纯内存模式 (`:memory:`)

**解决**:
```python
# 在脚本中使用持久化路径
QDRANT_STORAGE_PATH = Path(__file__).parent.parent / "data" / "qdrant_storage"
client = QdrantClient(path=str(QDRANT_STORAGE_PATH))
```

已在脚本中默认启用持久化,数据不会丢失。

### 问题3: Milvus连接失败

**错误**: `MilvusException: (code=1, message=Fail connecting to server)`

**解决**:
```bash
# 检查Milvus是否运行
docker ps | grep milvus

# 启动Milvus (使用docker-compose)
docker-compose up -d

# 查看日志
docker logs milvus-standalone
```

### 问题4: Sentence-BERT模型下载慢

**错误**: 模型下载超时或速度很慢

**解决**:
```bash
# 方式1: 使用国内镜像
export HF_ENDPOINT=https://hf-mirror.com
python init_vector_db_qdrant.py

# 方式2: 手动下载模型
# 1. 访问 https://huggingface.co/sentence-transformers/paraphrase-multilingual-mpnet-base-v2
# 2. 下载模型文件到本地目录
# 3. 修改脚本中的EMBEDDING_MODEL路径为本地路径
```

### 问题5: qdrant-client版本不兼容

**错误**: `AttributeError: module 'qdrant_client' has no attribute 'xxx'`

**解决**:
```bash
# 更新qdrant-client到最新版本
pip install --upgrade qdrant-client

# 或安装指定版本
pip install qdrant-client==1.7.0
```

---

## 🧹 清理数据

### 清理MySQL数据

```sql
-- 删除整个数据库
DROP DATABASE IF EXISTS synapsetest;

-- 或只清空表数据
USE synapsetest;
TRUNCATE TABLE test_cases;
TRUNCATE TABLE generation_history;
TRUNCATE TABLE recommendation_history;
TRUNCATE TABLE user_feedback;
```

### 清理Qdrant数据

```bash
# 方式1: 删除持久化目录
rm -rf ai-service/data/qdrant_storage/

# 方式2: 使用Python删除
python -c "
from qdrant_client import QdrantClient
from pathlib import Path
storage_path = Path('ai-service/data/qdrant_storage')
client = QdrantClient(path=str(storage_path))
client.delete_collection('testcases')
print('✓ Collection已删除')
"
```

### 清理Milvus数据

```python
from pymilvus import connections, utility

# 连接
connections.connect("default", host="localhost", port="19530")

# 删除Collection
utility.drop_collection("historical_testcases")

print("✓ Collection已删除")
```

或执行:
```bash
python -c "
from pymilvus import connections, utility
connections.connect('default', host='localhost', port='19530')
utility.drop_collection('historical_testcases')
print('✓ Collection已删除')
"
```

---

## 📊 数据统计

### MySQL数据

| 表名 | 记录数 | 说明 |
|------|--------|------|
| test_cases | 10 | 历史测试用例 |
| generation_history | 5 | 用例生成历史 |
| recommendation_history | 3 | 策略推荐历史 |
| user_feedback | 3 | 用户反馈记录 |

### 向量数据库数据

#### Qdrant

| 项目 | 值 |
|------|-----|
| Collection名称 | testcases (⚠️ 已修正,与ai-service对齐) |
| 记录数 | 15 |
| 向量维度 | 384 (⚠️ 已修正,与ai-service对齐) |
| 距离度量 | COSINE |
| 模型 | paraphrase-multilingual-MiniLM-L12-v2 (⚠️ 已修正) |
| 模式 | 内存模式 + 持久化 (初始化脚本) / 纯内存 (ai-service) |
| 存储路径 | ai-service/data/qdrant_storage |

#### Milvus (可选)

| 项目 | 值 |
|------|-----|
| Collection名称 | historical_testcases |
| 记录数 | 15 |
| 向量维度 | 768 |
| 索引类型 | IVF_FLAT |
| 距离度量 | L2 |

---

## 🎯 下一步

数据初始化完成后,您可以:

1. **启动AI服务**
   ```bash
   cd ai-service
   python main.py
   ```

2. **测试API接口**
   ```bash
   # 测试生成用例接口
   curl -X POST http://localhost:8080/testcase/generate \
     -H "Content-Type: application/json" \
     -d '{
       "requirement_text": "用户登录功能需求",
       "module": "用户认证",
       "num_cases": 5
     }'
   ```

3. **查看测试方案**
   - 打开 `ai-service/docs/API接口测试方案.md`
   - 按照测试方案执行完整测试

4. **使用Postman测试**
   - 导入测试方案中的Postman Collection
   - 配置环境变量
   - 执行测试用例

---

## 📋 向量数据库选择指南

### 何时使用Qdrant (推荐)

✅ **适用场景**:
- 本地开发和测试
- 快速原型验证
- 中小规模数据 (< 100万条)
- 不需要分布式部署

✅ **优点**:
- 无需外部服务器
- 配置简单
- 启动快速
- 支持持久化

### 何时使用Milvus

✅ **适用场景**:
- 生产环境部署
- 大规模数据 (> 100万条)
- 需要高性能检索
- 需要分布式部署

✅ **优点**:
- 性能更强
- 支持GPU加速
- 支持分布式
- 企业级特性完善

---

## 🔄 切换向量数据库

如需从Qdrant切换到Milvus (或反之):

1. **修改 `.env` 配置**
   ```bash
   # 切换到Milvus
   VECTOR_DB_TYPE=milvus
   MILVUS_HOST=localhost
   MILVUS_PORT=19530

   # 切换到Qdrant
   VECTOR_DB_TYPE=qdrant
   QDRANT_MODE=memory
   ```

2. **运行对应的初始化脚本**
   ```bash
   # Milvus
   python init_vector_db_milvus.py

   # Qdrant
   python init_vector_db_qdrant.py
   ```

3. **重启AI服务**
   ```bash
   # 停止服务
   Ctrl+C

   # 重新启动
   python main.py
   ```

---

## 📞 联系支持

如遇到问题:
1. 查看日志: `ai-service/logs/app.log`
2. 查看测试方案: `ai-service/docs/API接口测试方案.md`
3. 检查 `.env` 配置是否正确
4. 提交Issue: https://github.com/your-repo/issues

---

**最后更新**: 2025-12-04
**版本**: v1.1
**变更**: 添加Qdrant支持,推荐使用Qdrant内存模式进行开发
