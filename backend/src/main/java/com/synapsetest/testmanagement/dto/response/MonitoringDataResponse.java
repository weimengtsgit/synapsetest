package com.synapsetest.testmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Monitoring Data Response DTO
 * Used for real-time monitoring data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonitoringDataResponse {
    
    /**
     * Task ID
     */
    private String taskId;
    
    /**
     * Task name
     */
    private String taskName;
    
    /**
     * Task status
     */
    private String status;
    
    /**
     * Progress percentage (0-100)
     */
    private Double progressPercentage;
    
    /**
     * Executed count
     */
    private Integer executedCount;
    
    /**
     * Total count
     */
    private Integer totalCount;
    
    /**
     * Pass count
     */
    private Integer passCount;
    
    /**
     * Fail count
     */
    private Integer failCount;
    
    /**
     * Timestamp
     */
    private LocalDateTime timestamp;
    
    /**
     * Additional metrics
     */
    private Map<String, Object> metrics;
}

