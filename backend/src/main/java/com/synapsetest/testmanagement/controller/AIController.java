package com.synapsetest.testmanagement.controller;

import com.synapsetest.testmanagement.client.AIServiceClient;
import com.synapsetest.testmanagement.constants.ApiVersion;
import com.synapsetest.testmanagement.dto.AITestCaseGenerationRequest;
import com.synapsetest.testmanagement.dto.ApiResponse;
import com.synapsetest.testmanagement.dto.ai.*;
import com.synapsetest.testmanagement.dto.response.TestCaseResponse;
import com.synapsetest.testmanagement.service.AITestCaseGenerationService;
import io.swagger.v3.oas.annotations.Operation;
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
    private final AITestCaseGenerationService aiTestCaseGenerationService;

    /**
     * AI generate test cases from requirement
     *
     * POST /api/v1/ai/testcase/generate
     */
    @PostMapping("/testcase/generate")
    @Operation(summary = "Generate test cases from requirement using AI",
               description = "Uses LLM to intelligently generate test cases from natural language requirements")
    public ApiResponse<List<TestCaseResponse>> generateTestCases(
            @Valid @RequestBody AITestCaseGenerationRequest request) {

        log.info("Received AI test case generation request");

        List<TestCaseResponse> result = aiTestCaseGenerationService.generateTestCases(request);

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
            @Valid @RequestBody List<AITestCaseGenerationRequest> requests) {

        log.info("Received batch generation request for {} requirements", requests.size());

        Map<String, Object> result = aiServiceClient.batchGenerate(requests);

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
    public ApiResponse<DeduplicationResponse> deduplicateTestCases(
            @Valid @RequestBody DeduplicationRequest request) {

        log.info("Received deduplication request for {} test cases", request.getTestcases().size());

        DeduplicationResponse result = aiServiceClient.deduplicateTestCases(request);

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
    public ApiResponse<PrioritizationResponse> prioritizeTestCases(
            @Valid @RequestBody PrioritizationRequest request) {

        log.info("Received prioritization request for {} test cases", request.getTestcases().size());

        PrioritizationResponse result = aiServiceClient.prioritizeTestCases(request);

        return ApiResponse.success(result);
    }

    /**
     * AI analyze test case quality
     *
     * POST /api/v1/ai/testcase/analyze/quality
     */
    @PostMapping("/testcase/analyze/quality")
    @Operation(summary = "Analyze test case quality using AI",
               description = "Analyzes test cases and provides quality score and improvement suggestions")
    public ApiResponse<Map<String, Object>> analyzeQuality(
            @Valid @RequestBody Map<String, Object> request) {

        List<Map<String, Object>> testcases = (List<Map<String, Object>>) request.get("testcases");
        log.info("Received quality analysis request for {} test cases", testcases.size());

        Map<String, Object> result = aiServiceClient.analyzeQuality(testcases);

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

        log.info("Received strategy recommendation request");

        // TODO: Forward to AI-Service recommendation endpoint
        // For now, return placeholder response

        Map<String, Object> recommendation = Map.of(
            "test_scope", "CORE",
            "environment", "STAGING",
            "priority", 5,
            "estimated_duration", 60,
            "risk_level", "MEDIUM",
            "reasoning", List.of(
                "Code changes detected in critical modules",
                "Recent test pass rate is acceptable",
                "Recommended core regression testing"
            )
        );

        return ApiResponse.success(recommendation);
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

        log.info("Received recommendation explanation request");

        // TODO: Implement explanation logic

        Map<String, Object> explanation = Map.of(
            "factors", List.of(
                Map.of("name", "Code Change Impact", "weight", 0.4, "score", 7),
                Map.of("name", "Historical Test Pass Rate", "weight", 0.3, "score", 8),
                Map.of("name", "Business Priority", "weight", 0.3, "score", 6)
            ),
            "reasoning", "Based on the factors above, AI recommends CORE regression testing with MEDIUM priority",
            "confidence", 0.85
        );

        return ApiResponse.success(explanation);
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

        log.info("Checking AI Service health");

        // TODO: Implement actual health check by calling AI-Service

        Map<String, Object> health = Map.of(
            "status", "UP",
            "ai_service_url", "http://localhost:8000",
            "message", "AI Service is healthy"
        );

        return ApiResponse.success(health);
    }
}
