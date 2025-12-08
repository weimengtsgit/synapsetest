package com.synapsetest.testmanagement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapsetest.testmanagement.dto.request.GenerateTestCaseRequest;
import com.synapsetest.testmanagement.dto.response.TestCaseResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Controller集成测试 - TestCaseController
 *
 * 测试目标：
 * 1. 验证AI测试用例生成的完整流程
 * 2. 验证测试用例的CRUD操作
 * 3. 验证批量操作的原子性
 * 4. 验证与AI服务的集成
 *
 * 对应User Story: US2-AI生成测试用例
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("TestCaseController API集成测试")
@Sql(scripts = "/test-schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class TestCaseControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private String getBaseUrl() {
        return "http://localhost:" + port + "/api/v1/test-cases";
    }

    @Test
    @DisplayName("场景2.1: AI生成测试用例 - 订单模块需求")
    @Sql(scripts = {"/test-schema.sql", "/test-data-us2.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void generateTestCases_OrderModule_ShouldGenerateMultipleCases() {
        // Given: 准备生成请求
        GenerateTestCaseRequest request = new GenerateTestCaseRequest();
        request.setRequirementText("用户可以创建订单、查看订单列表、取消订单");
        request.setModule("订单管理");
        request.setNumCases(5);
        request.setPriority("HIGH");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-User-Id", "zhangsan");
        HttpEntity<GenerateTestCaseRequest> entity = new HttpEntity<>(request, headers);

        // When: 发送POST请求生成测试用例
        ResponseEntity<Map> response = restTemplate.exchange(
            getBaseUrl() + "/generate",
            HttpMethod.POST,
            entity,
            Map.class
        );

        // Then: 验证响应
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> responseBody = response.getBody();
        assertNotNull(responseBody);
        Map<String, Object> data = (Map<String, Object>) responseBody.get("data");

        assertTrue(data.containsKey("test_cases"));
        List<?> testCases = (List<?>) data.get("test_cases");
        assertNotNull(testCases);
        assertTrue(testCases.size() > 0 && testCases.size() <= 5);

        // 验证生成的用例包含必要字段
        Map<?, ?> firstCase = (Map<?, ?>) testCases.get(0);
        assertTrue(firstCase.containsKey("title"));
        assertTrue(firstCase.containsKey("steps"));
        assertTrue(firstCase.containsKey("expectedResults"));
        
        // AI置信度是在响应体中，不是在每个测试用例中
        assertTrue(data.containsKey("confidence_score"));
        Double confidence = (Double) data.get("confidence_score");
        assertTrue(confidence >= 0 && confidence <= 1);
    }

    @Test
    @DisplayName("场景2.2: AI生成支付模块用例 - 高优先级")
    @Sql(scripts = {"/test-schema.sql", "/test-data-us2.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void generateTestCases_PaymentModule_ShouldGenerateHighPriorityCases() {
        // Given: 准备生成请求（支付模块）
        GenerateTestCaseRequest request = new GenerateTestCaseRequest();
        request.setRequirementText("支持微信支付、支付宝支付、银行卡支付，支持退款功能");
        request.setModule("支付模块");
        request.setNumCases(6);
        request.setPriority("CRITICAL");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-User-Id", "lisi");
        HttpEntity<GenerateTestCaseRequest> entity = new HttpEntity<>(request, headers);

        // When: 发送POST请求
        ResponseEntity<Map> response = restTemplate.exchange(
            getBaseUrl() + "/generate",
            HttpMethod.POST,
            entity,
            Map.class
        );

        // Then: 验证响应
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> responseBody = response.getBody();
        Map<String, Object> data = (Map<String, Object>) responseBody.get("data");

        List<?> testCases = (List<?>) data.get("test_cases");
        assertTrue(testCases.size() >= 3);  // 至少包含微信、支付宝、银行卡

        // 验证去重逻辑（相似用例应该被合并）
        assertTrue(data.containsKey("duplicates_removed"));
    }

    @Test
    @DisplayName("场景2.3: 批量保存AI生成的测试用例")
    @Sql(scripts = {"/test-schema.sql", "/test-data-us2.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void batchSaveTestCases_WithValidCases_ShouldSaveAll() {
        // Given: 准备批量保存请求
        List<Map<String, Object>> testCases = List.of(
            Map.of(
                "case_name", "订单创建-正常流程",
                "module", "订单管理",
                "priority", "HIGH",
                "type", "FUNCTIONAL",
                "steps", "[{\"step\":\"1\",\"action\":\"选择商品\"}]",
                "expected_result", "{\"status\":\"success\"}",
                "ai_generated", true,
                "ai_confidence", 0.92
            ),
            Map.of(
                "case_name", "订单取消-异常场景",
                "module", "订单管理",
                "priority", "MEDIUM",
                "type", "FUNCTIONAL",
                "steps", "[{\"step\":\"1\",\"action\":\"取消订单\"}]",
                "expected_result", "{\"status\":\"cancelled\"}",
                "ai_generated", true,
                "ai_confidence", 0.88
            )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-User-Id", "zhangsan");
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(
            Map.of("test_cases", testCases),
            headers
        );

        // When: 发送POST请求批量保存
        ResponseEntity<Map> response = restTemplate.exchange(
            getBaseUrl() + "/batch",
            HttpMethod.POST,
            entity,
            Map.class
        );

        // Then: 验证响应
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        Map<String, Object> responseBody = response.getBody();
        assertNotNull(responseBody);

        assertTrue(responseBody.containsKey("saved_count"));
        assertEquals(2, responseBody.get("saved_count"));

        assertTrue(responseBody.containsKey("saved_ids"));
        List<?> savedIds = (List<?>) responseBody.get("saved_ids");
        assertEquals(2, savedIds.size());
    }

    @Test
    @DisplayName("场景2.4: 查询AI生成的测试用例列表")
    @Sql(scripts = {"/test-schema.sql", "/test-data-us2.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void getAiGeneratedCases_ShouldReturnFilteredList() {
        // Given: 查询参数
        String url = getBaseUrl() + "?ai_generated=true&module=订单管理";

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", "zhangsan");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // When: 发送GET请求
        ResponseEntity<Map> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            entity,
            Map.class
        );

        // Then: 验证响应
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> responseBody = response.getBody();

        assertTrue(responseBody.containsKey("content"));
        List<?> cases = (List<?>) responseBody.get("content");
        assertNotNull(cases);

        // 验证所有用例都是AI生成的
        cases.forEach(caseObj -> {
            Map<?, ?> testCase = (Map<?, ?>) caseObj;
            assertTrue((Boolean) testCase.get("ai_generated"));
        });
    }

    @Test
    @DisplayName("场景2.5: 根据ID获取测试用例详情")
    @Sql(scripts = {"/test-schema.sql", "/test-data-us2.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void getTestCaseById_WithValidId_ShouldReturnDetails() {
        // Given: 已存在的测试用例ID
        String caseId = "case-001";
        String url = getBaseUrl() + "/" + caseId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", "zhangsan");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // When: 发送GET请求
        ResponseEntity<Map> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            entity,
            Map.class
        );

        // Then: 验证响应
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> responseBody = response.getBody();
        assertNotNull(responseBody);
        
        // 从data字段获取数据
        Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
        assertNotNull(data, "Response should contain 'data' field");
        
        TestCaseResponse testCase = objectMapper.convertValue(data, TestCaseResponse.class);
        assertNotNull(testCase);
        assertEquals(caseId, testCase.getId());
        assertNotNull(testCase.getCaseName());
        assertNotNull(testCase.getSteps());
    }

    @Test
    @DisplayName("场景2.6: 更新测试用例")
    @Sql(scripts = {"/test-schema.sql", "/test-data-us2.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void updateTestCase_WithValidData_ShouldUpdateSuccessfully() {
        // Given: 准备更新请求
        String caseId = "case-001";
        String url = getBaseUrl() + "/" + caseId;

        Map<String, Object> updateRequest = Map.of(
            "title", "订单创建-优化后的流程",
            "priority", "CRITICAL",
            "steps", "[{\"step\":\"1\",\"action\":\"新的操作步骤\"}]"
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-User-Id", "zhangsan");
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(updateRequest, headers);

        // When: 发送PUT请求
        ResponseEntity<Map> response = restTemplate.exchange(
            url,
            HttpMethod.PUT,
            entity,
            Map.class
        );

        // Then: 验证响应
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> responseBody = response.getBody();
        assertNotNull(responseBody);
        
        // 从data字段获取数据
        Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
        assertNotNull(data, "Response should contain 'data' field");
        
        TestCaseResponse testCase = objectMapper.convertValue(data, TestCaseResponse.class);
        assertNotNull(testCase);
        assertEquals("订单创建-优化后的流程", testCase.getCaseName());
        assertEquals(8, testCase.getPriority());
    }

    @Test
    @DisplayName("场景2.7: 删除测试用例")
    @Sql(scripts = {"/test-schema.sql", "/test-data-us2.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void deleteTestCase_WithValidId_ShouldDeleteSuccessfully() {
        // Given: 已存在的测试用例ID
        String caseId = "case-to-delete";
        String url = getBaseUrl() + "/" + caseId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", "zhangsan");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // When: 发送DELETE请求
        ResponseEntity<Void> response = restTemplate.exchange(
            url,
            HttpMethod.DELETE,
            entity,
            Void.class
        );

        // Then: 验证删除成功
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

        // 验证再次获取该用例返回404
        ResponseEntity<Map> getResponse = restTemplate.exchange(
            url,
            HttpMethod.GET,
            entity,
            Map.class
        );
        assertEquals(HttpStatus.NOT_FOUND, getResponse.getStatusCode());
    }

    @Test
    @DisplayName("场景2.8: 参数验证 - 生成请求缺少必填字段")
    void generateTestCases_WithMissingFields_ShouldReturnBadRequest() {
        // Given: 准备不完整的请求（缺少requirementText）
        GenerateTestCaseRequest request = new GenerateTestCaseRequest();
        request.setModule("订单管理");
        request.setNumCases(5);
        // requirementText未设置

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-User-Id", "zhangsan");
        HttpEntity<GenerateTestCaseRequest> entity = new HttpEntity<>(request, headers);

        // When: 发送POST请求
        ResponseEntity<Map> response = restTemplate.exchange(
            getBaseUrl() + "/generate",
            HttpMethod.POST,
            entity,
            Map.class
        );

        // Then: 验证返回400错误
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("场景2.9: 去重功能验证")
    @Sql(scripts = {"/test-schema.sql", "/test-data-us2.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void generateTestCases_WithDuplicates_ShouldRemoveSimilarCases() {
        // Given: 准备生成请求
        GenerateTestCaseRequest request = new GenerateTestCaseRequest();
        request.setRequirementText("用户登录功能：输入用户名密码进行登录");
        request.setModule("用户认证");
        request.setNumCases(10);
        request.setEnableDeduplication(true);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-User-Id", "zhangsan");
        HttpEntity<GenerateTestCaseRequest> entity = new HttpEntity<>(request, headers);

        // When: 发送POST请求
        ResponseEntity<Map> response = restTemplate.exchange(
            getBaseUrl() + "/generate",
            HttpMethod.POST,
            entity,
            Map.class
        );

        // Then: 验证响应
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> responseBody = response.getBody();
        Map<String, Object> data = (Map<String, Object>) responseBody.get("data");

        // 验证去重信息
        assertTrue(data.containsKey("duplicates_removed"));
        Integer duplicatesRemoved = (Integer) data.get("duplicates_removed");
        assertNotNull(duplicatesRemoved);

        // 生成的用例数量应该少于请求的数量（因为去重）
        List<?> testCases = (List<?>) data.get("test_cases");
        assertTrue(testCases.size() <= 10);
    }

    @Test
    @DisplayName("场景2.10: 查询高置信度用例")
    @Sql(scripts = {"/test-schema.sql", "/test-data-us2.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void getHighConfidenceCases_ShouldReturnFilteredList() {
        // Given: 查询参数（置信度>=0.85）
        String url = getBaseUrl() + "?ai_generated=true&min_confidence=0.85";

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", "zhangsan");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // When: 发送GET请求
        ResponseEntity<Map> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            entity,
            Map.class
        );

        // Then: 验证响应
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> responseBody = response.getBody();

        List<?> cases = (List<?>) responseBody.get("content");
        assertNotNull(cases);

        // 验证所有用例的置信度都>=0.85
        cases.forEach(caseObj -> {
            Map<?, ?> testCase = (Map<?, ?>) caseObj;
            Double confidence = (Double) testCase.get("ai_confidence");
            assertTrue(confidence >= 0.85);
        });
    }
}
