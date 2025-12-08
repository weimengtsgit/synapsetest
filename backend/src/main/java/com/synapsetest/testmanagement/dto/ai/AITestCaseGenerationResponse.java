package com.synapsetest.testmanagement.dto.ai;

import lombok.Data;
import java.util.List;

/**
 * Response from AI Service for test case generation
 */
@Data
public class AITestCaseGenerationResponse {
    private String requestId;
    private List<GeneratedTestCase> generatedCases;
    private Double generationTime;
    private Double confidenceScore;
    private String metadata;
    private boolean success = true;
    private String message;
}
