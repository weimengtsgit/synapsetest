# AI智能测试平台 - API接口测试方案

**文档版本**: v1.0
**创建日期**: 2025-12-04
**适用范围**: testcase.py 和 recommendation.py API接口测试
**项目**: SynapseTest AI驱动测试任务管理系统

---

## 一、方案概述

### 1.1 测试目标
- 验证 **testcase.py** 和 **recommendation.py** 两个API文件的所有接口功能
- 确保接口与前端页面功能设计一致
- 覆盖正常场景、异常场景、边界场景
- 验证数据预置和业务流程的完整性

### 1.2 测试范围

#### testcase.py 接口 (7个)
1. `POST /testcase/generate` - 生成测试用例
2. `POST /testcase/generate/batch` - 批量生成测试用例
3. `POST /testcase/feedback` - 提交用户反馈
4. `POST /testcase/optimize/deduplicate` - 测试用例去重
5. `POST /testcase/optimize/prioritize` - 优先级排序
6. `POST /testcase/analyze/quality` - 质量分析
7. `GET /testcase/health` - 健康检查

#### recommendation.py 接口 (3个)
1. `POST /recommendation/strategy` - 推荐测试策略
2. `POST /recommendation/strategy/explain` - 解释推荐
3. `GET /recommendation/health` - 健康检查

---

## 二、API接口功能分析

### 2.1 testcase.py 接口详细分析

#### 接口1: POST /testcase/generate (生成测试用例)

**功能描述**: 基于需求文档生成结构化测试用例

**对应页面**: 前端UI设计方案 3.2 - 用例生成页面(单个生成)

**请求参数**:
```json
{
  "requirement_text": "string (必填, 最少10字符)",
  "module": "string (选填, 默认'unknown')",
  "num_cases": "int (选填, 默认5, 范围1-50)",
  "include_edge_cases": "bool (选填, 默认true)",
  "optimization": {
    "deduplicate": "bool (选填, 默认true)",
    "prioritize": "bool (选填, 默认true)",
    "min_priority": "string (选填, P0-P3)",
    "max_cases": "int (选填, >=1)"
  }
}
```

**预期响应**:
```json
{
  "request_id": "string",
  "generated_cases": [
    {
      "name": "string",
      "priority": "P0/P1/P2/P3",
      "type": "string",
      "preconditions": ["string"],
      "steps": [
        {"step": 1, "action": "string", "expected": "string"}
      ],
      "tags": ["string"]
    }
  ],
  "generation_time": "float",
  "model_info": "object"
}
```

#### 接口2: POST /testcase/generate/batch (批量生成)

**功能描述**: 批量生成多个模块的测试用例

**对应页面**: 前端UI设计方案 3.3 - 批量生成页面

**请求参数**:
```json
{
  "requirements": [
    {
      "requirement_text": "string",
      "module": "string",
      "num_cases": "int",
      "include_edge_cases": "bool",
      "optimization": "object"
    }
  ]
}
```

#### 接口3: POST /testcase/feedback (提交反馈)

**功能描述**: 提交用户对生成用例的反馈,用于模型优化

**对应页面**: 前端UI设计方案 3.2 - 用例生成页面第3步(反馈部分)

**请求参数**:
```json
{
  "request_id": "string (必填)",
  "rating": "int (必填, 1-5)",
  "comments": "string (选填)",
  "accepted_cases": ["string"],
  "rejected_cases": ["string"]
}
```

#### 接口4: POST /testcase/optimize/deduplicate (去重)

**功能描述**: 使用语义相似度分析去除重复用例

**对应页面**: 前端UI设计方案 3.6 - 用例优化页面(智能去重)

**请求参数**:
```json
{
  "testcases": [
    {
      "name": "string",
      "steps": [{"action": "string", "expected": "string"}],
      "priority": "string"
    }
  ],
  "threshold": "float (默认0.85, 范围0.0-1.0)"
}
```

#### 接口5: POST /testcase/optimize/prioritize (优先级排序)

**功能描述**: 多因子评分,智能排序测试用例优先级

**对应页面**: 前端UI设计方案 3.6 - 用例优化页面(优先级排序)

**请求参数**:
```json
{
  "testcases": [
    {
      "name": "string",
      "priority": "string",
      "steps": "array"
    }
  ],
  "custom_weights": {
    "business_value": "float (选填)",
    "risk_level": "float (选填)"
  }
}
```

#### 接口6: POST /testcase/analyze/quality (质量分析)

**功能描述**: 分析测试用例质量指标

**对应页面**: 前端UI设计方案 3.2 - 用例生成页面第3步(用例质量评估)

**请求参数**:
```json
[
  {
    "name": "string",
    "steps": "array",
    "preconditions": "array"
  }
]
```

### 2.2 recommendation.py 接口详细分析

#### 接口1: POST /recommendation/strategy (推荐策略)

**功能描述**: 基于多维度因素推荐最优测试策略

**对应页面**: 前端UI设计方案 3.4 - 测试策略推荐页面

**请求参数**:
```json
{
  "task_id": "string (必填)",
  "context": {
    "code_change": {
      "changed_files_count": "int (>=0)",
      "changed_lines_count": "int (>=0)",
      "code_complexity_delta": "float",
      "test_coverage_delta": "float",
      "changed_modules": ["string"],
      "change_type": "string (feature/bugfix/hotfix)"
    },
    "historical": {
      "recent_pass_rate": "float (0.0-1.0, 默认0.95)",
      "avg_execution_time": "float (>=0.0, 默认30.0)",
      "recent_defect_count": "int (>=0, 默认0)",
      "failure_frequency": "float (0.0-1.0, 默认0.05)"
    },
    "business": {
      "module_importance": "float (0.0-1.0, 默认0.5)",
      "business_priority": "string (P0-P3, 默认P2)",
      "release_urgency": "string (默认normal)"
    },
    "environment": {
      "available_resources": "int (>=1, 默认5)",
      "queue_length": "int (>=0, 默认0)",
      "env_stability_score": "float (0.0-1.0, 默认0.95)",
      "current_load": "float (0.0-1.0, 默认0.5)"
    }
  }
}
```

**预期响应**:
```json
{
  "task_id": "string",
  "recommendation": {
    "test_scope": "SMOKE/CORE/FULL",
    "environment": "DEV/STAGING/PROD",
    "priority": "int (0-10)",
    "estimated_duration": "int (分钟)",
    "resource_requirement": "int",
    "confidence": "float (0-1)",
    "reasoning": ["string"]
  },
  "risk_assessment": {
    "risk_level": "LOW/MEDIUM/HIGH/CRITICAL",
    "risk_factors": ["string"]
  },
  "environment_recommendations": ["string"],
  "timestamp": "string"
}
```

#### 接口2: POST /recommendation/strategy/explain (解释推荐)

**功能描述**: 详细解释推荐决策的依据

**对应页面**: 前端UI设计方案 3.4 - 推荐解释部分(点击"为什么这样推荐"后展开)

---

## 三、业务场景测试用例设计

### 场景1: 用户登录模块用例生成流程

**业务背景**: 测试工程师需要为"用户登录功能"生成测试用例

**页面操作流程**:
1. 进入"用例生成"页面
2. 输入需求文档
3. 配置生成参数
4. 点击"开始生成"
5. 等待AI生成
6. 查看生成结果 + 质量评估
7. 编辑和保存用例
8. 提交反馈

**对应测试用例**:
- **TC-001**: 生成用户登录模块用例 (对应步骤1-6: 完整生成流程)
- **TC-020**: 分析用例质量 (对应步骤6: 质量评估)
- **TC-012**: 5星好评反馈 (对应步骤8: 提交反馈)
- **TC-013**: 部分接受的反馈 (对应步骤8: 部分满意场景)

### 场景2: 支付模块批量生成流程

**业务背景**: 需要为多个支付相关模块批量生成测试用例

**页面操作流程**:
1. 进入"批量生成"页面
2. 上传Excel或手动添加多个需求
3. 配置批量参数
4. 开始批量生成
5. 查看生成进度
6. 分别查看各模块结果 + 优化处理

**对应测试用例**:
- **TC-009**: 批量生成3个模块 (对应步骤1-6: 正常批量生成流程)
- **TC-010**: 批量生成最大数量(20个模块) (对应步骤4: 边界场景)
- **TC-015**: 去重相似用例 (对应步骤6: 批量生成后去重优化)
- **TC-018**: 优先级排序 (对应步骤6: 批量生成后优先级优化)

### 场景3: 测试策略推荐流程

**业务背景**: 代码提交后需要AI推荐测试策略

**页面操作流程**:
1. 进入"策略推荐"页面
2. 输入任务信息和代码变更信息
3. 点击"获取推荐"
4. 查看推荐结果和风险评估
5. 点击"为什么这样推荐"查看详细解释
6. 应用推荐创建测试计划

**对应测试用例**:
- **TC-022**: 推荐高风险模块测试策略 (对应步骤1-4: 高风险场景完整流程)
- **TC-023**: 推荐低风险模块测试策略 (对应步骤1-4: 低风险场景)
- **TC-024**: 中风险推荐 (对应步骤1-4: 中风险场景)
- **TC-027**: 解释高风险推荐 (对应步骤5: 查看推荐解释)

### 场景4: 用例优化流程

**业务背景**: 用例库积累了大量用例,需要去重和优化

**页面操作流程**:
1. 进入"用例优化"页面
2. 选择用例范围
3. 执行智能去重分析
4. 查看重复用例详情
5. 确认保留或删除
6. 执行优先级排序

**对应测试用例**:
- **TC-015**: 去重相似用例 (对应步骤3-5: 正常去重流程, 阈值0.85)
- **TC-016**: 相似度阈值为1.0(最严格) (对应步骤3: 边界场景)
- **TC-017**: 相似度阈值为0.0(最宽松) (对应步骤3: 边界场景)
- **TC-018**: 默认权重排序 (对应步骤6: 优先级排序)
- **TC-019**: 自定义权重排序 (对应步骤6: 自定义权重场景)

---

## 四、详细测试用例列表

### 4.1 testcase.py - 生成测试用例接口

#### TC-001: 正常场景 - 生成用户登录模块用例

**场景**: 用户登录功能用例生成(对应前端页面操作)

**请求方法**: POST
**请求URL**: `/testcase/generate`

**请求Body**:
```json
{
  "requirement_text": "用户登录功能需求：\n1. 用户可以通过手机号+验证码登录\n2. 支持微信、支付宝第三方登录\n3. 登录失败3次后锁定账户30分钟\n4. 支持记住登录状态7天\n5. 密码需要加密存储",
  "module": "用户认证模块",
  "num_cases": 10,
  "include_edge_cases": true,
  "optimization": {
    "deduplicate": true,
    "prioritize": true,
    "min_priority": "P1",
    "max_cases": 20
  }
}
```

**预期结果**:
- HTTP状态码: 200
- 返回request_id
- 返回10条左右测试用例(去重后可能少于10条)
- 每条用例包含: name, priority, type, preconditions, steps, tags
- generation_time < 60秒
- 用例优先级 >= P1
- 至少包含1-2条边界用例(如:登录失败3次锁定)

---

#### TC-002: 正常场景 - 生成支付模块用例

**请求Body**:
```json
{
  "requirement_text": "在线支付功能需求：\n1. 支持支付宝、微信、银行卡支付\n2. 支付金额范围: 0.01元-50000元\n3. 支付失败后支持重试\n4. 支付成功后发送通知\n5. 支持支付退款功能",
  "module": "支付系统",
  "num_cases": 15,
  "include_edge_cases": true,
  "optimization": {
    "deduplicate": true,
    "prioritize": true
  }
}
```

**预期结果**:
- 返回15条左右测试用例
- 包含边界金额测试(0.01元, 50000元)
- 包含支付失败重试场景
- 包含退款场景用例

---

#### TC-003: 边界场景 - 最小需求文本长度

**请求Body**:
```json
{
  "requirement_text": "测试功能需求最小长度",
  "module": "测试模块",
  "num_cases": 5
}
```

**预期结果**:
- HTTP状态码: 200
- 能够生成用例(但质量可能较低)

---

#### TC-004: 异常场景 - 需求文本过短(少于10字符)

**请求Body**:
```json
{
  "requirement_text": "测试",
  "module": "测试",
  "num_cases": 5
}
```

**预期结果**:
- HTTP状态码: 422 (Validation Error)
- 错误信息提示: requirement_text至少10个字符

---

#### TC-005: 异常场景 - num_cases超出范围

**请求Body**:
```json
{
  "requirement_text": "用户注册功能需求：需要验证手机号、邮箱、密码强度",
  "module": "用户认证",
  "num_cases": 100
}
```

**预期结果**:
- HTTP状态码: 422
- 错误信息提示: num_cases必须在1-50之间

---

#### TC-006: 异常场景 - 缺少必填参数

**请求Body**:
```json
{
  "module": "测试模块",
  "num_cases": 5
}
```

**预期结果**:
- HTTP状态码: 422
- 错误信息提示: requirement_text是必填字段

---

#### TC-007: 边界场景 - 生成最大数量用例

**请求Body**:
```json
{
  "requirement_text": "电商平台订单管理功能需求：\n1. 创建订单\n2. 修改订单\n3. 取消订单\n4. 订单支付\n5. 订单发货\n6. 订单收货\n7. 订单评价\n8. 订单退款\n9. 订单查询\n10. 订单导出",
  "module": "订单管理",
  "num_cases": 50,
  "include_edge_cases": true
}
```

**预期结果**:
- 返回50条左右用例
- 覆盖所有10个功能点

---

#### TC-008: 正常场景 - 不包含边界用例

**请求Body**:
```json
{
  "requirement_text": "商品搜索功能需求：\n1. 支持关键词搜索\n2. 支持分类筛选\n3. 支持价格排序\n4. 支持销量排序",
  "module": "搜索引擎",
  "num_cases": 8,
  "include_edge_cases": false
}
```

**预期结果**:
- 返回8条左右用例
- 主要覆盖正常流程,较少边界场景

---

### 4.2 testcase.py - 批量生成接口

#### TC-009: 正常场景 - 批量生成3个模块

**场景**: 批量生成页面 - 添加多个模块批量生成

**请求方法**: POST
**请求URL**: `/testcase/generate/batch`

**请求Body**:
```json
{
  "requirements": [
    {
      "requirement_text": "用户登录功能：手机号+验证码登录,支持第三方登录",
      "module": "用户认证",
      "num_cases": 10,
      "include_edge_cases": true
    },
    {
      "requirement_text": "支付功能：支持支付宝、微信支付,金额范围0.01-50000元",
      "module": "支付系统",
      "num_cases": 15,
      "include_edge_cases": true
    },
    {
      "requirement_text": "订单管理：创建订单、修改订单、取消订单、查询订单",
      "module": "订单中心",
      "num_cases": 12,
      "include_edge_cases": true
    }
  ]
}
```

**预期结果**:
- HTTP状态码: 200
- 返回3个模块的生成结果
- 每个模块都有独立的request_id
- 总用例数约37条(10+15+12)

---

#### TC-010: 边界场景 - 批量生成最大数量(20个模块)

**请求Body**:
```json
{
  "requirements": [
    {"requirement_text": "模块1需求：测试功能1", "module": "模块1", "num_cases": 5},
    {"requirement_text": "模块2需求：测试功能2", "module": "模块2", "num_cases": 5},
    {"requirement_text": "模块3需求：测试功能3", "module": "模块3", "num_cases": 5},
    {"requirement_text": "模块4需求：测试功能4", "module": "模块4", "num_cases": 5},
    {"requirement_text": "模块5需求：测试功能5", "module": "模块5", "num_cases": 5},
    {"requirement_text": "模块6需求：测试功能6", "module": "模块6", "num_cases": 5},
    {"requirement_text": "模块7需求：测试功能7", "module": "模块7", "num_cases": 5},
    {"requirement_text": "模块8需求：测试功能8", "module": "模块8", "num_cases": 5},
    {"requirement_text": "模块9需求：测试功能9", "module": "模块9", "num_cases": 5},
    {"requirement_text": "模块10需求：测试功能10", "module": "模块10", "num_cases": 5},
    {"requirement_text": "模块11需求：测试功能11", "module": "模块11", "num_cases": 5},
    {"requirement_text": "模块12需求：测试功能12", "module": "模块12", "num_cases": 5},
    {"requirement_text": "模块13需求：测试功能13", "module": "模块13", "num_cases": 5},
    {"requirement_text": "模块14需求：测试功能14", "module": "模块14", "num_cases": 5},
    {"requirement_text": "模块15需求：测试功能15", "module": "模块15", "num_cases": 5},
    {"requirement_text": "模块16需求：测试功能16", "module": "模块16", "num_cases": 5},
    {"requirement_text": "模块17需求：测试功能17", "module": "模块17", "num_cases": 5},
    {"requirement_text": "模块18需求：测试功能18", "module": "模块18", "num_cases": 5},
    {"requirement_text": "模块19需求：测试功能19", "module": "模块19", "num_cases": 5},
    {"requirement_text": "模块20需求：测试功能20", "module": "模块20", "num_cases": 5}
  ]
}
```

**预期结果**:
- HTTP状态码: 200
- 成功处理20个模块

---

#### TC-011: 异常场景 - 批量生成超过最大数量

**请求Body**:
```json
{
  "requirements": [
    {"requirement_text": "模块1需求：测试功能1", "module": "模块1", "num_cases": 5},
    {"requirement_text": "模块2需求：测试功能2", "module": "模块2", "num_cases": 5},
    ...21个模块
  ]
}
```

**预期结果**:
- HTTP状态码: 422
- 错误信息提示: requirements最多20项

---

### 4.3 testcase.py - 提交反馈接口

#### TC-012: 正常场景 - 5星好评反馈

**场景**: 用例生成页面第3步 - 用户对生成结果很满意

**请求方法**: POST
**请求URL**: `/testcase/feedback`

**请求Body**:
```json
{
  "request_id": "req_abc123",
  "rating": 5,
  "comments": "生成的用例非常完整,覆盖了所有场景,步骤清晰,直接可用!",
  "accepted_cases": ["TC001", "TC002", "TC003", "TC004", "TC005"],
  "rejected_cases": []
}
```

**预期结果**:
- HTTP状态码: 200
- 返回success标志
- 反馈已记录到数据库

---

#### TC-013: 正常场景 - 部分接受的反馈

**请求Body**:
```json
{
  "request_id": "req_def456",
  "rating": 3,
  "comments": "部分用例还可以,但有些用例步骤不够详细,需要修改",
  "accepted_cases": ["TC001", "TC002"],
  "rejected_cases": ["TC005", "TC008"]
}
```

**预期结果**:
- HTTP状态码: 200
- 反馈已记录

---

#### TC-014: 异常场景 - rating超出范围

**请求Body**:
```json
{
  "request_id": "req_ghi789",
  "rating": 6,
  "comments": "测试",
  "accepted_cases": [],
  "rejected_cases": []
}
```

**预期结果**:
- HTTP状态码: 422
- 错误信息: rating必须在1-5之间

---

### 4.4 testcase.py - 去重接口

#### TC-015: 正常场景 - 去重相似用例

**场景**: 用例优化页面 - 智能去重

**请求方法**: POST
**请求URL**: `/testcase/optimize/deduplicate`

**请求Body**:
```json
{
  "testcases": [
    {
      "name": "手机号+验证码正常登录",
      "steps": [
        {"step": 1, "action": "打开登录页面", "expected": "页面正常显示"},
        {"step": 2, "action": "输入手机号13800138000", "expected": "手机号格式正确"},
        {"step": 3, "action": "点击获取验证码", "expected": "收到验证码"},
        {"step": 4, "action": "输入验证码并登录", "expected": "登录成功"}
      ],
      "priority": "P0"
    },
    {
      "name": "测试用户登录功能",
      "steps": [
        {"step": 1, "action": "进入登录页", "expected": "显示登录表单"},
        {"step": 2, "action": "填写手机号13800138000", "expected": "手机号输入成功"},
        {"step": 3, "action": "获取短信验证码", "expected": "验证码发送成功"},
        {"step": 4, "action": "提交验证码登录", "expected": "成功登录系统"}
      ],
      "priority": "P0"
    },
    {
      "name": "支付宝支付成功",
      "steps": [
        {"step": 1, "action": "选择支付宝支付", "expected": "跳转支付宝"},
        {"step": 2, "action": "完成支付", "expected": "支付成功"}
      ],
      "priority": "P0"
    }
  ],
  "threshold": 0.85
}
```

**预期结果**:
- HTTP状态码: 200
- 识别出前两个用例相似度>85%
- 返回去重后的用例列表(2条)
- 返回duplicate_groups信息

---

#### TC-016: 边界场景 - 相似度阈值为1.0(最严格)

**请求Body**:
```json
{
  "testcases": [
    {
      "name": "手机号+验证码正常登录",
      "steps": [
        {"step": 1, "action": "打开登录页面", "expected": "页面正常显示"},
        {"step": 2, "action": "输入手机号13800138000", "expected": "手机号格式正确"}
      ],
      "priority": "P0"
    },
    {
      "name": "测试用户登录功能",
      "steps": [
        {"step": 1, "action": "进入登录页", "expected": "显示登录表单"},
        {"step": 2, "action": "填写手机号13800138000", "expected": "手机号输入成功"}
      ],
      "priority": "P0"
    },
    {
      "name": "支付宝支付成功",
      "steps": [
        {"step": 1, "action": "选择支付宝支付", "expected": "跳转支付宝"},
        {"step": 2, "action": "完成支付", "expected": "支付成功"}
      ],
      "priority": "P0"
    }
  ],
  "threshold": 1.0
}
```

**预期结果**:
- 不去重任何用例(因为没有完全相同的)
- 返回3条用例

---

#### TC-017: 边界场景 - 相似度阈值为0.0(最宽松)

**请求Body**:
```json
{
  "testcases": [
    {
      "name": "手机号+验证码正常登录",
      "steps": [{"step": 1, "action": "打开登录页面", "expected": "页面正常显示"}],
      "priority": "P0"
    },
    {
      "name": "测试用户登录功能",
      "steps": [{"step": 1, "action": "进入登录页", "expected": "显示登录表单"}],
      "priority": "P0"
    },
    {
      "name": "支付宝支付成功",
      "steps": [{"step": 1, "action": "选择支付宝支付", "expected": "跳转支付宝"}],
      "priority": "P0"
    }
  ],
  "threshold": 0.0
}
```

**预期结果**:
- 可能会去除所有用例(因为任何用例都相似)
- 返回1条用例

---

### 4.5 testcase.py - 优先级排序接口

#### TC-018: 正常场景 - 默认权重排序

**场景**: 用例优化页面 - 优先级排序

**请求方法**: POST
**请求URL**: `/testcase/optimize/prioritize`

**请求Body**:
```json
{
  "testcases": [
    {
      "name": "用户登录",
      "priority": "P0",
      "steps": [{"action": "登录", "expected": "成功"}],
      "module": "认证",
      "business_value": 0.9
    },
    {
      "name": "修改个人资料",
      "priority": "P2",
      "steps": [{"action": "修改", "expected": "成功"}],
      "module": "用户",
      "business_value": 0.3
    },
    {
      "name": "支付订单",
      "priority": "P0",
      "steps": [{"action": "支付", "expected": "成功"}],
      "module": "支付",
      "business_value": 1.0
    }
  ]
}
```

**预期结果**:
- HTTP状态码: 200
- 返回排序后的用例列表
- 每条用例包含priority_score
- 顺序大致为: 支付订单 > 用户登录 > 修改个人资料

---

#### TC-019: 正常场景 - 自定义权重排序

**请求Body**:
```json
{
  "testcases": [
    {
      "name": "用户登录",
      "priority": "P0",
      "steps": [{"action": "登录", "expected": "成功"}]
    },
    {
      "name": "修改个人资料",
      "priority": "P2",
      "steps": [{"action": "修改", "expected": "成功"}]
    },
    {
      "name": "支付订单",
      "priority": "P0",
      "steps": [{"action": "支付", "expected": "成功"}]
    }
  ],
  "custom_weights": {
    "business_value": 0.5,
    "risk_level": 0.3,
    "execution_cost": 0.2
  }
}
```

**预期结果**:
- 使用自定义权重计算priority_score
- 返回排序后的用例

---

### 4.6 testcase.py - 质量分析接口

#### TC-020: 正常场景 - 分析用例质量

**场景**: 用例生成页面第3步 - 用例质量评估

**请求方法**: POST
**请求URL**: `/testcase/analyze/quality`

**请求Body**:
```json
[
  {
    "name": "手机号+验证码正常登录",
    "steps": [
      {"step": 1, "action": "打开登录页面", "expected": "页面正常显示"},
      {"step": 2, "action": "输入手机号13800138000", "expected": "手机号格式正确"},
      {"step": 3, "action": "点击获取验证码", "expected": "收到验证码"},
      {"step": 4, "action": "输入验证码并登录", "expected": "登录成功"}
    ],
    "preconditions": ["用户已注册", "系统正常运行"],
    "priority": "P0"
  },
  {
    "name": "测试",
    "steps": [
      {"step": 1, "action": "操作", "expected": "结果"}
    ],
    "preconditions": [],
    "priority": "P3"
  }
]
```

**预期结果**:
- HTTP状态码: 200
- 返回质量指标:
  - completeness (完整性): 第1条>90%, 第2条<50%
  - coverage (覆盖率): 整体评分
  - executability (可执行性): 第1条高, 第2条低

---

### 4.7 testcase.py - 健康检查接口

#### TC-021: 正常场景 - 健康检查

**请求方法**: GET
**请求URL**: `/testcase/health`

**预期结果**:
- HTTP状态码: 200
- 返回:
```json
{
  "status": "UP",
  "service": "testcase-generation",
  "llm_available": true
}
```

---

### 4.8 recommendation.py - 推荐策略接口

#### TC-022: 正常场景 - 推荐高风险模块测试策略

**场景**: 测试策略推荐页面 - 支付模块代码变更后推荐测试策略

**请求方法**: POST
**请求URL**: `/recommendation/strategy`

**请求Body**:
```json
{
  "task_id": "TASK-2024-001",
  "context": {
    "code_change": {
      "changed_files_count": 8,
      "changed_lines_count": 350,
      "code_complexity_delta": 0.25,
      "test_coverage_delta": -0.05,
      "changed_modules": ["payment", "order"],
      "change_type": "feature"
    },
    "historical": {
      "recent_pass_rate": 0.88,
      "avg_execution_time": 65.0,
      "recent_defect_count": 5,
      "failure_frequency": 0.12
    },
    "business": {
      "module_importance": 0.95,
      "business_priority": "P0",
      "release_urgency": "urgent"
    },
    "environment": {
      "available_resources": 3,
      "queue_length": 2,
      "env_stability_score": 0.90,
      "current_load": 0.65
    }
  }
}
```

**预期结果**:
- HTTP状态码: 200
- 推荐结果:
  - test_scope: "FULL" (完整回归)
  - environment: "STAGING" 或 "PROD"
  - priority: 9-10 (最高优先级)
  - risk_assessment.risk_level: "HIGH" 或 "CRITICAL"
  - reasoning包含关键理由(支付模块高风险、P0优先级等)

---

#### TC-023: 正常场景 - 推荐低风险模块测试策略

**请求Body**:
```json
{
  "task_id": "TASK-2024-002",
  "context": {
    "code_change": {
      "changed_files_count": 2,
      "changed_lines_count": 30,
      "code_complexity_delta": 0.05,
      "test_coverage_delta": 0.02,
      "changed_modules": ["ui", "css"],
      "change_type": "bugfix"
    },
    "historical": {
      "recent_pass_rate": 0.98,
      "avg_execution_time": 20.0,
      "recent_defect_count": 0,
      "failure_frequency": 0.02
    },
    "business": {
      "module_importance": 0.3,
      "business_priority": "P3",
      "release_urgency": "low"
    },
    "environment": {
      "available_resources": 8,
      "queue_length": 0,
      "env_stability_score": 0.99,
      "current_load": 0.20
    }
  }
}
```

**预期结果**:
- test_scope: "SMOKE" (冒烟测试)
- environment: "DEV"
- priority: 2-3 (低优先级)
- risk_assessment.risk_level: "LOW"
- estimated_duration: < 30分钟

---

#### TC-024: 正常场景 - 中风险推荐

**请求Body**:
```json
{
  "task_id": "TASK-2024-003",
  "context": {
    "code_change": {
      "changed_files_count": 5,
      "changed_lines_count": 150,
      "code_complexity_delta": 0.1,
      "test_coverage_delta": 0.0,
      "changed_modules": ["user", "profile"],
      "change_type": "feature"
    },
    "historical": {
      "recent_pass_rate": 0.92,
      "avg_execution_time": 45.0,
      "recent_defect_count": 2,
      "failure_frequency": 0.08
    },
    "business": {
      "module_importance": 0.6,
      "business_priority": "P1",
      "release_urgency": "normal"
    },
    "environment": {
      "available_resources": 5,
      "queue_length": 3,
      "env_stability_score": 0.93,
      "current_load": 0.50
    }
  }
}
```

**预期结果**:
- test_scope: "CORE" (核心测试)
- risk_assessment.risk_level: "MEDIUM"
- priority: 5-7

---

#### TC-025: 异常场景 - 缺少必填字段task_id

**请求Body**:
```json
{
  "context": {
    "code_change": {
      "changed_files_count": 5,
      "changed_lines_count": 150
    }
  }
}
```

**预期结果**:
- HTTP状态码: 422
- 错误信息: task_id是必填字段

---

#### TC-026: 边界场景 - 所有指标都是边界值

**请求Body**:
```json
{
  "task_id": "TASK-2024-EDGE",
  "context": {
    "code_change": {
      "changed_files_count": 0,
      "changed_lines_count": 0,
      "code_complexity_delta": 0.0,
      "test_coverage_delta": 0.0,
      "changed_modules": [],
      "change_type": "feature"
    },
    "historical": {
      "recent_pass_rate": 1.0,
      "avg_execution_time": 0.0,
      "recent_defect_count": 0,
      "failure_frequency": 0.0
    },
    "business": {
      "module_importance": 0.0,
      "business_priority": "P3",
      "release_urgency": "low"
    },
    "environment": {
      "available_resources": 1,
      "queue_length": 0,
      "env_stability_score": 1.0,
      "current_load": 0.0
    }
  }
}
```

**预期结果**:
- HTTP状态码: 200
- 能够给出推荐(可能是SMOKE测试)

---

### 4.9 recommendation.py - 解释推荐接口

#### TC-027: 正常场景 - 解释高风险推荐

**场景**: 策略推荐页面 - 点击"为什么这样推荐"

**请求方法**: POST
**请求URL**: `/recommendation/strategy/explain`

**请求Body**:
```json
{
  "recommendation": {
    "test_scope": "FULL",
    "environment": "STAGING",
    "priority": 9,
    "estimated_duration": 135,
    "resource_requirement": 5
  },
  "context": {
    "code_change": {
      "changed_files_count": 8,
      "changed_lines_count": 350,
      "changed_modules": ["payment", "order"]
    },
    "business": {
      "business_priority": "P0"
    }
  }
}
```

**预期结果**:
- HTTP状态码: 200
- 返回详细解释:
  - success: true
  - explanation: 包含多个维度的分析说明
    - 代码变更影响分析
    - 历史数据分析
    - 业务优先级分析
    - 资源与环境分析

---

### 4.10 recommendation.py - 健康检查

#### TC-028: 正常场景 - 健康检查

**请求方法**: GET
**请求URL**: `/recommendation/health`

**预期结果**:
- HTTP状态码: 200
- 返回:
```json
{
  "status": "UP",
  "service": "recommendation",
  "models_loaded": true
}
```

---

## 五、数据预置方案

### 5.1 MySQL数据预置

基于文档分析,需要预置以下数据:

#### 表1: test_cases (测试用例表)

```sql
CREATE TABLE test_cases (
  id VARCHAR(50) PRIMARY KEY,
  name VARCHAR(200) NOT NULL,
  module VARCHAR(100),
  priority ENUM('P0', 'P1', 'P2', 'P3'),
  type VARCHAR(50),
  preconditions JSON,
  steps JSON,
  tags JSON,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  created_by VARCHAR(50),
  quality_score FLOAT
);

-- 预置数据
INSERT INTO test_cases VALUES
('TC001', '手机号+验证码正常登录', '用户认证', 'P0', '功能测试',
 '["用户已注册", "系统正常运行"]',
 '[{"step":1,"action":"打开登录页面","expected":"页面正常显示"},{"step":2,"action":"输入手机号13800138000","expected":"手机号格式正确"},{"step":3,"action":"点击获取验证码","expected":"收到验证码"},{"step":4,"action":"输入验证码并登录","expected":"登录成功"}]',
 '["登录", "认证", "验证码"]',
 NOW(), 'admin', 0.95),

('TC002', '微信第三方登录成功', '用户认证', 'P0', '功能测试',
 '["用户已有微信账号", "微信授权正常"]',
 '[{"step":1,"action":"点击微信登录按钮","expected":"跳转微信授权页"},{"step":2,"action":"确认授权","expected":"自动登录成功"}]',
 '["登录", "第三方登录", "微信"]',
 NOW(), 'admin', 0.92),

('TC003', '支付宝支付成功', '支付系统', 'P0', '功能测试',
 '["用户已登录", "订单已创建", "支付宝账户余额充足"]',
 '[{"step":1,"action":"选择支付宝支付","expected":"跳转支付宝页面"},{"step":2,"action":"确认支付","expected":"支付成功并跳转"},{"step":3,"action":"查看订单状态","expected":"订单状态为已支付"}]',
 '["支付", "支付宝"]',
 NOW(), 'admin', 0.98),

('TC004', '订单创建成功', '订单中心', 'P1', '功能测试',
 '["用户已登录", "购物车有商品"]',
 '[{"step":1,"action":"进入购物车","expected":"显示商品列表"},{"step":2,"action":"点击结算","expected":"进入订单确认页"},{"step":3,"action":"确认订单信息","expected":"订单信息正确"},{"step":4,"action":"提交订单","expected":"订单创建成功"}]',
 '["订单", "创建"]',
 NOW(), 'admin', 0.88);
```

#### 表2: generation_history (生成历史表)

```sql
CREATE TABLE generation_history (
  request_id VARCHAR(50) PRIMARY KEY,
  user_id VARCHAR(50),
  requirement_text TEXT,
  module VARCHAR(100),
  num_cases INT,
  generated_count INT,
  generation_time FLOAT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 预置数据
INSERT INTO generation_history VALUES
('req_abc123', 'user001', '用户登录功能需求', '用户认证', 10, 10, 23.5, NOW()),
('req_def456', 'user001', '支付功能需求', '支付系统', 15, 14, 35.2, NOW()),
('req_ghi789', 'user002', '订单管理需求', '订单中心', 12, 12, 28.8, NOW());
```

#### 表3: recommendation_history (推荐历史表)

```sql
CREATE TABLE recommendation_history (
  id INT AUTO_INCREMENT PRIMARY KEY,
  task_id VARCHAR(50),
  recommendation JSON,
  risk_assessment JSON,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 预置数据
INSERT INTO recommendation_history (task_id, recommendation, risk_assessment) VALUES
('TASK-2024-001',
 '{"test_scope":"FULL","environment":"STAGING","priority":9}',
 '{"risk_level":"HIGH","risk_factors":["支付模块历史缺陷率高","代码复杂度上升"]}'),

('TASK-2024-002',
 '{"test_scope":"SMOKE","environment":"DEV","priority":3}',
 '{"risk_level":"LOW","risk_factors":[]}');
```

#### 表4: user_feedback (用户反馈表)

```sql
CREATE TABLE user_feedback (
  id INT AUTO_INCREMENT PRIMARY KEY,
  request_id VARCHAR(50),
  rating INT,
  comments TEXT,
  accepted_cases JSON,
  rejected_cases JSON,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 5.2 向量数据库(Milvus/Chroma)预置

向量数据库用于存储历史用例的embedding,用于RAG检索。

#### Collection: historical_testcases

**Schema**:
```python
{
  "id": "string",  # 用例ID
  "name": "string",  # 用例名称
  "module": "string",  # 模块
  "text": "string",  # 用例完整文本(用于embedding)
  "embedding": "vector(768)",  # 使用Sentence-BERT生成的768维向量
  "priority": "string",
  "tags": "list"
}
```

**预置数据导入脚本**:

```python
# scripts/init_vector_db.py
from sentence_transformers import SentenceTransformer
from pymilvus import connections, Collection, FieldSchema, CollectionSchema, DataType
import json

# 连接Milvus
connections.connect("default", host="localhost", port="19530")

# 定义Schema
fields = [
    FieldSchema(name="id", dtype=DataType.VARCHAR, is_primary=True, max_length=50),
    FieldSchema(name="name", dtype=DataType.VARCHAR, max_length=200),
    FieldSchema(name="module", dtype=DataType.VARCHAR, max_length=100),
    FieldSchema(name="text", dtype=DataType.VARCHAR, max_length=2000),
    FieldSchema(name="embedding", dtype=DataType.FLOAT_VECTOR, dim=768),
    FieldSchema(name="priority", dtype=DataType.VARCHAR, max_length=10),
]

schema = CollectionSchema(fields, description="Historical test cases")
collection = Collection("historical_testcases", schema)

# 加载模型
model = SentenceTransformer('paraphrase-multilingual-mpnet-base-v2')

# 准备数据
testcases = [
    {
        "id": "TC001",
        "name": "手机号+验证码正常登录",
        "module": "用户认证",
        "text": "手机号+验证码正常登录。前置条件:用户已注册。步骤:1.打开登录页面 2.输入手机号 3.获取验证码 4.登录成功",
        "priority": "P0"
    },
    {
        "id": "TC002",
        "name": "微信第三方登录",
        "module": "用户认证",
        "text": "微信第三方登录成功。前置条件:用户已有微信账号。步骤:1.点击微信登录 2.授权 3.登录成功",
        "priority": "P0"
    },
    {
        "id": "TC003",
        "name": "支付宝支付成功",
        "module": "支付系统",
        "text": "支付宝支付成功。前置条件:订单已创建,余额充足。步骤:1.选择支付宝 2.确认支付 3.支付成功",
        "priority": "P0"
    },
    {
        "id": "TC004",
        "name": "订单创建成功",
        "module": "订单中心",
        "text": "订单创建成功。前置条件:用户已登录,购物车有商品。步骤:1.进入购物车 2.点击结算 3.确认信息 4.提交订单",
        "priority": "P1"
    },
    {
        "id": "TC005",
        "name": "商品搜索精确匹配",
        "module": "搜索引擎",
        "text": "商品搜索精确匹配。步骤:1.输入商品关键词 2.点击搜索 3.查看搜索结果",
        "priority": "P1"
    }
]

# 生成embeddings
texts = [tc["text"] for tc in testcases]
embeddings = model.encode(texts)

# 构造插入数据
data = [
    [tc["id"] for tc in testcases],
    [tc["name"] for tc in testcases],
    [tc["module"] for tc in testcases],
    [tc["text"] for tc in testcases],
    embeddings.tolist(),
    [tc["priority"] for tc in testcases],
]

# 插入数据
collection.insert(data)
collection.flush()

# 创建索引
index_params = {
    "metric_type": "L2",
    "index_type": "IVF_FLAT",
    "params": {"nlist": 128}
}
collection.create_index("embedding", index_params)

print(f"✅ 成功导入 {len(testcases)} 条历史用例到向量数据库")
```

**使用方法**:
```bash
cd ai-service
python scripts/init_vector_db.py
```

### 5.3 数据导入指导

#### 步骤1: 导入MySQL数据

```bash
# 1. 连接MySQL
mysql -u root -p

# 2. 创建数据库
CREATE DATABASE synapsetest CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE synapsetest;

# 3. 执行上面的建表和插入语句
# 将SQL语句保存为 init_mysql.sql 后执行:
source /path/to/init_mysql.sql;

# 4. 验证数据
SELECT COUNT(*) FROM test_cases;
SELECT COUNT(*) FROM generation_history;
```

#### 步骤2: 导入向量数据库数据

```bash
# 1. 确保Milvus服务运行
docker ps | grep milvus

# 2. 安装依赖
pip install pymilvus sentence-transformers

# 3. 运行导入脚本
python ai-service/scripts/init_vector_db.py

# 4. 验证数据
python -c "
from pymilvus import connections, Collection
connections.connect('default', host='localhost', port='19530')
collection = Collection('historical_testcases')
print(f'向量数据库用例数量: {collection.num_entities}')
"
```

#### 步骤3: 验证数据预置成功

```bash
# 测试API能否正常访问数据
curl -X POST http://localhost:8000/testcase/generate \
  -H "Content-Type: application/json" \
  -d '{
    "requirement_text": "测试用户登录功能需求",
    "module": "测试",
    "num_cases": 3
  }'
```

---

## 六、Postman测试集合

### 6.1 环境变量配置

在Postman中创建环境 "AI Test Platform - Dev":

```json
{
  "name": "AI Test Platform - Dev",
  "values": [
    {
      "key": "base_url",
      "value": "http://localhost:8000",
      "enabled": true
    },
    {
      "key": "request_id",
      "value": "",
      "enabled": true
    },
    {
      "key": "task_id",
      "value": "TASK-2024-001",
      "enabled": true
    }
  ]
}
```

### 6.2 测试执行顺序

建议按以下顺序执行测试:

1. **健康检查** (TC-021, TC-028) - 验证服务可用性
2. **生成用例** (TC-001 ~ TC-008) - 测试核心生成功能
3. **提交反馈** (TC-012 ~ TC-014) - 测试反馈机制
4. **批量生成** (TC-009 ~ TC-011) - 测试批量处理
5. **用例优化** (TC-015 ~ TC-019) - 测试优化功能
6. **质量分析** (TC-020) - 测试分析功能
7. **策略推荐** (TC-022 ~ TC-026) - 测试推荐功能
8. **解释推荐** (TC-027) - 测试解释功能

### 6.3 Postman Collection结构

```
AI Test Platform API Tests
├── 1. Health Checks
│   ├── TC-021: TestCase Health Check
│   └── TC-028: Recommendation Health Check
├── 2. TestCase Generation
│   ├── TC-001: Generate Login Module Cases
│   ├── TC-002: Generate Payment Module Cases
│   ├── TC-003: Minimum Requirement Text
│   ├── TC-004: Requirement Text Too Short (Error)
│   ├── TC-005: Num Cases Out of Range (Error)
│   ├── TC-006: Missing Required Field (Error)
│   ├── TC-007: Generate Maximum Cases
│   └── TC-008: Without Edge Cases
├── 3. Batch Generation
│   ├── TC-009: Batch Generate 3 Modules
│   ├── TC-010: Batch Generate Max (20 Modules)
│   └── TC-011: Batch Over Limit (Error)
├── 4. User Feedback
│   ├── TC-012: 5-Star Feedback
│   ├── TC-013: Partial Acceptance
│   └── TC-014: Rating Out of Range (Error)
├── 5. TestCase Optimization
│   ├── TC-015: Deduplicate Similar Cases
│   ├── TC-016: Strict Threshold (1.0)
│   ├── TC-017: Loose Threshold (0.0)
│   ├── TC-018: Default Weight Prioritization
│   └── TC-019: Custom Weight Prioritization
├── 6. Quality Analysis
│   └── TC-020: Analyze Quality
├── 7. Strategy Recommendation
│   ├── TC-022: High Risk Recommendation
│   ├── TC-023: Low Risk Recommendation
│   ├── TC-024: Medium Risk Recommendation
│   ├── TC-025: Missing Task ID (Error)
│   └── TC-026: Boundary Values
└── 8. Explain Recommendation
    └── TC-027: Explain High Risk Recommendation
```

### 6.4 Pre-request Script示例

在测试用例中可以使用Pre-request Script来动态生成数据:

```javascript
// 自动保存request_id到环境变量
pm.sendRequest({
    url: pm.environment.get("base_url") + "/testcase/generate",
    method: 'POST',
    header: {
        'Content-Type': 'application/json',
    },
    body: {
        mode: 'raw',
        raw: JSON.stringify({
            requirement_text: "测试需求",
            module: "测试",
            num_cases: 5
        })
    }
}, function (err, res) {
    if (!err) {
        const response = res.json();
        pm.environment.set("request_id", response.request_id);
    }
});
```

### 6.5 Tests Script示例

在测试用例中使用Tests Script进行自动断言:

```javascript
// TC-001的测试脚本
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Response has request_id", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData).to.have.property('request_id');
    pm.environment.set("request_id", jsonData.request_id);
});

pm.test("Generated cases count >= 1", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.generated_cases.length).to.be.at.least(1);
});

pm.test("Each case has required fields", function () {
    var jsonData = pm.response.json();
    jsonData.generated_cases.forEach(function(testcase) {
        pm.expect(testcase).to.have.property('name');
        pm.expect(testcase).to.have.property('priority');
        pm.expect(testcase).to.have.property('steps');
    });
});

pm.test("Generation time < 60 seconds", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.generation_time).to.be.below(60);
});
```

---

## 七、测试执行建议

### 7.1 测试准备

#### 环境准备清单:
- [ ] ai-service服务已启动 (端口: 8000)
- [ ] MySQL数据库已准备 (端口: 3306)
- [ ] 向量数据库Milvus已准备 (端口: 19530)
- [ ] 大语言模型可用 (Qwen或API配置)
- [ ] Redis已启动 (端口: 6379)

#### 数据准备清单:
- [ ] MySQL数据已导入 (4张表)
- [ ] 向量数据库数据已导入
- [ ] 验证数据导入成功

#### 工具准备清单:
- [ ] Postman已安装
- [ ] 测试集合已导入
- [ ] 环境变量已配置

### 7.2 测试执行

#### 冒烟测试 (快速验证 ~5分钟):
1. TC-021: TestCase服务健康检查
2. TC-028: Recommendation服务健康检查
3. TC-001: 基础用例生成
4. TC-022: 基础策略推荐

**通过标准**: 4个用例全部通过

#### 功能测试 (完整验证 ~2小时):
按照测试执行顺序执行所有28个测试用例:
- 记录每个用例的执行结果
- 截图保存关键响应
- 记录响应时间
- 记录失败原因

#### 场景测试 (端到端 ~1小时):

**场景1: 用户登录模块完整流程**
1. 生成用例 (TC-001)
2. 查看质量分析 (TC-020)
3. 提交反馈 (TC-012)

**场景2: 支付模块批量生成流程**
1. 批量生成3个模块 (TC-009)
2. 去重优化 (TC-015)
3. 优先级排序 (TC-018)

**场景3: 测试策略推荐流程**
1. 高风险推荐 (TC-022)
2. 解释推荐 (TC-027)

**场景4: 用例优化流程**
1. 去重分析 (TC-015)
2. 优先级排序 (TC-018)
3. 质量分析 (TC-020)

### 7.3 测试报告

测试完成后,建议输出以下报告:

#### 1. 测试执行报告

```
测试执行摘要
-----------------
执行日期: 2025-12-04
执行人: XXX
测试环境: Dev

测试统计:
- 总用例数: 28
- 通过: 25
- 失败: 3
- 阻塞: 0
- 跳过: 0
- 通过率: 89.3%

失败用例:
- TC-005: num_cases超出范围 - 预期422,实际500
- TC-014: rating超出范围 - 参数校验未生效
- TC-026: 边界值推荐 - 返回空推荐

执行时长:
- 冒烟测试: 5分钟
- 功能测试: 1小时45分钟
- 场景测试: 55分钟
- 总计: 2小时45分钟
```

#### 2. 性能分析报告

```
API响应时间统计
-----------------
接口名称                          平均响应时间    最大响应时间
/testcase/generate                 23.5s          45.2s
/testcase/generate/batch           68.3s          120.5s
/testcase/feedback                 0.3s           0.8s
/testcase/optimize/deduplicate     5.2s           8.9s
/testcase/optimize/prioritize      1.8s           3.2s
/testcase/analyze/quality          2.5s           4.1s
/recommendation/strategy           3.8s           6.5s
/recommendation/strategy/explain   2.1s           4.0s

性能问题:
- 批量生成耗时过长,需要优化
- 单个生成偶尔超过30秒,需要调查
```

#### 3. 缺陷清单

```
缺陷ID  优先级  模块        描述
------  ------  ----------  ----------------------------------
BUG-001  P1     参数校验    num_cases参数校验失败,返回500而非422
BUG-002  P2     参数校验    rating参数校验未生效
BUG-003  P2     推荐引擎    边界值场景返回空推荐
BUG-004  P3     响应时间    批量生成耗时过长(>2分钟)
```

#### 4. 改进建议

```
1. 接口优化建议:
   - 增强参数校验,确保所有异常返回正确状态码
   - 优化批量生成性能,考虑异步处理
   - 完善边界场景处理逻辑

2. 文档完善建议:
   - 补充错误码说明文档
   - 添加性能基线文档
   - 完善接口调用示例

3. 测试优化建议:
   - 建立自动化测试流程
   - 增加压力测试场景
   - 建立性能监控
```

---

## 八、预期问题和解决方案

### 8.1 可能遇到的问题

#### 问题1: 大模型响应慢

**现象**:
- 生成用例耗时超过60秒
- 批量生成超过3分钟无响应

**可能原因**:
- GPU资源不足
- 模型参数配置不当
- 网络延迟(API调用)

**解决方案**:
1. 调整模型参数: 降低max_tokens、增加temperature
2. 使用模型量化: 8-bit量化减少内存占用
3. 启用缓存: 对相似请求使用缓存结果
4. 优化Prompt: 简化Prompt模板

**验证方法**:
```bash
# 检查GPU使用情况
nvidia-smi

# 查看模型加载日志
tail -f ai-service/logs/app.log | grep "model"
```

---

#### 问题2: 向量检索失败

**现象**:
- 相似用例检索不到结果
- 去重功能异常

**可能原因**:
- Milvus服务未启动
- 索引未创建
- Collection不存在

**解决方案**:
1. 检查Milvus服务:
```bash
docker ps | grep milvus
```

2. 重建索引:
```python
from pymilvus import connections, Collection
connections.connect("default", host="localhost", port="19530")
collection = Collection("historical_testcases")
collection.drop_index()
# 重新创建索引(参考数据预置脚本)
```

3. 验证数据:
```python
collection = Collection("historical_testcases")
print(f"数据量: {collection.num_entities}")
```

---

#### 问题3: 去重效果不理想

**现象**:
- 明显重复的用例没有被去重
- 不相似的用例被误判为重复

**可能原因**:
- 相似度阈值设置不当
- Embedding模型问题
- 用例文本特征提取不充分

**解决方案**:
1. 调整阈值:
   - 提高阈值(0.85 -> 0.90): 更严格,减少误判
   - 降低阈值(0.85 -> 0.80): 更宽松,增加去重

2. 优化特征提取:
```python
# 改进用例文本构造
case_text = f"{tc['name']} {tc['module']} {' '.join([s['action'] for s in tc['steps']])}"
```

3. 测试不同模型:
```python
# 尝试不同的Sentence-BERT模型
model = SentenceTransformer('paraphrase-multilingual-MiniLM-L12-v2')
```

---

#### 问题4: 推荐结果不合理

**现象**:
- 低风险任务被推荐全量测试
- 高风险任务被推荐冒烟测试

**可能原因**:
- 特征值错误
- 权重配置不当
- 规则引擎逻辑问题

**解决方案**:
1. 检查输入特征:
```python
# 打印特征值进行调试
logger.info(f"Features: {features}")
logger.info(f"Prediction: {prediction}")
```

2. 调整权重:
```python
# 在recommendation_service.py中调整权重
weights = {
    'code_change': 0.40,  # 增加代码变更权重
    'historical': 0.30,
    'business': 0.20,
    'environment': 0.10
}
```

3. 验证规则引擎:
```python
# 检查规则引擎是否正确修正了预测结果
if business_priority == "P0" and prediction != "FULL":
    recommendation = "FULL"  # 强制修正
```

---

#### 问题5: MySQL连接失败

**现象**:
- 接口返回500错误
- 日志显示数据库连接错误

**解决方案**:
1. 检查MySQL服务:
```bash
mysql -u root -p -e "SELECT 1"
```

2. 验证配置:
```python
# config.py
MYSQL_URI = "mysql+pymysql://root:password@localhost:3306/synapsetest"
```

3. 测试连接:
```python
from sqlalchemy import create_engine
engine = create_engine(MYSQL_URI)
connection = engine.connect()
print("连接成功")
```

---

### 8.2 测试数据说明

**重要提示**:

1. **request_id是动态生成的**:
   - 在TC-012~TC-014中使用的request_id需要从TC-001的响应中获取
   - 使用Postman的环境变量功能保存request_id

2. **时间戳会自动更新**:
   - SQL中的NOW()会生成当前时间
   - 不需要手动修改

3. **某些字段需要根据实际调整**:
   - 模型路径: QWEN_MODEL_PATH
   - 数据库连接: MYSQL_URI, MONGODB_URI
   - 服务端口: 8000

---

## 九、方案总结

### 9.1 测试覆盖度

| 覆盖维度 | 覆盖情况 |
|---------|---------|
| **接口覆盖** | 10个接口,28个测试用例,覆盖率100% |
| **场景覆盖** | 4个核心业务场景,覆盖前端主要页面操作 |
| **数据覆盖** | 正常数据16例、边界数据7例、异常数据5例 |
| **异常覆盖** | 参数校验、业务异常、系统异常全覆盖 |

### 9.2 测试价值

1. **验证功能完整性**: 确保所有接口按设计实现
2. **保障页面对接**: 测试用例与前端页面操作一致
3. **发现潜在问题**: 边界场景和异常场景测试
4. **性能基线建立**: 记录各接口响应时间基线
5. **数据预置验证**: 确保数据库和向量数据库配置正确

### 9.3 后续优化

#### 自动化测试
```bash
# 使用Newman执行Postman Collection
newman run AI_Test_Platform.postman_collection.json \
  -e Dev.postman_environment.json \
  --reporters cli,html \
  --reporter-html-export report.html
```

#### 压力测试
```python
# 使用Locust进行压力测试
from locust import HttpUser, task, between

class APITestUser(HttpUser):
    wait_time = between(1, 3)

    @task
    def generate_testcase(self):
        self.client.post("/testcase/generate", json={
            "requirement_text": "测试需求",
            "module": "测试",
            "num_cases": 5
        })
```

#### 监控告警
```python
# 使用Prometheus监控接口性能
from prometheus_client import Counter, Histogram

api_requests_total = Counter('api_requests_total', 'Total API requests')
api_request_duration = Histogram('api_request_duration_seconds', 'API request duration')
```

#### 持续集成
```yaml
# .github/workflows/api-test.yml
name: API Tests
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Run API Tests
        run: |
          docker-compose up -d
          newman run tests/postman/collection.json
```

---

## 十、附录

### 10.1 SQL初始化脚本位置

完整的SQL初始化脚本建议保存为:
- `ai-service/scripts/init_mysql.sql`

### 10.2 向量数据库初始化脚本位置

完整的Python初始化脚本建议保存为:
- `ai-service/scripts/init_vector_db.py`

### 10.3 Postman Collection导出

建议将所有28个测试用例制作成Postman Collection并保存为:
- `ai-service/tests/postman/AI_Test_Platform.postman_collection.json`

### 10.4 测试报告模板

建议创建测试报告Excel模板:
- `ai-service/tests/templates/测试报告模板.xlsx`

---

**文档结束**

**联系方式**: 如有问题请联系测试团队

**版本历史**:
- v1.0 (2025-12-04): 初始版本,包含28个测试用例和完整数据预置方案
