package com.synapsetest.testmanagement.mapper;

import com.synapsetest.testmanagement.model.RiskAssessment;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * RiskAssessment MyBatis Mapper接口
 * 处理质量报告中的风险评估数据
 */
@Mapper
public interface RiskAssessmentMapper {

    /**
     * 插入风险评估数据
     */
    int insert(RiskAssessment riskAssessment);

    /**
     * 根据报告ID查询风险评估
     */
    RiskAssessment selectByReportId(String reportId);

    /**
     * 更新风险评估
     */
    int update(RiskAssessment riskAssessment);

    /**
     * 根据报告ID删除风险评估
     */
    int deleteByReportId(String reportId);

    /**
     * 批量删除风险评估
     */
    int deleteByReportIds(@Param("reportIds") List<String> reportIds);
}