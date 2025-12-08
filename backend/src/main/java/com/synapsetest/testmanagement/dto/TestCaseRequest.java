package com.synapsetest.testmanagement.dto;

import lombok.Data;

import javax.validation.constraints.*;
import java.util.List;

/**
 * DTO for creating a test case
 */
@Data
public class TestCaseRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    private String description;

    @NotEmpty(message = "Steps cannot be empty")
    private List<String> steps;

    private String expectedResults;

    @Min(value = 0, message = "Priority must be at least 0")
    @Max(value = 10, message = "Priority must not exceed 10")
    private Integer priority;

    @NotBlank(message = "Type is required")
    private String type;

    private List<String> tags;

    private String relatedRequirement;
}
