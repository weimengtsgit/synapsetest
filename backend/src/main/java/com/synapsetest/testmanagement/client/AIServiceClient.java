package com.synapsetest.testmanagement.client;

import com.synapsetest.testmanagement.dto.AITestCaseGenerationRequest;
import com.synapsetest.testmanagement.dto.ai.*;
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
import java.util.stream.Collectors;

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
    public AITestCaseGenerationResponse generateTestCases(AITestCaseGenerationRequest request) {
        String url = buildUrl("/testcase/generate");

        log.info("Calling AI Service to generate test cases: {}", url);

        try {
            // Build request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("requirement_text", request.getInput());
            requestBody.put("module", "unknown");
            requestBody.put("num_cases", 10);
            requestBody.put("include_edge_cases", true);

            HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(requestBody, createHeaders());

            ResponseEntity<AITestCaseGenerationResponse> response = restTemplate.postForEntity(
                url,
                httpEntity,
                AITestCaseGenerationResponse.class
            );

            AITestCaseGenerationResponse result = response.getBody();
            log.info("AI Service returned {} test cases",
                result != null ? result.getGeneratedCases().size() : 0);

            return result;

        } catch (Exception e) {
            log.error("Failed to call AI Service: {}", e.getMessage(), e);
            throw new AIServiceException("Failed to generate test cases from AI Service", e);
        }
    }

    /**
     * Batch generate test cases
     *
     * @param requests List of generation requests
     * @return Batch generation response
     */
    @CircuitBreaker(name = "aiService", fallbackMethod = "batchGenerateFallback")
    @Retry(name = "aiService")
    public Map<String, Object> batchGenerate(List<AITestCaseGenerationRequest> requests) {
        String url = buildUrl("/testcase/generate/batch");

        log.info("Calling AI Service to batch generate test cases for {} requirements", requests.size());

        try {
            // Build request body
            List<Map<String, Object>> requirements = requests.stream()
                .map(req -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("requirement_text", req.getInput());
                    item.put("module", "unknown");
                    item.put("num_cases", 5);
                    item.put("include_edge_cases", true);
                    return item;
                })
                    .collect(Collectors.toList());

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("requirements", requirements);

            HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(requestBody, createHeaders());

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
    public DeduplicationResponse deduplicateTestCases(DeduplicationRequest request) {
        String url = buildUrl("/testcase/optimize/deduplicate");

        log.info("Calling AI Service to deduplicate {} test cases", request.getTestcases().size());

        try {
            HttpEntity<DeduplicationRequest> httpEntity = new HttpEntity<>(request, createHeaders());

            ResponseEntity<DeduplicationResponse> response = restTemplate.postForEntity(
                url,
                httpEntity,
                DeduplicationResponse.class
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
    public PrioritizationResponse prioritizeTestCases(PrioritizationRequest request) {
        String url = buildUrl("/testcase/optimize/prioritize");

        log.info("Calling AI Service to prioritize {} test cases", request.getTestcases().size());

        try {
            HttpEntity<PrioritizationRequest> httpEntity = new HttpEntity<>(request, createHeaders());

            ResponseEntity<PrioritizationResponse> response = restTemplate.postForEntity(
                url,
                httpEntity,
                PrioritizationResponse.class
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
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("testcases", testcases);

            HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(requestBody, createHeaders());

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

    // ==================== Fallback Methods ====================

    /**
     * Fallback method for generateTestCases
     */
    private AITestCaseGenerationResponse generateTestCasesFallback(
            AITestCaseGenerationRequest request, Exception e) {
        log.warn("AI Service is unavailable, using fallback for test case generation: {}", e.getMessage());

        AITestCaseGenerationResponse response = new AITestCaseGenerationResponse();
        response.setSuccess(false);
        response.setMessage("AI服务暂时不可用，请稍后重试或使用手动创建功能");
        response.setGeneratedCases(Collections.emptyList());
        response.setConfidenceScore(0.0);

        return response;
    }

    /**
     * Fallback method for batch generate
     */
    private Map<String, Object> batchGenerateFallback(
            List<AITestCaseGenerationRequest> requests, Exception e) {
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
    private DeduplicationResponse deduplicateFallback(
            DeduplicationRequest request, Exception e) {
        log.warn("AI Service is unavailable, using fallback for deduplication: {}", e.getMessage());

        DeduplicationResponse response = new DeduplicationResponse();
        response.setOptimizedCases(request.getTestcases());
        response.setDuplicateGroups(Collections.emptyList());
        response.setReductionRate(0.0);
        response.setSummary("AI服务不可用，未进行去重");

        return response;
    }

    /**
     * Fallback method for prioritize
     */
    private PrioritizationResponse prioritizeFallback(
            PrioritizationRequest request, Exception e) {
        log.warn("AI Service is unavailable, using fallback for prioritization: {}", e.getMessage());

        PrioritizationResponse response = new PrioritizationResponse();
        response.setPrioritizedCases(Collections.emptyList());
        response.setSummary("AI服务不可用，未进行优先级排序");

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
