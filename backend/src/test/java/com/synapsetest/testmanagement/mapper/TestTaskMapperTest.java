package com.synapsetest.testmanagement.mapper;

import com.synapsetest.testmanagement.model.TestTask;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Mapper层单元测试 - TestTaskMapper
 *
 * 测试目标：
 * 1. 验证MyBatis Mapper的CRUD操作
 * 2. 验证SQL映射的正确性
 * 3. 验证数据库约束（唯一性、非空等）
 * 4. 验证复杂查询逻辑
 *
 * 技术栈：
 * - @SpringBootTest: 加载完整的Spring上下文
 * - @Transactional: 自动回滚测试数据
 * - @Sql: 执行SQL脚本初始化测试数据
 */
@SpringBootTest
@Transactional
@DisplayName("TestTaskMapper数据访问层测试")
public class TestTaskMapperTest {

    @Autowired
    private TestTaskMapper testTaskMapper;

    @Test
    @DisplayName("Mapper测试1: 插入测试任务成功")
    @Sql(scripts = "/test-data-mapper.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void insert_WithValidTestTask_ShouldReturnAffectedRows() {
        // Given: 准备测试任务数据
        TestTask testTask = new TestTask();
        testTask.setId(UUID.randomUUID().toString());
        testTask.setName("冒烟测试-订单模块");
        testTask.setDescription("验证订单模块的核心功能");
        testTask.setTestScope("SMOKE");
        testTask.setEnvironment("DEV");
        testTask.setVersion("v1.2.0");
        testTask.setCreatedBy("zhangsan");
        testTask.setStatus("PENDING");
        testTask.setPriority(8);  // HIGH priority as integer
        testTask.setCreatedAt(LocalDateTime.now());
        testTask.setUpdatedAt(LocalDateTime.now());

        // When: 执行插入操作
        int affectedRows = testTaskMapper.insert(testTask);

        // Then: 验证插入成功
        assertEquals(1, affectedRows, "应该插入1条记录");
        assertNotNull(testTask.getId(), "ID应该已设置");

        // 验证可以通过ID查询到刚插入的记录
        TestTask inserted = testTaskMapper.selectById(testTask.getId());
        assertNotNull(inserted);
        assertEquals("冒烟测试-订单模块", inserted.getName());
        assertEquals("SMOKE", inserted.getTestScope());
    }

    @Test
    @DisplayName("Mapper测试2: 根据ID查询测试任务")
    @Sql(scripts = "/test-data-mapper.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void selectById_WithExistingId_ShouldReturnTestTask() {
        // Given: 先插入一条数据
        TestTask testTask = createSimpleTestTask("查询测试任务");
        testTaskMapper.insert(testTask);
        String taskId = testTask.getId();

        // When: 根据ID查询
        TestTask found = testTaskMapper.selectById(taskId);

        // Then: 验证查询结果
        assertNotNull(found);
        assertEquals(taskId, found.getId());
        assertEquals("SMOKE", found.getTestScope());
    }

    @Test
    @DisplayName("Mapper测试3: 查询不存在的ID返回null")
    void selectById_WithNonExistingId_ShouldReturnNull() {
        // Given: 不存在的ID
        String nonExistingId = UUID.randomUUID().toString();

        // When: 根据ID查询
        TestTask testTask = testTaskMapper.selectById(nonExistingId);

        // Then: 应该返回null
        assertNull(testTask);
    }

    @Test
    @DisplayName("Mapper测试4: 更新测试任务状态")
    @Sql(scripts = "/test-data-mapper.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void update_WithValidData_ShouldUpdateSuccessfully() {
        // Given: 插入一条数据
        TestTask testTask = createSimpleTestTask("待更新任务");
        testTaskMapper.insert(testTask);
        String id = testTask.getId();

        // When: 更新状态
        TestTask toUpdate = testTaskMapper.selectById(id);
        assertNotNull(toUpdate);
        String originalStatus = toUpdate.getStatus();

        toUpdate.setStatus("RUNNING");
        toUpdate.setUpdatedAt(LocalDateTime.now());
        int affectedRows = testTaskMapper.update(toUpdate);

        // Then: 验证更新成功
        assertEquals(1, affectedRows);

        TestTask updated = testTaskMapper.selectById(id);
        assertEquals("RUNNING", updated.getStatus());
        assertNotEquals(originalStatus, updated.getStatus());
    }

    @Test
    @DisplayName("Mapper测试5: 根据状态查询任务列表")
    @Sql(scripts = "/test-data-mapper.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void selectByStatus_WithValidStatus_ShouldReturnList() {
        // Given: 插入多个不同状态的任务
        TestTask pendingTask1 = createSimpleTestTask("待执行任务1");
        pendingTask1.setStatus("PENDING");
        testTaskMapper.insert(pendingTask1);

        TestTask pendingTask2 = createSimpleTestTask("待执行任务2");
        pendingTask2.setStatus("PENDING");
        testTaskMapper.insert(pendingTask2);

        TestTask runningTask = createSimpleTestTask("运行中任务");
        runningTask.setStatus("RUNNING");
        testTaskMapper.insert(runningTask);

        // When: 根据状态查询
        List<TestTask> tasks = testTaskMapper.selectByStatus("PENDING");

        // Then: 验证查询结果
        assertNotNull(tasks);
        assertTrue(tasks.size() >= 2);
        tasks.forEach(task -> assertEquals("PENDING", task.getStatus()));
    }

    @Test
    @DisplayName("Mapper测试6: 根据创建者查询任务列表")
    @Sql(scripts = "/test-data-mapper.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void selectByCreatedBy_WithValidUser_ShouldReturnList() {
        // Given: 插入不同创建者的任务
        TestTask zhangsanTask = createSimpleTestTask("张三的任务");
        zhangsanTask.setCreatedBy("zhangsan");
        testTaskMapper.insert(zhangsanTask);

        TestTask lisiTask = createSimpleTestTask("李四的任务");
        lisiTask.setCreatedBy("lisi");
        testTaskMapper.insert(lisiTask);

        // When: 根据创建者查询
        List<TestTask> tasks = testTaskMapper.selectByCreatedBy("zhangsan");

        // Then: 验证查询结果
        assertNotNull(tasks);
        assertTrue(tasks.size() > 0);
        tasks.forEach(task -> assertEquals("zhangsan", task.getCreatedBy()));
    }

    @Test
    @DisplayName("Mapper测试7: 删除测试任务")
    @Sql(scripts = "/test-data-mapper.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void deleteById_WithValidId_ShouldDeleteSuccessfully() {
        // Given: 插入一条数据
        TestTask testTask = createSimpleTestTask("待删除任务");
        testTaskMapper.insert(testTask);
        String taskId = testTask.getId();

        // When: 执行删除
        int affectedRows = testTaskMapper.deleteById(taskId);

        // Then: 验证删除成功
        assertEquals(1, affectedRows);

        TestTask deleted = testTaskMapper.selectById(taskId);
        assertNull(deleted);
    }

    @Test
    @DisplayName("Mapper测试8: 根据环境查询任务")
    @Sql(scripts = "/test-data-mapper.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void selectByEnvironment_WithValidEnvironment_ShouldReturnList() {
        // Given: 插入不同环境的任务
        TestTask devTask = createSimpleTestTask("开发环境任务");
        devTask.setEnvironment("DEV");
        testTaskMapper.insert(devTask);

        TestTask testTask = createSimpleTestTask("测试环境任务");
        testTask.setEnvironment("TEST");
        testTaskMapper.insert(testTask);

        // When: 根据环境查询
        List<TestTask> tasks = testTaskMapper.selectByEnvironment("DEV");

        // Then: 验证查询结果
        assertNotNull(tasks);
        assertTrue(tasks.size() > 0);
        tasks.forEach(task -> assertEquals("DEV", task.getEnvironment()));
    }

    @Test
    @DisplayName("Mapper测试9: 按优先级倒序查询待执行任务")
    @Sql(scripts = "/test-data-mapper.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void selectByStatusOrderByPriorityDesc_ShouldReturnSortedList() {
        // Given: 插入不同优先级的待执行任务
        TestTask lowPriorityTask = createSimpleTestTask("低优先级任务");
        lowPriorityTask.setPriority(3);
        lowPriorityTask.setStatus("PENDING");
        testTaskMapper.insert(lowPriorityTask);

        TestTask highPriorityTask = createSimpleTestTask("高优先级任务");
        highPriorityTask.setPriority(9);
        highPriorityTask.setStatus("PENDING");
        testTaskMapper.insert(highPriorityTask);

        TestTask mediumPriorityTask = createSimpleTestTask("中优先级任务");
        mediumPriorityTask.setPriority(5);
        mediumPriorityTask.setStatus("PENDING");
        testTaskMapper.insert(mediumPriorityTask);

        // When: 查询待执行任务并按优先级倒序
        List<TestTask> tasks = testTaskMapper.selectByStatusOrderByPriorityDesc("PENDING");

        // Then: 验证查询结果按优先级降序排列
        assertNotNull(tasks);
        assertTrue(tasks.size() >= 3);
        for (int i = 0; i < tasks.size() - 1; i++) {
            assertTrue(tasks.get(i).getPriority() >= tasks.get(i + 1).getPriority());
        }
    }

    @Test
    @DisplayName("Mapper测试10: 统计任务总数")
    @Sql(scripts = "/test-data-mapper.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void count_ShouldReturnCorrectCount() {
        // Given: 插入多个任务
        for (int i = 0; i < 5; i++) {
            TestTask task = createSimpleTestTask("任务" + i);
            testTaskMapper.insert(task);
        }

        // When: 统计任务数量
        int count = testTaskMapper.count();

        // Then: 验证统计结果
        assertTrue(count >= 5);
    }

    @Test
    @DisplayName("Mapper测试11: 查询所有任务")
    @Sql(scripts = "/test-data-mapper.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void selectAll_ShouldReturnAllTasks() {
        // Given: 插入测试数据
        TestTask task1 = createSimpleTestTask("任务1");
        testTaskMapper.insert(task1);

        TestTask task2 = createSimpleTestTask("任务2");
        testTaskMapper.insert(task2);

        // When: 查询所有任务
        List<TestTask> allTasks = testTaskMapper.selectAll();

        // Then: 验证查询结果
        assertNotNull(allTasks);
        assertTrue(allTasks.size() >= 2);
    }

    /**
     * 辅助方法：创建简单的测试任务
     */
    private TestTask createSimpleTestTask(String name) {
        TestTask testTask = new TestTask();
        testTask.setId(UUID.randomUUID().toString());
        testTask.setName(name);
        testTask.setDescription("描述: " + name);
        testTask.setTestScope("SMOKE");
        testTask.setEnvironment("DEV");
        testTask.setVersion("v1.0.0");
        testTask.setStatus("PENDING");
        testTask.setPriority(5);
        testTask.setCreatedBy("test-user");
        testTask.setCreatedAt(LocalDateTime.now());
        testTask.setUpdatedAt(LocalDateTime.now());
        return testTask;
    }
}
