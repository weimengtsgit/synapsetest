package com.synapsetest.testmanagement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapsetest.testmanagement.dto.request.CreateTestTaskRequest;
import com.synapsetest.testmanagement.dto.response.TestTaskResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Controller集成测试 - TestTaskController
 *
 * 测试目标：
 * 1. 验证HTTP API端点的完整请求-响应流程
 * 2. 验证请求参数验证和错误处理
 * 3. 验证业务逻辑的端到端执行
 * 4. 验证与外部服务（AI服务）的集成
 *
 * 技术栈：
 * - @SpringBootTest(webEnvironment = RANDOM_PORT): 启动完整应用上下文
 * - TestRestTemplate: 用于发送HTTP请求
 * - @Sql: 初始化测试数据
 *
 * 对应User Story: US1-智能测试任务调度
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("TestTaskController API集成测试")
public class TestTaskControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private String getBaseUrl() {
        return "http://localhost:" + port + "/api/v1/test-tasks";
    }

    @Test
    @DisplayName("场景1.1: 创建冒烟测试任务 - 小范围代码变更")
    @Sql(scripts = "/test-data-us1.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void createTestTask_SmallCodeChange_ShouldRecommendSmokeTest() {
        // Given: 准备创建任务请求（小范围代码变更）
        CreateTestTaskRequest request = new CreateTestTaskRequest();
        request.setTaskName("冒烟测试-订单模块");
        request.setEnvironment("DEV");
        request.setVersion("v1.2.0");
        request.setModules(List.of("order", "payment"));
        request.setCodeChangeInfo(Map.of(
            "changed_files_count", 3,
            "changed_lines_count", 25,
            "is_hotfix", false
        ));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-User-Id", "zhangsan");
        HttpEntity<CreateTestTaskRequest> entity = new HttpEntity<>(request, headers);

        // When: 发送POST请求创建任务
        ResponseEntity<TestTaskResponse> response = restTemplate.exchange(
            getBaseUrl(),
            HttpMethod.POST,
            entity,
            TestTaskResponse.class
        );

        // Then: 验证响应
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());

        TestTaskResponse taskResponse = response.getBody();
        assertNotNull(taskResponse.getId());
        assertEquals("冒烟测试-订单模块", taskResponse.getTaskName());
        assertEquals("PENDING", taskResponse.getStatus());

        // 验证AI推荐（应该推荐SMOKE测试）
        assertNotNull(taskResponse.getAiRecommendation());
        assertEquals("SMOKE", taskResponse.getAiRecommendation().get("test_scope"));
        assertTrue((Double) taskResponse.getAiRecommendation().get("confidence") >= 0.7);
    }

    @Test
    @DisplayName("场景1.2: 创建核心功能测试 - 支付流程变更")
    @Sql(scripts = "/test-data-us1.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void createTestTask_PaymentModuleChange_ShouldRecommendCoreTest() {
        // Given: 准备创建任务请求（支付模块变更）
        CreateTestTaskRequest request = new CreateTestTaskRequest();
        request.setTaskName("核心功能测试-支付流程");
        request.setEnvironment("TEST");
        request.setVersion("v1.2.1");
        request.setModules(List.of("payment", "order"));
        request.setCodeChangeInfo(Map.of(
            "changed_files_count", 15,
            "changed_lines_count", 200,
            "is_critical_module", true
        ));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-User-Id", "lisi");
        HttpEntity<CreateTestTaskRequest> entity = new HttpEntity<>(request, headers);

        // When: 发送POST请求
        ResponseEntity<TestTaskResponse> response = restTemplate.exchange(
            getBaseUrl(),
            HttpMethod.POST,
            entity,
            TestTaskResponse.class
        );

        // Then: 验证响应
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        TestTaskResponse taskResponse = response.getBody();
        assertNotNull(taskResponse);

        // 验证AI推荐CORE测试
        assertEquals("CORE", taskResponse.getAiRecommendation().get("test_scope"));
        assertEquals("TEST", taskResponse.getAiRecommendation().get("environment"));
    }

    @Test
    @DisplayName("场景1.3: 获取任务列表 - 分页查询")
    @Sql(scripts = "/test-data-us1.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void getTestTasks_WithPagination_ShouldReturnPagedList() {
        // Given: 分页参数
        String url = getBaseUrl() + "?page=0&size=10&status=PENDING";

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

        assertTrue(responseBody.containsKey("content"));
        assertTrue(responseBody.containsKey("totalElements"));
        assertTrue(responseBody.containsKey("totalPages"));

        List<?> tasks = (List<?>) responseBody.get("content");
        assertNotNull(tasks);
        assertTrue(tasks.size() <= 10);
    }

    @Test
    @DisplayName("场景1.4: 根据ID获取任务详情")
    @Sql(scripts = "/test-data-us1.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void getTestTaskById_WithValidId_ShouldReturnTaskDetails() {
        // Given: 已存在的任务ID（通过SQL脚本插入）
        String taskId = "task-001"; // UUID from test-data-us1.sql
        String url = getBaseUrl() + "/" + taskId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", "zhangsan");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // When: 发送GET请求
        ResponseEntity<TestTaskResponse> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            entity,
            TestTaskResponse.class
        );

        // Then: 验证响应
        assertEquals(HttpStatus.OK, response.getStatusCode());
        TestTaskResponse task = response.getBody();
        assertNotNull(task);
        assertEquals(taskId, task.getId());
        assertNotNull(task.getTaskName());
    }

    @Test
    @DisplayName("场景1.5: 更新任务状态")
    @Sql(scripts = "/test-data-us1.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void updateTaskStatus_WithValidData_ShouldUpdateSuccessfully() {
        // Given: 准备更新请求
        String taskId = "task-pending"; // UUID from test-data-us1.sql (PENDING status)
        String url = getBaseUrl() + "/" + taskId + "/status";

        Map<String, String> updateRequest = Map.of(
            "status", "RUNNING",
            "updated_by", "zhangsan"
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-User-Id", "zhangsan");
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(updateRequest, headers);

        // When: 发送POST请求更新状态
        ResponseEntity<TestTaskResponse> response = restTemplate.exchange(
            url,
            HttpMethod.POST,
            entity,
            TestTaskResponse.class
        );

        // Then: 验证响应
        assertEquals(HttpStatus.OK, response.getStatusCode());
        TestTaskResponse task = response.getBody();
        assertNotNull(task);
        assertEquals("RUNNING", task.getStatus());
    }

    @Test
    @DisplayName("场景1.6: 参数验证 - 缺少必填字段")
    void createTestTask_WithMissingRequiredFields_ShouldReturnBadRequest() {
        // Given: 准备不完整的请求（缺少taskName, modules, codeChangeInfo）
        CreateTestTaskRequest request = new CreateTestTaskRequest();
        request.setEnvironment("DEV");
        request.setVersion("v1.2.0");
        // taskName、modules、codeChangeInfo未设置（这些都是必填字段）

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-User-Id", "zhangsan");
        HttpEntity<CreateTestTaskRequest> entity = new HttpEntity<>(request, headers);

        // When: 发送POST请求
        ResponseEntity<Map> response = restTemplate.exchange(
            getBaseUrl(),
            HttpMethod.POST,
            entity,
            Map.class
        );

        // Then: 验证返回400错误
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> errorResponse = response.getBody();
        assertNotNull(errorResponse, "Error response should not be null");
        
        // 验证错误响应包含必要的字段
        assertTrue(errorResponse.containsKey("message"), "Response should contain 'message' field");
        assertEquals("Validation failed", errorResponse.get("message"), "Message should indicate validation failure");
        
        // 验证包含字段错误详情
        assertTrue(errorResponse.containsKey("errors"), "Response should contain 'errors' field");
        
        @SuppressWarnings("unchecked")
        Map<String, String> fieldErrors = (Map<String, String>) errorResponse.get("errors");
        assertNotNull(fieldErrors, "Field errors should not be null");
        assertTrue(fieldErrors.size() > 0, "Should have at least one field error");
    }

    @Test
    @DisplayName("场景1.7: 获取不存在的任务 - 返回404")
    void getTestTaskById_WithNonExistingId_ShouldReturnNotFound() {
        // Given: 不存在的任务ID
        String nonExistingId = "non-existing-task-uuid";
        String url = getBaseUrl() + "/" + nonExistingId;

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

        // Then: 验证返回404错误
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    @DisplayName("场景1.8: 删除测试任务（软删除）")
    @Sql(scripts = "/test-data-us1.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void deleteTestTask_WithValidId_ShouldSoftDelete() {
        // Given: 已存在的任务ID
        String taskId = "task-001"; // UUID from test-data-us1.sql
        String url = getBaseUrl() + "/" + taskId;

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

        // 验证再次获取该任务返回404或标记为已删除
        ResponseEntity<Map> getResponse = restTemplate.exchange(
            url,
            HttpMethod.GET,
            entity,
            Map.class
        );
        assertTrue(getResponse.getStatusCode() == HttpStatus.NOT_FOUND ||
                  (getResponse.getBody() != null && getResponse.getBody().containsKey("deleted")));
    }

    @Test
    @DisplayName("场景1.9: AI推荐策略接口")
    @Sql(scripts = "/test-data-us1.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void getAiRecommendation_WithValidContext_ShouldReturnRecommendation() {
        // Given: 准备推荐请求上下文
        String url = getBaseUrl() + "/ai/recommendation";
        Map<String, Object> context = Map.of(
            "environment", "DEV",
            "version", "v1.2.0",
            "modules", List.of("order"),
            "code_change", Map.of(
                "changed_files_count", 5,
                "changed_lines_count", 50
            )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-User-Id", "zhangsan");
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(context, headers);

        // When: 发送POST请求获取AI推荐
        ResponseEntity<Map> response = restTemplate.exchange(
            url,
            HttpMethod.POST,
            entity,
            Map.class
        );

        // Then: 验证响应
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> recommendation = response.getBody();
        assertNotNull(recommendation);

        assertTrue(recommendation.containsKey("test_scope"));
        assertTrue(recommendation.containsKey("environment"));
        assertTrue(recommendation.containsKey("priority"));
        assertTrue(recommendation.containsKey("confidence"));

        // 验证推荐置信度
        Double confidence = (Double) recommendation.get("confidence");
        assertTrue(confidence >= 0 && confidence <= 1);
    }
}
