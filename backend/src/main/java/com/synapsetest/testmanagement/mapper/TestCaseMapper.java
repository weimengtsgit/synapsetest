package com.synapsetest.testmanagement.mapper;

import com.synapsetest.testmanagement.model.TestCase;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * TestCase MyBatis Mapper
 * SQL statements are defined in TestCaseMapper.xml
 */
@Mapper
public interface TestCaseMapper {

    TestCase selectById(String id);

    List<TestCase> selectAll();

    List<TestCase> selectByType(String type);

    List<TestCase> selectByStatus(String status);

    List<TestCase> selectByRelatedRequirement(String relatedRequirement);

    List<TestCase> selectByCreatedBy(String createdBy);

    List<TestCase> selectByStatusOrderByPriorityDesc(String status);

    int insert(TestCase testCase);

    int update(TestCase testCase);

    int deleteById(String id);

    int count();

    /**
     * Find the maximum case number with the given prefix (e.g., "TC20251215")
     * Returns the full case_number or null if none exists
     * 
     * @param prefix The prefix to search for (e.g., "TC20251215")
     * @return The maximum case_number with that prefix, or null if none exists
     */
    String findMaxCaseNumberByPrefix(String prefix);
}

