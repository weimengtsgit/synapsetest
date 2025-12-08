# Swagger API 文档使用指南

## 概述

SynapseTest Backend已集成Springdoc OpenAPI 3.0（现代化的Swagger实现），提供完整的RESTful API交互式文档。

## 快速开始

### 1. 启动应用

```bash
cd backend
mvn spring-boot:run
```

### 2. 访问Swagger UI

启动成功后，在浏览器中访问：

- **Swagger UI（交互式文档）**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs
- **OpenAPI YAML**: http://localhost:8080/v3/api-docs.yaml

## Swagger UI功能介绍

### 主界面功能

1. **API分组标签**
   - 测试任务管理
   - 测试用例管理
   - 监控与报告
   - 测试环境管理
   - 版本管理
   - 系统健康检查

2. **服务器环境切换**
   - 开发环境: http://localhost:8080
   - 测试环境: http://test-api.synapsetest.com
   - 生产环境: https://api.synapsetest.com

3. **API搜索**
   - 在顶部搜索框输入关键词快速定位API

### 使用示例

#### 示例1: 创建测试任务

1. 展开"测试任务管理"标签
2. 点击 `POST /api/v1/test-tasks` 接口
3. 点击"Try it out"按钮
4. 在请求体中填写JSON：

```json
{
  "taskName": "冒烟测试-订单模块",
  "environment": "DEV",
  "version": "v1.2.0",
  "modules": ["order", "payment"],
  "codeChangeInfo": {
    "changedFilesCount": 3,
    "changedLinesCount": 25,
    "isHotfix": false
  }
}
```

5. 在Header中添加 `X-User-Name: zhangsan`
6. 点击"Execute"按钮执行请求
7. 查看响应结果

#### 示例2: 查询任务状态

1. 展开 `GET /api/v1/test-tasks`
2. 在Parameters中输入 `status=PENDING`
3. 点击"Execute"
4. 查看返回的待执行任务列表

## 技术架构

### 依赖配置

**pom.xml**:
```xml
<!-- Springdoc OpenAPI (Swagger) -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-ui</artifactId>
    <version>1.7.0</version>
</dependency>
```

### 配置文件

**application.yml**:
```yaml
springdoc:
  api-docs:
    enabled: true
    path: /v3/api-docs
  swagger-ui:
    enabled: true
    path: /swagger-ui.html
    tags-sorter: alpha
    operations-sorter: alpha
    display-request-duration: true
    try-it-out-enabled: true
  paths-to-match:
    - /api/**
  packages-to-scan:
    - com.synapsetest.testmanagement.controller
```

### 配置类

**OpenApiConfig.java**:
```java
@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(serverList())
                .tags(tagList());
    }
}
```

## Controller注解使用

### 基本注解

#### @Tag - API分组标签

```java
@Tag(name = "测试任务管理", description = "测试任务的创建、查询、更新、删除及AI推荐功能")
@RestController
@RequestMapping("/api/v1/test-tasks")
public class TestTaskController {
    // ...
}
```

#### @Operation - 接口说明

```java
@Operation(
    summary = "创建测试任务",
    description = "创建一个新的测试任务。系统会根据代码变更信息自动调用AI服务推荐测试策略。",
    tags = {"测试任务管理"}
)
@PostMapping
public ResponseEntity<ApiResponse<TestTaskResponse>> createTestTask(...) {
    // ...
}
```

#### @Parameter - 参数说明

```java
@PostMapping
public ResponseEntity<ApiResponse<TestTaskResponse>> createTestTask(
    @Parameter(
        description = "测试任务创建请求",
        required = true,
        example = "{\"taskName\": \"冒烟测试\", ...}"
    )
    @RequestBody TestTaskRequest request
) {
    // ...
}
```

#### @ApiResponses - 响应说明

```java
@ApiResponses(value = {
    @ApiResponse(
        responseCode = "201",
        description = "测试任务创建成功",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiResponse.class),
            examples = @ExampleObject(value = "{...}")
        )
    ),
    @ApiResponse(
        responseCode = "400",
        description = "请求参数验证失败"
    )
})
```

### 完整示例

参考文件：`TestTaskControllerWithSwagger.java`

```java
@Tag(name = "测试任务管理")
@RestController
@RequestMapping("/api/v1/test-tasks")
public class TestTaskController {

    @Operation(summary = "创建测试任务", description = "...")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "创建成功"),
        @ApiResponse(responseCode = "400", description = "参数错误")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<TestTaskResponse>> createTestTask(
        @Parameter(description = "任务请求", required = true)
        @RequestBody TestTaskRequest request,

        @Parameter(description = "用户名", example = "zhangsan")
        @RequestHeader(value = "X-User-Name", defaultValue = "system")
        String username
    ) {
        // 业务逻辑
    }
}
```

## DTO/Model注解

### @Schema - 数据模型说明

```java
@Data
@Schema(description = "测试任务请求对象")
public class TestTaskRequest {

    @Schema(description = "任务名称", example = "冒烟测试-订单模块", required = true)
    @NotBlank(message = "任务名称不能为空")
    private String taskName;

    @Schema(description = "测试环境", example = "DEV", allowableValues = {"DEV", "TEST", "STAGING", "PROD"})
    private String environment;

    @Schema(description = "版本号", example = "v1.2.0")
    private String version;

    @Schema(description = "测试模块列表", example = "[\"order\", \"payment\"]")
    private List<String> modules;

    @Schema(description = "代码变更信息")
    private CodeChangeInfo codeChangeInfo;
}
```

### 嵌套对象示例

```java
@Data
@Schema(description = "代码变更信息")
public class CodeChangeInfo {

    @Schema(description = "变更文件数量", example = "3")
    private Integer changedFilesCount;

    @Schema(description = "变更代码行数", example = "25")
    private Integer changedLinesCount;

    @Schema(description = "是否为Hotfix", example = "false")
    private Boolean isHotfix;
}
```

## 最佳实践

### 1. 统一响应格式

使用 `ApiResponse<T>` 包装所有响应：

```java
@Data
@Schema(description = "API统一响应格式")
public class ApiResponse<T> {
    @Schema(description = "是否成功", example = "true")
    private boolean success;

    @Schema(description = "响应消息", example = "操作成功")
    private String message;

    @Schema(description = "响应数据")
    private T data;

    @Schema(description = "错误信息")
    private String error;

    @Schema(description = "时间戳", example = "2024-01-15T10:00:00Z")
    private String timestamp;
}
```

### 2. 提供完整示例

在 `@ExampleObject` 中提供真实的JSON示例：

```java
@ApiResponse(
    responseCode = "201",
    content = @Content(
        examples = @ExampleObject(value = """
            {
              "success": true,
              "message": "Test task created successfully",
              "data": {
                "id": "task-001",
                "taskName": "冒烟测试-订单模块",
                "status": "PENDING"
              }
            }
            """)
    )
)
```

### 3. 详细的错误响应

为每种错误情况提供示例：

```java
@ApiResponse(
    responseCode = "400",
    description = "请求参数验证失败",
    content = @Content(
        examples = @ExampleObject(value = """
            {
              "success": false,
              "error": "Validation Error",
              "message": "taskName不能为空"
            }
            """)
    )
)
```

### 4. 枚举值说明

使用 `allowableValues` 限制参数值：

```java
@Parameter(
    description = "任务状态",
    schema = @Schema(
        type = "string",
        allowableValues = {"PENDING", "RUNNING", "COMPLETED", "FAILED", "CANCELLED"}
    )
)
```

## 与前端集成

### 生成TypeScript类型定义

使用 `openapi-generator` 生成前端类型：

```bash
# 安装工具
npm install -g @openapitools/openapi-generator-cli

# 生成TypeScript类型
openapi-generator-cli generate \
  -i http://localhost:8080/v3/api-docs \
  -g typescript-axios \
  -o frontend/src/api/generated
```

### 在前端代码中使用

```typescript
import { TestTaskApi, TestTaskRequest } from '@/api/generated';

const api = new TestTaskApi();

const createTask = async () => {
  const request: TestTaskRequest = {
    taskName: "冒烟测试-订单模块",
    environment: "DEV",
    version: "v1.2.0"
  };

  const response = await api.createTestTask(request, "zhangsan");
  console.log(response.data);
};
```

## 环境配置

### 开发环境配置

**application-dev.yml**:
```yaml
springdoc:
  swagger-ui:
    enabled: true
  api-docs:
    enabled: true
```

### 生产环境配置

**application-prod.yml**:
```yaml
springdoc:
  swagger-ui:
    enabled: false  # 生产环境禁用UI
  api-docs:
    enabled: false  # 生产环境禁用文档
```

### 安全配置

如果启用了Spring Security，需要放行Swagger路径：

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
            .antMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
            .anyRequest().authenticated();
    }
}
```

## 常见问题

### 1. Swagger UI无法访问

**问题**: 访问 http://localhost:8080/swagger-ui.html 返回404

**解决方案**:
- 确认依赖已添加到pom.xml
- 检查application.yml配置是否正确
- 确认Controller包路径在扫描范围内
- 运行 `mvn clean install` 重新编译

### 2. 接口不显示

**问题**: 某些Controller的接口在Swagger UI中不显示

**解决方案**:
- 确认Controller类有 `@RestController` 注解
- 检查 `@RequestMapping` 路径是否匹配配置的 `paths-to-match`
- 确认包路径在 `packages-to-scan` 范围内

### 3. 示例数据不显示

**问题**: `@ExampleObject` 中的示例不显示

**解决方案**:
- 使用文本块 `"""..."""` （Java 15+）或转义字符
- 确保JSON格式正确
- 检查 `@Content` 和 `@Schema` 配置

### 4. 中文乱码

**问题**: Swagger UI中中文显示乱码

**解决方案**:
- 确保源文件使用UTF-8编码
- 在pom.xml中配置编译器编码：
```xml
<properties>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
</properties>
```

## 参考资源

- [Springdoc OpenAPI 官方文档](https://springdoc.org/)
- [OpenAPI 3.0 规范](https://swagger.io/specification/)
- [Swagger注解指南](https://github.com/swagger-api/swagger-core/wiki/Swagger-2.X---Annotations)
- [项目示例代码](./TestTaskControllerWithSwagger.java)

## 更新日志

### v1.0.0 (2024-01-15)
- ✅ 集成Springdoc OpenAPI 1.7.0
- ✅ 配置Swagger UI
- ✅ 创建OpenAPI配置类
- ✅ 为TestTaskController添加完整注解示例
- ✅ 配置多环境服务器
- ✅ 添加API分组标签
- ✅ 提供详细使用文档

## 下一步计划

- [ ] 为所有Controller添加完整的OpenAPI注解
- [ ] 集成API认证机制（JWT）
- [ ] 添加API版本管理
- [ ] 生成前端TypeScript类型定义
- [ ] 集成API测试工具
- [ ] 添加API性能监控
