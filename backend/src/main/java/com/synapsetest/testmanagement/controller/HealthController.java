package com.synapsetest.testmanagement.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.synapsetest.testmanagement.dto.ApiResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * Health Check Controller
 * Provides health status endpoints
 */
@Tag(name = "健康检查", description = "系统健康检查和状态监控")
@RestController
@RequestMapping("")
public class HealthController {

    @Operation(summary = "健康检查", description = "检查系统健康状态")
    @GetMapping("/health")
    public ApiResponse<Map<String, String>> health() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "UP");
        status.put("service", "test-management-backend");
        return ApiResponse.success(status);
    }
}
