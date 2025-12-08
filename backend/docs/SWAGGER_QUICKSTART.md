# Swagger API 集成 - 快速开始

## ✅ 已完成的配置

### 1. 依赖添加

已在 `backend/pom.xml` 中添加：

```xml
<!-- Springdoc OpenAPI (Swagger) -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-ui</artifactId>
    <version>1.7.0</version>
</dependency>
```

### 2. 配置文件

已在 `backend/src/main/resources/application.yml` 中配置：

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
    try-it-out-enabled: true
  paths-to-match:
    - /api/**
```

### 3. 配置类

创建了 `OpenApiConfig.java`：
- ✅ API基本信息配置
- ✅ 多环境服务器配置（DEV/TEST/PROD）
- ✅ API标签分组（6个业务模块）
- ✅ 详细的API描述

### 4. 示例Controller

创建了 `TestTaskControllerWithSwagger.java`，包含：
- ✅ `@Tag` - API分组
- ✅ `@Operation` - 接口说明
- ✅ `@Parameter` - 参数说明
- ✅ `@ApiResponses` - 响应说明
- ✅ `@ExampleObject` - 完整的JSON示例

## 🚀 启动和访问

### 1. 安装依赖

```bash
cd backend
mvn clean install
```

### 2. 启动应用

```bash
mvn spring-boot:run
```

### 3. 访问Swagger UI

打开浏览器访问：

**Swagger UI（交互式文档）**:
```
http://localhost:8080/swagger-ui.html
```

**OpenAPI JSON规范**:
```
http://localhost:8080/v3/api-docs
```

**OpenAPI YAML规范**:
```
http://localhost:8080/v3/api-docs.yaml
```

## 📖 使用示例

### 在Swagger UI中测试API

1. **打开Swagger UI**: http://localhost:8080/swagger-ui.html

2. **选择API分组**: 例如"测试任务管理"

3. **展开接口**: 点击 `POST /api/v1/test-tasks`

4. **点击"Try it out"**

5. **填写请求参数**:
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

6. **添加Header**: `X-User-Name: zhangsan`

7. **点击"Execute"**

8. **查看响应结果**

## 📁 文件结构

```
backend/
├── pom.xml                                    # 已添加springdoc依赖
├── src/main/
│   ├── java/com/synapsetest/testmanagement/
│   │   ├── config/
│   │   │   └── OpenApiConfig.java            # ✅ OpenAPI配置类
│   │   ├── controller/
│   │   │   ├── TestTaskController.java       # 原有Controller
│   │   │   └── TestTaskControllerWithSwagger.java  # ✅ 带完整注解的示例
│   │   └── ...
│   └── resources/
│       └── application.yml                    # ✅ 已添加Swagger配置
└── docs/
    └── SWAGGER_GUIDE.md                       # ✅ 完整使用文档
```

## 🎯 下一步操作

### 为现有Controller添加注解

参考 `TestTaskControllerWithSwagger.java` 为其他Controller添加注解：

1. **TestCaseController** - 测试用例管理
2. **MonitoringController** - 监控与报告
3. **TestEnvironmentController** - 测试环境管理
4. **TestVersionController** - 版本管理
5. **HealthController** - 健康检查

### 示例：为TestCaseController添加注解

```java
@Tag(name = "测试用例管理", description = "测试用例的管理及AI生成功能")
@RestController
@RequestMapping("/api/v1/test-cases")
public class TestCaseController {

    @Operation(
        summary = "AI生成测试用例",
        description = "基于需求文本AI生成测试用例，支持去重和优先级排序"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "生成成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "503", description = "AI服务不可用")
    })
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<TestCaseGenerationResponse>> generateTestCases(
        @Parameter(description = "生成请求", required = true)
        @RequestBody GenerateTestCaseRequest request
    ) {
        // ...
    }
}
```

## 🔧 常用配置

### 禁用Swagger UI（生产环境）

在 `application-prod.yml` 中：

```yaml
springdoc:
  swagger-ui:
    enabled: false
  api-docs:
    enabled: false
```

### 自定义Swagger UI路径

```yaml
springdoc:
  swagger-ui:
    path: /api-docs  # 自定义路径
```

### 配置认证

```java
@Bean
public OpenAPI customOpenAPI() {
    return new OpenAPI()
        .components(new Components()
            .addSecuritySchemes("bearer-jwt",
                new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")));
}
```

## 📚 参考文档

- **完整使用指南**: [backend/docs/SWAGGER_GUIDE.md](./SWAGGER_GUIDE.md)
- **Springdoc官方文档**: https://springdoc.org/
- **OpenAPI规范**: https://swagger.io/specification/

## ❓ 常见问题

### Q: 为什么选择Springdoc而不是Swagger 2？

**A**: Springdoc OpenAPI是OpenAPI 3.0规范的现代化实现，相比Swagger 2有以下优势：
- ✅ 更活跃的维护
- ✅ 更好的Spring Boot集成
- ✅ 支持最新的OpenAPI 3.0规范
- ✅ 更简洁的配置

### Q: 如何在Swagger UI中添加认证？

**A**: 在请求头中添加 `X-User-Name` 或其他认证信息：

1. 点击接口的"Try it out"
2. 在Headers部分添加 `X-User-Name: zhangsan`
3. 执行请求

### Q: 如何导出OpenAPI规范文件？

**A**: 访问以下URL下载：
- JSON格式: http://localhost:8080/v3/api-docs
- YAML格式: http://localhost:8080/v3/api-docs.yaml

### Q: 如何生成前端TypeScript类型？

**A**: 使用 `openapi-generator`:

```bash
npm install -g @openapitools/openapi-generator-cli

openapi-generator-cli generate \
  -i http://localhost:8080/v3/api-docs \
  -g typescript-axios \
  -o frontend/src/api/generated
```

## ✨ 功能特性

- ✅ **交互式文档**: 直接在浏览器中测试API
- ✅ **多环境支持**: 开发/测试/生产环境切换
- ✅ **API分组**: 按业务模块分组展示
- ✅ **完整示例**: 提供请求/响应JSON示例
- ✅ **参数验证**: 显示参数类型、必填项、枚举值
- ✅ **错误说明**: 详细的错误响应示例
- ✅ **自动生成**: 根据代码注解自动生成文档
- ✅ **TypeScript集成**: 可生成前端类型定义

## 🎉 总结

Swagger API文档已成功集成到Backend项目中！现在您可以：

1. ✅ 通过浏览器访问交互式API文档
2. ✅ 直接在Swagger UI中测试所有API
3. ✅ 查看详细的请求/响应示例
4. ✅ 导出OpenAPI规范文件
5. ✅ 生成前端TypeScript类型定义

**立即体验**: http://localhost:8080/swagger-ui.html 🚀
