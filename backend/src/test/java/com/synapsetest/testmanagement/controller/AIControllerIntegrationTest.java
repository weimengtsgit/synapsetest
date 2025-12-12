package com.synapsetest.testmanagement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
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
 * AIController API集成测试
 *
 * 测试目标：
 * 1. 验证AIController作为API网关的功能
 * 2. 验证所有AI相关API端点的正确性
 * 3. 验证请求/响应的格式转换
 * 4. 验证错误处理和降级策略
 *
 * 对应架构整改方案: 问题1 - 统一AI接口网关
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("AIController API网关集成测试")
public class AIControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${services.ai-service.url:http://localhost:8000}")
    private String aiServiceUrl;

    private MockRestServiceServer mockServer;

    private String getBaseUrl() {
        return "http://localhost:" + port + "/api/v1/ai";
    }

    @BeforeEach
    void setUp() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @AfterEach
    void tearDown() {
        mockServer.verify();
    }

    // ========== 1. 测试用例生成API ==========

    @Test
    @DisplayName("API场景1.1: POST /ai/testcase/generate - 成功生成")
    void apiGenerateTestCases_Success() throws Exception {
        // Given: 准备请求体
        Map<String, Object> requestBody = Map.of(
                "input", "用户注册功能：用户可以通过手机号或邮箱注册",
                "test_type", "functional",
                "tags", Arrays.asList("用户", "注册"),
                "related_requirement", "REQ-001"
        );

        // Mock AI-Service响应
        Map<String, Object> mockAiResponse = Map.of(
                "success", true,
                "message", "成功生成3个测试用例",
                "generated_cases", Arrays.asList(
                        Map.of(
                                "title", "手机号注册-正常流程",
                                "description", "验证用户使用手机号注册",
                                "steps", Arrays.asList("输入手机号", "获取验证码", "提交注册"),
                                "expected_results", "注册成功",
                                "priority", 8,
                                "type", "FUNCTIONAL",
                                "confidence", 0.92
                        )
                ),
                "total_count", 3,
                "processing_time_ms", 1200
        );

        mockServer.expect(requestTo(aiServiceUrl + "/api/v1/ai/testcase/generate"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockAiResponse), MediaType.APPLICATION_JSON));

        // When: 发送POST请求到AIController
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = testRestTemplate.exchange(
                getBaseUrl() + "/testcase/generate",
                HttpMethod.POST,
                entity,
                Map.class
        );

        // Then: 验证响应
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> responseBody = response.getBody();
        assertNotNull(responseBody);
        assertTrue((Boolean) responseBody.get("success"));

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
        assertNotNull(data);
        assertEquals(3, data.get("total_count"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> cases = (List<Map<String, Object>>) data.get("generated_cases");
        assertEquals(1, cases.size());
        assertEquals("手机号注册-正常流程", cases.get(0).get("title"));
    }

    @Test
    @DisplayName("API场景1.2: POST /ai/testcase/generate - 参数验证失败")
    void apiGenerateTestCases_ValidationFailed() {
        // Given: 缺少必填字段input
        Map<String, Object> requestBody = Map.of(
                "test_type", "functional"
                // 缺少input字段
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        // When: 发送POST请求
        ResponseEntity<Map> response = testRestTemplate.exchange(
                getBaseUrl() + "/testcase/generate",
                HttpMethod.POST,
                entity,
                Map.class
        );

        // Then: 验证返回400错误
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("API场景1.3: POST /ai/testcase/generate - AI服务降级")
    void apiGenerateTestCases_ServiceDegraded() {
        // Given: 准备请求
        Map<String, Object> requestBody = Map.of(
                "input", "测试降级场景"
        );

        // Mock AI-Service返回500错误
        mockServer.expect(requestTo(aiServiceUrl + "/api/v1/ai/testcase/generate"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        // When: 发送POST请求（会触发降级）
        ResponseEntity<Map> response = testRestTemplate.exchange(
                getBaseUrl() + "/testcase/generate",
                HttpMethod.POST,
                entity,
                Map.class
        );

        // Then: 验证返回成功但带有降级提示
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> responseBody = response.getBody();
        assertNotNull(responseBody);
        assertFalse((Boolean) responseBody.get("success"));
        assertTrue(responseBody.get("message").toString().contains("AI服务暂时不可用"));
    }

    // ========== 2. 批量生成API ==========

    @Test
    @DisplayName("API场景2.1: POST /ai/testcase/generate/batch - 批量生成成功")
    void apiBatchGenerate_Success() throws Exception {
        // Given: 准备批量请求
        Map<String, Object> requestBody = Map.of(
                "requirements", Arrays.asList(
                        "用户登录功能",
                        "用户注销功能",
                        "密码找回功能"
                ),
                "test_type", "functional",
                "enable_deduplication", true,
                "enable_prioritization", true
        );

        Map<String, Object> mockAiResponse = Map.of(
                "success", true,
                "message", "批量生成完成",
                "total_requirements", 3,
                "total_cases_generated", 12,
                "results", Arrays.asList(
                        Map.of("requirement", "用户登录功能", "cases_count", 4),
                        Map.of("requirement", "用户注销功能", "cases_count", 4),
                        Map.of("requirement", "密码找回功能", "cases_count", 4)
                )
        );

        mockServer.expect(requestTo(aiServiceUrl + "/api/v1/ai/testcase/generate/batch"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockAiResponse), MediaType.APPLICATION_JSON));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        // When: 发送POST请求
        ResponseEntity<Map> response = testRestTemplate.exchange(
                getBaseUrl() + "/testcase/generate/batch",
                HttpMethod.POST,
                entity,
                Map.class
        );

        // Then: 验证响应
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> responseBody = response.getBody();
        assertNotNull(responseBody);
        assertTrue((Boolean) responseBody.get("success"));

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
        assertEquals(3, data.get("total_requirements"));
        assertEquals(12, data.get("total_cases_generated"));
    }

    // ========== 3. 去重API ==========

    @Test
    @DisplayName("API场景3.1: POST /ai/testcase/optimize/deduplicate - 去重成功")
    void apiDeduplicate_Success() throws Exception {
        // Given: 准备去重请求
        Map<String, Object> requestBody = Map.of(
                "test_case_ids", Arrays.asList("case-1", "case-2", "case-3", "case-4", "case-5"),
                "similarity_threshold", 0.85
        );

        Map<String, Object> mockAiResponse = Map.of(
                "success", true,
                "message", "去重完成",
                "original_count", 5,
                "deduplicated_count", 3,
                "duplicates_removed", 2,
                "duplicate_groups", Arrays.asList(
                        Arrays.asList("case-2", "case-3"),
                        Arrays.asList("case-4", "case-5")
                )
        );

        mockServer.expect(requestTo(aiServiceUrl + "/api/v1/ai/testcase/optimize/deduplicate"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockAiResponse), MediaType.APPLICATION_JSON));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        // When: 发送POST请求
        ResponseEntity<Map> response = testRestTemplate.exchange(
                getBaseUrl() + "/testcase/optimize/deduplicate",
                HttpMethod.POST,
                entity,
                Map.class
        );

        // Then: 验证响应
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> responseBody = response.getBody();
        assertNotNull(responseBody);
        assertTrue((Boolean) responseBody.get("success"));

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
        assertEquals(5, data.get("original_count"));
        assertEquals(3, data.get("deduplicated_count"));
        assertEquals(2, data.get("duplicates_removed"));
    }

    // ========== 4. 优先级排序API ==========

    @Test
    @DisplayName("API场景4.1: POST /ai/testcase/optimize/prioritize - 排序成功")
    void apiPrioritize_Success() throws Exception {
        // Given: 准备优先级排序请求
        Map<String, Object> requestBody = Map.of(
                "test_case_ids", Arrays.asList("case-1", "case-2", "case-3"),
                "context", Map.of(
                        "release_urgency", "high",
                        "available_time_hours", 4
                )
        );

        Map<String, Object> mockAiResponse = Map.of(
                "success", true,
                "message", "优先级排序完成",
                "prioritized_cases", Arrays.asList(
                        Map.of(
                                "test_case_id", "case-1",
                                "priority", 10,
                                "priority_reason", "核心功能，高风险"
                        ),
                        Map.of(
                                "test_case_id", "case-3",
                                "priority", 7,
                                "priority_reason", "重要功能"
                        ),
                        Map.of(
                                "test_case_id", "case-2",
                                "priority", 5,
                                "priority_reason", "普通功能"
                        )
                )
        );

        mockServer.expect(requestTo(aiServiceUrl + "/api/v1/ai/testcase/optimize/prioritize"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockAiResponse), MediaType.APPLICATION_JSON));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        // When: 发送POST请求
        ResponseEntity<Map> response = testRestTemplate.exchange(
                getBaseUrl() + "/testcase/optimize/prioritize",
                HttpMethod.POST,
                entity,
                Map.class
        );

        // Then: 验证响应
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> responseBody = response.getBody();
        assertNotNull(responseBody);
        assertTrue((Boolean) responseBody.get("success"));

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> prioritizedCases = (List<Map<String, Object>>) data.get("prioritized_cases");
        assertEquals(3, prioritizedCases.size());
        assertEquals(10, prioritizedCases.get(0).get("priority"));  // 验证已按优先级排序
    }

    // ========== 5. 质量分析API ==========

    @Test
    @DisplayName("API场景5.1: POST /ai/testcase/analyze/quality - 质量分析成功")
    void apiAnalyzeQuality_Success() throws Exception {
        // Given: 准备质量分析请求 (Backend expects {"testcases": [...]})
        Map<String, Object> requestBody = Map.of(
                "testcases", Arrays.asList(
                        Map.of(
                                "id", "case-1",
                                "name", "用户登录测试",
                                "steps", Arrays.asList("打开登录页", "输入用户名密码", "点击登录"),
                                "expected_result", "成功登录"
                        ),
                        Map.of(
                                "id", "case-2",
                                "name", "密码错误测试",
                                "steps", Arrays.asList("打开登录页", "输入错误密码"),
                                "expected_result", "显示错误提示"
                        )
                )
        );

        Map<String, Object> mockAiResponse = Map.of(
                "success", true,
                "message", "质量分析完成",
                "overall_quality_score", 0.85,
                "analysis_results", Arrays.asList(
                        Map.of(
                                "test_case_id", "case-1",
                                "quality_score", 0.92,
                                "completeness_score", 0.95,
                                "clarity_score", 0.88,
                                "coverage_score", 0.90,
                                "issues", Arrays.asList("建议补充异常场景"),
                                "suggestions", Arrays.asList("增加边界值测试")
                        ),
                        Map.of(
                                "test_case_id", "case-2",
                                "quality_score", 0.78,
                                "completeness_score", 0.80,
                                "clarity_score", 0.75,
                                "coverage_score", 0.80,
                                "issues", Arrays.asList("测试步骤不够详细", "缺少预期结果"),
                                "suggestions", Arrays.asList("细化测试步骤", "补充预期结果")
                        )
                )
        );

        // AI Service expects a direct array
        mockServer.expect(requestTo(aiServiceUrl + "/api/v1/ai/testcase/analyze/quality"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value("case-1"))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockAiResponse), MediaType.APPLICATION_JSON));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        // When: 发送POST请求
        ResponseEntity<Map> response = testRestTemplate.exchange(
                getBaseUrl() + "/testcase/analyze/quality",
                HttpMethod.POST,
                entity,
                Map.class
        );

        // Then: 验证响应
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> responseBody = response.getBody();
        assertNotNull(responseBody);
        assertTrue((Boolean) responseBody.get("success"));

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
        assertEquals(0.85, data.get("overall_quality_score"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results = (List<Map<String, Object>>) data.get("analysis_results");
        assertEquals(2, results.size());
        assertEquals(0.92, results.get(0).get("quality_score"));
    }

    // ========== 6. 策略推荐API ==========

    @Test
    @DisplayName("API场景6.1: POST /ai/recommendation/strategy - 推荐成功（含环境和版本）")
    void apiRecommendStrategy_WithEnvAndVersion_Success() throws Exception {
        // Given: 准备策略推荐请求
        Map<String, Object> requestBody = Map.of(
                "task_id", "task-001",
                "environment_id", "env-prod",
                "version_id", "version-2.0.0",
                "context", Map.of(
                        "code_change", Map.of(
                                "changed_files_count", 20,
                                "changed_lines_count", 800,
                                "changed_modules", Arrays.asList("订单模块", "支付模块")
                        ),
                        "historical", Map.of(
                                "recent_pass_rate", 0.88,
                                "recent_defect_count", 5
                        ),
                        "business", Map.of(
                                "module_importance", 0.95,
                                "business_priority", "P0"
                        )
                )
        );

        Map<String, Object> mockAiResponse = Map.of(
                "task_id", "task-001",
                "recommendation", Map.of(
                        "test_scope", "FULL",
                        "priority", 10,
                        "environment", "PROD",
                        "estimated_duration_hours", 8,
                        "reasoning", Arrays.asList(
                                "代码变更涉及核心模块(订单、支付)",
                                "环境为生产环境(PROD)，推荐全量回归测试",
                                "版本2.0.0为大版本升级，建议全面回归测试",
                                "历史通过率偏低(88%)，需加强测试"
                        )
                ),
                "risk_assessment", Map.of(
                        "risk_level", "HIGH",
                        "risk_score", 0.85,
                        "risk_factors", Arrays.asList(
                                "生产环境部署，风险容忍度降低",
                                "大版本升级",
                                "涉及核心业务模块",
                                "近期缺陷较多"
                        )
                ),
                "environment_recommendations", Arrays.asList("STAGING", "PROD"),
                "timestamp", "2025-12-08T10:00:00Z"
        );

        mockServer.expect(requestTo(aiServiceUrl + "/api/v1/ai/recommendation/strategy"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.task_id").value("task-001"))
                .andExpect(jsonPath("$.environment_id").value("env-prod"))
                .andExpect(jsonPath("$.version_id").value("version-2.0.0"))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockAiResponse), MediaType.APPLICATION_JSON));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        // When: 发送POST请求
        ResponseEntity<Map> response = testRestTemplate.exchange(
                getBaseUrl() + "/recommendation/strategy",
                HttpMethod.POST,
                entity,
                Map.class
        );

        // Then: 验证响应
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> responseBody = response.getBody();
        assertNotNull(responseBody);
        assertTrue((Boolean) responseBody.get("success"));

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
        assertEquals("task-001", data.get("task_id"));

        @SuppressWarnings("unchecked")
        Map<String, Object> recommendation = (Map<String, Object>) data.get("recommendation");
        assertEquals("FULL", recommendation.get("test_scope"));
        assertEquals(10, recommendation.get("priority"));
        assertEquals("PROD", recommendation.get("environment"));

        @SuppressWarnings("unchecked")
        Map<String, Object> riskAssessment = (Map<String, Object>) data.get("risk_assessment");
        assertEquals("HIGH", riskAssessment.get("risk_level"));

        @SuppressWarnings("unchecked")
        List<String> envRecommendations = (List<String>) data.get("environment_recommendations");
        assertTrue(envRecommendations.contains("PROD"));
    }

    @Test
    @DisplayName("API场景6.2: POST /ai/recommendation/strategy/explain - 推荐解释成功")
    void apiExplainRecommendation_Success() throws Exception {
        // Given: 准备解释请求
        Map<String, Object> requestBody = Map.of(
                "recommendation", Map.of(
                        "test_scope", "FULL",
                        "priority", 10,
                        "environment", "PROD"
                ),
                "context", Map.of(
                        "code_change", Map.of(
                                "changed_files_count", 20
                        )
                )
        );

        Map<String, Object> mockAiResponse = Map.of(
                "success", true,
                "explanation", Map.of(
                        "summary", "推荐执行FULL范围测试，在PROD环境运行，优先级为P10",
                        "key_factors", Arrays.asList(
                                "代码变更范围较大",
                                "业务优先级高",
                                "部署至生产环境"
                        ),
                        "reasoning", Arrays.asList(
                                "变更涉及20个文件，影响范围大",
                                "生产环境要求高质量保障"
                        ),
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
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockAiResponse), MediaType.APPLICATION_JSON));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        // When: 发送POST请求
        ResponseEntity<Map> response = testRestTemplate.exchange(
                getBaseUrl() + "/recommendation/strategy/explain",
                HttpMethod.POST,
                entity,
                Map.class
        );

        // Then: 验证响应
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> responseBody = response.getBody();
        assertNotNull(responseBody);
        assertTrue((Boolean) responseBody.get("success"));

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
        @SuppressWarnings("unchecked")
        Map<String, Object> explanation = (Map<String, Object>) data.get("explanation");
        assertNotNull(explanation.get("summary"));
        assertEquals(0.88, explanation.get("confidence_level"));

        @SuppressWarnings("unchecked")
        List<String> keyFactors = (List<String>) explanation.get("key_factors");
        assertEquals(3, keyFactors.size());
    }

    // ========== 7. 健康检查API ==========

    @Test
    @DisplayName("API场景7.1: GET /ai/health - 健康检查成功")
    void apiHealthCheck_Success() throws Exception {
        // Given: 模拟AI服务健康
        Map<String, Object> mockAiResponse = Map.of(
                "status", "UP",
                "service", "ai-service",
                "models_loaded", true,
                "version", "1.0.0"
        );

        mockServer.expect(requestTo(aiServiceUrl + "/api/v1/ai/health"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockAiResponse), MediaType.APPLICATION_JSON));

        // When: 发送GET请求
        ResponseEntity<Map> response = testRestTemplate.exchange(
                getBaseUrl() + "/health",
                HttpMethod.GET,
                null,
                Map.class
        );

        // Then: 验证响应
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> responseBody = response.getBody();
        assertNotNull(responseBody);
        assertTrue((Boolean) responseBody.get("success"));

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
        assertEquals("UP", data.get("status"));
        assertTrue((Boolean) data.get("models_loaded"));
    }

    @Test
    @DisplayName("API场景7.2: GET /ai/health - AI服务异常")
    void apiHealthCheck_ServiceDown() {
        // Given: 模拟AI服务不可用
        mockServer.expect(requestTo(aiServiceUrl + "/api/v1/ai/health"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        // When: 发送GET请求
        ResponseEntity<Map> response = testRestTemplate.exchange(
                getBaseUrl() + "/health",
                HttpMethod.GET,
                null,
                Map.class
        );

        // Then: 验证降级响应
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> responseBody = response.getBody();
        assertNotNull(responseBody);
        assertTrue((Boolean) responseBody.get("success"));

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
        assertEquals("DOWN", data.get("status"));
    }

    // ========== 8. 统一错误处理 ==========

    @Test
    @DisplayName("API场景8.1: 404 - 不存在的端点")
    void apiNotFound() {
        // When: 访问不存在的端点
        ResponseEntity<Map> response = testRestTemplate.exchange(
                getBaseUrl() + "/nonexistent",
                HttpMethod.GET,
                null,
                Map.class
        );

        // Then: 验证404响应
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    @DisplayName("API场景8.2: 405 - 不支持的HTTP方法")
    void apiMethodNotAllowed() {
        // When: 使用错误的HTTP方法（GET代替POST）
        ResponseEntity<Map> response = testRestTemplate.exchange(
                getBaseUrl() + "/testcase/generate",
                HttpMethod.GET,
                null,
                Map.class
        );

        // Then: 验证405响应
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
    }

    @Test
    @DisplayName("API场景8.3: 415 - 不支持的Content-Type")
    void apiUnsupportedMediaType() {
        // Given: 使用错误的Content-Type
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        HttpEntity<String> entity = new HttpEntity<>("plain text body", headers);

        // When: 发送请求
        ResponseEntity<Map> response = testRestTemplate.exchange(
                getBaseUrl() + "/testcase/generate",
                HttpMethod.POST,
                entity,
                Map.class
        );

        // Then: 验证415响应
        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, response.getStatusCode());
    }
}
