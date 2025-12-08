# 补充测试代码说明文档

本文档说明了根据TDD测试方案补充生成的四类测试代码。

## 概述

根据用户要求，补充生成了以下四类测试：

1. **Backend Mapper层测试**（数据访问层单元测试）
2. **Backend Controller集成测试**（API端点集成测试）
3. **Frontend Service层测试**（服务层单元测试）
4. **AI-Service模型单元测试**（独立模型逻辑测试）

---

## 一、Backend Mapper层测试

### 文件列表

```
backend/src/test/java/com/synapsetest/testmanagement/mapper/
├── TestTaskMapperTest.java           # 测试任务Mapper测试（10个测试场景）
├── TestCaseMapperTest.java           # 测试用例Mapper测试（10个测试场景）
└── MonitoringDataMapperTest.java     # 监控数据Mapper测试（10个测试场景）

backend/src/test/resources/
├── test-data-mapper.sql              # Mapper测试数据初始化脚本
└── cleanup.sql                       # 测试数据清理脚本
```

### 测试目标

- 验证MyBatis Mapper的CRUD操作
- 验证SQL映射的正确性
- 验证数据库约束（唯一性、非空等）
- 验证复杂查询逻辑
- 验证JSON字段的序列化/反序列化

### 技术栈

- `@MybatisTest`: 轻量级MyBatis测试，只加载Mapper层
- `@AutoConfigureTestDatabase(replace = NONE)`: 使用真实数据库（H2）
- `@Sql`: 执行SQL脚本初始化测试数据

### 测试场景示例

**TestTaskMapperTest.java**
- 插入测试任务成功
- 根据ID查询测试任务
- 更新测试任务状态
- 根据状态查询任务列表
- 软删除测试任务
- 分页查询测试任务
- 统计各状态任务数量

**TestCaseMapperTest.java**
- 插入测试用例（含JSON字段）
- 查询AI生成的测试用例
- 批量插入测试用例
- 查询高置信度AI用例
- 验证JSON字段序列化/反序列化

**MonitoringDataMapperTest.java**
- 插入监控数据
- 根据时间范围查询监控数据
- 统计平均执行时间
- 批量插入监控数据
- 删除过期监控数据
- 聚合统计（按指标类型分组）

### 运行方式

```bash
cd backend
mvn test -Dtest=TestTaskMapperTest
mvn test -Dtest=TestCaseMapperTest
mvn test -Dtest=MonitoringDataMapperTest
```

---

## 二、Backend Controller集成测试

### 文件列表

```
backend/src/test/java/com/synapsetest/testmanagement/controller/
├── TestTaskControllerIntegrationTest.java      # US1 测试任务API集成测试（9个场景）
├── TestCaseControllerIntegrationTest.java      # US2 测试用例API集成测试（10个场景）
└── MonitoringControllerIntegrationTest.java    # US3 监控API集成测试（12个场景）

backend/src/test/resources/
└── test-data-us3.sql                           # US3 监控测试数据
```

### 测试目标

- 验证HTTP API端点的完整请求-响应流程
- 验证请求参数验证和错误处理
- 验证业务逻辑的端到端执行
- 验证与外部服务（AI服务）的集成
- 验证各种HTTP状态码（200, 201, 400, 404, 503等）

### 技术栈

- `@SpringBootTest(webEnvironment = RANDOM_PORT)`: 启动完整应用上下文
- `TestRestTemplate`: 用于发送HTTP请求
- `@Sql`: 初始化测试数据
- `@ActiveProfiles("mongodb")`: 激活MongoDB配置（US3）

### 测试场景示例

**TestTaskControllerIntegrationTest.java (US1)**
- 创建冒烟测试任务 - 小范围代码变更
- 创建核心功能测试 - 支付流程变更
- 获取任务列表（分页查询）
- 根据ID获取任务详情
- 更新任务状态
- 参数验证 - 缺少必填字段（返回400）
- 获取不存在的任务（返回404）
- 删除测试任务（软删除）
- 获取AI推荐策略

**TestCaseControllerIntegrationTest.java (US2)**
- AI生成测试用例 - 订单模块需求
- AI生成支付模块用例（高优先级）
- 批量保存AI生成的测试用例
- 查询AI生成的用例列表
- 更新测试用例
- 删除测试用例
- 参数验证 - 生成请求缺少必填字段
- 去重功能验证
- 查询高置信度用例

**MonitoringControllerIntegrationTest.java (US3)**
- 获取Dashboard统计概览
- 获取实时任务执行进度
- 获取任务监控数据（时间序列）
- 获取测试通过率趋势
- 获取执行时间统计
- 按模块分组的统计数据
- 获取失败用例详情列表
- MongoDB历史数据查询
- 导出监控数据（CSV格式）
- 实时数据自动刷新（WebSocket/SSE）
- 参数验证 - 无效的时间范围
- 获取不存在任务的监控数据（返回404）

### 运行方式

```bash
cd backend
mvn test -Dtest=TestTaskControllerIntegrationTest
mvn test -Dtest=TestCaseControllerIntegrationTest
mvn test -Dtest=MonitoringControllerIntegrationTest
```

---

## 三、Frontend Service层测试

### 文件列表

```
frontend/src/services/__tests__/
├── testTaskService.test.ts        # US1 测试任务服务测试（7个场景组）
├── testCaseService.test.ts        # US2 测试用例服务测试（7个场景组）
└── monitoringService.test.ts      # US3 监控服务测试（10个场景组）
```

### 测试目标

- 验证Service层的API调用逻辑
- 验证请求参数的构建
- 验证响应数据的处理和转换
- 验证错误处理逻辑
- 验证请求拦截器和响应拦截器

### 技术栈

- `Jest`: 测试框架
- `jest.mock`: Mock axios模块
- `TypeScript`: 类型检查

### 测试场景示例

**testTaskService.test.ts (US1)**
- `createTestTask` - 创建测试任务成功/失败/网络错误
- `getTestTasks` - 获取分页任务列表/无查询参数
- `getTestTaskById` - 获取任务详情/任务不存在（404）
- `updateTaskStatus` - 更新任务状态/无效状态
- `deleteTestTask` - 删除任务/任务不存在
- `getAiRecommendation` - 获取AI推荐/AI服务不可用（503）
- 验证默认请求头设置
- 验证响应数据转换

**testCaseService.test.ts (US2)**
- `generateTestCases` - AI生成测试用例/AI服务超时/启用去重功能
- `batchSaveTestCases` - 批量保存成功/部分失败/空数组
- `getTestCases` - 获取AI生成用例列表/查询高置信度用例
- `getTestCaseById` - 获取用例详情/用例不存在（404）
- `updateTestCase` - 更新用例成功
- `deleteTestCase` - 删除用例/用例不存在
- 错误处理和重试逻辑 - 网络错误自动重试/服务器错误不重试

**monitoringService.test.ts (US3)**
- `getDashboardStats` - 获取Dashboard统计数据/服务不可用（503）
- `getTaskProgress` - 获取实时任务进度/任务已完成
- `getTaskMonitoringData` - 获取24小时监控数据/历史数据（MongoDB）/自定义时间范围
- `getPassRateTrend` - 获取最近7天通过率趋势
- `getExecutionTimeStats` - 获取执行时间统计
- `getStatsByModule` - 按模块获取统计数据
- `getFailedCases` - 获取失败用例详情
- `exportMonitoringData` - 导出CSV/JSON格式
- 实时数据轮询机制 - 启动轮询/任务完成后停止轮询
- 错误处理 - 无效时间范围（400）/任务不存在（404）

### 运行方式

```bash
cd frontend
npm test -- src/services/__tests__/testTaskService.test.ts
npm test -- src/services/__tests__/testCaseService.test.ts
npm test -- src/services/__tests__/monitoringService.test.ts
```

---

## 四、AI-Service模型单元测试

### 文件列表

```
ai-service/tests/models/
├── __init__.py
├── test_strategy_recommender.py      # US1 策略推荐器测试（13个场景）
├── test_semantic_deduplicator.py     # US2 语义去重器测试（14个场景）
├── test_testcase_prioritizer.py      # US2 用例优先级排序器测试（13个场景）
└── test_risk_predictor.py            # US3 风险预测器测试（17个场景）
```

### 测试目标

- 验证模型核心算法逻辑（独立于HTTP层）
- 验证特征提取和向量计算
- 验证规则引擎决策逻辑
- 验证边界情况处理
- 验证模型预测准确性（使用Mock）

### 技术栈

- `pytest`: 测试框架
- `unittest.mock`: Mock机器学习模型和依赖项
- `numpy`: 数值计算验证

### 测试场景示例

**test_strategy_recommender.py (US1 - 测试策略推荐)**
- `TestStrategyRecommender`类测试：
  - 小范围代码变更推荐SMOKE测试
  - 核心模块变更推荐CORE测试
  - 发布版本推荐FULL测试
  - 验证特征提取的准确性
  - 验证规则引擎覆盖ML预测
  - 验证置信度计算逻辑
  - 低置信度预测警告
  - 验证预估时间计算
  - 处理缺失上下文字段
  - 边界情况 - 零代码变更

- `RuleEngine`类测试：
  - Hotfix规则 - 强制SMOKE测试
  - 发布规则 - 强制FULL测试
  - 核心模块规则 - 至少CORE测试
  - 无规则适用 - 保持ML预测
  - 多个规则冲突 - 验证优先级

**test_semantic_deduplicator.py (US2 - 语义去重)**
- `SemanticDeduplicator`类测试：
  - 去除完全相同的测试用例
  - 去除语义相似的测试用例
  - 保留所有不同的测试用例
  - 验证相似度阈值的影响
  - 验证相似度分数计算
  - 验证向量生成（Sentence-BERT）
  - 批量去重性能测试
  - 处理空输入/单个用例/缺失字段
  - 去重后保留元数据
  - 保留置信度更高的用例

- `TestCaseEmbedding`类测试：
  - 从测试用例创建向量表示
  - 验证组合文本生成

**test_testcase_prioritizer.py (US2 - 用例优先级排序)**
- `TestCasePrioritizer`类测试：
  - 按AI置信度排序
  - 按优先级等级排序（CRITICAL > HIGH > MEDIUM > LOW）
  - 按代码覆盖率排序
  - 按风险等级排序（HIGH > MEDIUM > LOW）
  - 多因子综合排序
  - 自定义因子权重
  - 验证优先级分数计算
  - 优先级等级/风险等级转分数
  - 处理空输入/单个用例/缺失字段
  - 相同分数的稳定排序
  - 排序后保留所有元数据
  - Top-N用例选择
  - 按最低分数过滤

- `PriorityFactors`类测试：
  - 默认因子权重
  - 自定义因子权重
  - 因子权重归一化

**test_risk_predictor.py (US3 - 风险预测)**
- `RiskPredictor`类测试：
  - 核心模块变更预测高风险
  - 小范围变更预测低风险
  - 中等变更预测中等风险
  - 识别风险因子（低通过率、最近失败等）
  - 计算风险分数
  - 风险分数组成权重验证
  - 从分数分类风险等级
  - 生成风险缓解建议
  - 低通过率的建议/核心模块的建议
  - 处理缺失上下文字段
  - 边界情况 - 零失败/100%通过率
  - 风险预测置信度
  - 低置信度预测警告
  - 历史数据影响风险预测

- `RiskLevel`枚举测试：
  - 风险等级排序（LOW < MEDIUM < HIGH < CRITICAL）
  - 从字符串创建风险等级

- `RiskFactor`类测试：
  - 创建风险因子
  - 风险因子贡献度计算

### 运行方式

```bash
cd ai-service
pytest tests/models/test_strategy_recommender.py -v
pytest tests/models/test_semantic_deduplicator.py -v
pytest tests/models/test_testcase_prioritizer.py -v
pytest tests/models/test_risk_predictor.py -v

# 运行所有模型测试
pytest tests/models/ -v

# 查看覆盖率
pytest tests/models/ --cov=ai_service/models --cov-report=html
```

---

## 测试覆盖率统计

### Backend测试覆盖率

| 层级 | 文件数 | 测试场景数 | 目标覆盖率 |
|------|--------|-----------|-----------|
| Mapper层 | 3 | 30 | ≥80% |
| Controller层 | 3 | 31 | ≥80% |

### Frontend测试覆盖率

| 层级 | 文件数 | 测试场景组数 | 目标覆盖率 |
|------|--------|-------------|-----------|
| Service层 | 3 | 24 | ≥75% |

### AI-Service测试覆盖率

| 层级 | 文件数 | 测试场景数 | 目标覆盖率 |
|------|--------|-----------|-----------|
| 模型层 | 4 | 57 | ≥70% |

---

## 测试执行顺序建议

### 1. 先运行单元测试（速度快）

```bash
# Backend Mapper层
mvn test -Dtest=*MapperTest

# Frontend Service层
npm test -- src/services/__tests__/

# AI-Service 模型层
pytest ai-service/tests/models/ -v
```

### 2. 再运行集成测试（速度慢）

```bash
# Backend Controller层
mvn test -Dtest=*ControllerIntegrationTest
```

### 3. CI/CD集成

在`.github/workflows/test.yml`或Jenkins Pipeline中添加：

```yaml
# Backend测试
- name: Run Backend Mapper Tests
  run: mvn test -Dtest=*MapperTest

- name: Run Backend Controller Integration Tests
  run: mvn test -Dtest=*ControllerIntegrationTest

# Frontend测试
- name: Run Frontend Service Tests
  run: npm test -- src/services/__tests__/

# AI-Service测试
- name: Run AI-Service Model Tests
  run: pytest ai-service/tests/models/ -v --cov=ai_service/models
```

---

## 测试数据管理

### Backend测试数据

- `test-data-us1.sql`: US1测试任务调度数据
- `test-data-us2.sql`: US2测试用例生成数据
- `test-data-us3.sql`: US3监控可视化数据
- `test-data-mapper.sql`: Mapper层通用测试数据
- `cleanup.sql`: 测试数据清理脚本

**使用方式**：
- 通过`@Sql`注解在测试方法执行前后自动加载/清理数据
- 保证测试的独立性和可重复性

### Frontend测试数据

- 通过`jest.mock`模拟axios响应
- 在每个测试用例中定义Mock数据
- 使用`beforeEach`清除Mock状态

### AI-Service测试数据

- 通过`unittest.mock`模拟机器学习模型
- 使用`pytest.fixture`定义可复用的测试数据
- Mock Sentence-BERT、XGBoost等模型的输出

---

## 测试最佳实践

### 1. 遵循AAA模式

所有测试都遵循 **Arrange-Act-Assert** 模式：
- **Given (Arrange)**: 准备测试数据和Mock
- **When (Act)**: 执行被测试的方法
- **Then (Assert)**: 验证结果

### 2. 独立性和可重复性

- 每个测试方法独立运行
- 使用`@Sql`脚本或Mock确保测试数据一致
- 测试之间不共享状态

### 3. 命名规范

- Backend: `场景X.Y: <场景描述>`
- Frontend: `场景X.Y: <场景描述>`
- AI-Service: `场景X: <场景描述>`

### 4. 覆盖边界情况

所有测试都包含：
- 正常场景
- 异常场景（错误处理）
- 边界情况（空输入、单个元素、极端值）
- 参数验证（缺失字段、无效值）

---

## 对比原有测试

### 已有测试（之前生成）

- Backend Service层单元测试（US1/US2/US3）
- Frontend组件单元测试（US1/US2/US3）
- AI-Service API集成测试（US1/US2/US3）

### 新增测试（本次补充）

- ✅ Backend Mapper层单元测试（数据访问层）
- ✅ Backend Controller集成测试（API端点）
- ✅ Frontend Service层单元测试（服务层）
- ✅ AI-Service模型单元测试（模型逻辑）

### 完整测试金字塔

```
       /\
      /  \  E2E测试 (未实现)
     /----\
    /      \ Integration测试 (Backend Controller + AI-Service API)
   /--------\
  /          \ Unit测试 (Backend Service/Mapper + Frontend Component/Service + AI-Service Model)
 /____________\
```

现在已经覆盖了**单元测试**和**集成测试**两个层级，完整实现了TDD测试金字塔的底层和中层。

---

## 总结

本次补充生成了**4大类**共**13个测试文件**，包含**142个测试场景**：

1. **Backend Mapper层**: 3个文件，30个场景
2. **Backend Controller集成**: 3个文件，31个场景
3. **Frontend Service层**: 3个文件，24个场景组
4. **AI-Service模型层**: 4个文件，57个场景

这些测试全面覆盖了数据访问层、API端点层、服务调用层和AI模型逻辑层，确保代码质量和业务逻辑的正确性。

所有测试均遵循TDD最佳实践，采用Given-When-Then结构，具有良好的可读性和可维护性。
