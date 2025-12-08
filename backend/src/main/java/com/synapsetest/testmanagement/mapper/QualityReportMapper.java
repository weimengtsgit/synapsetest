package com.synapsetest.testmanagement.mapper;

import com.synapsetest.testmanagement.model.QualityReport;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * QualityReport MyBatis Mapper接口
 * 替代MongoDB Repository，提供MySQL数据访问
 */
@Mapper
public interface QualityReportMapper {

    /**
     * 插入质量报告
     */
    int insert(QualityReport qualityReport);

    /**
     * 根据ID查询质量报告
     */
    QualityReport selectById(String id);

    /**
     * 根据任务ID查询质量报告
     */
    List<QualityReport> selectByTaskId(String taskId);

    /**
     * 根据状态查询
     */
    List<QualityReport> selectByStatus(String status);

    /**
     * 根据生成时间范围查询
     */
    List<QualityReport> selectByGeneratedTimeRange(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 获取最新的10条质量报告
     */
    List<QualityReport> selectLatest10();

    /**
     * 更新质量报告
     */
    int update(QualityReport qualityReport);

    /**
     * 更新状态
     */
    int updateStatus(@Param("id") String id, @Param("status") String status);

    /**
     * 更新摘要
     */
    int updateSummary(@Param("id") String id, @Param("summary") String summary);

    /**
     * 删除质量报告
     */
    int deleteById(String id);

    /**
     * 根据任务ID删除
     */
    int deleteByTaskId(String taskId);

    /**
     * 批量删除
     */
    int deleteByIds(@Param("ids") List<String> ids);
}