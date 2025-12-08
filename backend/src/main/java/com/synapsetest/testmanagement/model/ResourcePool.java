package com.synapsetest.testmanagement.model;

import com.synapsetest.testmanagement.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.*;
import java.util.Map;

/**
 * ResourcePool Model
 * Represents a pool of test resources
 * MyBatis POJO (removed JPA annotations)
 *
 * User Story 1: 智能测试任务调度
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ResourcePool extends BaseEntity {

    @NotBlank(message = "Resource pool name is required")
    @Size(max = 100, message = "Resource pool name must not exceed 100 characters")
    private String name;

    private String description;

    @NotBlank(message = "Type is required")
    private String type; // VM, CONTAINER, DEVICE

    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer capacity;

    @Min(value = 0, message = "Allocated must be at least 0")
    private Integer allocated = 0;

    private String location;

    private Map<String, String> config;

    @NotBlank(message = "Status is required")
    private String status; // AVAILABLE, MAINTENANCE, UNAVAILABLE

    public enum ResourceType {
        VM, CONTAINER, DEVICE
    }

    public enum ResourceStatus {
        AVAILABLE, MAINTENANCE, UNAVAILABLE
    }

    /**
     * Check if resources are available
     */
    public boolean hasAvailableResources() {
        return allocated < capacity;
    }

    /**
     * Get available resources count
     */
    public int getAvailableResources() {
        return capacity - allocated;
    }
}
