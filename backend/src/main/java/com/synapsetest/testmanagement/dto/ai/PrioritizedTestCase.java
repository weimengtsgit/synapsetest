package com.synapsetest.testmanagement.dto.ai;

import lombok.Data;
import java.util.Map;

/**
 * Prioritized Test Case with score
 */
@Data
public class PrioritizedTestCase {
    private Map<String, Object> testcase;
    private Double finalScore;
    private String reasoning;
}
