package com.synapsetest.testmanagement.mapper;

import com.synapsetest.testmanagement.model.TestEnvironment;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * TestEnvironment MyBatis Mapper
 * SQL statements are defined in TestEnvironmentMapper.xml
 */
@Mapper
public interface TestEnvironmentMapper {

    TestEnvironment selectById(String id);

    List<TestEnvironment> selectAll();

    TestEnvironment selectByName(String name);

    List<TestEnvironment> selectByStatus(String status);

    int insert(TestEnvironment testEnvironment);

    int update(TestEnvironment testEnvironment);

    int deleteById(String id);

    int count();
}

