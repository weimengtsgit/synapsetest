package com.synapsetest.testmanagement.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Optimization options for test case generation
 */
@Data
public class OptimizationOptions {

    /**
     * Whether to deduplicate generated test cases
     */
    private Boolean deduplicate = false;

    /**
     * Whether to prioritize test cases
     */
    private Boolean prioritize = false;

    /**
     * Minimum priority level (P0, P1, P2, P3)
     */
    @JsonProperty("min_priority")
    private String minPriority;

    /**
     * Maximum number of test cases to return after optimization
     */
    @JsonProperty("max_cases")
    private Integer maxCases;
}
