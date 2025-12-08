package com.synapsetest.testmanagement.exception;

/**
 * Exception thrown when AI Service communication fails
 */
public class AIServiceException extends RuntimeException {

    public AIServiceException(String message) {
        super(message);
    }

    public AIServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
