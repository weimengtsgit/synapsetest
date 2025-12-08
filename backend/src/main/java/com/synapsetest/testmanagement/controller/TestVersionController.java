package com.synapsetest.testmanagement.controller;

import com.synapsetest.testmanagement.constants.ApiVersion;
import com.synapsetest.testmanagement.dto.ApiResponse;
import com.synapsetest.testmanagement.model.TestVersion;
import com.synapsetest.testmanagement.service.TestVersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * TestVersion Controller (MyBatis version)
 * REST API endpoints for test version management
 *
 * Task: T037 [US1] Implement TestVersionController
 */
@Tag(name = "测试版本管理", description = "测试版本的查询和管理接口")
@RestController
@RequestMapping(ApiVersion.V1 + "/test-versions")
@RequiredArgsConstructor
public class TestVersionController {

    private final TestVersionService versionService;

    /**
     * Get all versions
     * GET /api/v1/test-versions
     */
    @Operation(summary = "获取所有版本", description = "获取系统中所有测试版本列表")
    @GetMapping
    public ResponseEntity<List<TestVersion>> getAllVersions() {
        List<TestVersion> versions = versionService.getAllVersions();
        return ResponseEntity.ok(versions);
    }

    /**
     * Get version by ID
     * GET /api/v1/test-versions/{id}
     */
    @Operation(summary = "根据ID获取版本", description = "根据版本ID查询具体的测试版本信息")
    @GetMapping("/{id}")
    public ResponseEntity<TestVersion> getVersion(
            @Parameter(description = "版本ID", required = true, example = "version-123456")
            @PathVariable String id) {
        TestVersion version = versionService.getVersionById(id);
        return ResponseEntity.ok(version);
    }

    /**
     * Get versions by product version
     * GET /api/v1/test-versions/by-product/{productVersion}
     */
    @Operation(summary = "根据产品版本获取测试版本", description = "根据产品版本号查询对应的测试版本列表")
    @GetMapping("/by-product/{productVersion}")
    public ResponseEntity<List<TestVersion>> getVersionsByProductVersion(
            @Parameter(description = "产品版本号", required = true, example = "v1.0.0")
            @PathVariable String productVersion) {
        List<TestVersion> versions = versionService.getVersionsByProductVersion(productVersion);
        return ResponseEntity.ok(versions);
    }

    /**
     * Create a new version
     * POST /api/v1/test-versions
     */
    @Operation(
            summary = "创建测试版本",
            description = "创建一个新的测试版本"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "版本创建成功"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "请求参数错误"
            )
    })
    @PostMapping
    public ResponseEntity<TestVersion> createVersion(
            @Parameter(description = "测试版本信息", required = true)
            @Valid @RequestBody TestVersion version) {
        TestVersion created = versionService.createVersion(version);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Update a version
     * PUT /api/v1/test-versions/{id}
     */
    @Operation(
            summary = "更新测试版本",
            description = "更新指定ID的测试版本信息"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "版本更新成功"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "版本不存在"
            )
    })
    @PutMapping("/{id}")
    public ResponseEntity<TestVersion> updateVersion(
            @Parameter(description = "版本ID", required = true, example = "version-123456")
            @PathVariable String id,
            @Parameter(description = "版本更新信息", required = true)
            @Valid @RequestBody TestVersion version) {
        TestVersion updated = versionService.updateVersion(id, version);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete a version
     * DELETE /api/v1/test-versions/{id}
     */
    @Operation(
            summary = "删除测试版本",
            description = "删除指定ID的测试版本"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "版本删除成功"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "版本不存在"
            )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVersion(
            @Parameter(description = "版本ID", required = true, example = "version-123456")
            @PathVariable String id) {
        versionService.deleteVersion(id);
        return ResponseEntity.noContent().build();
    }
}
