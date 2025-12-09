package com.synapsetest.testmanagement.model;

import com.synapsetest.testmanagement.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.*;
import java.util.List;

/**
 * 用户反馈实体类
 * 对应表: user_feedback
 * 用途: 记录用户对AI生成结果的反馈
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserFeedback extends BaseEntity {

    /**
     * 生成请求ID
     */
    @Size(max = 50, message = "Request ID must not exceed 50 characters")
    private String requestId;

    /**
     * 评分 (1-5)
     */
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must not exceed 5")
    private Integer rating;

    /**
     * 反馈类型
     */
    private String feedbackType;  // GENERATION, RECOMMENDATION, QUALITY

    /**
     * 评论
     */
    private String comments;

    /**
     * 接受的用例ID列表 (JSON数组)
     */
    private List<String> acceptedCases;

    /**
     * 拒绝的用例ID列表 (JSON数组)
     */
    private List<String> rejectedCases;

    /**
     * 用户ID
     */
    @Size(max = 50, message = "User ID must not exceed 50 characters")
    private String userId;

    public enum FeedbackType {
        GENERATION, RECOMMENDATION, QUALITY
    }
}
