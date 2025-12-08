package com.synapsetest.testmanagement.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * Request DTO for creating a test task
 * Used in US1: 智能测试任务调度
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTestTaskRequest {
    
    /**
     * Task name
     */
    @NotBlank(message = "Task name cannot be blank")
    private String taskName;
    
    /**
     * Test environment (e.g., DEV, TEST, STAGING, PROD)
     */
    @NotBlank(message = "Environment cannot be blank")
    private String environment;
    
    /**
     * Version identifier
     */
    @NotBlank(message = "Version cannot be blank")
    private String version;
    
    /**
     * List of modules to test
     */
    @NotEmpty(message = "At least one module is required")
    private List<String> modules;
    
    /**
     * Code change information for AI analysis
     * Expected keys:
     * - changed_files_count: Integer
     * - changed_lines_count: Integer
     * - is_hotfix: Boolean
     * - is_critical_module: Boolean
     */
    @NotNull(message = "Code change info cannot be null")
    private Map<String, Object> codeChangeInfo;
}

