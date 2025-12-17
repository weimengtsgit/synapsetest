package com.synapsetest.testmanagement.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * TaskTestCase Model
 * Represents the association between a test task and test cases
 * 
 * Table: task_test_cases
 * Purpose: Link test tasks with their associated test cases and execution order
 */
@Data
public class TaskTestCase {
    
    /**
     * Task ID (foreign key to test_tasks)
     */
    private String taskId;
    
    /**
     * Test Case ID (foreign key to test_cases)
     */
    private String testCaseId;
    
    /**
     * Execution order (determines the sequence of test case execution)
     */
    private Integer executionOrder;
    
    /**
     * Creation timestamp
     */
    private LocalDateTime createdAt;
}
