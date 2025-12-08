package com.synapsetest.testmanagement.controller;

import com.synapsetest.testmanagement.constants.ApiVersion;
import com.synapsetest.testmanagement.dto.ApiResponse;
import com.synapsetest.testmanagement.model.MonitoringData;
import com.synapsetest.testmanagement.service.MonitoringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


/**
 * Monitoring Controller
 * REST API endpoints for real-time monitoring
 *
 * Task: T066 [US3] Implement MonitoringController
 */
@Tag(name = "监控与报告", description = "测试执行监控、实时状态和报告生成")
@RestController
@RequestMapping(ApiVersion.V1 + "/monitoring")
@RequiredArgsConstructor
public class MonitoringController {

    private final MonitoringService monitoringService;

    /**
     * Get monitoring data by task ID
     * GET /api/v1/monitoring/tasks/{taskId}
     */
    @Operation(summary = "获取任务监控数据", description = "根据任务ID获取监控数据")
    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<ApiResponse<MonitoringData>> getMonitoringData(
            @Parameter(description = "任务ID", required = true, example = "task-123456")
            @PathVariable String taskId) {
        MonitoringData data = monitoringService.getMonitoringDataByTaskId(taskId);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * Get recent monitoring data
     * GET /api/v1/monitoring/recent
     */
    @Operation(summary = "获取最近监控数据", description = "获取最近的监控数据列表")
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<MonitoringData>>> getRecentMonitoringData() {
        List<MonitoringData> data = monitoringService.getRecentMonitoringData();
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * Get running tasks
     * GET /api/v1/monitoring/running
     */
    @Operation(summary = "获取运行中的任务", description = "获取所有正在运行的任务监控数据")
    @GetMapping("/running")
    public ResponseEntity<ApiResponse<List<MonitoringData>>> getRunningTasks() {
        List<MonitoringData> data = monitoringService.getRunningTasks();
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * Get dashboard statistics
     * GET /api/v1/monitoring/dashboard/stats
     */
    @Operation(summary = "获取仪表盘统计", description = "获取监控仪表盘的统计信息")
    @GetMapping("/dashboard/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardStatistics() {
        Map<String, Object> stats = monitoringService.getDashboardStatistics();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * Get monitoring data by environment
     * GET /api/v1/monitoring/environment/{environment}
     */
    @Operation(summary = "按环境获取监控数据", description = "根据环境筛选监控数据")
    @GetMapping("/environment/{environment}")
    public ResponseEntity<ApiResponse<List<MonitoringData>>> getMonitoringDataByEnvironment(
            @Parameter(description = "环境名称", required = true, example = "DEV")
            @PathVariable String environment) {
        List<MonitoringData> data = monitoringService.getMonitoringDataByEnvironment(environment);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * Update resource usage
     * POST /api/v1/monitoring/tasks/{taskId}/resource-usage
     */
    @PostMapping("/tasks/{taskId}/resource-usage")
    public ResponseEntity<ApiResponse<MonitoringData>> updateResourceUsage(
            @PathVariable String taskId,
            @RequestBody Map<String, Object> resourceUsage) {
        MonitoringData data = monitoringService.updateResourceUsage(taskId, resourceUsage);
        return ResponseEntity.ok(ApiResponse.success("Resource usage updated", data));
    }

    /**
     * Update performance metrics
     * POST /api/v1/monitoring/tasks/{taskId}/performance-metrics
     */
    @PostMapping("/tasks/{taskId}/performance-metrics")
    public ResponseEntity<ApiResponse<MonitoringData>> updatePerformanceMetrics(
            @PathVariable String taskId,
            @RequestBody Map<String, Object> performanceMetrics) {
        MonitoringData data = monitoringService.updatePerformanceMetrics(taskId, performanceMetrics);
        return ResponseEntity.ok(ApiResponse.success("Performance metrics updated", data));
    }
}
