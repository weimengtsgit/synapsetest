package com.synapsetest.testmanagement.dto;

import lombok.Data;

import javax.validation.constraints.*;

/**
 * DTO for creating a test task
 */
@Data
public class TestTaskRequest {

    @NotBlank(message = "Task name is required")
    @Size(max = 100, message = "Task name must not exceed 100 characters")
    private String name;

    private String description;

    @NotBlank(message = "Environment is required")
    private String environment;

    @NotBlank(message = "Version is required")
    private String version;

    private String testScope;

    @Min(value = 0, message = "Priority must be at least 0")
    @Max(value = 10, message = "Priority must not exceed 10")
    private Integer priority;
}
