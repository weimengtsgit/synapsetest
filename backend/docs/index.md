# Backend Directory Exploration - Complete Index

This is a comprehensive guide to the SynapseTest backend architecture. Use this document to navigate the codebase efficiently.

## Documentation Files Overview

This exploration includes 4 comprehensive documents:

1. **backend_exploration_summary.md** - Complete detailed analysis

   - Technology stack details (Spring Boot 2.7.18, MyBatis)
   - Package structure breakdown
   - Database architecture (MySQL + MongoDB with @Profile)
   - User story mapping
   - Development guidelines
2. **quick_reference.md** - Fast lookup guide

   - Framework info (MyBatis, MySQL)
   - Database connections
   - Service inventory
   - API endpoints (with ApiVersion.V1)
   - MyBatis mapper methods
   - Development checklist
3. **architecture_diagram.md** - Visual representations

   - High-level architecture
   - Component interactions
   - Data flow diagrams
   - Database relationships
   - Request/response flows
4. **index.md** - This file

   - Document navigation
   - Quick file location guide
   - Key information at a glance

---

## Quick File Location Guide

### By File Type

#### Models (8 POJOs - No JPA Annotations)

| File                 | Story | Location                        | Notes             |
| -------------------- | ----- | ------------------------------- | ----------------- |
| TestTask.java        | US1   | `/model/TestTask.java`        | MyBatis POJO      |
| TestEnvironment.java | US1   | `/model/TestEnvironment.java` | MyBatis POJO      |
| TestVersion.java     | US1   | `/model/TestVersion.java`     | MyBatis POJO      |
| ResourcePool.java    | US1   | `/model/ResourcePool.java`    | MyBatis POJO      |
| TestCase.java        | US2   | `/model/TestCase.java`        | MyBatis POJO      |
| AIModel.java         | US2   | `/model/AIModel.java`         | MongoDB @Document |
| QualityReport.java   | US3   | `/model/QualityReport.java`   | MongoDB @Document |
| MonitoringData.java  | US3   | `/model/MonitoringData.java`  | MongoDB @Document |

#### Services (13 files)

| File                               | Story | Responsibility          |
| ---------------------------------- | ----- | ----------------------- |
| TestTaskService.java               | US1   | Task CRUD and lifecycle |
| TestEnvironmentService.java        | US1   | Environment management  |
| TestVersionService.java            | US1   | Version management      |
| ResourcePoolService.java           | US1   | Resource allocation     |
| TestRecommendationService.java     | US1   | AI recommendations      |
| AITestCaseGenerationService.java   | US2   | Generate from text      |
| AITestCaseOptimizationService.java | US2   | Optimize cases          |
| AIModelService.java                | US2   | Model lifecycle         |
| TestCaseService.java               | US2   | Case CRUD               |
| QualityReportService.java          | US3   | Report generation       |
| MonitoringService.java             | US3   | Real-time monitoring    |
| ReportingService.java              | US3   | Report details          |
| QualityTraceabilityService.java    | US3   | Quality tracking        |

#### Controllers (7 files)

| File                           | Endpoints                 | Story  | Notes               |
| ------------------------------ | ------------------------- | ------ | ------------------- |
| HealthController.java          | /health                   | System | No version prefix   |
| TestTaskController.java        | /api/v1/test-tasks        | US1    | ApiVersion.V1       |
| TestEnvironmentController.java | /api/v1/test-environments | US1    | ApiVersion.V1       |
| TestVersionController.java     | /api/v1/test-versions     | US1    | ApiVersion.V1       |
| TestCaseController.java        | /api/v1/test-cases        | US2    | ApiVersion.V1       |
| ReportController.java          | /api/v1/reports           | US3    | @Profile("mongodb") |
| MonitoringController.java      | /api/v1/monitoring        | US3    | @Profile("mongodb") |

#### MyBatis Mappers (5 files)

| File                       | Database | Story | XML Location                     |
| -------------------------- | -------- | ----- | -------------------------------- |
| TestTaskMapper.java        | MySQL    | US1   | mapper/TestTaskMapper.xml        |
| TestEnvironmentMapper.java | MySQL    | US1   | mapper/TestEnvironmentMapper.xml |
| TestVersionMapper.java     | MySQL    | US1   | mapper/TestVersionMapper.xml     |
| ResourcePoolMapper.java    | MySQL    | US1   | mapper/ResourcePoolMapper.xml    |
| TestCaseMapper.java        | MySQL    | US2   | mapper/TestCaseMapper.xml        |

#### MongoDB Repositories (3 files - @Profile required)

| File                          | Database | Story |
| ----------------------------- | -------- | ----- |
| AIModelRepository.java        | MongoDB  | US2   |
| QualityReportRepository.java  | MongoDB  | US3   |
| MonitoringDataRepository.java | MongoDB  | US3   |

---

## User Story Quick Links

### User Story 1: Intelligent Test Task Scheduling

**Goal:** Smart task scheduling with AI recommendations and resource management

**Key Files:**

- Models: TestTask, TestEnvironment, TestVersion, ResourcePool
- Services: TestTaskService, ResourcePoolService, TestRecommendationService
- Controller: TestTaskController
- DB: PostgreSQL (test_tasks, test_environments, test_versions, resource_pools)
- Key Methods: createTestTask(), allocateResources(), getRecommendation()

**Critical Paths:**

- Task Creation: `POST /api/v1/test-tasks` → TestTaskController → TestTaskService → PostgreSQL
- Resource Allocation: `ResourcePoolService.allocateResources()` → Capacity check → Update DB
- Task Scheduling: `TestRecommendationService.getTestRecommendation()` → AI logic

### User Story 2: AI-Powered Test Case Generation

**Goal:** Generate test cases from natural language using AI/ML

**Key Files:**

- Models: TestCase, AIModel
- Services: AITestCaseGenerationService, AITestCaseOptimizationService, AIModelService
- Controller: TestCaseController
- DB: PostgreSQL (test_cases), MongoDB (ai_models)
- Key Methods: generateTestCases(), optimizeTestCases(), calculateConfidenceScore()

**Critical Paths:**

- Generation: `POST /api/v1/test-cases/generate` → AITestCaseGenerationService → Scenario analysis → MySQL
- Optimization: `AITestCaseOptimizationService.optimize()` → ML model evaluation
- Security Audit: `AIModelService` → Vulnerability scan → Compliance tracking

### User Story 3: Monitoring & Reporting (Visualization & Analysis)

**Goal:** Real-time test monitoring and comprehensive quality reporting

**Key Files:**

- Models: QualityReport, MonitoringData
- Services: MonitoringService, QualityReportService, ReportingService, QualityTraceabilityService
- Controllers: MonitoringController, ReportController
- DB: MongoDB (quality_reports, monitoring_data)
- Key Methods: updateMonitoringData(), generateReport(), assessRisk()

**Critical Paths:**

- Monitoring: `GET /api/v1/monitoring/{taskId}` → MonitoringService → Real-time metrics
- Report Generation: Task completion → QualityReportService → Risk assessment → Report creation
- Risk Analysis: Defect stats + Performance metrics → Risk calculation → Recommendations

---

## Database Location Reference

### MySQL Tables (MyBatis)

**Connection:** `jdbc:mysql://localhost:3306/test_management`

- **test_tasks** (US1) - CHAR(36) UUIDs, JSON columns
- **test_cases** (US2) - JSON: steps, tags (via JsonTypeHandler)
- **test_environments** (US1) - JSON: config
- **test_versions** (US1) - JSON: config
- **resource_pools** (US1) - JSON: config
- **task_test_cases** (Association)

**Schema Location:** `/src/main/resources/schema.sql` (MySQL DDL)
**Mapper XMLs:** `/src/main/resources/mapper/*.xml`

### MongoDB Collections (Requires @Profile("mongodb"))

**Connection:** `mongodb://localhost:27017/test_management_ai`

- **ai_models** (US2) - @Document
- **quality_reports** (US3) - @Document
- **monitoring_data** (US3) - @Document

---

## Configuration Files

### Spring Boot Configuration

- **Main Config:** `/src/main/resources/application.yml`

  - Server: port 8080, **NO context-path** (API versioning in Controllers)
  - Datasource: MySQL (jdbc:mysql://localhost:3306/test_management)
  - MyBatis: mapper-locations, type-aliases-package
  - Autoconfigure exclusions: MongoDB, Redis, Kafka, RabbitMQ, JPA
  - Management: Prometheus metrics enabled
- **Additional Config:** `/src/main/resources/application-config.yml`
- **Service Discovery:** `/src/main/resources/service-discovery.yml`

### Configuration Classes

Located in `/config/`:

- **SecurityConfig.java** - Auth setup (SecurityFilterChain for Spring Boot 2.7)
- **JsonTypeHandler.java** - MyBatis JSON column handler
- **ApiVersion.java** - API version constants (`/api/v1`, `/api/v2`)
- KafkaConfig.java - Stream processing (disabled in dev)
- RabbitMQConfig.java - Async messaging (disabled in dev)
- MonitoringConfig.java - Prometheus integration
- ~~GatewayConfig.java~~ - Commented out (incompatible with Spring MVC)
- WebConfig.java - Web settings

---

## Directory Tree (Complete)

```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/synapsetest/testmanagement/
│   │   │   ├── config/               (6+ config files, incl. JsonTypeHandler)
│   │   │   ├── constants/            (ApiVersion)
│   │   │   ├── controller/           (7 controllers)
│   │   │   ├── service/              (13 services)
│   │   │   ├── model/                (8 POJOs - no JPA annotations)
│   │   │   ├── mapper/               (5 MyBatis mappers)
│   │   │   ├── repository/           (3 MongoDB repositories with @Profile)
│   │   │   ├── dto/                  (6 DTOs)
│   │   │   ├── entity/               (BaseEntity - no JPA annotations)
│   │   │   ├── exception/            (Custom exceptions)
│   │   │   ├── interceptor/          (HTTP interceptors)
│   │   │   └── TestManagementApplication.java
│   │   └── resources/
│   │       ├── application.yml      (no context-path)
│   │       ├── application-config.yml
│   │       ├── service-discovery.yml
│   │       ├── schema.sql           (MySQL DDL)
│   │       └── mapper/              (5 MyBatis XML mappings)
│   │           ├── TestCaseMapper.xml
│   │           ├── TestTaskMapper.xml
│   │           ├── ResourcePoolMapper.xml
│   │           ├── TestEnvironmentMapper.xml
│   │           └── TestVersionMapper.xml
│   └── tests/
│       ├── unit/
│       ├── integration/
│       └── contract/
├── pom.xml                           (Maven config)
└── Dockerfile                        (Docker config)
```

---

## Package Organization

```
com.synapsetest.testmanagement
├── config              Spring configuration beans, JsonTypeHandler
├── constants           ApiVersion constants
├── controller          REST API endpoints (@RestController)
├── service             Business logic (@Service)
├── model               POJOs (no JPA annotations, @Data from Lombok)
├── mapper              MyBatis Mapper interfaces (@Mapper)
├── repository          MongoDB repositories (@Repository, @Profile)
├── dto                 Transfer objects (Request/Response)
├── entity              Base entity classes (no JPA annotations)
├── exception           Custom exception classes
└── interceptor         HTTP interceptors
```

---

## API Base URL

```
Port: 8080
System Endpoints: http://localhost:8080/health
Business APIs: http://localhost:8080/api/v1
```

### Main Endpoint Groups

- `/health` - System health (no version)
- `/api/v1/test-tasks` - Test task management
- `/api/v1/test-cases` - Test case management (with optional AI features)
- `/api/v1/test-environments` - Environment configuration
- `/api/v1/test-versions` - Version management
- `/api/v1/reports` - Quality reports (@Profile("mongodb") required)
- `/api/v1/monitoring` - Real-time monitoring (@Profile("mongodb") required)

---

## Key Statistics

| Metric               | Value        |
| -------------------- | ------------ |
| Total Models/POJOs   | 8            |
| Total Services       | 13           |
| Total Controllers    | 7            |
| MyBatis Mappers      | 5            |
| MyBatis XML Files    | 5            |
| MongoDB Repositories | 3 (@Profile) |
| MySQL Tables         | 6            |
| MongoDB Collections  | 3            |
| API Endpoints        | 30+          |
| Configuration Files  | 3            |
| Config Classes       | 6+           |

---

## Technology Stack Summary

| Component     | Technology                | Version           |
| ------------- | ------------------------- | ----------------- |
| Framework     | Spring Boot               | 2.7.18            |
| Language      | Java                      | 11                |
| Build Tool    | Maven                     | 3.x               |
| ORM           | MyBatis                   | 2.3.1             |
| Primary DB    | MySQL                     | 8.0               |
| NoSQL DB      | MongoDB                   | Latest (@Profile) |
| Cache         | Redis                     | Disabled in dev   |
| Message Queue | Kafka/RabbitMQ            | Disabled in dev   |
| API Gateway   | ~~Spring Cloud Gateway~~ | Commented out     |
| Testing       | JUnit 5                   | Latest            |
| Resilience    | ~~Resilience4j~~         | Commented out     |
| Monitoring    | Prometheus                | Latest            |

---

## Development Workflow

### To Add a New Feature:

1. Create POJO Model in `/model/` (extends BaseEntity, NO JPA annotations)
2. Create MyBatis Mapper interface in `/mapper/` with `@Mapper`
3. Create MyBatis XML mapping in `/resources/mapper/` with SQL statements
4. Add JsonTypeHandler to XML if using JSON columns
5. Create Service in `/service/` (manual ID/timestamp generation, NO @Transactional)
6. Create Controller in `/controller/` using `ApiVersion.V1`
7. Add DTOs in `/dto/` if needed (use String for ID fields)
8. Update `/resources/schema.sql` for DB changes (MySQL DDL)
9. For MongoDB features, add `@Profile("mongodb")` to all related classes
10. Add tests to `/tests/`

### To Understand Existing Code:

1. Start with User Story mapping (see section above)
2. Locate corresponding POJO model in `/model/`
3. Check MyBatis Mapper interface in `/mapper/`
4. Review MyBatis XML mapping in `/resources/mapper/`
5. Review service implementation in `/service/`
6. Check controller endpoints in `/controller/`
7. Examine database schema in `schema.sql`

---

## Important Notes

### Data Storage Strategy

- **MySQL:** Relational data (tasks, cases, environments, versions, resources)
- **MyBatis XML:** SQL mappings for full control
- **MongoDB:** Flexible schema data (AI models, quality reports, monitoring) - **@Profile("mongodb") required**
- **Redis/Kafka/RabbitMQ:** Disabled by default in development

### Architecture Patterns

- MyBatis Mapper Pattern for data access (XML-based)
- Service Layer for business logic
- DTO Pattern for API communication
- Dependency Injection for loose coupling
- Global Exception Handling
- Profile-Based Loading for optional features
- API Versioning via Controller constants

### Key Features

- RESTful API with standard HTTP methods
- Request validation with JSR-303 (`@Valid`)
- Manual ID generation (`UUID.randomUUID().toString()`)
- Custom JsonTypeHandler for MySQL JSON columns
- Logging with SLF4J
- Health checks and metrics
- ~~Circuit breaker~~ (commented out - version incompatibility)
- Prometheus monitoring
- Optional AI features via @Profile

---

## Document Navigation Matrix

| Need                  | Document                       | Section                             |
| --------------------- | ------------------------------ | ----------------------------------- |
| Architecture overview | architecture_diagram.md        | High-Level Architecture             |
| File locations        | INDEX.md                       | Quick File Location Guide           |
| Service details       | backend_exploration_summary.md | Service Layer (Section 3.3)         |
| Database schema       | backend_exploration_summary.md | Database Architecture (Section 4)   |
| API endpoints         | quick_reference.md             | API Endpoints Structure             |
| User story info       | backend_exploration_summary.md | User Story Mapping (Section 6)      |
| Development guide     | backend_exploration_summary.md | Development Guidelines (Section 12) |
| Quick lookup          | quick_reference.md             | All sections                        |

---

## Last Updated

- Framework: Spring Boot 2.7.18
- ORM: MyBatis 2.3.1 (replaced JPA/Hibernate)
- Java Version: 11
- Database: MySQL 8.0 + MongoDB (@Profile required)
- API Versioning: Controller-based via ApiVersion.V1
- Total Components: 50+ files in main codebase
- Key Migration: PostgreSQL → MySQL, JPA → MyBatis, context-path removed

---

END OF INDEX
