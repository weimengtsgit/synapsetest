package com.synapsetest.testmanagement.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * Request DTO for AI-powered test case generation
 * Used in US2: AI生成测试用例
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateTestCaseRequest {
    
    /**
     * Requirement text describing the functionality
     */
    @NotBlank(message = "Requirement text cannot be blank")
    private String requirementText;
    
    /**
     * Module name for the test cases
     */
    @NotBlank(message = "Module cannot be blank")
    private String module;
    
    /**
     * Number of test cases to generate
     */
    @NotNull(message = "Number of cases cannot be null")
    @Min(value = 1, message = "Number of cases must be at least 1")
    private Integer numCases;
    
    /**
     * Priority level for generated test cases (e.g., HIGH, CRITICAL, LOW)
     */
    private String priority;
    
    /**
     * Enable deduplication for generated test cases
     */
    private Boolean enableDeduplication;
}

