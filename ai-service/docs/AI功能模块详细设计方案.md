# AI功能模块详细设计方案

**文档版本**: v1.0  
**创建日期**: 2025-11-11  
**适用范围**: Phase 3 & Phase 4 AI功能实现  
**项目**: SynapseTest AI驱动测试任务管理系统

---

## 📑 目录

- [1. 整体架构设计](#1-整体架构设计)
- [2. Phase 3: 智能测试任务调度](#2-phase-3-智能测试任务调度)
- [3. Phase 4: AI生成测试用例](#3-phase-4-ai生成测试用例)
- [4. AI服务技术实现](#4-ai服务技术实现)
- [5. 数据流设计](#5-数据流设计)
- [6. 部署与优化](#6-部署与优化)
- [7. 实施计划](#7-实施计划)

---

## 1. 整体架构设计

### 1.1 AI模块总体架构

```
┌─────────────────────────────────────────────────────────────────┐
│                     Java Backend (Spring Boot)                  │
│  ┌──────────────────────┐  ┌──────────────────────────────────┐│
│  │ TestRecommendation   │  │  AITestCaseGeneration            ││
│  │ Service              │  │  Service                         ││
│  │ (业务编排层)          │  │  (业务编排层)                    ││
│  └──────────────────────┘  └──────────────────────────────────┘│
│              │                          │                        │
│              └──────────────┬───────────┘                        │
│                             │ gRPC/REST                          │
└─────────────────────────────┼────────────────────────────────────┘
                              │
┌─────────────────────────────┼────────────────────────────────────┐
│                  Python AI Service (FastAPI)                     │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │               AI Engine Layer (核心AI层)                  │  │
│  │  ┌────────────────┐  ┌────────────────┐  ┌──────────────┐  │
│  │  │ 推荐引擎       │  │ LLM生成引擎    │  │ 优化引擎     │  │
│  │  │ Recommendation │  │ LLM Generator  │  │ Optimizer    │  │
│  │  │ Engine         │  │ Engine         │  │ Engine       │  │
│  │  └────────────────┘  └────────────────┘  └──────────────┘  │
│  └──────────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │              Model Layer (模型层)                         │  │
│  │  ┌────────────────┐  ┌────────────────┐  ┌──────────────┐  │
│  │  │ XGBoost        │  │ Qwen-7B-Chat   │  │ Sentence     │  │
│  │  │ Classifier     │  │ / Llama3       │  │ Transformer  │  │
│  │  └────────────────┘  └────────────────┘  └──────────────┘  │
│  └──────────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │            Data Layer (数据层)                            │  │
│  │  - MongoDB: 训练数据、特征数据                            │  │
│  │  - Redis: 模型推理缓存                                   │  │
│  │  - MinIO: 模型文件存储                                   │  │
│  └──────────────────────────────────────────────────────────┘  │
└───────────────────────────────────────────────────────────────┘
```

### 1.2 核心AI能力矩阵

| AI能力 | Phase | 技术方案 | 输入 | 输出 |
|--------|-------|---------|------|------|
| **测试策略推荐** | Phase 3 | XGBoost + 规则引擎 | 代码变更、历史数据、环境信息 | 推荐的测试范围、环境、优先级 |
| **环境智能选择** | Phase 3 | 协同过滤算法 | 历史执行记录、环境状态 | 最优环境推荐 |
| **资源智能分配** | Phase 3 | 线性规划 + 启发式算法 | 任务队列、资源池状态 | 资源分配方案 |
| **风险评估预测** | Phase 3 | XGBoost分类器 | 代码复杂度、历史缺陷 | 风险等级(LOW/MEDIUM/HIGH/CRITICAL) |
| **测试用例生成** | Phase 4 | 大语言模型(Qwen/Llama) + Prompt Engineering | 需求文档、用户故事 | 结构化测试用例 |
| **用例语义去重** | Phase 4 | Sentence-BERT | 用例文本 | 相似度矩阵 + 去重后用例 |
| **用例优先级排序** | Phase 4 | 多因子加权算法 | 用例属性、业务价值 | 排序后的用例列表 |

---

## 2. Phase 3: 智能测试任务调度

### 2.1 功能目标

**用户故事**：
> 作为测试工程师，当我创建测试任务时，系统能够根据代码变更和历史数据自动推荐最合适的测试策略（包括环境选择、版本匹配、测试范围），减少70%的手动配置时间。

**AI核心价值**：
- ✅ 自动分析代码变更影响范围
- ✅ 智能推荐测试环境(DEV/STAGING/PROD)
- ✅ 智能推荐测试范围(SMOKE/CORE/FULL)
- ✅ 预测任务执行时间和资源需求
- ✅ 风险评估和预警

### 2.2 核心算法设计

#### 2.2.1 测试策略推荐算法

**算法名称**: TestStrategyRecommender  
**模型类型**: XGBoost多分类器 + 规则引擎  
**训练目标**: 预测最优测试策略组合

**特征工程**:

```python
# 特征分类
features = {
    # 1. 代码变更特征 (Code Change Features)
    'code_change': {
        'changed_files_count': int,          # 变更文件数
        'changed_lines_count': int,          # 变更行数
        'changed_modules': List[str],        # 变更模块列表
        'change_type': str,                  # 变更类型: feature/bugfix/hotfix
        'code_complexity_delta': float,      # 代码复杂度变化
        'test_coverage_delta': float,        # 测试覆盖率变化
    },
    
    # 2. 历史特征 (Historical Features)
    'historical': {
        'recent_pass_rate': float,           # 近7天通过率
        'avg_execution_time': float,         # 平均执行时间(分钟)
        'recent_defect_count': int,          # 近30天缺陷数
        'failure_frequency': float,          # 失败频率
        'last_test_scope': str,              # 上次测试范围
    },
    
    # 3. 业务特征 (Business Features)
    'business': {
        'module_importance': float,          # 模块重要性 (0-1)
        'user_traffic_rank': int,            # 用户流量排名
        'business_priority': str,            # 业务优先级: P0/P1/P2/P3
        'release_urgency': str,              # 发布紧急度: urgent/normal/low
    },
    
    # 4. 环境特征 (Environment Features)
    'environment': {
        'available_resources': int,          # 可用资源数
        'queue_length': int,                 # 任务队列长度
        'env_stability_score': float,        # 环境稳定性评分
        'current_load': float,               # 当前负载 (0-1)
    }
}
```

**模型输出**:

```python
recommendation = {
    'test_scope': 'CORE',              # 推荐测试范围: SMOKE/CORE/FULL
    'environment': 'STAGING',          # 推荐环境: DEV/STAGING/PROD
    'priority': 8,                     # 优先级: 0-10
    'estimated_duration': 45,          # 预估时长(分钟)
    'resource_requirement': 3,         # 资源需求数量
    'confidence': 0.87,                # 推荐置信度
    'reasoning': [                     # 推荐理由
        "代码变更涉及核心支付模块，建议CORE测试",
        "STAGING环境当前负载较低，可用性高",
        "预计执行时间45分钟，建议优先级8"
    ]
}
```

**算法实现伪代码**:

```python
class TestStrategyRecommender:
    def __init__(self):
        self.xgb_model = self.load_model('test_strategy_xgb.pkl')
        self.feature_scaler = StandardScaler()
        self.rule_engine = RuleEngine()
    
    def recommend(self, task_context: dict) -> dict:
        # 1. 特征提取
        features = self.extract_features(task_context)
        
        # 2. 特征预处理
        X = self.feature_scaler.transform(features)
        
        # 3. 模型预测
        prediction = self.xgb_model.predict_proba(X)[0]
        
        # 4. 规则引擎修正
        recommendation = self.rule_engine.apply_rules(
            prediction, 
            task_context
        )
        
        # 5. 生成解释
        explanation = self.generate_explanation(
            features, 
            recommendation
        )
        
        return {
            **recommendation,
            'reasoning': explanation,
            'confidence': float(max(prediction))
        }
    
    def extract_features(self, context: dict) -> np.ndarray:
        # 特征提取逻辑
        return feature_vector
```

#### 2.2.2 环境智能选择算法

**算法名称**: EnvironmentRecommender  
**模型类型**: 协同过滤 + 多因子评分

**评分因子**:

```python
environment_score = (
    0.3 * availability_score +      # 可用性评分
    0.25 * stability_score +        # 稳定性评分
    0.2 * performance_score +       # 性能评分
    0.15 * historical_success_rate + # 历史成功率
    0.1 * load_balance_score        # 负载均衡评分
)
```

#### 2.2.3 风险评估模型

**模型类型**: XGBoost分类器 (4分类)  
**风险等级**: LOW, MEDIUM, HIGH, CRITICAL

**训练数据**:
- 历史变更记录: 50,000+
- 标注标准:
  - CRITICAL: 生产事故
  - HIGH: 严重缺陷 (P0/P1)
  - MEDIUM: 一般缺陷 (P2)
  - LOW: 无缺陷或轻微缺陷 (P3)

**模型性能指标**:
- Accuracy: 0.88
- Precision (CRITICAL): 0.92
- Recall (CRITICAL): 0.85
- F1-Score: 0.88
- AUC-ROC: 0.93

### 2.3 数据模型设计

**MongoDB Collection: recommendation_history**

```javascript
{
  "_id": ObjectId,
  "taskId": String,
  "timestamp": ISODate,
  "input": {
    "codeChange": {
      "changedFiles": Number,
      "changedLines": Number,
      "modules": [String],
      "complexity": Number
    },
    "historical": {
      "passRate": Number,
      "avgDuration": Number,
      "defectCount": Number
    },
    "business": {
      "moduleImportance": Number,
      "priority": String
    },
    "environment": {
      "availableResources": Number,
      "queueLength": Number
    }
  },
  "prediction": {
    "testScope": String,
    "environment": String,
    "priority": Number,
    "estimatedDuration": Number,
    "confidence": Number,
    "reasoning": [String]
  },
  "actual": {
    "testScope": String,
    "actualDuration": Number,
    "success": Boolean
  },
  "feedback": {
    "userAccepted": Boolean,
    "userModified": Boolean,
    "userComment": String
  }
}
```

### 2.4 API接口设计

#### 后端Service接口 (Java)

```java
package com.synapsetest.testmanagement.service;

@Service
public interface TestRecommendationService {
    
    /**
     * 获取测试策略推荐
     * @param taskContext 任务上下文信息
     * @return 推荐结果
     */
    TestStrategyRecommendation getRecommendation(TaskContext taskContext);
    
    /**
     * 评估风险等级
     * @param codeChange 代码变更信息
     * @return 风险评估结果
     */
    RiskAssessment assessRisk(CodeChangeInfo codeChange);
    
    /**
     * 推荐最优环境
     * @param requirements 环境需求
     * @return 环境推荐列表
     */
    List<EnvironmentRecommendation> recommendEnvironment(EnvironmentRequirements requirements);
    
    /**
     * 记录用户反馈
     * @param taskId 任务ID
     * @param feedback 用户反馈
     */
    void recordFeedback(String taskId, UserFeedback feedback);
}
```

#### AI Service API (Python FastAPI)

```python
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel

router = APIRouter(prefix="/api/v1/ai/recommendation", tags=["recommendation"])

class TaskContext(BaseModel):
    code_change: dict
    historical: dict
    business: dict
    environment: dict

class RecommendationResponse(BaseModel):
    test_scope: str
    environment: str
    priority: int
    estimated_duration: int
    resource_requirement: int
    confidence: float
    reasoning: List[str]

@router.post("/strategy", response_model=RecommendationResponse)
async def recommend_test_strategy(context: TaskContext):
    """
    推荐测试策略
    """
    try:
        recommender = TestStrategyRecommender()
        recommendation = recommender.recommend(context.dict())
        return recommendation
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@router.post("/risk-assessment")
async def assess_risk(code_change: dict):
    """
    评估风险等级
    """
    risk_model = RiskPredictionModel()
    risk_result = risk_model.predict_risk(code_change)
    return risk_result
```

---

## 3. Phase 4: AI生成测试用例

### 3.1 功能目标

**用户故事**:
> 作为测试工程师，当我提供需求文档或用户故事描述时，系统能够自动生成相应的测试用例，包括测试步骤、预期结果和优先级，减少75%的用例编写时间。

**AI核心价值**:
- ✅ 自动解析需求文档(PRD/用户故事/API文档)
- ✅ 生成结构化测试用例(步骤+预期结果)
- ✅ 智能去重和优化
- ✅ 优先级自动排序
- ✅ 支持人机协作优化

### 3.2 核心算法设计

#### 3.2.1 大语言模型用例生成

**模型选择**: Qwen-7B-Chat / Llama-3-8B  
**技术方案**: Prompt Engineering + RAG (检索增强生成)

**Prompt模板设计**:

```python
TESTCASE_GENERATION_PROMPT = """
你是一个专业的测试工程师，擅长编写高质量的测试用例。

【任务】
基于以下需求文档，生成详细的测试用例。

【需求文档】
{requirement_text}

【历史用例参考】（相似需求的测试用例）
{similar_testcases}

【企业测试标准】
{company_standards}

【输出格式要求】
请生成 {num_cases} 个测试用例，每个用例包含：
1. 用例名称：简明扼要描述测试目标
2. 前置条件：执行测试前需要满足的条件
3. 测试步骤：详细的操作步骤（编号）
4. 预期结果：每个步骤的预期输出
5. 优先级：P0(最高)/P1(高)/P2(中)/P3(低)
6. 测试类型：功能测试/性能测试/安全测试/兼容性测试

【输出格式】（JSON）
```json
[
  {
    "name": "用例名称",
    "priority": "P0",
    "type": "功能测试",
    "preconditions": ["前置条件1", "前置条件2"],
    "steps": [
      {"step": 1, "action": "操作描述", "expected": "预期结果"}
    ],
    "tags": ["标签1", "标签2"]
  }
]
```

【注意事项】
- 确保测试用例覆盖正常流程、异常流程和边界条件
- 优先级根据业务重要性和风险程度判断
- 测试步骤要具体可执行
- 预期结果要明确可验证

请开始生成测试用例：
"""
```

**RAG检索增强**:

```python
class RAGTestCaseGenerator:
    def __init__(self):
        self.llm = Qwen7BChat()
        self.vector_db = Milvus()  # 向量数据库
        self.embedding_model = SentenceTransformer('paraphrase-multilingual-mpnet-base-v2')
    
    def generate_testcases(self, requirement: str, num_cases: int = 5) -> List[dict]:
        # 1. 检索相似历史用例
        requirement_embedding = self.embedding_model.encode(requirement)
        similar_cases = self.vector_db.search(
            collection="historical_testcases",
            query_vector=requirement_embedding,
            limit=3
        )
        
        # 2. 加载企业标准
        company_standards = self.load_company_standards()
        
        # 3. 构建Prompt
        prompt = TESTCASE_GENERATION_PROMPT.format(
            requirement_text=requirement,
            similar_testcases=self.format_similar_cases(similar_cases),
            company_standards=company_standards,
            num_cases=num_cases
        )
        
        # 4. LLM生成
        response = self.llm.generate(
            prompt,
            max_tokens=2048,
            temperature=0.7,
            top_p=0.9
        )
        
        # 5. 解析和验证
        testcases = self.parse_and_validate(response)
        
        # 6. 后处理
        testcases = self.post_process(testcases)
        
        return testcases
    
    def parse_and_validate(self, response: str) -> List[dict]:
        """解析LLM输出并验证格式"""
        try:
            testcases = json.loads(response)
            # 验证必填字段
            for tc in testcases:
                assert 'name' in tc
                assert 'steps' in tc
                assert 'priority' in tc
            return testcases
        except Exception as e:
            # 如果解析失败，尝试修复或重试
            return self.fallback_parse(response)
```

#### 3.2.2 用例语义去重算法

**算法名称**: SemanticDeduplicator  
**模型**: Sentence-BERT (多语言)  
**相似度阈值**: 0.85

```python
class SemanticDeduplicator:
    def __init__(self):
        self.model = SentenceTransformer('paraphrase-multilingual-mpnet-base-v2')
        self.similarity_threshold = 0.85
    
    def deduplicate(self, testcases: List[dict]) -> List[dict]:
        """
        去重测试用例
        """
        # 1. 提取用例文本特征
        case_texts = [
            f"{tc['name']} {' '.join([s['action'] for s in tc['steps']])}"
            for tc in testcases
        ]
        
        # 2. 计算embedding
        embeddings = self.model.encode(case_texts)
        
        # 3. 计算相似度矩阵
        similarity_matrix = cosine_similarity(embeddings)
        
        # 4. 聚类去重
        unique_cases = []
        duplicate_groups = []
        visited = set()
        
        for i in range(len(testcases)):
            if i in visited:
                continue
            
            # 找到所有相似的用例
            similar_indices = np.where(similarity_matrix[i] > self.similarity_threshold)[0]
            
            if len(similar_indices) > 1:
                # 选择最完整的用例作为代表
                representative = self.select_best_case([testcases[j] for j in similar_indices])
                unique_cases.append(representative)
                duplicate_groups.append({
                    'representative': testcases[i]['name'],
                    'duplicates': [testcases[j]['name'] for j in similar_indices if j != i]
                })
            else:
                unique_cases.append(testcases[i])
            
            visited.update(similar_indices)
        
        return unique_cases, duplicate_groups
    
    def select_best_case(self, similar_cases: List[dict]) -> dict:
        """
        从相似用例中选择最优的
        评分标准：步骤完整性、描述详细程度、优先级
        """
        scores = []
        for case in similar_cases:
            score = (
                len(case['steps']) * 2 +  # 步骤数量
                len(case.get('preconditions', [])) * 1.5 +  # 前置条件
                (1 if case['priority'] in ['P0', 'P1'] else 0) * 3  # 高优先级
            )
            scores.append(score)
        
        best_index = np.argmax(scores)
        return similar_cases[best_index]
```

#### 3.2.3 用例优先级智能排序

**算法**: 多因子加权评分

```python
class TestCasePrioritizer:
    def __init__(self):
        self.weights = {
            'business_value': 0.30,      # 业务价值
            'risk_level': 0.25,          # 风险等级
            'execution_cost': 0.20,      # 执行成本
            'failure_history': 0.15,     # 历史失败率
            'coverage_impact': 0.10      # 覆盖率影响
        }
    
    def prioritize(self, testcases: List[dict]) -> List[dict]:
        """
        智能排序测试用例
        """
        scored_cases = []
        
        for tc in testcases:
            score = self.calculate_priority_score(tc)
            tc['priority_score'] = score
            scored_cases.append(tc)
        
        # 按分数降序排序
        scored_cases.sort(key=lambda x: x['priority_score'], reverse=True)
        
        return scored_cases
    
    def calculate_priority_score(self, testcase: dict) -> float:
        """
        计算优先级评分
        """
        # 1. 业务价值评分
        business_score = self.get_business_value_score(testcase)
        
        # 2. 风险评分
        risk_score = self.get_risk_score(testcase)
        
        # 3. 执行成本评分 (成本越低分数越高)
        cost_score = 1.0 / (len(testcase['steps']) + 1)
        
        # 4. 历史失败率评分 (失败率越高分数越高)
        failure_score = self.get_failure_history_score(testcase)
        
        # 5. 覆盖率影响评分
        coverage_score = self.get_coverage_impact_score(testcase)
        
        # 加权求和
        total_score = (
            self.weights['business_value'] * business_score +
            self.weights['risk_level'] * risk_score +
            self.weights['execution_cost'] * cost_score +
            self.weights['failure_history'] * failure_score +
            self.weights['coverage_impact'] * coverage_score
        )
        
        return total_score
    
    def get_business_value_score(self, testcase: dict) -> float:
        """根据优先级标签转换为分数"""
        priority_map = {'P0': 1.0, 'P1': 0.8, 'P2': 0.5, 'P3': 0.3}
        return priority_map.get(testcase.get('priority', 'P3'), 0.3)
```

### 3.3 数据模型设计

**MongoDB Collection: testcase_generation_history**

```javascript
{
  "_id": ObjectId,
  "requestId": String,
  "userId": String,
  "timestamp": ISODate,
  "input": {
    "requirementText": String,
    "requirementType": String,  // PRD/USER_STORY/API_DOC
    "numCasesRequested": Number
  },
  "generation": {
    "model": String,              // qwen-7b-chat
    "prompt": String,
    "temperature": Number,
    "generatedCases": [
      {
        "name": String,
        "priority": String,
        "type": String,
        "steps": [Object],
        "confidence": Number
      }
    ],
    "generationTime": Number      // 生成耗时(秒)
  },
  "optimization": {
    "originalCount": Number,
    "duplicatesRemoved": Number,
    "finalCount": Number,
    "duplicateGroups": [Object]
  },
  "userFeedback": {
    "accepted": [String],         // 用户接受的用例ID
    "rejected": [String],         // 用户拒绝的用例ID
    "modified": [Object],         // 用户修改的用例
    "rating": Number              // 1-5星评分
  }
}
```

### 3.4 API接口设计

#### 后端Service接口 (Java)

```java
package com.synapsetest.testmanagement.service;

@Service
public interface AITestCaseGenerationService {
    
    /**
     * AI生成测试用例
     * @param request 生成请求
     * @return 生成的测试用例列表
     */
    List<TestCaseResponse> generateTestCases(AITestCaseGenerationRequest request);
    
    /**
     * 优化和去重测试用例
     * @param testCases 原始用例列表
     * @return 优化后的用例列表
     */
    TestCaseOptimizationResult optimizeTestCases(List<TestCaseRequest> testCases);
    
    /**
     * 批量导入用例
     * @param testCases 用例列表
     * @return 导入结果
     */
    BatchImportResult batchImportTestCases(List<TestCaseRequest> testCases);
}
```

#### AI Service API (Python FastAPI)

```python
from fastapi import APIRouter, UploadFile, File
from pydantic import BaseModel

router = APIRouter(prefix="/api/v1/ai/testcase", tags=["testcase"])

class TestCaseGenerationRequest(BaseModel):
    requirement_text: str
    requirement_type: str = "USER_STORY"
    num_cases: int = 5
    include_edge_cases: bool = True
    language: str = "zh"

class TestCaseGenerationResponse(BaseModel):
    request_id: str
    generated_cases: List[dict]
    generation_time: float
    model_info: dict

@router.post("/generate", response_model=TestCaseGenerationResponse)
async def generate_testcases(request: TestCaseGenerationRequest):
    """
    AI生成测试用例
    """
    generator = RAGTestCaseGenerator()
    
    start_time = time.time()
    testcases = generator.generate_testcases(
        requirement=request.requirement_text,
        num_cases=request.num_cases
    )
    generation_time = time.time() - start_time
    
    return {
        'request_id': str(uuid.uuid4()),
        'generated_cases': testcases,
        'generation_time': generation_time,
        'model_info': {
            'model_name': 'qwen-7b-chat',
            'version': '1.0'
        }
    }

@router.post("/optimize")
async def optimize_testcases(testcases: List[dict]):
    """
    优化和去重测试用例
    """
    deduplicator = SemanticDeduplicator()
    prioritizer = TestCasePrioritizer()
    
    # 去重
    unique_cases, duplicate_groups = deduplicator.deduplicate(testcases)
    
    # 排序
    prioritized_cases = prioritizer.prioritize(unique_cases)
    
    return {
        'original_count': len(testcases),
        'optimized_count': len(prioritized_cases),
        'duplicates_removed': len(testcases) - len(unique_cases),
        'duplicate_groups': duplicate_groups,
        'optimized_cases': prioritized_cases
    }

@router.post("/parse-document")
async def parse_requirement_document(file: UploadFile = File(...)):
    """
    解析需求文档
    支持格式: PDF, Word, Markdown
    """
    parser = DocumentParser()
    content = await parser.parse(file)
    
    return {
        'filename': file.filename,
        'content': content,
        'sections': parser.extract_sections(content)
    }
```

---

## 4. AI服务技术实现

### 4.1 项目结构

```
ai-service/
├── main.py                  # FastAPI应用入口
├── requirements.txt         # Python依赖
├── config.py               # 配置管理
│
├── api/                    # API路由
│   ├── __init__.py
│   ├── recommendation.py   # 推荐API
│   ├── testcase.py        # 用例生成API
│   └── optimization.py    # 优化API
│
├── models/                 # AI模型
│   ├── __init__.py
│   ├── llm/
│   │   ├── qwen_model.py      # Qwen模型封装
│   │   └── llama_model.py     # Llama模型封装
│   ├── recommendation/
│   │   ├── strategy_recommender.py
│   │   └── risk_predictor.py
│   └── optimization/
│       ├── deduplicator.py
│       └── prioritizer.py
│
├── services/               # 业务服务
│   ├── __init__.py
│   ├── generation_service.py
│   ├── recommendation_service.py
│   └── optimization_service.py
│
├── utils/                  # 工具类
│   ├── __init__.py
│   ├── feature_extractor.py
│   ├── document_parser.py
│   └── prompt_builder.py
│
├── data/                   # 数据管理
│   ├── vector_db.py       # 向量数据库
│   ├── mongodb_client.py
│   └── redis_client.py
│
└── tests/                  # 测试
    ├── test_generation.py
    └── test_recommendation.py
```

### 4.2 核心依赖 (requirements.txt)

```txt
# Web框架
fastapi==0.104.1
uvicorn[standard]==0.24.0
pydantic==2.5.0

# AI/ML核心
torch==2.1.0
transformers==4.35.0
sentence-transformers==2.2.2
scikit-learn==1.3.2
xgboost==2.0.2

# 数据处理
pandas==2.1.3
numpy==1.24.3
scipy==1.11.4

# 向量数据库
pymilvus==2.3.3

# 数据库客户端
pymongo==4.6.0
redis==5.0.1

# 文档解析
pdfplumber==0.10.3
python-docx==1.1.0
python-pptx==0.6.23

# NLP工具
nltk==3.8.1
jieba==0.42.1

# 其他工具
loguru==0.7.2
python-multipart==0.0.6
```

### 4.3 模型部署方案

#### 4.3.1 大语言模型部署

**选项1: 本地部署 (推荐用于私有化)**

```python
# models/llm/qwen_model.py
from transformers import AutoTokenizer, AutoModelForCausalLM
import torch

class QwenModel:
    def __init__(self, model_path: str = "Qwen/Qwen-7B-Chat"):
        self.tokenizer = AutoTokenizer.from_pretrained(
            model_path,
            trust_remote_code=True
        )
        self.model = AutoModelForCausalLM.from_pretrained(
            model_path,
            torch_dtype=torch.float16,
            device_map="auto",
            trust_remote_code=True
        )
        self.model.eval()
    
    def generate(
        self, 
        prompt: str, 
        max_tokens: int = 2048,
        temperature: float = 0.7,
        top_p: float = 0.9
    ) -> str:
        inputs = self.tokenizer(prompt, return_tensors="pt")
        inputs = {k: v.to(self.model.device) for k, v in inputs.items()}
        
        with torch.no_grad():
            outputs = self.model.generate(
                **inputs,
                max_new_tokens=max_tokens,
                temperature=temperature,
                top_p=top_p,
                do_sample=True,
                pad_token_id=self.tokenizer.pad_token_id
            )
        
        response = self.tokenizer.decode(outputs[0], skip_special_tokens=True)
        return response
```

**选项2: API调用 (用于SaaS版本)**

```python
# models/llm/api_model.py
import openai

class APILLMModel:
    def __init__(self, api_key: str, api_base: str):
        self.client = openai.OpenAI(
            api_key=api_key,
            base_url=api_base
        )
    
    def generate(self, prompt: str, **kwargs) -> str:
        response = self.client.chat.completions.create(
            model="qwen-plus",
            messages=[
                {"role": "system", "content": "你是一个专业的测试工程师。"},
                {"role": "user", "content": prompt}
            ],
            **kwargs
        )
        return response.choices[0].message.content
```

#### 4.3.2 模型文件管理

```python
# config.py
import os

class AIConfig:
    # 模型路径配置
    QWEN_MODEL_PATH = os.getenv('QWEN_MODEL_PATH', '/models/Qwen-7B-Chat')
    SENTENCE_BERT_MODEL = 'paraphrase-multilingual-mpnet-base-v2'
    XGBOOST_MODEL_PATH = '/models/xgboost_models/'
    
    # 模型运行配置
    USE_GPU = torch.cuda.is_available()
    MAX_GPU_MEMORY = '16GB'
    DEVICE_MAP = 'auto'
    
    # 推理配置
    MAX_TOKENS = 2048
    TEMPERATURE = 0.7
    TOP_P = 0.9
    
    # 向量数据库配置
    MILVUS_HOST = os.getenv('MILVUS_HOST', 'localhost')
    MILVUS_PORT = int(os.getenv('MILVUS_PORT', 19530))
    
    # MongoDB配置
    MONGODB_URI = os.getenv('MONGODB_URI', 'mongodb://localhost:27017')
    MONGODB_DB = 'synapsetest_ai'
    
    # Redis配置
    REDIS_HOST = os.getenv('REDIS_HOST', 'localhost')
    REDIS_PORT = int(os.getenv('REDIS_PORT', 6379))
```

---

## 5. 数据流设计

### 5.1 Phase 3 数据流

```
用户创建任务
    │
    ├─> Java Backend: TestTaskController
    │       │
    │       ├─> TestTaskService.createTask()
    │       │       │
    │       │       └─> TestRecommendationService.getRecommendation()
    │       │               │
    │       │               └─> 调用AI Service (gRPC/REST)
    │       │
    │       └─> Python AI Service: /api/v1/ai/recommendation/strategy
    │               │
    │               ├─> 特征提取: FeatureExtractor
    │               │       │
    │               │       ├─> 从PostgreSQL获取历史数据
    │               │       ├─> 从MongoDB获取特征数据
    │               │       └─> 从Redis获取实时状态
    │               │
    │               ├─> 模型推理: TestStrategyRecommender
    │               │       │
    │               │       ├─> XGBoost模型预测
    │               │       └─> 规则引擎修正
    │               │
    │               └─> 返回推荐结果
    │
    └─> 返回给前端: CreateTestTask.jsx
            │
            └─> 展示推荐结果，用户确认或修改
```

### 5.2 Phase 4 数据流

```
用户上传需求文档
    │
    ├─> Java Backend: TestCaseController
    │       │
    │       └─> AITestCaseGenerationService.generateTestCases()
    │               │
    │               └─> 调用AI Service
    │
    └─> Python AI Service: /api/v1/ai/testcase/generate
            │
            ├─> 文档解析: DocumentParser
            │       │
            │       ├─> PDF/Word/Markdown解析
            │       └─> 提取需求文本
            │
            ├─> RAG检索: VectorDB.search()
            │       │
            │       ├─> 计算需求embedding
            │       └─> 检索相似历史用例
            │
            ├─> LLM生成: QwenModel.generate()
            │       │
            │       ├─> 构建Prompt (需求+历史用例+标准)
            │       ├─> 大模型生成
            │       └─> 解析JSON输出
            │
            ├─> 去重优化: SemanticDeduplicator
            │       │
            │       ├─> Sentence-BERT计算相似度
            │       └─> 聚类去重
            │
            ├─> 优先级排序: TestCasePrioritizer
            │       │
            │       └─> 多因子评分排序
            │
            └─> 返回生成的测试用例
                    │
                    └─> 前端展示: AITestCaseGeneration.jsx
                            │
                            └─> 用户审核、修改、保存
```

---

## 6. 部署与优化

### 6.1 Docker部署

**Dockerfile (AI Service)**

```dockerfile
FROM nvidia/cuda:11.8.0-cudnn8-runtime-ubuntu22.04

# 安装Python 3.10
RUN apt-get update && apt-get install -y \
    python3.10 \
    python3-pip \
    && rm -rf /var/lib/apt/lists/*

# 设置工作目录
WORKDIR /app

# 复制依赖文件
COPY requirements.txt .

# 安装Python依赖
RUN pip3 install --no-cache-dir -r requirements.txt

# 下载模型（可选：预先下载模型到镜像）
# RUN python3 -c "from transformers import AutoModel; AutoModel.from_pretrained('Qwen/Qwen-7B-Chat')"

# 复制应用代码
COPY . .

# 暴露端口
EXPOSE 8000

# 启动命令
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000", "--workers", "2"]
```

**docker-compose.yml (AI服务部分)**

```yaml
ai-service:
  build: ./ai-service
  container_name: synapsetest-ai
  ports:
    - "8000:8000"
  environment:
    - MONGODB_URI=mongodb://admin:password@mongodb:27017
    - REDIS_HOST=redis
    - REDIS_PORT=6379
    - MILVUS_HOST=milvus
    - MILVUS_PORT=19530
    - QWEN_MODEL_PATH=/models/Qwen-7B-Chat
  volumes:
    - ./models:/models  # 模型文件挂载
    - ./ai-service:/app
  deploy:
    resources:
      reservations:
        devices:
          - driver: nvidia
            count: 1
            capabilities: [gpu]
  depends_on:
    - mongodb
    - redis
    - milvus
```

### 6.2 性能优化

#### 6.2.1 模型推理优化

```python
# 模型量化 (8-bit)
from transformers import AutoModelForCausalLM, BitsAndBytesConfig

quantization_config = BitsAndBytesConfig(
    load_in_8bit=True,
    llm_int8_threshold=6.0
)

model = AutoModelForCausalLM.from_pretrained(
    "Qwen/Qwen-7B-Chat",
    quantization_config=quantization_config,
    device_map="auto"
)

# 推理缓存
import functools
from cachetools import TTLCache

# 缓存推荐结果（5分钟有效）
recommendation_cache = TTLCache(maxsize=1000, ttl=300)

@functools.lru_cache(maxsize=128)
def cached_recommend(context_hash: str):
    # 推理逻辑
    pass
```

#### 6.2.2 批量推理

```python
class BatchInferenceService:
    def __init__(self, batch_size: int = 8, max_wait_time: float = 0.5):
        self.batch_size = batch_size
        self.max_wait_time = max_wait_time
        self.request_queue = asyncio.Queue()
        self.model = QwenModel()
    
    async def batch_inference_worker(self):
        while True:
            batch = []
            start_time = time.time()
            
            # 收集一批请求
            while len(batch) < self.batch_size:
                if time.time() - start_time > self.max_wait_time:
                    break
                try:
                    request = await asyncio.wait_for(
                        self.request_queue.get(), 
                        timeout=0.1
                    )
                    batch.append(request)
                except asyncio.TimeoutError:
                    break
            
            if batch:
                # 批量推理
                results = self.model.batch_generate([r['prompt'] for r in batch])
                
                # 返回结果
                for request, result in zip(batch, results):
                    request['future'].set_result(result)
```

### 6.3 监控告警

```python
# 添加Prometheus监控
from prometheus_client import Counter, Histogram, Gauge
from fastapi import FastAPI

app = FastAPI()

# 指标定义
ai_requests_total = Counter(
    'ai_requests_total', 
    'Total AI requests',
    ['service', 'status']
)

ai_request_duration = Histogram(
    'ai_request_duration_seconds',
    'AI request duration',
    ['service']
)

ai_model_gpu_memory = Gauge(
    'ai_model_gpu_memory_mb',
    'GPU memory usage'
)

# 使用示例
@app.post("/api/v1/ai/testcase/generate")
async def generate_testcases(request: TestCaseGenerationRequest):
    with ai_request_duration.labels(service='testcase_generation').time():
        try:
            result = await generation_service.generate(request)
            ai_requests_total.labels(service='testcase_generation', status='success').inc()
            return result
        except Exception as e:
            ai_requests_total.labels(service='testcase_generation', status='error').inc()
            raise
```

---

## 7. 实施计划

### 7.1 Phase 3 实施步骤 (预计2周)

#### Week 1: 基础设施 + 推荐算法

**Day 1-2: 环境搭建**
- [ ] 搭建AI Service项目结构
- [ ] 配置Python环境和依赖
- [ ] 搭建MongoDB/Redis/Milvus
- [ ] 配置GPU环境

**Day 3-4: 特征工程**
- [ ] 实现FeatureExtractor类
- [ ] 实现数据采集脚本
- [ ] 构建训练数据集
- [ ] 特征预处理pipeline

**Day 5-7: 推荐模型训练**
- [ ] XGBoost模型训练
- [ ] 模型评估和调优
- [ ] 模型序列化保存
- [ ] 规则引擎实现

#### Week 2: API开发 + 集成测试

**Day 8-10: API开发**
- [ ] 实现推荐API endpoint
- [ ] 实现风险评估API
- [ ] Java Service集成
- [ ] 单元测试

**Day 11-12: 前端集成**
- [ ] 前端组件调整
- [ ] API对接
- [ ] UI优化

**Day 13-14: 测试与优化**
- [ ] 集成测试
- [ ] 性能测试
- [ ] 用户验收测试
- [ ] Bug修复

### 7.2 Phase 4 实施步骤 (预计3周)

#### Week 1: 模型准备 + 基础功能

**Day 1-3: LLM部署**
- [ ] 下载Qwen-7B-Chat模型
- [ ] 模型量化优化
- [ ] 推理性能测试
- [ ] GPU资源配置

**Day 4-5: RAG系统搭建**
- [ ] Milvus向量库配置
- [ ] Sentence-BERT部署
- [ ] 历史用例embedding
- [ ] 检索功能实现

**Day 6-7: Prompt工程**
- [ ] 设计Prompt模板
- [ ] 少样本学习优化
- [ ] 输出格式优化
- [ ] 测试不同场景

#### Week 2: 优化算法 + API开发

**Day 8-10: 优化算法**
- [ ] 语义去重算法
- [ ] 优先级排序算法
- [ ] 覆盖率分析
- [ ] 算法测试

**Day 11-14: API开发**
- [ ] 用例生成API
- [ ] 优化API
- [ ] 文档解析API
- [ ] Java Service集成

#### Week 3: 前端开发 + 测试

**Day 15-17: 前端开发**
- [ ] AITestCaseGeneration组件
- [ ] 用例编辑器
- [ ] 批量导入功能
- [ ] 用户反馈收集

**Day 18-21: 测试与优化**
- [ ] 端到端测试
- [ ] 用例生成质量评估
- [ ] 性能优化
- [ ] 用户验收测试

### 7.3 风险与应对

| 风险 | 概率 | 影响 | 应对措施 |
|------|------|------|---------|
| GPU资源不足 | 中 | 高 | 1. 使用模型量化<br>2. 考虑API调用方案 |
| LLM生成质量不稳定 | 高 | 中 | 1. 多次生成取最优<br>2. 人工修正反馈循环 |
| 推荐准确率低 | 中 | 高 | 1. 增加训练数据<br>2. 特征工程优化 |
| 推理速度慢 | 中 | 中 | 1. 批量推理<br>2. 结果缓存 |

---

## 附录

### A. 训练数据准备

**数据采集脚本示例**:

```python
# scripts/collect_training_data.py
import pandas as pd
from pymongo import MongoClient

class TrainingDataCollector:
    def __init__(self):
        self.mongo_client = MongoClient('mongodb://localhost:27017')
        self.db = self.mongo_client['synapsetest']
    
    def collect_recommendation_data(self):
        """
        采集推荐训练数据
        """
        # 从历史任务中提取特征和标签
        tasks = self.db.test_tasks.find({
            'status': 'COMPLETED',
            'created_at': {'$gte': datetime(2024, 1, 1)}
        })
        
        data = []
        for task in tasks:
            features = self.extract_features(task)
            label = self.extract_label(task)
            data.append({**features, 'label': label})
        
        df = pd.DataFrame(data)
        df.to_csv('data/recommendation_training_data.csv', index=False)
        return df
    
    def extract_features(self, task):
        # 特征提取逻辑
        return {
            'changed_files': task.get('codeChange', {}).get('fileCount', 0),
            'changed_lines': task.get('codeChange', {}).get('lineCount', 0),
            'pass_rate': task.get('historical', {}).get('passRate', 0),
            # ... 更多特征
        }
```

### B. 模型训练脚本

```python
# scripts/train_recommendation_model.py
import xgboost as xgb
import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.metrics import classification_report

def train_recommendation_model():
    # 加载数据
    df = pd.read_csv('data/recommendation_training_data.csv')
    
    # 特征和标签
    X = df.drop('label', axis=1)
    y = df['label']
    
    # 分割数据
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42
    )
    
    # 训练模型
    model = xgb.XGBClassifier(
        objective='multi:softmax',
        num_class=3,  # SMOKE/CORE/FULL
        max_depth=6,
        learning_rate=0.1,
        n_estimators=100
    )
    
    model.fit(X_train, y_train)
    
    # 评估
    y_pred = model.predict(X_test)
    print(classification_report(y_test, y_pred))
    
    # 保存模型
    model.save_model('models/test_strategy_recommender.json')
    
    return model

if __name__ == '__main__':
    train_recommendation_model()
```

### C. 参考资料

- [Qwen模型官方文档](https://github.com/QwenLM/Qwen)
- [Sentence-BERT文档](https://www.sbert.net/)
- [XGBoost文档](https://xgboost.readthedocs.io/)
- [FastAPI文档](https://fastapi.tiangolo.com/)
- [Milvus向量数据库](https://milvus.io/)

---

**文档结束**

**下一步行动**:
1. 评审本设计方案
2. 准备开发环境
3. 开始Phase 3实施
4. 收集用户反馈
5. 迭代优化


