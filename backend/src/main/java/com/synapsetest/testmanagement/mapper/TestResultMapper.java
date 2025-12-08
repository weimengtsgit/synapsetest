package com.synapsetest.testmanagement.mapper;

import com.synapsetest.testmanagement.model.TestResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * TestResult MyBatis Mapper接口
 * 处理质量报告中的测试结果数据
 */
@Mapper
public interface TestResultMapper {

    /**
     * 插入测试结果
     */
    int insert(TestResult testResult);

    /**
     * 批量插入测试结果
     */
    int insertBatch(@Param("testResults") List<TestResult> testResults);

    /**
     * 根据报告ID查询测试结果列表
     */
    List<TestResult> selectByReportId(String reportId);

    /**
     * 根据报告ID和状态查询
     */
    List<TestResult> selectByReportIdAndStatus(
            @Param("reportId") String reportId,
            @Param("status") String status);

    /**
     * 更新测试结果
     */
    int update(TestResult testResult);

    /**
     * 根据报告ID删除测试结果
     */
    int deleteByReportId(String reportId);

    /**
     * 批量删除测试结果
     */
    int deleteByReportIds(@Param("reportIds") List<String> reportIds);
}