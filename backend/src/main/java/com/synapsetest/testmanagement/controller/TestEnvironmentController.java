package com.synapsetest.testmanagement.controller;

import com.synapsetest.testmanagement.constants.ApiVersion;
import com.synapsetest.testmanagement.model.TestEnvironment;
import com.synapsetest.testmanagement.service.TestEnvironmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * TestEnvironment Controller (MyBatis version)
 * REST API endpoints for test environment management
 *
 * Task: T036 [US1] Implement TestEnvironmentController
 */
@Tag(name = "测试环境管理", description = "测试环境的查询和管理接口")
@RestController
@RequestMapping(ApiVersion.V1 + "/test-environments")
@RequiredArgsConstructor
public class TestEnvironmentController {

    private final TestEnvironmentService environmentService;

    /**
     * Get all environments
     * GET /api/v1/test-environments
     */
    @Operation(summary = "获取所有环境", description = "获取系统中所有的测试环境列表（可通过参数过滤）")
    @GetMapping
    public ResponseEntity<List<TestEnvironment>> getAllEnvironments(
            @Parameter(description = "按状态过滤（可选）", example = "AVAILABLE")
            @RequestParam(required = false) String status) {
        List<TestEnvironment> environments;
        if (status != null && !status.isEmpty()) {
            environments = environmentService.getAvailableEnvironments();
        } else {
            environments = environmentService.getAllEnvironments();
        }
        return ResponseEntity.ok(environments);
    }

    /**
     * Get environment by ID
     * GET /api/v1/test-environments/{id}
     */
    @Operation(summary = "根据ID获取环境", description = "根据环境ID查询具体的测试环境信息")
    @GetMapping("/{id}")
    public ResponseEntity<TestEnvironment> getEnvironment(
            @Parameter(description = "环境ID", required = true, example = "env-123456")
            @PathVariable String id) {
        TestEnvironment environment = environmentService.getEnvironmentById(id);
        return ResponseEntity.ok(environment);
    }

    /**
     * Get environment by name
     * GET /api/v1/test-environments/by-name/{name}
     */
    @Operation(summary = "根据名称获取环境", description = "根据环境名称查询测试环境信息")
    @GetMapping("/by-name/{name}")
    public ResponseEntity<TestEnvironment> getEnvironmentByName(
            @Parameter(description = "环境名称", required = true, example = "DEV")
            @PathVariable String name) {
        TestEnvironment environment = environmentService.getEnvironmentByName(name);
        return ResponseEntity.ok(environment);
    }

    /**
     * Create a new environment
     * POST /api/v1/test-environments
     */
    @Operation(
            summary = "创建测试环境",
            description = "创建一个新的测试环境"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "环境创建成功"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "请求参数错误"
            )
    })
    @PostMapping
    public ResponseEntity<TestEnvironment> createEnvironment(
            @Parameter(description = "测试环境信息", required = true)
            @Valid @RequestBody TestEnvironment environment) {
        TestEnvironment created = environmentService.createEnvironment(environment);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Update an environment
     * PUT /api/v1/test-environments/{id}
     */
    @Operation(
            summary = "更新测试环境",
            description = "更新指定ID的测试环境信息"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "环境更新成功"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "环境不存在"
            )
    })
    @PutMapping("/{id}")
    public ResponseEntity<TestEnvironment> updateEnvironment(
            @Parameter(description = "环境ID", required = true, example = "env-123456")
            @PathVariable String id,
            @Parameter(description = "环境更新信息", required = true)
            @Valid @RequestBody TestEnvironment environment) {
        TestEnvironment updated = environmentService.updateEnvironment(id, environment);
        return ResponseEntity.ok(updated);
    }

    /**
     * Update environment status
     * POST /api/v1/test-environments/{id}/status
     */
    @Operation(
            summary = "更新环境状态",
            description = "更新指定环境的状态（AVAILABLE, MAINTENANCE, UNAVAILABLE）"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "状态更新成功"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "环境不存在"
            )
    })
    @PostMapping("/{id}/status")
    public ResponseEntity<TestEnvironment> updateEnvironmentStatus(
            @Parameter(description = "环境ID", required = true, example = "env-123456")
            @PathVariable String id,
            @Parameter(description = "状态更新请求", required = true)
            @RequestBody java.util.Map<String, String> statusUpdate) {
        String newStatus = statusUpdate.get("status");
        TestEnvironment updated = environmentService.updateEnvironmentStatus(id, newStatus);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete an environment
     * DELETE /api/v1/test-environments/{id}
     */
    @Operation(
            summary = "删除测试环境",
            description = "删除指定ID的测试环境"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "环境删除成功"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "环境不存在"
            )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEnvironment(
            @Parameter(description = "环境ID", required = true, example = "env-123456")
            @PathVariable String id) {
        environmentService.deleteEnvironment(id);
        return ResponseEntity.noContent().build();
    }
}
