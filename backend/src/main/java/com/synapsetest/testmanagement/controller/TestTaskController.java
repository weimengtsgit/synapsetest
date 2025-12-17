package com.synapsetest.testmanagement.controller;

import com.synapsetest.testmanagement.constants.ApiVersion;
import com.synapsetest.testmanagement.dto.ApiResponse;
import com.synapsetest.testmanagement.dto.request.CreateTestTaskRequest;
import com.synapsetest.testmanagement.dto.response.PageResponse;
import com.synapsetest.testmanagement.dto.response.TestTaskResponse;
import com.synapsetest.testmanagement.exception.ValidationException;
import com.synapsetest.testmanagement.model.TestCase;
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
    public ResponseEntity<ApiResponse<TestTaskResponse>> createTestTask(
            @Parameter(description = "测试任务创建请求", required = true)
            @Valid @RequestBody CreateTestTaskRequest request,
            @Parameter(description = "用户ID", required = false, example = "zhangsan")
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String userId) {

        TestTaskResponse response = testTaskService.createTestTask(request, userId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Test task created successfully", response));
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
    public ResponseEntity<ApiResponse<TestTaskResponse>> getTestTask(
            @Parameter(description = "任务ID", required = true, example = "task-123456")
            @PathVariable String id) {
        TestTaskResponse response = testTaskService.getTestTaskById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
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
    public ResponseEntity<ApiResponse<List<TestTaskResponse>>> getAllTestTasks() {
        List<TestTaskResponse> response = testTaskService.getAllTestTasks();
        return ResponseEntity.ok(ApiResponse.success(response));
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
    public ResponseEntity<ApiResponse<?>> getTestTasksByStatus(
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
            return ResponseEntity.ok(ApiResponse.success(response));
        }
        
        // Otherwise, return simple list
        List<TestTaskResponse> response = testTaskService.getTestTasksByStatus(status);
        return ResponseEntity.ok(ApiResponse.success(response));
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
    public ResponseEntity<ApiResponse<TestTaskResponse>> startTestTask(
            @Parameter(description = "任务ID", required = true, example = "task-123456")
            @PathVariable String id) {
        TestTaskResponse response = testTaskService.startTestTask(id);
        return ResponseEntity.ok(ApiResponse.success("Test task started successfully", response));
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
    public ResponseEntity<ApiResponse<TestTaskResponse>> cancelTestTask(
            @Parameter(description = "任务ID", required = true, example = "task-123456")
            @PathVariable String id) {
        TestTaskResponse response = testTaskService.cancelTestTask(id);
        return ResponseEntity.ok(ApiResponse.success("Test task cancelled successfully", response));
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
    public ResponseEntity<ApiResponse<TestTaskResponse>> updateTaskStatus(
            @Parameter(description = "任务ID", required = true)
            @PathVariable String id,
            @Parameter(description = "状态更新请求", required = true)
            @RequestBody java.util.Map<String, String> statusUpdate) {
        String newStatus = statusUpdate.get("status");
        if (newStatus == null || newStatus.isEmpty()) {
            throw new ValidationException("Status is required");
        }
        TestTaskResponse response = testTaskService.updateTaskStatus(id, newStatus);
        return ResponseEntity.ok(ApiResponse.success("Task status updated successfully", response));
    }

    /**
     * Delete a test task (soft delete)
     * DELETE /api/v1/test-tasks/{id}
     *
     * Note: AI recommendation has been moved to /api/v1/ai/recommendation/strategy
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
     * Preview matched test cases (without creating a task)
     * POST /api/v1/test-tasks/preview-test-cases
     */
    @Operation(
            summary = "预览匹配的测试用例",
            description = "根据任务条件预览将要关联的测试用例，不创建任务"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "预览成功"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "请求参数错误"
            )
    })
    @PostMapping("/preview-test-cases")
    public ResponseEntity<ApiResponse<List<TestCase>>> previewTestCases(
            @Parameter(description = "测试任务创建请求", required = true)
            @Valid @RequestBody CreateTestTaskRequest request) {
        List<TestCase> testCases = testTaskService.previewMatchedTestCases(request);
        return ResponseEntity.ok(ApiResponse.success(
                String.format("Matched %d test cases", testCases.size()), 
                testCases));
    }
    
    /**
     * Get test cases associated with a task
     * GET /api/v1/test-tasks/{id}/test-cases
     */
    @Operation(
            summary = "获取任务关联的测试用例",
            description = "获取指定任务ID关联的所有测试用例"
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
    @GetMapping("/{id}/test-cases")
    public ResponseEntity<ApiResponse<List<TestCase>>> getTestCasesByTaskId(
            @Parameter(description = "任务ID", required = true, example = "task-123456")
            @PathVariable String id) {
        List<TestCase> testCases = testTaskService.getTestCasesByTaskId(id);
        return ResponseEntity.ok(ApiResponse.success(testCases));
    }
}
