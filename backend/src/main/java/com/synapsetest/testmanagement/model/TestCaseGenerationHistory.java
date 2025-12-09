package com.synapsetest.testmanagement.model;

import com.synapsetest.testmanagement.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.*;

/**
 * 测试用例生成历史实体类
 * 对应表: testcase_generation_history
 * 用途: 记录AI生成测试用例的历史
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TestCaseGenerationHistory extends BaseEntity {

    /**
     * 请求ID (兼容旧版)
     */
    @Size(max = 50, message = "Request ID must not exceed 50 characters")
    private String requestId;

    /**
     * 用户ID
     */
    @Size(max = 50, message = "User ID must not exceed 50 characters")
    private String userId;

    /**
     * 需求文本
     */
    private String requirementText;

    /**
     * 模块名称
     */
    @Size(max = 100, message = "Module name must not exceed 100 characters")
    private String module;

    /**
     * 请求生成数量
     */
    @Min(value = 0, message = "Requested cases count must be non-negative")
    private Integer numCasesRequested;

    /**
     * 实际生成数量
     */
    @Min(value = 0, message = "Generated cases count must be non-negative")
    private Integer numCasesGenerated;

    /**
     * 生成耗时(毫秒)
     */
    @Min(value = 0, message = "Generation time must be non-negative")
    private Long generationTimeMs;

    /**
     * LLM提供商
     */
    @Size(max = 50, message = "LLM provider name must not exceed 50 characters")
    private String llmProvider;

    /**
     * 模型名称
     */
    @Size(max = 100, message = "Model name must not exceed 100 characters")
    private String modelName;

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 错误信息
     */
    private String errorMessage;
}
