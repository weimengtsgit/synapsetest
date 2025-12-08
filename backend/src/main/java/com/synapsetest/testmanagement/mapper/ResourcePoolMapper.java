package com.synapsetest.testmanagement.mapper;

import com.synapsetest.testmanagement.model.ResourcePool;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * ResourcePool MyBatis Mapper
 * SQL statements are defined in ResourcePoolMapper.xml
 */
@Mapper
public interface ResourcePoolMapper {

    ResourcePool selectById(String id);

    List<ResourcePool> selectAll();

    ResourcePool selectByName(String name);

    List<ResourcePool> selectByStatus(String status);

    List<ResourcePool> selectByType(String type);

    List<ResourcePool> selectByStatusAndType(@Param("status") String status, @Param("type") String type);

    int insert(ResourcePool resourcePool);

    int update(ResourcePool resourcePool);

    int deleteById(String id);

    int count();
}

