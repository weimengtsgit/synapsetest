package com.synapsetest.testmanagement.mapper;

import com.synapsetest.testmanagement.model.MonitoringData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * MonitoringData MyBatis Mapper接口
 * 替代MongoDB Repository，提供MySQL数据访问
 */
@Mapper
public interface MonitoringDataMapper {

    /**
     * 插入监控数据
     */
    int insert(MonitoringData monitoringData);

    /**
     * 根据ID查询监控数据
     */
    Optional<MonitoringData> selectById(String id);

    /**
     * 根据任务ID查询监控数据
     */
    Optional<MonitoringData> selectByTaskId(String taskId);

    /**
     * 根据任务ID列表批量查询
     */
    List<MonitoringData> selectByTaskIds(@Param("taskIds") List<String> taskIds);

    /**
     * 根据状态查询
     */
    List<MonitoringData> selectByStatus(String status);

    /**
     * 根据时间范围查询
     */
    List<MonitoringData> selectByTimeRange(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 获取最新的20条监控数据
     */
    List<MonitoringData> selectLatest20();

    /**
     * 更新监控数据
     */
    int update(MonitoringData monitoringData);

    /**
     * 查询最近的监控数据
     */
    List<MonitoringData> selectRecentData(@Param("limit") int limit);

    /**
     * 根据环境查询监控数据
     */
    List<MonitoringData> selectByEnvironment(String environment);

    /**
     * 更新进度信息
     */
    int updateProgress(
            @Param("taskId") String taskId,
            @Param("progress") int progress,
            @Param("executedCases") int executedCases,
            @Param("passedCases") int passedCases,
            @Param("failedCases") int failedCases,
            @Param("skippedCases") int skippedCases);

    /**
     * 更新状态
     */
    int updateStatus(@Param("taskId") String taskId, @Param("status") String status);

    /**
     * 删除监控数据
     */
    int deleteById(String id);

    /**
     * 根据任务ID删除
     */
    int deleteByTaskId(String taskId);

    /**
     * 根据时间范围删除历史数据
     */
    int deleteByTimeRange(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
}