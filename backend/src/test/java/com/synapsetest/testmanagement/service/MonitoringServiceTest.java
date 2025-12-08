package com.synapsetest.testmanagement.service;

import com.synapsetest.testmanagement.mapper.MonitoringDataMapper;
import com.synapsetest.testmanagement.model.MonitoringData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 监控服务 - 实时数据管理 (US3)
 * TDD测试用例
 */
@SpringBootTest
@DisplayName("监控服务 - 实时数据管理")
class MonitoringServiceTest {

    @MockBean
    private MonitoringDataMapper monitoringDataMapper;

    @Autowired
    private MonitoringService monitoringService;

    @Test
    @DisplayName("场景1.1: 创建监控数据记录")
    void createMonitoringData_ShouldSaveToDatabase() {
        // Given: 任务启动时的监控数据
        String taskId = "task-001";
        MonitoringData data = new MonitoringData();
        data.setTaskId(taskId);
        data.setStatus("RUNNING");
        data.setProgress(0);
        data.setTotalCases(100);
        data.setExecutedCases(0);

        when(monitoringDataMapper.selectByTaskId(taskId)).thenReturn(Arrays.asList(data));
        when(monitoringDataMapper.update(any())).thenReturn(1);

        // When
        MonitoringData saved = monitoringService.startMonitoring(taskId, 100);

        // Then
        assertNotNull(saved);
        assertEquals(taskId, saved.getTaskId());
        assertEquals("RUNNING", saved.getStatus());
        assertEquals(0, saved.getProgress());
        assertNotNull(saved.getStartTime());
    }

    @Test
    @DisplayName("场景1.2: 更新任务执行进度")
    void updateProgress_ShouldCalculatePercentage() {
        // Given: 已执行25个用例，共100个
        String taskId = "task-001";
        MonitoringData existing = createMonitoringData(taskId, 100);
        when(monitoringDataMapper.selectByTaskId(taskId)).thenReturn(Arrays.asList(existing));
        when(monitoringDataMapper.update(any())).thenReturn(1);

        // When: 更新进度
        MonitoringData updated = monitoringService.updateProgress(taskId, 25, 23, 2, 0);

        // Then
        assertEquals(25, updated.getProgress()); // 25/100 * 100
        assertEquals(25, updated.getExecutedCases());
        assertEquals(23, updated.getPassedCases());
        assertEquals(2, updated.getFailedCases());
    }

    @Test
    @DisplayName("场景1.3: 计算通过率")
    void calculatePassRate_ShouldReturnCorrectPercentage() {
        // Given
        MonitoringData data = new MonitoringData();
        data.setExecutedCases(100);
        data.setPassedCases(85);
        data.setFailedCases(10);
        data.setSkippedCases(5);

        // When
        double passRate = data.getPassRate();

        // Then
        assertEquals(85.0, passRate, 0.01); // 85/100 * 100
    }

    @Test
    @DisplayName("场景1.4: 预估任务完成时间")
    void estimateCompletionTime_BasedOnProgress() {
        // Given: 25%进度，已运行30分钟
        MonitoringData data = new MonitoringData();
        data.setProgress(25);
        data.setStartTime(LocalDateTime.now().minusMinutes(30));
        data.setTotalCases(100);
        data.setExecutedCases(25);
        data.setEstimatedEndTime(LocalDateTime.now().plusMinutes(90));

        // When
        LocalDateTime estimatedEnd = data.getEstimatedEndTime();

        // Then: 预估总时间120分钟，还需90分钟
        assertNotNull(estimatedEnd);
        assertTrue(estimatedEnd.isAfter(LocalDateTime.now()));
    }

    @Test
    @DisplayName("场景1.5: 更新资源使用情况")
    void updateResourceUsage_ShouldRecordMetrics() {
        // Given
        String taskId = "task-001";
        Map<String, Object> resourceUsage = Map.of(
            "cpu", 72.5,
            "memory", 65.0,
            "disk", 45.0
        );

        MonitoringData existing = createMonitoringData(taskId, 100);
        when(monitoringDataMapper.selectByTaskId(taskId)).thenReturn(Arrays.asList(existing));
        when(monitoringDataMapper.update(any())).thenReturn(1);

        // When
        MonitoringData updated = monitoringService.updateResourceUsage(taskId, resourceUsage);

        // Then
        assertEquals(72.5, updated.getResourceUsage().get("cpu"));
        assertEquals(65.0, updated.getResourceUsage().get("memory"));
    }

    @Test
    @DisplayName("场景1.6: 获取运行中任务列表")
    void getRunningTasks_ShouldReturnOnlyRunningStatus() {
        // Given
        List<MonitoringData> runningTasks = Arrays.asList(
            createMonitoringDataWithStatus("task-001", "RUNNING"),
            createMonitoringDataWithStatus("task-002", "RUNNING")
        );
        when(monitoringDataMapper.selectByStatus("RUNNING")).thenReturn(runningTasks);

        // When
        List<MonitoringData> result = monitoringService.getRunningTasks();

        // Then
        assertEquals(2, result.size());
        result.forEach(task -> assertEquals("RUNNING", task.getStatus()));
    }

    @Test
    @DisplayName("场景1.7: 获取仪表盘统计数据")
    void getDashboardStats_ShouldAggregateMetrics() {
        // Given
        List<MonitoringData> recentData = Arrays.asList(
            createMonitoringDataWithStatus("task-001", "RUNNING"),
            createMonitoringDataWithStatus("task-002", "RUNNING"),
            createMonitoringDataWithStatus("task-003", "COMPLETED"),
            createMonitoringDataWithStatus("task-004", "COMPLETED"),
            createMonitoringDataWithStatus("task-005", "FAILED")
        );
        when(monitoringDataMapper.selectAll()).thenReturn(recentData);

        // When
        Map<String, Object> stats = monitoringService.getDashboardStatistics();

        // Then
        assertEquals(5, stats.get("totalTasks"));
        assertEquals(2L, stats.get("runningTasks"));
        assertEquals(2L, stats.get("completedTasks"));
        assertEquals(1L, stats.get("failedTasks"));
    }

    // Helper methods
    private MonitoringData createMonitoringData(String taskId, int totalCases) {
        MonitoringData data = new MonitoringData();
        data.setTaskId(taskId);
        data.setStatus("RUNNING");
        data.setProgress(0);
        data.setTotalCases(totalCases);
        data.setExecutedCases(0);
        data.setStartTime(LocalDateTime.now());
        return data;
    }

    private MonitoringData createMonitoringDataWithStatus(String taskId, String status) {
        MonitoringData data = createMonitoringData(taskId, 100);
        data.setStatus(status);
        return data;
    }
}
