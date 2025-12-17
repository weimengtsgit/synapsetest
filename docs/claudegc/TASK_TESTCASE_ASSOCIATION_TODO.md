# 测试任务与测试用例关联功能 - 待实现

## 问题描述

当前系统的创建任务功能**只是创建了一条任务记录**，并没有实现**任务与测试用例的关联**。这导致：

1. ❌ 创建的任务不知道要执行哪些用例
2. ❌ 无法根据条件筛选和推荐相关用例
3. ❌ 启动任务时没有具体的执行计划
4. ❌ 无法追踪任务执行了哪些用例

虽然数据库中已经设计了关联表 `task_test_cases`，但**代码层面完全没有实现**。

## 设计方案

### 1. 数据模型扩展

#### 1.1 TaskTestCase 实体（新增）

```java
// backend/src/main/java/com/synapsetest/testmanagement/model/TaskTestCase.java
@Data
public class TaskTestCase {
    private String taskId;           // 任务ID
    private String testCaseId;       // 用例ID
    private Integer executionOrder;  // 执行顺序
    private LocalDateTime createdAt; // 创建时间
}
```

#### 1.2 扩展 TestTaskResponse

```java
// backend/src/main/java/com/synapsetest/testmanagement/dto/response/TestTaskResponse.java
@Data
public class TestTaskResponse {
    // ... 现有字段 ...
    
    // 新增字段
    private List<TestCaseInfo> testCases;     // 关联的测试用例列表
    private Integer totalTestCases;            // 用例总数
    
    @Data
    public static class TestCaseInfo {
        private String id;
        private String caseNumber;
        private String title;
        private String module;
        private Integer priority;
        private String type;
        private Integer executionOrder;
    }
}
```

### 2. 后端实现

#### 2.1 创建 TaskTestCaseMapper

```java
// backend/src/main/java/com/synapsetest/testmanagement/mapper/TaskTestCaseMapper.java
@Mapper
public interface TaskTestCaseMapper {
    
    /**
     * 批量插入任务-用例关联
     */
    void batchInsert(@Param("associations") List<TaskTestCase> associations);
    
    /**
     * 根据任务ID查询关联的用例ID列表
     */
    List<String> selectTestCaseIdsByTaskId(@Param("taskId") String taskId);
    
    /**
     * 根据任务ID查询关联的用例详情（带顺序）
     */
    List<TestCase> selectTestCasesByTaskId(@Param("taskId") String taskId);
    
    /**
     * 删除任务的所有用例关联
     */
    void deleteByTaskId(@Param("taskId") String taskId);
    
    /**
     * 更新执行顺序
     */
    void updateExecutionOrder(@Param("taskId") String taskId, 
                              @Param("testCaseId") String testCaseId, 
                              @Param("executionOrder") Integer executionOrder);
}
```

#### 2.2 创建 TaskTestCaseMapper.xml

```xml
<!-- backend/src/main/resources/mapper/TaskTestCaseMapper.xml -->
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" 
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.synapsetest.testmanagement.mapper.TaskTestCaseMapper">
    
    <!-- 批量插入 -->
    <insert id="batchInsert">
        INSERT INTO task_test_cases (task_id, test_case_id, execution_order)
        VALUES
        <foreach collection="associations" item="item" separator=",">
            (#{item.taskId}, #{item.testCaseId}, #{item.executionOrder})
        </foreach>
    </insert>
    
    <!-- 查询用例ID列表 -->
    <select id="selectTestCaseIdsByTaskId" resultType="String">
        SELECT test_case_id 
        FROM task_test_cases 
        WHERE task_id = #{taskId}
        ORDER BY execution_order
    </select>
    
    <!-- 查询用例详情 -->
    <select id="selectTestCasesByTaskId" resultType="TestCase">
        SELECT tc.*, ttc.execution_order
        FROM test_cases tc
        INNER JOIN task_test_cases ttc ON tc.id = ttc.test_case_id
        WHERE ttc.task_id = #{taskId}
        ORDER BY ttc.execution_order, tc.priority DESC
    </select>
    
    <!-- 删除关联 -->
    <delete id="deleteByTaskId">
        DELETE FROM task_test_cases WHERE task_id = #{taskId}
    </delete>
    
    <!-- 更新顺序 -->
    <update id="updateExecutionOrder">
        UPDATE task_test_cases 
        SET execution_order = #{executionOrder}
        WHERE task_id = #{taskId} AND test_case_id = #{testCaseId}
    </update>
    
</mapper>
```

#### 2.3 扩展 TestTaskService

```java
// backend/src/main/java/com/synapsetest/testmanagement/service/TestTaskService.java
@Service
@RequiredArgsConstructor
@Slf4j
public class TestTaskService {
    
    private final TestTaskMapper testTaskMapper;
    private final TaskTestCaseMapper taskTestCaseMapper;  // 新增
    private final TestCaseMapper testCaseMapper;          // 新增
    private final TestRecommendationService recommendationService;
    
    /**
     * 创建测试任务（包含用例匹配和关联）
     */
    @Transactional
    public TestTaskResponse createTestTask(CreateTestTaskRequest request, String username) {
        log.info("Creating test task: {} by user: {}", request.getTaskName(), username);
        
        // 1. 获取AI推荐
        TestTaskResponse.TestRecommendation recommendation =
                recommendationService.getTestRecommendation(request);
        
        // 2. 创建任务记录
        TestTask testTask = new TestTask();
        testTask.setId(UUID.randomUUID().toString());
        testTask.setName(request.getTaskName());
        testTask.setDescription("Test task for modules: " + String.join(", ", request.getModules()));
        testTask.setEnvironment(request.getEnvironment());
        testTask.setVersion(request.getVersion());
        testTask.setTestScope(recommendation.getRecommendedScope());
        testTask.setStatus(TestTask.Status.PENDING.name());
        testTask.setPriority(8);
        testTask.setCreatedBy(username);
        testTask.setCreatedAt(LocalDateTime.now());
        testTask.setUpdatedAt(LocalDateTime.now());
        
        testTaskMapper.insert(testTask);
        
        // 3. 【新增】匹配相关测试用例
        List<TestCase> matchedTestCases = matchTestCases(request, recommendation);
        
        // 4. 【新增】建立任务-用例关联
        if (!matchedTestCases.isEmpty()) {
            List<TaskTestCase> associations = new ArrayList<>();
            for (int i = 0; i < matchedTestCases.size(); i++) {
                TaskTestCase association = new TaskTestCase();
                association.setTaskId(testTask.getId());
                association.setTestCaseId(matchedTestCases.get(i).getId());
                association.setExecutionOrder(i + 1);
                associations.add(association);
            }
            taskTestCaseMapper.batchInsert(associations);
            log.info("Associated {} test cases with task {}", matchedTestCases.size(), testTask.getId());
        }
        
        // 5. 返回完整的响应（包含用例列表）
        return convertToResponseWithTestCases(testTask, recommendation, matchedTestCases);
    }
    
    /**
     * 【新增】匹配相关测试用例
     */
    private List<TestCase> matchTestCases(CreateTestTaskRequest request, 
                                          TestTaskResponse.TestRecommendation recommendation) {
        List<TestCase> allTestCases = new ArrayList<>();
        
        // 策略1: 按模块匹配
        for (String module : request.getModules()) {
            List<TestCase> moduleCases = testCaseMapper.selectByModule(module);
            allTestCases.addAll(moduleCases);
        }
        
        // 策略2: 根据测试范围过滤
        String scope = recommendation.getRecommendedScope();
        List<TestCase> filteredCases = filterByTestScope(allTestCases, scope);
        
        // 策略3: 按优先级排序
        filteredCases.sort((a, b) -> b.getPriority().compareTo(a.getPriority()));
        
        // 策略4: 根据用例状态过滤（只选择已批准的）
        filteredCases = filteredCases.stream()
            .filter(tc -> "APPROVED".equals(tc.getStatus()))
            .collect(Collectors.toList());
        
        log.info("Matched {} test cases for task", filteredCases.size());
        return filteredCases;
    }
    
    /**
     * 根据测试范围过滤用例
     */
    private List<TestCase> filterByTestScope(List<TestCase> testCases, String scope) {
        switch (scope) {
            case "SMOKE":
                // 冒烟测试：只选择P0高优先级用例
                return testCases.stream()
                    .filter(tc -> tc.getPriority() >= 8)
                    .limit(20)  // 限制数量
                    .collect(Collectors.toList());
                
            case "CORE":
                // 核心回归：选择P0-P1用例
                return testCases.stream()
                    .filter(tc -> tc.getPriority() >= 5)
                    .limit(50)
                    .collect(Collectors.toList());
                
            case "FULL":
                // 全量回归：所有用例
                return testCases;
                
            default:
                return testCases;
        }
    }
    
    /**
     * 【新增】转换为包含用例列表的响应
     */
    private TestTaskResponse convertToResponseWithTestCases(
            TestTask testTask,
            TestTaskResponse.TestRecommendation recommendation,
            List<TestCase> testCases) {
        
        TestTaskResponse response = new TestTaskResponse();
        response.setId(testTask.getId());
        response.setName(testTask.getName());
        response.setTaskName(testTask.getName());
        response.setDescription(testTask.getDescription());
        response.setEnvironment(testTask.getEnvironment());
        response.setVersion(testTask.getVersion());
        response.setTestScope(testTask.getTestScope());
        response.setStatus(testTask.getStatus());
        response.setPriority(testTask.getPriority());
        response.setCreatedAt(testTask.getCreatedAt());
        response.setUpdatedAt(testTask.getUpdatedAt());
        response.setCreatedBy(testTask.getCreatedBy());
        response.setRecommendation(recommendation);
        
        // 添加用例列表
        List<TestTaskResponse.TestCaseInfo> testCaseInfos = testCases.stream()
            .map(tc -> {
                TestTaskResponse.TestCaseInfo info = new TestTaskResponse.TestCaseInfo();
                info.setId(tc.getId());
                info.setCaseNumber(tc.getCaseNumber());
                info.setTitle(tc.getTitle());
                info.setModule(tc.getModule());
                info.setPriority(tc.getPriority());
                info.setType(tc.getType());
                return info;
            })
            .collect(Collectors.toList());
        
        response.setTestCases(testCaseInfos);
        response.setTotalTestCases(testCaseInfos.size());
        
        return response;
    }
    
    /**
     * 【新增】获取任务的测试用例列表
     */
    public List<TestCase> getTestCasesByTaskId(String taskId) {
        return taskTestCaseMapper.selectTestCasesByTaskId(taskId);
    }
}
```

#### 2.4 扩展 TestCaseMapper

```java
// backend/src/main/java/com/synapsetest/testmanagement/mapper/TestCaseMapper.java
@Mapper
public interface TestCaseMapper {
    // ... 现有方法 ...
    
    /**
     * 【新增】按模块查询用例
     */
    List<TestCase> selectByModule(@Param("module") String module);
    
    /**
     * 【新增】按模块和优先级查询
     */
    List<TestCase> selectByModuleAndPriority(
        @Param("module") String module, 
        @Param("minPriority") Integer minPriority
    );
}
```

```xml
<!-- mapper/TestCaseMapper.xml 新增查询 -->
<select id="selectByModule" resultMap="TestCaseResultMap">
    SELECT * FROM test_cases 
    WHERE module = #{module} 
    AND status = 'APPROVED'
    ORDER BY priority DESC, created_at DESC
</select>

<select id="selectByModuleAndPriority" resultMap="TestCaseResultMap">
    SELECT * FROM test_cases 
    WHERE module = #{module} 
    AND priority >= #{minPriority}
    AND status = 'APPROVED'
    ORDER BY priority DESC
</select>
```

### 3. 前端实现

#### 3.1 扩展创建任务表单

```jsx
// frontend/src/components/test-task/CreateTestTask.jsx

const CreateTestTask = () => {
  const [form] = Form.useForm()
  const [loading, setLoading] = useState(false)
  const [matchedTestCases, setMatchedTestCases] = useState([])  // 新增：匹配的用例
  const [selectedTestCases, setSelectedTestCases] = useState([]) // 新增：选中的用例
  
  // 【新增】模块变化时自动匹配用例
  const handleModulesChange = async (modules) => {
    if (modules && modules.length > 0) {
      try {
        // 调用后端API预览匹配的用例
        const testCases = await testTaskService.previewTestCases({
          modules,
          environment: form.getFieldValue('environment'),
          version: form.getFieldValue('version'),
        })
        setMatchedTestCases(testCases)
      } catch (error) {
        console.error('Failed to load test cases:', error)
      }
    } else {
      setMatchedTestCases([])
    }
  }
  
  return (
    <Card title="创建测试任务">
      <Form form={form} onFinish={handleSubmit}>
        {/* 现有字段... */}
        
        <Form.Item label="测试模块" name="modules">
          <Select 
            mode="multiple" 
            onChange={handleModulesChange}  // 新增
          >
            {modules.map(m => <Option key={m} value={m}>{m}</Option>)}
          </Select>
        </Form.Item>
        
        {/* 【新增】显示匹配的测试用例 */}
        {matchedTestCases.length > 0 && (
          <Card 
            title={`匹配的测试用例 (${matchedTestCases.length}个)`}
            size="small"
            style={{ marginBottom: 16 }}
          >
            <Table
              dataSource={matchedTestCases}
              rowKey="id"
              size="small"
              rowSelection={{
                type: 'checkbox',
                selectedRowKeys: selectedTestCases,
                onChange: setSelectedTestCases,
              }}
              columns={[
                { title: '用例编号', dataIndex: 'caseNumber', width: 120 },
                { title: '用例标题', dataIndex: 'title' },
                { title: '模块', dataIndex: 'module', width: 100 },
                { 
                  title: '优先级', 
                  dataIndex: 'priority', 
                  width: 80,
                  render: (p) => <Tag color={p >= 8 ? 'red' : p >= 5 ? 'orange' : 'blue'}>P{p}</Tag>
                },
                { title: '类型', dataIndex: 'type', width: 100 },
              ]}
              pagination={{ pageSize: 10 }}
            />
          </Card>
        )}
        
        <Form.Item>
          <Button type="primary" htmlType="submit" loading={loading}>
            创建任务
          </Button>
        </Form.Item>
      </Form>
    </Card>
  )
}
```

#### 3.2 创建任务详情页面（新增）

```jsx
// frontend/src/components/test-task/TestTaskDetail.jsx

const TestTaskDetail = ({ taskId }) => {
  const [task, setTask] = useState(null)
  const [testCases, setTestCases] = useState([])
  const [loading, setLoading] = useState(false)
  
  useEffect(() => {
    loadTaskDetail()
  }, [taskId])
  
  const loadTaskDetail = async () => {
    setLoading(true)
    try {
      const taskResponse = await testTaskService.getTestTaskById(taskId)
      const casesResponse = await testTaskService.getTestCases(taskId)
      
      setTask(taskResponse.data)
      setTestCases(casesResponse.data)
    } catch (error) {
      message.error('Failed to load task detail')
    } finally {
      setLoading(false)
    }
  }
  
  return (
    <Card title="任务详情" loading={loading}>
      <Descriptions bordered>
        <Descriptions.Item label="任务名称">{task?.name}</Descriptions.Item>
        <Descriptions.Item label="状态">{task?.status}</Descriptions.Item>
        <Descriptions.Item label="环境">{task?.environment}</Descriptions.Item>
        <Descriptions.Item label="版本">{task?.version}</Descriptions.Item>
        <Descriptions.Item label="测试范围">{task?.testScope}</Descriptions.Item>
        <Descriptions.Item label="用例总数">{testCases.length}</Descriptions.Item>
      </Descriptions>
      
      <Divider>关联的测试用例</Divider>
      
      <Table
        dataSource={testCases}
        rowKey="id"
        columns={[
          { title: '执行顺序', dataIndex: 'executionOrder', width: 80 },
          { title: '用例编号', dataIndex: 'caseNumber', width: 120 },
          { title: '用例标题', dataIndex: 'title' },
          { title: '模块', dataIndex: 'module', width: 100 },
          { title: '优先级', dataIndex: 'priority', width: 80 },
          { title: '类型', dataIndex: 'type', width: 100 },
        ]}
      />
    </Card>
  )
}
```

### 4. API 端点扩展

```java
// backend/src/main/java/com/synapsetest/testmanagement/controller/TestTaskController.java

@RestController
@RequestMapping(ApiVersion.V1 + "/test-tasks")
public class TestTaskController {
    
    // ... 现有方法 ...
    
    /**
     * 【新增】预览匹配的测试用例（不创建任务）
     */
    @PostMapping("/preview-test-cases")
    public ResponseEntity<ApiResponse<List<TestCaseResponse>>> previewTestCases(
            @RequestBody PreviewTestCasesRequest request) {
        List<TestCase> testCases = testTaskService.previewMatchedTestCases(request);
        List<TestCaseResponse> responses = testCases.stream()
            .map(this::convertToTestCaseResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(responses));
    }
    
    /**
     * 【新增】获取任务关联的测试用例
     */
    @GetMapping("/{id}/test-cases")
    public ResponseEntity<ApiResponse<List<TestCaseResponse>>> getTestCasesByTaskId(
            @PathVariable String id) {
        List<TestCase> testCases = testTaskService.getTestCasesByTaskId(id);
        List<TestCaseResponse> responses = testCases.stream()
            .map(this::convertToTestCaseResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(responses));
    }
    
    /**
     * 【新增】更新任务关联的测试用例
     */
    @PutMapping("/{id}/test-cases")
    public ResponseEntity<ApiResponse<Void>> updateTaskTestCases(
            @PathVariable String id,
            @RequestBody UpdateTaskTestCasesRequest request) {
        testTaskService.updateTaskTestCases(id, request.getTestCaseIds());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
```

## 实施优先级

### Phase 1: 核心功能（P0 - 必须实现）
1. ✅ 创建 TaskTestCaseMapper 和 XML
2. ✅ 扩展 TestTaskService.createTestTask() 实现用例匹配
3. ✅ 基础用例匹配策略（按模块、优先级、状态）
4. ✅ 前端展示关联的用例列表

### Phase 2: 用户体验优化（P1 - 重要）
1. ⭕ 前端支持预览匹配的用例
2. ⭕ 用户可以手动选择/取消用例
3. ⭕ 显示任务详情页（包含用例列表）
4. ⭕ 支持调整用例执行顺序

### Phase 3: 智能推荐（P2 - 增强）
1. ⭕ AI 智能推荐相关用例
2. ⭕ 基于历史数据推荐
3. ⭕ 用例去重和优化
4. ⭕ 推荐理由说明

## 测试用例

### 单元测试

```java
@Test
void testCreateTaskWithTestCases() {
    // Given
    CreateTestTaskRequest request = new CreateTestTaskRequest();
    request.setTaskName("测试任务");
    request.setModules(Arrays.asList("用户认证", "订单中心"));
    request.setEnvironment("DEV");
    
    // When
    TestTaskResponse response = testTaskService.createTestTask(request, "user1");
    
    // Then
    assertNotNull(response);
    assertNotNull(response.getTestCases());
    assertTrue(response.getTotalTestCases() > 0);
    
    // Verify associations created
    List<String> caseIds = taskTestCaseMapper.selectTestCaseIdsByTaskId(response.getId());
    assertEquals(response.getTotalTestCases(), caseIds.size());
}
```

### 集成测试

```java
@Test
void testTaskExecutionWithTestCases() {
    // 1. 创建任务（自动关联用例）
    TestTaskResponse task = testTaskService.createTestTask(request, "user1");
    assertEquals("PENDING", task.getStatus());
    
    // 2. 启动任务
    TestTaskResponse runningTask = testTaskService.startTestTask(task.getId());
    assertEquals("RUNNING", runningTask.getStatus());
    
    // 3. 验证用例已关联
    List<TestCase> testCases = testTaskService.getTestCasesByTaskId(task.getId());
    assertFalse(testCases.isEmpty());
    
    // 4. 模拟执行用例...
}
```

## 数据库迁移

```sql
-- 验证关联表是否存在
SELECT * FROM task_test_cases;

-- 如果表不存在，执行创建
CREATE TABLE IF NOT EXISTS task_test_cases (
    task_id CHAR(36) NOT NULL,
    test_case_id CHAR(36) NOT NULL,
    execution_order INTEGER,
    PRIMARY KEY (task_id, test_case_id),
    FOREIGN KEY (task_id) REFERENCES test_tasks(id) ON DELETE CASCADE,
    FOREIGN KEY (test_case_id) REFERENCES test_cases(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建索引
CREATE INDEX idx_task_test_cases_task_id ON task_test_cases(task_id);
CREATE INDEX idx_task_test_cases_test_case_id ON task_test_cases(test_case_id);
```

## 总结

当前系统确实**只是创建了任务记录**，完整的测试任务管理应该包括：

1. ✅ **任务创建** - 已实现
2. ❌ **用例匹配** - 未实现（核心缺失）
3. ❌ **关联管理** - 未实现
4. ❌ **执行计划** - 未实现
5. ❌ **结果追踪** - 未实现

建议**优先实现 Phase 1 的核心功能**，使测试任务真正具有可执行性。

---

**创建日期**: 2024-12-16  
**分析人**: Claude AI Assistant  
**状态**: 待实现
