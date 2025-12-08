package com.synapsetest.testmanagement.model;

import com.synapsetest.testmanagement.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Map;

/**
 * TestVersion Model
 * Represents a test version configuration
 * MyBatis POJO (removed JPA annotations)
 *
 * User Story 1: 智能测试任务调度
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TestVersion extends BaseEntity {

    @NotBlank(message = "Version name is required")
    @Size(max = 100, message = "Version name must not exceed 100 characters")
    private String name;

    private String description;

    @NotBlank(message = "Product version is required")
    private String productVersion;

    private LocalDate releaseDate;

    private Map<String, String> config;
}
