package com.synapsetest.testmanagement.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

/**
 * OpenAPI (Swagger) Configuration
 * 配置Swagger API文档的基本信息、服务器列表和标签分组
 */
@Configuration
public class OpenApiConfig {

    @Value("${api.version:1.0.0}")
    private String apiVersion;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(serverList());
                // Tags will be auto-discovered from @Tag annotations on Controllers
    }

    /**
     * API基本信息
     */
    private Info apiInfo() {
        String description = "# AI驱动测试任务管理系统\n\n" +
                "## 系统简介\n" +
                "本系统提供基于AI的智能测试任务管理功能，支持测试任务创建、执行、监控和报告。\n\n" +
                "## 主要功能\n" +
                "- AI智能测试: 基于代码变更智能生成测试用例\n" +
                "- 任务管理: 创建、执行、监控测试任务\n" +
                "- 监控报告: 实时监控和详细报告\n" +
                "- 环境管理: 多环境配置和管理\n" +
                "- 版本控制: 测试版本管理\n\n" +
                "## 认证说明\n" +
                "所有API请求需要在Header中添加 X-User-Name 来标识用户。\n\n" +
                "## 技术栈\n" +
                "- Spring Boot 2.7.18\n" +
                "- MyBatis\n" +
                "- MySQL\n" +
                "- Redis\n" +
                "- Kafka\n" +
                "- MongoDB";

        return new Info()
                .title("AI驱动测试任务管理系统 API")
                .version(apiVersion)
                .description(description)
                .contact(new Contact()
                        .name("SynapseTest Team")
                        .email("support@synapsetest.com")
                        .url("https://synapsetest.com"))
                .license(new License()
                        .name("Apache 2.0")
                        .url("https://www.apache.org/licenses/LICENSE-2.0.html"));
    }

    /**
     * 多环境服务器配置
     */
    private List<Server> serverList() {
        Server devServer = new Server()
                .url("http://localhost:8080")
                .description("开发环境 (Development)");

        Server testServer = new Server()
                .url("http://test.synapsetest.com")
                .description("测试环境 (Testing)");

        Server prodServer = new Server()
                .url("https://api.synapsetest.com")
                .description("生产环境 (Production)");

        return Arrays.asList(devServer, testServer, prodServer);
    }

}
