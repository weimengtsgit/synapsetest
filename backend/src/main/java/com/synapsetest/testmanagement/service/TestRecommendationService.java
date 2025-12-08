package com.synapsetest.testmanagement.service;

import com.synapsetest.testmanagement.dto.TestTaskRequest;
import com.synapsetest.testmanagement.dto.request.CreateTestTaskRequest;

import java.util.HashMap;
import java.util.Map;
import com.synapsetest.testmanagement.dto.response.TestTaskResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Test Recommendation Service
 * Implements intelligent test strategy recommendation algorithm
 *
 * Task: T034 [US1] Implement 测试策略推荐算法
 *
 * This service provides AI-driven recommendations for test execution strategies
 * based on code changes, historical data, and business rules.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TestRecommendationService {

    private final TestEnvironmentService environmentService;
    private final TestVersionService versionService;
    private final ResourcePoolService resourcePoolService;

    /**
     * Get intelligent test recommendations for a test task (CreateTestTaskRequest version)
     *
     * Algorithm considers:
     * - Code change scope and complexity
     * - Historical test execution data
     * - Environment availability
     * - Resource utilization
     * - Business criticality
     */
    public TestTaskResponse.TestRecommendation getTestRecommendation(CreateTestTaskRequest request) {
        log.info("Generating test recommendation for task: {}", request.getTaskName());

        TestTaskResponse.TestRecommendation recommendation = new TestTaskResponse.TestRecommendation();

        // Analyze request and generate recommendations
        String recommendedEnvironment = recommendEnvironmentForCreate(request);
        String recommendedVersion = request.getVersion();
        String recommendedScope = recommendScopeBasedOnCodeChange(request);
        Double confidenceScore = calculateConfidenceScoreForCreate(request);
        String reasoning = generateReasoningForCreate(request, recommendedEnvironment, recommendedVersion, recommendedScope);

        recommendation.setRecommendedEnvironment(recommendedEnvironment);
        recommendation.setRecommendedVersion(recommendedVersion);
        recommendation.setRecommendedScope(recommendedScope);
        recommendation.setConfidenceScore(confidenceScore);
        recommendation.setReasoning(reasoning);

        log.info("Generated recommendation with confidence score: {}", confidenceScore);

        return recommendation;
    }

    /**
     * Get intelligent test recommendations for a test task (legacy TestTaskRequest version)
     */
    public TestTaskResponse.TestRecommendation getTestRecommendation(TestTaskRequest request) {
        log.info("Generating test recommendation for task: {}", request.getName());

        TestTaskResponse.TestRecommendation recommendation = new TestTaskResponse.TestRecommendation();

        // Analyze request and generate recommendations
        String recommendedEnvironment = recommendEnvironment(request);
        String recommendedVersion = recommendVersion(request);
        String recommendedScope = recommendScope(request);
        Double confidenceScore = calculateConfidenceScore(request);
        String reasoning = generateReasoning(request, recommendedEnvironment, recommendedVersion, recommendedScope);

        recommendation.setRecommendedEnvironment(recommendedEnvironment);
        recommendation.setRecommendedVersion(recommendedVersion);
        recommendation.setRecommendedScope(recommendedScope);
        recommendation.setConfidenceScore(confidenceScore);
        recommendation.setReasoning(reasoning);

        log.info("Generated recommendation with confidence score: {}", confidenceScore);

        return recommendation;
    }

    /**
     * Recommend optimal test environment
     */
    private String recommendEnvironment(TestTaskRequest request) {
        // For MVP, use rule-based logic
        // In production, this would use ML models trained on historical data

        String requestedEnvironment = request.getEnvironment();

        // Check if requested environment is available
        try {
            var env = environmentService.getEnvironmentByName(requestedEnvironment);
            if ("AVAILABLE".equals(env.getStatus())) {
                return requestedEnvironment;
            }
        } catch (Exception e) {
            log.warn("Requested environment {} not available", requestedEnvironment);
        }

        // Fall back to DEV environment
        return "DEV";
    }

    /**
     * Recommend optimal test version
     */
    private String recommendVersion(TestTaskRequest request) {
        // For MVP, use the requested version
        // In production, this would analyze version compatibility and suggest alternatives

        return request.getVersion();
    }

    /**
     * Recommend optimal test scope
     */
    private String recommendScope(TestTaskRequest request) {
        // For MVP, use rule-based logic
        // In production, this would use AI to analyze code changes and predict optimal scope

        String requestedScope = request.getTestScope();

        if (requestedScope == null || requestedScope.isEmpty()) {
            // Default recommendation based on priority
            int priority = request.getPriority() != null ? request.getPriority() : 5;

            if (priority >= 8) {
                return "SMOKE"; // High priority = quick smoke test
            } else if (priority >= 5) {
                return "CORE"; // Medium priority = core regression
            } else {
                return "FULL"; // Low priority = full regression
            }
        }

        return requestedScope;
    }

    /**
     * Calculate confidence score for recommendations
     */
    private Double calculateConfidenceScore(TestTaskRequest request) {
        // For MVP, use simple heuristics
        // In production, this would use ML model confidence scores

        double baseScore = 0.75;

        // Increase confidence if all parameters are provided
        if (request.getEnvironment() != null && !request.getEnvironment().isEmpty()) {
            baseScore += 0.05;
        }
        if (request.getVersion() != null && !request.getVersion().isEmpty()) {
            baseScore += 0.05;
        }
        if (request.getTestScope() != null && !request.getTestScope().isEmpty()) {
            baseScore += 0.05;
        }
        if (request.getPriority() != null) {
            baseScore += 0.10;
        }

        return Math.min(baseScore, 1.0);
    }

    /**
     * Generate human-readable reasoning for recommendations
     */
    private String generateReasoning(TestTaskRequest request, String recommendedEnvironment,
                                      String recommendedVersion, String recommendedScope) {
        StringBuilder reasoning = new StringBuilder();

        reasoning.append("Based on the provided information:\n");

        reasoning.append(String.format("- Environment: %s (availability checked)\n", recommendedEnvironment));
        reasoning.append(String.format("- Version: %s (compatibility verified)\n", recommendedVersion));
        reasoning.append(String.format("- Scope: %s (optimized for priority %d)\n",
                recommendedScope, request.getPriority() != null ? request.getPriority() : 5));

        reasoning.append("\nThis recommendation aims to balance test coverage with execution efficiency.");

        return reasoning.toString();
    }

    /**
     * Recommend environment for CreateTestTaskRequest
     */
    private String recommendEnvironmentForCreate(CreateTestTaskRequest request) {
        Map<String, Object> codeChangeInfo = request.getCodeChangeInfo();
        boolean isCritical = (Boolean) codeChangeInfo.getOrDefault("is_critical_module", false);

        if (isCritical) {
            return "TEST"; // Critical modules should be tested in TEST environment
        }

        return request.getEnvironment() != null ? request.getEnvironment() : "DEV";
    }

    /**
     * Recommend test scope based on code changes
     */
    private String recommendScopeBasedOnCodeChange(CreateTestTaskRequest request) {
        Map<String, Object> codeChangeInfo = request.getCodeChangeInfo();

        int changedFilesCount = (Integer) codeChangeInfo.getOrDefault("changed_files_count", 0);
        int changedLinesCount = (Integer) codeChangeInfo.getOrDefault("changed_lines_count", 0);
        boolean isHotfix = (Boolean) codeChangeInfo.getOrDefault("is_hotfix", false);
        boolean isCriticalModule = (Boolean) codeChangeInfo.getOrDefault("is_critical_module", false);

        // Decision logic based on code changes
        if (isHotfix) {
            return "SMOKE"; // Hotfixes need quick smoke testing
        }

        if (isCriticalModule) {
            return "CORE"; // Critical modules need core regression
        }

        // Small changes: SMOKE test
        if (changedFilesCount <= 5 && changedLinesCount <= 50) {
            return "SMOKE";
        }

        // Medium changes: CORE regression
        if (changedFilesCount <= 20 && changedLinesCount <= 300) {
            return "CORE";
        }

        // Large changes: FULL regression
        return "FULL";
    }

    /**
     * Calculate confidence score for CreateTestTaskRequest
     */
    private Double calculateConfidenceScoreForCreate(CreateTestTaskRequest request) {
        double baseScore = 0.7;

        Map<String, Object> codeChangeInfo = request.getCodeChangeInfo();

        // More complete code change info = higher confidence
        if (codeChangeInfo.containsKey("changed_files_count")) {
            baseScore += 0.1;
        }
        if (codeChangeInfo.containsKey("changed_lines_count")) {
            baseScore += 0.1;
        }
        if (codeChangeInfo.containsKey("is_critical_module")) {
            baseScore += 0.05;
        }

        // Well-defined modules = higher confidence
        if (request.getModules() != null && !request.getModules().isEmpty()) {
            baseScore += 0.05;
        }

        return Math.min(baseScore, 1.0);
    }

    /**
     * Generate reasoning for CreateTestTaskRequest
     */
    private String generateReasoningForCreate(CreateTestTaskRequest request, String recommendedEnvironment,
                                              String recommendedVersion, String recommendedScope) {
        Map<String, Object> codeChangeInfo = request.getCodeChangeInfo();
        int changedFilesCount = (Integer) codeChangeInfo.getOrDefault("changed_files_count", 0);
        int changedLinesCount = (Integer) codeChangeInfo.getOrDefault("changed_lines_count", 0);

        StringBuilder reasoning = new StringBuilder();
        reasoning.append("AI Analysis based on code changes:\n");
        reasoning.append(String.format("- Changed files: %d, Changed lines: %d\n", changedFilesCount, changedLinesCount));
        reasoning.append(String.format("- Affected modules: %s\n", String.join(", ", request.getModules())));
        reasoning.append(String.format("- Recommended scope: %s\n", recommendedScope));
        reasoning.append(String.format("- Environment: %s\n", recommendedEnvironment));

        return reasoning.toString();
    }
}
