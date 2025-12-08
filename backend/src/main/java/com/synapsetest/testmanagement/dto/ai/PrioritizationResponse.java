package com.synapsetest.testmanagement.dto.ai;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * Response from AI Service for prioritization
 */
@Data
public class PrioritizationResponse {
    private String requestId;
    private List<PrioritizedTestCase> prioritizedCases;
    private String summary;
}
