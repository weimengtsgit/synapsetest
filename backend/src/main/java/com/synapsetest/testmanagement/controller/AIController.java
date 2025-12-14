package com.synapsetest.testmanagement.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapsetest.testmanagement.client.AIServiceClient;
import com.synapsetest.testmanagement.constants.ApiVersion;
import com.synapsetest.testmanagement.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * AI Controller
 * Unified controller for all AI-powered features
 *
 * This controller acts as a gateway between Frontend and AI-Service:
 * - Frontend calls Backend (this controller)
 * - Backend forwards requests to AI-Service
 * - Backend applies business logic and saves results
 *
 * Architecture: Frontend -> Backend -> AI-Service
 */
@Slf4j
@RestController
@RequestMapping(ApiVersion.V1 + "/ai")
@RequiredArgsConstructor
@Tag(name = "AI Features", description = "AI-powered test management features")
public class AIController {

    private final AIServiceClient aiServiceClient;
    private final ObjectMapper objectMapper;

    /**
     * Helper method to convert object to JSON string for logging
     */
    private String toJsonString(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return obj.toString();
        }
    }
    /**
     * AI generate test cases from requirement
     *
     * POST /api/v1/ai/testcase/generate
     */
    @PostMapping("/testcase/generate")
    @Operation(summary = "Generate test cases from requirement using AI",
               description = "Uses LLM to intelligently generate test cases from natural language requirements")
    public ApiResponse<Map<String, Object>> generateTestCases(
            @RequestBody Map<String, Object> request) {

        long startTime = System.currentTimeMillis();
        log.info("[REQUEST] POST /api/v1/ai/testcase/generate\n{}", toJsonString(request));

        Map<String, Object> result = aiServiceClient.generateTestCases(request);

        long elapsedTime = System.currentTimeMillis() - startTime;
        log.info("[RESPONSE] POST /api/v1/ai/testcase/generate - Time: {}ms\n{}",
                 elapsedTime, toJsonString(result));

        return ApiResponse.success(result);
    }

    /**
     * AI batch generate test cases
     *
     * POST /api/v1/ai/testcase/generate/batch
     */
    @PostMapping("/testcase/generate/batch")
    @Operation(summary = "Batch generate test cases",
               description = "Generate test cases for multiple requirements at once")
    public ApiResponse<Map<String, Object>> batchGenerateTestCases(
            @RequestBody Map<String, Object> request) {

        long startTime = System.currentTimeMillis();
        log.info("[REQUEST] POST /api/v1/ai/testcase/generate/batch\n{}",
                 toJsonString(request));

        Map<String, Object> result = aiServiceClient.batchGenerate(request);

        long elapsedTime = System.currentTimeMillis() - startTime;
        log.info("[RESPONSE] POST /api/v1/ai/testcase/generate/batch - Time: {}ms\n{}",
                 elapsedTime, toJsonString(result));

        return ApiResponse.success(result);
    }

    /**
     * AI deduplicate test cases
     *
     * POST /api/v1/ai/testcase/optimize/deduplicate
     */
    @PostMapping("/testcase/optimize/deduplicate")
    @Operation(summary = "Deduplicate test cases using AI",
               description = "Uses semantic similarity analysis to identify and remove duplicate test cases")
    public ApiResponse<Map<String, Object>> deduplicateTestCases(
            @RequestBody Map<String, Object> request) {

        long startTime = System.currentTimeMillis();
        log.info("[REQUEST] POST /api/v1/ai/testcase/optimize/deduplicate\n{}",
                 toJsonString(request));

        Map<String, Object> result = aiServiceClient.deduplicateTestCases(request);

        long elapsedTime = System.currentTimeMillis() - startTime;
        log.info("[RESPONSE] POST /api/v1/ai/testcase/optimize/deduplicate - Time: {}ms\n{}",
                 elapsedTime, toJsonString(result));

        return ApiResponse.success(result);
    }

    /**
     * AI prioritize test cases
     *
     * POST /api/v1/ai/testcase/optimize/prioritize
     */
    @PostMapping("/testcase/optimize/prioritize")
    @Operation(summary = "Prioritize test cases using AI",
               description = "Uses ML model to calculate priority scores based on multiple factors")
    public ApiResponse<Map<String, Object>> prioritizeTestCases(
            @RequestBody Map<String, Object> request) {

        long startTime = System.currentTimeMillis();
        log.info("[REQUEST] POST /api/v1/ai/testcase/optimize/prioritize\n{}",
                 toJsonString(request));

        Map<String, Object> result = aiServiceClient.prioritizeTestCases(request);

        long elapsedTime = System.currentTimeMillis() - startTime;
        log.info("[RESPONSE] POST /api/v1/ai/testcase/optimize/prioritize - Time: {}ms\n{}",
                 elapsedTime, toJsonString(result));

        return ApiResponse.success(result);
    }

    /**
     * AI analyze test case quality
     *
     * POST /api/v1/ai/testcase/analyze/quality
     *
     * Accepts two formats:
     * 1. Object format: {"testcases": [...]} (recommended for frontend)
     * 2. Array format: [...] (backward compatibility)
     */
    @PostMapping("/testcase/analyze/quality")
    @Operation(summary = "Analyze test case quality using AI",
               description = "Analyzes test cases and provides quality score and improvement suggestions. " +
                           "Accepts either an array of test cases or an object with 'testcases' key.")
    public ApiResponse<Map<String, Object>> analyzeQuality(
            @RequestBody Object request) {

        long startTime = System.currentTimeMillis();
        List<Map<String, Object>> testcases;

        log.info("[REQUEST] POST /api/v1/ai/testcase/analyze/quality\n{}", toJsonString(request));

        // Handle both formats: direct array or object with "testcases" key
        if (request instanceof List) {
            // Format: [...]
            testcases = (List<Map<String, Object>>) request;
        } else if (request instanceof Map) {
            // Format: {"testcases": [...]}
            Map<String, Object> requestMap = (Map<String, Object>) request;
            testcases = (List<Map<String, Object>>) requestMap.get("testcases");
            if (testcases == null) {
                throw new IllegalArgumentException("Missing 'testcases' field in request body");
            }
        } else {
            throw new IllegalArgumentException(
                "Invalid request format. Expected either an array of test cases or an object with 'testcases' key.");
        }

        Map<String, Object> result = aiServiceClient.analyzeQuality(testcases);

        long elapsedTime = System.currentTimeMillis() - startTime;
        log.info("[RESPONSE] POST /api/v1/ai/testcase/analyze/quality - Test cases: {} - Time: {}ms\n{}",
                 testcases.size(), elapsedTime, toJsonString(result));

        return ApiResponse.success(result);
    }

    /**
     * Submit user feedback for AI-generated test cases
     *
     * POST /api/v1/ai/testcase/feedback
     */
    @PostMapping("/testcase/feedback")
    @Operation(summary = "Submit feedback for AI-generated test cases",
               description = "Collects user feedback to improve AI model through reinforcement learning")
    public ApiResponse<Map<String, Object>> submitFeedback(
            @RequestBody Map<String, Object> request) {

        long startTime = System.currentTimeMillis();
        log.info("[REQUEST] POST /api/v1/ai/testcase/feedback\n{}", toJsonString(request));

        Map<String, Object> result = aiServiceClient.submitFeedback(request);

        long elapsedTime = System.currentTimeMillis() - startTime;
        log.info("[RESPONSE] POST /api/v1/ai/testcase/feedback - Time: {}ms\n{}",
                 elapsedTime, toJsonString(result));

        return ApiResponse.success(result);
    }

    /**
     * Get AI test strategy recommendation
     *
     * POST /api/v1/ai/recommendation/strategy
     *
     * Note: This endpoint will be enhanced to support environment_id and version_id
     */
    @PostMapping("/recommendation/strategy")
    @Operation(summary = "Get AI test strategy recommendation",
               description = "Analyzes code changes, historical data, and business context to recommend optimal test strategy")
    public ApiResponse<Map<String, Object>> getRecommendation(
            @Valid @RequestBody Map<String, Object> request) {

        long startTime = System.currentTimeMillis();
        log.info("[REQUEST] POST /api/v1/ai/recommendation/strategy\n{}", toJsonString(request));

        Map<String, Object> result = aiServiceClient.getRecommendation(request);

        long elapsedTime = System.currentTimeMillis() - startTime;
        log.info("[RESPONSE] POST /api/v1/ai/recommendation/strategy - Time: {}ms\n{}",
                 elapsedTime, toJsonString(result));

        return ApiResponse.success(result);
    }

    /**
     * Explain AI recommendation
     *
     * POST /api/v1/ai/recommendation/strategy/explain
     */
    @PostMapping("/recommendation/strategy/explain")
    @Operation(summary = "Explain AI recommendation",
               description = "Provides detailed explanation of why AI recommended specific test strategy")
    public ApiResponse<Map<String, Object>> explainRecommendation(
            @Valid @RequestBody Map<String, Object> request) {

        long startTime = System.currentTimeMillis();
        log.info("[REQUEST] POST /api/v1/ai/recommendation/strategy/explain\n{}", toJsonString(request));

        Map<String, Object> result = aiServiceClient.explainRecommendation(request);

        long elapsedTime = System.currentTimeMillis() - startTime;
        log.info("[RESPONSE] POST /api/v1/ai/recommendation/strategy/explain - Time: {}ms\n{}",
                 elapsedTime, toJsonString(result));

        return ApiResponse.success(result);
    }

    /**
     * Get test case generation history
     *
     * GET /api/v1/ai/testcase/history
     */
    @GetMapping("/testcase/history")
    @Operation(summary = "Get test case generation history",
               description = "Retrieves historical test case generation records from vector database")
    public ApiResponse<Map<String, Object>> getGenerationHistory(
            @Parameter(description = "Number of records to return", example = "50")
            @RequestParam(required = false, defaultValue = "50") Integer limit,
            @Parameter(description = "Number of records to skip", example = "0")
            @RequestParam(required = false, defaultValue = "0") Integer offset,
            @Parameter(description = "Filter by module name")
            @RequestParam(required = false) String module) {

        long startTime = System.currentTimeMillis();
        log.info("[REQUEST] GET /api/v1/ai/testcase/history?limit={}&offset={}&module={}",
                 limit, offset, module);

        Map<String, Object> result = aiServiceClient.getGenerationHistory(limit, offset, module);

        long elapsedTime = System.currentTimeMillis() - startTime;
        log.info("[RESPONSE] GET /api/v1/ai/testcase/history - Time: {}ms\n{}",
                 elapsedTime, toJsonString(result));

        return ApiResponse.success(result);
    }

    /**
     * Health check for AI Service integration
     *
     * GET /api/v1/ai/health
     */
    @GetMapping("/health")
    @Operation(summary = "Check AI Service health",
               description = "Verifies connection to AI-Service and its availability")
    public ApiResponse<Map<String, Object>> checkHealth() {

        long startTime = System.currentTimeMillis();
        log.info("[REQUEST] GET /api/v1/ai/health");

        Map<String, Object> health = aiServiceClient.checkHealth();

        long elapsedTime = System.currentTimeMillis() - startTime;
        log.info("[RESPONSE] GET /api/v1/ai/health - Time: {}ms\n{}",
                 elapsedTime, toJsonString(health));

        return ApiResponse.success(health);
    }
}
