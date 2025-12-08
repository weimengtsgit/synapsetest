package com.synapsetest.testmanagement.service;

import com.synapsetest.testmanagement.dto.TestCaseRequest;
import com.synapsetest.testmanagement.dto.response.TestCaseResponse;
import com.synapsetest.testmanagement.exception.ResourceNotFoundException;
import com.synapsetest.testmanagement.mapper.TestCaseMapper;
import com.synapsetest.testmanagement.model.TestCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * TestCase Service
 * Business logic for test case management (MyBatis version)
 *
 * Task: T048 [US2] Implement TestCaseService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TestCaseService {

    private final TestCaseMapper testCaseMapper;

    /**
     * Create a new test case
     */
    public TestCaseResponse createTestCase(TestCaseRequest request, String username) {
        log.info("Creating test case: {} by user: {}", request.getTitle(), username);

        TestCase testCase = new TestCase();
        testCase.setId(UUID.randomUUID().toString());
        testCase.setTitle(request.getTitle());
        testCase.setDescription(request.getDescription());
        testCase.setSteps(request.getSteps());
        testCase.setExpectedResult(request.getExpectedResults());
        testCase.setPriority(request.getPriority() != null ? request.getPriority() : 0);
        testCase.setType(request.getType());
        testCase.setStatus(TestCase.TestCaseStatus.DRAFT.name());
        testCase.setTags(request.getTags());
        testCase.setRelatedRequirement(request.getRelatedRequirement());
        testCase.setCreatedBy(username);
        testCase.setCreatedAt(LocalDateTime.now());
        testCase.setUpdatedAt(LocalDateTime.now());

        testCaseMapper.insert(testCase);

        log.info("Test case created successfully with ID: {}", testCase.getId());

        return convertToResponse(testCase);
    }

    /**
     * Get test case by ID
     */
    public TestCaseResponse getTestCaseById(String id) {
        TestCase testCase = testCaseMapper.selectById(id);
        if (testCase == null) {
            throw new ResourceNotFoundException("TestCase", "id", id);
        }

        return convertToResponse(testCase);
    }

    /**
     * Get all test cases
     */
    public List<TestCaseResponse> getAllTestCases() {
        return testCaseMapper.selectAll()
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get test cases by status
     */
    public List<TestCaseResponse> getTestCasesByStatus(String status) {
        return testCaseMapper.selectByStatus(status)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get test cases by type
     */
    public List<TestCaseResponse> getTestCasesByType(String type) {
        return testCaseMapper.selectByType(type)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Update test case
     */
    public TestCaseResponse updateTestCase(String id, TestCaseRequest request) {
        TestCase testCase = testCaseMapper.selectById(id);
        if (testCase == null) {
            throw new ResourceNotFoundException("TestCase", "id", id);
        }

        testCase.setTitle(request.getTitle());
        testCase.setDescription(request.getDescription());
        testCase.setSteps(request.getSteps());
        testCase.setExpectedResult(request.getExpectedResults());
        testCase.setPriority(request.getPriority());
        testCase.setType(request.getType());
        testCase.setTags(request.getTags());
        testCase.setRelatedRequirement(request.getRelatedRequirement());
        testCase.setUpdatedAt(LocalDateTime.now());

        testCaseMapper.update(testCase);

        log.info("Test case updated: {}", id);

        return convertToResponse(testCase);
    }

    /**
     * Approve test case
     */
    public TestCaseResponse approveTestCase(String id) {
        TestCase testCase = testCaseMapper.selectById(id);
        if (testCase == null) {
            throw new ResourceNotFoundException("TestCase", "id", id);
        }

        testCase.setStatus(TestCase.TestCaseStatus.APPROVED.name());
        testCase.setUpdatedAt(LocalDateTime.now());
        testCaseMapper.update(testCase);

        log.info("Test case approved: {}", id);

        return convertToResponse(testCase);
    }

    /**
     * Delete test case
     */
    public void deleteTestCase(String id) {
        TestCase testCase = testCaseMapper.selectById(id);
        if (testCase == null) {
            throw new ResourceNotFoundException("TestCase", "id", id);
        }

        testCaseMapper.deleteById(id);
        log.info("Test case deleted: {}", id);
    }

    /**
     * Batch save test cases
     */
    public List<String> batchSaveTestCases(List<Map<String, Object>> testCaseMaps, String userId) {
        log.info("Batch saving {} test cases by user: {}", testCaseMaps.size(), userId);

        List<String> savedIds = new ArrayList<>();

        for (Map<String, Object> tcMap : testCaseMaps) {
            TestCase testCase = new TestCase();
            testCase.setId(UUID.randomUUID().toString());
            testCase.setTitle((String) tcMap.get("case_name"));
            testCase.setDescription((String) tcMap.getOrDefault("description", ""));

            // Parse steps - could be a List or a String
            Object stepsObj = tcMap.get("steps");
            if (stepsObj instanceof List) {
                testCase.setSteps((List<String>) stepsObj);
            } else if (stepsObj instanceof String) {
                // If it's a string, wrap it in a list
                testCase.setSteps(Collections.singletonList((String) stepsObj));
            }

            // Parse expected_result - could be a Map or a String
            Object expectedResultObj = tcMap.get("expected_result");
            if (expectedResultObj instanceof Map) {
                testCase.setExpectedResult(expectedResultObj.toString());
            } else if (expectedResultObj instanceof String) {
                testCase.setExpectedResult((String) expectedResultObj);
            }

            // Parse priority - could be Integer or String
            Object priorityObj = tcMap.getOrDefault("priority", "5");
            int priority;
            if (priorityObj instanceof Integer) {
                priority = (Integer) priorityObj;
            } else if (priorityObj instanceof String) {
                String priorityStr = (String) priorityObj;
                if ("HIGH".equalsIgnoreCase(priorityStr)) {
                    priority = 8;
                } else if ("MEDIUM".equalsIgnoreCase(priorityStr)) {
                    priority = 5;
                } else if ("LOW".equalsIgnoreCase(priorityStr)) {
                    priority = 3;
                } else if ("CRITICAL".equalsIgnoreCase(priorityStr)) {
                    priority = 10;
                } else {
                    priority = Integer.parseInt(priorityStr);
                }
            } else {
                priority = 5;
            }
            testCase.setPriority(priority);

            testCase.setType((String) tcMap.get("type"));
            testCase.setStatus(TestCase.TestCaseStatus.DRAFT.name());
            testCase.setTags(Collections.emptyList());
            testCase.setRelatedRequirement((String) tcMap.getOrDefault("module", ""));
            testCase.setCreatedBy(userId);
            testCase.setCreatedAt(LocalDateTime.now());
            testCase.setUpdatedAt(LocalDateTime.now());

            testCaseMapper.insert(testCase);
            savedIds.add(testCase.getId());
        }

        log.info("Batch saved {} test cases successfully", savedIds.size());
        return savedIds;
    }

    /**
     * Get test cases with filters
     */
    public List<TestCaseResponse> getTestCasesWithFilters(Boolean aiGenerated, String module, Double minConfidence) {
        log.info("Getting test cases with filters: aiGenerated={}, module={}, minConfidence={}",
                aiGenerated, module, minConfidence);

        // For now, return all test cases as filtering by AI-related fields
        // would require additional database schema changes
        // This is a placeholder implementation
        List<TestCase> testCases = testCaseMapper.selectAll();

        // Apply module filter if provided
        if (module != null && !module.isEmpty()) {
            testCases = testCases.stream()
                    .filter(tc -> module.equals(tc.getRelatedRequirement()))
                    .collect(Collectors.toList());
        }

        return testCases.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Convert entity to response DTO
     */
    private TestCaseResponse convertToResponse(TestCase testCase) {
        TestCaseResponse response = new TestCaseResponse();
        response.setId(testCase.getId());
        response.setTitle(testCase.getTitle());
        response.setCaseName(testCase.getTitle()); // 设置caseName字段，使用与title相同的值
        response.setDescription(testCase.getDescription());
        response.setSteps(testCase.getSteps());
        response.setExpectedResults(testCase.getExpectedResult());
        response.setPriority(testCase.getPriority());
        response.setType(testCase.getType());
        response.setStatus(testCase.getStatus());
        response.setTags(testCase.getTags());
        response.setRelatedRequirement(testCase.getRelatedRequirement());
        response.setCreatedAt(testCase.getCreatedAt());
        response.setUpdatedAt(testCase.getUpdatedAt());
        response.setCreatedBy(testCase.getCreatedBy());

        // Set AI-related fields (default values for now)
        response.setAi_generated(false);
        response.setAi_confidence(0.0);

        return response;
    }
}
