package com.synapsetest.testmanagement.mapper;

import com.synapsetest.testmanagement.model.TestCase;
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
 * Mapper层单元测试 - TestCaseMapper
 *
 * 测试目标：
 * 1. 验证测试用例的CRUD操作
 * 2. 验证复杂查询（类型查询、状态查询）
 * 3. 验证数据持久化的正确性
 *
 * 重点测试场景：
 * - CRUD 操作的正确性
 * - 查询条件的准确性
 * - 数据完整性
 */
@SpringBootTest
@Transactional
@DisplayName("TestCaseMapper数据访问层测试")
public class TestCaseMapperTest {

    @Autowired
    private TestCaseMapper testCaseMapper;

    @Test
    @DisplayName("Mapper测试1: 插入测试用例")
    @Sql(scripts = "/test-data-mapper.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void insert_WithValidTestCase_ShouldPersistCorrectly() {
        // Given: 准备测试用例数据
        TestCase testCase = new TestCase();
        testCase.setId(UUID.randomUUID().toString());
        testCase.setTitle("登录功能-正常流程");
        testCase.setDescription("验证用户正常登录流程");
        testCase.setPriority(8);  // HIGH priority
        testCase.setType("FUNCTIONAL");
        testCase.setStatus("DRAFT");
        
        // 设置测试步骤
        List<String> steps = List.of(
            "打开登录页面",
            "输入用户名密码",
            "点击登录按钮"
        );
        testCase.setSteps(steps);
        testCase.setExpectedResult("成功跳转到首页");
        
        testCase.setTags(List.of("登录", "核心功能"));
        testCase.setCreatedBy("test-user");
        testCase.setCreatedAt(LocalDateTime.now());
        testCase.setUpdatedAt(LocalDateTime.now());

        // When: 执行插入操作
        int affectedRows = testCaseMapper.insert(testCase);

        // Then: 验证插入成功
        assertEquals(1, affectedRows);
        assertNotNull(testCase.getId());

        // 验证可以查询到
        TestCase inserted = testCaseMapper.selectById(testCase.getId());
        assertNotNull(inserted);
        assertEquals("登录功能-正常流程", inserted.getTitle());
        assertEquals(8, inserted.getPriority());
    }

    @Test
    @DisplayName("Mapper测试2: 根据ID查询测试用例")
    @Sql(scripts = "/test-data-mapper.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void selectById_WithExistingId_ShouldReturnTestCase() {
        // Given: 先插入一条数据
        TestCase testCase = createSimpleTestCase("查询测试用例");
        testCaseMapper.insert(testCase);
        String id = testCase.getId();

        // When: 根据ID查询
        TestCase found = testCaseMapper.selectById(id);

        // Then: 验证查询结果
        assertNotNull(found);
        assertEquals(id, found.getId());
        assertEquals("查询测试用例", found.getTitle());
    }

    @Test
    @DisplayName("Mapper测试3: 根据类型查询测试用例")
    @Sql(scripts = "/test-data-mapper.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void selectByType_WithValidType_ShouldReturnList() {
        // Given: 插入不同类型的测试用例
        TestCase functionalCase = createSimpleTestCase("功能测试用例");
        functionalCase.setType("FUNCTIONAL");
        testCaseMapper.insert(functionalCase);

        TestCase performanceCase = createSimpleTestCase("性能测试用例");
        performanceCase.setType("PERFORMANCE");
        testCaseMapper.insert(performanceCase);

        // When: 根据类型查询
        List<TestCase> functionalCases = testCaseMapper.selectByType("FUNCTIONAL");

        // Then: 验证查询结果
        assertNotNull(functionalCases);
        assertTrue(functionalCases.size() > 0);
        functionalCases.forEach(testCase -> assertEquals("FUNCTIONAL", testCase.getType()));
    }

    @Test
    @DisplayName("Mapper测试4: 根据状态查询测试用例")
    @Sql(scripts = "/test-data-mapper.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void selectByStatus_WithValidStatus_ShouldReturnList() {
        // Given: 插入不同状态的测试用例
        TestCase draftCase = createSimpleTestCase("草稿用例");
        draftCase.setStatus("DRAFT");
        testCaseMapper.insert(draftCase);

        TestCase approvedCase = createSimpleTestCase("已批准用例");
        approvedCase.setStatus("APPROVED");
        testCaseMapper.insert(approvedCase);

        // When: 根据状态查询
        List<TestCase> draftCases = testCaseMapper.selectByStatus("DRAFT");

        // Then: 验证查询结果
        assertNotNull(draftCases);
        assertTrue(draftCases.size() > 0);
        draftCases.forEach(testCase -> assertEquals("DRAFT", testCase.getStatus()));
    }

    @Test
    @DisplayName("Mapper测试5: 更新测试用例")
    @Sql(scripts = "/test-data-mapper.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void update_WithValidData_ShouldUpdateSuccessfully() {
        // Given: 插入一条数据
        TestCase testCase = createSimpleTestCase("待更新用例");
        testCaseMapper.insert(testCase);
        String id = testCase.getId();

        // When: 更新状态和优先级
        TestCase toUpdate = testCaseMapper.selectById(id);
        toUpdate.setStatus("APPROVED");
        toUpdate.setPriority(9);
        toUpdate.setUpdatedAt(LocalDateTime.now());
        int affectedRows = testCaseMapper.update(toUpdate);

        // Then: 验证更新成功
        assertEquals(1, affectedRows);

        TestCase updated = testCaseMapper.selectById(id);
        assertEquals("APPROVED", updated.getStatus());
        assertEquals(9, updated.getPriority());
    }

    @Test
    @DisplayName("Mapper测试6: 根据优先级排序查询")
    @Sql(scripts = "/test-data-mapper.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void selectByStatusOrderByPriorityDesc_ShouldReturnSortedList() {
        // Given: 插入不同优先级的用例
        TestCase lowPriority = createSimpleTestCase("低优先级用例");
        lowPriority.setPriority(3);
        lowPriority.setStatus("DRAFT");
        testCaseMapper.insert(lowPriority);

        TestCase highPriority = createSimpleTestCase("高优先级用例");
        highPriority.setPriority(9);
        highPriority.setStatus("DRAFT");
        testCaseMapper.insert(highPriority);

        // When: 按优先级倒序查询
        List<TestCase> cases = testCaseMapper.selectByStatusOrderByPriorityDesc("DRAFT");

        // Then: 验证排序正确
        assertNotNull(cases);
        assertTrue(cases.size() >= 2);
        // 验证优先级降序
        for (int i = 0; i < cases.size() - 1; i++) {
            assertTrue(cases.get(i).getPriority() >= cases.get(i + 1).getPriority());
        }
    }

    @Test
    @DisplayName("Mapper测试7: 删除测试用例")
    @Sql(scripts = "/test-data-mapper.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void deleteById_WithValidId_ShouldDeleteSuccessfully() {
        // Given: 插入一条数据
        TestCase testCase = createSimpleTestCase("待删除用例");
        testCaseMapper.insert(testCase);
        String id = testCase.getId();

        // When: 删除数据
        int affectedRows = testCaseMapper.deleteById(id);

        // Then: 验证删除成功
        assertEquals(1, affectedRows);

        TestCase deleted = testCaseMapper.selectById(id);
        assertNull(deleted);
    }

    @Test
    @DisplayName("Mapper测试8: 统计测试用例数量")
    @Sql(scripts = "/test-data-mapper.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void count_ShouldReturnCorrectCount() {
        // Given: 插入多条数据
        for (int i = 0; i < 5; i++) {
            TestCase testCase = createSimpleTestCase("测试用例" + i);
            testCaseMapper.insert(testCase);
        }

        // When: 统计数量
        int count = testCaseMapper.count();

        // Then: 验证统计结果
        assertTrue(count >= 5);
    }

    @Test
    @DisplayName("Mapper测试9: 查询所有测试用例")
    @Sql(scripts = "/test-data-mapper.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void selectAll_ShouldReturnAllTestCases() {
        // Given: 插入测试数据
        TestCase testCase1 = createSimpleTestCase("用例1");
        testCaseMapper.insert(testCase1);

        TestCase testCase2 = createSimpleTestCase("用例2");
        testCaseMapper.insert(testCase2);

        // When: 查询所有
        List<TestCase> allCases = testCaseMapper.selectAll();

        // Then: 验证查询结果
        assertNotNull(allCases);
        assertTrue(allCases.size() >= 2);
    }

    @Test
    @DisplayName("Mapper测试10: 根据创建者查询测试用例")
    @Sql(scripts = "/test-data-mapper.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void selectByCreatedBy_WithValidUser_ShouldReturnList() {
        // Given: 插入不同创建者的用例
        TestCase case1 = createSimpleTestCase("张三的用例");
        case1.setCreatedBy("zhangsan");
        testCaseMapper.insert(case1);

        TestCase case2 = createSimpleTestCase("李四的用例");
        case2.setCreatedBy("lisi");
        testCaseMapper.insert(case2);

        // When: 根据创建者查询
        List<TestCase> cases = testCaseMapper.selectByCreatedBy("zhangsan");

        // Then: 验证查询结果
        assertNotNull(cases);
        assertTrue(cases.size() > 0);
        cases.forEach(testCase -> assertEquals("zhangsan", testCase.getCreatedBy()));
    }

    /**
     * 辅助方法：创建简单的测试用例
     */
    private TestCase createSimpleTestCase(String title) {
        TestCase testCase = new TestCase();
        testCase.setId(UUID.randomUUID().toString());
        testCase.setTitle(title);
        testCase.setDescription("描述: " + title);
        testCase.setPriority(5);
        testCase.setType("FUNCTIONAL");
        testCase.setStatus("DRAFT");
        testCase.setSteps(List.of("步骤1", "步骤2"));
        testCase.setExpectedResult("预期结果");
        testCase.setTags(List.of("测试"));
        testCase.setCreatedBy("test-user");
        testCase.setCreatedAt(LocalDateTime.now());
        testCase.setUpdatedAt(LocalDateTime.now());
        return testCase;
    }
}
