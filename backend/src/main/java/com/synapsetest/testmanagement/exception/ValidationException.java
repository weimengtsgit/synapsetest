package com.synapsetest.testmanagement.exception;

/**
 * Custom exception for validation errors
 */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }
}
