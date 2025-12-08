package com.synapsetest.testmanagement.controller;

import com.synapsetest.testmanagement.constants.ApiVersion;
import com.synapsetest.testmanagement.dto.ApiResponse;
import com.synapsetest.testmanagement.model.QualityReport;
import com.synapsetest.testmanagement.service.MonitoringService;
import com.synapsetest.testmanagement.service.QualityReportService;
import com.synapsetest.testmanagement.service.QualityTraceabilityService;
import com.synapsetest.testmanagement.service.ReportingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Report Controller
 * REST API endpoints for quality reports and traceability
 *
 * Task: T067 [US3] Implement ReportController
 */
@Tag(name = "质量报告", description = "测试质量报告生成和可追溯性分析")
@RestController
@RequestMapping(ApiVersion.V1 + "/reports")
@RequiredArgsConstructor
// @Profile("mongodb") // Temporarily disabled to show in Swagger UI
public class ReportController {

    private final QualityReportService qualityReportService;
    private final ReportingService reportingService;
    private final QualityTraceabilityService traceabilityService;
    private final MonitoringService monitoringService;

    /**
     * Get quality report by ID
     * GET /api/v1/reports/{id}
     */
    @Operation(summary = "获取质量报告", description = "根据报告ID获取详细的质量报告")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getReport(
            @Parameter(description = "报告ID", required = true, example = "report-123456")
            @PathVariable String id) {
        QualityReport report = qualityReportService.getReportById(id);
        Map<String, Object> response = transformReportToMap(report);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get quality report by task ID
     * GET /api/v1/reports/task/{taskId}
     */
    @Operation(summary = "根据任务获取质量报告", description = "根据测试任务ID获取对应的质量报告")
    @GetMapping("/task/{taskId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getReportByTaskId(
            @Parameter(description = "任务ID", required = true, example = "task-123456")
            @PathVariable String taskId) {
        QualityReport report = qualityReportService.getReportByTaskId(taskId);
        Map<String, Object> response = transformReportToMap(report);
        // Get passRate from monitoring data
        try {
            var monitoringData = monitoringService.getMonitoringDataByTaskId(taskId);
            response.put("passRate", monitoringData.getPassRate());
        } catch (Exception e) {
            // If monitoring data not available, calculate from test results
            if (report.getTestResults() != null && !report.getTestResults().isEmpty()) {
                long passedCount = report.getTestResults().stream()
                        .filter(r -> "PASSED".equals(r.getStatus()))
                        .count();
                double passRate = (double) passedCount / report.getTestResults().size() * 100;
                response.put("passRate", passRate);
            }
        }
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get recent reports
     * GET /api/v1/reports/recent
     */
    @Operation(summary = "获取最近的报告", description = "获取最近生成的质量报告列表")
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRecentReports() {
        List<QualityReport> reports = qualityReportService.getRecentReports();
        List<Map<String, Object>> transformedReports = reports.stream()
                .map(this::transformReportToMap)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(transformedReports));
    }

    /**
     * Generate comprehensive report
     * GET /api/v1/reports/comprehensive/{taskId}
     */
    @Operation(summary = "生成综合报告", description = "生成指定任务的综合质量分析报告")
    @GetMapping("/comprehensive/{taskId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getComprehensiveReport(
            @Parameter(description = "任务ID", required = true, example = "task-123456")
            @PathVariable String taskId) {
        Map<String, Object> report = reportingService.generateComprehensiveReport(taskId);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    /**
     * Generate comparison report
     * GET /api/v1/reports/compare
     */
    @Operation(summary = "对比报告", description = "对比两个任务的测试结果和质量指标")
    @GetMapping("/compare")
    public ResponseEntity<ApiResponse<Map<String, Object>>> compareReports(
            @Parameter(description = "任务1的ID", required = true, example = "task-111111")
            @RequestParam String taskId1,
            @Parameter(description = "任务2的ID", required = true, example = "task-222222")
            @RequestParam(required = false) String taskId2) {
        // Validate parameters
        if (taskId2 == null || taskId2.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("taskId2 parameter is required"));
        }
        Map<String, Object> comparison = reportingService.generateComparisonReport(taskId1, taskId2);
        return ResponseEntity.ok(ApiResponse.success(comparison));
    }

    /**
     * Generate HTML report
     * GET /api/v1/reports/html/{taskId}
     */
    @Operation(summary = "生成HTML报告", description = "生成可视化的HTML格式质量报告")
    @GetMapping("/html/{taskId}")
    public ResponseEntity<String> getHtmlReport(
            @Parameter(description = "任务ID", required = true, example = "task-123456")
            @PathVariable String taskId) {
        String htmlReport = reportingService.generateHtmlReport(taskId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_HTML);

        return ResponseEntity.ok()
                .headers(headers)
                .body(htmlReport);
    }

    /**
     * Trace requirement coverage
     * GET /api/v1/reports/traceability/requirement/{requirementId}
     */
    @Operation(summary = "需求覆盖追溯", description = "追踪指定需求的测试用例覆盖情况")
    @GetMapping("/traceability/requirement/{requirementId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> traceRequirementCoverage(
            @Parameter(description = "需求ID", required = true, example = "REQ-123")
            @PathVariable String requirementId) {
        Map<String, Object> traceability = traceabilityService.traceRequirementCoverage(requirementId);
        return ResponseEntity.ok(ApiResponse.success(traceability));
    }

    /**
     * Generate traceability matrix
     * POST /api/v1/reports/traceability/matrix
     */
    @Operation(summary = "生成追溯矩阵", description = "生成需求与测试用例的追溯关系矩阵")
    @PostMapping("/traceability/matrix")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateTraceabilityMatrix(
            @Parameter(description = "需求ID列表", required = true)
            @RequestBody List<String> requirementIds) {
        // Validate input
        if (requirementIds == null || requirementIds.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("requirementIds list cannot be empty"));
        }
        Map<String, Object> matrix = traceabilityService.generateTraceabilityMatrix(requirementIds);
        return ResponseEntity.ok(ApiResponse.success(matrix));
    }

    /**
     * Check release quality gates
     * GET /api/v1/reports/quality-gates/{taskId}
     */
    @Operation(summary = "质量门禁检查", description = "检查任务是否满足发布质量门禁条件")
    @GetMapping("/quality-gates/{taskId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkQualityGates(
            @Parameter(description = "任务ID", required = true, example = "task-123456")
            @PathVariable String taskId) {
        Map<String, Object> gateCheck = traceabilityService.checkReleaseQualityGates(taskId);
        return ResponseEntity.ok(ApiResponse.success(gateCheck));
    }

    /**
     * Trace defect impact
     * GET /api/v1/reports/traceability/defect/{defectId}
     */
    @Operation(summary = "缺陷影响追溯", description = "追踪缺陷对相关功能和测试用例的影响")
    @GetMapping("/traceability/defect/{defectId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> traceDefectImpact(
            @Parameter(description = "缺陷ID", required = true, example = "BUG-123")
            @PathVariable String defectId) {
        Map<String, Object> impact = traceabilityService.traceDefectImpact(defectId);
        return ResponseEntity.ok(ApiResponse.success(impact));
    }

    /**
     * Analyze change impact
     * POST /api/v1/reports/traceability/change-impact
     */
    @Operation(summary = "变更影响分析", description = "分析代码变更对测试范围的影响")
    @PostMapping("/traceability/change-impact")
    public ResponseEntity<ApiResponse<Map<String, Object>>> analyzeChangeImpact(
            @Parameter(description = "变更ID", required = true, example = "CHG-123")
            @RequestParam String changeId,
            @Parameter(description = "变更文件列表", required = true)
            @RequestBody List<String> changedFiles) {
        Map<String, Object> analysis = traceabilityService.analyzeChangeImpact(changeId, changedFiles);
        return ResponseEntity.ok(ApiResponse.success(analysis));
    }

    /**
     * Transform QualityReport to Map with expected field names
     */
    private Map<String, Object> transformReportToMap(QualityReport report) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", report.getId());
        map.put("taskId", report.getTaskId());
        map.put("reportName", report.getName()); // Transform "name" to "reportName"
        map.put("name", report.getName());
        map.put("summary", report.getSummary());
        map.put("generatedAt", report.getGeneratedAt());
        map.put("createdAt", report.getGeneratedAt()); // Add "createdAt" alias for "generatedAt"
        map.put("status", report.getStatus());
        map.put("defectStats", report.getDefectStats());
        map.put("performanceMetrics", report.getPerformanceMetrics());
        map.put("testResults", report.getTestResults());
        map.put("riskAssessment", report.getRiskAssessment());
        return map;
    }
}
