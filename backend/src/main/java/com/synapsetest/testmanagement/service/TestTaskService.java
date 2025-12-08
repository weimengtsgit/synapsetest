package com.synapsetest.testmanagement.service;

import com.synapsetest.testmanagement.dto.TestTaskRequest;
import com.synapsetest.testmanagement.dto.request.CreateTestTaskRequest;
import com.synapsetest.testmanagement.dto.response.PageResponse;
import com.synapsetest.testmanagement.dto.response.TestTaskResponse;
import com.synapsetest.testmanagement.exception.ResourceNotFoundException;
import com.synapsetest.testmanagement.exception.ValidationException;
import com.synapsetest.testmanagement.mapper.TestTaskMapper;
import com.synapsetest.testmanagement.model.TestTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * TestTask Service (MyBatis version)
 * Business logic for test task management
 *
 * Task: T030 [US1] Implement TestTaskService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TestTaskService {

    private final TestTaskMapper testTaskMapper;
    private final TestRecommendationService recommendationService;

    /**
     * Create a new test task with AI recommendations
     */
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

        // Convert to response DTO
        return convertToResponse(testTask, recommendation);
    }

    /**
     * Get test task by ID
     * Note: Soft-deleted tasks (CANCELLED status) are treated as not found
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

        return convertToResponse(testTask, null);
    }

    /**
     * Get all test tasks (excluding soft-deleted tasks)
     */
    public List<TestTaskResponse> getAllTestTasks() {
        return testTaskMapper.selectAll()
                .stream()
                .filter(task -> !TestTask.Status.CANCELLED.name().equals(task.getStatus()))
                .map(task -> convertToResponse(task, null))
                .collect(Collectors.toList());
    }

    /**
     * Get test tasks by status
     * Note: When querying non-CANCELLED status, soft-deleted tasks are excluded
     */
    public List<TestTaskResponse> getTestTasksByStatus(String status) {
        List<TestTask> tasks = testTaskMapper.selectByStatus(status);
        
        // If explicitly querying CANCELLED status, return all cancelled tasks
        // Otherwise, exclude soft-deleted tasks
        if (TestTask.Status.CANCELLED.name().equals(status)) {
            return tasks.stream()
                    .map(task -> convertToResponse(task, null))
                    .collect(Collectors.toList());
        }
        
        // For other statuses, exclude CANCELLED tasks (soft-deleted)
        return tasks.stream()
                .filter(task -> !TestTask.Status.CANCELLED.name().equals(task.getStatus()))
                .map(task -> convertToResponse(task, null))
                .collect(Collectors.toList());
    }

    /**
     * Get test tasks by status with pagination
     * Note: When querying non-CANCELLED status, soft-deleted tasks are excluded
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
                .map(task -> convertToResponse(task, null))
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

        return convertToResponse(testTask, null);
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

        return convertToResponse(testTask, null);
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

        return convertToResponse(testTask, null);
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
    public void deleteTestTask(String id) {
        TestTask testTask = testTaskMapper.selectById(id);
        if (testTask == null) {
            throw new ResourceNotFoundException("TestTask", "id", id);
        }

        // Soft delete: mark as cancelled
        testTask.setStatus(TestTask.Status.CANCELLED.name());
        testTask.setUpdatedAt(LocalDateTime.now());
        testTaskMapper.update(testTask);

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
     * Convert entity to response DTO
     */
    private TestTaskResponse convertToResponse(TestTask task, TestTaskResponse.TestRecommendation recommendation) {
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

        return response;
    }
}
