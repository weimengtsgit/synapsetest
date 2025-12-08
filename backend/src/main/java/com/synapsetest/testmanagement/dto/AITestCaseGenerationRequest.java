package com.synapsetest.testmanagement.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * DTO for AI test case generation request
 */
@Data
public class AITestCaseGenerationRequest {

    @NotBlank(message = "Input is required")
    private String input; // Requirement document or natural language description

    private String format; // Output format preference

    private String testType; // FUNCTIONAL, PERFORMANCE, SECURITY

    private List<String> tags; // Optional tags for categorization

    private String relatedRequirement; // Link to requirement ID
}
