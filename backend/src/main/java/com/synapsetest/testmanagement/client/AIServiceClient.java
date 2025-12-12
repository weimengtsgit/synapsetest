package com.synapsetest.testmanagement.client;

import com.synapsetest.testmanagement.exception.AIServiceException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI Service HTTP Client
 * Handles all HTTP communications with AI-Service
 *
 * Features:
 * - Circuit Breaker pattern for fault tolerance
 * - Retry mechanism for transient failures
 * - Timeout configuration
 * - Structured logging
 */
@Slf4j
@Component
public class AIServiceClient {

    private final RestTemplate restTemplate;

    @Value("${services.ai-service.url}")
    private String aiServiceUrl;

    @Value("${services.ai-service.base-path}")
    private String basePath;

    public AIServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Generate test cases from requirement text
     *
     * @param request AI generation request
     * @return AI generation response
     */
    @CircuitBreaker(name = "aiService", fallbackMethod = "generateTestCasesFallback")
    @Retry(name = "aiService")
    public Map<String, Object> generateTestCases(Map<String, Object> request) {
        String url = buildUrl("/testcase/generate");

        log.info("Calling AI Service to generate test cases: {}", url);

        try {
            HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(request, createHeaders());

            ResponseEntity<Map> response = restTemplate.postForEntity(
                url,
                httpEntity,
                Map.class
            );

            Map<String, Object> result = response.getBody();
            log.info("AI Service returned response for test case generation");

            return result;

        } catch (Exception e) {
            log.error("Failed to call AI Service: {}", e.getMessage(), e);
            throw new AIServiceException("Failed to generate test cases from AI Service", e);
        }
    }

    /**
     * Batch generate test cases
     *
     * @param request Batch generation request
     * @return Batch generation response
     */
    @CircuitBreaker(name = "aiService", fallbackMethod = "batchGenerateFallback")
    @Retry(name = "aiService")
    public Map<String, Object> batchGenerate(Map<String, Object> request) {
        String url = buildUrl("/testcase/generate/batch");

        log.info("Calling AI Service to batch generate test cases");

        try {
            HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(request, createHeaders());

            ResponseEntity<Map> response = restTemplate.postForEntity(
                url,
                httpEntity,
                Map.class
            );

            return response.getBody();

        } catch (Exception e) {
            log.error("Failed to batch generate from AI Service: {}", e.getMessage(), e);
            throw new AIServiceException("Failed to batch generate test cases", e);
        }
    }

    /**
     * Deduplicate test cases
     *
     * @param request Deduplication request
     * @return Deduplication response
     */
    @CircuitBreaker(name = "aiService", fallbackMethod = "deduplicateFallback")
    @Retry(name = "aiService")
    public Map<String, Object> deduplicateTestCases(Map<String, Object> request) {
        String url = buildUrl("/testcase/optimize/deduplicate");

        log.info("Calling AI Service to deduplicate test cases");

        try {
            HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(request, createHeaders());

            ResponseEntity<Map> response = restTemplate.postForEntity(
                url,
                httpEntity,
                Map.class
            );

            return response.getBody();

        } catch (Exception e) {
            log.error("Failed to deduplicate test cases: {}", e.getMessage(), e);
            throw new AIServiceException("Failed to deduplicate test cases", e);
        }
    }

    /**
     * Prioritize test cases
     *
     * @param request Prioritization request
     * @return Prioritization response
     */
    @CircuitBreaker(name = "aiService", fallbackMethod = "prioritizeFallback")
    @Retry(name = "aiService")
    public Map<String, Object> prioritizeTestCases(Map<String, Object> request) {
        String url = buildUrl("/testcase/optimize/prioritize");

        log.info("Calling AI Service to prioritize test cases");

        try {
            HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(request, createHeaders());

            ResponseEntity<Map> response = restTemplate.postForEntity(
                url,
                httpEntity,
                Map.class
            );

            return response.getBody();

        } catch (Exception e) {
            log.error("Failed to prioritize test cases: {}", e.getMessage(), e);
            throw new AIServiceException("Failed to prioritize test cases", e);
        }
    }

    /**
     * Analyze test case quality
     *
     * @param testcases List of test cases
     * @return Quality analysis response
     */
    @CircuitBreaker(name = "aiService", fallbackMethod = "analyzeQualityFallback")
    @Retry(name = "aiService")
    public Map<String, Object> analyzeQuality(List<Map<String, Object>> testcases) {
        String url = buildUrl("/testcase/analyze/quality");

        log.info("Calling AI Service to analyze quality of {} test cases", testcases.size());

        try {
            // AI Service expects a direct array, not wrapped in an object
            HttpEntity<List<Map<String, Object>>> httpEntity = new HttpEntity<>(testcases, createHeaders());

            ResponseEntity<Map> response = restTemplate.postForEntity(
                url,
                httpEntity,
                Map.class
            );

            return response.getBody();

        } catch (Exception e) {
            log.error("Failed to analyze test case quality: {}", e.getMessage(), e);
            throw new AIServiceException("Failed to analyze test case quality", e);
        }
    }

    /**
     * Submit user feedback for AI-generated test cases
     *
     * @param request Feedback request
     * @return Feedback response
     */
    @CircuitBreaker(name = "aiService", fallbackMethod = "submitFeedbackFallback")
    @Retry(name = "aiService")
    public Map<String, Object> submitFeedback(Map<String, Object> request) {
        String url = buildUrl("/testcase/feedback");

        log.info("Calling AI Service to submit feedback");

        try {
            HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(request, createHeaders());

            ResponseEntity<Map> response = restTemplate.postForEntity(
                url,
                httpEntity,
                Map.class
            );

            Map<String, Object> result = response.getBody();
            log.info("Feedback submitted successfully");

            return result;

        } catch (Exception e) {
            log.error("Failed to submit feedback to AI Service: {}", e.getMessage(), e);
            throw new AIServiceException("Failed to submit feedback", e);
        }
    }

    /**
     * Get AI test strategy recommendation
     *
     * @param request Recommendation request containing task context
     * @return Recommendation response
     */
    @CircuitBreaker(name = "aiService", fallbackMethod = "getRecommendationFallback")
    @Retry(name = "aiService")
    public Map<String, Object> getRecommendation(Map<String, Object> request) {
        String url = buildUrl("/recommendation/strategy");

        log.info("Calling AI Service for strategy recommendation");

        try {
            HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(request, createHeaders());

            ResponseEntity<Map> response = restTemplate.postForEntity(
                url,
                httpEntity,
                Map.class
            );

            Map<String, Object> result = response.getBody();
            log.info("Strategy recommendation received successfully");

            return result;

        } catch (Exception e) {
            log.error("Failed to get strategy recommendation: {}", e.getMessage(), e);
            throw new AIServiceException("Failed to get strategy recommendation", e);
        }
    }

    /**
     * Explain AI recommendation
     *
     * @param request Explanation request containing recommendation and context
     * @return Explanation response
     */
    @CircuitBreaker(name = "aiService", fallbackMethod = "explainRecommendationFallback")
    @Retry(name = "aiService")
    public Map<String, Object> explainRecommendation(Map<String, Object> request) {
        String url = buildUrl("/recommendation/strategy/explain");

        log.info("Calling AI Service for recommendation explanation");

        try {
            HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(request, createHeaders());

            ResponseEntity<Map> response = restTemplate.postForEntity(
                url,
                httpEntity,
                Map.class
            );

            Map<String, Object> result = response.getBody();
            log.info("Recommendation explanation received successfully");

            return result;

        } catch (Exception e) {
            log.error("Failed to get recommendation explanation: {}", e.getMessage(), e);
            throw new AIServiceException("Failed to get recommendation explanation", e);
        }
    }

    /**
     * Check AI Service health
     *
     * @return Health status
     */
    @CircuitBreaker(name = "aiService", fallbackMethod = "checkHealthFallback")
    @Retry(name = "aiService")
    public Map<String, Object> checkHealth() {
        String url = buildUrl("/testcase/health");

        log.info("Checking AI Service health");

        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            Map<String, Object> result = response.getBody();
            log.info("AI Service health check successful");

            return result;

        } catch (Exception e) {
            log.error("AI Service health check failed: {}", e.getMessage(), e);
            throw new AIServiceException("AI Service health check failed", e);
        }
    }

    // ==================== Fallback Methods ====================

    /**
     * Fallback method for generateTestCases
     */
    private Map<String, Object> generateTestCasesFallback(
            Map<String, Object> request, Exception e) {
        log.warn("AI Service is unavailable, using fallback for test case generation: {}", e.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "AI服务暂时不可用，请稍后重试或使用手动创建功能");
        response.put("generated_cases", Collections.emptyList());
        response.put("confidence_score", 0.0);

        return response;
    }

    /**
     * Fallback method for batch generate
     */
    private Map<String, Object> batchGenerateFallback(
            Map<String, Object> request, Exception e) {
        log.warn("AI Service is unavailable, using fallback for batch generation: {}", e.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "AI服务暂时不可用，批量生成失败");
        response.put("results", Collections.emptyList());

        return response;
    }

    /**
     * Fallback method for deduplicate
     */
    private Map<String, Object> deduplicateFallback(
            Map<String, Object> request, Exception e) {
        log.warn("AI Service is unavailable, using fallback for deduplication: {}", e.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("optimized_cases", request.get("testcases"));
        response.put("duplicate_groups", Collections.emptyList());
        response.put("reduction_rate", 0.0);
        response.put("summary", "AI服务不可用，未进行去重");

        return response;
    }

    /**
     * Fallback method for prioritize
     */
    private Map<String, Object> prioritizeFallback(
            Map<String, Object> request, Exception e) {
        log.warn("AI Service is unavailable, using fallback for prioritization: {}", e.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("prioritized_cases", Collections.emptyList());
        response.put("summary", "AI服务不可用，未进行优先级排序");

        return response;
    }

    /**
     * Fallback method for quality analysis
     */
    private Map<String, Object> analyzeQualityFallback(
            List<Map<String, Object>> testcases, Exception e) {
        log.warn("AI Service is unavailable, using fallback for quality analysis: {}", e.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("overallScore", 0.0);
        response.put("issues", Collections.emptyList());
        response.put("recommendations", Collections.emptyList());
        response.put("message", "AI服务不可用，无法进行质量分析");

        return response;
    }

    /**
     * Fallback method for feedback submission
     */
    private Map<String, Object> submitFeedbackFallback(
            Map<String, Object> request, Exception e) {
        log.warn("AI Service is unavailable, using fallback for feedback submission: {}", e.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "AI服务暂时不可用，反馈提交失败，请稍后重试");
        response.put("request_id", request.get("request_id"));

        return response;
    }

    /**
     * Fallback method for strategy recommendation
     */
    private Map<String, Object> getRecommendationFallback(
            Map<String, Object> request, Exception e) {
        log.warn("AI Service is unavailable, using fallback for strategy recommendation: {}", e.getMessage());

        Map<String, Object> fallbackRecommendation = new HashMap<>();
        fallbackRecommendation.put("task_id", request.get("task_id"));

        Map<String, Object> recommendation = new HashMap<>();
        recommendation.put("test_scope", "CORE");
        recommendation.put("environment", "STAGING");
        recommendation.put("priority", 5);
        recommendation.put("estimated_duration", 60);
        recommendation.put("risk_level", "MEDIUM");
        fallbackRecommendation.put("recommendation", recommendation);

        Map<String, Object> riskAssessment = new HashMap<>();
        riskAssessment.put("risk_level", "MEDIUM");
        riskAssessment.put("risk_factors", Collections.emptyList());
        fallbackRecommendation.put("risk_assessment", riskAssessment);

        fallbackRecommendation.put("environment_recommendations", Collections.emptyList());
        fallbackRecommendation.put("message", "AI服务暂时不可用，返回默认推荐策略");

        return fallbackRecommendation;
    }

    /**
     * Fallback method for recommendation explanation
     */
    private Map<String, Object> explainRecommendationFallback(
            Map<String, Object> request, Exception e) {
        log.warn("AI Service is unavailable, using fallback for recommendation explanation: {}", e.getMessage());

        Map<String, Object> fallbackExplanation = new HashMap<>();
        fallbackExplanation.put("success", false);

        Map<String, Object> explanation = new HashMap<>();
        explanation.put("summary", "AI服务暂时不可用，无法生成详细解释");
        explanation.put("key_factors", Collections.emptyList());
        explanation.put("confidence_level", 0.0);
        explanation.put("alternatives", Collections.emptyList());
        fallbackExplanation.put("explanation", explanation);

        fallbackExplanation.put("message", "AI服务暂时不可用");

        return fallbackExplanation;
    }

    /**
     * Fallback method for health check
     */
    private Map<String, Object> checkHealthFallback(Exception e) {
        log.warn("AI Service health check failed: {}", e.getMessage());

        Map<String, Object> health = new HashMap<>();
        health.put("status", "DOWN");
        health.put("ai_service_url", aiServiceUrl);
        health.put("message", "AI Service is unavailable: " + e.getMessage());
        health.put("error", e.getClass().getSimpleName());

        return health;
    }

    // ==================== Helper Methods ====================

    /**
     * Build full URL for AI Service endpoint
     */
    private String buildUrl(String endpoint) {
        return aiServiceUrl + basePath + endpoint;
    }

    /**
     * Create HTTP headers
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return headers;
    }
}
