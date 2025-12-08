package com.synapsetest.testmanagement.model;

import com.synapsetest.testmanagement.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * AIModel Model
 * Represents an AI model with security audit status
 * MyBatis POJO (removed MongoDB annotations)
 *
 * User Story 2: AI生成测试用例
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AIModel extends BaseEntity {

    @NotBlank(message = "AI model name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @NotBlank(message = "Version is required")
    @Size(max = 50, message = "Version must not exceed 50 characters")
    private String version;

    private String description;

    private String filePath;

    @NotBlank(message = "Security status is required")
    private String securityStatus; // PENDING, IN_REVIEW, APPROVED, REJECTED

    private String vulnerabilityScanResult;

    private LocalDateTime lastScanTime;

    @NotBlank(message = "Compliance status is required")
    private String complianceStatus; // COMPLIANT, NON_COMPLIANT, PENDING

    private Map<String, Object> metrics; // Model performance metrics

    public enum SecurityStatus {
        PENDING, IN_REVIEW, APPROVED, REJECTED
    }

    public enum ComplianceStatus {
        COMPLIANT, NON_COMPLIANT, PENDING
    }
}
