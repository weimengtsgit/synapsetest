package com.synapsetest.testmanagement.service;

import com.synapsetest.testmanagement.exception.ResourceNotFoundException;
import com.synapsetest.testmanagement.model.MonitoringData;
import com.synapsetest.testmanagement.mapper.MonitoringDataMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Monitoring Service
 * Business logic for real-time test monitoring
 *
 * Task: T064 [US3] Implement MonitoringService
 */
@Slf4j
@Service
@RequiredArgsConstructor
// @Profile("mongodb") // Temporarily disabled to show in Swagger UI
public class MonitoringService {

    private final MonitoringDataMapper monitoringDataMapper;

    /**
     * Create monitoring data for a test task
     */
    public MonitoringData createMonitoringData(String taskId, String environment, String version) {
        log.info("Creating monitoring data for task: {}", taskId);

        MonitoringData data = new MonitoringData();
        data.setTaskId(taskId);
        data.setStatus(MonitoringData.Status.PENDING.name());
        data.setProgress(0);
        data.setExecutedCases(0);
        data.setTotalCases(0);
        data.setPassedCases(0);
        data.setFailedCases(0);
        data.setSkippedCases(0);
        data.setEnvironment(environment);
        data.setVersion(version);
        data.setTimestamp(LocalDateTime.now());

        // Generate UUID for the new monitoring data
        if (data.getId() == null) {
            data.setId(java.util.UUID.randomUUID().toString());
        }
        monitoringDataMapper.insert(data);
        MonitoringData saved = data;

        log.info("Monitoring data created: {}", saved.getId());

        return saved;
    }

    /**
     * Get monitoring data by task ID
     */
    public MonitoringData getMonitoringDataByTaskId(String taskId) {
        return monitoringDataMapper.selectByTaskId(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("MonitoringData", "taskId", taskId));
    }

    /**
     * Update monitoring data
     */
    public MonitoringData updateMonitoringData(String taskId, MonitoringData updates) {
        MonitoringData existing = getMonitoringDataByTaskId(taskId);

        if (updates.getStatus() != null) {
            existing.setStatus(updates.getStatus());
        }
        if (updates.getProgress() != null) {
            existing.setProgress(updates.getProgress());
        }
        if (updates.getExecutedCases() != null) {
            existing.setExecutedCases(updates.getExecutedCases());
        }
        if (updates.getTotalCases() != null) {
            existing.setTotalCases(updates.getTotalCases());
        }
        if (updates.getPassedCases() != null) {
            existing.setPassedCases(updates.getPassedCases());
        }
        if (updates.getFailedCases() != null) {
            existing.setFailedCases(updates.getFailedCases());
        }
        if (updates.getSkippedCases() != null) {
            existing.setSkippedCases(updates.getSkippedCases());
        }
        if (updates.getStartTime() != null) {
            existing.setStartTime(updates.getStartTime());
        }
        if (updates.getEstimatedEndTime() != null) {
            existing.setEstimatedEndTime(updates.getEstimatedEndTime());
        }
        if (updates.getActualEndTime() != null) {
            existing.setActualEndTime(updates.getActualEndTime());
        }
        if (updates.getResourceUsage() != null) {
            existing.setResourceUsage(updates.getResourceUsage());
        }
        if (updates.getPerformanceMetrics() != null) {
            existing.setPerformanceMetrics(updates.getPerformanceMetrics());
        }

        existing.setTimestamp(LocalDateTime.now());

        monitoringDataMapper.update(existing);
        MonitoringData updated = existing;

        log.info("Monitoring data updated for task: {}", taskId);

        return updated;
    }

    /**
     * Start monitoring for a task
     */
    public MonitoringData startMonitoring(String taskId, int totalCases) {
        MonitoringData data = getMonitoringDataByTaskId(taskId);

        data.setStatus(MonitoringData.Status.RUNNING.name());
        data.setStartTime(LocalDateTime.now());
        data.setTotalCases(totalCases);
        data.setProgress(0);

        // Estimate end time (simple heuristic: 5 seconds per test case)
        LocalDateTime estimatedEnd = LocalDateTime.now().plusSeconds(totalCases * 5L);
        data.setEstimatedEndTime(estimatedEnd);

        log.info("Started monitoring for task: {} with {} test cases", taskId, totalCases);

        monitoringDataMapper.update(data);
        return data;
    }

    /**
     * Update progress during test execution
     */
    public MonitoringData updateProgress(String taskId, int executedCases, int passedCases,
                                          int failedCases, int skippedCases) {
        MonitoringData data = getMonitoringDataByTaskId(taskId);

        data.setExecutedCases(executedCases);
        data.setPassedCases(passedCases);
        data.setFailedCases(failedCases);
        data.setSkippedCases(skippedCases);

        // Calculate progress percentage
        if (data.getTotalCases() != null && data.getTotalCases() > 0) {
            int progress = (int) ((double) executedCases / data.getTotalCases() * 100);
            data.setProgress(Math.min(progress, 100));
        }

        // Update estimated end time based on current progress
        if (data.getStartTime() != null && data.getTotalCases() != null && executedCases > 0) {
            long elapsedSeconds = java.time.Duration.between(data.getStartTime(), LocalDateTime.now()).getSeconds();
            long avgSecondsPerCase = elapsedSeconds / executedCases;
            long remainingCases = data.getTotalCases() - executedCases;
            LocalDateTime estimatedEnd = LocalDateTime.now().plusSeconds(remainingCases * avgSecondsPerCase);
            data.setEstimatedEndTime(estimatedEnd);
        }

        data.setTimestamp(LocalDateTime.now());

        log.debug("Progress updated for task {}: {}/{} cases executed", taskId, executedCases, data.getTotalCases());

        monitoringDataMapper.update(data);
        return data;
    }

    /**
     * Complete monitoring
     */
    public MonitoringData completeMonitoring(String taskId, boolean success) {
        MonitoringData data = getMonitoringDataByTaskId(taskId);

        data.setStatus(success ? MonitoringData.Status.COMPLETED.name() : MonitoringData.Status.FAILED.name());
        data.setActualEndTime(LocalDateTime.now());
        data.setProgress(100);
        data.setTimestamp(LocalDateTime.now());

        log.info("Completed monitoring for task: {} - Status: {}", taskId, data.getStatus());

        monitoringDataMapper.update(data);
        return data;
    }

    /**
     * Get recent monitoring data
     */
    public List<MonitoringData> getRecentMonitoringData() {
        return monitoringDataMapper.selectRecentData(20);
    }

    /**
     * Get monitoring data by environment
     */
    public List<MonitoringData> getMonitoringDataByEnvironment(String environment) {
        return monitoringDataMapper.selectByEnvironment(environment);
    }

    /**
     * Get running tasks
     */
    public List<MonitoringData> getRunningTasks() {
        return monitoringDataMapper.selectByStatus(MonitoringData.Status.RUNNING.name());
    }

    /**
     * Update resource usage metrics
     */
    public MonitoringData updateResourceUsage(String taskId, Map<String, Object> resourceUsage) {
        MonitoringData data = getMonitoringDataByTaskId(taskId);

        if (data.getResourceUsage() == null) {
            data.setResourceUsage(new HashMap<>());
        }

        data.getResourceUsage().putAll(resourceUsage);
        data.setTimestamp(LocalDateTime.now());

        monitoringDataMapper.update(data);
        return data;
    }

    /**
     * Update performance metrics
     */
    public MonitoringData updatePerformanceMetrics(String taskId, Map<String, Object> performanceMetrics) {
        MonitoringData data = getMonitoringDataByTaskId(taskId);

        if (data.getPerformanceMetrics() == null) {
            data.setPerformanceMetrics(new HashMap<>());
        }

        data.getPerformanceMetrics().putAll(performanceMetrics);
        data.setTimestamp(LocalDateTime.now());

        monitoringDataMapper.update(data);
        return data;
    }

    /**
     * Get dashboard statistics
     */
    public Map<String, Object> getDashboardStatistics() {
        Map<String, Object> stats = new HashMap<>();

        // Get all monitoring data
        List<MonitoringData> allData = getRecentMonitoringData();

        // Calculate statistics
        int totalTasks = allData.size();
        long runningTasks = allData.stream()
                .filter(d -> MonitoringData.Status.RUNNING.name().equals(d.getStatus()))
                .count();
        long completedTasks = allData.stream()
                .filter(d -> MonitoringData.Status.COMPLETED.name().equals(d.getStatus()))
                .count();
        long failedTasks = allData.stream()
                .filter(d -> MonitoringData.Status.FAILED.name().equals(d.getStatus()))
                .count();

        // Calculate overall pass rate
        double totalPassRate = allData.stream()
                .filter(d -> d.getExecutedCases() != null && d.getExecutedCases() > 0)
                .mapToDouble(MonitoringData::getPassRate)
                .average()
                .orElse(0.0);

        stats.put("totalTasks", totalTasks);
        stats.put("runningTasks", runningTasks);
        stats.put("completedTasks", completedTasks);
        stats.put("failedTasks", failedTasks);
        stats.put("averagePassRate", totalPassRate);

        return stats;
    }
}
