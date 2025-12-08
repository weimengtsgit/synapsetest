package com.synapsetest.testmanagement.mapper;

import com.synapsetest.testmanagement.model.MonitoringData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Mapper层单元测试 - MonitoringDataMapper
 *
 * 测试目标：
 * 1. 验证监控数据的写入和查询
 * 2. 验证时间范围查询的准确性
 * 3. 验证按任务ID、状态的查询
 * 4. 验证实时数据和历史数据的查询
 *
 * 重点测试场景：
 * - 时间序列数据的存储
 * - 按时间范围的高效查询
 * - 统计聚合（成功率、平均执行时间等）
 */
@SpringBootTest
@Transactional
@DisplayName("MonitoringDataMapper数据访问层测试")
public class MonitoringDataMapperTest {

    @Autowired
    private MonitoringDataMapper monitoringDataMapper;

    @Test
    @DisplayName("Mapper测试1: 插入监控数据")
    @Sql(scripts = "/test-data-mapper.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void insert_WithValidMonitoringData_ShouldPersist() {
        // Given: 准备监控数据
        MonitoringData data = createMonitoringData("task-001", "RUNNING", 50);

        // When: 执行插入
        int affectedRows = monitoringDataMapper.insert(data);

        // Then: 验证插入成功
        assertEquals(1, affectedRows);
        assertNotNull(data.getId());
        
        // 验证可以查询到
        MonitoringData found = monitoringDataMapper.selectById(data.getId());
        assertNotNull(found);
        assertEquals("task-001", found.getTaskId());
        assertEquals("RUNNING", found.getStatus());
        assertEquals(50, found.getProgress());
    }

    @Test
    @DisplayName("Mapper测试2: 根据任务ID查询监控数据")
    @Sql(scripts = "/test-data-mapper.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void selectByTaskId_WithValidTaskId_ShouldReturnData() {
        // Given: 插入测试数据
        MonitoringData data = createMonitoringData("task-001", "RUNNING", 50);
        monitoringDataMapper.insert(data);

        // When: 根据任务ID查询
        List<MonitoringData> found = monitoringDataMapper.selectByTaskId("task-001");

        // Then: 验证查询结果
        assertNotNull(found);
        assertFalse(found.isEmpty());
        assertEquals("task-001", found.get(0).getTaskId());
        assertEquals("RUNNING", found.get(0).getStatus());
    }

    @Test
    @DisplayName("Mapper测试3: 根据状态查询监控数据")
    @Sql(scripts = "/test-data-mapper.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void selectByStatus_WithValidStatus_ShouldReturnList() {
        // Given: 插入多条不同状态的数据
        monitoringDataMapper.insert(createMonitoringData("task-001", "RUNNING", 50));
        monitoringDataMapper.insert(createMonitoringData("task-002", "RUNNING", 60));
        monitoringDataMapper.insert(createMonitoringData("task-003", "COMPLETED", 100));

        // When: 根据状态查询
        List<MonitoringData> runningTasks = monitoringDataMapper.selectByStatus("RUNNING");

        // Then: 验证查询结果
        assertNotNull(runningTasks);
        assertTrue(runningTasks.size() >= 2);
        runningTasks.forEach(task -> assertEquals("RUNNING", task.getStatus()));
    }

    @Test
    @DisplayName("Mapper测试4: 根据时间范围查询监控数据")
    @Sql(scripts = "/test-data-mapper.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void selectByTimestampBetween_WithValidRange_ShouldReturnFilteredData() {
        // Given: 插入不同时间的数据
        LocalDateTime now = LocalDateTime.now();
        
        MonitoringData data1 = createMonitoringData("task-001", "RUNNING", 50);
        data1.setTimestamp(now.minusHours(2));
        monitoringDataMapper.insert(data1);

        MonitoringData data2 = createMonitoringData("task-002", "RUNNING", 60);
        data2.setTimestamp(now.minusHours(1));
        monitoringDataMapper.insert(data2);

        MonitoringData data3 = createMonitoringData("task-003", "COMPLETED", 100);
        data3.setTimestamp(now.minusHours(5));
        monitoringDataMapper.insert(data3);

        // When: 查询最近3小时的数据
        LocalDateTime startTime = now.minusHours(3);
        LocalDateTime endTime = now;
        List<MonitoringData> recentData = monitoringDataMapper.selectByTimestampBetween(startTime, endTime);

        // Then: 验证查询结果
        assertNotNull(recentData);
        assertTrue(recentData.size() >= 2);
        recentData.forEach(data -> {
            assertTrue(data.getTimestamp().isAfter(startTime) || data.getTimestamp().isEqual(startTime));
            assertTrue(data.getTimestamp().isBefore(endTime) || data.getTimestamp().isEqual(endTime));
        });
    }

    @Test
    @DisplayName("Mapper测试5: 根据环境查询监控数据")
    @Sql(scripts = "/test-data-mapper.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void selectByEnvironment_WithValidEnvironment_ShouldReturnList() {
        // Given: 插入不同环境的数据
        MonitoringData devData = createMonitoringData("task-001", "RUNNING", 50);
        devData.setEnvironment("DEV");
        monitoringDataMapper.insert(devData);

        MonitoringData testData = createMonitoringData("task-002", "RUNNING", 60);
        testData.setEnvironment("TEST");
        monitoringDataMapper.insert(testData);

        // When: 查询DEV环境的数据
        List<MonitoringData> devMonitoring = monitoringDataMapper.selectByEnvironment("DEV");

        // Then: 验证查询结果
        assertNotNull(devMonitoring);
        assertTrue(devMonitoring.size() >= 1);
        devMonitoring.forEach(data -> assertEquals("DEV", data.getEnvironment()));
    }

    @Test
    @DisplayName("Mapper测试6: 查询最新的监控数据")
    @Sql(scripts = "/test-data-mapper.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void selectLatestByTaskId_ShouldReturnMostRecentData() {
        // Given: 为同一任务插入多条数据
        String taskId = "task-001";
        LocalDateTime now = LocalDateTime.now();
        
        MonitoringData data1 = createMonitoringData(taskId, "RUNNING", 30);
        data1.setTimestamp(now.minusMinutes(10));
        monitoringDataMapper.insert(data1);

        MonitoringData data2 = createMonitoringData(taskId, "RUNNING", 60);
        data2.setTimestamp(now.minusMinutes(5));
        monitoringDataMapper.insert(data2);

        MonitoringData data3 = createMonitoringData(taskId, "RUNNING", 90);
        data3.setTimestamp(now.minusMinutes(1));
        monitoringDataMapper.insert(data3);

        // When: 查询最新数据
        MonitoringData latest = monitoringDataMapper.selectLatestByTaskId(taskId);

        // Then: 验证查询结果
        assertNotNull(latest);
        assertEquals(taskId, latest.getTaskId());
        assertEquals(90, latest.getProgress());
    }

    @Test
    @DisplayName("Mapper测试7: 更新监控数据")
    @Sql(scripts = "/test-data-mapper.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void update_WithValidData_ShouldUpdateSuccessfully() {
        // Given: 插入初始数据
        MonitoringData data = createMonitoringData("task-001", "RUNNING", 50);
        monitoringDataMapper.insert(data);
        String id = data.getId();

        // When: 更新进度
        MonitoringData toUpdate = monitoringDataMapper.selectById(id);
        assertNotNull(toUpdate);
        toUpdate.setProgress(80);
        toUpdate.setExecutedCases(80);
        toUpdate.setPassedCases(75);
        toUpdate.setUpdatedAt(LocalDateTime.now());
        int affectedRows = monitoringDataMapper.update(toUpdate);

        // Then: 验证更新成功
        assertEquals(1, affectedRows);
        MonitoringData updated = monitoringDataMapper.selectById(id);
        assertEquals(80, updated.getProgress());
        assertEquals(80, updated.getExecutedCases());
        assertEquals(75, updated.getPassedCases());
    }

    @Test
    @DisplayName("Mapper测试8: 删除监控数据")
    @Sql(scripts = "/test-data-mapper.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void deleteById_WithValidId_ShouldDeleteSuccessfully() {
        // Given: 插入测试数据
        MonitoringData data = createMonitoringData("task-001", "COMPLETED", 100);
        monitoringDataMapper.insert(data);
        String id = data.getId();

        // When: 删除数据
        int affectedRows = monitoringDataMapper.deleteById(id);

        // Then: 验证删除成功
        assertEquals(1, affectedRows);
        MonitoringData deleted = monitoringDataMapper.selectById(id);
        assertNull(deleted);
    }

    @Test
    @DisplayName("Mapper测试9: 根据任务ID删除所有监控数据")
    @Sql(scripts = "/test-data-mapper.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void deleteByTaskId_WithValidTaskId_ShouldDeleteAll() {
        // Given: 插入多条相同任务ID的数据
        String taskId = "task-001";
        monitoringDataMapper.insert(createMonitoringData(taskId, "RUNNING", 30));
        monitoringDataMapper.insert(createMonitoringData(taskId, "RUNNING", 60));
        monitoringDataMapper.insert(createMonitoringData(taskId, "COMPLETED", 100));

        // When: 根据任务ID删除
        int affectedRows = monitoringDataMapper.deleteByTaskId(taskId);

        // Then: 验证删除成功
        assertTrue(affectedRows >= 3);
        List<MonitoringData> remaining = monitoringDataMapper.selectByTaskId(taskId);
        assertTrue(remaining.isEmpty());
    }

    @Test
    @DisplayName("Mapper测试10: 测试通过率计算")
    void calculatePassRate_ShouldReturnCorrectPercentage() {
        // Given: 创建包含用例执行数据的监控数据
        MonitoringData data = new MonitoringData();
        data.setTaskId("task-001");
        data.setExecutedCases(100);
        data.setPassedCases(85);
        data.setFailedCases(15);

        // When: 计算通过率
        Double passRate = data.getPassRate();

        // Then: 验证计算结果
        assertNotNull(passRate);
        assertEquals(85.0, passRate, 0.01);
    }

    @Test
    @DisplayName("Mapper测试11: 存储复杂的资源使用和性能指标数据")
    @Sql(scripts = "/test-data-mapper.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void save_WithComplexMetrics_ShouldPersistCorrectly() {
        // Given: 准备包含复杂指标的监控数据
        MonitoringData data = createMonitoringData("task-001", "RUNNING", 50);
        
        Map<String, Object> resourceUsage = new HashMap<>();
        resourceUsage.put("cpu_usage", 75.5);
        resourceUsage.put("memory_mb", 2048);
        resourceUsage.put("disk_io", "120MB/s");
        data.setResourceUsage(resourceUsage);

        Map<String, Object> performanceMetrics = new HashMap<>();
        performanceMetrics.put("avg_response_time_ms", 150);
        performanceMetrics.put("throughput_rps", 1000);
        performanceMetrics.put("error_rate", 0.05);
        data.setPerformanceMetrics(performanceMetrics);

        // When: 保存数据
        monitoringDataMapper.insert(data);

        // Then: 验证复杂字段正确保存
        MonitoringData saved = monitoringDataMapper.selectById(data.getId());
        assertNotNull(saved);
        assertNotNull(saved.getResourceUsage());
        assertEquals(75.5, saved.getResourceUsage().get("cpu_usage"));
        assertNotNull(saved.getPerformanceMetrics());
        assertEquals(150, saved.getPerformanceMetrics().get("avg_response_time_ms"));
    }

    @Test
    @DisplayName("Mapper测试12: 统计监控数据总数")
    @Sql(scripts = "/test-data-mapper.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void count_ShouldReturnCorrectCount() {
        // Given: 插入多条数据
        for (int i = 0; i < 5; i++) {
            monitoringDataMapper.insert(createMonitoringData("task-" + i, "RUNNING", 50));
        }

        // When: 统计数量
        int count = monitoringDataMapper.count();

        // Then: 验证统计结果
        assertTrue(count >= 5);
    }

    /**
     * 辅助方法：创建监控数据
     */
    private MonitoringData createMonitoringData(String taskId, String status, int progress) {
        MonitoringData data = new MonitoringData();
        data.setId(UUID.randomUUID().toString());
        data.setTaskId(taskId);
        data.setStatus(status);
        data.setProgress(progress);
        data.setExecutedCases(progress);
        data.setTotalCases(100);
        data.setPassedCases((int) (progress * 0.9));
        data.setFailedCases((int) (progress * 0.1));
        data.setSkippedCases(0);
        data.setStartTime(LocalDateTime.now());
        data.setTimestamp(LocalDateTime.now());
        data.setEnvironment("DEV");
        data.setVersion("v1.0.0");
        data.setCreatedAt(LocalDateTime.now());
        data.setUpdatedAt(LocalDateTime.now());
        return data;
    }
}
