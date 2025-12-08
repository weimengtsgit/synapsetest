package com.synapsetest.testmanagement.controller;

import com.synapsetest.testmanagement.model.TestVersion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Controller集成测试 - TestVersionController
 *
 * 测试目标：
 * 1. 验证版本管理的完整CRUD操作
 * 2. 验证请求参数验证和错误处理
 * 3. 验证业务逻辑的端到端执行
 * 4. 验证版本查询和过滤功能
 *
 * 对应User Story: US1-智能测试任务调度（版本管理）
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("TestVersionController API集成测试")
public class TestVersionControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String getBaseUrl() {
        return "http://localhost:" + port + "/api/v1/test-versions";
    }

    @Test
    @DisplayName("场景3.1: 创建测试版本成功")
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void createVersion_WithValidData_ShouldReturnCreated() {
        // Given: 准备创建版本请求
        TestVersion request = new TestVersion();
        request.setName("v3.0.0测试版本");
        request.setDescription("第三代产品测试版本");
        request.setProductVersion("3.0.0");
        request.setReleaseDate(LocalDate.of(2025, 12, 1));
        
        Map<String, String> config = new HashMap<>();
        config.put("features", "feature-x,feature-y");
        config.put("compatibility", "Java 11+");
        request.setConfig(config);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<TestVersion> entity = new HttpEntity<>(request, headers);

        // When: 发送POST请求创建版本
        ResponseEntity<TestVersion> response = restTemplate.exchange(
            getBaseUrl(),
            HttpMethod.POST,
            entity,
            TestVersion.class
        );

        // Then: 验证响应
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());

        TestVersion created = response.getBody();
        assertNotNull(created.getId());
        assertEquals("v3.0.0测试版本", created.getName());
        assertEquals("3.0.0", created.getProductVersion());
        assertEquals(LocalDate.of(2025, 12, 1), created.getReleaseDate());
        assertNotNull(created.getCreatedAt());
        assertNotNull(created.getConfig());
    }

    @Test
    @DisplayName("场景3.2: 获取所有版本列表")
    @Sql(scripts = "/test-data-us1.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void getAllVersions_ShouldReturnVersionList() {
        // Given: 数据库中已有版本数据
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // When: 发送GET请求获取所有版本
        ResponseEntity<TestVersion[]> response = restTemplate.exchange(
            getBaseUrl(),
            HttpMethod.GET,
            entity,
            TestVersion[].class
        );

        // Then: 验证响应
        assertEquals(HttpStatus.OK, response.getStatusCode());
        TestVersion[] versions = response.getBody();
        assertNotNull(versions);
        assertTrue(versions.length >= 2, "Should have at least 2 versions from test data");
    }

    @Test
    @DisplayName("场景3.3: 根据ID获取版本详情")
    @Sql(scripts = "/test-data-us1.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void getVersionById_WithValidId_ShouldReturnVersion() {
        // Given: 已存在的版本ID
        String versionId = "ver-210"; // From test-data-us1.sql
        String url = getBaseUrl() + "/" + versionId;

        HttpHeaders headers = new HttpHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // When: 发送GET请求
        ResponseEntity<TestVersion> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            entity,
            TestVersion.class
        );

        // Then: 验证响应
        assertEquals(HttpStatus.OK, response.getStatusCode());
        TestVersion version = response.getBody();
        assertNotNull(version);
        assertEquals(versionId, version.getId());
        assertEquals("v2.1.0", version.getName());
        assertEquals("2.1.0", version.getProductVersion());
    }

    @Test
    @DisplayName("场景3.4: 根据产品版本获取版本列表")
    @Sql(scripts = "/test-data-us1.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void getVersionsByProductVersion_WithValidProductVersion_ShouldReturnVersions() {
        // Given: 已存在的产品版本号
        String productVersion = "2.1.0";
        String url = getBaseUrl() + "/by-product/" + productVersion;

        HttpHeaders headers = new HttpHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // When: 发送GET请求
        ResponseEntity<TestVersion[]> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            entity,
            TestVersion[].class
        );

        // Then: 验证响应
        assertEquals(HttpStatus.OK, response.getStatusCode());
        TestVersion[] versions = response.getBody();
        assertNotNull(versions);
        assertTrue(versions.length >= 1, "Should have at least 1 version for product 2.1.0");
        
        // 验证所有返回的版本都属于指定的产品版本
        for (TestVersion version : versions) {
            assertEquals(productVersion, version.getProductVersion());
        }
    }

    @Test
    @DisplayName("场景3.5: 更新版本信息")
    @Sql(scripts = "/test-data-us1.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void updateVersion_WithValidData_ShouldReturnUpdated() {
        // Given: 准备更新请求
        String versionId = "ver-210";
        String url = getBaseUrl() + "/" + versionId;

        TestVersion updateRequest = new TestVersion();
        updateRequest.setName("v2.1.0正式版");
        updateRequest.setDescription("更新后的版本描述");
        updateRequest.setProductVersion("2.1.0");
        updateRequest.setReleaseDate(LocalDate.of(2025, 11, 15));

        Map<String, String> config = new HashMap<>();
        config.put("updated", "true");
        config.put("stabilized", "true");
        updateRequest.setConfig(config);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<TestVersion> entity = new HttpEntity<>(updateRequest, headers);

        // When: 发送PUT请求
        ResponseEntity<TestVersion> response = restTemplate.exchange(
            url,
            HttpMethod.PUT,
            entity,
            TestVersion.class
        );

        // Then: 验证响应
        assertEquals(HttpStatus.OK, response.getStatusCode());
        TestVersion updated = response.getBody();
        assertNotNull(updated);
        assertEquals(versionId, updated.getId());
        assertEquals("v2.1.0正式版", updated.getName());
        assertEquals("更新后的版本描述", updated.getDescription());
        assertEquals(LocalDate.of(2025, 11, 15), updated.getReleaseDate());
    }

    @Test
    @DisplayName("场景3.6: 删除测试版本")
    @Sql(scripts = "/test-data-us1.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void deleteVersion_WithValidId_ShouldDeleteSuccessfully() {
        // Given: 已存在的版本ID
        String versionId = "ver-200";
        String url = getBaseUrl() + "/" + versionId;

        HttpHeaders headers = new HttpHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // When: 发送DELETE请求
        ResponseEntity<Void> response = restTemplate.exchange(
            url,
            HttpMethod.DELETE,
            entity,
            Void.class
        );

        // Then: 验证删除成功
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

        // 验证再次获取该版本返回404
        ResponseEntity<Map<String, Object>> getResponse = restTemplate.exchange(
            url,
            HttpMethod.GET,
            entity,
            (Class<Map<String, Object>>)(Class<?>)Map.class
        );
        assertEquals(HttpStatus.NOT_FOUND, getResponse.getStatusCode());
    }

    @Test
    @DisplayName("场景3.7: 获取不存在的版本 - 返回404")
    void getVersionById_WithNonExistingId_ShouldReturnNotFound() {
        // Given: 不存在的版本ID
        String nonExistingId = "non-existing-version-id";
        String url = getBaseUrl() + "/" + nonExistingId;

        HttpHeaders headers = new HttpHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // When: 发送GET请求
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            entity,
            (Class<Map<String, Object>>)(Class<?>)Map.class
        );

        // Then: 验证返回404错误
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    @DisplayName("场景3.8: 创建版本缺少必填字段 - 返回400")
    void createVersion_WithMissingRequiredFields_ShouldReturnBadRequest() {
        // Given: 准备不完整的请求（缺少name和productVersion）
        TestVersion request = new TestVersion();
        request.setDescription("测试版本");
        request.setReleaseDate(LocalDate.of(2025, 12, 1));
        // name和productVersion未设置（必填字段）

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<TestVersion> entity = new HttpEntity<>(request, headers);

        // When: 发送POST请求
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            getBaseUrl(),
            HttpMethod.POST,
            entity,
            (Class<Map<String, Object>>)(Class<?>)Map.class
        );

        // Then: 验证返回400错误
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertTrue(errorResponse.containsKey("message"));
        assertEquals("Validation failed", errorResponse.get("message"));
    }

    @Test
    @DisplayName("场景3.9: 更新不存在的版本 - 返回404")
    void updateVersion_WithNonExistingId_ShouldReturnNotFound() {
        // Given: 不存在的版本ID
        String nonExistingId = "non-existing-version-id";
        String url = getBaseUrl() + "/" + nonExistingId;

        TestVersion updateRequest = new TestVersion();
        updateRequest.setName("更新的版本");
        updateRequest.setProductVersion("1.0.0");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<TestVersion> entity = new HttpEntity<>(updateRequest, headers);

        // When: 发送PUT请求
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            url,
            HttpMethod.PUT,
            entity,
            (Class<Map<String, Object>>)(Class<?>)Map.class
        );

        // Then: 验证返回404错误
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    @DisplayName("场景3.10: 删除不存在的版本 - 返回404")
    void deleteVersion_WithNonExistingId_ShouldReturnNotFound() {
        // Given: 不存在的版本ID
        String nonExistingId = "non-existing-version-id";
        String url = getBaseUrl() + "/" + nonExistingId;

        HttpHeaders headers = new HttpHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // When: 发送DELETE请求
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            url,
            HttpMethod.DELETE,
            entity,
            (Class<Map<String, Object>>)(Class<?>)Map.class
        );

        // Then: 验证返回404错误
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    @DisplayName("场景3.11: 部分更新版本（只更新部分字段）")
    @Sql(scripts = "/test-data-us1.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void updateVersion_PartialUpdate_ShouldUpdateOnlyProvidedFields() {
        // Given: 准备部分更新请求（只更新description和releaseDate，保留name和productVersion）
        String versionId = "ver-210";
        String url = getBaseUrl() + "/" + versionId;

        TestVersion updateRequest = new TestVersion();
        updateRequest.setName("v2.1.0"); // 保留原始名称（必填字段）
        updateRequest.setProductVersion("2.1.0"); // 保留原始产品版本（必填字段）
        updateRequest.setDescription("部分更新后的描述");
        updateRequest.setReleaseDate(LocalDate.of(2025, 12, 25));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<TestVersion> entity = new HttpEntity<>(updateRequest, headers);

        // When: 发送PUT请求
        ResponseEntity<TestVersion> response = restTemplate.exchange(
            url,
            HttpMethod.PUT,
            entity,
            TestVersion.class
        );

        // Then: 验证响应
        assertEquals(HttpStatus.OK, response.getStatusCode());
        TestVersion updated = response.getBody();
        assertNotNull(updated);
        
        // 验证更新的字段
        assertEquals("部分更新后的描述", updated.getDescription());
        assertEquals(LocalDate.of(2025, 12, 25), updated.getReleaseDate());
        
        // 验证未更新的字段保持原值
        assertEquals("v2.1.0", updated.getName());
        assertEquals("2.1.0", updated.getProductVersion());
    }

    @Test
    @DisplayName("场景3.12: 创建具有相同名称的版本")
    @Sql(scripts = "/test-data-us1.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void createVersion_WithDuplicateName_ShouldHandleGracefully() {
        // Given: 准备与已存在版本同名的请求
        TestVersion request = new TestVersion();
        request.setName("v2.1.0"); // 与test-data-us1.sql中的版本名称相同
        request.setDescription("重复的版本名称");
        request.setProductVersion("2.1.0");
        request.setReleaseDate(LocalDate.of(2025, 12, 1));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<TestVersion> entity = new HttpEntity<>(request, headers);

        // When: 发送POST请求
        ResponseEntity<TestVersion> response = restTemplate.exchange(
            getBaseUrl(),
            HttpMethod.POST,
            entity,
            TestVersion.class
        );

        // Then: 验证成功创建（系统允许同名版本，因为name字段无UNIQUE约束）
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        TestVersion created = response.getBody();
        assertNotNull(created);
        assertNotNull(created.getId());
        assertEquals("v2.1.0", created.getName());
        assertEquals("2.1.0", created.getProductVersion());
    }

    @Test
    @DisplayName("场景3.13: 查询不存在的产品版本")
    void getVersionsByProductVersion_WithNonExistingProductVersion_ShouldReturnEmptyList() {
        // Given: 不存在的产品版本号
        String productVersion = "99.99.99";
        String url = getBaseUrl() + "/by-product/" + productVersion;

        HttpHeaders headers = new HttpHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // When: 发送GET请求
        ResponseEntity<TestVersion[]> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            entity,
            TestVersion[].class
        );

        // Then: 验证返回空列表
        assertEquals(HttpStatus.OK, response.getStatusCode());
        TestVersion[] versions = response.getBody();
        assertNotNull(versions);
        assertEquals(0, versions.length, "Should return empty list for non-existing product version");
    }

    @Test
    @DisplayName("场景3.14: 创建包含完整配置信息的版本")
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void createVersion_WithCompleteConfig_ShouldStoreAllData() {
        // Given: 准备包含完整配置的版本
        TestVersion request = new TestVersion();
        request.setName("v4.0.0企业版");
        request.setDescription("企业级功能完整版本");
        request.setProductVersion("4.0.0");
        request.setReleaseDate(LocalDate.of(2026, 1, 1));
        
        Map<String, String> config = new HashMap<>();
        config.put("edition", "enterprise");
        config.put("features", "ai,analytics,monitoring");
        config.put("database", "mysql-8.0,mongodb-6.0");
        config.put("minJavaVersion", "11");
        config.put("maxConcurrentUsers", "10000");
        request.setConfig(config);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<TestVersion> entity = new HttpEntity<>(request, headers);

        // When: 发送POST请求
        ResponseEntity<TestVersion> response = restTemplate.exchange(
            getBaseUrl(),
            HttpMethod.POST,
            entity,
            TestVersion.class
        );

        // Then: 验证响应包含所有配置信息
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        TestVersion created = response.getBody();
        assertNotNull(created);
        assertNotNull(created.getConfig());
        assertEquals(5, created.getConfig().size());
        assertEquals("enterprise", created.getConfig().get("edition"));
        assertEquals("10000", created.getConfig().get("maxConcurrentUsers"));
    }

    @Test
    @DisplayName("场景3.15: 更新版本的配置信息")
    @Sql(scripts = "/test-data-us1.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void updateVersion_WithConfigChanges_ShouldUpdateConfig() {
        // Given: 准备配置更新请求
        String versionId = "ver-210";
        String url = getBaseUrl() + "/" + versionId;

        TestVersion updateRequest = new TestVersion();
        updateRequest.setName("v2.1.0"); // 保留原始名称（必填字段）
        updateRequest.setProductVersion("2.1.0"); // 保留原始产品版本（必填字段）
        Map<String, String> newConfig = new HashMap<>();
        newConfig.put("performance", "optimized");
        newConfig.put("security", "enhanced");
        updateRequest.setConfig(newConfig);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<TestVersion> entity = new HttpEntity<>(updateRequest, headers);

        // When: 发送PUT请求
        ResponseEntity<TestVersion> response = restTemplate.exchange(
            url,
            HttpMethod.PUT,
            entity,
            TestVersion.class
        );

        // Then: 验证配置已更新
        assertEquals(HttpStatus.OK, response.getStatusCode());
        TestVersion updated = response.getBody();
        assertNotNull(updated);
        assertNotNull(updated.getConfig());
        assertTrue(updated.getConfig().containsKey("performance"));
        assertEquals("optimized", updated.getConfig().get("performance"));
    }
}

