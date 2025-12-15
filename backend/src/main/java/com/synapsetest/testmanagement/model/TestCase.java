package com.synapsetest.testmanagement.model;

import com.synapsetest.testmanagement.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.*;
import java.util.List;

/**
 * TestCase entity class
 * User Story 2: AI生成测试用例
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TestCase extends BaseEntity {

    @NotBlank(message = "Case number is required")
    @Size(max = 20, message = "Case number must not exceed 20 characters")
    private String caseNumber;

    @NotBlank(message = "Title is required")
    @Size(min = 5, max = 100, message = "Title must be between 5 and 100 characters")
    private String title;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    private List<String> steps;

    @NotBlank(message = "Expected result is required")
    private String expectedResult;

    @Min(value = 1, message = "Priority must be at least 1")
    @Max(value = 10, message = "Priority must not exceed 10")
    private Integer priority;

    private String type; // FUNCTIONAL, PERFORMANCE, SECURITY

    private String status; // DRAFT, APPROVED, DEPRECATED

    private List<String> tags;

    private String relatedRequirement;

    private String createdBy;

    // ========== AI相关字段 (数据库合并新增) ==========

    /**
     * 所属模块
     */
    @Size(max = 100, message = "Module name must not exceed 100 characters")
    private String module;

    /**
     * 前置条件 (JSON数组)
     */
    private List<String> preconditions;

    /**
     * AI质量评分 (0.0-1.0)
     */
    @Min(value = 0, message = "Quality score must be at least 0")
    @Max(value = 1, message = "Quality score must not exceed 1")
    private Float qualityScore;

    /**
     * 是否AI生成
     */
    private Boolean aiGenerated;

    /**
     * AI置信度 (0.0-1.0)
     */
    @Min(value = 0, message = "AI confidence must be at least 0")
    @Max(value = 1, message = "AI confidence must not exceed 1")
    private Float aiConfidence;

    // =================================================

    public enum TestCaseType {
        FUNCTIONAL, PERFORMANCE, SECURITY
    }

    public enum TestCaseStatus {
        DRAFT, APPROVED, DEPRECATED
    }
}
