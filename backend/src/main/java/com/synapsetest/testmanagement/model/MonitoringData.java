package com.synapsetest.testmanagement.model;

import com.synapsetest.testmanagement.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * MonitoringData Model
 * Represents real-time monitoring metrics
 * MyBatis POJO (removed MongoDB annotations)
 *
 * User Story 3: 测试结果可视化分析
 * Task: T062 [P] [US3] Create MonitoringData model
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MonitoringData extends BaseEntity {

    @NotBlank(message = "Task ID is required")
    private String taskId; // Reference to TestTask

    @NotBlank(message = "Status is required")
    private String status; // PENDING, RUNNING, COMPLETED, FAILED

    @Min(value = 0, message = "Progress must be at least 0")
    @Max(value = 100, message = "Progress must not exceed 100")
    private Integer progress; // 0-100

    @Min(value = 0, message = "Executed cases must be at least 0")
    private Integer executedCases;

    @Min(value = 0, message = "Total cases must be at least 0")
    private Integer totalCases;

    @Min(value = 0, message = "Passed cases must be at least 0")
    private Integer passedCases;

    @Min(value = 0, message = "Failed cases must be at least 0")
    private Integer failedCases;

    @Min(value = 0, message = "Skipped cases must be at least 0")
    private Integer skippedCases;

    private LocalDateTime startTime;

    private LocalDateTime estimatedEndTime;

    private LocalDateTime actualEndTime;

    private Map<String, Object> resourceUsage; // CPU, Memory, etc.

    private Map<String, Object> performanceMetrics; // Response time, Throughput, etc.

    private LocalDateTime timestamp;

    private String environment;

    private String version;

    /**
     * Calculate pass rate
     */
    public Double getPassRate() {
        if (executedCases == null || executedCases == 0) {
            return 0.0;
        }
        return (double) (passedCases != null ? passedCases : 0) / executedCases * 100;
    }

    /**
     * Calculate remaining time in minutes
     */
    public Long getRemainingMinutes() {
        if (estimatedEndTime == null || actualEndTime != null) {
            return 0L;
        }
        LocalDateTime now = LocalDateTime.now();
        return java.time.Duration.between(now, estimatedEndTime).toMinutes();
    }

    /**
     * Check if task is completed
     */
    public boolean isCompleted() {
        return "COMPLETED".equals(status) || "FAILED".equals(status);
    }

    public enum Status {
        PENDING, RUNNING, COMPLETED, FAILED, CANCELLED
    }
}
