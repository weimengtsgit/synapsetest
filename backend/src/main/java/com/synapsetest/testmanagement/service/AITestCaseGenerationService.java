package com.synapsetest.testmanagement.service;

import com.synapsetest.testmanagement.dto.AITestCaseGenerationRequest;
import com.synapsetest.testmanagement.dto.response.TestCaseResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * AI Test Case Generation Service
 * Implements AI-powered test case generation from requirements
 *
 * Task: T049 [US2] Implement AI测试用例生成服务
 *
 * This service uses AI/ML models to generate test cases from:
 * - Natural language requirements
 * - User stories
 * - API documentation
 * - UI screenshots
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Profile("mongodb")
public class AITestCaseGenerationService {

    private final AIModelService aiModelService;

    /**
     * Generate test cases from natural language input
     *
     * Algorithm:
     * 1. Parse and analyze input text
     * 2. Extract key scenarios and edge cases
     * 3. Generate test steps using LLM
     * 4. Create expected results
     * 5. Assign priority and categorize
     */
    public List<TestCaseResponse> generateTestCases(AITestCaseGenerationRequest request) {
        log.info("Generating test cases from input: {}", request.getInput().substring(0, Math.min(50, request.getInput().length())));

        // For MVP, use rule-based generation with templates
        // In production, this would use fine-tuned LLM models (Qwen, Llama)

        List<TestCaseResponse> generatedCases = new ArrayList<>();

        // Analyze input and determine test scenarios
        List<String> scenarios = analyzeInputScenarios(request.getInput());

        // Generate test cases for each scenario
        for (int i = 0; i < scenarios.size(); i++) {
            String scenario = scenarios.get(i);
            TestCaseResponse testCase = generateTestCaseForScenario(scenario, request, i + 1);
            generatedCases.add(testCase);
        }

        log.info("Generated {} test cases", generatedCases.size());

        return generatedCases;
    }

    /**
     * Analyze input and extract test scenarios
     */
    private List<String> analyzeInputScenarios(String input) {
        // For MVP: Simple keyword-based scenario extraction
        // In production: Use NLP and semantic analysis

        List<String> scenarios = new ArrayList<>();

        // Extract main scenarios based on keywords
        if (input.toLowerCase().contains("login") || input.toLowerCase().contains("登录")) {
            scenarios.add("User login with valid credentials");
            scenarios.add("User login with invalid credentials");
            scenarios.add("User login with empty fields");
        }

        if (input.toLowerCase().contains("register") || input.toLowerCase().contains("注册")) {
            scenarios.add("User registration with valid data");
            scenarios.add("User registration with duplicate email");
            scenarios.add("User registration with invalid email format");
        }

        if (input.toLowerCase().contains("search") || input.toLowerCase().contains("搜索")) {
            scenarios.add("Search with valid query");
            scenarios.add("Search with empty query");
            scenarios.add("Search with special characters");
        }

        // Default scenarios if no keywords matched
        if (scenarios.isEmpty()) {
            scenarios.add("Happy path scenario");
            scenarios.add("Error handling scenario");
            scenarios.add("Edge case scenario");
        }

        return scenarios;
    }

    /**
     * Generate a single test case for a scenario
     */
    private TestCaseResponse generateTestCaseForScenario(String scenario, AITestCaseGenerationRequest request, int index) {
        TestCaseResponse testCase = new TestCaseResponse();

        // Generate title
        testCase.setTitle(String.format("TC%03d: %s", index, scenario));

        // Generate description
        testCase.setDescription(String.format("This test case verifies: %s", scenario));

        // Generate test steps
        testCase.setSteps(generateTestSteps(scenario));

        // Generate expected results
        testCase.setExpectedResults(generateExpectedResults(scenario));

        // Assign priority based on scenario type
        testCase.setPriority(determinePriority(scenario));

        // Set type
        testCase.setType(request.getTestType() != null ? request.getTestType() : "FUNCTIONAL");

        // Set status
        testCase.setStatus("DRAFT");

        // Set tags
        testCase.setTags(request.getTags() != null ? request.getTags() : Arrays.asList("ai-generated"));

        // Set related requirement
        testCase.setRelatedRequirement(request.getRelatedRequirement());

        return testCase;
    }

    /**
     * Generate test steps for a scenario
     */
    private List<String> generateTestSteps(String scenario) {
        List<String> steps = new ArrayList<>();

        if (scenario.toLowerCase().contains("login")) {
            steps.add("Navigate to login page");
            steps.add("Enter username in username field");
            steps.add("Enter password in password field");
            steps.add("Click login button");
            steps.add("Verify login result");
        } else if (scenario.toLowerCase().contains("register")) {
            steps.add("Navigate to registration page");
            steps.add("Fill in all required fields");
            steps.add("Accept terms and conditions");
            steps.add("Click register button");
            steps.add("Verify registration confirmation");
        } else if (scenario.toLowerCase().contains("search")) {
            steps.add("Navigate to search page");
            steps.add("Enter search query in search box");
            steps.add("Click search button");
            steps.add("Verify search results");
        } else {
            steps.add("Precondition: System is ready");
            steps.add("Execute main action");
            steps.add("Verify expected outcome");
            steps.add("Cleanup and reset");
        }

        return steps;
    }

    /**
     * Generate expected results for a scenario
     */
    private String generateExpectedResults(String scenario) {
        if (scenario.toLowerCase().contains("valid") || scenario.toLowerCase().contains("success")) {
            return "Operation should complete successfully. User should see success message and be redirected appropriately.";
        } else if (scenario.toLowerCase().contains("invalid") || scenario.toLowerCase().contains("error")) {
            return "Operation should fail gracefully. User should see appropriate error message. System should remain in stable state.";
        } else if (scenario.toLowerCase().contains("empty")) {
            return "System should display validation error. User should be prompted to provide required information.";
        } else {
            return "System should behave according to specification. All expected outcomes should be achieved.";
        }
    }

    /**
     * Determine priority based on scenario characteristics
     */
    private Integer determinePriority(String scenario) {
        String lowerScenario = scenario.toLowerCase();

        if (lowerScenario.contains("critical") || lowerScenario.contains("security")) {
            return 10;
        } else if (lowerScenario.contains("valid") || lowerScenario.contains("happy")) {
            return 8;
        } else if (lowerScenario.contains("error") || lowerScenario.contains("invalid")) {
            return 6;
        } else if (lowerScenario.contains("edge")) {
            return 4;
        } else {
            return 5;
        }
    }

    /**
     * Calculate generation confidence score
     */
    public Double calculateConfidenceScore(AITestCaseGenerationRequest request) {
        // For MVP: Simple heuristics
        // In production: Use ML model confidence scores

        double baseScore = 0.70;

        // More detailed input = higher confidence
        if (request.getInput().length() > 100) {
            baseScore += 0.10;
        }

        // Specified type = higher confidence
        if (request.getTestType() != null && !request.getTestType().isEmpty()) {
            baseScore += 0.10;
        }

        // Related requirement = higher confidence
        if (request.getRelatedRequirement() != null && !request.getRelatedRequirement().isEmpty()) {
            baseScore += 0.10;
        }

        return Math.min(baseScore, 1.0);
    }
}
