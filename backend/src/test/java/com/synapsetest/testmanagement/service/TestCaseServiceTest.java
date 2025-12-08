package com.synapsetest.testmanagement.service;

import com.synapsetest.testmanagement.model.TestCase;
import com.synapsetest.testmanagement.dto.TestCaseRequest;
import com.synapsetest.testmanagement.dto.response.TestCaseResponse;
import com.synapsetest.testmanagement.exception.ValidationException;
import com.synapsetest.testmanagement.mapper.TestCaseMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 测试用例服务 - CRUD场景 (US2)
 * TDD测试用例
 */
@SpringBootTest
@DisplayName("测试用例服务 - CRUD场景")
class TestCaseServiceTest {

    @MockBean
    private TestCaseMapper testCaseMapper;

    @Autowired
    private TestCaseService testCaseService;

    @Test
    @DisplayName("场景1.1: 创建测试用例成功")
    void createTestCase_WithValidInput_ShouldReturnTestCase() {
        // Given: 有效的测试用例请求
        TestCaseRequest request = new TestCaseRequest();
        request.setTitle("验证用户使用正确密码登录成功");
        request.setDescription("测试正常登录流程");
        request.setSteps(Arrays.asList(
            "打开登录页面",
            "输入有效用户名: testuser",
            "输入正确密码: Password123",
            "点击登录按钮"
        ));
        request.setExpectedResults("成功登录并跳转到首页，显示欢迎信息");
        request.setType("FUNCTIONAL");
        request.setPriority(8);
        request.setTags(Arrays.asList("login", "smoke", "p0"));

        when(testCaseMapper.insert(any())).thenReturn(1);

        // When
        TestCaseResponse response = testCaseService.createTestCase(request, "qa_engineer");

        // Then
        assertNotNull(response.getId());
        assertEquals("验证用户使用正确密码登录成功", response.getTitle());
        assertEquals("DRAFT", response.getStatus());
        assertEquals(4, response.getSteps().size());
        assertEquals("qa_engineer", response.getCreatedBy());
    }

    @Test
    @DisplayName("场景1.2: 测试步骤为空时创建失败")
    void createTestCase_WithEmptySteps_ShouldThrowException() {
        // Given
        TestCaseRequest request = new TestCaseRequest();
        request.setTitle("测试用例");
        request.setSteps(Collections.emptyList()); // 空步骤

        // When & Then
        assertThrows(ValidationException.class,
            () -> testCaseService.createTestCase(request, "qa_engineer"));
    }

    @Test
    @DisplayName("场景1.3: 更新测试用例成功")
    void updateTestCase_ShouldUpdateFields() {
        // Given
        TestCase existingCase = createTestCase("case-001");
        when(testCaseMapper.selectById("case-001")).thenReturn(existingCase);
        when(testCaseMapper.update(any())).thenReturn(1);

        TestCaseRequest updateRequest = new TestCaseRequest();
        updateRequest.setTitle("更新后的标题");
        updateRequest.setSteps(Arrays.asList("新步骤1", "新步骤2"));

        // When
        TestCaseResponse response = testCaseService.updateTestCase("case-001", updateRequest);

        // Then
        assertEquals("更新后的标题", response.getTitle());
        assertNotNull(response.getUpdatedAt());
    }

    @Test
    @DisplayName("场景1.4: 审批测试用例状态变更")
    void approveTestCase_ShouldUpdateStatusToApproved() {
        // Given
        TestCase draftCase = createTestCase("case-001");
        draftCase.setStatus("DRAFT");
        when(testCaseMapper.selectById("case-001")).thenReturn(draftCase);
        when(testCaseMapper.update(any())).thenReturn(1);

        // When
        TestCaseResponse response = testCaseService.approveTestCase("case-001");

        // Then
        assertEquals("APPROVED", response.getStatus());
    }

    // Helper methods
    private TestCase createTestCase(String id) {
        TestCase testCase = new TestCase();
        testCase.setId(id);
        testCase.setTitle("测试用例标题");
        testCase.setSteps(Arrays.asList("步骤1", "步骤2"));
        testCase.setExpectedResult("预期结果");
        testCase.setType("FUNCTIONAL");
        testCase.setStatus("DRAFT");
        testCase.setPriority(5);
        testCase.setCreatedBy("tester");
        testCase.setCreatedAt(LocalDateTime.now());
        testCase.setUpdatedAt(LocalDateTime.now());
        return testCase;
    }
}
