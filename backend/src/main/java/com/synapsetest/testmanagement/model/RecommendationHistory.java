package com.synapsetest.testmanagement.model;

import com.synapsetest.testmanagement.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.*;

/**
 * 策略推荐历史实体类
 * 对应表: recommendation_history
 * 用途: 记录AI策略推荐的历史
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RecommendationHistory extends BaseEntity {

    /**
     * 任务ID
     */
    @Size(max = 50, message = "Task ID must not exceed 50 characters")
    private String taskId;

    /**
     * 环境ID
     */
    @Size(max = 36, message = "Environment ID must not exceed 36 characters")
    private String environmentId;

    /**
     * 版本ID
     */
    @Size(max = 36, message = "Version ID must not exceed 36 characters")
    private String versionId;

    /**
     * 推荐结果 (JSON格式)
     */
    @NotBlank(message = "Recommendation is required")
    private String recommendation;

    /**
     * 风险评估 (JSON格式)
     */
    private String riskAssessment;

    /**
     * 上下文信息 (JSON格式)
     */
    private String context;
}
