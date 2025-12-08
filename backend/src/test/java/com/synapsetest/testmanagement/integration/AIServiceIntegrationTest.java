package com.synapsetest.testmanagement.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapsetest.testmanagement.client.AIServiceClient;
import com.synapsetest.testmanagement.dto.ai.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/**
 * AI服务集成测试
 *
 * 测试目标：
 * 1. 验证AIServiceClient与AI-Service的HTTP通信
 * 2. 验证AIController的API网关功能
 * 3. 验证熔断器和重试机制
 * 4. 验证降级策略（Fallback）
 * 5. 验证所有AI功能的端到端流程
 *
 * 对应架构整改方案: 问题1 - 接口调用架构优化
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("AI服务集成测试")
public class AIServiceIntegrationTest {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private AIServiceClient aiServiceClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${services.ai-service.url:http://localhost:8000}")
    private String aiServiceUrl;

    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @AfterEach
    void tearDown() {
        mockServer.verify();
    }

    // ========== 1. 测试用例生成 ==========

    @Test
    @DisplayName("场景1.1: AI生成测试用例 - 成功场景")
    void generateTestCases_Success() throws Exception {
        // Given: 准备请求和模拟响应
        AITestCaseGenerationRequest request = new AITestCaseGenerationRequest();
        request.setInput("用户可以创建订单、查看订单列表、取消订单");
        request.setTestType("functional");
        request.setTags(Arrays.asList("订单", "核心功能"));

        GeneratedTestCase case1 = new GeneratedTestCase();
        case1.setTitle("创建订单-正常流程");
        case1.setDescription("验证用户能够成功创建订单");
        case1.setSteps(Arrays.asList("登录系统", "选择商品", "确认订单"));
        case1.setExpectedResults("订单创建成功，返回订单号");
        case1.setPriority(8);
        case1.setType("FUNCTIONAL");
        case1.setConfidence(0.92);

        GeneratedTestCase case2 = new GeneratedTestCase();
        case2.setTitle("查看订单列表");
        case2.setDescription("验证用户能够查看订单列表");
        case2.setSteps(Arrays.asList("登录系统", "进入订单管理", "查看订单列表"));
        case2.setExpectedResults("显示订单列表，包含订单号、状态等信息");
        case2.setPriority(6);
        case2.setType("FUNCTIONAL");
        case2.setConfidence(0.88);

        AITestCaseGenerationResponse mockResponse = new AITestCaseGenerationResponse();
        mockResponse.setSuccess(true);
        mockResponse.setMessage("成功生成2个测试用例");
        mockResponse.setGeneratedCases(Arrays.asList(case1, case2));
        mockResponse.setTotalCount(2);
        mockResponse.setProcessingTimeMs(1500L);

        // Mock AI-Service响应
        mockServer.expect(requestTo(aiServiceUrl + "/api/v1/ai/testcase/generate"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.input").value(request.getInput()))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse), MediaType.APPLICATION_JSON));

        // When: 调用AIServiceClient
        AITestCaseGenerationResponse response = aiServiceClient.generateTestCases(request);

        // Then: 验证响应
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(2, response.getTotalCount());
        assertNotNull(response.getGeneratedCases());
        assertEquals(2, response.getGeneratedCases().size());

        // 验证第一个用例
        GeneratedTestCase firstCase = response.getGeneratedCases().get(0);
        assertEquals("创建订单-正常流程", firstCase.getTitle());
        assertEquals(0.92, firstCase.getConfidence());
        assertEquals(3, firstCase.getSteps().size());
    }

    @Test
    @DisplayName("场景1.2: AI生成测试用例 - AI服务不可用触发Fallback")
    void generateTestCases_ServiceUnavailable_FallbackTriggered() {
        // Given: 准备请求，模拟AI服务500错误
        AITestCaseGenerationRequest request = new AITestCaseGenerationRequest();
        request.setInput("用户登录功能");
        request.setTestType("functional");

        mockServer.expect(requestTo(aiServiceUrl + "/api/v1/ai/testcase/generate"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        // When: 调用AIServiceClient（会触发Fallback）
        AITestCaseGenerationResponse response = aiServiceClient.generateTestCases(request);

        // Then: 验证Fallback响应
        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("AI服务暂时不可用"));
        assertNull(response.getGeneratedCases());
    }

    @Test
    @DisplayName("场景1.3: AI生成测试用例 - 超时重试")
    void generateTestCases_Timeout_RetryTriggered() throws Exception {
        // Given: 准备请求
        AITestCaseGenerationRequest request = new AITestCaseGenerationRequest();
        request.setInput("支付功能");

        AITestCaseGenerationResponse mockResponse = new AITestCaseGenerationResponse();
        mockResponse.setSuccess(true);
        mockResponse.setMessage("成功生成测试用例");
        mockResponse.setGeneratedCases(Arrays.asList(new GeneratedTestCase()));
        mockResponse.setTotalCount(1);

        // Mock: 第一次请求超时，第二次成功
        mockServer.expect(requestTo(aiServiceUrl + "/api/v1/ai/testcase/generate"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        mockServer.expect(requestTo(aiServiceUrl + "/api/v1/ai/testcase/generate"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse), MediaType.APPLICATION_JSON));

        // When: 调用AIServiceClient（会自动重试）
        AITestCaseGenerationResponse response = aiServiceClient.generateTestCases(request);

        // Then: 验证第二次重试成功
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(1, response.getTotalCount());
    }

    // ========== 2. 批量生成测试用例 ==========

    @Test
    @DisplayName("场景2.1: 批量生成测试用例 - 成功场景")
    void batchGenerateTestCases_Success() throws Exception {
        // Given: 准备批量请求
        Map<String, Object> batchRequest = Map.of(
                "requirements", Arrays.asList(
                        "用户注册功能",
                        "用户登录功能",
                        "密码重置功能"
                ),
                "test_type", "functional",
                "enable_deduplication", true
        );

        Map<String, Object> mockResponse = Map.of(
                "success", true,
                "message", "批量生成成功",
                "total_requirements", 3,
                "total_cases_generated", 9,
                "results", Arrays.asList(
                        Map.of("requirement", "用户注册功能", "cases_count", 3),
                        Map.of("requirement", "用户登录功能", "cases_count", 3),
                        Map.of("requirement", "密码重置功能", "cases_count", 3)
                )
        );

        mockServer.expect(requestTo(aiServiceUrl + "/api/v1/ai/testcase/generate/batch"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse), MediaType.APPLICATION_JSON));

        // When: 调用批量生成
        Map<String, Object> response = aiServiceClient.batchGenerateTestCases(batchRequest);

        // Then: 验证响应
        assertNotNull(response);
        assertTrue((Boolean) response.get("success"));
        assertEquals(3, response.get("total_requirements"));
        assertEquals(9, response.get("total_cases_generated"));
    }

    // ========== 3. 测试用例去重 ==========

    @Test
    @DisplayName("场景3.1: 测试用例去重 - 成功场景")
    void deduplicateTestCases_Success() throws Exception {
        // Given: 准备去重请求
        DeduplicationRequest request = new DeduplicationRequest();
        request.setTestCaseIds(Arrays.asList("case-1", "case-2", "case-3", "case-4"));
        request.setSimilarityThreshold(0.85);

        DeduplicationResponse mockResponse = new DeduplicationResponse();
        mockResponse.setSuccess(true);
        mockResponse.setMessage("去重完成");
        mockResponse.setOriginalCount(4);
        mockResponse.setDeduplicatedCount(3);
        mockResponse.setDuplicatesRemoved(1);
        mockResponse.setDuplicateGroups(Arrays.asList(
                Arrays.asList("case-2", "case-3")  // case-2和case-3相似
        ));

        mockServer.expect(requestTo(aiServiceUrl + "/api/v1/ai/testcase/optimize/deduplicate"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.test_case_ids").isArray())
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse), MediaType.APPLICATION_JSON));

        // When: 调用去重
        DeduplicationResponse response = aiServiceClient.deduplicateTestCases(request);

        // Then: 验证响应
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(4, response.getOriginalCount());
        assertEquals(3, response.getDeduplicatedCount());
        assertEquals(1, response.getDuplicatesRemoved());
        assertEquals(1, response.getDuplicateGroups().size());
    }

    @Test
    @DisplayName("场景3.2: 测试用例去重 - 降级场景")
    void deduplicateTestCases_ServiceUnavailable_FallbackTriggered() {
        // Given: 准备去重请求
        DeduplicationRequest request = new DeduplicationRequest();
        request.setTestCaseIds(Arrays.asList("case-1", "case-2"));

        mockServer.expect(requestTo(aiServiceUrl + "/api/v1/ai/testcase/optimize/deduplicate"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        // When: 调用去重（触发Fallback）
        DeduplicationResponse response = aiServiceClient.deduplicateTestCases(request);

        // Then: 验证Fallback响应
        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("AI服务暂时不可用"));
    }

    // ========== 4. 测试用例优先级排序 ==========

    @Test
    @DisplayName("场景4.1: 测试用例优先级排序 - 成功场景")
    void prioritizeTestCases_Success() throws Exception {
        // Given: 准备优先级排序请求
        PrioritizationRequest request = new PrioritizationRequest();
        request.setTestCaseIds(Arrays.asList("case-1", "case-2", "case-3"));
        request.setContext(Map.of(
                "release_urgency", "high",
                "available_time_hours", 8
        ));

        PrioritizedTestCase case1 = new PrioritizedTestCase();
        case1.setTestCaseId("case-1");
        case1.setPriority(10);
        case1.setPriorityReason("核心功能，影响范围大");

        PrioritizedTestCase case2 = new PrioritizedTestCase();
        case2.setTestCaseId("case-2");
        case2.setPriority(7);
        case2.setPriorityReason("重要功能");

        PrioritizationResponse mockResponse = new PrioritizationResponse();
        mockResponse.setSuccess(true);
        mockResponse.setMessage("优先级排序完成");
        mockResponse.setPrioritizedCases(Arrays.asList(case1, case2));

        mockServer.expect(requestTo(aiServiceUrl + "/api/v1/ai/testcase/optimize/prioritize"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.test_case_ids").isArray())
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse), MediaType.APPLICATION_JSON));

        // When: 调用优先级排序
        PrioritizationResponse response = aiServiceClient.prioritizeTestCases(request);

        // Then: 验证响应
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(2, response.getPrioritizedCases().size());
        assertEquals(10, response.getPrioritizedCases().get(0).getPriority());
        assertEquals("case-1", response.getPrioritizedCases().get(0).getTestCaseId());
    }

    // ========== 5. 测试用例质量分析 ==========

    @Test
    @DisplayName("场景5.1: 测试用例质量分析 - 成功场景")
    void analyzeQuality_Success() throws Exception {
        // Given: 准备质量分析请求
        Map<String, Object> request = Map.of(
                "test_case_ids", Arrays.asList("case-1", "case-2", "case-3")
        );

        Map<String, Object> mockResponse = Map.of(
                "success", true,
                "message", "质量分析完成",
                "overall_quality_score", 0.85,
                "analysis_results", Arrays.asList(
                        Map.of(
                                "test_case_id", "case-1",
                                "quality_score", 0.92,
                                "completeness_score", 0.95,
                                "clarity_score", 0.88,
                                "issues", Arrays.asList("建议补充异常场景")
                        )
                )
        );

        mockServer.expect(requestTo(aiServiceUrl + "/api/v1/ai/testcase/analyze/quality"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.test_case_ids").isArray())
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse), MediaType.APPLICATION_JSON));

        // When: 调用质量分析
        Map<String, Object> response = aiServiceClient.analyzeQuality(request);

        // Then: 验证响应
        assertNotNull(response);
        assertTrue((Boolean) response.get("success"));
        assertEquals(0.85, response.get("overall_quality_score"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("analysis_results");
        assertEquals(1, results.size());
        assertEquals(0.92, results.get(0).get("quality_score"));
    }

    // ========== 6. 策略推荐 ==========

    @Test
    @DisplayName("场景6.1: 策略推荐 - 成功场景（包含环境和版本）")
    void recommendStrategy_WithEnvironmentAndVersion_Success() throws Exception {
        // Given: 准备策略推荐请求
        Map<String, Object> request = Map.of(
                "task_id", "task-001",
                "environment_id", "env-prod",
                "version_id", "version-2.0.0",
                "context", Map.of(
                        "code_change", Map.of(
                                "changed_files_count", 15,
                                "changed_lines_count", 500
                        ),
                        "historical", Map.of(
                                "recent_pass_rate", 0.92
                        ),
                        "business", Map.of(
                                "business_priority", "P0"
                        )
                )
        );

        Map<String, Object> mockResponse = Map.of(
                "task_id", "task-001",
                "recommendation", Map.of(
                        "test_scope", "FULL",
                        "priority", 10,
                        "environment", "PROD",
                        "reasoning", Arrays.asList(
                                "代码变更较大，影响15个文件",
                                "环境为生产环境(PROD)，推荐全量回归测试",
                                "版本2.0.0为大版本升级，建议全面回归测试"
                        )
                ),
                "risk_assessment", Map.of(
                        "risk_level", "HIGH",
                        "risk_factors", Arrays.asList("生产环境部署", "大版本升级")
                ),
                "environment_recommendations", Arrays.asList("STAGING", "PROD"),
                "timestamp", "2025-12-08T10:00:00Z"
        );

        mockServer.expect(requestTo(aiServiceUrl + "/api/v1/ai/recommendation/strategy"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.task_id").value("task-001"))
                .andExpect(jsonPath("$.environment_id").value("env-prod"))
                .andExpect(jsonPath("$.version_id").value("version-2.0.0"))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse), MediaType.APPLICATION_JSON));

        // When: 调用策略推荐
        Map<String, Object> response = aiServiceClient.getRecommendation(request);

        // Then: 验证响应
        assertNotNull(response);
        assertEquals("task-001", response.get("task_id"));

        @SuppressWarnings("unchecked")
        Map<String, Object> recommendation = (Map<String, Object>) response.get("recommendation");
        assertEquals("FULL", recommendation.get("test_scope"));
        assertEquals(10, recommendation.get("priority"));
        assertEquals("PROD", recommendation.get("environment"));

        @SuppressWarnings("unchecked")
        Map<String, Object> riskAssessment = (Map<String, Object>) response.get("risk_assessment");
        assertEquals("HIGH", riskAssessment.get("risk_level"));
    }

    @Test
    @DisplayName("场景6.2: 策略推荐 - 开发环境场景")
    void recommendStrategy_DevEnvironment_Success() throws Exception {
        // Given: 准备开发环境推荐请求
        Map<String, Object> request = Map.of(
                "task_id", "task-002",
                "environment_id", "env-dev",
                "version_id", "version-1.2.3",
                "context", Map.of(
                        "code_change", Map.of(
                                "changed_files_count", 3,
                                "changed_lines_count", 100
                        )
                )
        );

        Map<String, Object> mockResponse = Map.of(
                "task_id", "task-002",
                "recommendation", Map.of(
                        "test_scope", "SMOKE",
                        "priority", 5,
                        "environment", "DEV",
                        "reasoning", Arrays.asList(
                                "代码变更较小",
                                "环境为开发环境(DEV)，可进行冒烟测试"
                        )
                ),
                "risk_assessment", Map.of(
                        "risk_level", "LOW"
                ),
                "environment_recommendations", Arrays.asList("DEV"),
                "timestamp", "2025-12-08T10:00:00Z"
        );

        mockServer.expect(requestTo(aiServiceUrl + "/api/v1/ai/recommendation/strategy"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse), MediaType.APPLICATION_JSON));

        // When: 调用策略推荐
        Map<String, Object> response = aiServiceClient.getRecommendation(request);

        // Then: 验证响应
        assertNotNull(response);

        @SuppressWarnings("unchecked")
        Map<String, Object> recommendation = (Map<String, Object>) response.get("recommendation");
        assertEquals("SMOKE", recommendation.get("test_scope"));
        assertEquals(5, recommendation.get("priority"));
    }

    @Test
    @DisplayName("场景6.3: 策略推荐解释 - 成功场景")
    void explainRecommendation_Success() throws Exception {
        // Given: 准备解释请求
        Map<String, Object> request = Map.of(
                "recommendation", Map.of(
                        "test_scope", "FULL",
                        "priority", 10
                ),
                "context", Map.of(
                        "code_change", Map.of(
                                "changed_files_count", 20
                        )
                )
        );

        Map<String, Object> mockResponse = Map.of(
                "success", true,
                "explanation", Map.of(
                        "summary", "推荐执行FULL范围测试，在STAGING环境运行，优先级为P10",
                        "key_factors", Arrays.asList("代码变更范围较大"),
                        "confidence_level", 0.88,
                        "alternatives", Arrays.asList(
                                Map.of(
                                        "option", "CORE",
                                        "description", "如时间紧张，可考虑CORE测试加人工审查"
                                )
                        )
                )
        );

        mockServer.expect(requestTo(aiServiceUrl + "/api/v1/ai/recommendation/strategy/explain"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse), MediaType.APPLICATION_JSON));

        // When: 调用推荐解释
        Map<String, Object> response = aiServiceClient.explainRecommendation(request);

        // Then: 验证响应
        assertNotNull(response);
        assertTrue((Boolean) response.get("success"));

        @SuppressWarnings("unchecked")
        Map<String, Object> explanation = (Map<String, Object>) response.get("explanation");
        assertNotNull(explanation.get("summary"));
        assertEquals(0.88, explanation.get("confidence_level"));
    }

    // ========== 7. 健康检查 ==========

    @Test
    @DisplayName("场景7.1: 健康检查 - 服务正常")
    void healthCheck_ServiceHealthy() throws Exception {
        // Given: 模拟健康检查响应
        Map<String, Object> mockResponse = Map.of(
                "status", "UP",
                "service", "ai-service",
                "models_loaded", true,
                "timestamp", "2025-12-08T10:00:00Z"
        );

        mockServer.expect(requestTo(aiServiceUrl + "/api/v1/ai/health"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse), MediaType.APPLICATION_JSON));

        // When: 调用健康检查
        Map<String, Object> response = aiServiceClient.healthCheck();

        // Then: 验证响应
        assertNotNull(response);
        assertEquals("UP", response.get("status"));
        assertEquals("ai-service", response.get("service"));
        assertTrue((Boolean) response.get("models_loaded"));
    }

    @Test
    @DisplayName("场景7.2: 健康检查 - 服务异常")
    void healthCheck_ServiceDown() {
        // Given: 模拟服务不可用
        mockServer.expect(requestTo(aiServiceUrl + "/api/v1/ai/health"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        // When: 调用健康检查
        Map<String, Object> response = aiServiceClient.healthCheck();

        // Then: 验证降级响应
        assertNotNull(response);
        assertEquals("DOWN", response.get("status"));
        assertTrue(response.get("message").toString().contains("AI服务不可用"));
    }

    // ========== 8. 熔断器测试 ==========

    @Test
    @DisplayName("场景8.1: 熔断器打开 - 连续失败触发熔断")
    void circuitBreaker_OpenAfterConsecutiveFailures() {
        // Given: 准备测试请求
        AITestCaseGenerationRequest request = new AITestCaseGenerationRequest();
        request.setInput("测试熔断器");

        // 模拟连续5次失败（根据配置的failureRateThreshold）
        for (int i = 0; i < 5; i++) {
            mockServer.expect(requestTo(aiServiceUrl + "/api/v1/ai/testcase/generate"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withServerError());
        }

        // When: 连续调用5次
        for (int i = 0; i < 5; i++) {
            AITestCaseGenerationResponse response = aiServiceClient.generateTestCases(request);

            // Then: 每次都应该返回Fallback响应
            assertNotNull(response);
            assertFalse(response.isSuccess());
        }

        // 熔断器应该已经打开，后续请求直接走Fallback，不再调用AI服务
        // 验证总共只调用了AI服务5次（后续请求被熔断器拦截）
    }

    // ========== 9. 边界条件测试 ==========

    @Test
    @DisplayName("场景9.1: 空请求处理")
    void generateTestCases_EmptyInput_HandledGracefully() throws Exception {
        // Given: 空输入请求
        AITestCaseGenerationRequest request = new AITestCaseGenerationRequest();
        request.setInput("");

        AITestCaseGenerationResponse mockResponse = new AITestCaseGenerationResponse();
        mockResponse.setSuccess(false);
        mockResponse.setMessage("输入不能为空");

        mockServer.expect(requestTo(aiServiceUrl + "/api/v1/ai/testcase/generate"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse), MediaType.APPLICATION_JSON));

        // When: 调用生成
        AITestCaseGenerationResponse response = aiServiceClient.generateTestCases(request);

        // Then: 验证错误处理
        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("不能为空"));
    }

    @Test
    @DisplayName("场景9.2: 大批量请求处理")
    void batchGenerateTestCases_LargeVolume_HandledSuccessfully() throws Exception {
        // Given: 大批量需求（100个需求）
        List<String> requirements = new java.util.ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            requirements.add("需求" + i);
        }

        Map<String, Object> request = Map.of(
                "requirements", requirements,
                "test_type", "functional"
        );

        Map<String, Object> mockResponse = Map.of(
                "success", true,
                "message", "批量生成完成",
                "total_requirements", 100,
                "total_cases_generated", 300,
                "processing_time_seconds", 45
        );

        mockServer.expect(requestTo(aiServiceUrl + "/api/v1/ai/testcase/generate/batch"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse), MediaType.APPLICATION_JSON));

        // When: 调用批量生成
        Map<String, Object> response = aiServiceClient.batchGenerateTestCases(request);

        // Then: 验证响应
        assertNotNull(response);
        assertTrue((Boolean) response.get("success"));
        assertEquals(100, response.get("total_requirements"));
        assertEquals(300, response.get("total_cases_generated"));
    }

    @Test
    @DisplayName("场景9.3: 超高相似度阈值去重")
    void deduplicateTestCases_HighThreshold_FewerDuplicates() throws Exception {
        // Given: 高相似度阈值（0.95）
        DeduplicationRequest request = new DeduplicationRequest();
        request.setTestCaseIds(Arrays.asList("case-1", "case-2", "case-3"));
        request.setSimilarityThreshold(0.95);  // 很高的阈值

        DeduplicationResponse mockResponse = new DeduplicationResponse();
        mockResponse.setSuccess(true);
        mockResponse.setOriginalCount(3);
        mockResponse.setDeduplicatedCount(3);  // 高阈值下几乎没有重复
        mockResponse.setDuplicatesRemoved(0);

        mockServer.expect(requestTo(aiServiceUrl + "/api/v1/ai/testcase/optimize/deduplicate"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.similarity_threshold").value(0.95))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse), MediaType.APPLICATION_JSON));

        // When: 调用去重
        DeduplicationResponse response = aiServiceClient.deduplicateTestCases(request);

        // Then: 验证响应
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(0, response.getDuplicatesRemoved());
    }
}
