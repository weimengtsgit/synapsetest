# 快速开始 - 测试任务与用例关联功能

## 🚀 5分钟快速体验

### 步骤 1: 数据库准备 (30秒)

```bash
# 进入项目目录
cd /Users/mengwei/ww/github/synapsetest

# 执行迁移脚本，添加 task_test_cases 表
mysql -u root -p synapsetest < database/migrations/add_task_test_cases_table.sql

# 输入数据库密码后，看到 "✅ task_test_cases 表迁移完成!"
```

**验证表创建成功**:
```sql
USE synapsetest;
DESCRIBE task_test_cases;
SHOW INDEX FROM task_test_cases;
```

### 步骤 2: 启动后端服务 (1分钟)

```bash
# 进入后端目录
cd backend

# 编译项目
mvn clean compile

# 启动服务
mvn spring-boot:run

# 等待看到: "Started TestManagementApplication"
```

### 步骤 3: 启动前端服务 (30秒)

```bash
# 打开新终端，进入前端目录
cd frontend

# 启动开发服务器
npm run dev

# 等待看到: "Local: http://localhost:5173/"
```

### 步骤 4: 浏览器测试 (2分钟)

1. **打开浏览器**: http://localhost:5173

2. **导航到创建任务页面**:
   ```
   侧边栏 → Test Tasks → Create Task
   ```

3. **填写表单**:
   ```
   任务名称: 订单模块冒烟测试
   测试环境: DEV
   测试版本: （选择一个）
   测试模块: 用户认证, 订单中心
   ```

4. **预览测试用例** 🆕:
   - 点击"预览匹配的测试用例"按钮
   - 查看系统匹配的用例列表
   - 观察用例的优先级、模块、类型

5. **填写代码变更信息**:
   ```
   变更文件数: 5
   变更行数: 100
   是否热修复: 否
   是否核心模块: 是
   ```

6. **创建任务**:
   - 点击"创建任务"按钮
   - 看到成功消息: "Test task created successfully!"
   - 注意消息会显示: "Task created with X test cases"

7. **查看任务列表**:
   ```
   侧边栏 → Test Tasks → Task List
   ```
   - 看到刚创建的任务
   - 注意每个任务都显示关联的用例数量

8. **查看任务详情** 🆕:
   - 点击任务的"查看"按钮
   - 查看任务基本信息
   - 查看 AI 推荐信息
   - **查看关联的测试用例列表**（新功能）

## 🎬 API 测试（可选）

如果您更喜欢使用 API 测试：

### 1. 预览测试用例

```bash
curl -X POST http://localhost:8080/api/v1/test-tasks/preview-test-cases \
  -H "Content-Type: application/json" \
  -d '{
    "taskName": "快速测试",
    "environment": "DEV",
    "version": "v1.0.0",
    "modules": ["用户认证"],
    "codeChangeInfo": {
      "changed_files_count": 5,
      "changed_lines_count": 100,
      "is_hotfix": false,
      "is_critical_module": false
    }
  }'
```

**期望输出**: 看到匹配的测试用例列表

### 2. 创建任务

```bash
curl -X POST http://localhost:8080/api/v1/test-tasks \
  -H "Content-Type: application/json" \
  -H "X-User-Id: testuser" \
  -d '{
    "taskName": "API测试任务",
    "environment": "DEV",
    "version": "v1.0.0",
    "modules": ["订单中心"],
    "codeChangeInfo": {
      "changed_files_count": 5,
      "changed_lines_count": 100,
      "is_hotfix": false,
      "is_critical_module": false
    }
  }'
```

**期望输出**: 
```json
{
  "success": true,
  "message": "Test task created successfully",
  "data": {
    "id": "xxx",
    "name": "API测试任务",
    "totalTestCases": 5,
    "testCases": [...]
  }
}
```

### 3. 查看任务用例

```bash
# 使用上一步返回的任务ID
curl http://localhost:8080/api/v1/test-tasks/{taskId}/test-cases
```

## ✅ 验收检查

完成上述步骤后，请确认：

- [x] 数据库中 `task_test_cases` 表已创建
- [x] 后端服务正常启动，无报错
- [x] 前端页面正常显示
- [x] 可以预览匹配的测试用例
- [x] 创建任务时显示关联的用例数量
- [x] 任务详情页显示用例列表
- [x] 用例按执行顺序正确排序

## 🎓 关键概念

### 测试任务 (Test Task)
一个测试执行计划，包含：
- 基本信息：名称、环境、版本
- 执行配置：测试范围、优先级
- **关联用例**：要执行哪些测试用例 ← 新增！

### 测试用例 (Test Case)
一个具体的测试步骤和预期结果，可以被多个任务关联

### 关联关系 (Association)
- **多对多关系**: 一个任务可以关联多个用例，一个用例可以被多个任务使用
- **执行顺序**: 每个用例在任务中有明确的执行顺序
- **自动匹配**: 系统根据规则自动建立关联

## 📊 数据流

```
用户创建任务
    ↓
填写模块、环境、版本
    ↓
【预览】查看将要关联的用例
    ↓
确认创建
    ↓
后端处理:
  1. 创建任务记录
  2. AI 推荐测试策略
  3. 匹配相关测试用例
     - 按模块查找
     - 按范围过滤
     - 按优先级排序
  4. 批量插入关联记录
  5. 设置执行顺序
    ↓
返回完整任务信息（包含用例列表）
    ↓
前端显示成功消息和用例数量
```

## 🔍 调试技巧

### 查看日志

**后端日志**:
```bash
# 查看任务创建日志
grep "Creating test task" backend/logs/application.log

# 查看用例匹配日志
grep "Matching test cases" backend/logs/application.log

# 查看关联记录插入日志
grep "Associated.*test cases" backend/logs/application.log
```

**前端控制台**:
```javascript
// 打开浏览器开发者工具 (F12)
// Console 标签页查看:
// - API 请求日志
// - 响应数据结构
// - 错误信息
```

### 数据库查询

```sql
-- 查看最新创建的任务及其用例
SELECT 
    t.name AS task_name,
    t.test_scope,
    COUNT(ttc.test_case_id) AS case_count,
    t.created_at
FROM test_tasks t
LEFT JOIN task_test_cases ttc ON t.id = ttc.task_id
WHERE t.status != 'CANCELLED'
GROUP BY t.id, t.name, t.test_scope, t.created_at
ORDER BY t.created_at DESC
LIMIT 5;

-- 查看具体的关联详情
SELECT 
    t.name AS task_name,
    tc.case_number,
    tc.title AS case_title,
    tc.module,
    tc.priority,
    ttc.execution_order
FROM task_test_cases ttc
JOIN test_tasks t ON ttc.task_id = t.id
JOIN test_cases tc ON ttc.test_case_id = tc.id
WHERE t.id = 'your-task-id-here'
ORDER BY ttc.execution_order;
```

## 💡 使用建议

1. **首次使用**: 先用预览功能熟悉匹配逻辑
2. **测试范围**: 根据实际需求选择 SMOKE/CORE/FULL
3. **模块选择**: 精确选择相关模块，避免匹配过多无关用例
4. **优先级设置**: 确保测试用例的优先级准确反映重要性

## 🎉 恭喜！

您已经成功设置并测试了**测试任务与测试用例关联功能**！

现在您的测试任务不再是空壳，而是真正具备可执行性的测试计划！

---

**下一步**: 尝试创建不同测试范围的任务，观察用例匹配的差异

**问题反馈**: 如遇到问题，请查看完整文档或联系开发团队
