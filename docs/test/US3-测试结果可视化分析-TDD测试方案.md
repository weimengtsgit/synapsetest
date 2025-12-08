# TDD测试方案 - User Story 3: 测试结果可视化分析

## 一、业务场景描述

**场景名称**：发布经理查看测试监控仪表盘并评估发布风险

**用户故事**：
> 作为发布经理，我需要在版本发布前查看实时的测试监控仪表盘，了解当前测试任务的执行进度、通过率、资源使用情况和质量趋势，并通过质量门禁检查评估发布风险，以便做出是否发布的决策。

**业务价值**：
- 实时监控测试进度，提供可见性
- 质量追溯能力，需求-用例-缺陷全链路关联
- 风险预警，发现80%高风险缺陷
- 数据驱动决策，发布回滚率降低70%

**业务流程**：
```
查看仪表盘 → 实时监控任务进度 → 查看质量报告 → 检查质量门禁 → 评估风险 → 决策发布
```

---

## 二、功能范围

| 工程 | 涉及组件 | 说明 |
|------|---------|------|
| Backend | MonitoringService, QualityReportService, QualityTraceabilityService | 监控数据 + 报告生成 + 质量追溯 |
| Frontend | Dashboard.jsx, QualityReport.jsx | 仪表盘页面 + 质量报告页面 |
| AI-Service | /api/v1/ai/prediction/risk | 风险预测接口 |

---

## 三、测试方案

### 3.1 Backend 测试

#### 3.1.1 Service层单元测试

**文件**: `backend/src/test/java/com/synapsetest/testmanagement/service/MonitoringServiceTest.java`

```java
@SpringBootTest
@ActiveProfiles("mongodb")
@DisplayName("监控服务 - 实时数据管理")
class MonitoringServiceTest {

    @MockBean
    private MonitoringDataRepository monitoringDataRepository;

    @Autowired
    private MonitoringService monitoringService;

    @Test
    @DisplayName("场景1.1: 创建监控数据记录")
    void createMonitoringData_ShouldSaveToMongoDB() {
        // Given: 任务启动时的监控数据
        String taskId = "task-001";
        MonitoringData data = new MonitoringData();
        data.setTaskId(taskId);
        data.setStatus("RUNNING");
        data.setProgress(0);
        data.setTotalCases(100);
        data.setExecutedCases(0);

        when(monitoringDataRepository.save(any())).thenReturn(data);

        // When
        MonitoringData saved = monitoringService.createMonitoringData(taskId, 100);

        // Then
        assertNotNull(saved);
        assertEquals(taskId, saved.getTaskId());
        assertEquals("RUNNING", saved.getStatus());
        assertEquals(0, saved.getProgress());
        assertNotNull(saved.getStartTime());
    }

    @Test
    @DisplayName("场景1.2: 更新任务执行进度")
    void updateProgress_ShouldCalculatePercentage() {
        // Given: 已执行25个用例，共100个
        String taskId = "task-001";
        MonitoringData existing = createMonitoringData(taskId, 100);
        when(monitoringDataRepository.findByTaskId(taskId)).thenReturn(Optional.of(existing));
        when(monitoringDataRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // When: 更新进度
        MonitoringData updated = monitoringService.updateProgress(taskId, 25, 23, 2, 0);

        // Then
        assertEquals(25, updated.getProgress()); // 25/100 * 100
        assertEquals(25, updated.getExecutedCases());
        assertEquals(23, updated.getPassedCases());
        assertEquals(2, updated.getFailedCases());
    }

    @Test
    @DisplayName("场景1.3: 计算通过率")
    void calculatePassRate_ShouldReturnCorrectPercentage() {
        // Given
        MonitoringData data = new MonitoringData();
        data.setExecutedCases(100);
        data.setPassedCases(85);
        data.setFailedCases(10);
        data.setSkippedCases(5);

        // When
        double passRate = monitoringService.calculatePassRate(data);

        // Then
        assertEquals(85.0, passRate, 0.01); // 85/100 * 100
    }

    @Test
    @DisplayName("场景1.4: 预估任务完成时间")
    void estimateCompletionTime_BasedOnProgress() {
        // Given: 25%进度，已运行30分钟
        MonitoringData data = new MonitoringData();
        data.setProgress(25);
        data.setStartTime(LocalDateTime.now().minusMinutes(30));
        data.setTotalCases(100);
        data.setExecutedCases(25);

        // When
        LocalDateTime estimatedEnd = monitoringService.estimateCompletionTime(data);

        // Then: 预估总时间120分钟，还需90分钟
        assertNotNull(estimatedEnd);
        assertTrue(estimatedEnd.isAfter(LocalDateTime.now()));
    }

    @Test
    @DisplayName("场景1.5: 更新资源使用情况")
    void updateResourceUsage_ShouldRecordMetrics() {
        // Given
        String taskId = "task-001";
        Map<String, Object> resourceUsage = Map.of(
            "cpu", 72.5,
            "memory", 65.0,
            "disk", 45.0
        );

        MonitoringData existing = createMonitoringData(taskId, 100);
        when(monitoringDataRepository.findByTaskId(taskId)).thenReturn(Optional.of(existing));
        when(monitoringDataRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // When
        MonitoringData updated = monitoringService.updateResourceUsage(taskId, resourceUsage);

        // Then
        assertEquals(72.5, updated.getResourceUsage().get("cpu"));
        assertEquals(65.0, updated.getResourceUsage().get("memory"));
    }

    @Test
    @DisplayName("场景1.6: 获取运行中任务列表")
    void getRunningTasks_ShouldReturnOnlyRunningStatus() {
        // Given
        List<MonitoringData> runningTasks = Arrays.asList(
            createMonitoringDataWithStatus("task-001", "RUNNING"),
            createMonitoringDataWithStatus("task-002", "RUNNING")
        );
        when(monitoringDataRepository.findByStatus("RUNNING")).thenReturn(runningTasks);

        // When
        List<MonitoringData> result = monitoringService.getRunningTasks();

        // Then
        assertEquals(2, result.size());
        result.forEach(task -> assertEquals("RUNNING", task.getStatus()));
    }

    @Test
    @DisplayName("场景1.7: 获取仪表盘统计数据")
    void getDashboardStats_ShouldAggregateMetrics() {
        // Given
        when(monitoringDataRepository.countByStatus("RUNNING")).thenReturn(5L);
        when(monitoringDataRepository.countTodayCompleted()).thenReturn(12L);
        when(monitoringDataRepository.calculateAveragePassRate()).thenReturn(89.5);

        // When
        Map<String, Object> stats = monitoringService.getDashboardStats();

        // Then
        assertEquals(5L, stats.get("runningTasks"));
        assertEquals(12L, stats.get("completedToday"));
        assertEquals(89.5, stats.get("avgPassRate"));
    }
}
```

**文件**: `backend/src/test/java/com/synapsetest/testmanagement/service/QualityReportServiceTest.java`

```java
@SpringBootTest
@ActiveProfiles("mongodb")
@DisplayName("质量报告服务")
class QualityReportServiceTest {

    @MockBean
    private QualityReportRepository qualityReportRepository;

    @MockBean
    private MonitoringDataRepository monitoringDataRepository;

    @Autowired
    private QualityReportService qualityReportService;

    @Test
    @DisplayName("场景2.1: 生成综合质量报告")
    void generateReport_ShouldAggregateResults() {
        // Given: 任务执行结果
        String taskId = "task-001";
        MonitoringData monitoringData = createCompletedMonitoringData(taskId);
        when(monitoringDataRepository.findByTaskId(taskId)).thenReturn(Optional.of(monitoringData));
        when(qualityReportRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // When
        QualityReport report = qualityReportService.generateReport(taskId);

        // Then
        assertNotNull(report.getId());
        assertEquals(taskId, report.getTaskId());
        assertEquals("COMPLETED", report.getStatus());
        assertNotNull(report.getSummary());
        assertNotNull(report.getDefectStats());
        assertNotNull(report.getPerformanceMetrics());
        assertNotNull(report.getRiskAssessment());
    }

    @Test
    @DisplayName("场景2.2: 计算风险评分")
    void calculateRiskScore_WithLowPassRate_ShouldReturnHighRisk() {
        // Given: 通过率70%，有Critical缺陷
        QualityReport report = new QualityReport();
        report.setPerformanceMetrics(Map.of("passRate", 70.0));
        report.setDefectStats(Map.of("critical", 2, "major", 5, "minor", 3));

        // When
        Map<String, Object> riskAssessment = qualityReportService.calculateRiskAssessment(report);

        // Then
        assertEquals("HIGH", riskAssessment.get("overallRisk"));
        assertTrue((double) riskAssessment.get("riskScore") > 0.7);
    }

    @Test
    @DisplayName("场景2.3: 识别高风险模块")
    void identifyHighRiskModules_ShouldReturnList() {
        // Given: 测试结果包含多个模块的失败
        List<Map<String, Object>> testResults = Arrays.asList(
            Map.of("module", "payment", "status", "FAILED"),
            Map.of("module", "payment", "status", "FAILED"),
            Map.of("module", "auth", "status", "FAILED"),
            Map.of("module", "user", "status", "PASSED")
        );

        // When
        List<String> highRiskModules = qualityReportService.identifyHighRiskModules(testResults);

        // Then
        assertTrue(highRiskModules.contains("payment"));
        assertTrue(highRiskModules.contains("auth"));
        assertEquals(2, highRiskModules.size());
    }

    @Test
    @DisplayName("场景2.4: 生成优化建议")
    void generateRecommendations_ShouldReturnSuggestions() {
        // Given
        QualityReport report = new QualityReport();
        report.setPerformanceMetrics(Map.of("passRate", 75.0, "avgResponseTime", 3500.0));
        report.setDefectStats(Map.of("critical", 1, "major", 8, "minor", 5));
        report.setRiskAssessment(Map.of(
            "highRiskModules", Arrays.asList("payment-service"),
            "overallRisk", "HIGH"
        ));

        // When
        List<String> recommendations = qualityReportService.generateRecommendations(report);

        // Then
        assertFalse(recommendations.isEmpty());
        assertTrue(recommendations.stream().anyMatch(r -> r.contains("payment")));
        assertTrue(recommendations.stream().anyMatch(r -> r.contains("通过率") || r.contains("覆盖率")));
    }

    @Test
    @DisplayName("场景2.5: 比较两个版本的报告")
    void compareReports_ShouldShowDifferences() {
        // Given
        QualityReport report1 = createReportWithPassRate("task-001", 85.0);
        QualityReport report2 = createReportWithPassRate("task-002", 92.0);

        when(qualityReportRepository.findByTaskId("task-001")).thenReturn(Optional.of(report1));
        when(qualityReportRepository.findByTaskId("task-002")).thenReturn(Optional.of(report2));

        // When
        Map<String, Object> comparison = qualityReportService.compareReports("task-001", "task-002");

        // Then
        assertNotNull(comparison.get("passRateDiff"));
        assertEquals(7.0, (double) comparison.get("passRateDiff"), 0.01);
        assertTrue((boolean) comparison.get("improved"));
    }
}
```

**文件**: `backend/src/test/java/com/synapsetest/testmanagement/service/QualityTraceabilityServiceTest.java`

```java
@SpringBootTest
@ActiveProfiles("mongodb")
@DisplayName("质量追溯服务")
class QualityTraceabilityServiceTest {

    @Autowired
    private QualityTraceabilityService traceabilityService;

    @Test
    @DisplayName("场景3.1: 追溯需求到测试用例")
    void traceRequirement_ShouldReturnRelatedCases() {
        // Given
        String requirementId = "REQ-001";

        // When
        Map<String, Object> traceResult = traceabilityService.traceRequirement(requirementId);

        // Then
        assertNotNull(traceResult.get("requirement"));
        assertNotNull(traceResult.get("testCases"));
        List<Map> cases = (List<Map>) traceResult.get("testCases");
        assertFalse(cases.isEmpty());
    }

    @Test
    @DisplayName("场景3.2: 生成追溯矩阵")
    void generateTraceabilityMatrix_ShouldReturnMappings() {
        // Given
        List<String> requirementIds = Arrays.asList("REQ-001", "REQ-002", "REQ-003");

        // When
        Map<String, Object> matrix = traceabilityService.generateTraceabilityMatrix(requirementIds);

        // Then
        assertNotNull(matrix.get("matrix"));
        assertNotNull(matrix.get("coverage"));
        double coverage = (double) matrix.get("coverage");
        assertTrue(coverage >= 0 && coverage <= 100);
    }

    @Test
    @DisplayName("场景3.3: 检查质量门禁")
    void checkQualityGates_WithAllRulesPassed_ShouldReturnPass() {
        // Given: 满足所有质量门禁规则
        String taskId = "task-001";
        // Mock数据: 通过率98%, 无Critical缺陷, 核心功能100%, 性能偏差5%

        // When
        Map<String, Object> gateResult = traceabilityService.checkQualityGates(taskId);

        // Then
        assertTrue((boolean) gateResult.get("passed"));
        List<String> passedRules = (List<String>) gateResult.get("passedRules");
        assertEquals(4, passedRules.size());
    }

    @Test
    @DisplayName("场景3.4: 质量门禁检查失败")
    void checkQualityGates_WithRuleFailed_ShouldReturnFail() {
        // Given: 通过率93% < 95%门禁
        String taskId = "task-low-pass-rate";

        // When
        Map<String, Object> gateResult = traceabilityService.checkQualityGates(taskId);

        // Then
        assertFalse((boolean) gateResult.get("passed"));
        List<String> failedRules = (List<String>) gateResult.get("failedRules");
        assertTrue(failedRules.stream().anyMatch(r -> r.contains("通过率")));
    }

    @Test
    @DisplayName("场景3.5: 追溯缺陷影响范围")
    void traceDefect_ShouldReturnImpactedAreas() {
        // Given
        String defectId = "BUG-001";

        // When
        Map<String, Object> impact = traceabilityService.traceDefect(defectId);

        // Then
        assertNotNull(impact.get("defect"));
        assertNotNull(impact.get("affectedTestCases"));
        assertNotNull(impact.get("relatedRequirements"));
        assertNotNull(impact.get("impactedModules"));
    }

    @Test
    @DisplayName("场景3.6: 分析代码变更影响")
    void analyzeChangeImpact_ShouldReturnAffectedTests() {
        // Given
        String changeId = "commit-abc123";
        List<String> changedFiles = Arrays.asList(
            "src/main/java/com/service/PaymentService.java",
            "src/main/java/com/controller/PaymentController.java"
        );

        // When
        Map<String, Object> impact = traceabilityService.analyzeChangeImpact(changeId, changedFiles);

        // Then
        assertNotNull(impact.get("affectedTests"));
        List<String> affectedTests = (List<String>) impact.get("affectedTests");
        assertTrue(affectedTests.stream().anyMatch(t -> t.contains("payment")));
        assertNotNull(impact.get("recommendedTestScope"));
    }
}
```

#### 3.1.2 Controller集成测试

**文件**: `backend/src/test/java/com/synapsetest/testmanagement/controller/MonitoringControllerIntegrationTest.java`

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("mongodb")
@DisplayName("监控API集成测试")
class MonitoringControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @MockBean
    private MonitoringService monitoringService;

    @Test
    @DisplayName("场景4.1: GET获取任务监控数据")
    void getTaskMonitoring_ShouldReturn200() {
        // Given
        String taskId = "task-001";
        MonitoringData mockData = createMonitoringData(taskId);
        when(monitoringService.getMonitoringData(taskId)).thenReturn(mockData);

        // When
        ResponseEntity<ApiResponse> response = restTemplate.getForEntity(
            "/api/v1/monitoring/tasks/" + taskId,
            ApiResponse.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> data = (Map<String, Object>) response.getBody().getData();
        assertEquals(taskId, data.get("taskId"));
        assertEquals("RUNNING", data.get("status"));
        assertNotNull(data.get("progress"));
    }

    @Test
    @DisplayName("场景4.2: GET获取仪表盘统计数据")
    void getDashboardStats_ShouldReturnAggregatedData() {
        // Given
        Map<String, Object> mockStats = Map.of(
            "runningTasks", 5,
            "completedToday", 12,
            "avgPassRate", 89.5,
            "resourceUsage", Map.of("cpu", 72, "memory", 65)
        );
        when(monitoringService.getDashboardStats()).thenReturn(mockStats);

        // When
        ResponseEntity<ApiResponse> response = restTemplate.getForEntity(
            "/api/v1/monitoring/dashboard/stats",
            ApiResponse.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> stats = (Map<String, Object>) response.getBody().getData();
        assertEquals(5, stats.get("runningTasks"));
        assertEquals(89.5, stats.get("avgPassRate"));
    }

    @Test
    @DisplayName("场景4.3: POST更新资源使用情况")
    void updateResourceUsage_ShouldReturn200() {
        // Given
        String taskId = "task-001";
        String requestBody = """
            {
                "cpu": 75.5,
                "memory": 68.0,
                "disk": 50.0
            }
            """;

        MonitoringData updatedData = createMonitoringData(taskId);
        when(monitoringService.updateResourceUsage(eq(taskId), any())).thenReturn(updatedData);

        // When
        ResponseEntity<ApiResponse> response = restTemplate.postForEntity(
            "/api/v1/monitoring/tasks/" + taskId + "/resource-usage",
            new HttpEntity<>(requestBody, createJsonHeaders()),
            ApiResponse.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @DisplayName("场景4.4: GET获取运行中任务列表")
    void getRunningTasks_ShouldReturnList() {
        // Given
        List<MonitoringData> runningTasks = Arrays.asList(
            createMonitoringData("task-001"),
            createMonitoringData("task-002")
        );
        when(monitoringService.getRunningTasks()).thenReturn(runningTasks);

        // When
        ResponseEntity<ApiResponse> response = restTemplate.getForEntity(
            "/api/v1/monitoring/running",
            ApiResponse.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<Map> tasks = (List<Map>) response.getBody().getData();
        assertEquals(2, tasks.size());
    }

    @Test
    @DisplayName("场景4.5: GET按环境筛选监控数据")
    void getMonitoringByEnvironment_ShouldFilterCorrectly() {
        // Given
        String environment = "DEV";
        List<MonitoringData> devTasks = Arrays.asList(createMonitoringData("task-dev-001"));
        when(monitoringService.getByEnvironment(environment)).thenReturn(devTasks);

        // When
        ResponseEntity<ApiResponse> response = restTemplate.getForEntity(
            "/api/v1/monitoring/environment/" + environment,
            ApiResponse.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
```

**文件**: `backend/src/test/java/com/synapsetest/testmanagement/controller/ReportControllerIntegrationTest.java`

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("mongodb")
@DisplayName("报告API集成测试")
class ReportControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @MockBean
    private QualityReportService qualityReportService;

    @MockBean
    private QualityTraceabilityService traceabilityService;

    @Test
    @DisplayName("场景5.1: GET获取质量报告详情")
    void getQualityReport_ShouldReturn200() {
        // Given
        String reportId = "report-001";
        QualityReport mockReport = createQualityReport(reportId);
        when(qualityReportService.getReportById(reportId)).thenReturn(mockReport);

        // When
        ResponseEntity<ApiResponse> response = restTemplate.getForEntity(
            "/api/v1/reports/" + reportId,
            ApiResponse.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> report = (Map<String, Object>) response.getBody().getData();
        assertNotNull(report.get("summary"));
        assertNotNull(report.get("riskAssessment"));
    }

    @Test
    @DisplayName("场景5.2: GET获取综合报告")
    void getComprehensiveReport_ShouldReturnAllMetrics() {
        // Given
        String taskId = "task-001";
        Map<String, Object> mockComprehensive = Map.of(
            "summary", "综合报告摘要",
            "testResults", Arrays.asList(),
            "defectStats", Map.of("total", 15),
            "riskAssessment", Map.of("overallRisk", "MEDIUM"),
            "recommendations", Arrays.asList("建议1", "建议2")
        );
        when(qualityReportService.getComprehensiveReport(taskId)).thenReturn(mockComprehensive);

        // When
        ResponseEntity<ApiResponse> response = restTemplate.getForEntity(
            "/api/v1/reports/comprehensive/" + taskId,
            ApiResponse.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> data = (Map<String, Object>) response.getBody().getData();
        assertNotNull(data.get("riskAssessment"));
        assertNotNull(data.get("recommendations"));
    }

    @Test
    @DisplayName("场景5.3: GET比较两个报告")
    void compareReports_ShouldReturnDifferences() {
        // Given
        Map<String, Object> comparison = Map.of(
            "passRateDiff", 7.0,
            "defectCountDiff", -3,
            "improved", true
        );
        when(qualityReportService.compareReports("task-001", "task-002")).thenReturn(comparison);

        // When
        ResponseEntity<ApiResponse> response = restTemplate.getForEntity(
            "/api/v1/reports/compare?taskId1=task-001&taskId2=task-002",
            ApiResponse.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> data = (Map<String, Object>) response.getBody().getData();
        assertTrue((boolean) data.get("improved"));
    }

    @Test
    @DisplayName("场景5.4: GET检查质量门禁")
    void checkQualityGates_ShouldReturnPassOrFail() {
        // Given
        String taskId = "task-001";
        Map<String, Object> gateResult = Map.of(
            "passed", true,
            "passedRules", Arrays.asList("Rule 1", "Rule 2", "Rule 3", "Rule 4"),
            "failedRules", Arrays.asList()
        );
        when(traceabilityService.checkQualityGates(taskId)).thenReturn(gateResult);

        // When
        ResponseEntity<ApiResponse> response = restTemplate.getForEntity(
            "/api/v1/reports/quality-gates/" + taskId,
            ApiResponse.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> data = (Map<String, Object>) response.getBody().getData();
        assertTrue((boolean) data.get("passed"));
    }

    @Test
    @DisplayName("场景5.5: GET获取HTML格式报告")
    void getHtmlReport_ShouldReturnHtmlString() {
        // Given
        String taskId = "task-001";
        String mockHtml = "<html><body><h1>Quality Report</h1></body></html>";
        when(qualityReportService.generateHtmlReport(taskId)).thenReturn(mockHtml);

        // When
        ResponseEntity<String> response = restTemplate.getForEntity(
            "/api/v1/reports/html/" + taskId,
            String.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("<html>"));
        assertTrue(response.getBody().contains("Quality Report"));
    }

    @Test
    @DisplayName("场景5.6: POST生成追溯矩阵")
    void generateTraceabilityMatrix_ShouldReturnMatrix() {
        // Given
        String requestBody = """
            ["REQ-001", "REQ-002", "REQ-003"]
            """;

        Map<String, Object> matrix = Map.of(
            "matrix", Map.of("REQ-001", Arrays.asList("TC-001", "TC-002")),
            "coverage", 85.0
        );
        when(traceabilityService.generateTraceabilityMatrix(any())).thenReturn(matrix);

        // When
        ResponseEntity<ApiResponse> response = restTemplate.postForEntity(
            "/api/v1/reports/traceability/matrix",
            new HttpEntity<>(requestBody, createJsonHeaders()),
            ApiResponse.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> data = (Map<String, Object>) response.getBody().getData();
        assertEquals(85.0, data.get("coverage"));
    }
}
```

---

### 3.2 Frontend 测试

#### 3.2.1 仪表盘组件测试

**文件**: `frontend/src/components/monitoring/__tests__/Dashboard.test.tsx`

```typescript
import { render, screen, waitFor, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import Dashboard from '../Dashboard';
import monitoringService from '../../../services/monitoringService';

jest.mock('../../../services/monitoringService');
jest.useFakeTimers();

describe('监控仪表盘场景', () => {

  const mockDashboardStats = {
    runningTasks: 5,
    completedToday: 12,
    avgPassRate: 89.5,
    resourceUsage: { cpu: 72, memory: 65, disk: 45 }
  };

  const mockRunningTasks = [
    {
      taskId: 'task-001',
      name: '登录模块测试',
      status: 'RUNNING',
      progress: 65,
      executedCases: 65,
      totalCases: 100,
      passedCases: 60,
      failedCases: 5
    },
    {
      taskId: 'task-002',
      name: '支付流程测试',
      status: 'RUNNING',
      progress: 30,
      executedCases: 30,
      totalCases: 100,
      passedCases: 28,
      failedCases: 2
    }
  ];

  beforeEach(() => {
    jest.clearAllMocks();
    (monitoringService.getDashboardStats as jest.Mock).mockResolvedValue(mockDashboardStats);
    (monitoringService.getRunningTasks as jest.Mock).mockResolvedValue(mockRunningTasks);
  });

  test('场景1.1: 正确渲染核心指标卡片', async () => {
    render(<Dashboard />);

    await waitFor(() => {
      expect(screen.getByText('当前运行任务')).toBeInTheDocument();
      expect(screen.getByText('5')).toBeInTheDocument();
      expect(screen.getByText('今日完成')).toBeInTheDocument();
      expect(screen.getByText('12')).toBeInTheDocument();
      expect(screen.getByText('平均通过率')).toBeInTheDocument();
      expect(screen.getByText('89.5%')).toBeInTheDocument();
    });
  });

  test('场景1.2: 显示实时任务监控列表', async () => {
    render(<Dashboard />);

    await waitFor(() => {
      expect(screen.getByText('登录模块测试')).toBeInTheDocument();
      expect(screen.getByText('支付流程测试')).toBeInTheDocument();
      expect(screen.getByText('65%')).toBeInTheDocument(); // 进度
      expect(screen.getByText('30%')).toBeInTheDocument();
    });
  });

  test('场景1.3: 显示资源使用情况', async () => {
    render(<Dashboard />);

    await waitFor(() => {
      expect(screen.getByText(/CPU: 72%/)).toBeInTheDocument();
      expect(screen.getByText(/内存: 65%/)).toBeInTheDocument();
      expect(screen.getByText(/磁盘: 45%/)).toBeInTheDocument();
    });
  });

  test('场景1.4: 数据自动刷新 (10秒间隔)', async () => {
    render(<Dashboard />);

    await waitFor(() => {
      expect(monitoringService.getDashboardStats).toHaveBeenCalledTimes(1);
    });

    // 快进10秒
    act(() => {
      jest.advanceTimersByTime(10000);
    });

    await waitFor(() => {
      expect(monitoringService.getDashboardStats).toHaveBeenCalledTimes(2);
    });

    // 再快进10秒
    act(() => {
      jest.advanceTimersByTime(10000);
    });

    await waitFor(() => {
      expect(monitoringService.getDashboardStats).toHaveBeenCalledTimes(3);
    });
  });

  test('场景1.5: 组件卸载时清除定时器', async () => {
    const { unmount } = render(<Dashboard />);

    await waitFor(() => {
      expect(monitoringService.getDashboardStats).toHaveBeenCalled();
    });

    unmount();

    // 卸载后定时器应该被清除，不再调用
    act(() => {
      jest.advanceTimersByTime(10000);
    });

    // 调用次数应该不变
    expect(monitoringService.getDashboardStats).toHaveBeenCalledTimes(1);
  });

  test('场景1.6: 点击指标卡片下钻查看详情', async () => {
    const mockOnDrillDown = jest.fn();
    render(<Dashboard onDrillDown={mockOnDrillDown} />);

    await waitFor(() => {
      expect(screen.getByText('当前运行任务')).toBeInTheDocument();
    });

    // When: 点击运行任务卡片
    await userEvent.click(screen.getByText('当前运行任务'));

    // Then: 触发下钻回调
    expect(mockOnDrillDown).toHaveBeenCalledWith('runningTasks');
  });

  test('场景1.7: 任务进度条正确显示', async () => {
    render(<Dashboard />);

    await waitFor(() => {
      const progressBars = screen.getAllByRole('progressbar');
      expect(progressBars.length).toBeGreaterThan(0);
    });
  });

  test('场景1.8: 加载失败显示错误提示', async () => {
    (monitoringService.getDashboardStats as jest.Mock).mockRejectedValue(
      new Error('网络错误')
    );

    render(<Dashboard />);

    await waitFor(() => {
      expect(screen.getByText(/加载失败/)).toBeInTheDocument();
    });
  });

  test('场景1.9: 支持自定义时间范围选择', async () => {
    render(<Dashboard />);

    await waitFor(() => {
      expect(screen.getByText('当前运行任务')).toBeInTheDocument();
    });

    // When: 选择时间范围
    const timeRangeSelector = screen.getByLabelText('时间范围');
    await userEvent.selectOptions(timeRangeSelector, '7days');

    // Then: 重新加载数据
    expect(monitoringService.getDashboardStats).toHaveBeenCalledWith({ range: '7days' });
  });
});
```

#### 3.2.2 质量报告组件测试

**文件**: `frontend/src/components/report/__tests__/QualityReport.test.tsx`

```typescript
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import QualityReport from '../QualityReport';
import reportService from '../../../services/reportService';

jest.mock('../../../services/reportService');

describe('质量报告场景', () => {

  const mockReport = {
    id: 'report-001',
    taskId: 'task-001',
    summary: '测试任务完成，通过率92%',
    testResults: [
      { testCaseId: 'tc-001', status: 'PASSED', executionTime: 1500 },
      { testCaseId: 'tc-002', status: 'FAILED', executionTime: 2300, error: '断言失败' },
      { testCaseId: 'tc-003', status: 'PASSED', executionTime: 1800 }
    ],
    defectStats: {
      critical: 0,
      major: 2,
      minor: 5,
      total: 7
    },
    performanceMetrics: {
      passRate: 92.0,
      avgResponseTime: 1867,
      throughput: 45.2
    },
    riskAssessment: {
      overallRisk: 'LOW',
      riskScore: 0.25,
      highRiskModules: [],
      recommendations: [
        '建议增加边界条件测试',
        '考虑增加性能测试覆盖'
      ]
    },
    generatedAt: '2025-11-16T14:30:00Z'
  };

  beforeEach(() => {
    (reportService.getQualityReport as jest.Mock).mockResolvedValue(mockReport);
  });

  test('场景2.1: 正确渲染报告摘要', async () => {
    render(<QualityReport taskId="task-001" />);

    await waitFor(() => {
      expect(screen.getByText(/测试任务完成/)).toBeInTheDocument();
      expect(screen.getByText(/通过率: 92%/)).toBeInTheDocument();
    });
  });

  test('场景2.2: 显示缺陷统计图表', async () => {
    render(<QualityReport taskId="task-001" />);

    await waitFor(() => {
      expect(screen.getByText('缺陷统计')).toBeInTheDocument();
      expect(screen.getByText('Critical: 0')).toBeInTheDocument();
      expect(screen.getByText('Major: 2')).toBeInTheDocument();
      expect(screen.getByText('Minor: 5')).toBeInTheDocument();
      expect(screen.getByText('总计: 7')).toBeInTheDocument();
    });
  });

  test('场景2.3: 显示风险评估结果', async () => {
    render(<QualityReport taskId="task-001" />);

    await waitFor(() => {
      expect(screen.getByText('风险评估')).toBeInTheDocument();
      expect(screen.getByText('LOW')).toBeInTheDocument();
      expect(screen.getByText(/风险分数: 0.25/)).toBeInTheDocument();
    });
  });

  test('场景2.4: 显示优化建议列表', async () => {
    render(<QualityReport taskId="task-001" />);

    await waitFor(() => {
      expect(screen.getByText('优化建议')).toBeInTheDocument();
      expect(screen.getByText('建议增加边界条件测试')).toBeInTheDocument();
      expect(screen.getByText('考虑增加性能测试覆盖')).toBeInTheDocument();
    });
  });

  test('场景2.5: 导出PDF功能', async () => {
    (reportService.exportToPdf as jest.Mock).mockResolvedValue(new Blob());

    render(<QualityReport taskId="task-001" />);

    await waitFor(() => {
      expect(screen.getByText('导出PDF')).toBeInTheDocument();
    });

    await userEvent.click(screen.getByRole('button', { name: '导出PDF' }));

    expect(reportService.exportToPdf).toHaveBeenCalledWith('task-001');
  });

  test('场景2.6: 版本对比功能', async () => {
    const mockComparison = {
      passRateDiff: 5.0,
      defectCountDiff: -3,
      improved: true
    };
    (reportService.compareReports as jest.Mock).mockResolvedValue(mockComparison);

    render(<QualityReport taskId="task-001" enableComparison={true} />);

    await waitFor(() => {
      expect(screen.getByText('版本对比')).toBeInTheDocument();
    });

    // 选择对比版本
    await userEvent.selectOptions(screen.getByLabelText('对比版本'), 'task-000');
    await userEvent.click(screen.getByRole('button', { name: '对比' }));

    await waitFor(() => {
      expect(screen.getByText(/通过率提升 5%/)).toBeInTheDocument();
      expect(screen.getByText(/缺陷减少 3 个/)).toBeInTheDocument();
    });
  });

  test('场景2.7: 高风险报告显示警告', async () => {
    const highRiskReport = {
      ...mockReport,
      riskAssessment: {
        overallRisk: 'HIGH',
        riskScore: 0.85,
        highRiskModules: ['payment-service', 'auth-service'],
        recommendations: ['紧急修复payment模块问题']
      }
    };
    (reportService.getQualityReport as jest.Mock).mockResolvedValue(highRiskReport);

    render(<QualityReport taskId="task-001" />);

    await waitFor(() => {
      expect(screen.getByText('HIGH')).toBeInTheDocument();
      expect(screen.getByRole('alert')).toBeInTheDocument();
      expect(screen.getByText(/高风险模块/)).toBeInTheDocument();
    });
  });
});
```

#### 3.2.3 Service层测试

**文件**: `frontend/src/services/__tests__/monitoringService.test.ts`

```typescript
import axios from 'axios';
import monitoringService from '../monitoringService';

jest.mock('axios');
const mockedAxios = axios as jest.Mocked<typeof axios>;

describe('monitoringService - 实时监控', () => {

  test('场景3.1: getDashboardStats返回统计数据', async () => {
    const mockStats = {
      runningTasks: 5,
      completedToday: 12,
      avgPassRate: 89.5
    };

    mockedAxios.get.mockResolvedValue({
      data: { success: true, data: mockStats }
    });

    const result = await monitoringService.getDashboardStats();

    expect(result).toEqual(mockStats);
    expect(mockedAxios.get).toHaveBeenCalledWith('/api/v1/monitoring/dashboard/stats');
  });

  test('场景3.2: getTaskMonitoring返回任务监控数据', async () => {
    const taskId = 'task-001';
    const mockData = {
      taskId,
      status: 'RUNNING',
      progress: 65
    };

    mockedAxios.get.mockResolvedValue({
      data: { success: true, data: mockData }
    });

    const result = await monitoringService.getTaskMonitoring(taskId);

    expect(result.taskId).toBe(taskId);
    expect(mockedAxios.get).toHaveBeenCalledWith(`/api/v1/monitoring/tasks/${taskId}`);
  });

  test('场景3.3: updateResourceUsage发送正确数据', async () => {
    const taskId = 'task-001';
    const resourceData = { cpu: 75, memory: 68 };

    mockedAxios.post.mockResolvedValue({
      data: { success: true, data: {} }
    });

    await monitoringService.updateResourceUsage(taskId, resourceData);

    expect(mockedAxios.post).toHaveBeenCalledWith(
      `/api/v1/monitoring/tasks/${taskId}/resource-usage`,
      resourceData
    );
  });
});

describe('reportService - 质量报告', () => {

  test('场景3.4: checkQualityGates返回门禁结果', async () => {
    const taskId = 'task-001';
    const mockResult = {
      passed: true,
      passedRules: ['Rule1', 'Rule2'],
      failedRules: []
    };

    mockedAxios.get.mockResolvedValue({
      data: { success: true, data: mockResult }
    });

    const result = await reportService.checkQualityGates(taskId);

    expect(result.passed).toBe(true);
    expect(mockedAxios.get).toHaveBeenCalledWith(`/api/v1/reports/quality-gates/${taskId}`);
  });

  test('场景3.5: generateTraceabilityMatrix发送需求ID列表', async () => {
    const requirementIds = ['REQ-001', 'REQ-002'];

    mockedAxios.post.mockResolvedValue({
      data: { success: true, data: { coverage: 85 } }
    });

    await reportService.generateTraceabilityMatrix(requirementIds);

    expect(mockedAxios.post).toHaveBeenCalledWith(
      '/api/v1/reports/traceability/matrix',
      requirementIds
    );
  });
});
```

---

### 3.3 AI-Service 测试

**文件**: `ai-service/tests/test_risk_prediction.py`

```python
import pytest
from fastapi.testclient import TestClient
from main import app
from models.risk_model import RiskPredictionModel

client = TestClient(app)

class TestRiskPredictionAPI:
    """风险预测API测试"""

    def test_scenario_1_predict_low_risk(self):
        """场景1: 低风险预测"""
        # Given: 稳定模块，小变更
        request_data = {
            "changed_files": 3,
            "changed_lines": 50,
            "module": "static-content",
            "historical_defects": 1,
            "test_coverage": 0.92,
            "days_since_last_change": 30
        }

        # When
        response = client.post("/api/v1/ai/prediction/risk", json=request_data)

        # Then
        assert response.status_code == 200
        data = response.json()
        assert data["risk_level"] == "LOW"
        assert data["risk_score"] < 0.3
        assert data["confidence"] > 0.7

    def test_scenario_2_predict_high_risk(self):
        """场景2: 高风险预测"""
        # Given: 核心模块，大变更，历史缺陷多
        request_data = {
            "changed_files": 25,
            "changed_lines": 500,
            "module": "payment-service",
            "historical_defects": 15,
            "test_coverage": 0.55,
            "days_since_last_change": 2
        }

        # When
        response = client.post("/api/v1/ai/prediction/risk", json=request_data)

        # Then
        data = response.json()
        assert data["risk_level"] in ["HIGH", "CRITICAL"]
        assert data["risk_score"] > 0.7
        assert "payment" in str(data.get("contributing_factors", []))

    def test_scenario_3_return_contributing_factors(self):
        """场景3: 返回风险贡献因素"""
        request_data = {
            "changed_files": 10,
            "changed_lines": 200,
            "module": "auth-service",
            "historical_defects": 8,
            "test_coverage": 0.65
        }

        response = client.post("/api/v1/ai/prediction/risk", json=request_data)

        data = response.json()
        assert "contributing_factors" in data
        factors = data["contributing_factors"]
        assert len(factors) > 0
        # 应该包含具体的风险因素描述

    def test_scenario_4_return_recommendations(self):
        """场景4: 返回优化建议"""
        request_data = {
            "changed_files": 15,
            "changed_lines": 300,
            "module": "user-service",
            "test_coverage": 0.60
        }

        response = client.post("/api/v1/ai/prediction/risk", json=request_data)

        data = response.json()
        assert "recommendations" in data
        recommendations = data["recommendations"]
        assert len(recommendations) > 0
        # 应该建议提高测试覆盖率
        assert any("测试" in r or "覆盖" in r for r in recommendations)

    def test_scenario_5_invalid_input_returns_400(self):
        """场景5: 无效输入返回400"""
        request_data = {
            "changed_files": -1,  # 无效
            "module": ""  # 空
        }

        response = client.post("/api/v1/ai/prediction/risk", json=request_data)

        assert response.status_code == 400


class TestRiskModelUnit:
    """风险预测模型单元测试"""

    def test_scenario_6_extract_features_correctly(self):
        """场景6: 正确提取特征向量"""
        model = RiskPredictionModel()
        features = {
            "changed_files": 10,
            "changed_lines": 200,
            "historical_defects": 5,
            "test_coverage": 0.75,
            "module_importance": 0.8
        }

        feature_vector = model._extract_features(features)

        assert feature_vector is not None
        assert len(feature_vector) > 0

    def test_scenario_7_risk_score_in_valid_range(self):
        """场景7: 风险分数在有效范围内"""
        model = RiskPredictionModel()
        features = {
            "changed_files": 10,
            "changed_lines": 200,
            "historical_defects": 5
        }

        result = model.predict_risk(features)

        assert 0.0 <= result["risk_score"] <= 1.0
        assert result["risk_level"] in ["LOW", "MEDIUM", "HIGH", "CRITICAL"]

    def test_scenario_8_confidence_score_valid(self):
        """场景8: 置信度分数有效"""
        model = RiskPredictionModel()
        features = {"changed_files": 5}

        result = model.predict_risk(features)

        assert 0.0 <= result["confidence"] <= 1.0
```

---

## 四、测试数据

**文件**: `backend/src/test/resources/test-data-us3.sql`

```sql
-- User Story 3 测试数据 (MongoDB数据通过@BeforeEach初始化)

-- 相关的MySQL数据
INSERT INTO test_tasks (id, name, environment, version, status, priority, created_by, created_at, updated_at) VALUES
('task-001', '登录模块测试', 'DEV', 'v2.1.0', 'RUNNING', 8, 'zhangsan', NOW(), NOW()),
('task-002', '支付流程测试', 'STAGING', 'v2.1.0', 'COMPLETED', 9, 'lisi', NOW(), NOW()),
('task-low-pass-rate', '低通过率任务', 'DEV', 'v2.0.0', 'COMPLETED', 5, 'wangwu', NOW(), NOW());
```

**MongoDB测试数据初始化**:

```java
@BeforeEach
void setupMongoData() {
    // 监控数据
    MonitoringData data = new MonitoringData();
    data.setTaskId("task-001");
    data.setStatus("RUNNING");
    data.setProgress(65);
    data.setTotalCases(100);
    data.setExecutedCases(65);
    data.setPassedCases(60);
    data.setFailedCases(5);
    data.setResourceUsage(Map.of("cpu", 72.0, "memory", 65.0));
    data.setStartTime(LocalDateTime.now().minusMinutes(30));
    monitoringDataRepository.save(data);

    // 质量报告
    QualityReport report = new QualityReport();
    report.setTaskId("task-002");
    report.setSummary("测试完成");
    report.setDefectStats(Map.of("critical", 0, "major", 2, "minor", 5));
    report.setRiskAssessment(Map.of("overallRisk", "LOW", "riskScore", 0.25));
    qualityReportRepository.save(report);
}
```

---

## 五、验收标准

| 测试场景 | 工程 | 验收条件 | 优先级 |
|---------|------|---------|-------|
| 仪表盘显示核心指标 | Frontend | 正确显示运行任务数、完成数、通过率、资源使用 | P0 |
| 实时数据自动刷新 | Frontend | 10秒间隔自动更新，组件卸载清除定时器 | P0 |
| 质量报告生成 | Backend | 包含摘要、缺陷统计、风险评估、优化建议 | P0 |
| 风险评分计算 | AI-Service | 基于多因素计算，分数0-1，等级LOW/MEDIUM/HIGH/CRITICAL | P0 |
| 质量门禁检查 | Backend | 满足所有规则返回PASS，否则返回FAIL及失败原因 | P0 |
| 追溯矩阵生成 | Backend | 需求-用例映射完整，覆盖率计算正确 | P1 |
| 报告对比 | Backend | 正确计算通过率差异、缺陷变化 | P1 |
| 错误处理 | 全部 | 网络错误显示提示，数据格式错误优雅处理 | P1 |

---

## 六、执行顺序

1. **Red阶段**: 编写失败的测试用例
2. **Green阶段**: 实现最小代码使测试通过
3. **Refactor阶段**: 优化代码，保持测试绿色

**建议执行路径**:
```
AI-Service风险预测测试 → Backend MongoDB Service测试 → Controller集成测试 → Frontend仪表盘组件测试 → 报告组件测试
```

**特别注意**:
- MongoDB相关测试需要 `@ActiveProfiles("mongodb")`
- 前端定时器测试使用 `jest.useFakeTimers()`
- WebSocket测试需要单独处理
