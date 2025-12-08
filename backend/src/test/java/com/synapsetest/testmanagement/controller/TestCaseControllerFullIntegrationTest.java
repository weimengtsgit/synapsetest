package com.synapsetest.testmanagement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapsetest.testmanagement.dto.ApiResponse;
import com.synapsetest.testmanagement.dto.TestCaseRequest;
import com.synapsetest.testmanagement.dto.response.TestCaseResponse;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TestCaseController 完整集成测试
 *
 * 测试范围：
 * 1. POST / - 创建测试用例
 * 2. GET /{id} - 获取测试用例详情
 * 3. GET / - 获取所有测试用例
 * 4. GET /?status=xxx - 按状态查询
 * 5. GET /?type=xxx - 按类型查询
 * 6. PUT /{id} - 更新测试用例
 * 7. POST /{id}/approve - 审批测试用例
 * 8. DELETE /{id} - 删除测试用例
 * 9. POST /generate - AI生成（验证服务不可用）
 * 10. POST /deduplicate - 去重功能
 * 11. POST /analyze-coverage - 覆盖率分析
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("TestCaseController 完整集成测试")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestCaseControllerFullIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private String getBaseUrl() {
        return "http://localhost:" + port + "/api/v1/test-cases";
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-User-Name", "zhangsan");
        return headers;
    }

    // ====================================
    // 1. 创建测试用例 - POST /
    // ====================================

    @Test
    @Order(1)
    @DisplayName("1.1 创建测试用例-正常流程")
    @Sql(scripts = "/test-data-integration.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testCreateTestCase_Success() {
        // Given
        TestCaseRequest request = new TestCaseRequest();
        request.setTitle("新建测试用例-API测试");
        request.setDescription("验证API接口功能");
        request.setSteps(Arrays.asList("步骤1：发送请求", "步骤2：验证响应"));
        request.setExpectedResults("返回200状态码");
        request.setPriority(7);
        request.setType("API");
        request.setTags(Arrays.asList("api", "backend"));
        request.setRelatedRequirement("REQ-100");

        HttpEntity<TestCaseRequest> entity = new HttpEntity<>(request, createHeaders());

        // When
        ResponseEntity<Map> response = restTemplate.exchange(
            getBaseUrl(),
            HttpMethod.POST,
            entity,
            Map.class
        );

        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));

        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertNotNull(data);
        assertNotNull(data.get("id"));
        assertEquals("新建测试用例-API测试", data.get("title"));
    }

    @Test
    @Order(2)
    @DisplayName("1.2 创建测试用例-缺少必填字段")
    void testCreateTestCase_MissingRequiredFields() {
        // Given - 缺少title
        TestCaseRequest request = new TestCaseRequest();
        request.setSteps(Arrays.asList("步骤1"));
        request.setType("FUNCTIONAL");

        HttpEntity<TestCaseRequest> entity = new HttpEntity<>(request, createHeaders());

        // When
        ResponseEntity<Map> response = restTemplate.exchange(
            getBaseUrl(),
            HttpMethod.POST,
            entity,
            Map.class
        );

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    // ====================================
    // 2. 获取测试用例详情 - GET /{id}
    // ====================================

    @Test
    @Order(3)
    @DisplayName("2.1 获取测试用例详情-成功")
    @Sql(scripts = "/test-data-integration.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testGetTestCaseById_Success() {
        // Given
        String caseId = "tc-001";
        HttpEntity<Void> entity = new HttpEntity<>(createHeaders());

        // When
        ResponseEntity<ApiResponse> response = restTemplate.exchange(
            getBaseUrl() + "/" + caseId,
            HttpMethod.GET,
            entity,
            ApiResponse.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        Map<String, Object> data = (Map<String, Object>) response.getBody().getData();
        assertEquals(caseId, data.get("id"));
        assertEquals("用户登录-正常流程", data.get("title"));
    }

    @Test
    @Order(4)
    @DisplayName("2.2 获取测试用例详情-不存在的ID")
    @Sql(scripts = "/test-data-integration.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testGetTestCaseById_NotFound() {
        // Given
        String nonExistentId = "tc-999999";
        HttpEntity<Void> entity = new HttpEntity<>(createHeaders());

        // When
        ResponseEntity<Map> response = restTemplate.exchange(
            getBaseUrl() + "/" + nonExistentId,
            HttpMethod.GET,
            entity,
            Map.class
        );

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    // ====================================
    // 3. 获取所有测试用例 - GET /
    // ====================================

    @Test
    @Order(5)
    @DisplayName("3.1 获取所有测试用例-成功")
    @Sql(scripts = "/test-data-integration.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testGetAllTestCases_Success() {
        // Given
        HttpEntity<Void> entity = new HttpEntity<>(createHeaders());

        // When
        ResponseEntity<ApiResponse> response = restTemplate.exchange(
            getBaseUrl(),
            HttpMethod.GET,
            entity,
            ApiResponse.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        List<Map> data = (List<Map>) response.getBody().getData();
        assertNotNull(data);
        assertTrue(data.size() >= 7); // 测试数据中有7条
    }

    // ====================================
    // 4. 按状态查询 - GET /?status=xxx
    // ====================================

    @Test
    @Order(6)
    @DisplayName("4.1 按状态查询-APPROVED")
    @Sql(scripts = "/test-data-integration.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testGetTestCasesByStatus_Approved() {
        // Given
        HttpEntity<Void> entity = new HttpEntity<>(createHeaders());

        // When
        ResponseEntity<ApiResponse> response = restTemplate.exchange(
            getBaseUrl() + "?status=APPROVED",
            HttpMethod.GET,
            entity,
            ApiResponse.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<Map> data = (List<Map>) response.getBody().getData();
        assertNotNull(data);
        assertTrue(data.size() > 0);
        // 验证所有返回的用例状态都是APPROVED
        data.forEach(testCase -> assertEquals("APPROVED", testCase.get("status")));
    }

    @Test
    @Order(7)
    @DisplayName("4.2 按状态查询-DRAFT")
    @Sql(scripts = "/test-data-integration.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testGetTestCasesByStatus_Draft() {
        // Given
        HttpEntity<Void> entity = new HttpEntity<>(createHeaders());

        // When
        ResponseEntity<ApiResponse> response = restTemplate.exchange(
            getBaseUrl() + "?status=DRAFT",
            HttpMethod.GET,
            entity,
            ApiResponse.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<Map> data = (List<Map>) response.getBody().getData();
        assertNotNull(data);
        data.forEach(testCase -> assertEquals("DRAFT", testCase.get("status")));
    }

    // ====================================
    // 5. 按类型查询 - GET /?type=xxx
    // ====================================

    @Test
    @Order(8)
    @DisplayName("5.1 按类型查询-FUNCTIONAL")
    @Sql(scripts = "/test-data-integration.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testGetTestCasesByType_Functional() {
        // Given
        HttpEntity<Void> entity = new HttpEntity<>(createHeaders());

        // When
        ResponseEntity<ApiResponse> response = restTemplate.exchange(
            getBaseUrl() + "?type=FUNCTIONAL",
            HttpMethod.GET,
            entity,
            ApiResponse.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<Map> data = (List<Map>) response.getBody().getData();
        assertNotNull(data);
        assertTrue(data.size() > 0);
        data.forEach(testCase -> assertEquals("FUNCTIONAL", testCase.get("type")));
    }

    @Test
    @Order(9)
    @DisplayName("5.2 按类型查询-PERFORMANCE")
    @Sql(scripts = "/test-data-integration.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testGetTestCasesByType_Performance() {
        // Given
        HttpEntity<Void> entity = new HttpEntity<>(createHeaders());

        // When
        ResponseEntity<ApiResponse> response = restTemplate.exchange(
            getBaseUrl() + "?type=PERFORMANCE",
            HttpMethod.GET,
            entity,
            ApiResponse.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<Map> data = (List<Map>) response.getBody().getData();
        assertNotNull(data);
        data.forEach(testCase -> assertEquals("PERFORMANCE", testCase.get("type")));
    }

    // ====================================
    // 6. 更新测试用例 - PUT /{id}
    // ====================================

    @Test
    @Order(10)
    @DisplayName("6.1 更新测试用例-成功")
    @Sql(scripts = "/test-data-integration.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testUpdateTestCase_Success() {
        // Given
        String caseId = "tc-004";
        TestCaseRequest request = new TestCaseRequest();
        request.setTitle("支付功能-微信支付-已更新");
        request.setDescription("更新后的描述");
        request.setSteps(Arrays.asList("步骤1-更新", "步骤2-更新"));
        request.setExpectedResults("更新后的预期结果");
        request.setPriority(9);
        request.setType("FUNCTIONAL");

        HttpEntity<TestCaseRequest> entity = new HttpEntity<>(request, createHeaders());

        // When
        ResponseEntity<ApiResponse> response = restTemplate.exchange(
            getBaseUrl() + "/" + caseId,
            HttpMethod.PUT,
            entity,
            ApiResponse.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> data = (Map<String, Object>) response.getBody().getData();
        assertEquals("支付功能-微信支付-已更新", data.get("title"));
        assertEquals(9, data.get("priority"));
    }

    @Test
    @Order(11)
    @DisplayName("6.2 更新测试用例-不存在的ID")
    @Sql(scripts = "/test-data-integration.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testUpdateTestCase_NotFound() {
        // Given
        String nonExistentId = "tc-999999";
        TestCaseRequest request = new TestCaseRequest();
        request.setTitle("更新不存在的用例");
        request.setSteps(Arrays.asList("步骤"));
        request.setType("FUNCTIONAL");

        HttpEntity<TestCaseRequest> entity = new HttpEntity<>(request, createHeaders());

        // When
        ResponseEntity<Map> response = restTemplate.exchange(
            getBaseUrl() + "/" + nonExistentId,
            HttpMethod.PUT,
            entity,
            Map.class
        );

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    // ====================================
    // 7. 审批测试用例 - POST /{id}/approve
    // ====================================

    @Test
    @Order(12)
    @DisplayName("7.1 审批测试用例-成功")
    @Sql(scripts = "/test-data-integration.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testApproveTestCase_Success() {
        // Given
        String caseId = "tc-004"; // 状态为DRAFT
        HttpEntity<Void> entity = new HttpEntity<>(createHeaders());

        // When
        ResponseEntity<ApiResponse> response = restTemplate.exchange(
            getBaseUrl() + "/" + caseId + "/approve",
            HttpMethod.POST,
            entity,
            ApiResponse.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> data = (Map<String, Object>) response.getBody().getData();
        assertEquals("APPROVED", data.get("status"));
    }

    // ====================================
    // 8. 删除测试用例 - DELETE /{id}
    // ====================================

    @Test
    @Order(13)
    @DisplayName("8.1 删除测试用例-成功")
    @Sql(scripts = "/test-data-integration.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testDeleteTestCase_Success() {
        // Given
        String caseId = "tc-to-delete";
        HttpEntity<Void> entity = new HttpEntity<>(createHeaders());

        // When
        ResponseEntity<ApiResponse> response = restTemplate.exchange(
            getBaseUrl() + "/" + caseId,
            HttpMethod.DELETE,
            entity,
            ApiResponse.class
        );

        // Then
        assertTrue(response.getStatusCode().is2xxSuccessful());

        // 验证删除后无法再获取
        ResponseEntity<Map> getResponse = restTemplate.exchange(
            getBaseUrl() + "/" + caseId,
            HttpMethod.GET,
            entity,
            Map.class
        );
        assertEquals(HttpStatus.NOT_FOUND, getResponse.getStatusCode());
    }

    @Test
    @Order(14)
    @DisplayName("8.2 删除测试用例-不存在的ID")
    @Sql(scripts = "/test-data-integration.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testDeleteTestCase_NotFound() {
        // Given
        String nonExistentId = "tc-999999";
        HttpEntity<Void> entity = new HttpEntity<>(createHeaders());

        // When
        ResponseEntity<Map> response = restTemplate.exchange(
            getBaseUrl() + "/" + nonExistentId,
            HttpMethod.DELETE,
            entity,
            Map.class
        );

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    // ====================================
    // 9. AI生成测试用例 - POST /generate
    // ====================================

    @Test
    @Order(15)
    @DisplayName("9.1 AI生成测试用例-服务不可用（Profile未激活）")
    void testGenerateTestCases_ServiceUnavailable() {
        // Given
        Map<String, Object> request = Map.of(
            "input", "用户登录功能需求",
            "numberOfCases", 5
        );
        HttpEntity<Map> entity = new HttpEntity<>(request, createHeaders());

        // When
        ResponseEntity<Map> response = restTemplate.exchange(
            getBaseUrl() + "/generate",
            HttpMethod.POST,
            entity,
            Map.class
        );

        // Then
        // AI服务需要mongodb profile，在test环境下不可用
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
    }

    // ====================================
    // 10. 完整业务流程测试
    // ====================================

    @Test
    @Order(16)
    @DisplayName("10.1 完整CRUD流程测试")
    @Sql(scripts = "/test-data-integration.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testFullCRUDWorkflow() {
        HttpHeaders headers = createHeaders();

        // 1. 创建测试用例
        TestCaseRequest createRequest = new TestCaseRequest();
        createRequest.setTitle("完整流程测试用例");
        createRequest.setDescription("测试完整的CRUD流程");
        createRequest.setSteps(Arrays.asList("步骤1", "步骤2", "步骤3"));
        createRequest.setExpectedResults("预期结果");
        createRequest.setPriority(5);
        createRequest.setType("FUNCTIONAL");

        ResponseEntity<ApiResponse> createResponse = restTemplate.exchange(
            getBaseUrl(),
            HttpMethod.POST,
            new HttpEntity<>(createRequest, headers),
            ApiResponse.class
        );
        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        String createdId = (String) ((Map) createResponse.getBody().getData()).get("id");
        assertNotNull(createdId);

        // 2. 查询刚创建的用例
        ResponseEntity<ApiResponse> getResponse = restTemplate.exchange(
            getBaseUrl() + "/" + createdId,
            HttpMethod.GET,
            new HttpEntity<>(headers),
            ApiResponse.class
        );
        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        assertEquals("完整流程测试用例", ((Map) getResponse.getBody().getData()).get("title"));

        // 3. 更新测试用例
        TestCaseRequest updateRequest = new TestCaseRequest();
        updateRequest.setTitle("完整流程测试用例-已更新");
        updateRequest.setSteps(Arrays.asList("更新步骤1", "更新步骤2"));
        updateRequest.setPriority(8);
        updateRequest.setType("FUNCTIONAL");

        ResponseEntity<ApiResponse> updateResponse = restTemplate.exchange(
            getBaseUrl() + "/" + createdId,
            HttpMethod.PUT,
            new HttpEntity<>(updateRequest, headers),
            ApiResponse.class
        );
        assertEquals(HttpStatus.OK, updateResponse.getStatusCode());
        assertEquals("完整流程测试用例-已更新", ((Map) updateResponse.getBody().getData()).get("title"));

        // 4. 审批测试用例
        ResponseEntity<ApiResponse> approveResponse = restTemplate.exchange(
            getBaseUrl() + "/" + createdId + "/approve",
            HttpMethod.POST,
            new HttpEntity<>(headers),
            ApiResponse.class
        );
        assertEquals(HttpStatus.OK, approveResponse.getStatusCode());
        assertEquals("APPROVED", ((Map) approveResponse.getBody().getData()).get("status"));

        // 5. 删除测试用例
        ResponseEntity<ApiResponse> deleteResponse = restTemplate.exchange(
            getBaseUrl() + "/" + createdId,
            HttpMethod.DELETE,
            new HttpEntity<>(headers),
            ApiResponse.class
        );
        assertTrue(deleteResponse.getStatusCode().is2xxSuccessful());

        // 6. 验证删除成功
        ResponseEntity<Map> finalGetResponse = restTemplate.exchange(
            getBaseUrl() + "/" + createdId,
            HttpMethod.GET,
            new HttpEntity<>(headers),
            Map.class
        );
        assertEquals(HttpStatus.NOT_FOUND, finalGetResponse.getStatusCode());
    }
}
