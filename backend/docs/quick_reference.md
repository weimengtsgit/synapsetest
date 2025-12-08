# Backend Quick Reference Guide

## Framework Information
- **Framework:** Spring Boot 2.7.18
- **Language:** Java 11
- **Build Tool:** Maven
- **ORM:** MyBatis 2.3.1 (replaced JPA/Hibernate)
- **Project Root:** `/Users/mengwei/ww/github/synapsetest-qwen/backend`

## Database Connections
```
MySQL:       jdbc:mysql://localhost:3306/test_management
MongoDB:     mongodb://localhost:27017/test_management_ai (requires @Profile("mongodb"))
Redis:       localhost:6379 (disabled by default in dev)
Kafka:       localhost:9092 (disabled by default in dev)
RabbitMQ:    localhost:5672 (disabled by default in dev)
```

## Core Entity Models

### User Story 1: Test Task Scheduling
- **TestTask** - Main test task entity
- **TestEnvironment** - Environment configuration
- **TestVersion** - Version configuration
- **ResourcePool** - Test resource management

### User Story 2: AI Test Case Generation
- **TestCase** - Test case definition
- **AIModel** - AI model metadata with security audit

### User Story 3: Monitoring & Reporting
- **QualityReport** - Test result analysis and risk assessment
- **MonitoringData** - Real-time test execution metrics

## Service Layer (13 Total)

### US1 Services
1. TestTaskService - Task CRUD and lifecycle
2. TestEnvironmentService - Environment management
3. TestVersionService - Version management
4. ResourcePoolService - Resource allocation/release
5. TestRecommendationService - AI recommendations

### US2 Services
6. AITestCaseGenerationService - Generate from text
7. AITestCaseOptimizationService - Optimize cases
8. AIModelService - Model lifecycle
9. TestCaseService - Case CRUD

### US3 Services
10. QualityReportService - Report generation & risk assessment
11. MonitoringService - Real-time monitoring
12. ReportingService - Report generation
13. QualityTraceabilityService - Quality tracking

## API Endpoints Structure
- **Port:** 8080
- **System Endpoints (no version):**
  - `/health` (HealthController)
- **Business API v1:** `http://localhost:8080/api/v1`
  - `/api/v1/test-tasks` (TestTaskController)
  - `/api/v1/test-cases` (TestCaseController)
  - `/api/v1/test-environments` (TestEnvironmentController)
  - `/api/v1/test-versions` (TestVersionController)
  - `/api/v1/reports` (ReportController - requires @Profile("mongodb"))
  - `/api/v1/monitoring` (MonitoringController - requires @Profile("mongodb"))

## Database Design

### MySQL Tables (MyBatis XML Mappers)
- **test_tasks** (CHAR(36) UUID, JSON columns)
- **test_cases** (with JSON: steps, tags)
- **test_environments** (with JSON: config)
- **test_versions** (with JSON: config)
- **resource_pools** (with JSON: config)
- **task_test_cases** (Association table)

**MyBatis XML Mappings:** `backend/src/main/resources/mapper/*.xml`
- TestCaseMapper.xml
- TestTaskMapper.xml
- ResourcePoolMapper.xml
- TestEnvironmentMapper.xml
- TestVersionMapper.xml

### MongoDB Collections (requires @Profile("mongodb"))
- **ai_models** (AI model records)
- **quality_reports** (Quality analysis reports)
- **monitoring_data** (Real-time metrics)

## File Structure Reference

```
src/main/java/com/synapsetest/testmanagement/
├── config/              # Spring config beans (6 files)
├── constants/           # ApiVersion constants
├── controller/          # REST endpoints (7 files)
├── service/             # Business logic (13 files)
├── model/               # POJOs (8 files, no JPA annotations)
├── mapper/              # MyBatis Mappers (5 files)
├── repository/          # MongoDB repositories (3 files, @Profile)
├── dto/                 # Transfer objects (6 files)
├── entity/              # Base entity
├── exception/           # Custom exceptions (3 files)
└── interceptor/         # Request interceptors (2 files)

resources/
├── application.yml      # Main config (no context-path)
├── schema.sql           # MySQL schema
├── mapper/              # MyBatis XML mappings (5 files)
└── application-config.yml
```

## Key Design Patterns Used

1. **Mapper Pattern** - MyBatis XML-based data access
2. **Service Layer Pattern** - Business logic separation
3. **DTO Pattern** - Request/response mapping
4. **Exception Handling** - Global exception handler
5. **Dependency Injection** - Spring IoC
6. **Manual ID Generation** - UUID.randomUUID().toString()
7. **Custom TypeHandler** - JsonTypeHandler for JSON columns
8. **Profile-Based Loading** - @Profile("mongodb") for optional features

## Important Features

### User Story 1: Intelligent Scheduling
- Task lifecycle: PENDING → RUNNING → COMPLETED/CANCELLED
- Priority levels: 0-10 scale
- Resource allocation with capacity checks
- Environment and version management

### User Story 2: AI Test Generation
- Natural language input processing
- Scenario extraction
- Test step generation
- Confidence scoring
- Support for FUNCTIONAL, PERFORMANCE, SECURITY types
- Security audit for AI models

### User Story 3: Monitoring & Reporting
- Real-time progress tracking
- Quality metrics calculation
- Pass rate computation
- Risk assessment (LOW, MEDIUM, HIGH, CRITICAL)
- Module-level risk identification
- Defect classification (critical, major, minor)
- Performance metrics (response time, throughput)

## Important Annotations Used

```java
@Mapper              // MyBatis mapper interface
@Document            // MongoDB document
@Profile("mongodb")  // Conditional bean loading
@Service             // Business logic
@RestController      // REST endpoint
@RequestMapping(ApiVersion.V1 + "/resource")  // API versioning
@Data                // Lombok getter/setter
@Slf4j               // Logging
@Autowired(required = false)  // Optional dependencies
@Valid               // Bean validation
```

## Test Structure (Empty but Ready)
- `/tests/unit/` - Unit tests
- `/tests/integration/` - Integration tests
- `/tests/contract/` - Contract tests

## Configuration Highlights

```yaml
# Server Config
server.port: 8080
# NO context-path (API versioning in Controllers via ApiVersion.V1)

# Database Config
spring.datasource.url: jdbc:mysql://localhost:3306/test_management
mybatis.mapper-locations: classpath:mapper/*.xml
mybatis.type-aliases-package: com.synapsetest.testmanagement.model

# Disabled in Dev (autoconfigure.exclude)
# - MongoDB (requires --spring.profiles.active=mongodb)
# - Redis, Kafka, RabbitMQ

# Monitoring
management.endpoints.web.exposure.include: health,info,metrics,prometheus

# Security (Development)
/api/v1/** - permitAll
```

## Common Task Queries

### MyBatis Mapper Methods (MySQL)
```
TestTaskMapper:
  - selectById(id)
  - selectAll()
  - selectByStatus(status)
  - selectByEnvironment(environment)
  - selectByCreatedBy(username)
  - insert(testTask), update(testTask), deleteById(id)

TestCaseMapper:
  - selectById(id), selectAll()
  - selectByType(type), selectByStatus(status)
  - Uses JsonTypeHandler for steps, tags

ResourcePoolMapper:
  - selectByStatus(status), selectByType(type)
  - selectByStatusAndType(status, type)
  - Uses JsonTypeHandler for config
```

### MongoDB Repository Methods (requires @Profile)
```
QualityReportRepository:
  - findByTaskId(taskId)
  - findByStatus(status)
  - findTop10ByOrderByGeneratedAtDesc()
```

## Development Checklist

When adding new features:
1. [ ] Create POJO model in `/model/` (no JPA annotations)
2. [ ] Create MyBatis Mapper interface in `/mapper/`
3. [ ] Create MyBatis XML mapping in `/resources/mapper/`
4. [ ] Add JsonTypeHandler if using JSON columns
5. [ ] Create service in `/service/` with manual ID/timestamp generation
6. [ ] Create controller in `/controller/` using `ApiVersion.V1`
7. [ ] Create DTOs in `/dto/` if needed
8. [ ] Update `schema.sql` for MySQL schema changes
9. [ ] Add tests to `/tests/`
10. [ ] For MongoDB features, add `@Profile("mongodb")`

## Important Constants

### Task Status
- PENDING
- RUNNING
- COMPLETED
- CANCELLED

### Test Types
- FUNCTIONAL
- PERFORMANCE
- SECURITY

### Environment Status
- AVAILABLE
- MAINTENANCE
- UNAVAILABLE

### Risk Levels
- LOW
- MEDIUM
- HIGH
- CRITICAL

### Resource Pool Types
- VM
- CONTAINER
- DEVICE

