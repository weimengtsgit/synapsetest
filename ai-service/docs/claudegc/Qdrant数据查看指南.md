# Qdrant数据查看指南

## 方案一:使用Python查看脚本(推荐)

### 使用步骤

1. **停止服务**(因为内存模式不支持并发访问)
```bash
lsof -ti:8080 | xargs kill -9
```

2. **运行查看脚本**
```bash
source .venv/bin/activate
python scripts/view_qdrant_data.py
```

3. **查看完毕后重启服务**
```bash
uvicorn main:app --host 0.0.0.0 --port 8080 --log-config logging_config.yaml
```

### 脚本说明

`scripts/view_qdrant_data.py` 提供以下功能:
- 显示所有集合信息
- 显示向量维度和距离度量
- 显示每个集合的数据条数
- 列出最近10条数据的详细信息
- 计算存储大小

## 方案二:使用Qdrant Web UI(需要切换到Server模式)

### 1. 安装Qdrant Server

**使用Docker**(推荐):
```bash
docker run -d -p 6333:6333 -p 6334:6334 \
    -v $(pwd)/data/qdrant_storage:/qdrant/storage \
    qdrant/qdrant
```

**或使用本地安装**:
```bash
# macOS
brew install qdrant

# 或从源码编译
git clone https://github.com/qdrant/qdrant.git
cd qdrant
cargo build --release
```

### 2. 修改配置文件

编辑 `config.py`:
```python
# 从内存模式改为服务器模式
QDRANT_MODE: str = os.getenv('QDRANT_MODE', 'server')  # 原来是 'memory'
QDRANT_HOST: str = os.getenv('QDRANT_HOST', 'localhost')
QDRANT_PORT: int = int(os.getenv('QDRANT_PORT', '6333'))
```

或在 `.env` 文件中添加:
```bash
QDRANT_MODE=server
QDRANT_HOST=localhost
QDRANT_PORT=6333
```

### 3. 迁移现有数据(可选)

如果想保留现有数据:
```bash
# Qdrant Server会自动读取存储文件夹中的数据
# 只需确保Docker或Qdrant Server指向正确的存储路径
```

### 4. 访问Web UI

启动服务后访问:
- **Dashboard**: http://localhost:6333/dashboard
- **API文档**: http://localhost:6333/docs

### Web UI功能
- ✅ 可视化查看所有集合
- ✅ 浏览向量数据
- ✅ 执行相似度搜索
- ✅ 查看集合统计信息
- ✅ 导入/导出数据
- ✅ 实时性能监控

## 方案三:使用REST API

即使使用内存模式,也可以通过Qdrant客户端的REST API功能查看:

### 查看所有集合
```bash
# 需要在Python中执行
python -c "
from qdrant_client import QdrantClient
client = QdrantClient(path='./data/qdrant_storage')
print(client.get_collections())
"
```

### 查看集合信息
```bash
python -c "
from qdrant_client import QdrantClient
client = QdrantClient(path='./data/qdrant_storage')
print(client.get_collection('testcases'))
"
```

### 查看数据点
```bash
python -c "
from qdrant_client import QdrantClient
client = QdrantClient(path='./data/qdrant_storage')
points, offset = client.scroll('testcases', limit=10, with_payload=True)
for p in points:
    print(f'ID: {p.id}')
    print(f'Payload: {p.payload}')
    print('---')
"
```

## 方案四:添加API端点查看数据

我可以在服务中添加一个查看端点:

### 添加到 `api/testcase.py`:
```python
@router.get("/qdrant/collections")
async def list_qdrant_collections():
    """查看Qdrant集合信息"""
    try:
        from data.vector_db_factory import vector_db_client
        if not hasattr(vector_db_client, '_client') or vector_db_client._client is None:
            return {"error": "Qdrant not available"}

        collections = vector_db_client._client.get_collections().collections
        result = []
        for col in collections:
            info = vector_db_client._client.get_collection(col.name)
            result.append({
                "name": col.name,
                "points_count": info.points_count,
                "vectors_count": info.vectors_count,
                "vector_size": info.config.params.vectors.size
            })
        return {"collections": result}
    except Exception as e:
        return {"error": str(e)}

@router.get("/qdrant/data/{collection_name}")
async def get_qdrant_data(collection_name: str, limit: int = 10):
    """查看Qdrant数据"""
    try:
        from data.vector_db_factory import vector_db_client
        if not hasattr(vector_db_client, '_client') or vector_db_client._client is None:
            return {"error": "Qdrant not available"}

        points, offset = vector_db_client._client.scroll(
            collection_name=collection_name,
            limit=limit,
            with_payload=True,
            with_vectors=False
        )

        return {
            "collection": collection_name,
            "count": len(points),
            "data": [
                {
                    "id": p.id,
                    "payload": p.payload
                }
                for p in points
            ]
        }
    except Exception as e:
        return {"error": str(e)}
```

然后就可以通过HTTP API访问:
```bash
# 查看所有集合
curl http://localhost:8080/api/v1/ai/qdrant/collections

# 查看testcases集合的数据
curl http://localhost:8080/api/v1/ai/qdrant/data/testcases?limit=10
```

## 推荐方案对比

| 方案 | 优点 | 缺点 | 适用场景 |
|------|------|------|----------|
| **Python脚本** | 简单快速,无需额外配置 | 需要停止服务 | 开发调试,快速查看 |
| **Web UI** | 功能完整,可视化好 | 需要Server模式 | 生产环境,长期使用 |
| **REST API** | 灵活,可编程 | 需要编写代码 | 自动化,集成测试 |
| **API端点** | 无需停服,实时查看 | 需要修改代码 | 运维监控 |

## 当前最快速的方法

立即查看当前数据:
```bash
# 1. 停止服务
lsof -ti:8080 | xargs kill -9

# 2. 查看数据
source .venv/bin/activate && python scripts/view_qdrant_data.py

# 3. 重启服务
uvicorn main:app --host 0.0.0.0 --port 8080 --log-config logging_config.yaml &
```

## 长期建议

建议切换到**Qdrant Server模式**,这样可以:
- ✅ 使用Web UI实时查看数据
- ✅ 支持多个客户端并发访问
- ✅ 更好的性能和稳定性
- ✅ 适合生产环境部署
