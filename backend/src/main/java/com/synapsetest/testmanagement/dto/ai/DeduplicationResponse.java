package com.synapsetest.testmanagement.dto.ai;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * Response from AI Service for deduplication
 */
@Data
public class DeduplicationResponse {
    private String requestId;
    private List<Map<String, Object>> optimizedCases;
    private List<List<Integer>> duplicateGroups;
    private Double reductionRate;
    private String summary;
}
