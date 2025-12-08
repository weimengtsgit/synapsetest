package com.synapsetest.testmanagement.dto.ai;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * Request for AI test case prioritization
 */
@Data
public class PrioritizationRequest {
    private List<Map<String, Object>> testcases;
    private Map<String, Double> customWeights;
}
