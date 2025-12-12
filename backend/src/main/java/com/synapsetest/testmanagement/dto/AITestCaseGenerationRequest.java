package com.synapsetest.testmanagement.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * DTO for AI test case generation request
 */
@Data
public class AITestCaseGenerationRequest {

    /**
     * Requirement text (natural language description)
     */
    @NotBlank(message = "Requirement text is required")
    @JsonProperty("requirement_text")
    private String requirementText;

    /**
     * Module name (e.g., "用户认证模块", "支付系统")
     */
    private String module;

    /**
     * Number of test cases to generate (default: 10)
     */
    @JsonProperty("num_cases")
    private Integer numCases = 10;

    /**
     * Whether to include edge cases (default: true)
     */
    @JsonProperty("include_edge_cases")
    private Boolean includeEdgeCases = true;

    /**
     * Optimization options
     */
    private OptimizationOptions optimization;

    // ========== Deprecated fields (for backward compatibility) ==========

    /**
     * @deprecated Use requirementText instead
     */
    @Deprecated
    @JsonProperty("input")
    private String input;

    /**
     * Output format preference
     * @deprecated No longer used
     */
    @Deprecated
    private String format;

    /**
     * Test type: FUNCTIONAL, PERFORMANCE, SECURITY
     * @deprecated No longer used
     */
    @Deprecated
    private String testType;

    /**
     * Optional tags for categorization
     * @deprecated No longer used
     */
    @Deprecated
    private List<String> tags;

    /**
     * Link to requirement ID
     * @deprecated No longer used
     */
    @Deprecated
    private String relatedRequirement;

    /**
     * Get requirement text (supports both old and new field names)
     */
    public String getRequirementText() {
        // Support backward compatibility: if input is set but requirementText is not, use input
        if (requirementText == null && input != null) {
            return input;
        }
        return requirementText;
    }
}
