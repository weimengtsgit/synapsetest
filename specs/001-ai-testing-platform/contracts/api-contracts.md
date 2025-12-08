# API Contracts: AI驱动测试任务管理系统

## 1. 测试任务管理API

### 创建测试任务
**POST** `/api/v1/test-tasks`

**Request Body**:
```json
{
  "name": "string",
  "description": "string",
  "environment": "string",
  "version": "string",
  "testScope": "string",
  "priority": 0
}
```

**Response**:
```json
{
  "id": "string",
  "name": "string",
  "description": "string",
  "environment": "string",
  "version": "string",
  "testScope": "string",
  "status": "string",
  "priority": 0,
  "createdAt": "2025-11-10T10:00:00Z",
  "updatedAt": "2025-11-10T10:00:00Z",
  "createdBy": "string"
}
```

### 获取测试任务列表
**GET** `/api/v1/test-tasks`

**Query Parameters**:
- page (integer, optional): 页码
- size (integer, optional): 每页大小
- status (string, optional): 任务状态

**Response**:
```json
{
  "items": [
    {
      "id": "string",
      "name": "string",
      "description": "string",
      "environment": "string",
      "version": "string",
      "testScope": "string",
      "status": "string",
      "priority": 0,
      "createdAt": "2025-11-10T10:00:00Z",
      "updatedAt": "2025-11-10T10:00:00Z",
      "createdBy": "string"
    }
  ],
  "total": 0,
  "page": 0,
  "size": 0
}
```

### 获取测试任务详情
**GET** `/api/v1/test-tasks/{id}`

**Response**:
```json
{
  "id": "string",
  "name": "string",
  "description": "string",
  "environment": "string",
  "version": "string",
  "testScope": "string",
  "status": "string",
  "priority": 0,
  "createdAt": "2025-11-10T10:00:00Z",
  "updatedAt": "2025-11-10T10:00:00Z",
  "createdBy": "string",
  "testCases": [
    {
      "id": "string",
      "title": "string",
      "description": "string"
    }
  ]
}
```

### 启动测试任务
**POST** `/api/v1/test-tasks/{id}/start`

**Response**:
```json
{
  "id": "string",
  "status": "执行中"
}
```

### 取消测试任务
**POST** `/api/v1/test-tasks/{id}/cancel`

**Response**:
```json
{
  "id": "string",
  "status": "已取消"
}
```

## 2. 测试用例管理API

### AI生成测试用例
**POST** `/api/v1/test-cases/generate`

**Request Body**:
```json
{
  "input": "string",  // 需求文档或自然语言描述
  "format": "string"  // 输出格式
}
```

**Response**:
```json
{
  "testCases": [
    {
      "id": "string",
      "title": "string",
      "description": "string",
      "steps": ["string"],
      "expectedResults": "string"
    }
  ]
}
```

### 创建测试用例
**POST** `/api/v1/test-cases`

**Request Body**:
```json
{
  "title": "string",
  "description": "string",
  "steps": ["string"],
  "expectedResults": "string",
  "priority": 0,
  "type": "string",
  "tags": ["string"],
  "relatedRequirement": "string"
}
```

**Response**:
```json
{
  "id": "string",
  "title": "string",
  "description": "string",
  "steps": ["string"],
  "expectedResults": "string",
  "priority": 0,
  "type": "string",
  "status": "string",
  "tags": ["string"],
  "relatedRequirement": "string",
  "createdAt": "2025-11-10T10:00:00Z",
  "updatedAt": "2025-11-10T10:00:00Z",
  "createdBy": "string"
}
```

### 获取测试用例列表
**GET** `/api/v1/test-cases`

**Query Parameters**:
- page (integer, optional): 页码
- size (integer, optional): 每页大小
- type (string, optional): 用例类型
- status (string, optional): 用例状态

**Response**:
```json
{
  "items": [
    {
      "id": "string",
      "title": "string",
      "description": "string",
      "priority": 0,
      "type": "string",
      "status": "string",
      "tags": ["string"],
      "relatedRequirement": "string",
      "createdAt": "2025-11-10T10:00:00Z",
      "updatedAt": "2025-11-10T10:00:00Z",
      "createdBy": "string"
    }
  ],
  "total": 0,
  "page": 0,
  "size": 0
}
```

## 3. 监控和报告API

### 获取测试任务实时状态
**GET** `/api/v1/monitoring/tasks/{id}`

**Response**:
```json
{
  "taskId": "string",
  "status": "string",
  "progress": 0,  // 0-100
  "executedCases": 0,
  "totalCases": 0,
  "passedCases": 0,
  "failedCases": 0,
  "startTime": "2025-11-10T10:00:00Z",
  "estimatedEndTime": "2025-11-10T11:00:00Z"
}
```

### 获取质量报告
**GET** `/api/v1/reports/{id}`

**Response**:
```json
{
  "id": "string",
  "taskId": "string",
  "name": "string",
  "summary": "string",
  "testResults": [
    {
      "testCaseId": "string",
      "status": "string",
      "executionTime": 0,
      "error": "string"
    }
  ],
  "defectStats": {
    "critical": 0,
    "major": 0,
    "minor": 0
  },
  "performanceMetrics": {
    "avgResponseTime": 0,
    "throughput": 0
  },
  "riskAssessment": {
    "overallRisk": "string",
    "highRiskModules": ["string"]
  },
  "generatedAt": "2025-11-10T10:00:00Z",
  "status": "string"
}
```