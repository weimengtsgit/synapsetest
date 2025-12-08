package com.synapsetest.testmanagement.dto.ai;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * Request for AI test case deduplication
 */
@Data
public class DeduplicationRequest {
    private List<Map<String, Object>> testcases;
    private Double threshold = 0.85;  // Similarity threshold
}
