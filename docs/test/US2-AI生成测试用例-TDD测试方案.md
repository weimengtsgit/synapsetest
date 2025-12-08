# TDD测试方案 - User Story 2: AI生成测试用例

## 一、业务场景描述

**场景名称**：QA工程师使用AI自动生成登录功能测试用例

**用户故事**：
> 作为测试工程师，我需要为新开发的用户登录功能生成测试用例，我将提供PRD需求文档描述，系统通过AI技术自动分析需求并生成覆盖正常流程、异常场景和边界条件的测试用例，以便减少测试用例编写时间，提高测试覆盖率。

**业务价值**：
- 用例编写效率提升75%（从2小时/用例降至30分钟/用例）
- AI生成用例覆盖率达90%+
- 首次通过率>75%
- 支持人机协作，持续优化生成质量

**业务流程**：
```
输入需求文档 → AI解析需求 → 生成用例初稿 → 用户审核编辑 → 用例去重优化 → 保存到用例库
```

---

## 二、功能范围

| 工程 | 涉及组件 | 说明 |
|------|---------|------|
| Backend | TestCaseService, AITestCaseGenerationService | 用例CRUD + AI生成服务 |
| Frontend | AITestCaseGeneration.jsx, TestCaseList.jsx | AI生成页面 + 用例列表 |
| AI-Service | /api/v1/ai/testcase/generate, LLMModel | 大语言模型生成接口 |

---

## 三、测试方案

### 3.1 Backend 测试

#### 3.1.1 Service层单元测试

**文件**: `backend/src/test/java/com/synapsetest/testmanagement/service/TestCaseServiceTest.java`

```java
@SpringBootTest
@DisplayName("测试用例服务 - CRUD场景")
class TestCaseServiceTest {

    @MockBean
    private TestCaseMapper testCaseMapper;

    @Autowired
    private TestCaseService testCaseService;

    @Test
    @DisplayName("场景1.1: 创建测试用例成功")
    void createTestCase_WithValidInput_ShouldReturnTestCase() {
        // Given: 有效的测试用例请求
        TestCaseRequest request = new TestCaseRequest();
        request.setTitle("验证用户使用正确密码登录成功");
        request.setDescription("测试正常登录流程");
        request.setSteps(Arrays.asList(
            "打开登录页面",
            "输入有效用户名: testuser",
            "输入正确密码: Password123",
            "点击登录按钮"
        ));
        request.setExpectedResults("成功登录并跳转到首页，显示欢迎信息");
        request.setType("FUNCTIONAL");
        request.setPriority(8);
        request.setTags(Arrays.asList("login", "smoke", "p0"));

        when(testCaseMapper.insert(any())).thenReturn(1);

        // When
        TestCaseResponse response = testCaseService.createTestCase(request, "qa_engineer");

        // Then
        assertNotNull(response.getId());
        assertEquals("验证用户使用正确密码登录成功", response.getTitle());
        assertEquals("DRAFT", response.getStatus());
        assertEquals(4, response.getSteps().size());
        assertEquals("qa_engineer", response.getCreatedBy());
    }

    @Test
    @DisplayName("场景1.2: 测试步骤为空时创建失败")
    void createTestCase_WithEmptySteps_ShouldThrowException() {
        // Given
        TestCaseRequest request = new TestCaseRequest();
        request.setTitle("测试用例");
        request.setSteps(Collections.emptyList()); // 空步骤

        // When & Then
        assertThrows(ValidationException.class,
            () -> testCaseService.createTestCase(request, "qa_engineer"));
    }

    @Test
    @DisplayName("场景1.3: 更新测试用例成功")
    void updateTestCase_ShouldUpdateFields() {
        // Given
        TestCase existingCase = createTestCase("case-001");
        when(testCaseMapper.selectById("case-001")).thenReturn(Optional.of(existingCase));
        when(testCaseMapper.update(any())).thenReturn(1);

        TestCaseRequest updateRequest = new TestCaseRequest();
        updateRequest.setTitle("更新后的标题");
        updateRequest.setSteps(Arrays.asList("新步骤1", "新步骤2"));

        // When
        TestCaseResponse response = testCaseService.updateTestCase("case-001", updateRequest);

        // Then
        assertEquals("更新后的标题", response.getTitle());
        assertNotNull(response.getUpdatedAt());
    }

    @Test
    @DisplayName("场景1.4: 审批测试用例状态变更")
    void approveTestCase_ShouldUpdateStatusToApproved() {
        // Given
        TestCase draftCase = createTestCase("case-001");
        draftCase.setStatus("DRAFT");
        when(testCaseMapper.selectById("case-001")).thenReturn(Optional.of(draftCase));
        when(testCaseMapper.update(any())).thenReturn(1);

        // When
        TestCaseResponse response = testCaseService.approveTestCase("case-001");

        // Then
        assertEquals("APPROVED", response.getStatus());
    }
}
```

**文件**: `backend/src/test/java/com/synapsetest/testmanagement/service/AITestCaseGenerationServiceTest.java`

```java
@SpringBootTest
@ActiveProfiles("mongodb")
@DisplayName("AI测试用例生成服务")
class AITestCaseGenerationServiceTest {

    @MockBean
    private RestTemplate restTemplate;  // Mock AI服务调用

    @Autowired
    private AITestCaseGenerationService aiGenerationService;

    @Test
    @DisplayName("场景2.1: 根据需求文档生成测试用例")
    void generateTestCases_WithValidRequirement_ShouldReturnCases() {
        // Given: 用户登录功能需求
        AITestCaseGenerationRequest request = new AITestCaseGenerationRequest();
        request.setRequirementText("""
            用户登录功能需求：
            1. 用户在登录页面输入用户名和密码
            2. 系统验证用户凭据
            3. 验证成功后跳转到首页
            4. 验证失败显示错误提示
            5. 支持"记住我"功能
            6. 连续3次失败锁定账户15分钟
            """);
        request.setNumberOfCases(5);
        request.setTestType("FUNCTIONAL");

        // Mock AI服务响应
        String mockAIResponse = """
            测试用例1: 正常登录成功
            步骤: 输入有效用户名 | 输入正确密码 | 点击登录
            预期结果: 成功跳转首页

            测试用例2: 密码错误登录失败
            步骤: 输入有效用户名 | 输入错误密码 | 点击登录
            预期结果: 显示密码错误提示

            测试用例3: 记住我功能验证
            步骤: 勾选记住我 | 登录成功 | 关闭浏览器 | 重新打开
            预期结果: 自动填充用户名

            测试用例4: 账户锁定验证
            步骤: 连续输入错误密码3次
            预期结果: 账户锁定15分钟

            测试用例5: 空用户名验证
            步骤: 不输入用户名 | 点击登录
            预期结果: 提示用户名必填
            """;

        when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
            .thenReturn(mockAIResponse);

        // When
        List<TestCaseResponse> generatedCases = aiGenerationService.generateTestCases(request);

        // Then
        assertEquals(5, generatedCases.size());

        // 验证生成的用例包含关键场景
        assertTrue(generatedCases.stream()
            .anyMatch(tc -> tc.getTitle().contains("正常登录")));
        assertTrue(generatedCases.stream()
            .anyMatch(tc -> tc.getTitle().contains("密码错误")));
        assertTrue(generatedCases.stream()
            .anyMatch(tc -> tc.getTitle().contains("记住我")));

        // 验证每个用例格式完整
        generatedCases.forEach(tc -> {
            assertNotNull(tc.getTitle());
            assertFalse(tc.getSteps().isEmpty());
            assertNotNull(tc.getExpectedResults());
            assertEquals("DRAFT", tc.getStatus());
            assertEquals("FUNCTIONAL", tc.getType());
        });
    }

    @Test
    @DisplayName("场景2.2: 需求文本为空时生成失败")
    void generateTestCases_WithEmptyRequirement_ShouldThrowException() {
        // Given
        AITestCaseGenerationRequest request = new AITestCaseGenerationRequest();
        request.setRequirementText("");

        // When & Then
        assertThrows(ValidationException.class,
            () -> aiGenerationService.generateTestCases(request));
    }

    @Test
    @DisplayName("场景2.3: AI服务超时时返回错误")
    void generateTestCases_WhenAIServiceTimeout_ShouldThrowException() {
        // Given
        AITestCaseGenerationRequest request = new AITestCaseGenerationRequest();
        request.setRequirementText("需求描述...");

        when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
            .thenThrow(new ResourceAccessException("Connection timeout"));

        // When & Then
        Exception exception = assertThrows(AIServiceException.class,
            () -> aiGenerationService.generateTestCases(request));
        assertTrue(exception.getMessage().contains("AI服务"));
    }

    @Test
    @DisplayName("场景2.4: 解析AI响应提取测试用例")
    void parseAIResponse_ShouldExtractTestCasesCorrectly() {
        // Given
        String aiResponse = """
            测试用例1: 验证登录
            步骤: 步骤A | 步骤B
            预期结果: 结果描述
            """;

        // When
        List<TestCaseResponse> cases = aiGenerationService.parseAIResponse(aiResponse);

        // Then
        assertEquals(1, cases.size());
        assertEquals("验证登录", cases.get(0).getTitle());
        assertEquals(2, cases.get(0).getSteps().size());
    }
}
```

**文件**: `backend/src/test/java/com/synapsetest/testmanagement/service/AITestCaseOptimizationServiceTest.java`

```java
@SpringBootTest
@ActiveProfiles("mongodb")
@DisplayName("AI测试用例优化服务")
class AITestCaseOptimizationServiceTest {

    @Autowired
    private AITestCaseOptimizationService optimizationService;

    @Test
    @DisplayName("场景3.1: 识别并移除语义重复用例")
    void removeDuplicates_ShouldIdentifySemanticDuplicates() {
        // Given: 包含重复用例的列表
        List<TestCaseResponse> cases = Arrays.asList(
            createCase("TC1", "验证用户登录成功",
                Arrays.asList("输入正确用户名", "输入正确密码", "点击登录")),
            createCase("TC2", "测试登录功能正常工作",
                Arrays.asList("填写有效用户名", "填写有效密码", "提交登录表单")),  // 语义重复
            createCase("TC3", "验证密码错误提示",
                Arrays.asList("输入正确用户名", "输入错误密码", "点击登录"))
        );

        // When
        List<TestCaseResponse> optimized = optimizationService.removeDuplicates(cases);

        // Then: TC1和TC2应该被识别为重复，保留其中一个
        assertEquals(2, optimized.size());
        assertTrue(optimized.stream().anyMatch(tc -> tc.getTitle().contains("密码错误")));
    }

    @Test
    @DisplayName("场景3.2: 按风险级别排序测试用例")
    void prioritizeCases_ByRiskLevel_ShouldSortCorrectly() {
        // Given
        List<TestCaseResponse> cases = Arrays.asList(
            createCaseWithPriority("TC1", 5),
            createCaseWithPriority("TC2", 9),
            createCaseWithPriority("TC3", 7)
        );

        // When
        List<TestCaseResponse> prioritized = optimizationService.prioritizeCases(cases);

        // Then: 按优先级降序排列
        assertEquals(9, prioritized.get(0).getPriority());
        assertEquals(7, prioritized.get(1).getPriority());
        assertEquals(5, prioritized.get(2).getPriority());
    }

    @Test
    @DisplayName("场景3.3: 计算测试覆盖率")
    void calculateCoverage_ShouldReturnPercentage() {
        // Given
        List<TestCaseResponse> cases = createCasesForModule("login");
        List<String> requirements = Arrays.asList("REQ-001", "REQ-002", "REQ-003");

        // When
        double coverage = optimizationService.calculateCoverage(cases, requirements);

        // Then
        assertTrue(coverage >= 0.0 && coverage <= 100.0);
    }

    @Test
    @DisplayName("场景3.4: 生成优化建议")
    void suggestImprovements_ShouldReturnRecommendations() {
        // Given
        List<TestCaseResponse> cases = Arrays.asList(
            createCase("TC1", "正常登录", Arrays.asList("步骤1")),
            createCase("TC2", "密码错误", Arrays.asList("步骤1"))
        );

        // When
        List<String> suggestions = optimizationService.suggestImprovements(cases);

        // Then
        assertFalse(suggestions.isEmpty());
        // 应该建议增加边界条件测试
        assertTrue(suggestions.stream()
            .anyMatch(s -> s.contains("边界") || s.contains("异常")));
    }
}
```

#### 3.1.2 Controller集成测试

**文件**: `backend/src/test/java/com/synapsetest/testmanagement/controller/TestCaseControllerIntegrationTest.java`

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase
@Sql(scripts = "/test-data-us2.sql")
@DisplayName("测试用例API集成测试")
class TestCaseControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @MockBean
    private AITestCaseGenerationService aiGenerationService;

    @Test
    @DisplayName("场景4.1: POST AI生成测试用例成功")
    void generateTestCases_ShouldReturn200WithCases() {
        // Given
        String requestBody = """
            {
                "requirementText": "用户登录功能：用户输入用户名密码进行验证...",
                "numberOfCases": 3,
                "testType": "FUNCTIONAL"
            }
            """;

        List<TestCaseResponse> mockCases = Arrays.asList(
            createMockTestCase("正常登录"),
            createMockTestCase("密码错误"),
            createMockTestCase("账户锁定")
        );
        when(aiGenerationService.generateTestCases(any())).thenReturn(mockCases);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // When
        ResponseEntity<ApiResponse> response = restTemplate.postForEntity(
            "/api/v1/test-cases/generate",
            new HttpEntity<>(requestBody, headers),
            ApiResponse.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().getSuccess());
        List<Map> cases = (List<Map>) response.getBody().getData();
        assertEquals(3, cases.size());
    }

    @Test
    @DisplayName("场景4.2: POST创建测试用例成功")
    void createTestCase_ShouldReturn201() {
        // Given
        String requestBody = """
            {
                "title": "验证用户登录成功",
                "description": "测试正常登录流程",
                "steps": ["打开登录页", "输入用户名", "输入密码", "点击登录"],
                "expectedResults": "登录成功跳转首页",
                "type": "FUNCTIONAL",
                "priority": 8,
                "tags": ["login", "smoke"]
            }
            """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-User-Name", "qa_engineer");

        // When
        ResponseEntity<ApiResponse> response = restTemplate.postForEntity(
            "/api/v1/test-cases",
            new HttpEntity<>(requestBody, headers),
            ApiResponse.class
        );

        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody().getData().get("id"));
        assertEquals("DRAFT", response.getBody().getData().get("status"));
    }

    @Test
    @DisplayName("场景4.3: PUT更新测试用例成功")
    void updateTestCase_ShouldReturn200() {
        // Given: 已存在的测试用例
        String caseId = "case-001";
        String requestBody = """
            {
                "title": "更新后的测试用例标题",
                "steps": ["新步骤1", "新步骤2", "新步骤3"],
                "expectedResults": "新的预期结果"
            }
            """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // When
        ResponseEntity<ApiResponse> response = restTemplate.exchange(
            "/api/v1/test-cases/" + caseId,
            HttpMethod.PUT,
            new HttpEntity<>(requestBody, headers),
            ApiResponse.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("更新后的测试用例标题", response.getBody().getData().get("title"));
    }

    @Test
    @DisplayName("场景4.4: POST优化用例集")
    void optimizeTestCases_ShouldReturnOptimizedList() {
        // Given
        String requestBody = """
            [
                {"id": "tc1", "title": "验证登录成功", "steps": ["步骤1"]},
                {"id": "tc2", "title": "测试登录功能", "steps": ["步骤1"]},
                {"id": "tc3", "title": "验证密码错误", "steps": ["步骤2"]}
            ]
            """;

        // When
        ResponseEntity<ApiResponse> response = restTemplate.postForEntity(
            "/api/v1/test-cases/optimize",
            new HttpEntity<>(requestBody, createJsonHeaders()),
            ApiResponse.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<Map> optimizedCases = (List<Map>) response.getBody().getData();
        assertTrue(optimizedCases.size() <= 3); // 去重后可能减少
    }

    @Test
    @DisplayName("场景4.5: DELETE删除测试用例成功")
    void deleteTestCase_ShouldReturn204() {
        // Given
        String caseId = "case-to-delete";

        // When
        ResponseEntity<Void> response = restTemplate.exchange(
            "/api/v1/test-cases/" + caseId,
            HttpMethod.DELETE,
            null,
            Void.class
        );

        // Then
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }
}
```

#### 3.1.3 Mapper层测试（JSON字段处理）

**文件**: `backend/src/test/java/com/synapsetest/testmanagement/mapper/TestCaseMapperTest.java`

```java
@MybatisTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@DisplayName("TestCase Mapper - JSON字段处理测试")
class TestCaseMapperTest {

    @Autowired
    private TestCaseMapper testCaseMapper;

    @Test
    @DisplayName("场景5.1: 插入包含JSON字段的测试用例")
    void insert_WithJsonFields_ShouldHandleCorrectly() {
        // Given
        TestCase testCase = new TestCase();
        testCase.setId(UUID.randomUUID().toString());
        testCase.setTitle("JSON测试");
        testCase.setSteps(Arrays.asList("步骤1", "步骤2", "步骤3"));
        testCase.setTags(Arrays.asList("tag1", "tag2"));
        testCase.setExpectedResults("预期结果");
        testCase.setType("FUNCTIONAL");
        testCase.setStatus("DRAFT");
        testCase.setPriority(5);
        testCase.setCreatedBy("tester");
        testCase.setCreatedAt(LocalDateTime.now());
        testCase.setUpdatedAt(LocalDateTime.now());

        // When
        int result = testCaseMapper.insert(testCase);

        // Then
        assertEquals(1, result);
        Optional<TestCase> saved = testCaseMapper.selectById(testCase.getId());
        assertTrue(saved.isPresent());
        assertEquals(3, saved.get().getSteps().size());
        assertEquals("步骤1", saved.get().getSteps().get(0));
        assertEquals(2, saved.get().getTags().size());
    }

    @Test
    @DisplayName("场景5.2: 查询时正确反序列化JSON")
    void selectById_ShouldDeserializeJson() {
        // Given: 数据库中已存在的记录

        // When
        Optional<TestCase> testCase = testCaseMapper.selectById("case-with-json");

        // Then
        assertTrue(testCase.isPresent());
        assertInstanceOf(List.class, testCase.get().getSteps());
        assertInstanceOf(List.class, testCase.get().getTags());
    }

    @Test
    @DisplayName("场景5.3: 更新JSON字段")
    void update_ShouldUpdateJsonFields() {
        // Given
        TestCase testCase = testCaseMapper.selectById("case-001").orElseThrow();
        testCase.setSteps(Arrays.asList("新步骤1", "新步骤2"));
        testCase.setUpdatedAt(LocalDateTime.now());

        // When
        int result = testCaseMapper.update(testCase);

        // Then
        assertEquals(1, result);
        TestCase updated = testCaseMapper.selectById("case-001").orElseThrow();
        assertEquals(2, updated.getSteps().size());
        assertEquals("新步骤1", updated.getSteps().get(0));
    }
}
```

---

### 3.2 Frontend 测试

#### 3.2.1 AI用例生成组件测试

**文件**: `frontend/src/components/test-case/__tests__/AITestCaseGeneration.test.tsx`

```typescript
import { render, screen, waitFor, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import AITestCaseGeneration from '../AITestCaseGeneration';
import testCaseService from '../../../services/testCaseService';

jest.mock('../../../services/testCaseService');

describe('AI生成测试用例场景', () => {

  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('场景1.1: 初始状态 - 生成按钮禁用', () => {
    // Given & When
    render(<AITestCaseGeneration />);

    // Then
    expect(screen.getByRole('button', { name: '生成用例' })).toBeDisabled();
    expect(screen.getByPlaceholderText(/输入需求描述/)).toHaveValue('');
  });

  test('场景1.2: 输入需求文本后生成按钮启用', async () => {
    // Given
    render(<AITestCaseGeneration />);

    // When: 输入足够长的需求描述
    const textarea = screen.getByPlaceholderText(/输入需求描述/);
    await userEvent.type(textarea, '用户登录功能：用户输入用户名和密码进行身份验证，验证成功后跳转首页...');

    // Then
    expect(screen.getByRole('button', { name: '生成用例' })).toBeEnabled();
    expect(screen.getByText(/字符数:/)).toBeInTheDocument();
  });

  test('场景1.3: 点击生成按钮显示加载状态', async () => {
    // Given
    (testCaseService.generateTestCases as jest.Mock).mockImplementation(
      () => new Promise(resolve => setTimeout(resolve, 1000))
    );

    render(<AITestCaseGeneration />);
    await userEvent.type(
      screen.getByPlaceholderText(/输入需求描述/),
      '用户登录功能需求描述...'
    );

    // When
    await userEvent.click(screen.getByRole('button', { name: '生成用例' }));

    // Then
    expect(screen.getByText(/AI正在分析需求/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '生成用例' })).toBeDisabled();
  });

  test('场景1.4: AI生成成功后显示用例列表', async () => {
    // Given
    const mockGeneratedCases = [
      {
        id: 'gen-001',
        title: '正常登录验证',
        steps: ['输入用户名', '输入密码', '点击登录'],
        expectedResults: '登录成功',
        priority: 8
      },
      {
        id: 'gen-002',
        title: '密码错误验证',
        steps: ['输入用户名', '输入错误密码', '点击登录'],
        expectedResults: '显示错误提示',
        priority: 7
      }
    ];

    (testCaseService.generateTestCases as jest.Mock).mockResolvedValue(mockGeneratedCases);

    render(<AITestCaseGeneration />);
    await userEvent.type(
      screen.getByPlaceholderText(/输入需求描述/),
      '用户登录功能需求...'
    );

    // When
    await userEvent.click(screen.getByRole('button', { name: '生成用例' }));

    // Then
    await waitFor(() => {
      expect(screen.getByText('正常登录验证')).toBeInTheDocument();
      expect(screen.getByText('密码错误验证')).toBeInTheDocument();
      expect(screen.getByText(/共生成 2 个用例/)).toBeInTheDocument();
    });
  });

  test('场景1.5: 点击用例可展开查看详情', async () => {
    // Given
    const mockCase = {
      id: 'gen-001',
      title: '正常登录验证',
      steps: ['步骤1', '步骤2'],
      expectedResults: '预期结果'
    };
    (testCaseService.generateTestCases as jest.Mock).mockResolvedValue([mockCase]);

    render(<AITestCaseGeneration />);
    await userEvent.type(screen.getByPlaceholderText(/输入需求描述/), '需求...');
    await userEvent.click(screen.getByRole('button', { name: '生成用例' }));

    // When: 点击展开
    await waitFor(() => {
      expect(screen.getByText('正常登录验证')).toBeInTheDocument();
    });
    await userEvent.click(screen.getByText('正常登录验证'));

    // Then
    await waitFor(() => {
      expect(screen.getByText('步骤1')).toBeInTheDocument();
      expect(screen.getByText('预期结果')).toBeInTheDocument();
    });
  });

  test('场景1.6: 编辑生成的用例', async () => {
    // Given
    const mockCase = {
      id: 'gen-001',
      title: '原标题',
      steps: ['步骤1'],
      expectedResults: '预期结果'
    };
    (testCaseService.generateTestCases as jest.Mock).mockResolvedValue([mockCase]);

    render(<AITestCaseGeneration />);
    await userEvent.type(screen.getByPlaceholderText(/输入需求描述/), '需求...');
    await userEvent.click(screen.getByRole('button', { name: '生成用例' }));

    await waitFor(() => {
      expect(screen.getByText('原标题')).toBeInTheDocument();
    });

    // When: 点击编辑按钮
    await userEvent.click(screen.getByRole('button', { name: '编辑' }));

    // Then: 显示编辑对话框
    await waitFor(() => {
      expect(screen.getByRole('dialog')).toBeInTheDocument();
      expect(screen.getByDisplayValue('原标题')).toBeInTheDocument();
    });
  });

  test('场景1.7: 批量保存选中的用例', async () => {
    // Given
    const mockCases = [
      { id: 'gen-001', title: '用例1', steps: ['步骤'], expectedResults: '结果' },
      { id: 'gen-002', title: '用例2', steps: ['步骤'], expectedResults: '结果' }
    ];
    (testCaseService.generateTestCases as jest.Mock).mockResolvedValue(mockCases);
    (testCaseService.batchSave as jest.Mock).mockResolvedValue({ saved: 2 });

    render(<AITestCaseGeneration />);
    await userEvent.type(screen.getByPlaceholderText(/输入需求描述/), '需求...');
    await userEvent.click(screen.getByRole('button', { name: '生成用例' }));

    await waitFor(() => {
      expect(screen.getByText('用例1')).toBeInTheDocument();
    });

    // When: 全选并保存
    await userEvent.click(screen.getByRole('checkbox', { name: '全选' }));
    await userEvent.click(screen.getByRole('button', { name: '保存选中' }));

    // Then
    await waitFor(() => {
      expect(screen.getByText(/成功保存 2 个用例/)).toBeInTheDocument();
    });
    expect(testCaseService.batchSave).toHaveBeenCalledWith(mockCases);
  });

  test('场景1.8: AI服务失败显示错误消息', async () => {
    // Given
    (testCaseService.generateTestCases as jest.Mock).mockRejectedValue(
      new Error('AI服务暂不可用')
    );

    render(<AITestCaseGeneration />);
    await userEvent.type(screen.getByPlaceholderText(/输入需求描述/), '需求...');

    // When
    await userEvent.click(screen.getByRole('button', { name: '生成用例' }));

    // Then
    await waitFor(() => {
      expect(screen.getByText(/生成失败/)).toBeInTheDocument();
    });
  });

  test('场景1.9: 需求文本太短时显示提示', async () => {
    // Given
    render(<AITestCaseGeneration />);

    // When: 输入太短的文本
    await userEvent.type(screen.getByPlaceholderText(/输入需求描述/), '短文本');

    // Then
    expect(screen.getByText(/请输入更详细的需求描述/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '生成用例' })).toBeDisabled();
  });
});
```

#### 3.2.2 测试用例列表组件测试

**文件**: `frontend/src/components/test-case/__tests__/TestCaseList.test.tsx`

```typescript
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import TestCaseList from '../TestCaseList';
import testCaseService from '../../../services/testCaseService';

jest.mock('../../../services/testCaseService');

describe('测试用例列表场景', () => {

  const mockTestCases = [
    {
      id: 'case-001',
      title: '验证用户登录成功',
      type: 'FUNCTIONAL',
      status: 'APPROVED',
      priority: 8,
      tags: ['login', 'smoke'],
      createdBy: 'qa_engineer',
      createdAt: '2025-11-16T10:00:00Z'
    },
    {
      id: 'case-002',
      title: '验证密码错误提示',
      type: 'FUNCTIONAL',
      status: 'DRAFT',
      priority: 7,
      tags: ['login', 'negative'],
      createdBy: 'qa_engineer',
      createdAt: '2025-11-16T11:00:00Z'
    }
  ];

  beforeEach(() => {
    (testCaseService.getAllTestCases as jest.Mock).mockResolvedValue(mockTestCases);
  });

  test('场景2.1: 正确渲染用例列表', async () => {
    render(<TestCaseList />);

    await waitFor(() => {
      expect(screen.getByText('验证用户登录成功')).toBeInTheDocument();
      expect(screen.getByText('验证密码错误提示')).toBeInTheDocument();
    });
  });

  test('场景2.2: 显示用例状态标签', async () => {
    render(<TestCaseList />);

    await waitFor(() => {
      expect(screen.getByText('APPROVED')).toBeInTheDocument();
      expect(screen.getByText('DRAFT')).toBeInTheDocument();
    });
  });

  test('场景2.3: 按类型筛选用例', async () => {
    render(<TestCaseList />);

    await waitFor(() => {
      expect(screen.getAllByRole('row')).toHaveLength(3); // header + 2 rows
    });

    // When: 选择筛选条件
    await userEvent.selectOptions(screen.getByLabelText('类型'), 'FUNCTIONAL');

    // Then: 应该只显示FUNCTIONAL类型的用例
    await waitFor(() => {
      const rows = screen.getAllByRole('row');
      expect(rows.length).toBeGreaterThan(1);
    });
  });

  test('场景2.4: 删除用例显示确认对话框', async () => {
    (testCaseService.deleteTestCase as jest.Mock).mockResolvedValue({});

    render(<TestCaseList />);

    await waitFor(() => {
      expect(screen.getByText('验证用户登录成功')).toBeInTheDocument();
    });

    // When: 点击删除按钮
    const deleteButtons = screen.getAllByRole('button', { name: '删除' });
    await userEvent.click(deleteButtons[0]);

    // Then: 显示确认对话框
    await waitFor(() => {
      expect(screen.getByText(/确认删除/)).toBeInTheDocument();
    });
  });
});
```

#### 3.2.3 Service层测试

**文件**: `frontend/src/services/__tests__/testCaseService.test.ts`

```typescript
import axios from 'axios';
import testCaseService from '../testCaseService';

jest.mock('axios');
const mockedAxios = axios as jest.Mocked<typeof axios>;

describe('testCaseService - AI生成测试用例', () => {

  test('场景3.1: generateTestCases调用AI生成API', async () => {
    // Given
    const request = {
      requirementText: '用户登录功能需求...',
      numberOfCases: 5,
      testType: 'FUNCTIONAL'
    };

    mockedAxios.post.mockResolvedValue({
      data: { success: true, data: [{ id: 'gen-001', title: '用例1' }] }
    });

    // When
    await testCaseService.generateTestCases(request);

    // Then
    expect(mockedAxios.post).toHaveBeenCalledWith(
      '/api/v1/test-cases/generate',
      request
    );
  });

  test('场景3.2: batchSave批量保存用例', async () => {
    // Given
    const cases = [
      { title: '用例1', steps: ['步骤1'] },
      { title: '用例2', steps: ['步骤2'] }
    ];

    mockedAxios.post.mockResolvedValue({
      data: { success: true, data: { saved: 2 } }
    });

    // When
    const result = await testCaseService.batchSave(cases);

    // Then
    expect(result.saved).toBe(2);
  });

  test('场景3.3: 验证用例数据格式', () => {
    // Given
    const testCase = {
      title: '',
      steps: []
    };

    // When & Then
    expect(() => testCaseService.validateTestCase(testCase))
      .toThrow('标题和步骤不能为空');
  });
});
```

---

### 3.3 AI-Service 测试

**文件**: `ai-service/tests/test_testcase_generation.py`

```python
import pytest
from fastapi.testclient import TestClient
from unittest.mock import patch, MagicMock
from main import app
from models.llm_model import TestCaseGenerator

client = TestClient(app)

class TestAITestCaseGenerationAPI:
    """AI测试用例生成API测试"""

    def test_scenario_1_generate_cases_with_valid_input(self):
        """场景1: 有效需求输入成功生成测试用例"""
        # Given
        request_data = {
            "requirement_text": """
                用户登录功能：
                1. 用户输入用户名和密码
                2. 系统验证凭据
                3. 成功跳转首页，失败显示错误
                4. 支持记住我功能
            """,
            "num_cases": 4,
            "test_type": "FUNCTIONAL"
        }

        # When
        response = client.post("/api/v1/ai/testcase/generate", json=request_data)

        # Then
        assert response.status_code == 200
        data = response.json()
        assert len(data["test_cases"]) == 4

        # 验证每个用例格式
        for case in data["test_cases"]:
            assert "title" in case
            assert "steps" in case
            assert len(case["steps"]) > 0
            assert "expected_results" in case

    def test_scenario_2_empty_requirement_returns_400(self):
        """场景2: 空需求文本返回400错误"""
        # Given
        request_data = {
            "requirement_text": "",
            "num_cases": 3
        }

        # When
        response = client.post("/api/v1/ai/testcase/generate", json=request_data)

        # Then
        assert response.status_code == 400
        assert "需求描述不能为空" in response.json()["detail"]

    def test_scenario_3_requirement_too_short_returns_400(self):
        """场景3: 需求文本太短返回400"""
        # Given
        request_data = {
            "requirement_text": "简短描述",
            "num_cases": 3
        }

        # When
        response = client.post("/api/v1/ai/testcase/generate", json=request_data)

        # Then
        assert response.status_code == 400
        assert "需求描述过于简短" in response.json()["detail"]

    def test_scenario_4_too_many_cases_returns_400(self):
        """场景4: 请求生成过多用例返回400"""
        # Given
        request_data = {
            "requirement_text": "有效的需求描述...",
            "num_cases": 100  # 超过限制
        }

        # When
        response = client.post("/api/v1/ai/testcase/generate", json=request_data)

        # Then
        assert response.status_code == 400
        assert "用例数量超过限制" in response.json()["detail"]

    @patch('models.llm_model.TestCaseGenerator.generate')
    def test_scenario_5_ai_model_timeout(self, mock_generate):
        """场景5: AI模型超时返回504"""
        # Given
        mock_generate.side_effect = TimeoutError("Model inference timeout")

        request_data = {
            "requirement_text": "用户登录功能需求...",
            "num_cases": 3
        }

        # When
        response = client.post("/api/v1/ai/testcase/generate", json=request_data)

        # Then
        assert response.status_code == 504
        assert "生成超时" in response.json()["detail"]

    def test_scenario_6_health_check(self):
        """场景6: 健康检查接口"""
        response = client.get("/health")
        assert response.status_code == 200
        assert response.json()["status"] == "healthy"


class TestLLMModelUnit:
    """大语言模型单元测试"""

    @patch('transformers.AutoTokenizer.from_pretrained')
    @patch('transformers.AutoModelForCausalLM.from_pretrained')
    def test_scenario_7_parse_model_response(self, mock_model, mock_tokenizer):
        """场景7: 解析LLM响应提取测试用例"""
        # Given
        generator = TestCaseGenerator.__new__(TestCaseGenerator)
        llm_response = """
        测试用例1: 正常登录
        步骤: 输入用户名 | 输入密码 | 点击登录
        预期结果: 成功登录

        测试用例2: 密码错误
        步骤: 输入用户名 | 输入错误密码
        预期结果: 显示错误提示
        """

        # When
        cases = generator._parse_testcases(llm_response)

        # Then
        assert len(cases) == 2
        assert cases[0]["title"] == "正常登录"
        assert len(cases[0]["steps"]) == 3
        assert cases[1]["title"] == "密码错误"

    def test_scenario_8_handle_malformed_response(self):
        """场景8: 处理格式不规范的响应"""
        # Given
        generator = TestCaseGenerator.__new__(TestCaseGenerator)
        malformed_response = "这不是一个有效的测试用例格式"

        # When
        cases = generator._parse_testcases(malformed_response)

        # Then
        assert len(cases) == 0  # 无法解析返回空列表


class TestOptimizationService:
    """用例优化服务测试"""

    def test_scenario_9_remove_duplicates(self):
        """场景9: 去除语义重复用例"""
        # Given
        from services.optimization import OptimizationService
        service = OptimizationService()

        cases = [
            {"title": "验证登录成功", "steps": ["输入用户名", "输入密码", "点击登录"]},
            {"title": "测试登录功能", "steps": ["填写用户名", "填写密码", "提交表单"]},
            {"title": "验证密码错误", "steps": ["输入用户名", "输入错误密码"]}
        ]

        # When
        optimized = service.remove_duplicates(cases, threshold=0.85)

        # Then
        assert len(optimized) == 2  # 前两个重复，保留一个

    def test_scenario_10_calculate_similarity(self):
        """场景10: 计算两个用例的语义相似度"""
        from services.optimization import OptimizationService
        service = OptimizationService()

        case1 = "验证用户使用正确密码登录成功"
        case2 = "测试正确凭据登录功能正常"

        similarity = service.calculate_similarity(case1, case2)

        assert 0.7 < similarity < 1.0  # 高相似度
```

---

## 四、测试数据

**文件**: `backend/src/test/resources/test-data-us2.sql`

```sql
-- User Story 2 测试数据
INSERT INTO test_cases (id, title, description, steps, expected_results, type, status, priority, tags, created_by, created_at, updated_at) VALUES
('case-001', '验证用户登录成功', '测试正常登录流程', '["打开登录页","输入用户名","输入密码","点击登录"]', '登录成功跳转首页', 'FUNCTIONAL', 'APPROVED', 8, '["login","smoke"]', 'qa_engineer', NOW(), NOW()),
('case-002', '验证密码错误提示', '测试密码错误场景', '["打开登录页","输入用户名","输入错误密码","点击登录"]', '显示密码错误提示', 'FUNCTIONAL', 'DRAFT', 7, '["login","negative"]', 'qa_engineer', NOW(), NOW()),
('case-with-json', 'JSON字段测试', '测试JSON序列化', '["步骤1","步骤2","步骤3"]', '预期结果', 'FUNCTIONAL', 'DRAFT', 5, '["test"]', 'tester', NOW(), NOW()),
('case-to-delete', '待删除用例', '用于测试删除功能', '["步骤"]', '结果', 'FUNCTIONAL', 'DRAFT', 3, '[]', 'tester', NOW(), NOW());
```

---

## 五、验收标准

| 测试场景 | 工程 | 验收条件 | 优先级 |
|---------|------|---------|-------|
| AI生成测试用例 | AI-Service | 返回指定数量用例，格式完整（标题、步骤、预期结果） | P0 |
| 用例覆盖关键场景 | AI-Service | 生成的用例覆盖正常流程、异常场景、边界条件 | P0 |
| 前端生成交互 | Frontend | 输入需求→显示加载→展示结果→支持编辑保存 | P0 |
| 用例CRUD操作 | Backend | 创建、读取、更新、删除均成功 | P0 |
| JSON字段处理 | Backend | steps和tags字段正确序列化/反序列化 | P1 |
| 用例去重优化 | AI-Service | 识别语义重复用例，相似度>0.85视为重复 | P1 |
| 错误处理 | 全部 | 空输入400，AI超时504，资源不存在404 | P1 |

---

## 六、执行顺序

1. **Red阶段**: 编写失败的测试用例
2. **Green阶段**: 实现最小代码使测试通过
3. **Refactor阶段**: 优化代码，保持测试绿色

**建议执行路径**:
```
AI-Service LLM解析测试 → Backend Service测试 → Mapper JSON处理测试 → Controller集成测试 → Frontend组件测试
```
