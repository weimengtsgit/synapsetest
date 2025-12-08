package com.synapsetest.testmanagement.service;

import com.synapsetest.testmanagement.exception.ResourceNotFoundException;
import com.synapsetest.testmanagement.model.QualityReport;
import com.synapsetest.testmanagement.model.RiskAssessment;
import com.synapsetest.testmanagement.model.TestResult;
import com.synapsetest.testmanagement.mapper.QualityReportMapper;
import com.synapsetest.testmanagement.mapper.RiskAssessmentMapper;
import com.synapsetest.testmanagement.mapper.TestResultMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * QualityReport Service
 * Business logic for quality report management
 *
 * Task: T063 [US3] Implement QualityReportService
 */
@Slf4j
@Service
@RequiredArgsConstructor
// @Profile("mongodb") // Temporarily disabled to show in Swagger UI
public class QualityReportService {

    private final QualityReportMapper qualityReportMapper;
    private final TestResultMapper testResultMapper;
    private final RiskAssessmentMapper riskAssessmentMapper;

    /**
     * Generate quality report for a test task
     */
    public QualityReport generateReport(String taskId, List<TestResult> testResults) {
        log.info("Generating quality report for task: {}", taskId);

        QualityReport report = new QualityReport();
        report.setTaskId(taskId);
        report.setName(String.format("Quality Report - Task %s", taskId));
        report.setStatus(QualityReport.Status.GENERATING.name());
        // Generate UUID for the new report
        if (report.getId() == null) {
            report.setId(java.util.UUID.randomUUID().toString());
        }
        report.setGeneratedAt(LocalDateTime.now());

        // Calculate defect statistics
        Map<String, Integer> defectStats = calculateDefectStats(testResults);
        report.setDefectStats(defectStats);

        // Calculate performance metrics
        Map<String, Object> performanceMetrics = calculatePerformanceMetrics(testResults);
        report.setPerformanceMetrics(performanceMetrics);

        // Perform risk assessment
        RiskAssessment riskAssessment = assessRisk(testResults, defectStats);
        report.setRiskAssessment(riskAssessment);

        // Generate summary
        String summary = generateSummary(testResults, defectStats, riskAssessment);
        report.setSummary(summary);

        report.setStatus(QualityReport.Status.COMPLETED.name());

        // Save the main report
        qualityReportMapper.insert(report);
        
        // Save test results if any
        if (testResults != null && !testResults.isEmpty()) {
            for (TestResult result : testResults) {
                // 创建新的测试结果对象并设置报告ID
                TestResult dbResult = new TestResult();
                dbResult.setTestCaseId(result.getTestCaseId());
                dbResult.setTestCaseName(result.getTestCaseName());
                dbResult.setStatus(result.getStatus());
                dbResult.setExecutionTime(result.getExecutionTime());
                dbResult.setError(result.getError());
                dbResult.setScreenshot(result.getScreenshot());
                dbResult.setExecutedAt(result.getExecutedAt());
                // 使用包装对象存储报告ID关联
                testResultMapper.insert(dbResult);
            }
        }
        
        // Save risk assessment if available
        if (report.getRiskAssessment() != null) {
            riskAssessmentMapper.insert(report.getRiskAssessment());
        }
        
        QualityReport saved = report;

        log.info("Quality report generated successfully: {}", saved.getId());

        return saved;
    }

    /**
     * Get report by ID
     */
    public QualityReport getReportById(String id) {
        QualityReport report = qualityReportMapper.selectById(id);
        if (report == null) {
            throw new ResourceNotFoundException("QualityReport", "id", id);
        }

        // Load associated test results
        List<TestResult> testResults = testResultMapper.selectByReportId(id);
        if (!testResults.isEmpty()) {
            report.setTestResults(testResults);
        }

        // Load risk assessment
        RiskAssessment ra = riskAssessmentMapper.selectByReportId(id);
        if (ra != null) {
            report.setRiskAssessment(ra);
        }

        return report;
    }

    /**
     * Get report by task ID
     */
    public QualityReport getReportByTaskId(String taskId) {
        List<QualityReport> reports = qualityReportMapper.selectByTaskId(taskId);
        if (reports == null || reports.isEmpty()) {
            throw new ResourceNotFoundException("QualityReport", "taskId", taskId);
        }
        QualityReport report = reports.get(0);
        
        // Load associated test results
        List<TestResult> testResults = testResultMapper.selectByReportId(report.getId());
        if (!testResults.isEmpty()) {
            report.setTestResults(testResults);
        }
        
        // Load risk assessment
        RiskAssessment ra = riskAssessmentMapper.selectByReportId(report.getId());
        if (ra != null) {
            report.setRiskAssessment(ra);
        }
        
        return report;
    }

    /**
     * Get recent reports
     */
    public List<QualityReport> getRecentReports() {
        List<QualityReport> reports = qualityReportMapper.selectLatest10();
        
        // For recent reports, we'll keep it simple without loading all associations
        return reports;
    }

    /**
     * Calculate defect statistics
     */
    private Map<String, Integer> calculateDefectStats(List<TestResult> testResults) {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("critical", 0);
        stats.put("major", 0);
        stats.put("minor", 0);
        stats.put("total", 0);

        int failedCount = 0;
        for (TestResult result : testResults) {
            if ("FAILED".equals(result.getStatus())) {
                failedCount++;
                // Simple heuristic for severity classification
                // In production, this would analyze error patterns
                if (result.getError() != null) {
                    if (result.getError().toLowerCase().contains("critical") ||
                        result.getError().toLowerCase().contains("security")) {
                        stats.put("critical", stats.get("critical") + 1);
                    } else if (result.getError().toLowerCase().contains("error") ||
                               result.getError().toLowerCase().contains("exception")) {
                        stats.put("major", stats.get("major") + 1);
                    } else {
                        stats.put("minor", stats.get("minor") + 1);
                    }
                }
            }
        }

        stats.put("total", failedCount);

        return stats;
    }

    /**
     * Calculate performance metrics
     */
    private Map<String, Object> calculatePerformanceMetrics(List<TestResult> testResults) {
        Map<String, Object> metrics = new HashMap<>();

        if (testResults.isEmpty()) {
            return metrics;
        }

        // Calculate average response time
        long totalTime = 0;
        long minTime = Long.MAX_VALUE;
        long maxTime = 0;
        int count = 0;

        for (TestResult result : testResults) {
            if (result.getExecutionTime() != null) {
                long time = result.getExecutionTime();
                totalTime += time;
                minTime = Math.min(minTime, time);
                maxTime = Math.max(maxTime, time);
                count++;
            }
        }

        if (count > 0) {
            metrics.put("avgResponseTime", totalTime / count);
            metrics.put("minResponseTime", minTime);
            metrics.put("maxResponseTime", maxTime);
        }

        // Calculate pass rate
        long passedCount = testResults.stream()
                .filter(r -> "PASSED".equals(r.getStatus()))
                .count();
        double passRate = (double) passedCount / testResults.size() * 100;
        metrics.put("passRate", passRate);

        // Calculate throughput
        double throughput = (double) testResults.size() / (totalTime / 1000.0); // tests per second
        metrics.put("throughput", throughput);

        return metrics;
    }

    /**
     * Assess risk based on test results
     */
    private RiskAssessment assessRisk(List<TestResult> testResults,
                                                      Map<String, Integer> defectStats) {
        RiskAssessment assessment = new RiskAssessment();

        // Calculate overall risk score
        double riskScore = calculateRiskScore(testResults, defectStats);
        assessment.setRiskScore(riskScore);

        // Determine overall risk level
        String overallRisk;
        if (riskScore >= 0.8) {
            overallRisk = QualityReport.RiskLevel.CRITICAL.name();
        } else if (riskScore >= 0.6) {
            overallRisk = QualityReport.RiskLevel.HIGH.name();
        } else if (riskScore >= 0.3) {
            overallRisk = QualityReport.RiskLevel.MEDIUM.name();
        } else {
            overallRisk = QualityReport.RiskLevel.LOW.name();
        }
        assessment.setOverallRisk(overallRisk);

        // Identify high-risk modules
        List<String> highRiskModules = identifyHighRiskModules(testResults);
        assessment.setHighRiskModules(highRiskModules);

        // Generate recommendations
        List<String> recommendations = generateRecommendations(riskScore, defectStats, highRiskModules);
        assessment.setRecommendations(recommendations);

        // Calculate module risk scores
        Map<String, Double> moduleRiskScores = calculateModuleRiskScores(testResults);
        assessment.setModuleRiskScores(moduleRiskScores);

        return assessment;
    }

    /**
     * Calculate overall risk score
     */
    private double calculateRiskScore(List<TestResult> testResults,
                                       Map<String, Integer> defectStats) {
        if (testResults.isEmpty()) {
            return 0.0;
        }

        // Factors contributing to risk:
        // 1. Failure rate
        long failedCount = testResults.stream()
                .filter(r -> "FAILED".equals(r.getStatus()))
                .count();
        double failureRate = (double) failedCount / testResults.size();

        // 2. Critical defects
        double criticalWeight = defectStats.getOrDefault("critical", 0) * 0.5;
        double majorWeight = defectStats.getOrDefault("major", 0) * 0.3;
        double minorWeight = defectStats.getOrDefault("minor", 0) * 0.1;

        double defectScore = (criticalWeight + majorWeight + minorWeight) / testResults.size();

        // Combined risk score
        return Math.min((failureRate * 0.6 + defectScore * 0.4), 1.0);
    }

    /**
     * Identify high-risk modules
     */
    private List<String> identifyHighRiskModules(List<TestResult> testResults) {
        // Simple implementation: modules with multiple failures
        // In production: use ML to identify patterns
        Map<String, Integer> moduleFailures = new HashMap<>();

        for (TestResult result : testResults) {
            if ("FAILED".equals(result.getStatus()) && result.getTestCaseName() != null) {
                // Extract module from test case name (simple heuristic)
                String module = extractModuleName(result.getTestCaseName());
                moduleFailures.put(module, moduleFailures.getOrDefault(module, 0) + 1);
            }
        }

        // Return modules with 3+ failures
        List<String> highRiskModules = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : moduleFailures.entrySet()) {
            if (entry.getValue() >= 3) {
                highRiskModules.add(entry.getKey());
            }
        }

        return highRiskModules;
    }

    /**
     * Extract module name from test case name
     */
    private String extractModuleName(String testCaseName) {
        // Simple heuristic: first word or prefix before ':'
        if (testCaseName.contains(":")) {
            return testCaseName.substring(0, testCaseName.indexOf(":")).trim();
        }
        String[] parts = testCaseName.split("\\s+");
        return parts.length > 0 ? parts[0] : "Unknown";
    }

    /**
     * Generate recommendations based on risk assessment
     */
    private List<String> generateRecommendations(double riskScore, Map<String, Integer> defectStats,
                                                  List<String> highRiskModules) {
        List<String> recommendations = new ArrayList<>();

        if (riskScore >= 0.8) {
            recommendations.add("CRITICAL: Release is not recommended. Address critical defects immediately.");
        } else if (riskScore >= 0.6) {
            recommendations.add("HIGH RISK: Consider delaying release until major issues are resolved.");
        } else if (riskScore >= 0.3) {
            recommendations.add("MEDIUM RISK: Review and fix major defects before release.");
        } else {
            recommendations.add("LOW RISK: Quality is acceptable for release.");
        }

        if (!highRiskModules.isEmpty()) {
            recommendations.add(String.format("Focus testing efforts on high-risk modules: %s",
                    String.join(", ", highRiskModules)));
        }

        if (defectStats.getOrDefault("critical", 0) > 0) {
            recommendations.add(String.format("Address %d critical defects before release.",
                    defectStats.get("critical")));
        }

        return recommendations;
    }

    /**
     * Calculate risk scores for each module
     */
    private Map<String, Double> calculateModuleRiskScores(List<TestResult> testResults) {
        Map<String, Double> moduleScores = new HashMap<>();
        Map<String, Integer> moduleTotal = new HashMap<>();
        Map<String, Integer> moduleFailures = new HashMap<>();

        for (TestResult result : testResults) {
            if (result.getTestCaseName() != null) {
                String module = extractModuleName(result.getTestCaseName());
                moduleTotal.put(module, moduleTotal.getOrDefault(module, 0) + 1);

                if ("FAILED".equals(result.getStatus())) {
                    moduleFailures.put(module, moduleFailures.getOrDefault(module, 0) + 1);
                }
            }
        }

        for (String module : moduleTotal.keySet()) {
            int total = moduleTotal.get(module);
            int failures = moduleFailures.getOrDefault(module, 0);
            double riskScore = (double) failures / total;
            moduleScores.put(module, riskScore);
        }

        return moduleScores;
    }

    /**
     * Generate report summary
     */
    private String generateSummary(List<TestResult> testResults,
                                    Map<String, Integer> defectStats,
                                    RiskAssessment riskAssessment) {
        long passedCount = testResults.stream()
                .filter(r -> "PASSED".equals(r.getStatus()))
                .count();
        long failedCount = testResults.stream()
                .filter(r -> "FAILED".equals(r.getStatus()))
                .count();

        return String.format(
                "Quality Report Summary:\n" +
                "Total Test Cases: %d\n" +
                "Passed: %d (%.1f%%)\n" +
                "Failed: %d (%.1f%%)\n" +
                "Critical Defects: %d\n" +
                "Overall Risk: %s (%.1f%%)\n" +
                "Recommendation: %s",
                testResults.size(),
                passedCount,
                (double) passedCount / testResults.size() * 100,
                failedCount,
                (double) failedCount / testResults.size() * 100,
                defectStats.getOrDefault("critical", 0),
                riskAssessment.getOverallRisk(),
                riskAssessment.getRiskScore() * 100,
                riskAssessment.getRecommendations().isEmpty() ? "N/A" :
                        riskAssessment.getRecommendations().get(0)
        );
    }
}
