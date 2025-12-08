# TDD测试代码运行指南

本目录包含根据TDD测试方案生成的测试代码，覆盖三个User Story的核心测试场景。

## 目录结构

```
.
├── backend/
│   └── src/test/java/com/synapsetest/testmanagement/
│       ├── service/
│       │   ├── TestTaskServiceTest.java              # US1: 测试任务服务
│       │   ├── TestCaseServiceTest.java              # US2: 测试用例服务
│       │   └── MonitoringServiceTest.java            # US3: 监控服务
│       └── resources/
│           ├── test-data-us1.sql                      # US1测试数据
│           └── test-data-us2.sql                      # US2测试数据
│
├── frontend/
│   └── src/components/
│       ├── test-task/__tests__/
│       │   └── CreateTestTask.test.tsx               # US1: 创建测试任务组件
│       ├── test-case/__tests__/
│       │   └── AITestCaseGeneration.test.tsx         # US2: AI生成用例组件
│       └── monitoring/__tests__/
│           └── Dashboard.test.tsx                     # US3: 监控仪表盘组件
│
└── ai-service/
    └── tests/
        ├── conftest.py                                # pytest配置和fixtures
        ├── test_recommendation_api.py                 # US1: 推荐策略API
        ├── test_testcase_generation.py                # US2: 测试用例生成API
        └── test_risk_prediction.py                    # US3: 风险预测API
```

## 运行测试

### Backend测试 (Java/JUnit 5)

#### 前置条件
- JDK 17+
- Maven 3.8+
- MySQL 8.0+ (测试数据库)

#### 运行全部测试
```bash
cd backend
mvn clean test
```

#### 运行特定测试类
```bash
# US1 - 测试任务服务
mvn test -Dtest=TestTaskServiceTest

# US2 - 测试用例服务
mvn test -Dtest=TestCaseServiceTest

# US3 - 监控服务 (需要MongoDB)
mvn test -Dtest=MonitoringServiceTest -Dspring.profiles.active=mongodb
```

#### 生成测试报告
```bash
mvn test jacoco:report
# 报告位置: target/site/jacoco/index.html
```

### Frontend测试 (TypeScript/Jest)

#### 前置条件
- Node.js 18+
- npm 9+ 或 yarn

#### 安装依赖
```bash
cd frontend
npm install
```

#### 运行全部测试
```bash
npm test
```

#### 运行特定测试文件
```bash
# US1 - 创建测试任务
npm test -- CreateTestTask.test.tsx

# US2 - AI生成测试用例
npm test -- AITestCaseGeneration.test.tsx

# US3 - 监控仪表盘
npm test -- Dashboard.test.tsx
```

#### 生成覆盖率报告
```bash
npm test -- --coverage
# 报告位置: coverage/lcov-report/index.html
```

#### 监听模式（开发时）
```bash
npm test -- --watch
```

### AI-Service测试 (Python/pytest)

#### 前置条件
- Python 3.9+
- pip

#### 安装依赖
```bash
cd ai-service
pip install -r requirements.txt
pip install pytest pytest-asyncio pytest-cov
```

#### 运行全部测试
```bash
pytest tests/
```

#### 运行特定测试文件
```bash
# US1 - 推荐策略API
pytest tests/test_recommendation_api.py

# US2 - 测试用例生成API
pytest tests/test_testcase_generation.py

# US3 - 风险预测API
pytest tests/test_risk_prediction.py
```

#### 生成覆盖率报告
```bash
pytest --cov=. --cov-report=html tests/
# 报告位置: htmlcov/index.html
```

#### 详细输出模式
```bash
pytest -v tests/
```

## TDD开发流程

### Red-Green-Refactor循环

1. **Red阶段 (红灯)**
   ```bash
   # 运行测试，确保测试失败
   npm test         # Frontend
   mvn test         # Backend
   pytest tests/    # AI-Service
   ```

2. **Green阶段 (绿灯)**
   - 编写最小代码使测试通过
   - 再次运行测试确认通过

3. **Refactor阶段 (重构)**
   - 优化代码结构
   - 运行测试确保仍然通过

### 验收标准

每个User Story都有明确的验收标准（参考docs/test/目录下的TDD方案）：

**US1 - 智能测试任务调度**
- ✅ Backend Service层单元测试通过率 ≥80%
- ✅ Frontend组件测试覆盖率 ≥75%
- ✅ AI推荐API返回正确的测试策略

**US2 - AI生成测试用例**
- ✅ AI生成的用例格式完整（标题、步骤、预期结果）
- ✅ 用例去重功能正常工作（相似度>0.85）
- ✅ 前端生成交互流程完整

**US3 - 测试结果可视化分析**
- ✅ 仪表盘数据10秒自动刷新
- ✅ 质量报告包含缺陷统计、风险评估
- ✅ 风险预测返回准确的风险等级

## 持续集成

### GitHub Actions示例

```yaml
# .github/workflows/test.yml
name: Run Tests

on: [push, pull_request]

jobs:
  backend-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
      - name: Run backend tests
        run: cd backend && mvn test

  frontend-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up Node.js
        uses: actions/setup-node@v3
        with:
          node-version: '18'
      - name: Install dependencies
        run: cd frontend && npm ci
      - name: Run frontend tests
        run: cd frontend && npm test -- --coverage

  ai-service-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up Python
        uses: actions/setup-python@v4
        with:
          python-version: '3.9'
      - name: Install dependencies
        run: |
          cd ai-service
          pip install -r requirements.txt
          pip install pytest pytest-cov
      - name: Run AI service tests
        run: cd ai-service && pytest --cov=. tests/
```

## 常见问题

### Backend测试

**Q: MongoDB测试失败怎么办？**
A: 确保使用了`@ActiveProfiles("mongodb")`注解，或者跳过MongoDB相关测试：
```bash
mvn test -Dtest=!*MongoDBTest
```

**Q: 测试数据库配置？**
A: 在`application-test.properties`中配置测试数据库连接。

### Frontend测试

**Q: 组件测试超时？**
A: 增加Jest超时时间：
```javascript
jest.setTimeout(10000);
```

**Q: Mock无效？**
A: 确保在`beforeEach`中清除mock：
```javascript
beforeEach(() => {
  jest.clearAllMocks();
});
```

### AI-Service测试

**Q: FastAPI客户端测试失败？**
A: 确保导入了正确的TestClient：
```python
from fastapi.testclient import TestClient
```

**Q: 异步测试问题？**
A: 使用pytest-asyncio：
```python
@pytest.mark.asyncio
async def test_async_function():
    result = await async_function()
    assert result is not None
```

## 测试覆盖率目标

| 层级 | 目标覆盖率 | 当前进度 |
|------|-----------|---------|
| Backend | ≥80% | 开始 |
| Frontend | ≥75% | 开始 |
| AI-Service | ≥70% | 开始 |

## 参考文档

- [TDD测试方案文档](../../docs/test/)
- [技术方案文档](../../docs/AI驱动测试任务管理系统%20-%20技术方案文档.md)
- [任务规划](../../specs/001-ai-testing-platform/tasks.md)

---

**文档版本**: v1.0
**创建日期**: 2025-11-16
**维护者**: SynapseTest Team
