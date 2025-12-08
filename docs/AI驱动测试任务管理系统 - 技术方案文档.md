# AI驱动测试任务管理系统 - 技术方案文档

**文档版本**: v1.1  
**创建日期**: 2025-11-11  
**最后更新**: 2025-11-16  
**适用范围**: 项目组全体成员  
**文档目的**: 帮助项目组成员快速理解系统架构、技术选型和实现方案

> **重要提示 (v1.1)**: 本版本进行了重大架构调整，从JPA迁移到MyBatis，从PostgreSQL迁移到MySQL。请仔细阅读第12章"架构变更总结"。

---

## 📑 目录

- [1. 项目概述](#1-项目概述)
- [2. 技术架构](#2-技术架构)
- [3. 技术栈详解](#3-技术栈详解)
- [4. 模块设计](#4-模块设计)
- [5. 数据模型](#5-数据模型)
- [6. API设计](#6-api设计)
- [7. 前端架构](#7-前端架构)
- [8. AI服务架构](#8-ai服务架构)
- [9. 部署方案](#9-部署方案)
- [10. 开发规范](#10-开发规范)
- [11. 测试策略](#11-测试策略)
- [12. 监控运维](#12-监控运维)

---

## 1. 项目概述

### 1.1 产品定位

**AI驱动测试任务管理系统（SynapseTest）** 是一个面向中大型企业研发团队的智能化测试管理平台。通过AI技术实现测试任务的智能调度、用例自动生成、质量风险预测，帮助企业提升测试效率70%，降低生产环境缺陷80%。

### 1.2 核心价值

| 价值维度 | 传统方式 | SynapseTest | 提升幅度 |
|---------|---------|-------------|---------|
| 测试配置时间 | 30分钟/任务 | 9分钟/任务 | ↓ 70% |
| 用例编写效率 | 2小时/用例 | 30分钟/用例 | ↑ 75% |
| 测试执行时间 | 4小时 | 2小时 | ↓ 50% |
| 生产环境缺陷 | 100个/月 | 20个/月 | ↓ 80% |
| 资源利用率 | 40% | 70% | ↑ 30% |

### 1.3 目标用户

- **测试工程师**: 用例管理、任务执行、结果分析
- **开发工程师**: CI/CD集成、快速反馈、缺陷修复
- **测试经理**: 资源分配、质量监控、策略制定
- **发布经理**: 风险评估、质量决策、发布控制

---

## 2. 技术架构

### 2.1 整体架构图

```
┌────────────────────────────────────────────────────────────────┐
│                         用户层 (User Layer)                     │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐      │
│  │ Web UI   │  │ Mobile   │  │ CLI Tool │  │ API SDK  │      │
│  │ (React)  │  │  (RN)    │  │          │  │          │      │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘      │
└────────────────────────────────────────────────────────────────┘
                              ↓ HTTPS/WSS
┌────────────────────────────────────────────────────────────────┐
│                    网关层 (Gateway Layer)                       │
│  ┌──────────────────────────────────────────────────────────┐ │
│  │  Spring Cloud Gateway + JWT认证 + 限流熔断               │ │
│  └──────────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────────────┘
                              ↓ gRPC/REST
┌────────────────────────────────────────────────────────────────┐
│                  业务服务层 (Service Layer)                     │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐      │
│  │ 测试任务 │  │ 测试用例 │  │ 测试环境 │  │ 版本管理 │      │
│  │ Service  │  │ Service  │  │ Service  │  │ Service  │      │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘      │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐      │
│  │ 监控分析 │  │ 质量报告 │  │ 推荐引擎 │  │ 用户管理 │      │
│  │ Service  │  │ Service  │  │ Service  │  │ Service  │      │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘      │
└────────────────────────────────────────────────────────────────┘
                              ↓ gRPC
┌────────────────────────────────────────────────────────────────┐
│                    AI服务层 (AI Service Layer)                  │
│  ┌──────────────────────────────────────────────────────────┐ │
│  │  Python FastAPI + PyTorch + 大语言模型                    │ │
│  │  • 用例生成服务  • 风险预测服务  • 智能推荐服务         │ │
│  └──────────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────────────┘
                              ↓
┌────────────────────────────────────────────────────────────────┐
│                   数据层 (Data Layer)                           │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐      │
│  │  MySQL   │  │ MongoDB  │  │  Redis   │  │  MinIO   │      │
│  │(业务数据)│  │(AI数据)  │  │ (缓存)   │  │ (文件)   │      │
│  │+ MyBatis │  │@Profile  │  │ (可选)   │  │ (可选)   │      │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘      │
└────────────────────────────────────────────────────────────────┘
                              ↓
┌────────────────────────────────────────────────────────────────┐
│                  基础设施层 (Infrastructure Layer)              │
│  ┌──────────────────────────────────────────────────────────┐ │
│  │  Kubernetes + Docker + Prometheus + ELK + Kafka         │ │
│  └──────────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────────────┘
```

### 2.2 技术架构特点

#### 2.2.1 微服务架构
- **服务拆分原则**: 按业务领域垂直拆分，每个服务独立部署
- **服务通信**: 同步调用使用gRPC，异步消息使用Kafka
- **服务治理**: 使用Nacos进行服务发现和配置管理
- **熔断降级**: 使用Resilience4j实现熔断、限流、降级

#### 2.2.2 前后端分离
- **前端**: React 18 + TypeScript + Ant Design
- **后端**: Spring Boot 3.0 提供RESTful API
- **通信协议**: HTTPS + JSON，WebSocket用于实时推送

#### 2.2.3 容器化部署
- **容器编排**: Kubernetes 1.27
- **镜像管理**: Docker Registry
- **自动扩缩容**: HPA (Horizontal Pod Autoscaler)
- **服务网格**: Istio 1.18 (可选)

---

## 3. 技术栈详解

### 3.1 后端技术栈

#### 3.1.1 Spring Boot 服务 (Java 11)

```yaml
核心框架:
  - Spring Boot: 2.7.18
  - MyBatis: 2.3.1 (替代 Spring Data JPA)
  - Spring Data MongoDB: AI数据持久化 (需要 @Profile("mongodb"))

服务治理:
  - ~~Spring Cloud Gateway~~: (已注释 - 与 Spring MVC 不兼容)
  - ~~Resilience4j~~: (已注释 - 版本兼容问题)

消息队列 (开发环境禁用):
  - Kafka: 高吞吐异步消息
  - RabbitMQ: 低延迟消息传递

安全认证:
  - Spring Security: 安全框架 (SecurityFilterChain 方式)
  - JWT: Token认证

数据库:
  - MySQL 8.0+: 主数据库 (替代 PostgreSQL)
  - MyBatis XML映射: SQL语句管理
  - MongoDB 5.0+: AI数据存储 (需要 @Profile("mongodb"))
  - Redis 7.0+: 缓存与会话 (开发环境禁用)

重要说明:
  - ID类型: String (UUID.randomUUID().toString())
  - 手动管理时间戳: LocalDateTime.now()
  - JSON处理: 自定义 JsonTypeHandler
  - API版本: Controller级别使用 ApiVersion.V1
```

**依赖管理 (pom.xml)**:
```xml
<dependencies>
    <!-- Spring Boot -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    
    <!-- MyBatis (替代 Spring Data JPA) -->
    <dependency>
        <groupId>org.mybatis.spring.boot</groupId>
        <artifactId>mybatis-spring-boot-starter</artifactId>
        <version>2.3.1</version>
    </dependency>
    
    <!-- MongoDB (需要 @Profile("mongodb")) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-mongodb</artifactId>
    </dependency>
    
    <!-- MySQL Driver (替代 PostgreSQL) -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <version>8.0.33</version>
        <scope>runtime</scope>
    </dependency>
    
    <!-- Redis (开发环境禁用) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>
    
    <!-- Spring Security -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    
    <!-- Lombok (需配置 annotation processor) -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
    </dependency>
    
    <!-- Validation -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
</dependencies>

<!-- Compiler Plugin 配置 (Lombok支持) -->
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.10.1</version>
            <configuration>
                <source>11</source>
                <target>11</target>
                <annotationProcessorPaths>
                    <path>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok</artifactId>
                        <version>1.18.30</version>
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </plugin>
    </plugins>
</build>
```

#### 3.1.2 AI服务 (Python 3.9+)

```yaml
核心框架:
  - FastAPI: 高性能Web框架
  - Uvicorn: ASGI服务器
  - Pydantic: 数据验证

AI/ML框架:
  - PyTorch: 深度学习框架
  - Transformers: 大语言模型
  - Scikit-learn: 机器学习
  - XGBoost: 梯度提升

数据处理:
  - Pandas: 数据分析
  - NumPy: 数值计算
  - Featuretools: 特征工程

模型服务:
  - TensorFlow Serving: 模型部署
  - ONNX Runtime: 模型推理加速
```

**依赖管理 (requirements.txt)**:
```txt
fastapi==0.104.1
uvicorn[standard]==0.24.0
torch==2.1.0
transformers==4.35.0
scikit-learn==1.3.2
xgboost==2.0.2
pandas==2.1.3
numpy==1.24.3
pymongo==4.6.0
redis==5.0.1
```

### 3.2 前端技术栈

#### 3.2.1 框架与库

```yaml
核心框架:
  - React: 18.2.0
  - TypeScript: 5.0+
  - Vite: 5.0+ (构建工具)

UI组件:
  - Ant Design: 5.11+ (UI组件库)
  - Ant Design Pro: 企业级模板

状态管理:
  - Redux Toolkit: 状态管理
  - React Query: 服务端状态管理

路由:
  - React Router: 6.20+ (SPA路由)

HTTP客户端:
  - Axios: HTTP请求库

可视化:
  - ECharts: 图表库
  - D3.js: 数据可视化

工具库:
  - Lodash: 工具函数
  - Day.js: 日期处理
  - immer: 不可变数据
```

**依赖管理 (package.json)**:
```json
{
  "dependencies": {
    "react": "^18.2.0",
    "react-dom": "^18.2.0",
    "react-router-dom": "^6.20.0",
    "antd": "^5.11.0",
    "@reduxjs/toolkit": "^1.9.7",
    "axios": "^1.6.2",
    "echarts": "^5.4.3",
    "lodash": "^4.17.21",
    "dayjs": "^1.11.10"
  },
  "devDependencies": {
    "@types/react": "^18.2.43",
    "@types/react-dom": "^18.2.17",
    "@vitejs/plugin-react": "^4.2.1",
    "typescript": "^5.3.3",
    "vite": "^5.0.8"
  }
}
```

### 3.3 基础设施技术栈

```yaml
容器化:
  - Docker: 24.0+
  - Docker Compose: 本地开发
  - Kubernetes: 1.27+
  - Helm: K8s包管理

监控告警:
  - Prometheus: 指标采集
  - Grafana: 可视化
  - AlertManager: 告警管理

日志系统:
  - ELK Stack:
    - Elasticsearch: 日志存储
    - Logstash: 日志处理
    - Kibana: 日志查询
  - Filebeat: 日志收集

服务网格 (可选):
  - Istio: 服务治理
  - Envoy: 边车代理

存储:
  - MinIO: 对象存储
  - NFS: 共享存储
```

---

## 4. 模块设计

### 4.1 后端模块结构

```
backend/
├── src/main/java/com/synapsetest/testmanagement/
│   ├── config/              # 配置类
│   │   ├── GatewayConfig.java (已禁用)
│   │   ├── JsonTypeHandler.java          # MyBatis JSON处理器
│   │   ├── KafkaConfig.java
│   │   ├── MonitoringConfig.java
│   │   ├── RabbitMQConfig.java
│   │   ├── SecurityConfig.java           # SecurityFilterChain方式
│   │   └── WebConfig.java
│   │
│   ├── constants/           # 常量定义
│   │   └── ApiVersion.java              # API版本常量 (/api/v1, /api/v2)
│   │
│   ├── controller/          # REST控制器
│   │   ├── HealthController.java        # @RequestMapping("") - 无版本前缀
│   │   ├── MonitoringController.java    # @Profile("mongodb") + ApiVersion.V1
│   │   ├── ReportController.java        # @Profile("mongodb") + ApiVersion.V1
│   │   ├── TestCaseController.java      # ApiVersion.V1, optional AI services
│   │   ├── TestEnvironmentController.java  # ApiVersion.V1
│   │   ├── TestTaskController.java      # ApiVersion.V1
│   │   └── TestVersionController.java   # ApiVersion.V1
│   │
│   ├── dto/                 # 数据传输对象 (ID字段为String)
│   │   ├── ApiResponse.java
│   │   ├── TestTaskRequest.java
│   │   ├── TestTaskResponse.java
│   │   ├── TestCaseRequest.java
│   │   ├── TestCaseResponse.java
│   │   └── AITestCaseGenerationRequest.java
│   │
│   ├── entity/              # 基础实体 (无JPA注解)
│   │   └── BaseEntity.java             # String id, LocalDateTime timestamps
│   │
│   ├── exception/           # 异常处理
│   │   ├── GlobalExceptionHandler.java
│   │   ├── ResourceNotFoundException.java
│   │   └── ValidationException.java
│   │
│   ├── interceptor/         # 拦截器
│   │   ├── AuthInterceptor.java
│   │   └── LoggingInterceptor.java
│   │
│   ├── mapper/              # MyBatis Mapper接口
│   │   ├── TestTaskMapper.java          # @Mapper, 对应XML
│   │   ├── TestCaseMapper.java          # @Mapper, 使用JsonTypeHandler
│   │   ├── TestEnvironmentMapper.java   # @Mapper, 对应XML
│   │   ├── TestVersionMapper.java       # @Mapper, 对应XML
│   │   └── ResourcePoolMapper.java      # @Mapper, 对应XML
│   │
│   ├── model/               # POJOs (无JPA注解)
│   │   ├── TestTask.java            # MyBatis POJO
│   │   ├── TestCase.java            # MyBatis POJO
│   │   ├── TestEnvironment.java     # MyBatis POJO
│   │   ├── TestVersion.java         # MyBatis POJO
│   │   ├── ResourcePool.java        # MyBatis POJO
│   │   ├── AIModel.java             # MongoDB @Document
│   │   ├── MonitoringData.java      # MongoDB @Document
│   │   └── QualityReport.java       # MongoDB @Document
│   │
│   ├── repository/          # MongoDB仓库 (需@Profile("mongodb"))
│   │   ├── AIModelRepository.java           # @Profile("mongodb")
│   │   ├── MonitoringDataRepository.java    # @Profile("mongodb")
│   │   └── QualityReportRepository.java     # @Profile("mongodb")
│   │
│   ├── service/             # 业务逻辑层 (手动ID/时间戳管理)
│   │   ├── TestTaskService.java                    # 使用TestTaskMapper
│   │   ├── TestCaseService.java                    # 使用TestCaseMapper
│   │   ├── TestEnvironmentService.java             # 使用TestEnvironmentMapper
│   │   ├── TestVersionService.java                 # 使用TestVersionMapper
│   │   ├── ResourcePoolService.java                # 使用ResourcePoolMapper
│   │   ├── MonitoringService.java                  # @Profile("mongodb")
│   │   ├── QualityReportService.java               # @Profile("mongodb")
│   │   ├── ReportingService.java                   # @Profile("mongodb")
│   │   ├── QualityTraceabilityService.java         # @Profile("mongodb")
│   │   ├── TestRecommendationService.java          # 推荐服务
│   │   ├── AIModelService.java                     # @Profile("mongodb")
│   │   ├── AITestCaseGenerationService.java        # @Profile("mongodb")
│   │   └── AITestCaseOptimizationService.java      # @Profile("mongodb")
│   │
│   └── TestManagementApplication.java  # 应用入口 (@MapperScan)
│
└── resources/
    ├── application.yml           # 应用配置 (无context-path, 自动配置排除)
    ├── application-config.yml    # 扩展配置
    ├── schema.sql                # MySQL Schema (非PostgreSQL)
    ├── service-discovery.yml     # 服务发现配置
    └── mapper/                   # MyBatis XML映射文件
        ├── TestTaskMapper.xml
        ├── TestCaseMapper.xml      # 使用JsonTypeHandler
        ├── TestEnvironmentMapper.xml
        ├── TestVersionMapper.xml
        └── ResourcePoolMapper.xml
```

### 4.2 User Story 模块映射

#### 4.2.1 User Story 1: 智能测试任务调度

**功能**: 根据代码变更和历史数据自动推荐测试策略

**涉及模块**:
```
Models:
  - TestTask.java          # 测试任务实体
  - TestEnvironment.java   # 测试环境实体
  - TestVersion.java       # 测试版本实体
  - ResourcePool.java      # 资源池实体

Services:
  - TestTaskService.java              # 任务CRUD和状态管理
  - TestEnvironmentService.java       # 环境管理
  - TestVersionService.java           # 版本管理
  - ResourcePoolService.java          # 资源分配
  - TestRecommendationService.java    # AI推荐算法

Controllers:
  - TestTaskController.java           # 任务API
  - TestEnvironmentController.java    # 环境API
  - TestVersionController.java        # 版本API

Frontend:
  - CreateTestTask.jsx     # 创建任务页面
  - TestTaskList.jsx       # 任务列表页面
  - testTaskService.js     # API客户端
```

**核心功能实现**:
1. **任务创建**: 用户输入基本信息 → AI推荐服务分析 → 返回推荐策略
2. **环境选择**: 根据历史数据推荐最适合的测试环境
3. **版本匹配**: 自动匹配当前版本的测试基线
4. **资源分配**: 智能分配测试资源池

#### 4.2.2 User Story 2: AI生成测试用例

**功能**: 基于需求文档自动生成测试用例

**涉及模块**:
```
Models:
  - TestCase.java          # 测试用例实体
  - AIModel.java           # AI模型实体

Services:
  - TestCaseService.java                  # 用例CRUD
  - AITestCaseGenerationService.java      # AI用例生成
  - AITestCaseOptimizationService.java    # 用例去重优化
  - AIModelService.java                   # AI模型管理

Controllers:
  - TestCaseController.java   # 用例API

Frontend:
  - AITestCaseGeneration.jsx  # AI生成页面
  - TestCaseList.jsx          # 用例列表页面
  - testCaseService.js        # API客户端

AI Service:
  - ai-service/services/      # Python AI服务
```

**核心功能实现**:
1. **文档解析**: 上传需求文档 → 文本提取 → 语义分析
2. **用例生成**: 调用大语言模型 → 生成用例初稿 → 格式化输出
3. **用例优化**: 语义去重 → 优先级排序 → 覆盖率分析
4. **人机协作**: 用户修正 → 反馈学习 → 模型优化

#### 4.2.3 User Story 3: 测试结果可视化分析

**功能**: 实时监控测试进度和质量分析

**涉及模块**:
```
Models:
  - MonitoringData.java    # 监控数据实体 (MongoDB)
  - QualityReport.java     # 质量报告实体 (MongoDB)

Services:
  - MonitoringService.java              # 监控数据管理
  - QualityReportService.java           # 报告生成
  - ReportingService.java               # 报告服务
  - QualityTraceabilityService.java     # 质量追溯

Controllers:
  - MonitoringController.java   # 监控API
  - ReportController.java       # 报告API

Frontend:
  - Dashboard.jsx           # 监控仪表盘
  - QualityReport.jsx       # 质量报告页面
  - monitoringService.js    # 监控API客户端
  - reportService.js        # 报告API客户端
```

**核心功能实现**:
1. **实时监控**: 
   - WebSocket推送测试进度
   - 自动刷新仪表盘（10秒间隔）
   - 实时计算通过率和预估完成时间
   
2. **质量报告**:
   - 生成综合质量报告
   - 风险评估和分级
   - 缺陷统计和分析
   
3. **质量追溯**:
   - 需求-用例-缺陷关联
   - 代码变更影响分析
   - 质量门禁检查

### 4.3 AI服务模块

```
ai-service/
├── main.py              # FastAPI应用入口
├── init_schema.py       # 数据库初始化
├── requirements.txt     # Python依赖
│
├── api/                 # API路由
│   ├── __init__.py
│   ├── testcase.py      # 用例生成API
│   ├── recommendation.py # 推荐API
│   └── prediction.py    # 预测API
│
├── models/              # AI模型
│   ├── __init__.py
│   ├── llm_model.py     # 大语言模型
│   ├── risk_model.py    # 风险预测模型
│   └── optimization.py  # 优化算法
│
└── services/            # 业务服务
    ├── __init__.py
    ├── generation.py    # 用例生成服务
    ├── optimization.py  # 用例优化服务
    └── prediction.py    # 风险预测服务
```

---

## 5. 数据模型

### 5.1 MySQL 数据模型 (MyBatis)

#### 5.1.1 核心业务表

**数据库创建**
```sql
CREATE DATABASE IF NOT EXISTS test_management DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE test_management;
```

**测试任务表 (test_tasks)**
```sql
CREATE TABLE IF NOT EXISTS test_tasks (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),  -- 字符串UUID
    name VARCHAR(100) NOT NULL,
    description TEXT,
    environment VARCHAR(50) NOT NULL CHECK (environment IN ('DEV', 'STAGING', 'PROD')),
    version VARCHAR(50) NOT NULL,
    test_scope VARCHAR(50),
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETED', 'CANCELLED')),
    priority INTEGER DEFAULT 0,
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_priority CHECK (priority >= 0 AND priority <= 10)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_test_tasks_status ON test_tasks(status);
CREATE INDEX idx_test_tasks_created_at ON test_tasks(created_at DESC);
```

**测试用例表 (test_cases)**
```sql
CREATE TABLE IF NOT EXISTS test_cases (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    title VARCHAR(200) NOT NULL,
    description TEXT,
    steps JSON NOT NULL,              -- JSON格式的测试步骤 (使用JsonTypeHandler)
    expected_results TEXT,
    priority INTEGER DEFAULT 0,
    type VARCHAR(20) NOT NULL CHECK (type IN ('FUNCTIONAL', 'PERFORMANCE', 'SECURITY')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('DRAFT', 'APPROVED', 'DEPRECATED')),
    tags JSON,                        -- JSON数组 (使用JsonTypeHandler)
    related_requirement VARCHAR(200),
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_test_cases_type ON test_cases(type);
CREATE INDEX idx_test_cases_status ON test_cases(status);
```

**测试环境表 (test_environments)**
```sql
CREATE TABLE IF NOT EXISTS test_environments (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    url VARCHAR(500),
    config JSON,                       -- JSON配置 (使用JsonTypeHandler)
    status VARCHAR(20) NOT NULL CHECK (status IN ('AVAILABLE', 'MAINTENANCE', 'UNAVAILABLE')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_test_environments_status ON test_environments(status);
```

**测试版本表 (test_versions)**
```sql
CREATE TABLE IF NOT EXISTS test_versions (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    product_version VARCHAR(50) NOT NULL,
    release_date DATE,
    config JSON,                       -- JSON配置 (使用JsonTypeHandler)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**资源池表 (resource_pools)**
```sql
CREATE TABLE IF NOT EXISTS resource_pools (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    type VARCHAR(20) NOT NULL CHECK (type IN ('VM', 'CONTAINER', 'DEVICE')),
    capacity INTEGER NOT NULL CHECK (capacity > 0),
    allocated INTEGER DEFAULT 0 CHECK (allocated >= 0),
    location VARCHAR(200),
    config JSON,                       -- JSON配置 (使用JsonTypeHandler)
    status VARCHAR(20) NOT NULL CHECK (status IN ('AVAILABLE', 'MAINTENANCE', 'UNAVAILABLE')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_capacity_allocated CHECK (allocated <= capacity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_resource_pools_status ON resource_pools(status);
```

**关联表 (task_test_cases)**
```sql
CREATE TABLE IF NOT EXISTS task_test_cases (
    task_id CHAR(36) NOT NULL,
    test_case_id CHAR(36) NOT NULL,
    execution_order INTEGER,
    PRIMARY KEY (task_id, test_case_id),
    FOREIGN KEY (task_id) REFERENCES test_tasks(id) ON DELETE CASCADE,
    FOREIGN KEY (test_case_id) REFERENCES test_cases(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**注意事项**:
- **AI模型数据**: 存储在MongoDB中，而非MySQL
- **质量报告**: 存储在MongoDB中
- **监控数据**: 存储在MongoDB中
- 所有需要MongoDB的功能都标记为 `@Profile("mongodb")`

### 5.2 MongoDB 数据模型

#### 5.2.1 监控数据 (monitoring_data)

```javascript
{
  "_id": ObjectId,
  "taskId": String,              // 关联的任务ID
  "status": String,              // PENDING/RUNNING/COMPLETED/FAILED
  "progress": Number,            // 0-100
  "executedCases": Number,
  "totalCases": Number,
  "passedCases": Number,
  "failedCases": Number,
  "skippedCases": Number,
  "startTime": ISODate,
  "estimatedEndTime": ISODate,
  "actualEndTime": ISODate,
  "resourceUsage": {             // 资源使用情况
    "cpu": Number,
    "memory": Number,
    "disk": Number
  },
  "performanceMetrics": {        // 性能指标
    "avgResponseTime": Number,
    "throughput": Number,
    "errorRate": Number
  },
  "timestamp": ISODate,
  "environment": String,
  "version": String
}
```

**索引**:
```javascript
db.monitoring_data.createIndex({ "taskId": 1 })
db.monitoring_data.createIndex({ "timestamp": -1 })
db.monitoring_data.createIndex({ "status": 1 })
db.monitoring_data.createIndex({ "environment": 1 })
```

#### 5.2.2 质量报告 (quality_reports)

```javascript
{
  "_id": ObjectId,
  "taskId": String,
  "name": String,
  "summary": String,
  "testResults": [               // 测试结果列表
    {
      "testCaseId": String,
      "testCaseName": String,
      "status": String,          // PASSED/FAILED/SKIPPED/BLOCKED
      "executionTime": Number,   // 毫秒
      "error": String,
      "screenshot": String,
      "executedAt": ISODate
    }
  ],
  "defectStats": {               // 缺陷统计
    "critical": Number,
    "major": Number,
    "minor": Number,
    "total": Number
  },
  "performanceMetrics": {
    "avgResponseTime": Number,
    "minResponseTime": Number,
    "maxResponseTime": Number,
    "passRate": Number,
    "throughput": Number
  },
  "riskAssessment": {            // 风险评估
    "overallRisk": String,       // LOW/MEDIUM/HIGH/CRITICAL
    "riskScore": Number,         // 0.0-1.0
    "highRiskModules": [String],
    "recommendations": [String],
    "moduleRiskScores": {
      "module1": Number,
      "module2": Number
    }
  },
  "generatedAt": ISODate,
  "status": String               // GENERATING/COMPLETED/ARCHIVED
}
```

**索引**:
```javascript
db.quality_reports.createIndex({ "taskId": 1 })
db.quality_reports.createIndex({ "generatedAt": -1 })
db.quality_reports.createIndex({ "riskAssessment.overallRisk": 1 })
```

### 5.3 Redis 数据结构

#### 5.3.1 缓存策略

```
# 用户会话
Key: session:{userId}
Type: String (JSON)
TTL: 7200 seconds (2 hours)
Value: {userId, username, roles, permissions, loginTime}

# 测试任务缓存
Key: task:{taskId}
Type: Hash
TTL: 3600 seconds (1 hour)
Fields: {name, status, environment, version, priority}

# 实时监控数据
Key: monitoring:realtime:{taskId}
Type: String (JSON)
TTL: 600 seconds (10 minutes)
Value: MonitoringData JSON

# API限流
Key: ratelimit:{apiPath}:{userId}
Type: String
TTL: 60 seconds
Value: request count

# 分布式锁
Key: lock:task:{taskId}
Type: String
TTL: 30 seconds
Value: {lockId, timestamp}
```

---

## 6. API设计

### 6.1 API设计原则

1. **RESTful规范**: 使用标准HTTP方法 (GET/POST/PUT/DELETE)
2. **版本控制**: 
   - Controller级别版本控制，使用 `ApiVersion.V1` 常量
   - 业务API: `/api/v1/*` (如 `/api/v1/test-tasks`)
   - 系统API: 无版本前缀 (如 `/health`, `/actuator/*`)
3. **统一响应格式**: 所有API返回统一的ApiResponse结构
4. **错误处理**: 使用标准HTTP状态码和错误信息
5. **安全认证**: 
   - 开发环境: `/api/v1/**` permitAll
   - 生产环境: 需要JWT Token认证
6. **Profile管理**: MongoDB相关API需要 `@Profile("mongodb")` 激活

### 6.2 统一响应格式

```java
@Data
public class ApiResponse<T> {
    private Boolean success;        // 是否成功
    private String message;         // 消息
    private T data;                 // 数据
    private Long timestamp;         // 时间戳
    private String errorCode;       // 错误码 (可选)
    
    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(true);
        response.setData(data);
        response.setTimestamp(System.currentTimeMillis());
        return response;
    }
    
    public static <T> ApiResponse<T> success(String message, T data) {
        ApiResponse<T> response = success(data);
        response.setMessage(message);
        return response;
    }
    
    public static <T> ApiResponse<T> error(String message, String errorCode) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(false);
        response.setMessage(message);
        response.setErrorCode(errorCode);
        response.setTimestamp(System.currentTimeMillis());
        return response;
    }
}
```

### 6.3 核心API端点

#### 6.3.1 测试任务API

```
# 创建测试任务
POST /api/v1/test-tasks
Request Body: TestTaskRequest
Response: ApiResponse<TestTaskResponse>

# 获取任务列表
GET /api/v1/test-tasks?page=0&size=10&sort=createdAt,desc
Response: ApiResponse<Page<TestTaskResponse>>

# 获取任务详情
GET /api/v1/test-tasks/{id}
Response: ApiResponse<TestTaskResponse>

# 按状态查询
GET /api/v1/test-tasks?status=RUNNING
Response: ApiResponse<List<TestTaskResponse>>

# 启动任务
POST /api/v1/test-tasks/{id}/start
Response: ApiResponse<TestTaskResponse>

# 取消任务
POST /api/v1/test-tasks/{id}/cancel
Response: ApiResponse<TestTaskResponse>
```

#### 6.3.2 测试用例API

```
# AI生成测试用例
POST /api/v1/test-cases/generate
Request Body: AITestCaseGenerationRequest
Response: ApiResponse<List<TestCaseResponse>>

# 创建测试用例
POST /api/v1/test-cases
Request Body: TestCaseRequest
Response: ApiResponse<TestCaseResponse>

# 获取用例列表
GET /api/v1/test-cases?page=0&size=10
Response: ApiResponse<Page<TestCaseResponse>>

# 获取用例详情
GET /api/v1/test-cases/{id}
Response: ApiResponse<TestCaseResponse>

# 更新用例
PUT /api/v1/test-cases/{id}
Request Body: TestCaseRequest
Response: ApiResponse<TestCaseResponse>

# 删除用例
DELETE /api/v1/test-cases/{id}
Response: ApiResponse<Void>

# 优化用例
POST /api/v1/test-cases/optimize
Request Body: List<TestCaseResponse>
Response: ApiResponse<List<TestCaseResponse>>
```

#### 6.3.3 监控API

```
# 获取任务监控数据
GET /api/v1/monitoring/tasks/{taskId}
Response: ApiResponse<MonitoringData>

# 获取最近监控数据
GET /api/v1/monitoring/recent
Response: ApiResponse<List<MonitoringData>>

# 获取运行中任务
GET /api/v1/monitoring/running
Response: ApiResponse<List<MonitoringData>>

# 获取仪表盘统计
GET /api/v1/monitoring/dashboard/stats
Response: ApiResponse<Map<String, Object>>

# 按环境获取监控数据
GET /api/v1/monitoring/environment/{environment}
Response: ApiResponse<List<MonitoringData>>

# 更新资源使用情况
POST /api/v1/monitoring/tasks/{taskId}/resource-usage
Request Body: Map<String, Object>
Response: ApiResponse<MonitoringData>

# 更新性能指标
POST /api/v1/monitoring/tasks/{taskId}/performance-metrics
Request Body: Map<String, Object>
Response: ApiResponse<MonitoringData>
```

#### 6.3.4 报告API

```
# 获取质量报告
GET /api/v1/reports/{id}
Response: ApiResponse<QualityReport>

# 通过任务ID获取报告
GET /api/v1/reports/task/{taskId}
Response: ApiResponse<QualityReport>

# 获取最近报告
GET /api/v1/reports/recent
Response: ApiResponse<List<QualityReport>>

# 获取综合报告
GET /api/v1/reports/comprehensive/{taskId}
Response: ApiResponse<Map<String, Object>>

# 比较报告
GET /api/v1/reports/compare?taskId1=xxx&taskId2=yyy
Response: ApiResponse<Map<String, Object>>

# 获取HTML报告
GET /api/v1/reports/html/{taskId}
Response: HTML String

# 追踪需求覆盖
GET /api/v1/reports/traceability/requirement/{requirementId}
Response: ApiResponse<Map<String, Object>>

# 生成追溯矩阵
POST /api/v1/reports/traceability/matrix
Request Body: List<String> (requirement IDs)
Response: ApiResponse<Map<String, Object>>

# 检查质量门禁
GET /api/v1/reports/quality-gates/{taskId}
Response: ApiResponse<Map<String, Object>>

# 追踪缺陷影响
GET /api/v1/reports/traceability/defect/{defectId}
Response: ApiResponse<Map<String, Object>>

# 分析变更影响
POST /api/v1/reports/traceability/change-impact?changeId=xxx
Request Body: List<String> (changed files)
Response: ApiResponse<Map<String, Object>>
```

### 6.4 错误码定义

```java
public enum ErrorCode {
    // 通用错误 (1xxx)
    SUCCESS("1000", "操作成功"),
    SYSTEM_ERROR("1001", "系统内部错误"),
    INVALID_PARAMETER("1002", "参数验证失败"),
    UNAUTHORIZED("1003", "未授权访问"),
    FORBIDDEN("1004", "禁止访问"),
    
    // 业务错误 (2xxx)
    RESOURCE_NOT_FOUND("2001", "资源不存在"),
    RESOURCE_ALREADY_EXISTS("2002", "资源已存在"),
    OPERATION_NOT_ALLOWED("2003", "操作不允许"),
    
    // 任务错误 (3xxx)
    TASK_NOT_FOUND("3001", "测试任务不存在"),
    TASK_ALREADY_RUNNING("3002", "任务已在运行中"),
    TASK_CANNOT_START("3003", "任务无法启动"),
    
    // 用例错误 (4xxx)
    TESTCASE_NOT_FOUND("4001", "测试用例不存在"),
    TESTCASE_GENERATION_FAILED("4002", "用例生成失败"),
    
    // AI服务错误 (5xxx)
    AI_SERVICE_UNAVAILABLE("5001", "AI服务不可用"),
    AI_MODEL_NOT_FOUND("5002", "AI模型不存在"),
    AI_GENERATION_TIMEOUT("5003", "AI生成超时");
    
    private final String code;
    private final String message;
    
    // Constructor and getters...
}
```

---

## 7. 前端架构

### 7.1 项目结构

```
frontend/
├── public/              # 静态资源
├── src/
│   ├── App.tsx         # 应用入口
│   ├── main.tsx        # 主文件
│   ├── App.css         # 全局样式
│   │
│   ├── components/     # 组件目录
│   │   ├── test-task/
│   │   │   ├── CreateTestTask.jsx     # 创建任务
│   │   │   └── TestTaskList.jsx       # 任务列表
│   │   ├── test-case/
│   │   │   ├── AITestCaseGeneration.jsx  # AI生成用例
│   │   │   └── TestCaseList.jsx          # 用例列表
│   │   ├── monitoring/
│   │   │   └── Dashboard.jsx          # 监控仪表盘
│   │   └── report/
│   │       └── QualityReport.jsx      # 质量报告
│   │
│   ├── pages/          # 页面目录
│   │   ├── Home.tsx
│   │   ├── Login.tsx
│   │   └── Settings.tsx
│   │
│   ├── services/       # API服务
│   │   ├── testTaskService.js      # 任务API
│   │   ├── testCaseService.js      # 用例API
│   │   ├── monitoringService.js    # 监控API
│   │   └── reportService.js        # 报告API
│   │
│   ├── store/          # Redux Store
│   │   ├── index.ts
│   │   ├── slices/
│   │   │   ├── authSlice.ts
│   │   │   ├── taskSlice.ts
│   │   │   └── uiSlice.ts
│   │   └── hooks.ts
│   │
│   ├── utils/          # 工具函数
│   │   ├── request.ts       # HTTP请求封装
│   │   ├── storage.ts       # 本地存储
│   │   └── constants.ts     # 常量定义
│   │
│   └── types/          # TypeScript类型定义
│       ├── task.ts
│       ├── testcase.ts
│       └── api.ts
│
├── package.json
├── tsconfig.json
├── vite.config.ts
└── nginx.conf         # Nginx配置
```

### 7.2 路由设计

```typescript
// App.tsx
import { Routes, Route } from 'react-router-dom'

const App = () => {
  return (
    <Layout>
      <Sider>
        <Menu items={menuItems} />
      </Sider>
      <Layout>
        <Header />
        <Content>
          <Routes>
            {/* 首页 */}
            <Route path="/" element={<Home />} />
            
            {/* 测试任务 */}
            <Route path="/test-tasks/create" element={<CreateTestTask />} />
            <Route path="/test-tasks/list" element={<TestTaskList />} />
            
            {/* 测试用例 */}
            <Route path="/test-cases/generate" element={<AITestCaseGeneration />} />
            <Route path="/test-cases/list" element={<TestCaseList />} />
            
            {/* 监控 */}
            <Route path="/monitoring/dashboard" element={<Dashboard />} />
            
            {/* 报告 */}
            <Route path="/reports/quality" element={<QualityReport />} />
            
            {/* 404 */}
            <Route path="*" element={<NotFound />} />
          </Routes>
        </Content>
        <Footer />
      </Layout>
    </Layout>
  )
}
```

### 7.3 状态管理

```typescript
// store/index.ts
import { configureStore } from '@reduxjs/toolkit'
import authReducer from './slices/authSlice'
import taskReducer from './slices/taskSlice'
import uiReducer from './slices/uiSlice'

export const store = configureStore({
  reducer: {
    auth: authReducer,
    task: taskReducer,
    ui: uiReducer,
  },
})

export type RootState = ReturnType<typeof store.getState>
export type AppDispatch = typeof store.dispatch
```

### 7.4 HTTP请求封装

```typescript
// utils/request.ts
import axios from 'axios'

const request = axios.create({
  baseURL: '/api/v1',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    const { success, data, message } = response.data
    if (success) {
      return data
    } else {
      message.error(message || '请求失败')
      return Promise.reject(new Error(message))
    }
  },
  (error) => {
    if (error.response?.status === 401) {
      // 未授权，跳转登录
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default request
```

---

## 8. AI服务架构

### 8.1 AI服务技术栈

```yaml
框架:
  - FastAPI: Web框架
  - Uvicorn: ASGI服务器
  - Pydantic: 数据验证

AI/ML:
  - PyTorch: 深度学习
  - Transformers: 大语言模型
  - Scikit-learn: 机器学习
  - XGBoost: 梯度提升

数据处理:
  - Pandas: 数据处理
  - NumPy: 数值计算
  - NLTK: 自然语言处理

数据库:
  - PyMongo: MongoDB客户端
  - Redis-py: Redis客户端
```

### 8.2 AI服务端点

```python
# main.py
from fastapi import FastAPI
from api import testcase, recommendation, prediction

app = FastAPI(title="AI Service", version="1.0.0")

# 注册路由
app.include_router(testcase.router, prefix="/api/v1/ai/testcase", tags=["testcase"])
app.include_router(recommendation.router, prefix="/api/v1/ai/recommendation", tags=["recommendation"])
app.include_router(prediction.router, prefix="/api/v1/ai/prediction", tags=["prediction"])

@app.get("/health")
async def health_check():
    return {"status": "healthy"}
```

### 8.3 大语言模型集成

```python
# models/llm_model.py
from transformers import AutoTokenizer, AutoModelForCausalLM
import torch

class TestCaseGenerator:
    def __init__(self, model_name: str = "Qwen/Qwen-7B-Chat"):
        self.tokenizer = AutoTokenizer.from_pretrained(model_name)
        self.model = AutoModelForCausalLM.from_pretrained(
            model_name,
            torch_dtype=torch.float16,
            device_map="auto"
        )
    
    def generate_testcases(self, requirement: str, num_cases: int = 5):
        prompt = f"""基于以下需求文档，生成{num_cases}个测试用例：

需求描述：
{requirement}

请生成测试用例，包含：
1. 用例名称
2. 测试步骤
3. 预期结果

测试用例：
"""
        inputs = self.tokenizer(prompt, return_tensors="pt").to(self.model.device)
        outputs = self.model.generate(
            **inputs,
            max_length=2048,
            temperature=0.7,
            top_p=0.9,
            do_sample=True
        )
        
        result = self.tokenizer.decode(outputs[0], skip_special_tokens=True)
        return self._parse_testcases(result)
    
    def _parse_testcases(self, text: str):
        # 解析生成的文本，提取测试用例
        # 实现省略...
        pass
```

### 8.4 风险预测模型

```python
# models/risk_model.py
import xgboost as xgb
import numpy as np

class RiskPredictionModel:
    def __init__(self):
        self.model = xgb.XGBClassifier(
            objective='multi:softmax',
            num_class=4,  # LOW/MEDIUM/HIGH/CRITICAL
            max_depth=6,
            learning_rate=0.1,
            n_estimators=100
        )
    
    def predict_risk(self, features: dict) -> dict:
        """
        预测风险等级
        
        Args:
            features: {
                'code_changes': int,
                'file_count': int,
                'complexity': float,
                'history_defects': int,
                'test_coverage': float,
                'module_importance': float
            }
        
        Returns:
            {
                'risk_level': str,  # LOW/MEDIUM/HIGH/CRITICAL
                'risk_score': float,  # 0.0-1.0
                'confidence': float
            }
        """
        X = self._extract_features(features)
        risk_class = self.model.predict(X)[0]
        risk_proba = self.model.predict_proba(X)[0]
        
        risk_levels = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL']
        
        return {
            'risk_level': risk_levels[risk_class],
            'risk_score': float(risk_proba[risk_class]),
            'confidence': float(np.max(risk_proba))
        }
    
    def _extract_features(self, features: dict) -> np.ndarray:
        # 特征提取和归一化
        # 实现省略...
        pass
```

---

## 9. 部署方案

### 9.1 本地开发环境

#### 9.1.1 使用Docker Compose

```yaml
# docker-compose.yml
version: '3.8'

services:
  # MySQL (替代 PostgreSQL)
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_DATABASE: test_management
      MYSQL_ROOT_PASSWORD: root
      MYSQL_USER: testuser
      MYSQL_PASSWORD: password
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
    command: --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci
  
  # MongoDB (需要激活 mongodb profile)
  mongodb:
    image: mongo:5.0
    ports:
      - "27017:27017"
    environment:
      MONGO_INITDB_ROOT_USERNAME: admin
      MONGO_INITDB_ROOT_PASSWORD: password
    volumes:
      - mongo_data:/data/db
    profiles:
      - mongodb  # 默认不启动，需要明确指定 profile
  
  # Redis
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
  
  # Kafka
  kafka:
    image: confluentinc/cp-kafka:7.5.0
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
    depends_on:
      - zookeeper
  
  # Zookeeper
  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    ports:
      - "2181:2181"
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
  
  # Backend
  backend:
    build: ./backend
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: dev
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/test_management
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: root
      # MongoDB仅在需要时启用
      # SPRING_DATA_MONGODB_URI: mongodb://admin:password@mongodb:27017
      # SPRING_REDIS_HOST: redis
      # SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092
    depends_on:
      - mysql
    # 可选依赖 (按需启用)
    # depends_on:
    #   - mysql
    #   - mongodb
    #   - redis
    #   - kafka
  
  # AI Service
  ai-service:
    build: ./ai-service
    ports:
      - "8000:8000"
    environment:
      MONGODB_URI: mongodb://admin:password@mongodb:27017
      REDIS_HOST: redis
    depends_on:
      - mongodb
      - redis
  
  # Frontend
  frontend:
    build: ./frontend
    ports:
      - "3000:80"
    depends_on:
      - backend

volumes:
  mysql_data:
  mongo_data:
  redis_data:
```

**启动命令**:
```bash
# 启动核心服务 (仅MySQL)
docker-compose up -d

# 启动所有服务 (包括MongoDB)
docker-compose --profile mongodb up -d

# 查看日志
docker-compose logs -f backend

# 停止服务
docker-compose down

# 停止并删除数据
docker-compose down -v

# 仅启动数据库
docker-compose up -d mysql

# 启动应用并激活MongoDB profile
SPRING_PROFILES_ACTIVE=mongodb docker-compose --profile mongodb up -d
```

### 9.2 Kubernetes生产环境

#### 9.2.1 命名空间

```yaml
# k8s/namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: synapsetest
  labels:
    name: synapsetest
```

#### 9.2.2 后端部署

```yaml
# k8s/backend-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: backend
  namespace: synapsetest
spec:
  replicas: 3
  selector:
    matchLabels:
      app: backend
  template:
    metadata:
      labels:
        app: backend
    spec:
      containers:
      - name: backend
        image: synapsetest/backend:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: SPRING_DATASOURCE_URL
          valueFrom:
            secretKeyRef:
              name: database-secret
              key: url
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "2000m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: backend-service
  namespace: synapsetest
spec:
  type: ClusterIP
  ports:
  - port: 8080
    targetPort: 8080
  selector:
    app: backend
```

#### 9.2.3 前端部署

```yaml
# k8s/frontend-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: frontend
  namespace: synapsetest
spec:
  replicas: 2
  selector:
    matchLabels:
      app: frontend
  template:
    metadata:
      labels:
        app: frontend
    spec:
      containers:
      - name: frontend
        image: synapsetest/frontend:latest
        ports:
        - containerPort: 80
        resources:
          requests:
            memory: "128Mi"
            cpu: "100m"
          limits:
            memory: "256Mi"
            cpu: "200m"
---
apiVersion: v1
kind: Service
metadata:
  name: frontend-service
  namespace: synapsetest
spec:
  type: LoadBalancer
  ports:
  - port: 80
    targetPort: 80
  selector:
    app: frontend
```

#### 9.2.4 AI服务部署

```yaml
# k8s/ai-service-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ai-service
  namespace: synapsetest
spec:
  replicas: 2
  selector:
    matchLabels:
      app: ai-service
  template:
    metadata:
      labels:
        app: ai-service
    spec:
      containers:
      - name: ai-service
        image: synapsetest/ai-service:latest
        ports:
        - containerPort: 8000
        resources:
          requests:
            memory: "4Gi"
            cpu: "2000m"
            nvidia.com/gpu: 1
          limits:
            memory: "8Gi"
            cpu: "4000m"
            nvidia.com/gpu: 1
---
apiVersion: v1
kind: Service
metadata:
  name: ai-service
  namespace: synapsetest
spec:
  type: ClusterIP
  ports:
  - port: 8000
    targetPort: 8000
  selector:
    app: ai-service
```

### 9.3 CI/CD流程

```yaml
# .github/workflows/ci-cd.yml
name: CI/CD Pipeline

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  # 后端构建和测试
  backend-build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Build with Maven
        run: |
          cd backend
          mvn clean package -DskipTests
      
      - name: Run tests
        run: |
          cd backend
          mvn test
      
      - name: Build Docker image
        run: |
          cd backend
          docker build -t synapsetest/backend:${{ github.sha }} .
      
      - name: Push to Registry
        run: |
          echo ${{ secrets.DOCKER_PASSWORD }} | docker login -u ${{ secrets.DOCKER_USERNAME }} --password-stdin
          docker push synapsetest/backend:${{ github.sha }}
  
  # 前端构建
  frontend-build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Setup Node.js
        uses: actions/setup-node@v3
        with:
          node-version: '18'
      
      - name: Install dependencies
        run: |
          cd frontend
          npm ci
      
      - name: Run tests
        run: |
          cd frontend
          npm test
      
      - name: Build
        run: |
          cd frontend
          npm run build
      
      - name: Build Docker image
        run: |
          cd frontend
          docker build -t synapsetest/frontend:${{ github.sha }} .
      
      - name: Push to Registry
        run: |
          docker push synapsetest/frontend:${{ github.sha }}
  
  # 部署到K8s
  deploy:
    needs: [backend-build, frontend-build]
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    steps:
      - uses: actions/checkout@v3
      
      - name: Deploy to Kubernetes
        run: |
          kubectl set image deployment/backend backend=synapsetest/backend:${{ github.sha }} -n synapsetest
          kubectl set image deployment/frontend frontend=synapsetest/frontend:${{ github.sha }} -n synapsetest
          kubectl rollout status deployment/backend -n synapsetest
          kubectl rollout status deployment/frontend -n synapsetest
```

---

## 10. 开发规范

### 10.1 代码规范

#### 10.1.1 Java代码规范 (MyBatis方式)

**Service层示例**:
```java
/**
 * 测试任务服务
 * 
 * @author SynapseTest Team
 * @since v1.1
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TestTaskService {
    
    private final TestTaskMapper testTaskMapper;  // MyBatis Mapper
    
    /**
     * 创建测试任务
     * 
     * @param request 任务请求对象
     * @param username 创建用户
     * @return 任务响应对象
     */
    public TestTaskResponse createTestTask(TestTaskRequest request, String username) {
        // 参数验证
        validateRequest(request);
        
        // 构建实体 (手动设置ID和时间戳)
        TestTask task = new TestTask();
        task.setId(UUID.randomUUID().toString());  // 手动生成ID
        task.setName(request.getName());
        task.setDescription(request.getDescription());
        task.setEnvironment(request.getEnvironment());
        task.setVersion(request.getVersion());
        task.setCreatedBy(username);
        task.setCreatedAt(LocalDateTime.now());    // 手动设置时间戳
        task.setUpdatedAt(LocalDateTime.now());
        
        // 使用MyBatis Mapper插入
        testTaskMapper.insert(task);
        
        // 返回结果
        return convertToResponse(task);
    }
    
    /**
     * 更新测试任务
     */
    public TestTaskResponse updateTestTask(String id, TestTaskRequest request) {
        TestTask task = testTaskMapper.selectById(id)
            .orElseThrow(() -> new ResourceNotFoundException("任务不存在: " + id));
        
        // 更新字段
        task.setName(request.getName());
        task.setDescription(request.getDescription());
        task.setUpdatedAt(LocalDateTime.now());  // 手动更新时间戳
        
        // 执行更新
        testTaskMapper.update(task);
        
        return convertToResponse(task);
    }
}
```

**MyBatis Mapper接口**:
```java
/**
 * 测试任务Mapper接口
 * 对应mapper/TestTaskMapper.xml
 */
@Mapper
public interface TestTaskMapper {
    
    void insert(TestTask testTask);
    
    void update(TestTask testTask);
    
    Optional<TestTask> selectById(String id);
    
    List<TestTask> selectAll();
    
    List<TestTask> selectByStatus(@Param("status") String status);
    
    void deleteById(String id);
}
```

**MyBatis XML映射文件**:
```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" 
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.synapsetest.testmanagement.mapper.TestTaskMapper">
    
    <resultMap id="testTaskResultMap" type="com.synapsetest.testmanagement.model.TestTask">
        <id property="id" column="id"/>
        <result property="name" column="name"/>
        <result property="description" column="description"/>
        <result property="environment" column="environment"/>
        <result property="version" column="version"/>
        <result property="testScope" column="test_scope"/>
        <result property="status" column="status"/>
        <result property="priority" column="priority"/>
        <result property="createdBy" column="created_by"/>
        <result property="createdAt" column="created_at"/>
        <result property="updatedAt" column="updated_at"/>
    </resultMap>
    
    <insert id="insert" parameterType="TestTask">
        INSERT INTO test_tasks (
            id, name, description, environment, version, 
            test_scope, status, priority, created_by, 
            created_at, updated_at
        ) VALUES (
            #{id}, #{name}, #{description}, #{environment}, #{version},
            #{testScope}, #{status}, #{priority}, #{createdBy},
            #{createdAt}, #{updatedAt}
        )
    </insert>
    
    <update id="update" parameterType="TestTask">
        UPDATE test_tasks SET
            name = #{name},
            description = #{description},
            environment = #{environment},
            version = #{version},
            updated_at = #{updatedAt}
        WHERE id = #{id}
    </update>
    
    <select id="selectById" resultMap="testTaskResultMap">
        SELECT * FROM test_tasks WHERE id = #{id}
    </select>
    
    <select id="selectAll" resultMap="testTaskResultMap">
        SELECT * FROM test_tasks ORDER BY created_at DESC
    </select>
    
    <select id="selectByStatus" resultMap="testTaskResultMap">
        SELECT * FROM test_tasks WHERE status = #{status}
    </select>
    
    <delete id="deleteById">
        DELETE FROM test_tasks WHERE id = #{id}
    </delete>
</mapper>
```

**JSON字段处理 (使用自定义TypeHandler)**:
```java
// Model类中JSON字段
public class TestCase {
    private String id;
    private String title;
    
    // JSON字段，使用自定义TypeHandler
    private List<String> steps;  // 对应MySQL的JSON类型
    private List<String> tags;   // 对应MySQL的JSON类型
    
    // ... other fields
}

// MyBatis XML中指定TypeHandler
<resultMap id="testCaseResultMap" type="TestCase">
    <id property="id" column="id"/>
    <result property="title" column="title"/>
    <result property="steps" column="steps" 
            typeHandler="com.synapsetest.testmanagement.config.JsonTypeHandler"/>
    <result property="tags" column="tags" 
            typeHandler="com.synapsetest.testmanagement.config.JsonTypeHandler"/>
</resultMap>
```

**命名规范**:
- 类名: 大驼峰 (PascalCase)
- 方法名: 小驼峰 (camelCase)
- 常量: 全大写+下划线 (UPPER_SNAKE_CASE)
- 包名: 全小写

#### 10.1.2 TypeScript/React代码规范

```typescript
/**
 * 组件文档注释
 * 
 * @description 测试任务列表组件
 * @author 开发者姓名
 */
interface TestTaskListProps {
  status?: string
  onTaskClick?: (taskId: string) => void
}

const TestTaskList: React.FC<TestTaskListProps> = ({ status, onTaskClick }) => {
  // 状态定义
  const [tasks, setTasks] = useState<TestTask[]>([])
  const [loading, setLoading] = useState(false)
  
  // 副作用
  useEffect(() => {
    loadTasks()
  }, [status])
  
  // 事件处理
  const loadTasks = async () => {
    setLoading(true)
    try {
      const data = await testTaskService.getAllTestTasks({ status })
      setTasks(data)
    } catch (error) {
      message.error('加载失败')
    } finally {
      setLoading(false)
    }
  }
  
  // 渲染
  return (
    <Table
      dataSource={tasks}
      loading={loading}
      columns={columns}
      rowKey="id"
    />
  )
}

export default TestTaskList
```

**命名规范**:
- 组件名: 大驼峰 (PascalCase)
- 变量/函数: 小驼峰 (camelCase)
- 常量: 全大写+下划线 (UPPER_SNAKE_CASE)
- 接口: I前缀或Props/State后缀

### 10.2 Git工作流

#### 10.2.1 分支策略

```
main                     # 主分支，生产环境
  ├── develop            # 开发分支
  │    ├── feature/xxx   # 功能分支
  │    ├── bugfix/xxx    # 缺陷修复分支
  │    └── hotfix/xxx    # 紧急修复分支
  └── release/v1.0.0     # 发布分支
```

#### 10.2.2 提交规范

```bash
# 提交消息格式
<type>(<scope>): <subject>

<body>

<footer>

# 类型 (type)
feat:     新功能
fix:      Bug修复
docs:     文档更新
style:    代码格式调整
refactor: 重构
perf:     性能优化
test:     测试相关
chore:    构建/工具变动

# 示例
feat(task): add task scheduling service

- Implement task creation API
- Add recommendation service integration
- Add unit tests

Closes #123
```

### 10.3 文档规范

#### 10.3.1 API文档

使用Swagger/OpenAPI规范：

```java
@RestController
@RequestMapping("/api/v1/test-tasks")
@Tag(name = "测试任务", description = "测试任务管理API")
public class TestTaskController {
    
    @Operation(
        summary = "创建测试任务",
        description = "创建新的测试任务，系统会自动推荐测试策略"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "创建成功"),
        @ApiResponse(responseCode = "400", description = "参数验证失败"),
        @ApiResponse(responseCode = "500", description = "服务器错误")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<TestTaskResponse>> createTestTask(
        @Parameter(description = "任务请求对象") @Valid @RequestBody TestTaskRequest request,
        @Parameter(description = "创建用户") @RequestHeader("X-User-Name") String username
    ) {
        // 实现...
    }
}
```

#### 10.3.2 数据库变更文档

使用Flyway管理数据库版本：

```sql
-- V1__initial_schema.sql
-- 描述：初始化数据库表结构
-- 作者：开发者姓名
-- 日期：2025-01-01

CREATE TABLE test_tasks (
    -- 表结构...
);

CREATE INDEX idx_test_tasks_status ON test_tasks(status);

-- 数据初始化
INSERT INTO test_environments (name, environment_type) VALUES
    ('开发环境', 'DEV'),
    ('预发环境', 'STAGING'),
    ('生产环境', 'PROD');
```

---

## 11. 测试策略

### 11.1 测试金字塔

```
        /\
       /  \
      / E2E\          10%  (端到端测试)
     /______\
    /        \
   /Integration\      30%  (集成测试)
  /____________\
 /              \
/   Unit Tests   \    60%  (单元测试)
/__________________\
```

### 11.2 单元测试

#### 11.2.1 后端单元测试

```java
@SpringBootTest
class TestTaskServiceTest {
    
    @MockBean
    private TestTaskRepository testTaskRepository;
    
    @MockBean
    private TestRecommendationService recommendationService;
    
    @Autowired
    private TestTaskService testTaskService;
    
    @Test
    @DisplayName("创建测试任务 - 成功场景")
    void createTestTask_Success() {
        // Given
        TestTaskRequest request = new TestTaskRequest();
        request.setName("测试任务1");
        request.setEnvironment("DEV");
        
        TestTask task = new TestTask();
        task.setId(UUID.randomUUID());
        task.setName(request.getName());
        
        when(testTaskRepository.save(any())).thenReturn(task);
        
        // When
        TestTaskResponse response = testTaskService.createTestTask(request, "user1");
        
        // Then
        assertNotNull(response);
        assertEquals("测试任务1", response.getName());
        verify(testTaskRepository, times(1)).save(any());
    }
    
    @Test
    @DisplayName("创建测试任务 - 参数验证失败")
    void createTestTask_ValidationFailed() {
        // Given
        TestTaskRequest request = new TestTaskRequest();
        // name为空
        
        // When & Then
        assertThrows(ValidationException.class, () -> {
            testTaskService.createTestTask(request, "user1");
        });
    }
}
```

#### 11.2.2 前端单元测试

```typescript
// TestTaskList.test.tsx
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import TestTaskList from './TestTaskList'
import testTaskService from '../../services/testTaskService'

jest.mock('../../services/testTaskService')

describe('TestTaskList', () => {
  it('应该正确渲染任务列表', async () => {
    // Given
    const mockTasks = [
      { id: '1', name: '任务1', status: 'RUNNING' },
      { id: '2', name: '任务2', status: 'PENDING' },
    ]
    ;(testTaskService.getAllTestTasks as jest.Mock).mockResolvedValue(mockTasks)
    
    // When
    render(<TestTaskList />)
    
    // Then
    await waitFor(() => {
      expect(screen.getByText('任务1')).toBeInTheDocument()
      expect(screen.getByText('任务2')).toBeInTheDocument()
    })
  })
  
  it('应该处理加载失败', async () => {
    // Given
    ;(testTaskService.getAllTestTasks as jest.Mock).mockRejectedValue(
      new Error('加载失败')
    )
    
    // When
    render(<TestTaskList />)
    
    // Then
    await waitFor(() => {
      expect(screen.getByText(/加载失败/)).toBeInTheDocument()
    })
  })
})
```

### 11.3 集成测试

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase
@Sql(scripts = "/test-data.sql")
class TestTaskIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void testCreateAndGetTask() {
        // 创建任务
        TestTaskRequest request = new TestTaskRequest();
        request.setName("集成测试任务");
        request.setEnvironment("DEV");
        
        ResponseEntity<ApiResponse> createResponse = restTemplate.postForEntity(
            "/api/v1/test-tasks",
            request,
            ApiResponse.class
        );
        
        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        
        // 获取任务
        String taskId = (String) createResponse.getBody().getData().get("id");
        ResponseEntity<ApiResponse> getResponse = restTemplate.getForEntity(
            "/api/v1/test-tasks/" + taskId,
            ApiResponse.class
        );
        
        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        assertEquals("集成测试任务", getResponse.getBody().getData().get("name"));
    }
}
```

### 11.4 E2E测试

```typescript
// cypress/e2e/task-management.cy.ts
describe('任务管理流程', () => {
  beforeEach(() => {
    cy.login('testuser', 'password')
  })
  
  it('应该能够创建并查看任务', () => {
    // 访问创建页面
    cy.visit('/test-tasks/create')
    
    // 填写表单
    cy.get('[data-testid="task-name"]').type('E2E测试任务')
    cy.get('[data-testid="environment"]').select('DEV')
    cy.get('[data-testid="version"]').select('v1.0.0')
    
    // 提交
    cy.get('[data-testid="submit-btn"]').click()
    
    // 验证成功消息
    cy.contains('任务创建成功').should('be.visible')
    
    // 访问任务列表
    cy.visit('/test-tasks/list')
    
    // 验证任务出现在列表中
    cy.contains('E2E测试任务').should('be.visible')
  })
})
```

---

## 12. 监控运维

### 12.1 监控指标

#### 12.1.1 应用指标

```yaml
JVM指标:
  - jvm.memory.used            # 内存使用
  - jvm.memory.max             # 最大内存
  - jvm.gc.pause               # GC暂停时间
  - jvm.threads.live           # 活跃线程数

HTTP指标:
  - http.server.requests       # 请求总数
  - http.server.requests.duration  # 请求耗时
  - http.server.errors         # 错误数

数据库指标:
  - db.pool.connections.active # 活跃连接数
  - db.pool.connections.idle   # 空闲连接数
  - db.query.duration          # 查询耗时

业务指标:
  - task.created.total         # 创建任务总数
  - task.running.current       # 当前运行任务数
  - testcase.generated.total   # 生成用例总数
  - ai.request.duration        # AI请求耗时
```

#### 12.1.2 Prometheus配置

```yaml
# prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'backend'
    static_configs:
      - targets: ['backend-service:8080']
    metrics_path: '/actuator/prometheus'
  
  - job_name: 'ai-service'
    static_configs:
      - targets: ['ai-service:8000']
    metrics_path: '/metrics'
  
  - job_name: 'postgres'
    static_configs:
      - targets: ['postgres-exporter:9187']
  
  - job_name: 'mongodb'
    static_configs:
      - targets: ['mongodb-exporter:9216']
```

### 12.2 日志管理

#### 12.2.1 日志格式

```json
{
  "timestamp": "2025-01-15T10:30:45.123Z",
  "level": "INFO",
  "service": "backend",
  "traceId": "abc123def456",
  "spanId": "789ghi",
  "userId": "user123",
  "message": "Task created successfully",
  "context": {
    "taskId": "uuid-xxx",
    "environment": "DEV",
    "version": "v1.0.0"
  }
}
```

#### 12.2.2 日志级别

```
ERROR  - 错误日志，需要立即处理
WARN   - 警告日志，需要关注
INFO   - 信息日志，记录关键业务流程
DEBUG  - 调试日志，仅开发环境使用
TRACE  - 跟踪日志，详细的执行信息
```

### 12.3 告警规则

```yaml
# alerting-rules.yml
groups:
  - name: application
    rules:
      # 服务可用性告警
      - alert: ServiceDown
        expr: up{job="backend"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "服务 {{ $labels.instance }} 不可用"
          description: "Backend服务已下线超过1分钟"
      
      # 高错误率告警
      - alert: HighErrorRate
        expr: rate(http_server_requests_total{status=~"5.."}[5m]) > 0.05
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "高错误率检测"
          description: "错误率超过5%，持续5分钟"
      
      # 响应时间告警
      - alert: HighResponseTime
        expr: histogram_quantile(0.95, rate(http_server_requests_duration_seconds_bucket[5m])) > 2
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "响应时间过长"
          description: "95分位响应时间超过2秒"
      
      # 数据库连接池告警
      - alert: DatabaseConnectionPoolExhausted
        expr: db_pool_connections_active / db_pool_connections_max > 0.9
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "数据库连接池即将耗尽"
          description: "活跃连接数超过最大连接数的90%"
```

### 12.4 健康检查

```java
// HealthController.java
@RestController
@RequestMapping("/actuator/health")
public class HealthController {
    
    @Autowired
    private DataSource dataSource;
    
    @Autowired
    private MongoTemplate mongoTemplate;
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @GetMapping("/liveness")
    public ResponseEntity<Map<String, Object>> liveness() {
        // 存活检查：服务是否运行
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "timestamp", System.currentTimeMillis()
        ));
    }
    
    @GetMapping("/readiness")
    public ResponseEntity<Map<String, Object>> readiness() {
        // 就绪检查：依赖服务是否可用
        Map<String, String> checks = new HashMap<>();
        
        // 检查数据库
        try {
            dataSource.getConnection().isValid(1);
            checks.put("database", "UP");
        } catch (Exception e) {
            checks.put("database", "DOWN");
        }
        
        // 检查MongoDB
        try {
            mongoTemplate.executeCommand("{ ping: 1 }");
            checks.put("mongodb", "UP");
        } catch (Exception e) {
            checks.put("mongodb", "DOWN");
        }
        
        // 检查Redis
        try {
            redisTemplate.opsForValue().get("health-check");
            checks.put("redis", "UP");
        } catch (Exception e) {
            checks.put("redis", "DOWN");
        }
        
        boolean allUp = checks.values().stream().allMatch(s -> "UP".equals(s));
        
        return ResponseEntity
            .status(allUp ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
            .body(Map.of(
                "status", allUp ? "UP" : "DOWN",
                "checks", checks,
                "timestamp", System.currentTimeMillis()
            ));
    }
}
```

---

## 附录

### A. 常用命令

#### A.1 本地开发

```bash
# 后端
cd backend
mvn clean install
mvn spring-boot:run

# 前端
cd frontend
npm install
npm run dev

# AI服务
cd ai-service
pip install -r requirements.txt
uvicorn main:app --reload

# Docker Compose
docker-compose up -d
docker-compose logs -f backend
docker-compose down
```

#### A.2 Kubernetes

```bash
# 部署
kubectl apply -f k8s/

# 查看状态
kubectl get pods -n synapsetest
kubectl get services -n synapsetest

# 查看日志
kubectl logs -f deployment/backend -n synapsetest

# 扩容
kubectl scale deployment/backend --replicas=5 -n synapsetest

# 回滚
kubectl rollout undo deployment/backend -n synapsetest
```

### B. 故障排查

#### B.1 常见问题

**问题1: 服务启动失败**
```bash
# 检查日志
kubectl logs <pod-name> -n synapsetest

# 检查配置
kubectl describe pod <pod-name> -n synapsetest

# 检查资源
kubectl top pod -n synapsetest
```

**问题2: 数据库连接失败**
```bash
# 检查数据库状态
kubectl exec -it <postgres-pod> -n synapsetest -- psql -U postgres

# 检查网络连通性
kubectl exec -it <backend-pod> -n synapsetest -- nc -zv postgres 5432

# 检查配置
kubectl get secret database-secret -n synapsetest -o yaml
```

**问题3: AI服务响应慢**
```bash
# 检查GPU使用情况
kubectl exec -it <ai-pod> -n synapsetest -- nvidia-smi

# 检查内存使用
kubectl top pod <ai-pod> -n synapsetest

# 查看模型加载日志
kubectl logs <ai-pod> -n synapsetest | grep "model loaded"
```

### C. 参考资料

- [Spring Boot官方文档](https://spring.io/projects/spring-boot)
- [React官方文档](https://react.dev/)
- [Kubernetes官方文档](https://kubernetes.io/docs/)
- [PyTorch官方文档](https://pytorch.org/docs/)
- [Ant Design文档](https://ant.design/)

---

## 更新日志

| 版本 | 日期 | 作者 | 更新内容 |
|------|------|------|---------|
| v1.0 | 2025-11-11 | SynapseTest Team | 初始版本 |
| v1.1 | 2025-11-16 | SynapseTest Team | **重大架构更新**：<br/>1. ORM从JPA/Hibernate迁移到MyBatis 2.3.1<br/>2. 数据库从PostgreSQL迁移到MySQL 8.0<br/>3. Spring Boot从3.0.x降级到2.7.18<br/>4. ID类型从UUID改为String (手动生成)<br/>5. 添加自定义JsonTypeHandler处理MySQL JSON列<br/>6. API版本控制从context-path移至Controller级别<br/>7. MongoDB功能改为可选 (需@Profile("mongodb"))<br/>8. 禁用Spring Cloud Gateway和Resilience4j<br/>9. 添加MyBatis XML映射文件支持<br/>10. SecurityConfig改为SecurityFilterChain方式 |

---

## 架构变更总结 (v1.1)

### 🔄 重大变更

#### 1. **数据库迁移: PostgreSQL → MySQL**
- **原因**: MySQL生态更完善，社区支持更好
- **影响**: 
  - DDL语法调整 (UUID → CHAR(36), JSONB → JSON)
  - 时间戳自动更新: `ON UPDATE CURRENT_TIMESTAMP`
  - 字符集: utf8mb4
  
#### 2. **ORM迁移: JPA/Hibernate → MyBatis**
- **原因**: 更好的SQL控制，更灵活的映射
- **影响**:
  - 移除所有JPA注解 (@Entity, @Id, @GeneratedValue等)
  - 创建MyBatis Mapper接口 (5个)
  - 创建MyBatis XML映射文件 (5个)
  - 手动管理ID生成和时间戳
  - 自定义JsonTypeHandler处理JSON字段

#### 3. **Spring Boot降级: 3.0.x → 2.7.18**
- **原因**: 兼容性和稳定性考虑
- **影响**:
  - Java版本从17降至11
  - SecurityConfig使用SecurityFilterChain方式
  - 不再使用requestMatchers (使用antMatchers)

#### 4. **API版本策略变更**
- **之前**: `server.servlet.context-path: /api/v1`
- **现在**: Controller级别使用 `@RequestMapping(ApiVersion.V1 + "/resource")`
- **优势**: 
  - 支持多版本并存
  - 系统端点无版本前缀 (/health)
  - 更灵活的版本管理

#### 5. **Profile管理策略**
- **MongoDB功能**: 需要 `@Profile("mongodb")`
- **开发环境**: 默认禁用Redis、Kafka、RabbitMQ、MongoDB
- **AI功能**: 使用 `@Autowired(required = false)` + null检查
- **优势**: 降低开发环境复杂度，按需启用功能

### 📊 技术栈对比

| 组件 | v1.0 | v1.1 |
|------|------|------|
| Spring Boot | 3.0.x | 2.7.18 |
| Java | 17 | 11 |
| 数据库 | PostgreSQL 13+ | MySQL 8.0+ |
| ORM | Spring Data JPA | MyBatis 2.3.1 |
| ID类型 | UUID | String |
| JSON处理 | @Type(jsonb) | JsonTypeHandler |
| API版本 | context-path | Controller级别 |
| Gateway | Spring Cloud Gateway | ~~禁用~~ |
| 熔断器 | Resilience4j | ~~禁用~~ |
| Security | WebSecurityConfigurerAdapter | SecurityFilterChain |

### 📝 迁移建议

1. **数据库迁移**:
   ```bash
   # 导出PostgreSQL数据
   pg_dump test_management > backup.sql
   
   # 转换为MySQL格式
   # 手动调整UUID → CHAR(36), JSONB → JSON
   
   # 导入MySQL
   mysql test_management < converted.sql
   ```

2. **代码迁移检查清单**:
   - [ ] 移除所有JPA注解
   - [ ] 创建MyBatis Mapper接口
   - [ ] 创建MyBatis XML文件
   - [ ] 更新Service类使用Mapper
   - [ ] ID字段改为String类型
   - [ ] 手动生成ID: `UUID.randomUUID().toString()`
   - [ ] 手动设置时间戳: `LocalDateTime.now()`
   - [ ] Controller使用ApiVersion常量
   - [ ] MongoDB相关类添加@Profile

3. **配置文件调整**:
   ```yaml
   # application.yml
   spring:
     datasource:
       url: jdbc:mysql://localhost:3306/test_management  # 修改
       driver-class-name: com.mysql.cj.jdbc.Driver        # 修改
     autoconfigure:
       exclude:                                             # 新增
         - org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
         - org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration
   
   mybatis:                                                # 新增
     mapper-locations: classpath:mapper/*.xml
     type-aliases-package: com.synapsetest.testmanagement.model
   
   # 移除 server.servlet.context-path: /api/v1
   ```

---

**文档结束**

