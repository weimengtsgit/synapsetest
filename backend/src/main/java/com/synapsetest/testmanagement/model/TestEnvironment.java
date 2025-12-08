package com.synapsetest.testmanagement.model;

import com.synapsetest.testmanagement.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.Map;

/**
 * TestEnvironment Model
 * Represents a test environment configuration
 * MyBatis POJO (removed JPA annotations)
 *
 * User Story 1: 智能测试任务调度
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TestEnvironment extends BaseEntity {

    @NotBlank(message = "Environment name is required")
    @Size(max = 100, message = "Environment name must not exceed 100 characters")
    private String name;

    private String description;

    private String url;

    private Map<String, String> config;

    @NotBlank(message = "Status is required")
    private String status; // AVAILABLE, MAINTENANCE, UNAVAILABLE

    public enum Status {
        AVAILABLE, MAINTENANCE, UNAVAILABLE
    }
}
