package com.synapsetest.testmanagement.controller;

import com.synapsetest.testmanagement.constants.ApiVersion;
import com.synapsetest.testmanagement.dto.*;
import com.synapsetest.testmanagement.dto.request.GenerateTestCaseRequest;
import com.synapsetest.testmanagement.dto.response.TestCaseResponse;
import com.synapsetest.testmanagement.service.AITestCaseGenerationService;
import com.synapsetest.testmanagement.service.AITestCaseOptimizationService;
import com.synapsetest.testmanagement.service.TestCaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * TestCase Controller (MyBatis version)
 * REST API endpoints for test case management
 * AI features require mongodb profile to be active
 *
 * Task: T050 [US2] Implement TestCaseController
 */
@Tag(name = "测试用例管理", description = "测试用例的管理及AI生成功能")
@RestController
@RequestMapping(ApiVersion.V1 + "/test-cases")
public class TestCaseController {

    private final TestCaseService testCaseService;
    
    @Autowired(required = false)
    private AITestCaseGenerationService aiGenerationService;
    
    @Autowired(required = false)
    private AITestCaseOptimizationService aiOptimizationService;

    public TestCaseController(TestCaseService testCaseService) {
        this.testCaseService = testCaseService;
    }

    /**
     * Create a new test case
     * POST /api/v1/test-cases
     *
     * Note: AI generation has been moved to /api/v1/ai/testcase/generate
     */
    @Operation(
            summary = "创建测试用例",
            description = "手动创建一个新的测试用例"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "创建成功"
    )
    @PostMapping
    public ResponseEntity<ApiResponse<TestCaseResponse>> createTestCase(
            @Parameter(description = "测试用例创建请求", required = true)
            @Valid @RequestBody TestCaseRequest request,
            @Parameter(description = "用户名", required = false, example = "zhangsan")
            @RequestHeader(value = "X-User-Name", defaultValue = "system") String username) {

        TestCaseResponse response = testCaseService.createTestCase(request, username);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Test case created successfully", response));
    }

    /**
     * Batch save test cases
     * POST /api/v1/test-cases/batch
     */
    @Operation(
            summary = "批量保存测试用例",
            description = "批量保存多个测试用例"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "批量创建成功"
    )
    @PostMapping("/batch")
    public ResponseEntity<Map<String, Object>> batchSaveTestCases(
            @Parameter(description = "批量测试用例请求", required = true)
            @RequestBody Map<String, List<Map<String, Object>>> request,
            @Parameter(description = "用户ID", required = false, example = "zhangsan")
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String userId) {

        List<Map<String, Object>> testCases = request.get("test_cases");
        if (testCases == null || testCases.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "test_cases cannot be empty"));
        }

        List<String> savedIds = testCaseService.batchSaveTestCases(testCases, userId);

        Map<String, Object> response = Map.of(
                "saved_count", savedIds.size(),
                "saved_ids", savedIds
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get test case by ID
     * GET /api/v1/test-cases/{id}
     */
    @Operation(summary = "获取测试用例详情", description = "根据ID获取测试用例的详细信息")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TestCaseResponse>> getTestCase(
            @Parameter(description = "测试用例ID", required = true, example = "tc-123456")
            @PathVariable String id) {
        TestCaseResponse response = testCaseService.getTestCaseById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get all test cases or filter by parameters
     * GET /api/v1/test-cases
     */
    @Operation(summary = "获取测试用例列表", description = "获取系统中所有的测试用例列表或按条件筛选")
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllTestCases(
            @Parameter(description = "是否AI生成", required = false)
            @RequestParam(required = false) Boolean ai_generated,
            @Parameter(description = "模块名称", required = false)
            @RequestParam(required = false) String module,
            @Parameter(description = "最小置信度", required = false)
            @RequestParam(required = false) Double min_confidence) {

        List<TestCaseResponse> testCases;

        if (ai_generated != null || module != null || min_confidence != null) {
            // Apply filters
            testCases = testCaseService.getTestCasesWithFilters(ai_generated, module, min_confidence);
        } else {
            testCases = testCaseService.getAllTestCases();
        }

        Map<String, Object> data = Map.of(
                "content", testCases,
                "total", testCases.size()
        );

        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * Get all distinct modules
     * GET /api/v1/test-cases/modules
     */
    @Operation(summary = "获取所有模块列表", description = "获取系统中所有不重复的模块名称列表")
    @GetMapping("/modules")
    public ResponseEntity<ApiResponse<List<String>>> getAllModules() {
        List<String> modules = testCaseService.getAllModules();
        return ResponseEntity.ok(ApiResponse.success(modules));
    }

    /**
     * Get test cases by status
     * GET /api/v1/test-cases?status=DRAFT
     */
    @Operation(summary = "按状态查询测试用例", description = "根据状态筛选测试用例")
    @GetMapping(params = "status")
    public ResponseEntity<ApiResponse<List<TestCaseResponse>>> getTestCasesByStatus(
            @Parameter(description = "状态", required = true, example = "DRAFT")
            @RequestParam String status) {
        List<TestCaseResponse> response = testCaseService.getTestCasesByStatus(status);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get test cases by type
     * GET /api/v1/test-cases?type=FUNCTIONAL
     */
    @Operation(summary = "按类型查询测试用例", description = "根据类型筛选测试用例")
    @GetMapping(params = "type")
    public ResponseEntity<ApiResponse<List<TestCaseResponse>>> getTestCasesByType(
            @Parameter(description = "类型", required = true, example = "FUNCTIONAL")
            @RequestParam String type) {
        List<TestCaseResponse> response = testCaseService.getTestCasesByType(type);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Update test case
     * PUT /api/v1/test-cases/{id}
     */
    @Operation(summary = "更新测试用例", description = "更新指定ID的测试用例信息")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TestCaseResponse>> updateTestCase(
            @Parameter(description = "测试用例ID", required = true, example = "tc-123456")
            @PathVariable String id,
            @Parameter(description = "更新请求", required = true)
            @Valid @RequestBody TestCaseRequest request) {

        TestCaseResponse response = testCaseService.updateTestCase(id, request);
        return ResponseEntity.ok(ApiResponse.success("Test case updated successfully", response));
    }

    /**
     * Approve test case
     * POST /api/v1/test-cases/{id}/approve
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<TestCaseResponse>> approveTestCase(@PathVariable String id) {
        TestCaseResponse response = testCaseService.approveTestCase(id);
        return ResponseEntity.ok(ApiResponse.success("Test case approved", response));
    }

    /**
     * Delete test case
     * DELETE /api/v1/test-cases/{id}
     *
     * Note: AI deduplication has been moved to /api/v1/ai/testcase/optimize/deduplicate
     * Note: AI quality analysis has been moved to /api/v1/ai/testcase/analyze/quality
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTestCase(@PathVariable String id) {
        testCaseService.deleteTestCase(id);
        return ResponseEntity.ok(ApiResponse.success("Test case deleted successfully", null));
    }
}
