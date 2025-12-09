package com.synapsetest.testmanagement.mapper;

import com.synapsetest.testmanagement.model.TestCaseGenerationHistory;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 测试用例生成历史Mapper
 */
@Mapper
public interface GenerationHistoryMapper {

    /**
     * 插入生成历史记录
     */
    @Insert("INSERT INTO testcase_generation_history " +
            "(id, request_id, user_id, requirement_text, module, num_cases_requested, " +
            "num_cases_generated, generation_time_ms, llm_provider, model_name, success, " +
            "error_message, created_at, updated_at) " +
            "VALUES (#{id}, #{requestId}, #{userId}, #{requirementText}, #{module}, " +
            "#{numCasesRequested}, #{numCasesGenerated}, #{generationTimeMs}, #{llmProvider}, " +
            "#{modelName}, #{success}, #{errorMessage}, #{createdAt}, #{updatedAt})")
    int insert(TestCaseGenerationHistory history);

    /**
     * 根据请求ID查询
     */
    @Select("SELECT * FROM testcase_generation_history WHERE request_id = #{requestId}")
    TestCaseGenerationHistory findByRequestId(String requestId);

    /**
     * 根据用户ID查询历史记录
     */
    @Select("SELECT * FROM testcase_generation_history " +
            "WHERE user_id = #{userId} " +
            "ORDER BY created_at DESC LIMIT #{limit}")
    List<TestCaseGenerationHistory> findByUserId(@Param("userId") String userId, @Param("limit") int limit);

    /**
     * 查询所有记录
     */
    @Select("SELECT * FROM testcase_generation_history ORDER BY created_at DESC")
    List<TestCaseGenerationHistory> findAll();

    /**
     * 根据ID删除
     */
    @Delete("DELETE FROM testcase_generation_history WHERE id = #{id}")
    int deleteById(String id);
}
