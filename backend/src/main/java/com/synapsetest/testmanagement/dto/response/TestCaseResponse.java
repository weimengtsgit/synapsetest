package com.synapsetest.testmanagement.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for test case response (MyBatis version - using String for ID)
 */
@Data
public class TestCaseResponse {

    private String id;
    private String caseNumber;  // 业务编号(如TC20231215001)
    private String title;
    private String caseName;  // Alias for title, used in some API responses
    private String description;
    private List<String> steps;
    private String expectedResult;  // 统一使用单数,与TestCase模型和数据库schema一致
    private Integer priority;  // Priority level (0-10)
    private String type;
    private String status;
    private List<String> tags;
    private String relatedRequirement;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;

    // AI-related fields
    private String module;  // 所属模块
    private List<String> preconditions;  // 前置条件
    private Float qualityScore;  // AI质量评分 (0.0-1.0)
    private Boolean aiGenerated;  // 是否AI生成 (使用驼峰命名)
    private Float aiConfidence;  // AI置信度 (0.0-1.0,使用驼峰命名)
}

