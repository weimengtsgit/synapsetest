package com.synapsetest.testmanagement.mapper;

import com.synapsetest.testmanagement.model.TaskTestCase;
import com.synapsetest.testmanagement.model.TestCase;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * TaskTestCaseMapper
 * MyBatis mapper for task-testcase association management
 * 
 * Corresponding XML: mapper/TaskTestCaseMapper.xml
 */
@Mapper
public interface TaskTestCaseMapper {
    
    /**
     * Batch insert task-testcase associations
     * 
     * @param associations List of task-testcase associations to insert
     */
    void batchInsert(@Param("associations") List<TaskTestCase> associations);
    
    /**
     * Get test case IDs associated with a task
     * 
     * @param taskId Task ID
     * @return List of test case IDs ordered by execution order
     */
    List<String> selectTestCaseIdsByTaskId(@Param("taskId") String taskId);
    
    /**
     * Get test case details associated with a task (with execution order)
     * 
     * @param taskId Task ID
     * @return List of test cases with execution order
     */
    List<TestCase> selectTestCasesByTaskId(@Param("taskId") String taskId);
    
    /**
     * Delete all test case associations for a task
     * 
     * @param taskId Task ID
     */
    void deleteByTaskId(@Param("taskId") String taskId);
    
    /**
     * Update execution order for a specific task-testcase association
     * 
     * @param taskId Task ID
     * @param testCaseId Test case ID
     * @param executionOrder New execution order
     */
    void updateExecutionOrder(
        @Param("taskId") String taskId, 
        @Param("testCaseId") String testCaseId, 
        @Param("executionOrder") Integer executionOrder
    );
    
    /**
     * Count test cases associated with a task
     * 
     * @param taskId Task ID
     * @return Number of associated test cases
     */
    int countByTaskId(@Param("taskId") String taskId);
}
