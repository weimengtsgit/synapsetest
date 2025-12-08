package com.synapsetest.testmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Dashboard Statistics Response DTO
 * Used for monitoring dashboard overview statistics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {
    
    /**
     * Total number of test tasks
     */
    private Integer totalTasks;
    
    /**
     * Number of running tasks
     */
    private Integer runningTasks;
    
    /**
     * Number of completed tasks
     */
    private Integer completedTasks;
    
    /**
     * Average pass rate (0-100)
     */
    private Double averagePassRate;
    
    /**
     * Average execution time in seconds
     */
    private Double averageExecutionTime;
}

