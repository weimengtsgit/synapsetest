package com.synapsetest.testmanagement.service;

import com.synapsetest.testmanagement.dto.request.CreateTestTaskRequest;
import com.synapsetest.testmanagement.dto.response.PageResponse;
import com.synapsetest.testmanagement.dto.response.TestTaskResponse;
import com.synapsetest.testmanagement.exception.ResourceNotFoundException;
import com.synapsetest.testmanagement.exception.ValidationException;
import com.synapsetest.testmanagement.mapper.TaskTestCaseMapper;
import com.synapsetest.testmanagement.mapper.TestCaseMapper;
import com.synapsetest.testmanagement.mapper.TestTaskMapper;
import com.synapsetest.testmanagement.model.TaskTestCase;
import com.synapsetest.testmanagement.model.TestCase;
import com.synapsetest.testmanagement.model.TestTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * TestTask Service (MyBatis version with TestCase association)
 * Business logic for test task management
 *
 * Task: T030 [US1] Implement TestTaskService
 * Enhanced with task-testcase association management
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TestTaskService {

    private final TestTaskMapper testTaskMapper;
    private final TaskTestCaseMapper taskTestCaseMapper;
    private final TestCaseMapper testCaseMapper;
    private final TestRecommendationService recommendationService;

    /**
     * Create a new test task with AI recommendations and associated test cases
     */
    @Transactional
    public TestTaskResponse createTestTask(CreateTestTaskRequest request, String username) {
        log.info("Creating test task: {} by user: {}", request.getTaskName(), username);

        // Get AI recommendations based on code change info
        TestTaskResponse.TestRecommendation recommendation =
                recommendationService.getTestRecommendation(request);

        // Create test task entity
        TestTask testTask = new TestTask();
        testTask.setId(UUID.randomUUID().toString());
        testTask.setName(request.getTaskName());
        testTask.setDescription("Test task for modules: " + String.join(", ", request.getModules()));
        testTask.setEnvironment(request.getEnvironment());
        testTask.setVersion(request.getVersion());
        testTask.setTestScope(recommendation.getRecommendedScope());
        testTask.setStatus(TestTask.Status.PENDING.name());
        testTask.setPriority(8); // Default priority for AI-recommended tasks
        testTask.setCreatedBy(username);
        testTask.setCreatedAt(LocalDateTime.now());
        testTask.setUpdatedAt(LocalDateTime.now());

        // Save to database
        testTaskMapper.insert(testTask);

        log.info("Test task created successfully with ID: {}", testTask.getId());

        // Match relevant test cases
        List<TestCase> matchedTestCases = matchTestCases(request, recommendation);
        
        // Create task-testcase associations
        if (!matchedTestCases.isEmpty()) {
            List<TaskTestCase> associations = new ArrayList<>();
            for (int i = 0; i < matchedTestCases.size(); i++) {
                TaskTestCase association = new TaskTestCase();
                association.setTaskId(testTask.getId());
                association.setTestCaseId(matchedTestCases.get(i).getId());
                association.setExecutionOrder(i + 1);
                association.setCreatedAt(LocalDateTime.now());
                associations.add(association);
            }
            taskTestCaseMapper.batchInsert(associations);
            log.info("Associated {} test cases with task {}", matchedTestCases.size(), testTask.getId());
        } else {
            log.warn("No test cases matched for task {}", testTask.getId());
        }

        // Convert to response with test cases
        return convertToResponseWithTestCases(testTask, recommendation, matchedTestCases);
    }

    /**
     * Match relevant test cases based on request and recommendation
     */
    private List<TestCase> matchTestCases(CreateTestTaskRequest request, 
                                          TestTaskResponse.TestRecommendation recommendation) {
        log.info("Matching test cases for modules: {}", request.getModules());
        
        List<TestCase> allTestCases = new ArrayList<>();
        
        // Strategy 1: Match by modules
        for (String module : request.getModules()) {
            List<TestCase> moduleCases = testCaseMapper.selectByModule(module);
            allTestCases.addAll(moduleCases);
            log.debug("Found {} test cases for module: {}", moduleCases.size(), module);
        }
        
        if (allTestCases.isEmpty()) {
            log.warn("No test cases found for modules: {}", request.getModules());
            return Collections.emptyList();
        }
        
        // Strategy 2: Filter by test scope and priority
        String scope = recommendation.getRecommendedScope();
        List<TestCase> filteredCases = filterByTestScope(allTestCases, scope);
        
        // Strategy 3: Remove duplicates (keep higher priority)
        filteredCases = removeDuplicates(filteredCases);
        
        // Strategy 4: Sort by priority (descending)
        filteredCases.sort((a, b) -> b.getPriority().compareTo(a.getPriority()));
        
        log.info("Matched {} test cases after filtering (scope: {})", filteredCases.size(), scope);
        return filteredCases;
    }
    
    /**
     * Filter test cases by test scope
     */
    private List<TestCase> filterByTestScope(List<TestCase> testCases, String scope) {
        switch (scope) {
            case "SMOKE":
                // Smoke testing: Only P0 high priority cases (priority >= 8)
                return testCases.stream()
                    .filter(tc -> tc.getPriority() >= 8)
                    .limit(20)  // Limit to 20 cases for smoke test
                    .collect(Collectors.toList());
                
            case "CORE":
                // Core regression: P0-P1 cases (priority >= 5)
                return testCases.stream()
                    .filter(tc -> tc.getPriority() >= 5)
                    .limit(50)  // Limit to 50 cases
                    .collect(Collectors.toList());
                
            case "FULL":
                // Full regression: All cases
                return testCases;
                
            default:
                log.warn("Unknown test scope: {}, using CORE as default", scope);
                return testCases.stream()
                    .filter(tc -> tc.getPriority() >= 5)
                    .limit(50)
                    .collect(Collectors.toList());
        }
    }
    
    /**
     * Remove duplicate test cases (keep the one with higher priority)
     */
    private List<TestCase> removeDuplicates(List<TestCase> testCases) {
        Map<String, TestCase> uniqueCases = new LinkedHashMap<>();
        for (TestCase testCase : testCases) {
            String key = testCase.getId();
            TestCase existing = uniqueCases.get(key);
            if (existing == null || testCase.getPriority() > existing.getPriority()) {
                uniqueCases.put(key, testCase);
            }
        }
        return new ArrayList<>(uniqueCases.values());
    }

    /**
     * Preview matched test cases without creating a task
     */
    public List<TestCase> previewMatchedTestCases(CreateTestTaskRequest request) {
        log.info("Previewing test cases for modules: {}", request.getModules());
        
        // Get AI recommendations
        TestTaskResponse.TestRecommendation recommendation =
                recommendationService.getTestRecommendation(request);
        
        // Match test cases
        return matchTestCases(request, recommendation);
    }

    /**
     * Get test task by ID with associated test cases
     */
    public TestTaskResponse getTestTaskById(String id) {
        TestTask testTask = testTaskMapper.selectById(id);
        if (testTask == null) {
            throw new ResourceNotFoundException("TestTask", "id", id);
        }

        // Treat soft-deleted (CANCELLED) tasks as not found
        if (TestTask.Status.CANCELLED.name().equals(testTask.getStatus())) {
            throw new ResourceNotFoundException("TestTask", "id", id);
        }

        // Get associated test cases
        List<TestCase> testCases = taskTestCaseMapper.selectTestCasesByTaskId(id);
        
        return convertToResponseWithTestCases(testTask, null, testCases);
    }
    
    /**
     * Get test cases associated with a task
     */
    public List<TestCase> getTestCasesByTaskId(String taskId) {
        return taskTestCaseMapper.selectTestCasesByTaskId(taskId);
    }

    /**
     * Get all test tasks (excluding soft-deleted tasks)
     */
    public List<TestTaskResponse> getAllTestTasks() {
        return testTaskMapper.selectAll()
                .stream()
                .filter(task -> !TestTask.Status.CANCELLED.name().equals(task.getStatus()))
                .map(task -> convertToResponseWithTestCases(task, null, 
                    taskTestCaseMapper.selectTestCasesByTaskId(task.getId())))
                .collect(Collectors.toList());
    }

    /**
     * Get test tasks by status
     */
    public List<TestTaskResponse> getTestTasksByStatus(String status) {
        List<TestTask> tasks = testTaskMapper.selectByStatus(status);
        
        // If explicitly querying CANCELLED status, return all cancelled tasks
        // Otherwise, exclude soft-deleted tasks
        if (TestTask.Status.CANCELLED.name().equals(status)) {
            return tasks.stream()
                    .map(task -> convertToResponseWithTestCases(task, null,
                        taskTestCaseMapper.selectTestCasesByTaskId(task.getId())))
                    .collect(Collectors.toList());
        }
        
        // For other statuses, exclude CANCELLED tasks (soft-deleted)
        return tasks.stream()
                .filter(task -> !TestTask.Status.CANCELLED.name().equals(task.getStatus()))
                .map(task -> convertToResponseWithTestCases(task, null,
                    taskTestCaseMapper.selectTestCasesByTaskId(task.getId())))
                .collect(Collectors.toList());
    }

    /**
     * Get test tasks by status with pagination
     */
    public PageResponse<TestTaskResponse> getTestTasksByStatusWithPagination(
            String status, int page, int size) {
        
        // Get all tasks with the given status
        List<TestTask> allTasks = testTaskMapper.selectByStatus(status);
        
        // If not explicitly querying CANCELLED status, filter out soft-deleted tasks
        if (!TestTask.Status.CANCELLED.name().equals(status)) {
            allTasks = allTasks.stream()
                    .filter(task -> !TestTask.Status.CANCELLED.name().equals(task.getStatus()))
                    .collect(Collectors.toList());
        }
        
        long totalElements = allTasks.size();
        
        // Manual pagination
        List<TestTaskResponse> pagedTasks = allTasks.stream()
                .skip((long) page * size)
                .limit(size)
                .map(task -> convertToResponseWithTestCases(task, null,
                    taskTestCaseMapper.selectTestCasesByTaskId(task.getId())))
                .collect(Collectors.toList());
        
        return PageResponse.of(pagedTasks, page, size, totalElements);
    }

    /**
     * Start a test task
     */
    public TestTaskResponse startTestTask(String id) {
        TestTask testTask = testTaskMapper.selectById(id);
        if (testTask == null) {
            throw new ResourceNotFoundException("TestTask", "id", id);
        }

        if (!TestTask.Status.PENDING.name().equals(testTask.getStatus())) {
            throw new ValidationException("Test task must be in PENDING status to start");
        }

        testTask.setStatus(TestTask.Status.RUNNING.name());
        testTask.setUpdatedAt(LocalDateTime.now());
        testTaskMapper.update(testTask);

        log.info("Test task started: {}", id);

        List<TestCase> testCases = taskTestCaseMapper.selectTestCasesByTaskId(id);
        return convertToResponseWithTestCases(testTask, null, testCases);
    }

    /**
     * Cancel a test task
     */
    public TestTaskResponse cancelTestTask(String id) {
        TestTask testTask = testTaskMapper.selectById(id);
        if (testTask == null) {
            throw new ResourceNotFoundException("TestTask", "id", id);
        }

        if (TestTask.Status.COMPLETED.name().equals(testTask.getStatus())) {
            throw new ValidationException("Cannot cancel a completed test task");
        }

        testTask.setStatus(TestTask.Status.CANCELLED.name());
        testTask.setUpdatedAt(LocalDateTime.now());
        testTaskMapper.update(testTask);

        log.info("Test task cancelled: {}", id);

        List<TestCase> testCases = taskTestCaseMapper.selectTestCasesByTaskId(id);
        return convertToResponseWithTestCases(testTask, null, testCases);
    }

    /**
     * Update task status
     */
    public TestTaskResponse updateTaskStatus(String id, String newStatus) {
        TestTask testTask = testTaskMapper.selectById(id);
        if (testTask == null) {
            throw new ResourceNotFoundException("TestTask", "id", id);
        }

        // Validate status transition
        String currentStatus = testTask.getStatus();
        validateStatusTransition(currentStatus, newStatus);

        testTask.setStatus(newStatus);
        testTask.setUpdatedAt(LocalDateTime.now());
        testTaskMapper.update(testTask);

        log.info("Test task status updated: {} from {} to {}", id, currentStatus, newStatus);

        List<TestCase> testCases = taskTestCaseMapper.selectTestCasesByTaskId(id);
        return convertToResponseWithTestCases(testTask, null, testCases);
    }

    /**
     * Validate status transition
     */
    private void validateStatusTransition(String currentStatus, String newStatus) {
        // COMPLETED tasks cannot be changed
        if (TestTask.Status.COMPLETED.name().equals(currentStatus)) {
            throw new ValidationException("Cannot update status of a completed task");
        }

        // Validate new status is valid
        try {
            TestTask.Status.valueOf(newStatus);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid status: " + newStatus);
        }
    }

    /**
     * Delete a test task (soft delete by setting status to CANCELLED)
     */
    @Transactional
    public void deleteTestTask(String id) {
        TestTask testTask = testTaskMapper.selectById(id);
        if (testTask == null) {
            throw new ResourceNotFoundException("TestTask", "id", id);
        }

        // Soft delete: mark as cancelled
        testTask.setStatus(TestTask.Status.CANCELLED.name());
        testTask.setUpdatedAt(LocalDateTime.now());
        testTaskMapper.update(testTask);
        
        // Note: We keep the task-testcase associations for audit purposes

        log.info("Test task soft deleted (marked as CANCELLED): {}", id);
    }

    /**
     * Get AI recommendation for test task
     */
    public TestTaskResponse.TestRecommendation getAiRecommendation(CreateTestTaskRequest request) {
        log.info("Getting AI recommendation for context: {}", request.getTaskName());
        return recommendationService.getTestRecommendation(request);
    }
    
    /**
     * Convert entity to response DTO with test cases
     */
    private TestTaskResponse convertToResponseWithTestCases(
            TestTask task, 
            TestTaskResponse.TestRecommendation recommendation,
            List<TestCase> testCases) {
        
        TestTaskResponse response = new TestTaskResponse();
        response.setId(task.getId());
        response.setName(task.getName());
        response.setTaskName(task.getName()); // Set both name and taskName for compatibility
        response.setDescription(task.getDescription());
        response.setEnvironment(task.getEnvironment());
        response.setVersion(task.getVersion());
        response.setTestScope(task.getTestScope());
        response.setStatus(task.getStatus());
        response.setPriority(task.getPriority());
        response.setCreatedAt(task.getCreatedAt());
        response.setUpdatedAt(task.getUpdatedAt());
        response.setCreatedBy(task.getCreatedBy());
        response.setRecommendation(recommendation);
        
        // Convert recommendation to aiRecommendation Map format for API compatibility
        if (recommendation != null) {
            Map<String, Object> aiRecommendation = new HashMap<>();
            aiRecommendation.put("test_scope", recommendation.getRecommendedScope());
            aiRecommendation.put("environment", recommendation.getRecommendedEnvironment());
            aiRecommendation.put("version", recommendation.getRecommendedVersion());
            aiRecommendation.put("confidence", recommendation.getConfidenceScore());
            aiRecommendation.put("reasoning", recommendation.getReasoning());
            response.setAiRecommendation(aiRecommendation);
        }
        
        // Add test cases information
        if (testCases != null && !testCases.isEmpty()) {
            List<TestTaskResponse.TestCaseInfo> testCaseInfos = testCases.stream()
                .map(this::convertToTestCaseInfo)
                .collect(Collectors.toList());
            response.setTestCases(testCaseInfos);
            response.setTotalTestCases(testCaseInfos.size());
        } else {
            response.setTestCases(Collections.emptyList());
            response.setTotalTestCases(0);
        }

        return response;
    }
    
    /**
     * Convert TestCase to TestCaseInfo
     */
    private TestTaskResponse.TestCaseInfo convertToTestCaseInfo(TestCase testCase) {
        TestTaskResponse.TestCaseInfo info = new TestTaskResponse.TestCaseInfo();
        info.setId(testCase.getId());
        info.setCaseNumber(testCase.getCaseNumber());
        info.setTitle(testCase.getTitle());
        info.setModule(testCase.getModule());
        info.setPriority(testCase.getPriority());
        info.setType(testCase.getType());
        info.setStatus(testCase.getStatus());
        // Note: executionOrder is not set here as it's not part of TestCase entity
        // It should be set when querying from task_test_cases table
        return info;
    }
}
