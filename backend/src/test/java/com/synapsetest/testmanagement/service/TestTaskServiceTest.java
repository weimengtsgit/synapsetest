package com.synapsetest.testmanagement.service;

import com.synapsetest.testmanagement.model.TestTask;
import com.synapsetest.testmanagement.dto.request.CreateTestTaskRequest;
import com.synapsetest.testmanagement.dto.response.TestTaskResponse;
import com.synapsetest.testmanagement.exception.ResourceNotFoundException;
import com.synapsetest.testmanagement.exception.ValidationException;
import com.synapsetest.testmanagement.mapper.TestTaskMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 测试任务服务 - 智能调度场景 (US1)
 * TDD测试用例
 */
@SpringBootTest
@DisplayName("测试任务服务 - 智能调度场景")
class TestTaskServiceTest {

    @MockBean
    private TestTaskMapper testTaskMapper;

    @MockBean
    private TestRecommendationService recommendationService;

    @Autowired
    private TestTaskService testTaskService;

    @Test
    @DisplayName("场景1.1: 创建冒烟测试任务成功")
    void createSmokeTestTask_WithValidInput_ShouldReturnTask() {
        // Given: 有效的任务请求数据
        CreateTestTaskRequest request = new CreateTestTaskRequest();
        request.setTaskName("登录模块冒烟测试");
        request.setEnvironment("DEV");
        request.setVersion("v2.1.0");
        request.setModules(List.of("login", "auth"));
        request.setCodeChangeInfo(Map.of(
            "changed_files_count", 3,
            "changed_lines_count", 25,
            "is_hotfix", false
        ));

        // Mock recommendation service
        TestTaskResponse.TestRecommendation recommendation = new TestTaskResponse.TestRecommendation();
        recommendation.setRecommendedScope("SMOKE");
        recommendation.setRecommendedEnvironment("DEV");
        recommendation.setRecommendedVersion("v2.1.0");
        recommendation.setConfidenceScore(0.85);
        recommendation.setReasoning("Small code changes suggest smoke testing");
        when(recommendationService.getTestRecommendation(any(CreateTestTaskRequest.class)))
            .thenReturn(recommendation);

        when(testTaskMapper.insert(any(TestTask.class))).thenReturn(1);

        // When: 调用创建服务
        TestTaskResponse response = testTaskService.createTestTask(request, "zhangsan");

        // Then: 任务创建成功
        assertNotNull(response.getId());
        assertEquals("登录模块冒烟测试", response.getTaskName());
        assertEquals("PENDING", response.getStatus());
        assertEquals("zhangsan", response.getCreatedBy());
        assertEquals("SMOKE", response.getTestScope());
        assertNotNull(response.getCreatedAt());
        assertNotNull(response.getAiRecommendation());
        verify(testTaskMapper, times(1)).insert(any(TestTask.class));
        verify(recommendationService, times(1)).getTestRecommendation(any(CreateTestTaskRequest.class));
    }

    @Test
    @DisplayName("场景1.2: 创建带AI推荐的核心测试任务")
    void createCoreTestTask_WithCriticalModule_ShouldReturnTask() {
        // Given: 关键模块变更的任务请求
        CreateTestTaskRequest request = new CreateTestTaskRequest();
        request.setTaskName("支付模块核心测试");
        request.setEnvironment("TEST");
        request.setVersion("v2.1.0");
        request.setModules(List.of("payment", "order"));
        request.setCodeChangeInfo(Map.of(
            "changed_files_count", 15,
            "changed_lines_count", 200,
            "is_critical_module", true
        ));

        // Mock recommendation service
        TestTaskResponse.TestRecommendation recommendation = new TestTaskResponse.TestRecommendation();
        recommendation.setRecommendedScope("CORE");
        recommendation.setRecommendedEnvironment("TEST");
        recommendation.setRecommendedVersion("v2.1.0");
        recommendation.setConfidenceScore(0.90);
        recommendation.setReasoning("Critical module changes require core regression testing");
        when(recommendationService.getTestRecommendation(any(CreateTestTaskRequest.class)))
            .thenReturn(recommendation);

        when(testTaskMapper.insert(any(TestTask.class))).thenReturn(1);

        // When: 调用创建服务
        TestTaskResponse response = testTaskService.createTestTask(request, "lisi");

        // Then: 任务创建成功，AI推荐CORE测试
        assertNotNull(response.getId());
        assertEquals("支付模块核心测试", response.getTaskName());
        assertEquals("CORE", response.getTestScope());
        assertEquals("TEST", response.getEnvironment());
        assertNotNull(response.getAiRecommendation());
        assertEquals("CORE", response.getAiRecommendation().get("test_scope"));
        assertEquals(0.90, response.getAiRecommendation().get("confidence"));
    }

    @Test
    @DisplayName("场景1.4: 启动PENDING状态任务成功")
    void startTask_FromPendingStatus_ShouldSucceed() {
        // Given: PENDING状态的任务
        TestTask task = createTestTask("task-001", "PENDING");
        when(testTaskMapper.selectById("task-001")).thenReturn(task);
        when(testTaskMapper.update(any())).thenReturn(1);

        // When: 启动任务
        TestTaskResponse response = testTaskService.startTestTask("task-001");

        // Then: 状态变为RUNNING
        assertEquals("RUNNING", response.getStatus());
    }

    @Test
    @DisplayName("场景1.5: 启动已完成任务失败")
    void startTask_FromCompletedStatus_ShouldThrowException() {
        // Given: COMPLETED状态的任务
        TestTask task = createTestTask("task-001", "COMPLETED");
        when(testTaskMapper.selectById("task-001")).thenReturn(task);

        // When & Then: 抛出验证异常
        assertThrows(ValidationException.class,
            () -> testTaskService.startTestTask("task-001"));
    }

    @Test
    @DisplayName("场景1.6: 获取不存在的任务抛出异常")
    void getTaskById_WithNonExistingId_ShouldThrowResourceNotFoundException() {
        // Given: 不存在的ID
        when(testTaskMapper.selectById("non-existing")).thenReturn(null);

        // When & Then
        assertThrows(ResourceNotFoundException.class,
            () -> testTaskService.getTestTaskById("non-existing"));
    }

    // Helper methods
    private TestTask createTestTask(String id, String status) {
        TestTask task = new TestTask();
        task.setId(id);
        task.setName("测试任务");
        task.setEnvironment("DEV");
        task.setVersion("v1.0.0");
        task.setStatus(status);
        task.setPriority(5);
        task.setCreatedBy("tester");
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        return task;
    }
}
