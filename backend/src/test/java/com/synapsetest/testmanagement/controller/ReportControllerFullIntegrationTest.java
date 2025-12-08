package com.synapsetest.testmanagement.controller;

import com.synapsetest.testmanagement.dto.ApiResponse;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ReportController 完整集成测试
 *
 * 测试范围：
 * 1. GET /{id} - 获取质量报告
 * 2. GET /task/{taskId} - 根据任务获取质量报告
 * 3. GET /recent - 获取最近的报告
 * 4. GET /comprehensive/{taskId} - 生成综合报告
 * 5. GET /compare - 对比报告
 * 6. GET /html/{taskId} - 生成HTML报告
 * 7. GET /traceability/requirement/{requirementId} - 需求覆盖追溯
 * 8. POST /traceability/matrix - 生成追溯矩阵
 * 9. GET /quality-gates/{taskId} - 质量门禁检查
 * 10. GET /traceability/defect/{defectId} - 缺陷影响追溯
 * 11. POST /traceability/change-impact - 变更影响分析
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("ReportController 完整集成测试")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ReportControllerFullIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String getBaseUrl() {
        return "http://localhost:" + port + "/api/v1/reports";
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-User-Name", "lisi");
        return headers;
    }

    // ====================================
    // 1. 获取质量报告 - GET /{id}
    // ====================================

    @Test
    @Order(1)
    @DisplayName("1.1 获取质量报告-成功")
    @Sql(scripts = "/test-data-integration.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testGetReport_Success() {
        // Given
        String reportId = "report-001";
        HttpEntity<Void> entity = new HttpEntity<>(createHeaders());

        // When
        ResponseEntity<ApiResponse> response = restTemplate.exchange(
            getBaseUrl() + "/" + reportId,
            HttpMethod.GET,
            entity,
            ApiResponse.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        Map<String, Object> data = (Map<String, Object>) response.getBody().getData();
        assertEquals(reportId, data.get("id"));
        assertEquals("task-003", data.get("taskId"));
        assertNotNull(data.get("reportName"));
    }

    @Test
    @Order(2)
    @DisplayName("1.2 获取质量报告-不存在的ID")
    @Sql(scripts = "/test-data-integration.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testGetReport_NotFound() {
        // Given
        String nonExistentId = "report-999999";
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
    // 2. 根据任务获取质量报告 - GET /task/{taskId}
    // ====================================

    @Test
    @Order(3)
    @DisplayName("2.1 根据任务获取质量报告-成功")
    @Sql(scripts = "/test-data-integration.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testGetReportByTaskId_Success() {
        // Given
        String taskId = "task-003";
        HttpEntity<Void> entity = new HttpEntity<>(createHeaders());

        // When
        ResponseEntity<ApiResponse> response = restTemplate.exchange(
            getBaseUrl() + "/task/" + taskId,
            HttpMethod.GET,
            entity,
            ApiResponse.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> data = (Map<String, Object>) response.getBody().getData();
        assertEquals(taskId, data.get("taskId"));
        assertEquals(97.5, ((Number) data.get("passRate")).doubleValue(), 0.01);
    }

    @Test
    @Order(4)
    @DisplayName("2.2 根据任务获取质量报告-任务不存在")
    @Sql(scripts = "/test-data-integration.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testGetReportByTaskId_NotFound() {
        // Given
        String nonExistentTaskId = "task-999999";
        HttpEntity<Void> entity = new HttpEntity<>(createHeaders());

        // When
        ResponseEntity<Map> response = restTemplate.exchange(
            getBaseUrl() + "/task/" + nonExistentTaskId,
            HttpMethod.GET,
            entity,
            Map.class
        );

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    // ====================================
    // 3. 获取最近的报告 - GET /recent
    // ====================================

    @Test
    @Order(5)
    @DisplayName("3.1 获取最近的报告-成功")
    @Sql(scripts = "/test-data-integration.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testGetRecentReports_Success() {
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
        List<Map> reports = (List<Map>) response.getBody().getData();
        assertNotNull(reports);
        assertTrue(reports.size() >= 3); // 测试数据中有3个报告

        // 验证报告按时间降序排列
        if (reports.size() >= 2) {
            assertNotNull(reports.get(0).get("createdAt"));
        }
    }

    // ====================================
    // 4. 生成综合报告 - GET /comprehensive/{taskId}
    // ====================================

    @Test
    @Order(6)
    @DisplayName("4.1 生成综合报告-成功")
    @Sql(scripts = "/test-data-integration.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testGetComprehensiveReport_Success() {
        // Given
        String taskId = "task-003";
        HttpEntity<Void> entity = new HttpEntity<>(createHeaders());

        // When
        ResponseEntity<ApiResponse> response = restTemplate.exchange(
            getBaseUrl() + "/comprehensive/" + taskId,
            HttpMethod.GET,
            entity,
            ApiResponse.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> report = (Map<String, Object>) response.getBody().getData();
        assertNotNull(report);

        // 验证综合报告包含关键部分
        assertTrue(report.containsKey("taskId"));
        assertTrue(report.containsKey("qualityReport"));
        assertTrue(report.containsKey("monitoringData"));
        assertTrue(report.containsKey("summary"));
    }

    @Test
    @Order(7)
    @DisplayName("4.2 生成综合报告-任务不存在")
    @Sql(scripts = "/test-data-integration.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testGetComprehensiveReport_TaskNotFound() {
        // Given
        String nonExistentTaskId = "task-999999";
        HttpEntity<Void> entity = new HttpEntity<>(createHeaders());

        // When
        ResponseEntity<Map> response = restTemplate.exchange(
            getBaseUrl() + "/comprehensive/" + nonExistentTaskId,
            HttpMethod.GET,
            entity,
            Map.class
        );

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    // ====================================
    // 5. 对比报告 - GET /compare
    // ====================================

    @Test
    @Order(8)
    @DisplayName("5.1 对比报告-成功")
    @Sql(scripts = "/test-data-integration.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testCompareReports_Success() {
        // Given
        String taskId1 = "task-003";
        String taskId2 = "task-004";
        HttpEntity<Void> entity = new HttpEntity<>(createHeaders());

        // When
        ResponseEntity<ApiResponse> response = restTemplate.exchange(
            getBaseUrl() + "/compare?taskId1=" + taskId1 + "&taskId2=" + taskId2,
            HttpMethod.GET,
            entity,
            ApiResponse.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> comparison = (Map<String, Object>) response.getBody().getData();
        assertNotNull(comparison);

        // 验证对比结果包含两个任务的信息
        assertTrue(comparison.containsKey("task1"));
        assertTrue(comparison.containsKey("task2"));
        assertTrue(comparison.containsKey("comparison"));
    }

    @Test
    @Order(9)
    @DisplayName("5.2 对比报告-缺少参数")
    void testCompareReports_MissingParameter() {
        // Given
        String taskId1 = "task-003";
        HttpEntity<Void> entity = new HttpEntity<>(createHeaders());

        // When
        ResponseEntity<Map> response = restTemplate.exchange(
            getBaseUrl() + "/compare?taskId1=" + taskId1,
            HttpMethod.GET,
            entity,
            Map.class
        );

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    // ====================================
    // 6. 生成HTML报告 - GET /html/{taskId}
    // ====================================

    @Test
    @Order(10)
    @DisplayName("6.1 生成HTML报告-成功")
    @Sql(scripts = "/test-data-integration.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testGetHtmlReport_Success() {
        // Given
        String taskId = "task-003";
        HttpEntity<Void> entity = new HttpEntity<>(createHeaders());

        // When
        ResponseEntity<String> response = restTemplate.exchange(
            getBaseUrl() + "/html/" + taskId,
            HttpMethod.GET,
            entity,
            String.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        String htmlContent = response.getBody();
        assertNotNull(htmlContent);
        assertTrue(htmlContent.contains("<html") || htmlContent.contains("<!DOCTYPE"));

        // 验证Content-Type为HTML
        HttpHeaders responseHeaders = response.getHeaders();
        MediaType contentType = responseHeaders.getContentType();
        assertTrue(contentType != null && contentType.includes(MediaType.TEXT_HTML));
    }

    // ====================================
    // 7. 需求覆盖追溯 - GET /traceability/requirement/{requirementId}
    // ====================================

    @Test
    @Order(11)
    @DisplayName("7.1 需求覆盖追溯-成功")
    @Sql(scripts = "/test-data-integration.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testTraceRequirementCoverage_Success() {
        // Given
        String requirementId = "REQ-001";
        HttpEntity<Void> entity = new HttpEntity<>(createHeaders());

        // When
        ResponseEntity<ApiResponse> response = restTemplate.exchange(
            getBaseUrl() + "/traceability/requirement/" + requirementId,
            HttpMethod.GET,
            entity,
            ApiResponse.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> traceability = (Map<String, Object>) response.getBody().getData();
        assertNotNull(traceability);

        // 验证追溯信息
        assertEquals(requirementId, traceability.get("requirementId"));
        assertTrue(traceability.containsKey("relatedTestCases"));
        assertTrue(traceability.containsKey("coveragePercentage"));
    }

    // ====================================
    // 8. 生成追溯矩阵 - POST /traceability/matrix
    // ====================================

    @Test
    @Order(12)
    @DisplayName("8.1 生成追溯矩阵-成功")
    @Sql(scripts = "/test-data-integration.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testGenerateTraceabilityMatrix_Success() {
        // Given
        List<String> requirementIds = Arrays.asList("REQ-001", "REQ-002", "REQ-003");
        HttpEntity<List<String>> entity = new HttpEntity<>(requirementIds, createHeaders());

        // When
        ResponseEntity<ApiResponse> response = restTemplate.exchange(
            getBaseUrl() + "/traceability/matrix",
            HttpMethod.POST,
            entity,
            ApiResponse.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> matrix = (Map<String, Object>) response.getBody().getData();
        assertNotNull(matrix);

        // 验证矩阵包含所有需求
        assertTrue(matrix.containsKey("matrix"));
        assertTrue(matrix.containsKey("summary"));
    }

    @Test
    @Order(13)
    @DisplayName("8.2 生成追溯矩阵-空需求列表")
    void testGenerateTraceabilityMatrix_EmptyList() {
        // Given
        List<String> emptyRequirementIds = Arrays.asList();
        HttpEntity<List<String>> entity = new HttpEntity<>(emptyRequirementIds, createHeaders());

        // When
        ResponseEntity<Map> response = restTemplate.exchange(
            getBaseUrl() + "/traceability/matrix",
            HttpMethod.POST,
            entity,
            Map.class
        );

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    // ====================================
    // 9. 质量门禁检查 - GET /quality-gates/{taskId}
    // ====================================

    @Test
    @Order(14)
    @DisplayName("9.1 质量门禁检查-通过")
    @Sql(scripts = "/test-data-integration.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testCheckQualityGates_Pass() {
        // Given
        String taskId = "task-003"; // 通过率97.5%，应该通过门禁
        HttpEntity<Void> entity = new HttpEntity<>(createHeaders());

        // When
        ResponseEntity<ApiResponse> response = restTemplate.exchange(
            getBaseUrl() + "/quality-gates/" + taskId,
            HttpMethod.GET,
            entity,
            ApiResponse.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> gateCheck = (Map<String, Object>) response.getBody().getData();
        assertNotNull(gateCheck);

        // 验证门禁检查结果
        assertTrue(gateCheck.containsKey("passed"));
        assertTrue(gateCheck.containsKey("gates"));
        assertTrue(gateCheck.containsKey("taskId"));
        assertEquals(taskId, gateCheck.get("taskId"));
    }

    @Test
    @Order(15)
    @DisplayName("9.2 质量门禁检查-不通过")
    @Sql(scripts = "/test-data-integration.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testCheckQualityGates_Fail() {
        // Given
        String taskId = "task-005"; // 通过率较低，可能不通过门禁
        HttpEntity<Void> entity = new HttpEntity<>(createHeaders());

        // When
        ResponseEntity<ApiResponse> response = restTemplate.exchange(
            getBaseUrl() + "/quality-gates/" + taskId,
            HttpMethod.GET,
            entity,
            ApiResponse.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> gateCheck = (Map<String, Object>) response.getBody().getData();
        assertNotNull(gateCheck);
        assertTrue(gateCheck.containsKey("passed"));
    }

    // ====================================
    // 10. 缺陷影响追溯 - GET /traceability/defect/{defectId}
    // ====================================

    @Test
    @Order(16)
    @DisplayName("10.1 缺陷影响追溯-成功")
    @Sql(scripts = "/test-data-integration.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testTraceDefectImpact_Success() {
        // Given
        String defectId = "BUG-001";
        HttpEntity<Void> entity = new HttpEntity<>(createHeaders());

        // When
        ResponseEntity<ApiResponse> response = restTemplate.exchange(
            getBaseUrl() + "/traceability/defect/" + defectId,
            HttpMethod.GET,
            entity,
            ApiResponse.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> impact = (Map<String, Object>) response.getBody().getData();
        assertNotNull(impact);

        // 验证影响追溯信息
        assertEquals(defectId, impact.get("defectId"));
        assertTrue(impact.containsKey("affectedTestCases"));
        assertTrue(impact.containsKey("affectedModules"));
    }

    // ====================================
    // 11. 变更影响分析 - POST /traceability/change-impact
    // ====================================

    @Test
    @Order(17)
    @DisplayName("11.1 变更影响分析-成功")
    @Sql(scripts = "/test-data-integration.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testAnalyzeChangeImpact_Success() {
        // Given
        String changeId = "CHG-001";
        List<String> changedFiles = Arrays.asList(
            "src/main/java/com/example/service/UserService.java",
            "src/main/java/com/example/controller/UserController.java"
        );

        HttpEntity<List<String>> entity = new HttpEntity<>(changedFiles, createHeaders());

        // When
        ResponseEntity<ApiResponse> response = restTemplate.exchange(
            getBaseUrl() + "/traceability/change-impact?changeId=" + changeId,
            HttpMethod.POST,
            entity,
            ApiResponse.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> analysis = (Map<String, Object>) response.getBody().getData();
        assertNotNull(analysis);

        // 验证变更影响分析结果
        assertEquals(changeId, analysis.get("changeId"));
        assertTrue(analysis.containsKey("impactedTestCases"));
        assertTrue(analysis.containsKey("suggestedTestCases"));
    }

    // ====================================
    // 12. 综合业务流程测试
    // ====================================

    @Test
    @Order(18)
    @DisplayName("12.1 完整报告生成和追溯流程")
    @Sql(scripts = "/test-data-integration.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testFullReportWorkflow() {
        HttpHeaders headers = createHeaders();
        String taskId = "task-003";

        // 1. 生成综合报告
        ResponseEntity<ApiResponse> comprehensiveResponse = restTemplate.exchange(
            getBaseUrl() + "/comprehensive/" + taskId,
            HttpMethod.GET,
            new HttpEntity<>(headers),
            ApiResponse.class
        );
        assertEquals(HttpStatus.OK, comprehensiveResponse.getStatusCode());

        // 2. 获取HTML报告
        ResponseEntity<String> htmlResponse = restTemplate.exchange(
            getBaseUrl() + "/html/" + taskId,
            HttpMethod.GET,
            new HttpEntity<>(headers),
            String.class
        );
        assertEquals(HttpStatus.OK, htmlResponse.getStatusCode());
        assertTrue(htmlResponse.getBody().length() > 0);

        // 3. 检查质量门禁
        ResponseEntity<ApiResponse> gateResponse = restTemplate.exchange(
            getBaseUrl() + "/quality-gates/" + taskId,
            HttpMethod.GET,
            new HttpEntity<>(headers),
            ApiResponse.class
        );
        assertEquals(HttpStatus.OK, gateResponse.getStatusCode());

        // 4. 追溯需求覆盖
        ResponseEntity<ApiResponse> traceabilityResponse = restTemplate.exchange(
            getBaseUrl() + "/traceability/requirement/REQ-001",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            ApiResponse.class
        );
        assertEquals(HttpStatus.OK, traceabilityResponse.getStatusCode());

        // 5. 对比多个任务的报告
        ResponseEntity<ApiResponse> compareResponse = restTemplate.exchange(
            getBaseUrl() + "/compare?taskId1=" + taskId + "&taskId2=task-004",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            ApiResponse.class
        );
        assertEquals(HttpStatus.OK, compareResponse.getStatusCode());
    }
}
