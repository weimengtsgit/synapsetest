package com.synapsetest.testmanagement.mapper;

import com.synapsetest.testmanagement.model.AIModel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * AIModel MyBatis Mapper接口
 * 替代MongoDB Repository，提供MySQL数据访问
 */
@Mapper
public interface AIModelMapper {

    /**
     * 插入AI模型
     */
    int insert(AIModel aiModel);

    /**
     * 根据ID查询AI模型
     */
    Optional<AIModel> selectById(String id);

    /**
     * 根据名称和版本查询AI模型
     */
    Optional<AIModel> selectByNameAndVersion(
            @Param("name") String name, 
            @Param("version") String version);

    /**
     * 根据名称查询所有版本
     */
    List<AIModel> selectByName(String name);

    /**
     * 根据安全状态查询
     */
    List<AIModel> selectBySecurityStatus(String securityStatus);

    /**
     * 根据合规状态查询
     */
    List<AIModel> selectByComplianceStatus(String complianceStatus);

    /**
     * 查询所有模型
     */
    List<AIModel> selectAll();

    /**
     * 根据创建时间范围查询
     */
    List<AIModel> selectByCreatedTimeRange(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 更新AI模型
     */
    int update(AIModel aiModel);

    /**
     * 更新安全状态
     */
    int updateSecurityStatus(
            @Param("id") String id, 
            @Param("securityStatus") String securityStatus, 
            @Param("lastScanTime") LocalDateTime lastScanTime);

    /**
     * 更新合规状态
     */
    int updateComplianceStatus(
            @Param("id") String id, 
            @Param("complianceStatus") String complianceStatus);

    /**
     * 删除AI模型
     */
    int deleteById(String id);

    /**
     * 批量删除
     */
    int deleteByIds(@Param("ids") List<String> ids);

    /**
     * 检查名称和版本是否已存在
     */
    int countByNameAndVersion(
            @Param("name") String name, 
            @Param("version") String version,
            @Param("excludeId") String excludeId);
}