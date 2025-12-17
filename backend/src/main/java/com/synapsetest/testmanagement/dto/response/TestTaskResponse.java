package com.synapsetest.testmanagement.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for test task response (MyBatis version - using String for ID)
 */
@Data
public class TestTaskResponse {

    private String id;
    private String name;
    private String taskName;  // Alias for name, used in some API responses
    private String description;
    private String environment;
    private String version;
    private String testScope;
    private String status;
    private Integer priority;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private TestRecommendation recommendation;
    private Map<String, Object> aiRecommendation;  // AI recommendation data
    
    // Task-TestCase association fields
    private List<TestCaseInfo> testCases;     // Associated test cases
    private Integer totalTestCases;            // Total number of test cases

    @Data
    public static class TestRecommendation {
        private String recommendedEnvironment;
        private String recommendedVersion;
        private String recommendedScope;
        private Double confidenceScore;
        private String reasoning;
    }
    
    /**
     * Simplified test case information for task response
     */
    @Data
    public static class TestCaseInfo {
        private String id;
        private String caseNumber;
        private String title;
        private String module;
        private Integer priority;
        private String type;
        private String status;
        private Integer executionOrder;  // Order in which this test case should be executed
    }
}

