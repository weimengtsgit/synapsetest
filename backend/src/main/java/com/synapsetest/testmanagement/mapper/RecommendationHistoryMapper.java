package com.synapsetest.testmanagement.mapper;

import com.synapsetest.testmanagement.model.RecommendationHistory;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 策略推荐历史Mapper
 */
@Mapper
public interface RecommendationHistoryMapper {

    /**
     * 插入推荐历史记录
     */
    @Insert("INSERT INTO recommendation_history " +
            "(id, task_id, environment_id, version_id, recommendation, risk_assessment, " +
            "context, created_at, updated_at) " +
            "VALUES (#{id}, #{taskId}, #{environmentId}, #{versionId}, #{recommendation}, " +
            "#{riskAssessment}, #{context}, #{createdAt}, #{updatedAt})")
    int insert(RecommendationHistory history);

    /**
     * 根据任务ID查询
     */
    @Select("SELECT * FROM recommendation_history WHERE task_id = #{taskId} ORDER BY created_at DESC")
    List<RecommendationHistory> findByTaskId(String taskId);

    /**
     * 根据环境ID查询
     */
    @Select("SELECT * FROM recommendation_history WHERE environment_id = #{environmentId} ORDER BY created_at DESC")
    List<RecommendationHistory> findByEnvironmentId(String environmentId);

    /**
     * 根据版本ID查询
     */
    @Select("SELECT * FROM recommendation_history WHERE version_id = #{versionId} ORDER BY created_at DESC")
    List<RecommendationHistory> findByVersionId(String versionId);

    /**
     * 查询所有记录
     */
    @Select("SELECT * FROM recommendation_history ORDER BY created_at DESC")
    List<RecommendationHistory> findAll();

    /**
     * 根据ID删除
     */
    @Delete("DELETE FROM recommendation_history WHERE id = #{id}")
    int deleteById(String id);
}
