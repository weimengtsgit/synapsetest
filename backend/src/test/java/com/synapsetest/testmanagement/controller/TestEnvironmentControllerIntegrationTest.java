package com.synapsetest.testmanagement.controller;

import com.synapsetest.testmanagement.model.TestEnvironment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.jdbc.Sql;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Controller集成测试 - TestEnvironmentController
 *
 * 测试目标：
 * 1. 验证环境管理的完整CRUD操作
 * 2. 验证请求参数验证和错误处理
 * 3. 验证业务逻辑的端到端执行
 * 4. 验证环境状态管理
 *
 * 对应User Story: US1-智能测试任务调度（环境管理）
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("TestEnvironmentController API集成测试")
public class TestEnvironmentControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String getBaseUrl() {
        return "http://localhost:" + port + "/api/v1/test-environments";
    }

    @Test
    @DisplayName("场景2.1: 创建测试环境成功")
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void createEnvironment_WithValidData_ShouldReturnCreated() {
        // Given: 准备创建环境请求
        TestEnvironment request = new TestEnvironment();
        request.setName("QA环境");
        request.setDescription("质量保证测试环境");
        request.setUrl("http://qa.example.com");
        request.setStatus("AVAILABLE");
        
        Map<String, String> config = new HashMap<>();
        config.put("region", "cn-north-1");
        config.put("cluster", "qa-cluster");
        request.setConfig(config);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<TestEnvironment> entity = new HttpEntity<>(request, headers);

        // When: 发送POST请求创建环境
        ResponseEntity<TestEnvironment> response = restTemplate.exchange(
            getBaseUrl(),
            HttpMethod.POST,
            entity,
            TestEnvironment.class
        );

        // Then: 验证响应
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());

        TestEnvironment created = response.getBody();
        assertNotNull(created.getId());
        assertEquals("QA环境", created.getName());
        assertEquals("http://qa.example.com", created.getUrl());
        assertEquals("AVAILABLE", created.getStatus());
        assertNotNull(created.getCreatedAt());
        assertNotNull(created.getConfig());
    }

    @Test
    @DisplayName("场景2.2: 获取所有环境列表")
    @Sql(scripts = "/test-data-us1.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void getAllEnvironments_ShouldReturnEnvironmentList() {
        // Given: 数据库中已有环境数据
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // When: 发送GET请求获取所有环境
        ResponseEntity<TestEnvironment[]> response = restTemplate.exchange(
            getBaseUrl(),
            HttpMethod.GET,
            entity,
            TestEnvironment[].class
        );

        // Then: 验证响应
        assertEquals(HttpStatus.OK, response.getStatusCode());
        TestEnvironment[] environments = response.getBody();
        assertNotNull(environments);
        assertTrue(environments.length >= 2, "Should have at least 2 environments from test data");
    }

    @Test
    @DisplayName("场景2.3: 根据ID获取环境详情")
    @Sql(scripts = "/test-data-us1.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void getEnvironmentById_WithValidId_ShouldReturnEnvironment() {
        // Given: 已存在的环境ID
        String envId = "env-dev"; // UUID from test-data-us1.sql
        String url = getBaseUrl() + "/" + envId;

        HttpHeaders headers = new HttpHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // When: 发送GET请求
        ResponseEntity<TestEnvironment> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            entity,
            TestEnvironment.class
        );

        // Then: 验证响应
        assertEquals(HttpStatus.OK, response.getStatusCode());
        TestEnvironment environment = response.getBody();
        assertNotNull(environment);
        assertEquals(envId, environment.getId());
        assertEquals("DEV环境", environment.getName());
        assertEquals("AVAILABLE", environment.getStatus());
    }

    @Test
    @DisplayName("场景2.4: 根据名称获取环境")
    @Sql(scripts = "/test-data-us1.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void getEnvironmentByName_WithValidName_ShouldReturnEnvironment() {
        // Given: 已存在的环境名称
        String envName = "DEV环境";
        String url = getBaseUrl() + "/by-name/" + envName;

        HttpHeaders headers = new HttpHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // When: 发送GET请求
        ResponseEntity<TestEnvironment> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            entity,
            TestEnvironment.class
        );

        // Then: 验证响应
        assertEquals(HttpStatus.OK, response.getStatusCode());
        TestEnvironment environment = response.getBody();
        assertNotNull(environment);
        assertEquals("DEV环境", environment.getName());
    }

    @Test
    @DisplayName("场景2.5: 更新环境信息")
    @Sql(scripts = "/test-data-us1.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void updateEnvironment_WithValidData_ShouldReturnUpdated() {
        // Given: 准备更新请求
        String envId = "env-dev";
        String url = getBaseUrl() + "/" + envId;

        TestEnvironment updateRequest = new TestEnvironment();
        updateRequest.setName("DEV环境（已更新）");
        updateRequest.setDescription("开发环境 - 更新后");
        updateRequest.setUrl("http://dev-updated.example.com");
        updateRequest.setStatus("MAINTENANCE");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<TestEnvironment> entity = new HttpEntity<>(updateRequest, headers);

        // When: 发送PUT请求
        ResponseEntity<TestEnvironment> response = restTemplate.exchange(
            url,
            HttpMethod.PUT,
            entity,
            TestEnvironment.class
        );

        // Then: 验证响应
        assertEquals(HttpStatus.OK, response.getStatusCode());
        TestEnvironment updated = response.getBody();
        assertNotNull(updated);
        assertEquals(envId, updated.getId());
        assertEquals("DEV环境（已更新）", updated.getName());
        assertEquals("http://dev-updated.example.com", updated.getUrl());
        assertEquals("MAINTENANCE", updated.getStatus());
    }

    @Test
    @DisplayName("场景2.6: 更新环境状态")
    @Sql(scripts = "/test-data-us1.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void updateEnvironmentStatus_WithValidStatus_ShouldUpdateSuccessfully() {
        // Given: 准备状态更新请求
        String envId = "env-dev";
        String url = getBaseUrl() + "/" + envId + "/status";

        Map<String, String> statusUpdate = Map.of("status", "MAINTENANCE");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(statusUpdate, headers);

        // When: 发送POST请求更新状态
        ResponseEntity<TestEnvironment> response = restTemplate.exchange(
            url,
            HttpMethod.POST,
            entity,
            TestEnvironment.class
        );

        // Then: 验证响应
        assertEquals(HttpStatus.OK, response.getStatusCode());
        TestEnvironment updated = response.getBody();
        assertNotNull(updated);
        assertEquals("MAINTENANCE", updated.getStatus());
    }

    @Test
    @DisplayName("场景2.7: 删除测试环境")
    @Sql(scripts = "/test-data-us1.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void deleteEnvironment_WithValidId_ShouldDeleteSuccessfully() {
        // Given: 已存在的环境ID
        String envId = "env-staging";
        String url = getBaseUrl() + "/" + envId;

        HttpHeaders headers = new HttpHeaders();
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

        // 验证再次获取该环境返回404
        ResponseEntity<Map<String, Object>> getResponse = restTemplate.exchange(
            url,
            HttpMethod.GET,
            entity,
            (Class<Map<String, Object>>)(Class<?>)Map.class
        );
        assertEquals(HttpStatus.NOT_FOUND, getResponse.getStatusCode());
    }

    @Test
    @DisplayName("场景2.8: 获取不存在的环境 - 返回404")
    void getEnvironmentById_WithNonExistingId_ShouldReturnNotFound() {
        // Given: 不存在的环境ID
        String nonExistingId = "non-existing-env-id";
        String url = getBaseUrl() + "/" + nonExistingId;

        HttpHeaders headers = new HttpHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // When: 发送GET请求
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            entity,
            (Class<Map<String, Object>>)(Class<?>)Map.class
        );

        // Then: 验证返回404错误
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    @DisplayName("场景2.9: 创建环境缺少必填字段 - 返回400")
    void createEnvironment_WithMissingRequiredFields_ShouldReturnBadRequest() {
        // Given: 准备不完整的请求（缺少name和status）
        TestEnvironment request = new TestEnvironment();
        request.setDescription("测试环境");
        request.setUrl("http://test.example.com");
        // name和status未设置（必填字段）

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<TestEnvironment> entity = new HttpEntity<>(request, headers);

        // When: 发送POST请求
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            getBaseUrl(),
            HttpMethod.POST,
            entity,
            (Class<Map<String, Object>>)(Class<?>)Map.class
        );

        // Then: 验证返回400错误
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertTrue(errorResponse.containsKey("message"));
        assertEquals("Validation failed", errorResponse.get("message"));
    }

    @Test
    @DisplayName("场景2.10: 按状态过滤环境列表")
    @Sql(scripts = "/test-data-us1.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void getAllEnvironments_WithStatusFilter_ShouldReturnFilteredList() {
        // Given: 状态过滤参数
        String url = getBaseUrl() + "?status=AVAILABLE";

        HttpHeaders headers = new HttpHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // When: 发送GET请求
        ResponseEntity<TestEnvironment[]> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            entity,
            TestEnvironment[].class
        );

        // Then: 验证响应
        assertEquals(HttpStatus.OK, response.getStatusCode());
        TestEnvironment[] environments = response.getBody();
        assertNotNull(environments);

        // 验证所有返回的环境都是AVAILABLE状态
        for (TestEnvironment env : environments) {
            assertEquals("AVAILABLE", env.getStatus(), "All environments should be AVAILABLE");
        }
    }

    @Test
    @DisplayName("场景2.11: 更新不存在的环境 - 返回404")
    void updateEnvironment_WithNonExistingId_ShouldReturnNotFound() {
        // Given: 不存在的环境ID
        String nonExistingId = "non-existing-env-id";
        String url = getBaseUrl() + "/" + nonExistingId;

        TestEnvironment updateRequest = new TestEnvironment();
        updateRequest.setName("更新的环境");
        updateRequest.setStatus("AVAILABLE");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<TestEnvironment> entity = new HttpEntity<>(updateRequest, headers);

        // When: 发送PUT请求
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            url,
            HttpMethod.PUT,
            entity,
            (Class<Map<String, Object>>)(Class<?>)Map.class
        );

        // Then: 验证返回404错误
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    @DisplayName("场景2.12: 删除不存在的环境 - 返回404")
    void deleteEnvironment_WithNonExistingId_ShouldReturnNotFound() {
        // Given: 不存在的环境ID
        String nonExistingId = "non-existing-env-id";
        String url = getBaseUrl() + "/" + nonExistingId;

        HttpHeaders headers = new HttpHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // When: 发送DELETE请求
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            url,
            HttpMethod.DELETE,
            entity,
            (Class<Map<String, Object>>)(Class<?>)Map.class
        );

        // Then: 验证返回404错误
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    @DisplayName("场景2.13: 创建重复名称的环境 - 应处理唯一性约束")
    @Sql(scripts = "/test-data-us1.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void createEnvironment_WithDuplicateName_ShouldReturnError() {
        // Given: 准备与已存在环境同名的请求
        TestEnvironment request = new TestEnvironment();
        request.setName("DEV环境"); // 与test-data-us1.sql中的环境名称重复
        request.setDescription("重复的环境名称");
        request.setUrl("http://duplicate.example.com");
        request.setStatus("AVAILABLE");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<TestEnvironment> entity = new HttpEntity<>(request, headers);

        // When: 发送POST请求
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            getBaseUrl(),
            HttpMethod.POST,
            entity,
            (Class<Map<String, Object>>)(Class<?>)Map.class
        );

        // Then: 验证返回错误（可能是400或500，取决于数据库约束处理）
        assertTrue(response.getStatusCode().is4xxClientError() || 
                   response.getStatusCode().is5xxServerError(),
                   "Should return error for duplicate name");
    }

    @Test
    @DisplayName("场景2.14: 部分更新环境（只更新部分字段）")
    @Sql(scripts = "/test-data-us1.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void updateEnvironment_PartialUpdate_ShouldUpdateOnlyProvidedFields() {
        // Given: 准备部分更新请求（只更新description和url，保留name和status）
        String envId = "env-dev";
        String url = getBaseUrl() + "/" + envId;

        TestEnvironment updateRequest = new TestEnvironment();
        updateRequest.setName("DEV环境"); // 保留原始名称（必填字段）
        updateRequest.setStatus("AVAILABLE"); // 保留原始状态（必填字段）
        updateRequest.setDescription("更新后的描述信息");
        updateRequest.setUrl("http://dev-new-url.example.com");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<TestEnvironment> entity = new HttpEntity<>(updateRequest, headers);

        // When: 发送PUT请求
        ResponseEntity<TestEnvironment> response = restTemplate.exchange(
            url,
            HttpMethod.PUT,
            entity,
            TestEnvironment.class
        );

        // Then: 验证响应
        assertEquals(HttpStatus.OK, response.getStatusCode());
        TestEnvironment updated = response.getBody();
        assertNotNull(updated);
        
        // 验证更新的字段
        assertEquals("更新后的描述信息", updated.getDescription());
        assertEquals("http://dev-new-url.example.com", updated.getUrl());
        
        // 验证未更新的字段保持原值
        assertEquals("DEV环境", updated.getName()); // 名称应该保持不变
        assertEquals("AVAILABLE", updated.getStatus()); // 状态应该保持不变
    }

    @Test
    @DisplayName("场景2.15: 状态转换 - AVAILABLE到MAINTENANCE")
    @Sql(scripts = "/test-data-us1.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void updateEnvironmentStatus_FromAvailableToMaintenance_ShouldSucceed() {
        // Given: AVAILABLE状态的环境
        String envId = "env-dev";
        String url = getBaseUrl() + "/" + envId + "/status";

        Map<String, String> statusUpdate = Map.of("status", "MAINTENANCE");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(statusUpdate, headers);

        // When: 发送POST请求
        ResponseEntity<TestEnvironment> response = restTemplate.exchange(
            url,
            HttpMethod.POST,
            entity,
            TestEnvironment.class
        );

        // Then: 验证状态更新成功
        assertEquals(HttpStatus.OK, response.getStatusCode());
        TestEnvironment updated = response.getBody();
        assertNotNull(updated);
        assertEquals("MAINTENANCE", updated.getStatus());
        
        // 验证其他字段未改变
        assertEquals("DEV环境", updated.getName());
    }
}
