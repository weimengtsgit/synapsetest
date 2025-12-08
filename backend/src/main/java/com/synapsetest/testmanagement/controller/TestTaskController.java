package com.synapsetest.testmanagement.controller;

import com.synapsetest.testmanagement.constants.ApiVersion;
import com.synapsetest.testmanagement.dto.ApiResponse;
import com.synapsetest.testmanagement.dto.TestTaskRequest;
import com.synapsetest.testmanagement.dto.request.CreateTestTaskRequest;
import com.synapsetest.testmanagement.dto.response.PageResponse;
import com.synapsetest.testmanagement.dto.response.TestTaskResponse;
import com.synapsetest.testmanagement.exception.ValidationException;
import com.synapsetest.testmanagement.service.TestTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * TestTask Controller (MyBatis version)
 * REST API endpoints for test task management
 *
 * Task: T035 [US1] Implement TestTaskController
 */
@Tag(name = "测试任务管理", description = "测试任务的创建、执行、状态管理等功能")
@RestController
@RequestMapping(ApiVersion.V1 + "/test-tasks")
@RequiredArgsConstructor
public class TestTaskController {

    private final TestTaskService testTaskService;

    /**
     * Create a new test task
     * POST /api/v1/test-tasks
     */
    @Operation(
            summary = "创建测试任务",
            description = "创建一个新的测试任务，基于代码变更信息和测试配置生成测试计划"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "测试任务创建成功",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = "{\n" +
                                    "  \"success\": true,\n" +
                                    "  \"message\": \"Test task created successfully\",\n" +
                                    "  \"data\": {\n" +
                                    "    \"taskId\": \"task-123456\",\n" +
                                    "    \"taskName\": \"冒烟测试-订单模块\",\n" +
                                    "    \"status\": \"PENDING\",\n" +
                                    "    \"createdAt\": \"2025-11-18T10:30:00\"\n" +
                                    "  }\n" +
                                    "}")
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "请求参数错误"
            )
    })
    @PostMapping
    public ResponseEntity<TestTaskResponse> createTestTask(
            @Parameter(description = "测试任务创建请求", required = true)
            @Valid @RequestBody CreateTestTaskRequest request,
            @Parameter(description = "用户ID", required = false, example = "zhangsan")
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String userId) {

        TestTaskResponse response = testTaskService.createTestTask(request, userId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * Get test task by ID
     * GET /api/v1/test-tasks/{id}
     */
    @Operation(
            summary = "获取测试任务详情",
            description = "根据任务ID获取测试任务的详细信息"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "获取成功"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "任务不存在"
            )
    })
    @GetMapping("/{id}")
    public ResponseEntity<TestTaskResponse> getTestTask(
            @Parameter(description = "任务ID", required = true, example = "task-123456")
            @PathVariable String id) {
        TestTaskResponse response = testTaskService.getTestTaskById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all test tasks
     * GET /api/v1/test-tasks
     */
    @Operation(
            summary = "获取所有测试任务",
            description = "获取系统中所有的测试任务列表"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "获取成功"
    )
    @GetMapping
    public ResponseEntity<List<TestTaskResponse>> getAllTestTasks() {
        List<TestTaskResponse> response = testTaskService.getAllTestTasks();
        return ResponseEntity.ok(response);
    }

    /**
     * Get test tasks by status with pagination support
     * GET /api/v1/test-tasks?status=PENDING&page=0&size=10
     */
    @Operation(
            summary = "按状态查询测试任务（支持分页）",
            description = "根据任务状态筛选测试任务列表，支持分页参数"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "查询成功"
    )
    @GetMapping(params = "status")
    public ResponseEntity<?> getTestTasksByStatus(
            @Parameter(description = "任务状态", required = true, example = "PENDING")
            @RequestParam String status,
            @Parameter(description = "页码（从0开始）", required = false, example = "0")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "每页大小", required = false, example = "10")
            @RequestParam(required = false) Integer size) {
        
        // If pagination parameters are provided, return paginated response
        if (page != null && size != null) {
            PageResponse<TestTaskResponse> response =
                testTaskService.getTestTasksByStatusWithPagination(status, page, size);
            return ResponseEntity.ok(response);
        }
        
        // Otherwise, return simple list
        List<TestTaskResponse> response = testTaskService.getTestTasksByStatus(status);
        return ResponseEntity.ok(response);
    }

    /**
     * Start a test task
     * POST /api/v1/test-tasks/{id}/start
     */
    @Operation(
            summary = "启动测试任务",
            description = "启动指定ID的测试任务，开始执行测试"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "任务启动成功"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "任务不存在"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "任务状态不允许启动"
            )
    })
    @PostMapping("/{id}/start")
    public ResponseEntity<TestTaskResponse> startTestTask(
            @Parameter(description = "任务ID", required = true, example = "task-123456")
            @PathVariable String id) {
        TestTaskResponse response = testTaskService.startTestTask(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Cancel a test task
     * POST /api/v1/test-tasks/{id}/cancel
     */
    @Operation(
            summary = "取消测试任务",
            description = "取消指定ID的测试任务"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "任务取消成功"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "任务不存在"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "任务状态不允许取消"
            )
    })
    @PostMapping("/{id}/cancel")
    public ResponseEntity<TestTaskResponse> cancelTestTask(
            @Parameter(description = "任务ID", required = true, example = "task-123456")
            @PathVariable String id) {
        TestTaskResponse response = testTaskService.cancelTestTask(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Update test task status
     * POST /api/v1/test-tasks/{id}/status
     */
    @Operation(
            summary = "更新任务状态",
            description = "更新指定ID的测试任务状态"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "状态更新成功"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "任务不存在"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "无效的状态转换"
            )
    })
    @PostMapping("/{id}/status")
    public ResponseEntity<TestTaskResponse> updateTaskStatus(
            @Parameter(description = "任务ID", required = true)
            @PathVariable String id,
            @Parameter(description = "状态更新请求", required = true)
            @RequestBody java.util.Map<String, String> statusUpdate) {
        String newStatus = statusUpdate.get("status");
        if (newStatus == null || newStatus.isEmpty()) {
            throw new ValidationException("Status is required");
        }
        TestTaskResponse response = testTaskService.updateTaskStatus(id, newStatus);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a test task (soft delete)
     * DELETE /api/v1/test-tasks/{id}
     */
    @Operation(
            summary = "删除测试任务",
            description = "删除指定ID的测试任务（软删除，标记为CANCELLED）"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "删除成功"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "任务不存在"
            )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTestTask(
            @Parameter(description = "任务ID", required = true, example = "task-123456")
            @PathVariable String id) {
        testTaskService.deleteTestTask(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get AI recommendation for test task
     * POST /api/v1/test-tasks/ai/recommendation
     */
    @Operation(
            summary = "获取AI测试推荐",
            description = "基于代码变更信息获取AI推荐的测试策略"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "推荐生成成功"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "请求参数无效"
            )
    })
    @PostMapping("/ai/recommendation")
    public ResponseEntity<java.util.Map<String, Object>> getAiRecommendation(
            @Parameter(description = "推荐上下文", required = true)
            @RequestBody java.util.Map<String, Object> context) {
        
        // Build CreateTestTaskRequest from context
        CreateTestTaskRequest request = new CreateTestTaskRequest();
        request.setTaskName("AI Recommendation Request");
        request.setEnvironment((String) context.getOrDefault("environment", "DEV"));
        request.setVersion((String) context.getOrDefault("version", "1.0.0"));
        
        @SuppressWarnings("unchecked")
        List<String> modules = (List<String>) context.get("modules");
        request.setModules(modules != null ? modules : List.of("default"));
        
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> codeChange = (java.util.Map<String, Object>) context.get("code_change");
        if (codeChange == null) {
            codeChange = new java.util.HashMap<>();
            codeChange.put("changed_files_count", 0);
            codeChange.put("changed_lines_count", 0);
        }
        request.setCodeChangeInfo(codeChange);
        
        // Get AI recommendation
        TestTaskResponse.TestRecommendation recommendation = 
            testTaskService.getAiRecommendation(request);
        
        // Convert to response format
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("test_scope", recommendation.getRecommendedScope());
        response.put("environment", recommendation.getRecommendedEnvironment());
        response.put("version", recommendation.getRecommendedVersion());
        response.put("priority", calculatePriority(recommendation.getRecommendedScope()));
        response.put("confidence", recommendation.getConfidenceScore());
        response.put("reasoning", recommendation.getReasoning());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Calculate priority based on test scope
     */
    private int calculatePriority(String testScope) {
        if (testScope == null) return 5;
        
        switch (testScope) {
            case "SMOKE":
                return 8;
            case "CORE":
                return 9;
            case "FULL":
                return 6;
            default:
                return 5;
        }
    }
}
