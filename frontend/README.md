## 项目更新记录

### 2025-12-14 - 统一后端API返回格式
- 修复了"用例列表"页面显示 'Failed to load test cases' 的问题
- 问题原因: 后端部分接口未使用统一的 ApiResponse 包装格式
- 解决方案: 修改后端 TestCaseController.java，统一使用 ApiResponse 格式:
  - `GET /api/v1/test-cases` - 返回 `{success: true, data: {content: [...], total: 21}}`
  - `DELETE /api/v1/test-cases/{id}` - 返回 `{success: true, message: "...", data: null}`
- 前端保持原有的响应处理逻辑不变

### 2025-12-14 - 修复测试用例保存约束冲突
- 修复了保存测试用例到数据库时的约束冲突问题
- 问题原因: 数据库test_cases表的type字段有CHECK约束,仅允许: FUNCTIONAL, PERFORMANCE, SECURITY
- AI服务返回的类型为中文(如"安全测试"),导致约束冲突
- 解决方案: 新增 convertType() 函数,将中文类型转换为英文枚举值:
  - 功能测试/功能 → FUNCTIONAL
  - 性能测试/性能 → PERFORMANCE
  - 安全测试/安全 → SECURITY
- 在保存时自动转换type字段值

### 2025-12-14 - 智能生成功能增加保存到库
- 在"智能生成"页面增加了复选框,支持选择需要保存的测试用例
- 实现了"保存到库"按钮功能,调用后端接口将测试用例保存到 MySQL 数据库
- 支持全选/取消全选操作
- 添加了数据转换逻辑:
  - 优先级转换: P0→10, P1→8, P2→5, P3→3
  - 测试步骤转换: 将对象数组转换为字符串数组
  - 类型转换: 中文类型转换为英文枚举值
- 集成后端接口: `POST /api/v1/test-cases/batch`
- 显示选中数量和保存进度

### 2025-12-14 - 新增智能生成功能
- 新增 `SmartGenerate` 组件 (frontend/src/components/test-case/SmartGenerate.tsx)
- 在左侧菜单"测试用例"下新增"智能生成"菜单项
- 实现三步流程:
  1. 第1步: 输入需求文档、模块名称、生成数量等参数
  2. 第2步: AI生成中,显示进度条和状态提示
  3. 第3步: 查看生成结果,包括测试用例详情、统计数据等
- 集成后端接口: POST `/api/v1/ai/testcase/generate`
- 支持自动去重、优先级排序等高级配置
- 参考 `frontend/docs/UI原型演示.html` 的设计风格

### 2025-12-14 - 新增顶部导航栏
- 新增 `AppHeader` 组件 (frontend/src/components/layout/AppHeader.tsx)
- 顶部导航栏包含以下功能:
  - Logo 和平台名称展示
  - 项目选择器 (支持切换不同项目)
  - 通知中心 (带消息数量徽章)
  - 帮助中心
  - 用户信息下拉菜单
- 参考 `frontend/docs/UI原型演示.html` 的设计风格
- 采用深色背景 (#001529) 与侧边栏统一

## 开发规范

## 运行方式

### 以独立项目运行

```
// 配置node版本
nvm use 22
node --version

// 初始化项目
npm install 

// 开发模式
yarn run dev 或者 yarn dev 或者 npm run dev

<!-- 本地联调注意 -->
修改vite.config.ts 和 nginx.conf 文件中对应的地址