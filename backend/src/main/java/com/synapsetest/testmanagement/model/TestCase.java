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

    public enum TestCaseType {
        FUNCTIONAL, PERFORMANCE, SECURITY
    }

    public enum TestCaseStatus {
        DRAFT, APPROVED, DEPRECATED
    }
}
