package com.synapsetest.testmanagement.service;

import com.synapsetest.testmanagement.model.MonitoringData;
import com.synapsetest.testmanagement.model.QualityReport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Reporting Service
 * Business logic for generating comprehensive reports
 *
 * Task: T065 [US3] Implement ReportingService
 */
@Slf4j
@Service
@RequiredArgsConstructor
// @Profile("mongodb") // Temporarily disabled to show in Swagger UI
public class ReportingService {

    private final QualityReportService qualityReportService;
    private final MonitoringService monitoringService;

    /**
     * Generate comprehensive test execution report
     */
    public Map<String, Object> generateComprehensiveReport(String taskId) {
        log.info("Generating comprehensive report for task: {}", taskId);

        Map<String, Object> report = new HashMap<>();

        // Get quality report (will throw ResourceNotFoundException if not found)
        QualityReport qualityReport = qualityReportService.getReportByTaskId(taskId);
        report.put("qualityReport", qualityReport);

        // Get monitoring data (will throw ResourceNotFoundException if not found)
        MonitoringData monitoringData = monitoringService.getMonitoringDataByTaskId(taskId);
        report.put("monitoringData", monitoringData);

        // Generate summary
        Map<String, Object> summary = generateExecutionSummary(qualityReport, monitoringData);
        report.put("summary", summary);

        // Generate trends
        Map<String, Object> trends = generateTrends(taskId);
        report.put("trends", trends);

        report.put("generatedAt", LocalDateTime.now());
        report.put("taskId", taskId);

        return report;
    }

    /**
     * Generate execution summary
     */
    private Map<String, Object> generateExecutionSummary(QualityReport qualityReport,
                                                          MonitoringData monitoringData) {
        Map<String, Object> summary = new HashMap<>();

        // Basic statistics
        summary.put("totalCases", monitoringData.getTotalCases());
        summary.put("executedCases", monitoringData.getExecutedCases());
        summary.put("passedCases", monitoringData.getPassedCases());
        summary.put("failedCases", monitoringData.getFailedCases());
        summary.put("skippedCases", monitoringData.getSkippedCases());
        summary.put("passRate", monitoringData.getPassRate());

        // Execution time
        if (monitoringData.getStartTime() != null && monitoringData.getActualEndTime() != null) {
            long executionMinutes = java.time.Duration.between(
                    monitoringData.getStartTime(),
                    monitoringData.getActualEndTime()
            ).toMinutes();
            summary.put("executionTime", executionMinutes);
        }

        // Defect summary
        summary.put("defectStats", qualityReport.getDefectStats());

        // Risk assessment
        if (qualityReport.getRiskAssessment() != null) {
            summary.put("overallRisk", qualityReport.getRiskAssessment().getOverallRisk());
            summary.put("riskScore", qualityReport.getRiskAssessment().getRiskScore());
        }

        // Status
        summary.put("status", monitoringData.getStatus());
        summary.put("environment", monitoringData.getEnvironment());
        summary.put("version", monitoringData.getVersion());

        return summary;
    }

    /**
     * Generate trends analysis
     */
    private Map<String, Object> generateTrends(String taskId) {
        Map<String, Object> trends = new HashMap<>();

        // Get recent monitoring data for trend analysis
        List<MonitoringData> recentData = monitoringService.getRecentMonitoringData();

        // Calculate pass rate trend
        List<Double> passRateTrend = recentData.stream()
                .limit(10)
                .map(MonitoringData::getPassRate)
                .collect(Collectors.toList());
        Collections.reverse(passRateTrend); // Oldest to newest

        trends.put("passRateTrend", passRateTrend);

        // Calculate average pass rate
        double avgPassRate = passRateTrend.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
        trends.put("averagePassRate", avgPassRate);

        // Detect trend direction
        String trendDirection = detectTrendDirection(passRateTrend);
        trends.put("trendDirection", trendDirection);

        return trends;
    }

    /**
     * Detect trend direction (improving, declining, stable)
     */
    private String detectTrendDirection(List<Double> passRateTrend) {
        if (passRateTrend.size() < 3) {
            return "INSUFFICIENT_DATA";
        }

        // Compare recent average with older average
        int midPoint = passRateTrend.size() / 2;
        double olderAvg = passRateTrend.subList(0, midPoint).stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        double recentAvg = passRateTrend.subList(midPoint, passRateTrend.size()).stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        double diff = recentAvg - olderAvg;

        if (Math.abs(diff) < 2.0) {
            return "STABLE";
        } else if (diff > 0) {
            return "IMPROVING";
        } else {
            return "DECLINING";
        }
    }

    /**
     * Generate comparison report between two tasks/versions
     */
    public Map<String, Object> generateComparisonReport(String taskId1, String taskId2) {
        log.info("Generating comparison report: {} vs {}", taskId1, taskId2);

        Map<String, Object> result = new HashMap<>();

        try {
            QualityReport report1 = qualityReportService.getReportByTaskId(taskId1);
            QualityReport report2 = qualityReportService.getReportByTaskId(taskId2);

            MonitoringData data1 = monitoringService.getMonitoringDataByTaskId(taskId1);
            MonitoringData data2 = monitoringService.getMonitoringDataByTaskId(taskId2);

            // Build task1 summary
            Map<String, Object> task1Summary = new HashMap<>();
            task1Summary.put("taskId", taskId1);
            task1Summary.put("passRate", data1.getPassRate());
            task1Summary.put("defects", report1.getDefectStats());
            task1Summary.put("risk", report1.getRiskAssessment().getOverallRisk());
            result.put("task1", task1Summary);

            // Build task2 summary
            Map<String, Object> task2Summary = new HashMap<>();
            task2Summary.put("taskId", taskId2);
            task2Summary.put("passRate", data2.getPassRate());
            task2Summary.put("defects", report2.getDefectStats());
            task2Summary.put("risk", report2.getRiskAssessment().getOverallRisk());
            result.put("task2", task2Summary);

            // Build comparison details
            Map<String, Object> comparison = new HashMap<>();

            // Compare pass rates
            Map<String, Object> passRateComparison = new HashMap<>();
            passRateComparison.put("task1", data1.getPassRate());
            passRateComparison.put("task2", data2.getPassRate());
            passRateComparison.put("difference", data2.getPassRate() - data1.getPassRate());
            comparison.put("passRate", passRateComparison);

            // Compare defect counts
            Map<String, Object> defectComparison = new HashMap<>();
            defectComparison.put("task1", report1.getDefectStats());
            defectComparison.put("task2", report2.getDefectStats());
            comparison.put("defects", defectComparison);

            // Compare risk levels
            Map<String, Object> riskComparison = new HashMap<>();
            riskComparison.put("task1", report1.getRiskAssessment().getOverallRisk());
            riskComparison.put("task2", report2.getRiskAssessment().getOverallRisk());
            comparison.put("risk", riskComparison);

            // Overall comparison result
            String comparisonResult = determineComparisonResult(data1, data2, report1, report2);
            comparison.put("result", comparisonResult);

            result.put("comparison", comparison);

        } catch (Exception e) {
            log.error("Error generating comparison report", e);
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * Determine comparison result
     */
    private String determineComparisonResult(MonitoringData data1, MonitoringData data2,
                                              QualityReport report1, QualityReport report2) {
        double passRateDiff = data2.getPassRate() - data1.getPassRate();
        double riskDiff = report2.getRiskAssessment().getRiskScore() - report1.getRiskAssessment().getRiskScore();

        if (passRateDiff > 5.0 && riskDiff < -0.1) {
            return "SIGNIFICANTLY_IMPROVED";
        } else if (passRateDiff > 0 && riskDiff <= 0) {
            return "IMPROVED";
        } else if (Math.abs(passRateDiff) <= 2.0 && Math.abs(riskDiff) <= 0.05) {
            return "STABLE";
        } else if (passRateDiff < 0 && riskDiff >= 0) {
            return "DEGRADED";
        } else {
            return "SIGNIFICANTLY_DEGRADED";
        }
    }

    /**
     * Generate HTML report
     */
    public String generateHtmlReport(String taskId) {
        Map<String, Object> report = generateComprehensiveReport(taskId);

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html>\n<head>\n");
        html.append("<title>Test Execution Report - ").append(taskId).append("</title>\n");
        html.append("<style>\n");
        html.append("body { font-family: Arial, sans-serif; margin: 20px; }\n");
        html.append("table { border-collapse: collapse; width: 100%; margin: 20px 0; }\n");
        html.append("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }\n");
        html.append("th { background-color: #4CAF50; color: white; }\n");
        html.append(".passed { color: green; }\n");
        html.append(".failed { color: red; }\n");
        html.append(".risk-low { color: green; }\n");
        html.append(".risk-medium { color: orange; }\n");
        html.append(".risk-high { color: red; }\n");
        html.append("</style>\n");
        html.append("</head>\n<body>\n");

        html.append("<h1>Test Execution Report</h1>\n");
        html.append("<p>Task ID: ").append(taskId).append("</p>\n");
        html.append("<p>Generated: ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("</p>\n");

        // Add summary section
        Map<String, Object> summary = (Map<String, Object>) report.get("summary");
        if (summary != null) {
            html.append("<h2>Summary</h2>\n");
            html.append("<table>\n");
            html.append("<tr><th>Metric</th><th>Value</th></tr>\n");

            summary.forEach((key, value) -> {
                html.append("<tr><td>").append(key).append("</td><td>").append(value).append("</td></tr>\n");
            });

            html.append("</table>\n");
        }

        html.append("</body>\n</html>");

        return html.toString();
    }
}
