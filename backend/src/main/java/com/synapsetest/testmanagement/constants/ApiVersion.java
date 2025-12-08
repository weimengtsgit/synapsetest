package com.synapsetest.testmanagement.constants;

/**
 * API Version Constants
 * Centralized API versioning configuration for RESTful endpoints
 * 
 * Usage:
 * @RequestMapping(ApiVersion.V1 + "/test-cases")
 * 
 * Benefits:
 * - Easy to maintain and update versions
 * - Support multiple API versions simultaneously
 * - Clear separation between system endpoints and versioned APIs
 */
public final class ApiVersion {
    
    /**
     * API Version 1.0
     * Base path: /api/v1
     */
    public static final String V1 = "/api/v1";
    
    /**
     * API Version 2.0 (for future use)
     * Base path: /api/v2
     */
    public static final String V2 = "/api/v2";
    
    // Prevent instantiation
    private ApiVersion() {
        throw new AssertionError("ApiVersion class should not be instantiated");
    }
}

