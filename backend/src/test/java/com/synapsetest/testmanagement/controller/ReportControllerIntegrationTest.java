package com.synapsetest.testmanagement.controller;

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
 * Controller集成测试 - ReportController
 *
 * 测试目标：
 * 1. 验证质量报告的创建和获取
 * 2. 验证报告详情的完整查询
 * 3. 验证报告导出功能
 * 4. 验证与数据库的集成
 *
 * 对应User Story: US4-质量报告生成和管理
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("ReportController API集成测试")
public class ReportControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String getBaseUrl() {
        return "http://localhost:" + port + "/api/v1/reports";
    }

    @Test
    @DisplayName("场景4.1: 创建质量报告")
    @Sql(scripts = "/test-data-us4.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void createQualityReport_ShouldReturnCreatedReport() {
        // Given: 报告创建请求体
        Map<String, Object> requestBody = Map.of(
            "taskId", "1001",
            "name", "测试任务质量报告",
            "environment", "PRODUCTION",
            "version", "1.0.0"
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-User-Id", "lisi");
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        // When: 发送POST请求
        ResponseEntity<Map> response = restTemplate.exchange(
            getBaseUrl(),
            HttpMethod.POST,
            entity,
            Map.class
        );

        // Then: 验证响应
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        Map<String, Object> report = response.getBody();
        assertNotNull(report);

        // 验证报告包含必要字段
        assertTrue(report.containsKey("id"));
        assertEquals("1001", report.get("taskId"));
        assertEquals("测试任务质量报告", report.get("name"));
        assertTrue(report.containsKey("createdAt"));
        assertTrue(report.containsKey("status"));
    }

    @Test
    @DisplayName("场景4.2: 通过任务ID获取质量报告")
    @Sql(scripts = "/test-data-us4.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void getReportByTaskId_ShouldReturnFullReportDetails() {
        // Given: 任务ID
        String taskId = "1001";
        String url = getBaseUrl() + "/task/" + taskId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", "lisi");
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
        Map<String, Object> report = response.getBody();
        assertNotNull(report);

        // 验证报告完整字段
        assertEquals(taskId, report.get("taskId"));
        assertTrue(report.containsKey("summary"));
        assertTrue(report.containsKey("metrics"));
        assertTrue(report.containsKey("testResults"));
        assertTrue(report.containsKey("riskAssessment"));
    }

    @Test
    @DisplayName("场景4.3: 获取报告列表（带分页）")
    @Sql(scripts = "/test-data-us4.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void getReportList_WithPagination_ShouldReturnPaginatedResults() {
        // Given: 分页参数
        String url = getBaseUrl() + "?page=0&size=10&sort=createdAt,desc";

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", "lisi");
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
        Map<String, Object> pageResponse = response.getBody();
        assertNotNull(pageResponse);

        // 验证分页结构
        assertTrue(pageResponse.containsKey("content"));
        assertTrue(pageResponse.containsKey("totalElements"));
        assertTrue(pageResponse.containsKey("totalPages"));
        assertTrue(pageResponse.containsKey("number"));
        assertTrue(pageResponse.containsKey("size"));

        // 验证内容列表
        List<?> reports = (List<?>) pageResponse.get("content");
        assertNotNull(reports);
    }

    @Test
    @DisplayName("场景4.4: 获取报告详情（通过报告ID）")
    @Sql(scripts = "/test-data-us4.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void getReportById_ShouldReturnDetailedReport() {
        // Given: 报告ID (假设测试数据中有ID为"report-001"的报告)
        String reportId = "report-001";
        String url = getBaseUrl() + "/" + reportId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", "lisi");
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
        Map<String, Object> report = response.getBody();
        assertNotNull(report);
        assertEquals(reportId, report.get("id"));

        // 验证详细信息
        assertTrue(report.containsKey("detailedMetrics"));
        assertTrue(report.containsKey("testCases"));
        assertTrue(report.containsKey("performanceData"));
    }

    @Test
    @DisplayName("场景4.5: 导出报告为PDF")
    @Sql(scripts = "/test-data-us4.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void exportReport_AsPDF_ShouldReturnPDFDocument() {
        // Given: 报告ID和导出格式
        String reportId = "report-001";
        String url = getBaseUrl() + "/" + reportId + "/export?format=pdf";

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", "lisi");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // When: 发送GET请求
        ResponseEntity<byte[]> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            entity,
            byte[].class
        );

        // Then: 验证响应
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        // 验证内容不为空
        assertTrue(response.getBody().length > 0);

        // 验证Content-Type
        HttpHeaders responseHeaders = response.getHeaders();
        MediaType contentType = responseHeaders.getContentType();
        assertTrue(contentType != null && 
                  contentType.includes(MediaType.APPLICATION_PDF));
    }

    @Test
    @DisplayName("场景4.6: 导出报告为Excel")
    @Sql(scripts = "/test-data-us4.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void exportReport_AsExcel_ShouldReturnExcelFile() {
        // Given: 报告ID和导出格式
        String reportId = "report-001";
        String url = getBaseUrl() + "/" + reportId + "/export?format=xlsx";

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", "lisi");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // When: 发送GET请求
        ResponseEntity<byte[]> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            entity,
            byte[].class
        );

        // Then: 验证响应
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        // 验证内容不为空
        assertTrue(response.getBody().length > 0);

        // 验证Content-Disposition包含Excel相关信息
        HttpHeaders responseHeaders = response.getHeaders();
        List<String> contentDisposition = responseHeaders.get("Content-Disposition");
        assertTrue(contentDisposition != null && contentDisposition.size() > 0);
        assertTrue(contentDisposition.get(0).contains("xlsx"));
    }

    @Test
    @DisplayName("场景4.7: 获取报告摘要统计")
    @Sql(scripts = "/test-data-us4.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void getReportSummary_ShouldReturnAggregatedStats() {
        // Given: 统计接口
        String url = getBaseUrl() + "/summary";

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", "lisi");
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
        Map<String, Object> summary = response.getBody();
        assertNotNull(summary);

        // 验证统计指标
        assertTrue(summary.containsKey("totalReports"));
        assertTrue(summary.containsKey("passedReports"));
        assertTrue(summary.containsKey("failedReports"));
        assertTrue(summary.containsKey("averageTestCoverage"));
        assertTrue(summary.containsKey("monthlyTrend"));
    }

    @Test
    @DisplayName("场景4.8: 参数验证 - 无效的报告ID格式")
    void getReportById_WithInvalidIdFormat_ShouldReturnBadRequest() {
        // Given: 无效格式的报告ID
        String invalidId = "invalid-report-id-format";
        String url = getBaseUrl() + "/" + invalidId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", "lisi");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // When: 发送GET请求
        ResponseEntity<Map> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            entity,
            Map.class
        );

        // Then: 验证返回400错误
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("场景4.9: 获取不存在的报告 - 返回404")
    void getReportById_WithNonExistingId_ShouldReturnNotFound() {
        // Given: 不存在的报告ID
        String nonExistingId = "non-existing-report-999";
        String url = getBaseUrl() + "/" + nonExistingId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", "lisi");
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
    @DisplayName("场景4.10: 创建报告缺少必要字段")
    void createQualityReport_WithMissingRequiredFields_ShouldReturnBadRequest() {
        // Given: 缺少必要字段的请求体
        Map<String, Object> invalidRequestBody = Map.of(
            "name", "缺少任务ID的报告"
            // 故意缺少taskId字段
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-User-Id", "lisi");
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(invalidRequestBody, headers);

        // When: 发送POST请求
        ResponseEntity<Map> response = restTemplate.exchange(
            getBaseUrl(),
            HttpMethod.POST,
            entity,
            Map.class
        );

        // Then: 验证返回400错误
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}