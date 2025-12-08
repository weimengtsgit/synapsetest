# Backend Architecture Diagram

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                      REST API Layer (Port 8080)                      │
│      Spring Boot 2.7.18 - API Versioning via ApiVersion.V1          │
│              Health: /health  |  Business: /api/v1/*                 │
└────────────────────────────┬────────────────────────────────────────┘
                             │
         ┌───────────────────┼───────────────────┐
         │                   │                   │
    ┌────▼────┐      ┌──────▼──────┐      ┌─────▼─────┐
    │ US1: Task│      │ US2: AI Test │      │ US3: Report│
    │Scheduling│      │   Generation │      │ & Monitor  │
    └────┬────┘      └──────┬──────┘      └─────┬─────┘
         │                   │                   │
    ┌────┴───────────────────┴───────────────────┴────┐
    │         Service Layer (Business Logic)          │
    │  13 Services: Task, Case, Report, Monitoring   │
    └────┬───────────────────┬───────────────────┬───┘
         │                   │                   │
    ┌────▼────┐      ┌──────▼──────┐      ┌─────▼─────┐
    │ Mapper  │      │ Repository   │      │ Messaging │
    │(MyBatis)│      │ (MongoDB)    │      │ (Kafka)   │
    └────┬────┘      └──────┬──────┘      └─────┬─────┘
         │                   │                   │
         │       ┌───────────┴───────────┐       │
         │       │                       │       │
    ┌────▼───────▼──────────────┬────────▼──┐   │
    │   MySQL (8.0)             │ MongoDB   │   │
    │   test_management DB      │ AI DB     │   │
    │   - test_tasks            │ - models  │   │
    │   - test_cases            │ - reports │   │
    │   - test_environments     │ - monitor │   │
    │   - test_versions         │           │   │
    │   - resource_pools        │           │   │
    │   (MyBatis XML Mappings)  │           │   │
    └─────────────────────────────────────────┘

         ┌─────────────────────────────────────┐
         │      Infrastructure Services        │
         │                                     │
         │  ┌──────────┐  ┌──────────────┐   │
         │  │ Kafka    │  │ RabbitMQ     │   │
         │  │(disabled)│  │ (disabled)   │   │
         │  └──────────┘  └──────────────┘   │
         │  ┌──────────────────────────────┐ │
         │  │Redis Cache (disabled in dev) │ │
         │  └──────────────────────────────┘ │
         │                                     │
         │  Note: MongoDB requires             │
         │  --spring.profiles.active=mongodb   │
         └─────────────────────────────────────┘
```

## Component Interaction Flow

### User Story 1: Test Task Scheduling
```
Client Request
    │
    ▼
┌─────────────────────────────┐
│ TestTaskController          │
│ POST /api/v1/test-tasks     │
└──────────┬──────────────────┘
           │
           ▼
┌─────────────────────────────┐
│ TestTaskService             │
│ - Create task               │
│ - Manual ID generation      │
│ - Get AI recommendations    │
└──────────┬──────────────────┘
           │
      ┌────┴────┬──────────────────┐
      │          │                  │
      ▼          ▼                  ▼
  ┌────────┐  ┌──────────────┐  ┌──────────────┐
  │TestTask│  │ResourcePool  │  │TestRecommend│
  │Mapper  │  │Mapper        │  │Service       │
  └───┬────┘  └──────┬───────┘  └──────┬───────┘
      │              │                  │
      └──────────────┼──────────────────┘
                     │
                     ▼
           ┌──────────────────────┐
           │ MySQL Database       │
           │ (test_tasks,         │
           │  resource_pools)     │
           │ MyBatis XML Mappings │
           └──────────────────────┘
```

### User Story 2: AI Test Case Generation
```
Client Request (Natural Language)
    │
    ▼
┌──────────────────────────────────┐
│ TestCaseController               │
│ POST /api/v1/test-cases/generate │
└──────────┬───────────────────────┘
           │
           ▼
┌──────────────────────────────────────┐
│ AITestCaseGenerationService          │
│ - Requires @Profile("mongodb")       │
│ - Analyze scenarios                  │
│ - Generate test steps                │
│ - Calculate confidence               │
└──────────┬───────────────────────────┘
           │
      ┌────┴────┬──────────────────┐
      │          │                  │
      ▼          ▼                  ▼
  ┌────────┐  ┌──────────────┐  ┌──────────────┐
  │TestCase│  │AIModel       │  │Optimization  │
  │Mapper  │  │Repository    │  │Service       │
  └───┬────┘  └──────┬───────┘  └──────┬───────┘
      │              │                  │
      └──────────────┼──────────────────┘
                     │
        ┌────────────┴────────────┐
        │                         │
        ▼                         ▼
    ┌────────────┐         ┌──────────────┐
    │MySQL       │         │MongoDB       │
    │(test_cases)│         │(ai_models)   │
    │MyBatis XML │         │@Profile req. │
    └────────────┘         └──────────────┘
```

### User Story 3: Monitoring & Reporting
```
Test Execution in Progress
    │
    ▼
┌──────────────────────────────┐
│ MonitoringController         │
│ GET /api/v1/monitoring       │
│ @Profile("mongodb") required │
└──────────┬───────────────────┘
           │
           ▼
┌──────────────────────┐
│ MonitoringService    │
│ - Track progress     │
│ - Update metrics     │
└──────────┬───────────┘
           │
           ▼
    ┌──────────────────┐
    │ MongoDB          │
    │ (monitoring_data)│
    │ @Profile req.    │
    └──────────────────┘

Task Completion
    │
    ▼
┌──────────────────────────────┐
│ ReportController             │
│ POST /api/v1/reports         │
│ @Profile("mongodb") required │
└──────────┬───────────────────┘
           │
           ▼
┌──────────────────────────┐
│ QualityReportService     │
│ - Calculate metrics      │
│ - Assess risk            │
│ - Generate report        │
└──────────┬───────────────┘
           │
      ┌────┴─────────────────────┐
      │                          │
      ▼                          ▼
  ┌──────────┐           ┌──────────────┐
  │Reporting │           │QualityReport │
  │Service   │           │Repository    │
  └────┬─────┘           └──────┬───────┘
       │                        │
       └────────────┬───────────┘
                    │
                    ▼
            ┌──────────────────┐
            │ MongoDB          │
            │ (quality_reports)│
            │ @Profile req.    │
            └──────────────────┘
```

## Database Schema Relationships

```
MySQL Schema (MyBatis XML Mappers):
┌─────────────────────────────────────────────────────┐
│                  test_tasks                          │
│  ├─ id (PK, CHAR(36) UUID)                         │
│  ├─ name VARCHAR(100)                               │
│  ├─ environment VARCHAR(50)                         │
│  ├─ version VARCHAR(50)                             │
│  ├─ test_scope VARCHAR(50)                          │
│  ├─ status VARCHAR(20)                              │
│  ├─ priority INT (0-10)                             │
│  ├─ created_at/updated_at TIMESTAMP                 │
│  └─ created_by VARCHAR(100)                         │
└────────────────┬────────────────────────────────────┘
                 │
        ┌────────┴────────┬─────────────────┬────────────────┐
        │                 │                 │                │
        ▼                 ▼                 ▼                ▼
    ┌────────┐    ┌─────────────┐  ┌──────────┐   ┌──────────────┐
    │test_   │    │test_        │  │resource_ │   │task_test_    │
    │cases   │    │environments │  │pools     │   │cases (assoc) │
    │(JSON)  │    │(JSON config)│  │(JSON)    │   │              │
    └────────┘    └─────────────┘  └──────────┘   └──────┬───────┘
                                                          │
                                                    ┌─────▼──────┐
                                                    │test_cases  │
                                                    └────────────┘

MyBatis XML Mappings (backend/src/main/resources/mapper/):
├─ TestCaseMapper.xml     (with JsonTypeHandler)
├─ TestTaskMapper.xml
├─ ResourcePoolMapper.xml (with JsonTypeHandler)
├─ TestEnvironmentMapper.xml (with JsonTypeHandler)
└─ TestVersionMapper.xml  (with JsonTypeHandler)

MongoDB Collections (requires @Profile("mongodb")):
┌──────────────┐    ┌───────────────────┐    ┌──────────────────┐
│  ai_models   │    │quality_reports    │    │monitoring_data   │
├──────────────┤    ├───────────────────┤    ├──────────────────┤
│ id (PK)      │    │ id (PK)           │    │ id (PK)          │
│ name         │    │ taskId (FK)       │    │ taskId (FK)      │
│ version      │    │ testResults[]     │    │ progress         │
│ filePath     │    │ defectStats       │    │ executedCases    │
│ securityStat │    │ performanceMetric │    │ passedCases      │
│ compliance   │    │ riskAssessment    │    │ resourceUsage    │
└──────────────┘    └───────────────────┘    └──────────────────┘
```

## API Request/Response Flow

```
┌──────────────────────────────────────────────────────────────┐
│              REST API Request Flow                            │
└──────────────────────────────────────────────────────────────┘

1. Request arrives at Controller
   ├─ URL Mapping matched
   ├─ PathVariable/RequestBody extracted
   └─ @Valid annotation triggers validation

2. Controller delegates to Service
   ├─ Service method called
   ├─ Business logic executed
   └─ @Transactional ensures consistency

3. Service calls Mapper/Repository
   ├─ MyBatis Mapper for MySQL (XML-based SQL)
   ├─ MongoDB Repository for AI/Reports
   └─ Data returned to service

4. Service formats response
   ├─ Entity converted to DTO
   ├─ Recommendations/calculations added
   └─ Response prepared

5. Controller wraps in ApiResponse
   ├─ Success/error status
   ├─ Data payload
   └─ HTTP status code set

6. Response sent to client
   ├─ Content-Type: application/json
   └─ Body contains ApiResponse wrapper
```

## Service Layer Responsibilities

```
        ┌──────────────────────────────────────┐
        │       Service Layer Pattern           │
        └──────────────────────────────────────┘
                 │
    ┌────────────┼────────────┬────────────┐
    │            │            │            │
    ▼            ▼            ▼            ▼
┌────────┐  ┌────────┐  ┌─────────┐  ┌──────────┐
│Business│  │Data    │  │Error    │  │Cross-    │
│Logic   │  │Mapping │  │Handling │  │cutting   │
│        │  │        │  │         │  │Concerns  │
└────────┘  └────────┘  └─────────┘  └──────────┘
   │           │           │            │
   │           │           │            │
   ├─ Validat- ├─ Entity→ ├─ Try/Catch ├─ Logging
   │  ion       │   DTO    │  Global    │
   ├─ Rules    ├─ Data    │  Exception │
   │            │  Enrich- │  Handler   │
   └─ Algorithm└─ ment     └─ Custom    └─ Trans-
                             Exceptions    action
```

## Data Flow for User Story 3: Quality Report

```
Execution starts
    │
    ▼
┌──────────────────────────────────────┐
│ MonitoringService                     │
│ - createMonitoringData(taskId)       │
│ - Save initial state to MongoDB      │
└──────────────────────────────────────┘
    │
    ▼ (updates during execution)
┌──────────────────────────────────────┐
│ MonitoringService                     │
│ - updateMonitoringData(progress)     │
│ - Track: executed, passed, failed    │
└──────────────────────────────────────┘
    │
    ▼ (execution completes)
┌──────────────────────────────────────┐
│ QualityReportService                  │
│ - generateReport(testResults)        │
└──────────────────────────────────────┘
    │
    ├─ calculateDefectStats()
    ├─ calculatePerformanceMetrics()
    ├─ assessRisk()
    │  ├─ calculateRiskScore()
    │  ├─ identifyHighRiskModules()
    │  └─ generateRecommendations()
    └─ generateSummary()
    │
    ▼
┌──────────────────────────────────────┐
│ MongoDB: quality_reports             │
│ - Store complete report              │
│ - Include all calculations           │
│ - Ready for visualization            │
└──────────────────────────────────────┘
```

## Configuration & Infrastructure

```
┌────────────────────────────────────────────────────────┐
│          Infrastructure Configuration                  │
└────────────────────────────────────────────────────────┘

Application Properties (application.yml)
├─ server.port: 8080
├─ NO context-path (API versioning in Controllers)
├─ API Version Management: ApiVersion.V1 = "/api/v1"
└─ Datasources
   ├─ MySQL 8.0 (JDBC) - Primary
   │  └─ jdbc:mysql://localhost:3306/test_management
   ├─ MongoDB (URI) - @Profile("mongodb") required
   ├─ Redis - Disabled in dev (autoconfigure.exclude)
   ├─ Kafka - Disabled in dev (autoconfigure.exclude)
   └─ RabbitMQ - Disabled in dev (autoconfigure.exclude)

Spring Boot Modules (v2.7.18)
├─ spring-boot-starter-web
├─ mybatis-spring-boot-starter (2.3.1)
│  ├─ Replaces spring-data-jpa
│  └─ XML-based SQL mappings
├─ spring-boot-starter-data-mongodb
│  └─ Only active with mongodb profile
├─ spring-boot-starter-data-redis
├─ spring-kafka (disabled by default)
├─ spring-boot-starter-amqp (disabled by default)
├─ spring-boot-starter-security
│  └─ /api/v1/** permitAll for development
└─ spring-boot-starter-actuator

Supporting Libraries
├─ Lombok (Code generation + annotation processor)
├─ Micrometer Prometheus (Metrics export)
├─ JUnit 5 (Testing)
├─ Jackson (JSON serialization for MyBatis)
└─ MySQL Connector/J 8.x

Disabled/Commented Components
├─ Spring Cloud Gateway (incompatible with Spring MVC)
├─ Spring Cloud Config (not needed for local dev)
└─ Resilience4j (version compatibility issues)

MyBatis Configuration
├─ mapper-locations: classpath:mapper/*.xml
├─ type-aliases-package: com.synapsetest.testmanagement.model
├─ Custom JsonTypeHandler for JSON columns
└─ Manual ID generation (UUID.randomUUID().toString())
```

## Key Architecture Changes

### 1. API Version Management
**Before:**
- Global context-path in application.yml: `/api/v1`
- All endpoints automatically prefixed

**After:**
- API versioning in Controllers using `ApiVersion` constants
- System endpoints (e.g., `/health`) without version
- Business endpoints with explicit version: `/api/v1/*`

### 2. Database Layer
**Before:**
- PostgreSQL with Spring Data JPA/Hibernate
- Automatic entity management with `@Entity`
- Repository pattern with JpaRepository

**After:**
- MySQL 8.0 with MyBatis
- XML-based SQL mappings (mapper/*.xml)
- Manual ID and timestamp management
- Custom JsonTypeHandler for JSON columns
- Mapper interfaces with `@Mapper` annotation

### 3. Profile-Based Architecture
**MongoDB Features (Optional):**
- AI Test Case Generation
- Quality Reports
- Real-time Monitoring

To enable MongoDB features:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=mongodb
```

### 4. Controller Structure
All controllers now use:
```java
import com.synapsetest.testmanagement.constants.ApiVersion;

@RestController
@RequestMapping(ApiVersion.V1 + "/resource-name")
public class ResourceController {
    // Business endpoints at /api/v1/resource-name/*
}
```

System controllers (e.g., HealthController):
```java
@RestController
@RequestMapping("")
public class HealthController {
    @GetMapping("/health")  // Accessible at /health
}
```

### 5. Development Environment
**Simplified Setup:**
- Only MySQL required for core functionality
- MongoDB, Kafka, RabbitMQ, Redis disabled by default
- Enable via profiles or remove from `autoconfigure.exclude`

**Port:** 8080  
**Database:** MySQL (localhost:3306/test_management)  
**Health Check:** http://localhost:8080/health  
**API Endpoint:** http://localhost:8080/api/v1/{resource}

---

**Last Updated:** 2025-11-16  
**Spring Boot Version:** 2.7.18  
**Java Version:** 11  
**ORM:** MyBatis 2.3.1

