# TDD测试方案 - User Story 1: 智能测试任务调度

## 一、业务场景描述

**场景名称**：测试经理创建冒烟测试任务并获取AI推荐策略

**用户故事**：
> 作为测试经理，我需要为登录模块创建一个冒烟测试任务，系统应该根据代码变更范围和历史数据自动推荐合适的测试环境、版本匹配和测试范围，以便我能快速验证本次代码提交的质量，显著减少手动配置时间。

**业务价值**：
- 减少70%测试配置时间
- 自动匹配最适合的测试策略
- 基于历史数据的智能推荐

**业务流程**：
```
用户输入任务信息 → 系统分析代码变更 → AI推荐测试策略 → 用户确认创建 → 任务保存并分配资源
```

---

## 二、功能范围

| 工程 | 涉及组件 | 说明 |
|------|---------|------|
| Backend | TestTaskService, TestRecommendationService | 任务CRUD + 推荐算法 |
| Frontend | CreateTestTask.jsx, TestTaskList.jsx | 任务创建表单 + 列表展示 |
| AI-Service | /api/v1/ai/recommendation/strategy | 策略推荐接口 |

---

## 三、测试方案

### 3.1 Backend 测试

#### 3.1.1 Service层单元测试

**文件**: `backend/src/test/java/com/synapsetest/testmanagement/service/TestTaskServiceTest.java`

```java
@SpringBootTest
@DisplayName("测试任务服务 - 智能调度场景")
class TestTaskServiceTest {

    @MockBean
    private TestTaskMapper testTaskMapper;

    @MockBean
    private TestRecommendationService recommendationService;

    @Autowired
    private TestTaskService testTaskService;

    @Test
    @DisplayName("场景1.1: 创建冒烟测试任务成功")
    void createSmokeTestTask_WithValidInput_ShouldReturnTask() {
        // Given: 有效的任务请求数据
        TestTaskRequest request = new TestTaskRequest();
        request.setName("登录模块冒烟测试");
        request.setDescription("验证登录功能核心流程");
        request.setEnvironment("DEV");
        request.setVersion("v2.1.0");
        request.setTestScope("SMOKE");
        request.setPriority(8);

        when(testTaskMapper.insert(any(TestTask.class))).thenReturn(1);

        // When: 调用创建服务
        TestTaskResponse response = testTaskService.createTestTask(request, "zhangsan");

        // Then: 任务创建成功
        assertNotNull(response.getId());
        assertEquals("登录模块冒烟测试", response.getName());
        assertEquals("PENDING", response.getStatus());
        assertEquals("zhangsan", response.getCreatedBy());
        assertNotNull(response.getCreatedAt());
        verify(testTaskMapper, times(1)).insert(any(TestTask.class));
    }

    @Test
    @DisplayName("场景1.2: 任务名称为空时创建失败")
    void createTask_WithEmptyName_ShouldThrowValidationException() {
        // Given: 空名称
        TestTaskRequest request = new TestTaskRequest();
        request.setName("");
        request.setEnvironment("DEV");
        request.setVersion("v2.1.0");

        // When & Then: 抛出验证异常
        ValidationException exception = assertThrows(ValidationException.class,
            () -> testTaskService.createTestTask(request, "zhangsan"));

        assertTrue(exception.getMessage().contains("名称不能为空"));
    }

    @Test
    @DisplayName("场景1.3: 无效环境类型时创建失败")
    void createTask_WithInvalidEnvironment_ShouldThrowException() {
        // Given: 无效环境
        TestTaskRequest request = new TestTaskRequest();
        request.setName("测试任务");
        request.setEnvironment("INVALID_ENV");
        request.setVersion("v2.1.0");

        // When & Then: 抛出异常
        assertThrows(ValidationException.class,
            () -> testTaskService.createTestTask(request, "zhangsan"));
    }

    @Test
    @DisplayName("场景1.4: 启动PENDING状态任务成功")
    void startTask_FromPendingStatus_ShouldSucceed() {
        // Given: PENDING状态的任务
        TestTask task = createTestTask("task-001", "PENDING");
        when(testTaskMapper.selectById("task-001")).thenReturn(Optional.of(task));
        when(testTaskMapper.update(any())).thenReturn(1);

        // When: 启动任务
        TestTaskResponse response = testTaskService.startTask("task-001");

        // Then: 状态变为RUNNING
        assertEquals("RUNNING", response.getStatus());
    }

    @Test
    @DisplayName("场景1.5: 启动已完成任务失败")
    void startTask_FromCompletedStatus_ShouldThrowException() {
        // Given: COMPLETED状态的任务
        TestTask task = createTestTask("task-001", "COMPLETED");
        when(testTaskMapper.selectById("task-001")).thenReturn(Optional.of(task));

        // When & Then: 抛出异常
        assertThrows(IllegalStateException.class,
            () -> testTaskService.startTask("task-001"));
    }

    @Test
    @DisplayName("场景1.6: 获取不存在的任务抛出异常")
    void getTaskById_WithNonExistingId_ShouldThrowResourceNotFoundException() {
        // Given: 不存在的ID
        when(testTaskMapper.selectById("non-existing")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class,
            () -> testTaskService.getTaskById("non-existing"));
    }
}
```

**文件**: `backend/src/test/java/com/synapsetest/testmanagement/service/TestRecommendationServiceTest.java`

```java
@SpringBootTest
@DisplayName("测试推荐服务 - AI策略推荐")
class TestRecommendationServiceTest {

    @Autowired
    private TestRecommendationService recommendationService;

    @Test
    @DisplayName("场景2.1: 小范围代码变更推荐冒烟测试")
    void recommendStrategy_WithSmallCodeChange_ShouldRecommendSmokeTest() {
        // Given: 小范围代码变更
        CodeChangeInfo changeInfo = new CodeChangeInfo();
        changeInfo.setChangedFiles(3);
        changeInfo.setChangedLines(45);
        changeInfo.setAffectedModule("login-service");
        changeInfo.setHistoricalPassRate(0.95);

        // When: 请求推荐策略
        RecommendationResult result = recommendationService.getStrategy(changeInfo);

        // Then: 推荐冒烟测试
        assertEquals("SMOKE", result.getRecommendedScope());
        assertTrue(result.getEstimatedTimeMinutes() <= 10);
        assertEquals("DEV", result.getSuggestedEnvironment());
        assertTrue(result.getConfidence() > 0.8);
    }

    @Test
    @DisplayName("场景2.2: 大范围代码变更推荐全量回归")
    void recommendStrategy_WithLargeCodeChange_ShouldRecommendFullRegression() {
        // Given: 大范围代码变更
        CodeChangeInfo changeInfo = new CodeChangeInfo();
        changeInfo.setChangedFiles(25);
        changeInfo.setChangedLines(500);
        changeInfo.setAffectedModule("core-service");
        changeInfo.setHistoricalPassRate(0.75);

        // When: 请求推荐策略
        RecommendationResult result = recommendationService.getStrategy(changeInfo);

        // Then: 推荐全量回归测试
        assertEquals("FULL_REGRESSION", result.getRecommendedScope());
        assertTrue(result.getEstimatedTimeMinutes() > 120);
        assertFalse(result.getHighRiskModules().isEmpty());
    }

    @Test
    @DisplayName("场景2.3: 高风险模块变更优先推荐相关测试")
    void recommendStrategy_WithHighRiskModule_ShouldPrioritizeTests() {
        // Given: 高风险模块变更
        CodeChangeInfo changeInfo = new CodeChangeInfo();
        changeInfo.setChangedFiles(8);
        changeInfo.setChangedLines(120);
        changeInfo.setAffectedModule("payment-service");
        changeInfo.setModuleRiskLevel("HIGH");

        // When: 请求推荐策略
        RecommendationResult result = recommendationService.getStrategy(changeInfo);

        // Then: 推荐核心回归，优先支付相关测试
        assertEquals("CORE_REGRESSION", result.getRecommendedScope());
        assertTrue(result.getPrioritizedTests().stream()
            .anyMatch(test -> test.contains("payment")));
    }
}
```

#### 3.1.2 Controller集成测试

**文件**: `backend/src/test/java/com/synapsetest/testmanagement/controller/TestTaskControllerIntegrationTest.java`

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase
@Sql(scripts = "/test-data-us1.sql", executionPhase = BEFORE_TEST_METHOD)
@DisplayName("测试任务API集成测试")
class TestTaskControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("场景3.1: POST创建冒烟测试任务返回201")
    void createSmokeTestTask_ShouldReturn201() {
        // Given
        String requestBody = """
            {
                "name": "登录模块冒烟测试",
                "description": "验证登录功能核心流程",
                "environment": "DEV",
                "version": "v2.1.0",
                "testScope": "SMOKE",
                "priority": 8
            }
            """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-User-Name", "zhangsan");

        // When
        ResponseEntity<ApiResponse> response = restTemplate.postForEntity(
            "/api/v1/test-tasks",
            new HttpEntity<>(requestBody, headers),
            ApiResponse.class
        );

        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertTrue(response.getBody().getSuccess());
        assertNotNull(response.getBody().getData().get("id"));
        assertEquals("PENDING", response.getBody().getData().get("status"));
    }

    @Test
    @DisplayName("场景3.2: POST创建任务参数验证失败返回400")
    void createTask_WithInvalidInput_ShouldReturn400() {
        // Given: 缺少必填字段
        String requestBody = """
            {
                "name": "",
                "environment": "DEV"
            }
            """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // When
        ResponseEntity<ApiResponse> response = restTemplate.postForEntity(
            "/api/v1/test-tasks",
            new HttpEntity<>(requestBody, headers),
            ApiResponse.class
        );

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().getSuccess());
    }

    @Test
    @DisplayName("场景3.3: GET获取任务详情返回200")
    void getTaskById_WithExistingId_ShouldReturn200() {
        // Given: 数据库中存在任务 (由test-data-us1.sql初始化)
        String taskId = "task-001";

        // When
        ResponseEntity<ApiResponse> response = restTemplate.getForEntity(
            "/api/v1/test-tasks/" + taskId,
            ApiResponse.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().getSuccess());
        assertEquals(taskId, response.getBody().getData().get("id"));
    }

    @Test
    @DisplayName("场景3.4: GET获取不存在任务返回404")
    void getTaskById_WithNonExistingId_ShouldReturn404() {
        // When
        ResponseEntity<ApiResponse> response = restTemplate.getForEntity(
            "/api/v1/test-tasks/non-existing-id",
            ApiResponse.class
        );

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    @DisplayName("场景3.5: POST启动任务成功")
    void startTask_ShouldUpdateStatusToRunning() {
        // Given: PENDING状态任务
        String taskId = "task-pending";

        // When
        ResponseEntity<ApiResponse> response = restTemplate.postForEntity(
            "/api/v1/test-tasks/" + taskId + "/start",
            null,
            ApiResponse.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("RUNNING", response.getBody().getData().get("status"));
    }

    @Test
    @DisplayName("场景3.6: GET按状态筛选任务列表")
    void getTasksByStatus_ShouldReturnFilteredList() {
        // When
        ResponseEntity<ApiResponse> response = restTemplate.getForEntity(
            "/api/v1/test-tasks?status=RUNNING",
            ApiResponse.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<Map> tasks = (List<Map>) response.getBody().getData();
        tasks.forEach(task -> assertEquals("RUNNING", task.get("status")));
    }
}
```

#### 3.1.3 Mapper层测试

**文件**: `backend/src/test/java/com/synapsetest/testmanagement/mapper/TestTaskMapperTest.java`

```java
@MybatisTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@DisplayName("TestTask Mapper测试")
class TestTaskMapperTest {

    @Autowired
    private TestTaskMapper testTaskMapper;

    @Test
    @DisplayName("场景4.1: 插入任务数据成功")
    void insert_ShouldPersistTask() {
        // Given
        TestTask task = new TestTask();
        task.setId(UUID.randomUUID().toString());
        task.setName("测试任务");
        task.setEnvironment("DEV");
        task.setVersion("v1.0.0");
        task.setStatus("PENDING");
        task.setPriority(5);
        task.setCreatedBy("tester");
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());

        // When
        int result = testTaskMapper.insert(task);

        // Then
        assertEquals(1, result);
        Optional<TestTask> saved = testTaskMapper.selectById(task.getId());
        assertTrue(saved.isPresent());
        assertEquals("测试任务", saved.get().getName());
    }

    @Test
    @DisplayName("场景4.2: 按状态查询任务")
    void selectByStatus_ShouldFilterCorrectly() {
        // When
        List<TestTask> runningTasks = testTaskMapper.selectByStatus("RUNNING");

        // Then
        runningTasks.forEach(task -> assertEquals("RUNNING", task.getStatus()));
    }
}
```

---

### 3.2 Frontend 测试

#### 3.2.1 组件单元测试

**文件**: `frontend/src/components/test-task/__tests__/CreateTestTask.test.tsx`

```typescript
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Provider } from 'react-redux';
import { BrowserRouter } from 'react-router-dom';
import CreateTestTask from '../CreateTestTask';
import { store } from '../../../store';
import testTaskService from '../../../services/testTaskService';

jest.mock('../../../services/testTaskService');

const renderComponent = () => {
  render(
    <Provider store={store}>
      <BrowserRouter>
        <CreateTestTask />
      </BrowserRouter>
    </Provider>
  );
};

describe('创建冒烟测试任务场景', () => {

  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('场景1.1: 表单初始状态 - 提交按钮禁用', () => {
    // Given & When
    renderComponent();

    // Then
    expect(screen.getByRole('button', { name: '创建任务' })).toBeDisabled();
    expect(screen.getByLabelText('任务名称')).toHaveValue('');
  });

  test('场景1.2: 填写必填字段后提交按钮启用', async () => {
    // Given
    renderComponent();

    // When: 填写所有必填字段
    await userEvent.type(screen.getByLabelText('任务名称'), '登录模块冒烟测试');
    await userEvent.selectOptions(screen.getByLabelText('环境'), 'DEV');
    await userEvent.selectOptions(screen.getByLabelText('版本'), 'v2.1.0');

    // Then
    expect(screen.getByRole('button', { name: '创建任务' })).toBeEnabled();
  });

  test('场景1.3: 选择环境后显示AI推荐策略', async () => {
    // Given
    const mockRecommendation = {
      recommendedScope: 'SMOKE',
      estimatedTimeMinutes: 10,
      suggestedEnvironment: 'DEV',
      confidence: 0.85
    };
    (testTaskService.getRecommendation as jest.Mock).mockResolvedValue(mockRecommendation);

    renderComponent();

    // When
    await userEvent.type(screen.getByLabelText('任务名称'), '登录模块冒烟测试');
    await userEvent.selectOptions(screen.getByLabelText('环境'), 'DEV');

    // Then
    await waitFor(() => {
      expect(screen.getByText(/推荐测试范围: SMOKE/)).toBeInTheDocument();
      expect(screen.getByText(/预估时间: 10分钟/)).toBeInTheDocument();
      expect(screen.getByText(/置信度: 85%/)).toBeInTheDocument();
    });
  });

  test('场景1.4: 提交任务创建成功', async () => {
    // Given
    const mockResponse = {
      id: 'task-001',
      name: '登录模块冒烟测试',
      status: 'PENDING',
      createdBy: 'zhangsan'
    };
    (testTaskService.createTask as jest.Mock).mockResolvedValue(mockResponse);

    renderComponent();

    // When
    await userEvent.type(screen.getByLabelText('任务名称'), '登录模块冒烟测试');
    await userEvent.selectOptions(screen.getByLabelText('环境'), 'DEV');
    await userEvent.selectOptions(screen.getByLabelText('版本'), 'v2.1.0');
    await userEvent.click(screen.getByRole('button', { name: '创建任务' }));

    // Then
    await waitFor(() => {
      expect(screen.getByText('任务创建成功')).toBeInTheDocument();
    });
    expect(testTaskService.createTask).toHaveBeenCalledWith({
      name: '登录模块冒烟测试',
      environment: 'DEV',
      version: 'v2.1.0'
    });
  });

  test('场景1.5: 提交失败显示错误消息', async () => {
    // Given
    (testTaskService.createTask as jest.Mock).mockRejectedValue(
      new Error('服务器内部错误')
    );

    renderComponent();

    // When
    await userEvent.type(screen.getByLabelText('任务名称'), '测试任务');
    await userEvent.selectOptions(screen.getByLabelText('环境'), 'DEV');
    await userEvent.selectOptions(screen.getByLabelText('版本'), 'v2.1.0');
    await userEvent.click(screen.getByRole('button', { name: '创建任务' }));

    // Then
    await waitFor(() => {
      expect(screen.getByText(/创建失败/)).toBeInTheDocument();
    });
  });

  test('场景1.6: 名称为空时显示验证错误', async () => {
    // Given
    renderComponent();

    // When: 点击名称输入框后离开（触发验证）
    const nameInput = screen.getByLabelText('任务名称');
    await userEvent.click(nameInput);
    await userEvent.tab();

    // Then
    await waitFor(() => {
      expect(screen.getByText('任务名称不能为空')).toBeInTheDocument();
    });
  });
});
```

**文件**: `frontend/src/components/test-task/__tests__/TestTaskList.test.tsx`

```typescript
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import TestTaskList from '../TestTaskList';
import testTaskService from '../../../services/testTaskService';

jest.mock('../../../services/testTaskService');

describe('测试任务列表场景', () => {

  const mockTasks = [
    {
      id: 'task-001',
      name: '登录模块冒烟测试',
      environment: 'DEV',
      version: 'v2.1.0',
      status: 'PENDING',
      priority: 8,
      createdBy: 'zhangsan',
      createdAt: '2025-11-16T10:00:00Z'
    },
    {
      id: 'task-002',
      name: '支付流程回归测试',
      environment: 'STAGING',
      version: 'v2.1.0',
      status: 'RUNNING',
      priority: 9,
      createdBy: 'lisi',
      createdAt: '2025-11-16T11:00:00Z'
    }
  ];

  beforeEach(() => {
    (testTaskService.getAllTasks as jest.Mock).mockResolvedValue(mockTasks);
  });

  test('场景2.1: 正确渲染任务列表', async () => {
    // When
    render(<TestTaskList />);

    // Then
    await waitFor(() => {
      expect(screen.getByText('登录模块冒烟测试')).toBeInTheDocument();
      expect(screen.getByText('支付流程回归测试')).toBeInTheDocument();
    });
  });

  test('场景2.2: 显示任务状态标签', async () => {
    // When
    render(<TestTaskList />);

    // Then
    await waitFor(() => {
      expect(screen.getByText('PENDING')).toBeInTheDocument();
      expect(screen.getByText('RUNNING')).toBeInTheDocument();
    });
  });

  test('场景2.3: 启动按钮触发任务启动', async () => {
    // Given
    (testTaskService.startTask as jest.Mock).mockResolvedValue({
      ...mockTasks[0],
      status: 'RUNNING'
    });

    render(<TestTaskList />);

    // When
    await waitFor(() => {
      expect(screen.getByText('登录模块冒烟测试')).toBeInTheDocument();
    });
    const startButton = screen.getAllByRole('button', { name: '启动' })[0];
    await userEvent.click(startButton);

    // Then
    expect(testTaskService.startTask).toHaveBeenCalledWith('task-001');
  });

  test('场景2.4: 空列表显示提示信息', async () => {
    // Given
    (testTaskService.getAllTasks as jest.Mock).mockResolvedValue([]);

    // When
    render(<TestTaskList />);

    // Then
    await waitFor(() => {
      expect(screen.getByText('暂无测试任务')).toBeInTheDocument();
    });
  });
});
```

#### 3.2.2 Service层测试

**文件**: `frontend/src/services/__tests__/testTaskService.test.ts`

```typescript
import axios from 'axios';
import testTaskService from '../testTaskService';

jest.mock('axios');
const mockedAxios = axios as jest.Mocked<typeof axios>;

describe('testTaskService - 创建冒烟测试任务', () => {

  test('场景3.1: createTask调用正确的API端点', async () => {
    // Given
    const taskData = {
      name: '登录模块冒烟测试',
      environment: 'DEV',
      version: 'v2.1.0',
      testScope: 'SMOKE'
    };

    mockedAxios.post.mockResolvedValue({
      data: { success: true, data: { id: 'task-001' } }
    });

    // When
    await testTaskService.createTask(taskData);

    // Then
    expect(mockedAxios.post).toHaveBeenCalledWith(
      '/api/v1/test-tasks',
      taskData
    );
  });

  test('场景3.2: getRecommendation返回推荐策略', async () => {
    // Given
    const codeChangeInfo = { changedFiles: 3, changedLines: 45 };
    const expectedRecommendation = {
      recommendedScope: 'SMOKE',
      estimatedTimeMinutes: 10
    };

    mockedAxios.post.mockResolvedValue({
      data: { success: true, data: expectedRecommendation }
    });

    // When
    const result = await testTaskService.getRecommendation(codeChangeInfo);

    // Then
    expect(result).toEqual(expectedRecommendation);
  });

  test('场景3.3: API错误时抛出异常', async () => {
    // Given
    mockedAxios.post.mockRejectedValue(new Error('Network Error'));

    // When & Then
    await expect(testTaskService.createTask({})).rejects.toThrow('Network Error');
  });
});
```

---

### 3.3 AI-Service 测试

**文件**: `ai-service/tests/test_recommendation_api.py`

```python
import pytest
from fastapi.testclient import TestClient
from main import app

client = TestClient(app)

class TestRecommendationAPI:
    """测试策略推荐API - 智能调度场景"""

    def test_scenario_1_recommend_smoke_test_for_small_changes(self):
        """场景1: 小范围代码变更推荐冒烟测试"""
        # Given
        request_data = {
            "changed_files": 3,
            "changed_lines": 45,
            "affected_module": "login-service",
            "historical_pass_rate": 0.95
        }

        # When
        response = client.post(
            "/api/v1/ai/recommendation/strategy",
            json=request_data
        )

        # Then
        assert response.status_code == 200
        data = response.json()
        assert data["recommended_scope"] == "SMOKE"
        assert data["estimated_time_minutes"] <= 10
        assert data["suggested_environment"] == "DEV"
        assert data["confidence"] > 0.8

    def test_scenario_2_recommend_full_regression_for_large_changes(self):
        """场景2: 大范围代码变更推荐全量回归"""
        # Given
        request_data = {
            "changed_files": 25,
            "changed_lines": 500,
            "affected_module": "core-service",
            "historical_pass_rate": 0.75
        }

        # When
        response = client.post(
            "/api/v1/ai/recommendation/strategy",
            json=request_data
        )

        # Then
        assert response.status_code == 200
        data = response.json()
        assert data["recommended_scope"] == "FULL_REGRESSION"
        assert data["estimated_time_minutes"] > 120
        assert len(data["high_risk_modules"]) > 0

    def test_scenario_3_return_prioritized_test_cases(self):
        """场景3: 返回优先级排序的测试用例"""
        # Given
        request_data = {
            "changed_files": 8,
            "affected_module": "payment-service"
        }

        # When
        response = client.post(
            "/api/v1/ai/recommendation/strategy",
            json=request_data
        )

        # Then
        data = response.json()
        assert "prioritized_test_cases" in data
        priorities = [tc["priority"] for tc in data["prioritized_test_cases"]]
        assert priorities == sorted(priorities, reverse=True)

    def test_scenario_4_invalid_input_returns_400(self):
        """场景4: 无效输入返回400错误"""
        # Given
        request_data = {
            "changed_files": -1,  # 无效值
            "affected_module": ""
        }

        # When
        response = client.post(
            "/api/v1/ai/recommendation/strategy",
            json=request_data
        )

        # Then
        assert response.status_code == 400
```

---

## 四、测试数据

**文件**: `backend/src/test/resources/test-data-us1.sql`

```sql
-- User Story 1 测试数据
INSERT INTO test_tasks (id, name, description, environment, version, test_scope, status, priority, created_by, created_at, updated_at) VALUES
('task-001', '登录模块冒烟测试', '验证登录功能', 'DEV', 'v2.1.0', 'SMOKE', 'PENDING', 8, 'zhangsan', NOW(), NOW()),
('task-002', '支付流程回归测试', '支付功能回归', 'STAGING', 'v2.1.0', 'CORE_REGRESSION', 'RUNNING', 9, 'lisi', NOW(), NOW()),
('task-pending', '待启动任务', '等待启动', 'DEV', 'v2.0.0', 'SMOKE', 'PENDING', 5, 'wangwu', NOW(), NOW());

INSERT INTO test_environments (id, name, url, status, created_at, updated_at) VALUES
('env-dev', 'DEV环境', 'http://dev.example.com', 'AVAILABLE', NOW(), NOW()),
('env-staging', 'STAGING环境', 'http://staging.example.com', 'AVAILABLE', NOW(), NOW());

INSERT INTO test_versions (id, name, product_version, release_date, created_at, updated_at) VALUES
('ver-200', 'v2.0.0', '2.0.0', '2025-10-01', NOW(), NOW()),
('ver-210', 'v2.1.0', '2.1.0', '2025-11-01', NOW(), NOW());

INSERT INTO resource_pools (id, name, type, capacity, allocated, status, created_at, updated_at) VALUES
('pool-001', 'VM资源池', 'VM', 100, 45, 'AVAILABLE', NOW(), NOW());
```

---

## 五、验收标准

| 测试场景 | 工程 | 验收条件 | 优先级 |
|---------|------|---------|-------|
| 创建冒烟测试任务成功 | Backend | 返回201，任务状态为PENDING，生成UUID | P0 |
| AI推荐冒烟测试策略 | AI-Service | 小范围变更推荐SMOKE，时间≤10分钟，置信度>0.8 | P0 |
| 表单交互和验证 | Frontend | 必填字段验证，显示AI推荐结果 | P0 |
| 任务状态转换 | Backend | PENDING→RUNNING成功，COMPLETED→RUNNING失败 | P1 |
| 任务列表展示 | Frontend | 正确显示任务信息，支持启动/取消操作 | P1 |
| 错误处理 | 全部 | 无效输入返回400，资源不存在返回404 | P1 |

---

## 六、执行顺序

1. **Red阶段**: 编写上述所有测试用例，确保全部失败
2. **Green阶段**: 实现最小代码使测试通过
3. **Refactor阶段**: 优化代码结构，保持测试通过

**建议执行路径**:
```
Mapper测试 → Service单元测试 → Controller集成测试 → Frontend组件测试 → AI-Service测试
```
