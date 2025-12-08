# 🚀 MySQL + Milvus 快速开始

## 🎯 3分钟快速体验

### 前置条件

- Docker 和 Docker Compose
- Python 3.8+

---

## 📦 步骤 1：一键启动（Docker）

```bash
# 克隆项目
git clone https://github.com/your-repo/synapsetest-qwen.git
cd synapsetest-qwen

# 启动所有服务
docker-compose -f docker-compose.mysql-milvus.yml up -d

# 等待服务启动（约30秒）
docker-compose -f docker-compose.mysql-milvus.yml ps
```

**服务列表**：
- ✅ MySQL (端口 3306)
- ✅ Milvus (端口 19530)
- ✅ AI Service (端口 8001)
- ✅ Backend (端口 8080)

---

## 🧪 步骤 2：验证服务

### 2.1 检查健康状态

```bash
# AI Service 健康检查
curl http://localhost:8001/health/rag

# 预期输出：
{
  "status": "healthy",
  "mysql": {"connected": true, "count": 0},
  "milvus": {"available": true, "count": 0},
  "synchronized": true
}
```

### 2.2 查看服务日志

```bash
# AI Service 日志
docker-compose -f docker-compose.mysql-milvus.yml logs -f ai-service

# Milvus 日志
docker-compose -f docker-compose.mysql-milvus.yml logs -f milvus
```

---

## 🎮 步骤 3：测试功能

### 3.1 生成测试用例（AI Service）

```bash
curl -X POST http://localhost:8001/api/v1/ai/testcase/generate \
  -H "Content-Type: application/json" \
  -d '{
    "requirement_text": "实现用户登录功能，支持用户名密码登录，需要验证码保护",
    "module": "login",
    "num_cases": 5,
    "include_edge_cases": true
  }'
```

**预期输出**：
```json
{
  "success": true,
  "total_generated": 5,
  "total_unique": 5,
  "testcases": [
    {
      "name": "用户登录-正常流程",
      "priority": "P0",
      "steps": [...],
      ...
    }
  ],
  "request_id": "xxxx-xxxx-xxxx"
}
```

### 3.2 测试语义检索

```bash
# 添加测试用例后，测试相似度检索
curl -X POST http://localhost:8001/api/v1/ai/testcase/search \
  -H "Content-Type: application/json" \
  -d '{
    "query_text": "用户账号密码认证",
    "top_k": 3,
    "module": "login"
  }'
```

### 3.3 查询测试用例（Backend Service）

```bash
# Backend 直接查询 MySQL
curl http://localhost:8080/api/testcases?module=login
```

---

## 💻 步骤 4：Python 代码示例

### 4.1 安装依赖

```bash
pip install pymysql pymilvus sentence-transformers
```

### 4.2 使用 RAG Manager

```python
from data.rag_manager import rag_manager

# 1. 添加测试用例（自动同步到 MySQL + Milvus）
testcase = {
    "name": "用户登录-手机号验证码",
    "module": "login",
    "priority": "P0",
    "type": "功能测试",
    "description": "验证用户可以使用手机号和验证码登录",
    "steps": [
        {"step": 1, "action": "输入手机号", "expected": "显示验证码输入框"},
        {"step": 2, "action": "输入验证码", "expected": "验证通过"},
        {"step": 3, "action": "点击登录", "expected": "登录成功"}
    ],
    "tags": ["登录", "手机验证"]
}

testcase_id = rag_manager.add_testcase(testcase)
print(f"✅ 测试用例已添加: {testcase_id}")

# 2. 语义检索（Milvus 向量检索 + MySQL 完整数据）
results = rag_manager.search_similar_testcases(
    query_text="用户使用手机号登录系统",
    top_k=5,
    module_filter="login"
)

print(f"\n找到 {len(results)} 个相似测试用例：")
for r in results:
    print(f"  相似度: {r['similarity_score']:.2%}")
    print(f"  名称: {r['name']}")
    print(f"  优先级: {r['priority']}")
    print("  ---")

# 3. 查看统计信息
stats = rag_manager.get_statistics()
print(f"\n📊 数据库统计:")
print(f"  MySQL: {stats['structured_db']['active_testcases']} 条")
print(f"  Milvus: {stats['vector_db']['total_testcases']} 条")
print(f"  同步状态: {'✅' if stats['synchronized'] else '❌'}")
```

---

## 🔄 步骤 5：数据同步

### 5.1 同步现有数据

如果您已经有测试用例数据：

```python
from data.rag_manager import rag_manager

# 将 MySQL 数据同步到 Milvus
result = rag_manager.sync_databases()

print(f"同步结果:")
print(f"  总数: {result['total_testcases']}")
print(f"  成功: {result['synced']}")
print(f"  失败: {result['failed']}")
```

### 5.2 批量导入

```python
from data.rag_manager import rag_manager

# 批量添加测试用例
testcases = [
    {
        "name": "测试用例1",
        "module": "payment",
        "priority": "P0",
        ...
    },
    {
        "name": "测试用例2",
        "module": "payment",
        "priority": "P1",
        ...
    }
]

result = rag_manager.add_testcases_batch(testcases)
print(f"批量添加完成: {result}")
```

---

## 🎯 步骤 6：集成到现有系统

### 6.1 Backend Service 集成

```java
// Backend Service (Spring Boot)

@Autowired
private TestCaseRepository testCaseRepository;

// 直接查询 MySQL（AI Service 生成的测试用例）
public List<TestCase> getTestCasesByModule(String module) {
    return testCaseRepository.findByModule(module);
}

// 调用 AI Service 生成测试用例
public TestCaseGenerationResult generateTestCases(String requirement) {
    String aiServiceUrl = "http://ai-service:8001/api/v1/ai/testcase/generate";
    
    // 调用 AI Service
    TestCaseGenerationResult result = restTemplate.postForObject(
        aiServiceUrl,
        new GenerationRequest(requirement),
        TestCaseGenerationResult.class
    );
    
    // AI Service 已经保存到 MySQL，直接返回
    return result;
}
```

### 6.2 Frontend 集成

```javascript
// Frontend (React/Vue)

// 1. 调用 Backend 触发 AI 生成
async function generateTestCases(requirement) {
  const response = await fetch('/api/ai/generate', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ requirement_text: requirement })
  });
  return await response.json();
}

// 2. 查询测试用例列表（Backend 从 MySQL 查询）
async function getTestCases(module) {
  const response = await fetch(`/api/testcases?module=${module}`);
  return await response.json();
}

// 3. 完整流程
const result = await generateTestCases("用户注册功能");
console.log(`生成了 ${result.total_unique} 个测试用例`);

// 立即查询（无需等待同步，MySQL 已经有数据）
const testcases = await getTestCases("registration");
console.log(`查询到 ${testcases.length} 个测试用例`);
```

---

## 📊 步骤 7：性能验证

### 7.1 性能测试脚本

```python
import time
from data.rag_manager import rag_manager

# 测试向量检索性能
query_text = "用户登录功能测试"
top_k = 10

start_time = time.time()
results = rag_manager.search_similar_testcases(query_text, top_k)
elapsed = time.time() - start_time

print(f"向量检索性能:")
print(f"  查询时间: {elapsed*1000:.2f}ms")
print(f"  结果数量: {len(results)}")
print(f"  平均响应: {elapsed/len(results)*1000:.2f}ms/条" if results else "N/A")
```

**预期性能**：
- 向量检索：< 10ms (Milvus)
- 数据库查询：< 20ms (MySQL)
- 端到端：< 50ms

---

## 🛠️ 常见问题

### Q1: 如何停止服务？

```bash
docker-compose -f docker-compose.mysql-milvus.yml down
```

### Q2: 如何重启服务？

```bash
docker-compose -f docker-compose.mysql-milvus.yml restart ai-service
```

### Q3: 如何查看 Milvus 数据？

```python
from pymilvus import connections, Collection

connections.connect(host='localhost', port='19530')
collection = Collection("testcases")
collection.load()

print(f"Milvus 向量数量: {collection.num_entities}")
```

### Q4: 如何清空数据重新开始？

```python
from data.rag_manager import rag_manager
from data.milvus_client import milvus_client
from data.mysql_client import mysql_client

# 清空 Milvus
milvus_client.clear_collection()

# 清空 MySQL（谨慎操作！）
# 手动执行 SQL: TRUNCATE TABLE historical_testcases;
```

### Q5: 如何切换回 ChromaDB？

```bash
# 修改环境变量
export VECTOR_DB_TYPE=chromadb

# 重启服务
docker-compose restart ai-service
```

---

## 🎉 下一步

- 📘 查看[完整部署指南](./MYSQL_MILVUS_DEPLOYMENT.md)
- 🏗️ 查看[架构文档](./RAG_DUAL_DATABASE_ARCHITECTURE.md)
- 💡 查看[性能优化指南](./PERFORMANCE_TUNING.md)

---

**🎯 恭喜！您已经成功部署了企业级 RAG 架构！**

架构特点：
- ✅ MySQL + Milvus 双库架构
- ✅ 十亿级向量检索能力
- ✅ 与 Backend 完美集成
- ✅ 一键部署，开箱即用

