package com.synapsetest.testmanagement.mapper;

import com.synapsetest.testmanagement.model.TestTask;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * TestTask MyBatis Mapper
 * SQL statements are defined in TestTaskMapper.xml
 */
@Mapper
public interface TestTaskMapper {

    TestTask selectById(String id);

    List<TestTask> selectAll();

    List<TestTask> selectByStatus(String status);

    List<TestTask> selectByEnvironment(String environment);

    List<TestTask> selectByCreatedBy(String createdBy);

    List<TestTask> selectByStatusOrderByPriorityDesc(String status);

    int insert(TestTask testTask);

    int update(TestTask testTask);

    int deleteById(String id);

    int count();
}

