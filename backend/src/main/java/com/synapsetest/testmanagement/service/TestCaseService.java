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
    private final CaseNumberGenerator caseNumberGenerator;

    /**
     * Create a new test case from request
     */
    public TestCaseResponse createTestCase(TestCaseRequest request, String username) {
        log.info("Creating test case: {} by user: {}", request.getTitle(), username);

        TestCase testCase = new TestCase();
        testCase.setId(UUID.randomUUID().toString());
        testCase.setCaseNumber(caseNumberGenerator.generateNextCaseNumber());
        testCase.setTitle(request.getTitle());
        testCase.setDescription(request.getDescription());
        testCase.setSteps(request.getSteps());
        testCase.setExpectedResult(request.getExpectedResult());  // 使用单数
        testCase.setPriority(request.getPriority() != null ? request.getPriority() : 5);
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
     * Create a new test case from TestCase entity (for AI-generated cases)
     * This overloaded method is used when AI-Service generates test cases
     */
    public TestCase createTestCase(TestCase testCase) {
        log.info("Creating test case from entity: {}", testCase.getTitle());

        // Generate ID if not present
        if (testCase.getId() == null || testCase.getId().isEmpty()) {
            testCase.setId(UUID.randomUUID().toString());
        }

        // Set timestamps if not present
        if (testCase.getCreatedAt() == null) {
            testCase.setCreatedAt(LocalDateTime.now());
        }
        if (testCase.getUpdatedAt() == null) {
            testCase.setUpdatedAt(LocalDateTime.now());
        }

        // Set default values if not present
        if (testCase.getStatus() == null || testCase.getStatus().isEmpty()) {
            testCase.setStatus(TestCase.TestCaseStatus.DRAFT.name());
        }
        if (testCase.getCreatedBy() == null || testCase.getCreatedBy().isEmpty()) {
            testCase.setCreatedBy("system");
        }

        testCaseMapper.insert(testCase);

        log.info("Test case created successfully with ID: {}", testCase.getId());

        return testCase;
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
        testCase.setExpectedResult(request.getExpectedResult());  // 使用单数
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
     * Returns a list of maps containing id and case_number for each saved test case
     */
    public List<Map<String, String>> batchSaveTestCases(List<Map<String, Object>> testCaseMaps, String userId) {
        log.info("Batch saving {} test cases by user: {}", testCaseMaps.size(), userId);

        List<Map<String, String>> savedCases = new ArrayList<>();

        for (Map<String, Object> tcMap : testCaseMaps) {
            TestCase testCase = new TestCase();
            String id = UUID.randomUUID().toString();
            String caseNumber = caseNumberGenerator.generateNextCaseNumber();
            
            testCase.setId(id);
            testCase.setCaseNumber(caseNumber);

            // Support both "title" and "case_name"
            String title = (String) tcMap.get("title");
            if (title == null) {
                title = (String) tcMap.get("case_name");
            }
            testCase.setTitle(title);

            testCase.setDescription((String) tcMap.getOrDefault("description", ""));

            // Parse steps - could be a List or a String
            // Each element should be a JSON object string like {"step":1,"action":"...","expected":"..."}
            Object stepsObj = tcMap.get("steps");
            if (stepsObj instanceof List) {
                // Already a list, use it directly
                testCase.setSteps((List<String>) stepsObj);
            } else if (stepsObj instanceof String) {
                // Single string, wrap in a list
                testCase.setSteps(Collections.singletonList((String) stepsObj));
            }

            // Parse expected_result - support both "expectedResult" and "expected_result"
            Object expectedResultObj = tcMap.get("expectedResult");
            if (expectedResultObj == null) {
                expectedResultObj = tcMap.get("expected_result");
            }
            if (expectedResultObj instanceof Map) {
                testCase.setExpectedResult(expectedResultObj.toString());
            } else if (expectedResultObj instanceof String) {
                testCase.setExpectedResult((String) expectedResultObj);
            }

            // Parse priority - could be Integer or String (including P0/P1/P2/P3 format)
            Object priorityObj = tcMap.getOrDefault("priority", "5");
            int priority;
            if (priorityObj instanceof Integer) {
                priority = (Integer) priorityObj;
            } else if (priorityObj instanceof String) {
                String priorityStr = (String) priorityObj;
                // Handle P0/P1/P2/P3 format
                if ("P0".equalsIgnoreCase(priorityStr)) {
                    priority = 10;  // P0 = Highest priority
                } else if ("P1".equalsIgnoreCase(priorityStr)) {
                    priority = 8;
                } else if ("P2".equalsIgnoreCase(priorityStr)) {
                    priority = 5;
                } else if ("P3".equalsIgnoreCase(priorityStr)) {
                    priority = 3;
                } else if ("HIGH".equalsIgnoreCase(priorityStr)) {
                    priority = 8;
                } else if ("MEDIUM".equalsIgnoreCase(priorityStr)) {
                    priority = 5;
                } else if ("LOW".equalsIgnoreCase(priorityStr)) {
                    priority = 3;
                } else if ("CRITICAL".equalsIgnoreCase(priorityStr)) {
                    priority = 10;
                } else {
                    try {
                        priority = Integer.parseInt(priorityStr);
                    } catch (NumberFormatException e) {
                        log.warn("Invalid priority format: {}, using default 5", priorityStr);
                        priority = 5;
                    }
                }
            } else {
                priority = 5;
            }
            testCase.setPriority(priority);

            testCase.setType((String) tcMap.get("type"));
            testCase.setStatus(TestCase.TestCaseStatus.DRAFT.name());

            // Parse tags - support List
            Object tagsObj = tcMap.get("tags");
            if (tagsObj instanceof List) {
                testCase.setTags((List<String>) tagsObj);
            } else {
                testCase.setTags(Collections.emptyList());
            }

            // Set module and preconditions
            testCase.setModule((String) tcMap.get("module"));

            Object preconditionsObj = tcMap.get("preconditions");
            if (preconditionsObj instanceof List) {
                testCase.setPreconditions((List<String>) preconditionsObj);
            }

            // Set related_requirement from the correct field (not from module)
            testCase.setRelatedRequirement((String) tcMap.get("related_requirement"));
            
            // Parse quality_score
            Object qualityScoreObj = tcMap.get("quality_score");
            if (qualityScoreObj != null) {
                if (qualityScoreObj instanceof Double) {
                    testCase.setQualityScore(((Double) qualityScoreObj).floatValue());
                } else if (qualityScoreObj instanceof Float) {
                    testCase.setQualityScore((Float) qualityScoreObj);
                } else if (qualityScoreObj instanceof Number) {
                    testCase.setQualityScore(((Number) qualityScoreObj).floatValue());
                }
            }
            
            // Parse ai_generated
            Object aiGeneratedObj = tcMap.get("ai_generated");
            if (aiGeneratedObj instanceof Boolean) {
                testCase.setAiGenerated((Boolean) aiGeneratedObj);
            } else if (aiGeneratedObj != null) {
                testCase.setAiGenerated(Boolean.parseBoolean(aiGeneratedObj.toString()));
            }
            
            // Parse ai_confidence
            Object aiConfidenceObj = tcMap.get("ai_confidence");
            if (aiConfidenceObj != null) {
                if (aiConfidenceObj instanceof Double) {
                    testCase.setAiConfidence(((Double) aiConfidenceObj).floatValue());
                } else if (aiConfidenceObj instanceof Float) {
                    testCase.setAiConfidence((Float) aiConfidenceObj);
                } else if (aiConfidenceObj instanceof Number) {
                    testCase.setAiConfidence(((Number) aiConfidenceObj).floatValue());
                }
            }
            
            testCase.setCreatedBy(userId);
            testCase.setCreatedAt(LocalDateTime.now());
            testCase.setUpdatedAt(LocalDateTime.now());

            testCaseMapper.insert(testCase);
            
            // Add both id and case_number to result
            Map<String, String> savedCase = Map.of(
                "id", id,
                "case_number", caseNumber
            );
            savedCases.add(savedCase);
        }

        log.info("Batch saved {} test cases successfully", savedCases.size());
        return savedCases;
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
     * Get all distinct modules from test cases
     */
    public List<String> getAllModules() {
        log.info("Getting all distinct modules");

        List<TestCase> testCases = testCaseMapper.selectAll();

        // Extract distinct non-null, non-empty modules
        List<String> modules = testCases.stream()
                .map(TestCase::getModule)
                .filter(module -> module != null && !module.trim().isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        log.info("Found {} distinct modules", modules.size());
        return modules;
    }

    /**
     * Convert entity to response DTO
     */
    private TestCaseResponse convertToResponse(TestCase testCase) {
        TestCaseResponse response = new TestCaseResponse();
        response.setId(testCase.getId());
        response.setCaseNumber(testCase.getCaseNumber());
        response.setTitle(testCase.getTitle());
        response.setCaseName(testCase.getTitle()); // 设置caseName字段，使用与title相同的值
        response.setDescription(testCase.getDescription());
        response.setSteps(testCase.getSteps());
        response.setExpectedResult(testCase.getExpectedResult());  // 使用单数
        response.setPriority(testCase.getPriority());
        response.setType(testCase.getType());
        response.setStatus(testCase.getStatus());
        response.setTags(testCase.getTags());
        response.setRelatedRequirement(testCase.getRelatedRequirement());
        response.setCreatedAt(testCase.getCreatedAt());
        response.setUpdatedAt(testCase.getUpdatedAt());
        response.setCreatedBy(testCase.getCreatedBy());

        // Set AI-related fields
        response.setModule(testCase.getModule());
        response.setPreconditions(testCase.getPreconditions());
        response.setQualityScore(testCase.getQualityScore());
        response.setAiGenerated(testCase.getAiGenerated() != null ? testCase.getAiGenerated() : false);
        response.setAiConfidence(testCase.getAiConfidence() != null ? testCase.getAiConfidence() : 0.0f);

        return response;
    }
}
