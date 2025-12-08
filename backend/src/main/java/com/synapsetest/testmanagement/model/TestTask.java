package com.synapsetest.testmanagement.model;

import com.synapsetest.testmanagement.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.*;

/**
 * TestTask Model
 * Represents a test task in the system
 * MyBatis POJO (removed JPA annotations)
 *
 * User Story 1: 智能测试任务调度
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TestTask extends BaseEntity {

    @NotBlank(message = "Task name is required")
    @Size(max = 100, message = "Task name must not exceed 100 characters")
    private String name;

    private String description;

    @NotBlank(message = "Environment is required")
    private String environment;

    @NotBlank(message = "Version is required")
    private String version;

    private String testScope;

    @NotBlank(message = "Status is required")
    private String status;

    @Min(value = 0, message = "Priority must be at least 0")
    @Max(value = 10, message = "Priority must not exceed 10")
    private Integer priority = 0;

    @NotBlank(message = "Created by is required")
    private String createdBy;

    public enum Status {
        PENDING, RUNNING, COMPLETED, CANCELLED
    }

    public enum Environment {
        DEV, TEST, STAGING, PROD
    }
}
