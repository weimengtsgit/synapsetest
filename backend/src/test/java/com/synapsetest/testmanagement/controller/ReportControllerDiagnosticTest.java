package com.synapsetest.testmanagement.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 诊断测试 - 验证ReportController测试数据是否正确插入
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("ReportController数据诊断测试")
public class ReportControllerDiagnosticTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("诊断1: 验证test-data-us4.sql是否正确加载")
    @Sql(scripts = "/test-data-us4.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void verifyTestDataLoaded() {
        // 检查quality_report表是否存在
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM quality_report",
                Integer.class
            );

            System.out.println("===== 诊断结果 =====");
            System.out.println("quality_report表记录数: " + count);

            if (count != null && count > 0) {
                // 查询所有记录
                List<Map<String, Object>> reports = jdbcTemplate.queryForList(
                    "SELECT id, task_id, name, status FROM quality_report"
                );

                System.out.println("\n已插入的数据:");
                for (Map<String, Object> report : reports) {
                    System.out.println("  - ID: " + report.get("id") +
                                     ", TaskID: " + report.get("task_id") +
                                     ", Name: " + report.get("name") +
                                     ", Status: " + report.get("status"));
                }

                assertTrue(count >= 2, "应该至少有2条测试数据");
            } else {
                fail("quality_report表中没有数据！");
            }

        } catch (Exception e) {
            System.err.println("查询失败: " + e.getMessage());
            e.printStackTrace();
            fail("查询quality_report表失败: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("诊断2: 验证特定ID的报告是否存在")
    @Sql(scripts = "/test-data-us4.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void verifySpecificReportExists() {
        try {
            String reportId = "report-001";

            // 查询特定报告
            List<Map<String, Object>> reports = jdbcTemplate.queryForList(
                "SELECT * FROM quality_report WHERE id = ?",
                reportId
            );

            System.out.println("===== 查询report-001结果 =====");
            System.out.println("记录数: " + reports.size());

            if (!reports.isEmpty()) {
                Map<String, Object> report = reports.get(0);
                System.out.println("\n报告详情:");
                report.forEach((key, value) ->
                    System.out.println("  " + key + ": " + value)
                );

                assertEquals(reportId, report.get("id"));
                assertEquals("1001", report.get("task_id"));
            } else {
                fail("未找到ID为report-001的报告！");
            }

        } catch (Exception e) {
            System.err.println("查询失败: " + e.getMessage());
            e.printStackTrace();
            fail("查询report-001失败: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("诊断3: 验证所有相关表是否存在")
    @Sql(scripts = "/test-data-us4.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void verifyAllTablesExist() {
        System.out.println("===== 检查所有表 =====");

        // 检查quality_report表
        checkTableExists("quality_report");

        // 检查quality_report_test_result表
        checkTableExists("quality_report_test_result");

        // 检查quality_report_risk_assessment表
        checkTableExists("quality_report_risk_assessment");

        // 检查test_tasks表
        checkTableExists("test_tasks");

        // 检查test_cases表
        checkTableExists("test_cases");
    }

    private void checkTableExists(String tableName) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + tableName,
                Integer.class
            );
            System.out.println(tableName + " 表存在，记录数: " + count);
        } catch (Exception e) {
            System.err.println(tableName + " 表不存在或查询失败: " + e.getMessage());
        }
    }
}
