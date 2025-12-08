package com.synapsetest.testmanagement.repository;

import com.synapsetest.testmanagement.model.MonitoringData;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * MonitoringData Repository (MySQL/MyBatis)
 */
@Mapper
@Repository
public interface MonitoringDataRepository {

    Optional<MonitoringData> findByTaskId(String taskId);

    List<MonitoringData> findByStatus(String status);

    List<MonitoringData> findByEnvironment(String environment);

    List<MonitoringData> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    List<MonitoringData> findTop20ByOrderByTimestampDesc();
    
    MonitoringData save(MonitoringData monitoringData);
    
    MonitoringData update(MonitoringData monitoringData);
    
    void deleteById(String id);
    
    Optional<MonitoringData> findById(String id);
    
    void deleteAll();
}
