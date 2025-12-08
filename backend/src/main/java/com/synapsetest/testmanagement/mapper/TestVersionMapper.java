package com.synapsetest.testmanagement.mapper;

import com.synapsetest.testmanagement.model.TestVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * TestVersion MyBatis Mapper
 * SQL statements are defined in TestVersionMapper.xml
 */
@Mapper
public interface TestVersionMapper {

    TestVersion selectById(String id);

    List<TestVersion> selectAll();

    TestVersion selectByName(String name);

    List<TestVersion> selectByProductVersion(String productVersion);

    TestVersion selectByProductVersionAndName(@Param("productVersion") String productVersion, 
                                              @Param("name") String name);

    int insert(TestVersion testVersion);

    int update(TestVersion testVersion);

    int deleteById(String id);

    int count();
}

