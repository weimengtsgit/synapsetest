# 部署检查清单 - 任务用例关联功能

## 📋 部署前检查

### 1. 代码检查 ✅

- [x] 所有新建文件已提交到 Git
- [x] 所有修改文件已保存
- [x] 无编译错误
- [x] 无 linter 警告
- [x] 代码审查通过

### 2. 数据库准备 ⚠️ 必须执行

#### 方式 1: 使用迁移脚本（推荐）

```bash
# 如果数据库已初始化，只需添加新表
mysql -u root -p synapsetest < database/migrations/add_task_test_cases_table.sql
```

#### 方式 2: 完整初始化

```bash
# 如果是全新数据库
mysql -u root -p < database/unified_schema.sql
```

**验证脚本**:
```sql
USE synapsetest;

-- 1. 检查表是否存在
SHOW TABLES LIKE 'task_test_cases';

-- 2. 检查表结构
DESCRIBE task_test_cases;

-- 3. 检查外键约束
SELECT 
    CONSTRAINT_NAME,
    TABLE_NAME,
    COLUMN_NAME,
    REFERENCED_TABLE_NAME,
    REFERENCED_COLUMN_NAME
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = 'synapsetest'
AND TABLE_NAME = 'task_test_cases'
AND REFERENCED_TABLE_NAME IS NOT NULL;

-- 4. 检查索引
SHOW INDEX FROM task_test_cases;

-- 预期结果: 应该看到 3 个索引和 2 个外键约束
```

### 3. 依赖检查 ✅

#### 后端依赖
```bash
cd backend
mvn dependency:tree | grep -E "(mybatis|mysql)"
```

预期输出应包含:
- `mybatis-spring-boot-starter:2.3.1`
- `mysql-connector-j:8.0.33`

#### 前端依赖
```bash
cd frontend
npm list | grep -E "(antd|axios|react)"
```

预期输出应包含:
- `antd@5.x`
- `axios@1.x`
- `react@18.x`

## 🚀 部署步骤

### 步骤 1: 数据库迁移

```bash
# 执行迁移
mysql -u root -p synapsetest < database/migrations/add_task_test_cases_table.sql

# 验证成功
mysql -u root -p synapsetest -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_name='task_test_cases' AND table_schema='synapsetest';"
```

**期望输出**: `1`

### 步骤 2: 编译后端

```bash
cd backend

# 清理并编译
mvn clean compile

# 运行测试（可选）
mvn test

# 打包
mvn package -DskipTests
```

**期望输出**: `BUILD SUCCESS`

### 步骤 3: 启动后端服务

```bash
# 开发环境
mvn spring-boot:run

# 或使用 jar 包
java -jar target/test-management-0.0.1-SNAPSHOT.jar
```

**验证启动成功**:
```bash
# 健康检查
curl http://localhost:8080/health

# 预期输出: {"status":"UP"}
```

### 步骤 4: 构建前端

```bash
cd frontend

# 安装依赖（如果需要）
npm install

# 开发环境
npm run dev

# 生产环境构建
npm run build
```

**验证启动成功**:
- 开发环境: http://localhost:5173
- 生产环境: 查看 `dist` 目录

### 步骤 5: 功能验证

#### API 测试

```bash
# 1. 测试预览功能
curl -X POST http://localhost:8080/api/v1/test-tasks/preview-test-cases \
  -H "Content-Type: application/json" \
  -d '{
    "taskName": "测试",
    "environment": "DEV",
    "version": "v1.0.0",
    "modules": ["用户认证"],
    "codeChangeInfo": {
      "changed_files_count": 5,
      "changed_lines_count": 100,
      "is_hotfix": false,
      "is_critical_module": false
    }
  }' | jq '.success'

# 预期输出: true

# 2. 测试创建任务
curl -X POST http://localhost:8080/api/v1/test-tasks \
  -H "Content-Type: application/json" \
  -H "X-User-Id: admin" \
  -d '{...}' | jq '.data.totalTestCases'

# 预期输出: 一个数字（如 5）

# 3. 测试查询用例
TASK_ID=$(curl -s -X POST http://localhost:8080/api/v1/test-tasks -H "Content-Type: application/json" -H "X-User-Id: admin" -d '{...}' | jq -r '.data.id')
curl http://localhost:8080/api/v1/test-tasks/$TASK_ID/test-cases | jq '.data | length'

# 预期输出: 一个数字（关联的用例数量）
```

#### UI 测试

1. 打开浏览器: http://localhost:5173
2. 导航到: Test Tasks → Create Task
3. 填写表单并点击"预览测试用例"
4. 确认看到匹配的用例表格
5. 创建任务
6. 导航到: Test Tasks → Task List
7. 点击某个任务的"查看"按钮
8. 确认看到任务详情和关联的用例列表

## ⚠️ 注意事项

### 数据库迁移

**重要**: 必须先执行数据库迁移再启动服务！

如果启动后端时看到以下错误:
```
Table 'synapsetest.task_test_cases' doesn't exist
```

**解决方案**: 执行迁移脚本

### 兼容性

**后端**:
- Java 11+
- Spring Boot 2.7.18
- MyBatis 2.3.1
- MySQL 8.0+

**前端**:
- Node.js 16+
- React 18.2+
- Ant Design 5.11+

### 性能考虑

**大量用例场景**:
- SMOKE: 限制 20 个用例
- CORE: 限制 50 个用例
- FULL: 建议不超过 200 个

如果某个模块用例过多，考虑:
1. 提高优先级阈值
2. 拆分模块
3. 使用批量操作

## 🧪 测试场景

### 场景 1: 正常流程

```
1. 创建任务，选择1个模块
2. 预览用例，看到5-10个用例
3. 创建成功
4. 查看详情，用例列表正确显示
```

### 场景 2: 多模块

```
1. 创建任务，选择3个模块
2. 预览用例，看到15-30个用例
3. 观察去重是否正确
4. 创建成功
```

### 场景 3: 无用例

```
1. 创建任务，选择一个没有用例的模块
2. 预览用例，看到空列表
3. 创建仍然成功，totalTestCases = 0
4. 系统提示警告信息
```

### 场景 4: 不同测试范围

```
1. 创建3个任务，相同模块，不同代码变更
2. AI 推荐不同的测试范围 (SMOKE/CORE/FULL)
3. 观察匹配的用例数量差异
4. 验证过滤逻辑正确
```

## 📊 监控指标

部署后建议监控:

### 业务指标
- 创建任务的平均关联用例数
- 预览功能使用率
- 用例匹配成功率
- 不同测试范围的任务分布

### 性能指标
- 用例匹配耗时
- 批量插入耗时
- API 响应时间

### SQL 查询
```sql
-- 统计最近24小时创建的任务
SELECT 
    test_scope,
    COUNT(*) as task_count,
    AVG(total_cases) as avg_cases
FROM (
    SELECT 
        t.test_scope,
        COUNT(ttc.test_case_id) as total_cases
    FROM test_tasks t
    LEFT JOIN task_test_cases ttc ON t.id = ttc.task_id
    WHERE t.created_at >= DATE_SUB(NOW(), INTERVAL 24 HOUR)
    AND t.status != 'CANCELLED'
    GROUP BY t.id, t.test_scope
) AS stats
GROUP BY test_scope;
```

## 🔧 故障排查

### 问题 1: 表不存在错误

**错误信息**: `Table 'synapsetest.task_test_cases' doesn't exist`

**解决方案**:
```bash
mysql -u root -p synapsetest < database/migrations/add_task_test_cases_table.sql
```

### 问题 2: 外键约束失败

**错误信息**: `Cannot add foreign key constraint`

**解决方案**:
1. 确认 `test_tasks` 和 `test_cases` 表存在
2. 确认表引擎是 InnoDB
3. 重新执行完整的 unified_schema.sql

### 问题 3: 预览功能返回空列表

**原因**:
- 选择的模块没有测试用例
- 测试用例状态不是 APPROVED
- 用例的 module 字段为空

**解决方案**:
```sql
-- 检查模块的用例数量
SELECT module, COUNT(*) 
FROM test_cases 
WHERE status = 'APPROVED'
GROUP BY module;

-- 如果某个模块为空，手动插入测试用例
```

### 问题 4: 前端显示异常

**检查步骤**:
1. 打开浏览器开发者工具 (F12)
2. 查看 Console 标签页的错误信息
3. 查看 Network 标签页的 API 响应
4. 确认响应格式符合预期

## 📞 支持与反馈

### 文档位置
```
docs/claudegc/
├── FIX_CREATE_TASK_API_RESPONSE.md          # API修复文档
├── TASK_TESTCASE_ASSOCIATION_TODO.md        # 设计文档
├── TASK_TESTCASE_IMPLEMENTATION_COMPLETE.md # 实施文档
├── TASK_TESTCASE_USAGE_GUIDE.md             # 使用指南
├── QUICK_START_TASK_TESTCASE.md             # 快速开始
├── SUMMARY_COMPLETE_IMPLEMENTATION.md       # 完整总结
└── DEPLOYMENT_CHECKLIST.md                  # 本文档
```

### 关键文件
```
backend/
├── model/TaskTestCase.java                        # 新建
├── mapper/TaskTestCaseMapper.java                 # 新建
├── resources/mapper/TaskTestCaseMapper.xml        # 新建
└── service/TestTaskService.java                   # 重写

frontend/
├── components/test-task/CreateTestTask.jsx        # 重写
├── components/test-task/TestTaskDetail.jsx        # 新建
└── services/testTaskService.js                    # 扩展

database/
├── unified_schema.sql                             # 更新
└── migrations/add_task_test_cases_table.sql       # 新建
```

## ✅ 最终确认

部署完成后，请确认：

- [ ] 数据库 `task_test_cases` 表已创建
- [ ] 后端服务正常启动
- [ ] 前端页面正常访问
- [ ] 可以预览测试用例
- [ ] 可以创建任务并看到用例数量
- [ ] 可以查看任务详情和用例列表
- [ ] API 响应格式正确
- [ ] 用例执行顺序正确

全部通过后，**功能即可正式上线使用**！🎉

---

**版本**: v1.0  
**日期**: 2024-12-16  
**状态**: ✅ 准备就绪
