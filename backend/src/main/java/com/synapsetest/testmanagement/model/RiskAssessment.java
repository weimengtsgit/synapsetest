package com.synapsetest.testmanagement.model;

import java.util.Map;
import java.util.List;

/**
 * 风险评估模型
 * 用于存储测试报告的风险评估信息
 */
public class RiskAssessment {
    private String reportId;
    private String overallRisk; // 总体风险等级: LOW, MEDIUM, HIGH, CRITICAL
    private Double riskScore;  // 风险分数 (0.0-1.0)
    private List<String> highRiskModules; // 高风险模块列表
    private List<String> recommendations; // 改进建议列表
    private Map<String, Double> moduleRiskScores; // 各模块风险分数

    // Getters and Setters
    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    public String getOverallRisk() {
        return overallRisk;
    }

    public void setOverallRisk(String overallRisk) {
        this.overallRisk = overallRisk;
    }

    public Double getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Double riskScore) {
        this.riskScore = riskScore;
    }

    public List<String> getHighRiskModules() {
        return highRiskModules;
    }

    public void setHighRiskModules(List<String> highRiskModules) {
        this.highRiskModules = highRiskModules;
    }

    public List<String> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<String> recommendations) {
        this.recommendations = recommendations;
    }

    public Map<String, Double> getModuleRiskScores() {
        return moduleRiskScores;
    }

    public void setModuleRiskScores(Map<String, Double> moduleRiskScores) {
        this.moduleRiskScores = moduleRiskScores;
    }
}