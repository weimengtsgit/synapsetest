# Backend Architecture Exploration Summary

## Project Overview
**Name:** SynapseTest - AI驱动测试任务管理系统 (AI-Driven Test Task Management System)  
**Version:** 1.0.0-SNAPSHOT  
**Type:** Multi-module microservices backend  

---

## 1. Technology Stack & Framework

### Primary Framework: Spring Boot 2.7.18
- **Language:** Java 11
- **Build Tool:** Maven 3.x
- **Parent POM:** spring-boot-starter-parent:2.7.18

### Key Technologies:
- **Web Framework:** Spring Boot Web (REST APIs)
- **ORM/Data Access:** MyBatis 2.3.1 (XML-based SQL mappings)
- **Primary Database:** MySQL 8.0 (relational data)
- **NoSQL Database:** MongoDB (AI models, reports, monitoring) - **@Profile("mongodb") required**
- **Cache:** Redis (disabled by default in development)
- **Message Queues:** 
  - Kafka (disabled by default in development)
  - RabbitMQ (disabled by default in development)
- **API Gateway:** ~~Spring Cloud Gateway~~ (commented out - incompatible with Spring MVC)
- **Resilience:** ~~Resilience4j~~ (commented out - version compatibility issues)
- **Monitoring:** Micrometer + Prometheus
- **Testing Framework:** JUnit 5, Spring Boot Test

### Dependencies Summary:
```xml
- spring-boot-starter-web
- mybatis-spring-boot-starter (v2.3.1) - Replaces JPA
- mysql-connector-j (MySQL driver)
- spring-boot-starter-data-mongodb (@Profile required)
- spring-boot-starter-data-redis (disabled in dev)
- spring-kafka (disabled in dev)
- spring-boot-starter-amqp (disabled in dev)
- micrometer-registry-prometheus
- spring-boot-starter-actuator
- spring-boot-starter-validation
- spring-boot-starter-security
- lombok (with annotation processor configuration)
```

---

## 2. Folder Structure & Architecture

### Base Directory
```
/Users/mengwei/ww/github/synapsetest-qwen/backend/
├── src/
│   ├── main/
│   │   ├── java/com/synapsetest/testmanagement/
│   │   │   ├── config/                 (Spring configurations)
│   │   │   ├── constants/              (ApiVersion constants)
│   │   │   ├── controller/             (REST API endpoints)
│   │   │   ├── service/                (Business logic)
│   │   │   ├── model/                  (POJOs - no JPA annotations)
│   │   │   ├── mapper/                 (MyBatis Mapper interfaces)
│   │   │   ├── repository/             (MongoDB repositories with @Profile)
│   │   │   ├── dto/                    (Data transfer objects)
│   │   │   ├── entity/                 (Base entity classes)
│   │   │   ├── exception/              (Custom exceptions)
│   │   │   ├── interceptor/            (HTTP interceptors)
│   │   │   └── TestManagementApplication.java
│   │   └── resources/
│   │       ├── application.yml         (no context-path)
│   │       ├── application-config.yml
│   │       ├── service-discovery.yml
│   │       ├── schema.sql              (MySQL schema)
│   │       └── mapper/                 (MyBatis XML mappings)
│   │           ├── TestCaseMapper.xml
│   │           ├── TestTaskMapper.xml
│   │           ├── ResourcePoolMapper.xml
│   │           ├── TestEnvironmentMapper.xml
│   │           └── TestVersionMapper.xml
│   └── tests/
│       ├── unit/                       (Unit tests)
│       ├── integration/                (Integration tests)
│       └── contract/                   (Contract tests)
├── pom.xml
└── Dockerfile
```

---

## 3. Package Structure Details

### 3.1 Configuration Modules
**Location:** `/src/main/java/com/synapsetest/testmanagement/config/`

| File | Purpose |
|------|---------|
| `SecurityConfig.java` | Spring Security configuration, auth setup |
| `KafkaConfig.java` | Kafka producer/consumer configuration |
| `RabbitMQConfig.java` | RabbitMQ message queue setup |
| `MonitoringConfig.java` | Prometheus/Micrometer metrics configuration |
| `GatewayConfig.java` | Spring Cloud Gateway routing configuration |
| `WebConfig.java` | Web MVC configuration, CORS, interceptors |

### 3.2 Controllers (REST API Endpoints)
**Location:** `/src/main/java/com/synapsetest/testmanagement/controller/`

| Controller | Endpoints | Purpose | Notes |
|-----------|-----------|---------|-------|
| `HealthController.java` | `/health` | Health check endpoint | System endpoint (no version) |
| `TestTaskController.java` | `/api/v1/test-tasks` | Task CRUD, start/cancel operations | Uses ApiVersion.V1 |
| `TestCaseController.java` | `/api/v1/test-cases` | Test case management | Optional AI services |
| `TestEnvironmentController.java` | `/api/v1/test-environments` | Environment configuration | Uses ApiVersion.V1 |
| `TestVersionController.java` | `/api/v1/test-versions` | Version management | Uses ApiVersion.V1 |
| `ReportController.java` | `/api/v1/reports` | Quality report endpoints | @Profile("mongodb") |
| `MonitoringController.java` | `/api/v1/monitoring` | Real-time monitoring data | @Profile("mongodb") |

### 3.3 Service Layer (Business Logic)
**Location:** `/src/main/java/com/synapsetest/testmanagement/service/`

#### User Story 1: Test Task Scheduling Services
| Service | Responsibility |
|---------|-----------------|
| `TestTaskService.java` | Create, retrieve, start, cancel test tasks |
| `TestEnvironmentService.java` | Manage test environments |
| `TestVersionService.java` | Manage test versions |
| `ResourcePoolService.java` | Allocate/release test resources |
| `TestRecommendationService.java` | Provide AI-based task recommendations |

#### User Story 2: AI Test Case Generation Services
| Service | Responsibility |
|---------|-----------------|
| `AITestCaseGenerationService.java` | Generate test cases from natural language using AI/ML |
| `AITestCaseOptimizationService.java` | Optimize generated test cases |
| `AIModelService.java` | Manage AI models (security audit, compliance) |
| `TestCaseService.java` | Test case CRUD operations |

#### User Story 3: Monitoring & Reporting Services
| Service | Responsibility |
|---------|-----------------|
| `QualityReportService.java` | Generate quality reports with risk assessment |
| `MonitoringService.java` | Real-time test execution monitoring |
| `ReportingService.java` | Generate detailed test reports |
| `QualityTraceabilityService.java` | Track quality metrics and traceability |

### 3.4 Data Models (POJOs - MyBatis)
**Location:** `/src/main/java/com/synapsetest/testmanagement/model/`

#### User Story 1 Models (MySQL)
```java
TestTask.java              // Fields: String id, name, description, environment, version, testScope, status, priority, createdBy
                           // No JPA annotations, manual ID generation
TestEnvironment.java       // Fields: name, description, Map<String,String> config, status
                           // JSON column via JsonTypeHandler
TestVersion.java           // Fields: name, description, productVersion, releaseDate, Map<String,String> config
ResourcePool.java          // Fields: name, type, capacity, allocated (renamed from 'used'), Map<String,String> config, status
```

#### User Story 2 Models
```java
TestCase.java              // MySQL: title, description, List<String> steps, expectedResult, priority, type, status, List<String> tags, createdBy
                           // JSON columns (steps, tags) via JsonTypeHandler
AIModel.java               // MongoDB: name, version, description, filePath, securityStatus, vulnerabilityScanResult, complianceStatus, metrics
                           // @Document annotation, @Profile("mongodb")
```

#### User Story 3 Models (MongoDB)
```java
QualityReport.java         // taskId, name, summary, testResults[], defectStats, performanceMetrics, riskAssessment
                           // @Document annotation, @Profile("mongodb")
MonitoringData.java        // taskId, status, progress, executedCases, passedCases, failedCases, resourceUsage, performanceMetrics
                           // @Document annotation, @Profile("mongodb")
```

#### Base Entity
```java
BaseEntity.java            // Abstract parent: String id (changed from UUID), LocalDateTime createdAt, updatedAt
                           // No JPA annotations (@Id, @GeneratedValue removed)
```

### 3.5 Data Access Layer (Mappers & Repositories)
**Location:** `/src/main/java/com/synapsetest/testmanagement/mapper/` and `/repository/`

#### MyBatis Mappers (MySQL)
- `TestTaskMapper` - @Mapper interface, XML: backend/src/main/resources/mapper/TestTaskMapper.xml
  - Methods: selectById(), selectAll(), selectByStatus(), insert(), update(), deleteById()
- `TestCaseMapper` - Uses JsonTypeHandler for steps/tags columns
  - Methods: selectById(), selectByType(), selectByStatus(), insert(), update(), deleteById()
- `TestEnvironmentMapper` - Uses JsonTypeHandler for config
  - Methods: selectById(), selectByName(), selectByStatus(), insert(), update()
- `TestVersionMapper` - Uses JsonTypeHandler for config
  - Methods: selectById(), selectByName(), selectByProductVersion(), insert(), update()
- `ResourcePoolMapper` - Uses JsonTypeHandler for config
  - Methods: selectById(), selectByStatus(), selectByType(), selectByStatusAndType()

#### MongoDB Repositories (@Profile("mongodb") required)
- `QualityReportRepository` - @Repository with @Profile("mongodb")
  - Methods: findByTaskId(), findByStatus(), findByGeneratedAtBetween(), findTop10ByOrderByGeneratedAtDesc()
- `MonitoringDataRepository` - @Repository with @Profile("mongodb")
  - Methods: findByTaskId(), findByStatus()
- `AIModelRepository` - @Repository with @Profile("mongodb")
  - Methods: findBySecurityStatus(), findByName()

### 3.6 DTOs (Data Transfer Objects)
**Location:** `/src/main/java/com/synapsetest/testmanagement/dto/`

| DTO Class | Purpose |
|-----------|---------|
| `TestTaskRequest.java` | Input DTO for creating test tasks |
| `TestTaskResponse.java` | Output DTO with recommendations |
| `TestCaseRequest.java` | Input DTO for test case creation |
| `TestCaseResponse.java` | Output DTO for test cases |
| `AITestCaseGenerationRequest.java` | Input for AI test case generation |
| `ApiResponse.java` | Standard API response wrapper |

### 3.7 Exception Handling
**Location:** `/src/main/java/com/synapsetest/testmanagement/exception/`

- `GlobalExceptionHandler.java` - Centralized exception handling
- `ResourceNotFoundException.java` - 404 errors
- `ValidationException.java` - Validation error handling

### 3.8 Interceptors
**Location:** `/src/main/java/com/synapsetest/testmanagement/interceptor/`

- `AuthInterceptor.java` - Authentication token validation
- `LoggingInterceptor.java` - Request/response logging

---

## 4. Database Architecture

### MySQL Schema (MyBatis XML Mappings)
**Connection:** `jdbc:mysql://localhost:3306/test_management`

#### Tables
1. **test_tasks** (User Story 1)
   - PK: id (CHAR(36) - UUID string)
   - Fields: name, description, environment, version, test_scope, status, priority, created_by
   - Indexes: idx_test_tasks_status, idx_test_tasks_created_at
   - Constraints: status in (PENDING, RUNNING, COMPLETED, CANCELLED), priority 0-10
   - Timestamps: created_at, updated_at (TIMESTAMP with AUTO UPDATE)

2. **test_cases** (User Story 2)
   - PK: id (CHAR(36))
   - Fields: title, description, steps (JSON), expected_result, priority, type, status, tags (JSON), created_by
   - Indexes: idx_test_cases_type, idx_test_cases_status
   - JSON columns handled by JsonTypeHandler

3. **test_environments** (User Story 1)
   - PK: id (CHAR(36))
   - Fields: name (unique), description, url, config (JSON), status
   - Index: idx_test_environments_status

4. **test_versions** (User Story 1)
   - PK: id (CHAR(36))
   - Fields: name, description, product_version, release_date, config (JSON)

5. **resource_pools** (User Story 1)
   - PK: id (CHAR(36))
   - Fields: name (unique), description, type, capacity, allocated (renamed from 'used'), location, config (JSON), status
   - Constraint: allocated <= capacity
   - Index: idx_resource_pools_status

6. **task_test_cases** (Association)
   - PK: (task_id, test_case_id)
   - FKs: References test_tasks, test_cases with CASCADE
   - Field: execution_order

#### Features
- Manual UUID generation via UUID.randomUUID().toString()
- Automatic `created_at`/`updated_at` via MySQL TIMESTAMP DEFAULT and ON UPDATE
- MySQL JSON type for flexible config storage
- Custom JsonTypeHandler for Map<String, String> and List<String> JSON fields
- Foreign key constraints with CASCADE delete
- ENGINE=InnoDB, CHARSET=utf8mb4

### MongoDB Collections (NoSQL - Flexible Schema)
**Connection:** `mongodb://localhost:27017/test_management_ai`

1. **ai_models** (User Story 2)
   - Fields: id, name, version, description, filePath, securityStatus, complianceStatus, vulnerabilityScanResult, metrics

2. **quality_reports** (User Story 3)
   - Fields: taskId, name, summary, testResults[], defectStats, performanceMetrics, riskAssessment

3. **monitoring_data** (User Story 3)
   - Fields: taskId, status, progress, executedCases, passedCases, failedCases, resourceUsage, performanceMetrics

### Redis Cache
**Connection:** `localhost:6379`
- Session storage
- Cache management

---

## 5. Test Structure

### Test Directory Organization
```
/backend/tests/
├── unit/                  (Unit tests - mostly empty, ready for implementation)
├── integration/           (Integration tests - mostly empty, ready for implementation)
└── contract/             (Contract tests - mostly empty, ready for implementation)
```

### Current State
- **Test Framework:** JUnit 5 (Jupiter)
- **Test Support:** spring-boot-starter-test, spring-kafka-test
- **Status:** Test directories exist but are mostly empty (placeholder structure)

### Recommended Test Coverage Areas
1. Service layer tests (business logic)
2. Controller tests (API endpoint validation)
3. Repository tests (data access)
4. Integration tests (end-to-end flows)
5. Contract tests (API contracts)

---

## 6. User Story Mapping

### User Story 1: Intelligent Test Task Scheduling
**Key Components:**
- Models: TestTask, TestEnvironment, TestVersion, ResourcePool
- Services: TestTaskService, TestEnvironmentService, TestVersionService, ResourcePoolService, TestRecommendationService
- Controllers: TestTaskController, TestEnvironmentController, TestVersionController
- DB: PostgreSQL tables (test_tasks, test_environments, test_versions, resource_pools)
- Key Features:
  - Task creation with AI recommendations
  - Dynamic resource allocation
  - Environment and version management
  - Task status tracking (PENDING → RUNNING → COMPLETED/CANCELLED)

**API Endpoints:**
- POST `/api/v1/test-tasks` - Create task with recommendations
- GET `/api/v1/test-tasks/{id}` - Retrieve task
- GET `/api/v1/test-tasks?status=PENDING` - Filter by status
- POST `/api/v1/test-tasks/{id}/start` - Start task
- POST `/api/v1/test-tasks/{id}/cancel` - Cancel task

### User Story 2: AI Test Case Generation
**Key Components:**
- Models: TestCase, AIModel
- Services: AITestCaseGenerationService, AITestCaseOptimizationService, AIModelService, TestCaseService
- Controllers: TestCaseController
- DB: PostgreSQL (test_cases), MongoDB (ai_models)
- Key Features:
  - Generate test cases from natural language
  - AI model management with security audit
  - Scenario-based test case generation
  - Confidence scoring for generated cases
  - Support for multiple test types (FUNCTIONAL, PERFORMANCE, SECURITY)

**API Endpoints:**
- POST `/api/v1/test-cases/generate` - AI-generate test cases
- POST `/api/v1/test-cases` - Create test case
- GET `/api/v1/test-cases` - Retrieve test cases

### User Story 3: Monitoring & Reporting (Test Result Visualization & Analysis)
**Key Components:**
- Models: QualityReport, MonitoringData
- Services: QualityReportService, MonitoringService, ReportingService, QualityTraceabilityService
- Controllers: ReportController, MonitoringController
- DB: MongoDB collections
- Key Features:
  - Real-time test execution monitoring
  - Quality report generation with risk assessment
  - Defect statistics and performance metrics
  - Risk level calculation (LOW, MEDIUM, HIGH, CRITICAL)
  - Module-level risk identification
  - Recommendations based on quality metrics

**API Endpoints:**
- POST `/api/v1/reports/generate` - Generate quality report
- GET `/api/v1/reports/{id}` - Retrieve report
- GET `/api/v1/monitoring/{taskId}` - Get real-time monitoring data
- PUT `/api/v1/monitoring/{taskId}` - Update monitoring progress

---

## 7. Key File Locations Summary

### Models
```
/backend/src/main/java/com/synapsetest/testmanagement/model/
├── TestTask.java                    # User Story 1
├── TestEnvironment.java             # User Story 1
├── TestVersion.java                 # User Story 1
├── ResourcePool.java                # User Story 1
├── TestCase.java                    # User Story 2
├── AIModel.java                     # User Story 2
├── QualityReport.java               # User Story 3
└── MonitoringData.java              # User Story 3
```

### Services
```
/backend/src/main/java/com/synapsetest/testmanagement/service/
├── TestTaskService.java                    # US1
├── TestEnvironmentService.java             # US1
├── TestVersionService.java                 # US1
├── ResourcePoolService.java                # US1
├── TestRecommendationService.java          # US1
├── AITestCaseGenerationService.java        # US2
├── AITestCaseOptimizationService.java      # US2
├── AIModelService.java                     # US2
├── TestCaseService.java                    # US2
├── QualityReportService.java               # US3
├── MonitoringService.java                  # US3
├── ReportingService.java                   # US3
└── QualityTraceabilityService.java         # US3
```

### Controllers
```
/backend/src/main/java/com/synapsetest/testmanagement/controller/
├── TestTaskController.java          # US1
├── TestEnvironmentController.java   # US1
├── TestVersionController.java       # US1
├── TestCaseController.java          # US2
├── ReportController.java            # US3
├── MonitoringController.java        # US3
└── HealthController.java
```

### Repositories
```
/backend/src/main/java/com/synapsetest/testmanagement/repository/
├── TestTaskRepository.java          # US1 (PostgreSQL)
├── TestEnvironmentRepository.java   # US1 (PostgreSQL)
├── TestVersionRepository.java       # US1 (PostgreSQL)
├── ResourcePoolRepository.java      # US1 (PostgreSQL)
├── TestCaseRepository.java          # US2 (PostgreSQL)
├── AIModelRepository.java           # US2 (MongoDB)
├── QualityReportRepository.java     # US3 (MongoDB)
└── MonitoringDataRepository.java    # US3 (MongoDB)
```

### DTOs
```
/backend/src/main/java/com/synapsetest/testmanagement/dto/
├── TestTaskRequest.java
├── TestTaskResponse.java
├── TestCaseRequest.java
├── TestCaseResponse.java
├── AITestCaseGenerationRequest.java
└── ApiResponse.java
```

### Configuration & Resources
```
/backend/src/main/resources/
├── application.yml                  # Main Spring Boot configuration
├── application-config.yml           # Additional config
├── service-discovery.yml            # Service discovery configuration
└── schema.sql                       # PostgreSQL schema

/backend/src/main/java/com/synapsetest/testmanagement/config/
├── SecurityConfig.java
├── KafkaConfig.java
├── RabbitMQConfig.java
├── MonitoringConfig.java
├── GatewayConfig.java
└── WebConfig.java
```

---

## 8. Configuration Details

### Application Configuration (`application.yml`)
```yaml
server:
  port: 8080
  # NO context-path - API versioning in Controllers via ApiVersion.V1

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/test_management
    username: root
    password: ${DB_PASSWORD:root}
    driver-class-name: com.mysql.cj.jdbc.Driver
  
  autoconfigure:
    exclude:
      - MongoDataAutoConfiguration
      - MongoAutoConfiguration
      - RedisAutoConfiguration
      - KafkaAutoConfiguration
      - RabbitAutoConfiguration
      - HibernateJpaAutoConfiguration  # Using MyBatis instead
      - JpaRepositoriesAutoConfiguration

mybatis:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.synapsetest.testmanagement.model
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus

# Resilience4j - Temporarily disabled due to version compatibility
# resilience4j: ...
```

---

## 9. Notable Architecture Decisions

1. **Hybrid Database Strategy**
   - **MySQL** for structured, relational data (tasks, cases, environments, resources)
   - **MyBatis XML mappings** for SQL control and flexibility
   - **MongoDB** for flexible schema data (AI models, reports, monitoring) - **@Profile("mongodb") required**
   - Redis, Kafka, RabbitMQ disabled by default in development

2. **MyBatis Migration (from JPA/Hibernate)**
   - XML-based SQL mappings for better control
   - Custom `JsonTypeHandler` for MySQL JSON columns
   - Manual ID generation using `UUID.randomUUID().toString()`
   - Manual timestamp management in service layer

3. **Profile-Based Loading**
   - Core features work without MongoDB
   - AI features require `--spring.profiles.active=mongodb`
   - Optional dependencies via `@Autowired(required = false)`

4. **API Versioning Strategy**
   - No `context-path` in application.yml
   - Version defined in Controllers via `ApiVersion.V1` constant
   - System endpoints (e.g., `/health`) remain unversioned
   - Business APIs use `/api/v1` prefix

5. **Security**
   - Spring Security with `SecurityFilterChain` (Spring Boot 2.7.x compatible)
   - Development mode: `/api/v1/**` permitAll
   - Auth interceptor for request validation
   - AI model security audit tracking (when MongoDB profile active)

6. **Code Quality**
   - Lombok with explicit annotation processor configuration
   - Bean validation with JSR-303 (`@Valid`)
   - Global exception handling (`GlobalExceptionHandler`)
   - No `@Transactional` (MyBatis doesn't require it for simple CRUD)

---

## 10. Summary Statistics

| Category | Count |
|----------|-------|
| **Models/POJOs** | 8 core models (no JPA annotations) |
| **Services** | 13 services |
| **Controllers** | 7 controllers |
| **MyBatis Mappers** | 5 mappers (XML + interface) |
| **MongoDB Repositories** | 3 repositories (@Profile) |
| **DTOs** | 6 DTO classes |
| **Configuration Classes** | 6+ config files |
| **MySQL Tables** | 6 tables |
| **MyBatis XML Files** | 5 mapper XMLs |
| **MongoDB Collections** | 3 collections (@Profile) |
| **API Endpoints** | 30+ RESTful endpoints |
| **Java Version** | 11 |
| **Spring Boot Version** | 2.7.18 |

---

## 11. Deployment Artifacts

- **Docker:** Dockerfile present for containerization
- **Build Tool:** Maven with standard plugins (including Lombok annotation processor)
- **Docker Compose:** Available for multi-service orchestration
- **Database:** Manual schema creation via `schema.sql` (MySQL DDL)
- **Schema File:** backend/src/main/resources/schema.sql

---

## 12. Development Guidelines

### Adding New Features
1. Create POJO model in `/model/` (extends BaseEntity, NO JPA annotations)
2. Create MyBatis Mapper interface in `/mapper/` with `@Mapper`
3. Create MyBatis XML mapping in `/resources/mapper/` with SQL statements
4. If using JSON columns, add `JsonTypeHandler` to resultMap and insert/update
5. Create service in `/service/` with manual ID generation and timestamps
6. Create controller in `/controller/` with `@RequestMapping(ApiVersion.V1 + "/resource")`
7. Add DTOs in `/dto/` for request/response mapping (use String for ID fields)
8. Add database schema changes in `schema.sql` (MySQL syntax)
9. For MongoDB features, add `@Profile("mongodb")` to classes

### Adding Tests
1. Unit tests go to `/tests/unit/`
2. Integration tests go to `/tests/integration/`
3. Contract tests go to `/tests/contract/`
4. Use @SpringBootTest for integration tests
5. Use @MybatisTest (or custom test config) for mapper tests
6. Use @WebMvcTest for controller tests
7. Mock optional services (e.g., AI services with @Profile)

