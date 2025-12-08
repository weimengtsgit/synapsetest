package com.synapsetest.testmanagement.dto.ai;

import lombok.Data;
import java.util.List;

/**
 * Generated Test Case from AI Service
 */
@Data
public class GeneratedTestCase {
    private String name;
    private String description;
    private List<String> steps;
    private String expectedResult;
    private String priority;  // P0, P1, P2, P3
    private String type;      // FUNCTIONAL, PERFORMANCE, SECURITY
    private List<String> tags;
}
