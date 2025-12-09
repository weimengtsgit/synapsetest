package com.synapsetest.testmanagement.model;

import com.synapsetest.testmanagement.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.*;

/**
 * 公司测试标准实体类
 * 对应表: company_standards
 * 用途: 存储公司测试标准和规范
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CompanyStandard extends BaseEntity {

    /**
     * 标准名称
     */
    @NotBlank(message = "Standard name is required")
    @Size(max = 200, message = "Standard name must not exceed 200 characters")
    private String name;

    /**
     * 标准描述
     */
    private String description;

    /**
     * 标准类别
     */
    @Size(max = 50, message = "Category must not exceed 50 characters")
    private String category;

    /**
     * 标准内容 (JSON格式)
     */
    @NotBlank(message = "Standard data is required")
    private String standardData;

    /**
     * 版本号
     */
    @Size(max = 50, message = "Version must not exceed 50 characters")
    private String version;

    /**
     * 状态
     */
    private String status;  // ACTIVE, DEPRECATED, DRAFT

    public enum StandardStatus {
        ACTIVE, DEPRECATED, DRAFT
    }
}
