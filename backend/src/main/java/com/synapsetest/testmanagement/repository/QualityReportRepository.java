package com.synapsetest.testmanagement.repository;

import com.synapsetest.testmanagement.model.QualityReport;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * QualityReport Repository (MySQL/MyBatis)
 */
@Mapper
@Repository
public interface QualityReportRepository {

    Optional<QualityReport> findByTaskId(String taskId);

    List<QualityReport> findByStatus(String status);

    List<QualityReport> findByGeneratedAtBetween(LocalDateTime start, LocalDateTime end);

    List<QualityReport> findTop10ByOrderByGeneratedAtDesc();
    
    QualityReport save(QualityReport qualityReport);
    
    QualityReport update(QualityReport qualityReport);
    
    void deleteById(String id);
    
    Optional<QualityReport> findById(String id);
    
    void deleteAll();
}
