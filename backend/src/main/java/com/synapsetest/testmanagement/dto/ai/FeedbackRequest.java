package com.synapsetest.testmanagement.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.validation.constraints.*;
import java.util.List;

/**
 * User feedback request for AI-generated test cases
 *
 * This DTO is used to collect user feedback about AI-generated test cases
 * to help improve the AI model through reinforcement learning
 */
@Data
public class FeedbackRequest {

    /**
     * Generation request ID from testcase_generation_history table
     */
    @NotBlank(message = "Request ID is required")
    @JsonProperty("request_id")
    private String requestId;

    /**
     * User rating (1-5 stars)
     */
    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must not exceed 5")
    private Integer rating;

    /**
     * Optional comments from user
     */
    private String comments;

    /**
     * List of accepted test case IDs
     */
    @JsonProperty("accepted_cases")
    private List<String> acceptedCases;

    /**
     * List of rejected test case IDs
     */
    @JsonProperty("rejected_cases")
    private List<String> rejectedCases;

    /**
     * User ID who submitted the feedback
     */
    @JsonProperty("user_id")
    private String userId;

    /**
     * Feedback type: GENERATION, RECOMMENDATION, QUALITY
     */
    @JsonProperty("feedback_type")
    private String feedbackType = "GENERATION";
}
