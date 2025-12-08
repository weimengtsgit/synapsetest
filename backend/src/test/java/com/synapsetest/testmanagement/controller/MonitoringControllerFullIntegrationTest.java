package com.synapsetest.testmanagement.controller;

import com.synapsetest.testmanagement.dto.ApiResponse;
import com.synapsetest.testmanagement.model.MonitoringData;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MonitoringController 完整集成测试
 *
 * 测试范围：
 * 1. GET /tasks/{taskId} - 获取任务监控数据
 * 2. GET /recent - 获取最近监控数据
 * 3. GET /running - 获取运行中的任务
 * 4. GET /dashboard/stats - 获取仪表盘统计
 * 5. GET /environment/{environment} - 按环境获取监控数据
 * 6. POST /tasks/{taskId}/resource-usage - 更新资源使用
 * 7. POST /tasks/{taskId}/performance-metrics - 更新性能指标
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("MonitoringController 完整集成测试")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MonitoringControllerFullIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String getBaseUrl() {
        return "http://localhost:" + port + "/api/v1/monitoring";
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-User-Name", "zhangsan");
        return headers;
    }

    // ====================================
    // 1. 获取任务监控数据 - GET /tasks/{taskId}
    // ====================================

    @Test
    @Order(1)
    @DisplayName("1.1 获取任务监控数据-成功")
    @Sql(scripts = "/test-data-integration.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testGetMonitoringData_Success() {
        // Given
        String taskId = "task-001";
        HttpEntity<Void> entity = new HttpEntity<>(createHeaders());

        // When
        ResponseEntity<ApiResponse> response = restTemplate.exchange(
            getBaseUrl() + "/tasks/" + taskId,
            HttpMethod.GET,
            entity,
            ApiResponse.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        Map<String, Object> data = (Map<String, Object>) response.getBody().getData();
        assertEquals(taskId, data.get("taskId"));
        assertEquals("RUNNING", data.get("status"));
        assertEquals(65, data.get("progress"));
    }

    @Test
    @Order(2)
    @DisplayName("1.2 获取任务监控数据-不存在的任务")
    @Sql(scripts = "/test-data-integration.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testGetMonitoringData_NotFound() {
        // Given
        String nonExistentTaskId = "task-999999";
        HttpEntity<Void> entity = new HttpEntity<>(createHeaders());

        // When
        ResponseEntity<Map> response = restTemplate.exchange(
            getBaseUrl() + "/tasks/" + nonExistentTaskId,
            HttpMethod.GET,
            entity,
            Map.class
        );

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    // ====================================
    // 2. 获取最近监控数据 - GET /recent
    // ====================================

    @Test
    @Order(3)
    @DisplayName("2.1 获取最近监控数据-成功")
    @Sql(scripts = "/test-data-integration.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testGetRecentMonitoringData_Success() {
        // Given
        HttpEntity<Void> entity = new HttpEntity<>(createHeaders());

        // When
        ResponseEntity<ApiResponse> response = restTemplate.exchange(
            getBaseUrl() + "/recent",
            HttpMethod.GET,
            entity,
            ApiResponse.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        List<Map> data = (List<Map>) response.getBody().getData();
        assertNotNull(data);
        assertTrue(data.size() > 0);

        // 验证数据按时间降序排列（最新的在前面）
        if (data.size() >= 2) {
            assertNotNull(data.get(0).get("timestamp"));
        }
    }

    // ====================================
    // 3. 获取运行中的任务 - GET /running
    // ====================================

    @Test
    @Order(4)
    @DisplayName("3.1 获取运行中的任务-成功")
    @Sql(scripts = "/test-data-integration.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testGetRunningTasks_Success() {
        // Given
        HttpEntity<Void> entity = new HttpEntity<>(createHeaders());

        // When
        ResponseEntity<ApiResponse> response = restTemplate.exchange(
            getBaseUrl() + "/running",
            HttpMethod.GET,
            entity,
            ApiResponse.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<Map> data = (List<Map>) response.getBody().getData();
        assertNotNull(data);
        assertTrue(data.size() >= 2); // 测试数据中有2个RUNNING任务

        // 验证所有返回的任务状态都是RUNNING
        data.forEach(task -> assertEquals("RUNNING", task.get("status")));
    }

    // ====================================
    // 4. 获取仪表盘统计 - GET /dashboard/stats
    // ====================================

    @Test
    @Order(5)
    @DisplayName("4.1 获取仪表盘统计-成功")
    @Sql(scripts = "/test-data-integration.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testGetDashboardStatistics_Success() {
        // Given
        HttpEntity<Void> entity = new HttpEntity<>(createHeaders());

        // When
        ResponseEntity<ApiResponse> response = restTemplate.exchange(
            getBaseUrl() + "/dashboard/stats",
            HttpMethod.GET,
            entity,
            ApiResponse.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> stats = (Map<String, Object>) response.getBody().getData();
        assertNotNull(stats);

        // 验证关键统计指标存在
        assertTrue(stats.containsKey("totalTasks"));
        assertTrue(stats.containsKey("runningTasks"));
        assertTrue(stats.containsKey("completedTasks"));
        assertTrue(stats.containsKey("failedTasks"));
        assertTrue(stats.containsKey("averagePassRate"));

        // 验证数据合理性
        int totalTasks = ((Number) stats.get("totalTasks")).intValue();
        int runningTasks = ((Number) stats.get("runningTasks")).intValue();
        int completedTasks = ((Number) stats.get("completedTasks")).intValue();
        int failedTasks = ((Number) stats.get("failedTasks")).intValue();

        assertTrue(totalTasks >= 0);
        assertTrue(runningTasks >= 0);
        assertTrue(completedTasks >= 0);
        assertTrue(failedTasks >= 0);
        assertEquals(totalTasks, runningTasks + completedTasks + failedTasks + 1); // +1 for PENDING task
    }

    // ====================================
    // 5. 按环境获取监控数据 - GET /environment/{environment}
    // ====================================

    @Test
    @Order(6)
    @DisplayName("5.1 按环境获取监控数据-DEV环境")
    @Sql(scripts = "/test-data-integration.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testGetMonitoringDataByEnvironment_Dev() {
        // Given
        String environment = "DEV";
        HttpEntity<Void> entity = new HttpEntity<>(createHeaders());

        // When
        ResponseEntity<ApiResponse> response = restTemplate.exchange(
            getBaseUrl() + "/environment/" + environment,
            HttpMethod.GET,
            entity,
            ApiResponse.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<Map> data = (List<Map>) response.getBody().getData();
        assertNotNull(data);
        assertTrue(data.size() > 0);

        // 验证所有返回的数据都是DEV环境
        data.forEach(task -> assertEquals(environment, task.get("environment")));
    }

    @Test
    @Order(7)
    @DisplayName("5.2 按环境获取监控数据-PROD环境")
    @Sql(scripts = "/test-data-integration.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testGetMonitoringDataByEnvironment_Prod() {
        // Given
        String environment = "PROD";
        HttpEntity<Void> entity = new HttpEntity<>(createHeaders());

        // When
        ResponseEntity<ApiResponse> response = restTemplate.exchange(
            getBaseUrl() + "/environment/" + environment,
            HttpMethod.GET,
            entity,
            ApiResponse.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<Map> data = (List<Map>) response.getBody().getData();
        assertNotNull(data);
        data.forEach(task -> assertEquals(environment, task.get("environment")));
    }

    @Test
    @Order(8)
    @DisplayName("5.3 按环境获取监控数据-空结果")
    @Sql(scripts = "/test-data-integration.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testGetMonitoringDataByEnvironment_EmptyResult() {
        // Given
        String environment = "NONEXISTENT";
        HttpEntity<Void> entity = new HttpEntity<>(createHeaders());

        // When
        ResponseEntity<ApiResponse> response = restTemplate.exchange(
            getBaseUrl() + "/environment/" + environment,
            HttpMethod.GET,
            entity,
            ApiResponse.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<Map> data = (List<Map>) response.getBody().getData();
        assertNotNull(data);
        assertEquals(0, data.size());
    }

    // ====================================
    // 6. 更新资源使用 - POST /tasks/{taskId}/resource-usage
    // ====================================

    @Test
    @Order(9)
    @DisplayName("6.1 更新资源使用-成功")
    @Sql(scripts = "/test-data-integration.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testUpdateResourceUsage_Success() {
        // Given
        String taskId = "task-001";
        Map<String, Object> resourceUsage = new HashMap<>();
        resourceUsage.put("cpu", 75.5);
        resourceUsage.put("memory", 2048);
        resourceUsage.put("disk", 85);
        resourceUsage.put("network", 1024);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(resourceUsage, createHeaders());

        // When
        ResponseEntity<ApiResponse> response = restTemplate.exchange(
            getBaseUrl() + "/tasks/" + taskId + "/resource-usage",
            HttpMethod.POST,
            entity,
            ApiResponse.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> data = (Map<String, Object>) response.getBody().getData();
        assertNotNull(data);

        // 验证资源使用已更新
        Map<String, Object> updatedResourceUsage = (Map<String, Object>) data.get("resourceUsage");
        assertNotNull(updatedResourceUsage);
        assertEquals(75.5, ((Number) updatedResourceUsage.get("cpu")).doubleValue(), 0.01);
        assertEquals(2048, ((Number) updatedResourceUsage.get("memory")).intValue());
    }

    @Test
    @Order(10)
    @DisplayName("6.2 更新资源使用-任务不存在")
    @Sql(scripts = "/test-data-integration.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testUpdateResourceUsage_TaskNotFound() {
        // Given
        String nonExistentTaskId = "task-999999";
        Map<String, Object> resourceUsage = Map.of("cpu", 50.0);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(resourceUsage, createHeaders());

        // When
        ResponseEntity<Map> response = restTemplate.exchange(
            getBaseUrl() + "/tasks/" + nonExistentTaskId + "/resource-usage",
            HttpMethod.POST,
            entity,
            Map.class
        );

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    // ====================================
    // 7. 更新性能指标 - POST /tasks/{taskId}/performance-metrics
    // ====================================

    @Test
    @Order(11)
    @DisplayName("7.1 更新性能指标-成功")
    @Sql(scripts = "/test-data-integration.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testUpdatePerformanceMetrics_Success() {
        // Given
        String taskId = "task-001";
        Map<String, Object> performanceMetrics = new HashMap<>();
        performanceMetrics.put("avgResponseTime", 150);
        performanceMetrics.put("maxResponseTime", 500);
        performanceMetrics.put("minResponseTime", 50);
        performanceMetrics.put("throughput", 1000);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(performanceMetrics, createHeaders());

        // When
        ResponseEntity<ApiResponse> response = restTemplate.exchange(
            getBaseUrl() + "/tasks/" + taskId + "/performance-metrics",
            HttpMethod.POST,
            entity,
            ApiResponse.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> data = (Map<String, Object>) response.getBody().getData();
        assertNotNull(data);

        // 验证性能指标已更新
        Map<String, Object> updatedMetrics = (Map<String, Object>) data.get("performanceMetrics");
        assertNotNull(updatedMetrics);
        assertEquals(150, ((Number) updatedMetrics.get("avgResponseTime")).intValue());
        assertEquals(500, ((Number) updatedMetrics.get("maxResponseTime")).intValue());
    }

    @Test
    @Order(12)
    @DisplayName("7.2 更新性能指标-任务不存在")
    @Sql(scripts = "/test-data-integration.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testUpdatePerformanceMetrics_TaskNotFound() {
        // Given
        String nonExistentTaskId = "task-999999";
        Map<String, Object> performanceMetrics = Map.of("avgResponseTime", 200);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(performanceMetrics, createHeaders());

        // When
        ResponseEntity<Map> response = restTemplate.exchange(
            getBaseUrl() + "/tasks/" + nonExistentTaskId + "/performance-metrics",
            HttpMethod.POST,
            entity,
            Map.class
        );

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    // ====================================
    // 8. 综合业务流程测试
    // ====================================

    @Test
    @Order(13)
    @DisplayName("8.1 监控数据完整流程测试")
    @Sql(scripts = "/test-data-integration.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testMonitoringDataWorkflow() {
        HttpHeaders headers = createHeaders();
        String taskId = "task-001";

        // 1. 获取任务初始监控数据
        ResponseEntity<ApiResponse> initialResponse = restTemplate.exchange(
            getBaseUrl() + "/tasks/" + taskId,
            HttpMethod.GET,
            new HttpEntity<>(headers),
            ApiResponse.class
        );
        assertEquals(HttpStatus.OK, initialResponse.getStatusCode());
        Map<String, Object> initialData = (Map<String, Object>) initialResponse.getBody().getData();
        assertEquals("RUNNING", initialData.get("status"));

        // 2. 更新资源使用
        Map<String, Object> resourceUsage = Map.of(
            "cpu", 80.0,
            "memory", 3072,
            "disk", 90
        );
        ResponseEntity<ApiResponse> resourceResponse = restTemplate.exchange(
            getBaseUrl() + "/tasks/" + taskId + "/resource-usage",
            HttpMethod.POST,
            new HttpEntity<>(resourceUsage, headers),
            ApiResponse.class
        );
        assertEquals(HttpStatus.OK, resourceResponse.getStatusCode());

        // 3. 更新性能指标
        Map<String, Object> performanceMetrics = Map.of(
            "avgResponseTime", 180,
            "maxResponseTime", 600
        );
        ResponseEntity<ApiResponse> performanceResponse = restTemplate.exchange(
            getBaseUrl() + "/tasks/" + taskId + "/performance-metrics",
            HttpMethod.POST,
            new HttpEntity<>(performanceMetrics, headers),
            ApiResponse.class
        );
        assertEquals(HttpStatus.OK, performanceResponse.getStatusCode());

        // 4. 再次获取监控数据，验证更新成功
        ResponseEntity<ApiResponse> updatedResponse = restTemplate.exchange(
            getBaseUrl() + "/tasks/" + taskId,
            HttpMethod.GET,
            new HttpEntity<>(headers),
            ApiResponse.class
        );
        assertEquals(HttpStatus.OK, updatedResponse.getStatusCode());
        Map<String, Object> updatedData = (Map<String, Object>) updatedResponse.getBody().getData();

        // 验证资源使用和性能指标已更新
        Map<String, Object> updatedResourceUsage = (Map<String, Object>) updatedData.get("resourceUsage");
        Map<String, Object> updatedPerformanceMetrics = (Map<String, Object>) updatedData.get("performanceMetrics");

        assertNotNull(updatedResourceUsage);
        assertNotNull(updatedPerformanceMetrics);
        assertEquals(80.0, ((Number) updatedResourceUsage.get("cpu")).doubleValue(), 0.01);
        assertEquals(180, ((Number) updatedPerformanceMetrics.get("avgResponseTime")).intValue());

        // 5. 验证任务仍在运行中的任务列表中
        ResponseEntity<ApiResponse> runningResponse = restTemplate.exchange(
            getBaseUrl() + "/running",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            ApiResponse.class
        );
        List<Map> runningTasks = (List<Map>) runningResponse.getBody().getData();
        assertTrue(runningTasks.stream().anyMatch(task -> taskId.equals(task.get("taskId"))));
    }
}
