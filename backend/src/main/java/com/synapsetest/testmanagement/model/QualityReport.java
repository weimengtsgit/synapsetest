package com.synapsetest.testmanagement.model;

import com.synapsetest.testmanagement.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * QualityReport Model
 * Represents a quality report for test execution results
 * MyBatis POJO (removed MongoDB annotations)
 *
 * User Story 3: 测试结果可视化分析
 * Task: T061 [P] [US3] Create QualityReport model
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class QualityReport extends BaseEntity {

    @NotBlank(message = "Task ID is required")
    private String taskId; // Reference to TestTask

    @NotBlank(message = "Report name is required")
    @Size(max = 200, message = "Name must not exceed 200 characters")
    private String name;

    private String summary;

    // Use standalone TestResult class
    private List<TestResult> testResults;

    private Map<String, Integer> defectStats; // severity -> count

    private Map<String, Object> performanceMetrics;

    // Use standalone RiskAssessment class
    private RiskAssessment riskAssessment;

    private LocalDateTime generatedAt;

    @NotBlank(message = "Status is required")
    private String status; // GENERATING, COMPLETED, ARCHIVED

    public enum Status {
        GENERATING, COMPLETED, ARCHIVED
    }

    public enum RiskLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}
