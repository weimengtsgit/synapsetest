package com.synapsetest.testmanagement.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Response for feedback submission
 */
@Data
public class FeedbackResponse {

    /**
     * Success status
     */
    private Boolean success;

    /**
     * Result message
     */
    private String message;

    /**
     * Feedback ID (if saved to database)
     */
    @JsonProperty("feedback_id")
    private String feedbackId;

    /**
     * Request ID that this feedback is for
     */
    @JsonProperty("request_id")
    private String requestId;

    /**
     * Storage type: vector_db, none, error
     */
    private String storage;

    /**
     * Vector database type name (if stored)
     */
    @JsonProperty("vector_db_type")
    private String vectorDbType;
}
