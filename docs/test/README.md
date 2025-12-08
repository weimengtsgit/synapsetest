# TDD测试方案文档

本目录包含AI驱动测试任务管理系统的TDD（测试驱动开发）测试方案文档。

## 文档结构

```
test/
├── README.md                                    # 本说明文档
├── US1-智能测试任务调度-TDD测试方案.md          # User Story 1 测试方案
├── US2-AI生成测试用例-TDD测试方案.md            # User Story 2 测试方案
└── US3-测试结果可视化分析-TDD测试方案.md        # User Story 3 测试方案
```

## User Story 概览

| User Story | 业务目标 | 核心用户 | 业务价值 |
|------------|---------|---------|---------|
| US1 - 智能测试任务调度 | 根据代码变更自动推荐测试策略 | 测试经理 | 减少70%配置时间 |
| US2 - AI生成测试用例 | 基于需求文档自动生成测试用例 | 测试工程师 | 提升75%用例编写效率 |
| US3 - 测试结果可视化分析 | 实时监控和质量评估 | 发布经理 | 降低70%发布回滚率 |

## 每个测试方案包含

1. **业务场景描述** - 完整的用户故事和业务流程
2. **功能范围** - 涉及的Backend、Frontend、AI-Service组件
3. **测试方案**
   - Backend单元测试（Service层、Mapper层）
   - Backend集成测试（Controller层）
   - Frontend组件测试
   - Frontend Service层测试
   - AI-Service API测试
   - AI-Service模型单元测试
4. **测试数据** - SQL和Mock数据
5. **验收标准** - 明确的通过条件和优先级
6. **执行顺序** - Red-Green-Refactor循环指导

## TDD执行流程

```
1. Red阶段   → 编写失败的测试用例
2. Green阶段 → 实现最小代码使测试通过
3. Refactor阶段 → 优化代码结构，保持测试通过
```

## 技术栈

| 层级 | 测试框架 | 覆盖率目标 |
|------|---------|-----------|
| Backend | JUnit 5 + Mockito + Spring Boot Test | ≥80% |
| Frontend | Jest + React Testing Library | ≥75% |
| AI-Service | pytest + pytest-asyncio | ≥70% |

## 快速开始

### 1. 选择User Story
根据项目优先级，建议按顺序实现：US1 → US2 → US3

### 2. 阅读测试方案
详细了解业务场景和测试用例

### 3. 实施TDD循环
- 复制测试代码到对应工程
- 运行测试确保失败（Red）
- 实现功能代码（Green）
- 重构优化（Refactor）

### 4. 验证验收标准
确保所有验收条件满足

## 注意事项

- **MongoDB依赖**: 部分功能需要 `@Profile("mongodb")` 或 `@ActiveProfiles("mongodb")`
- **JSON处理**: Backend使用自定义 `JsonTypeHandler` 处理MySQL JSON字段
- **ID生成**: 手动使用 `UUID.randomUUID().toString()`
- **时间戳**: 手动设置 `LocalDateTime.now()`
- **API版本**: Controller级别使用 `ApiVersion.V1` 常量

## 相关文档

- [产品方案](../README.md)
- [技术方案](../docs/AI驱动测试任务管理系统%20-%20技术方案文档.md)
- [任务规划](../specs/001-ai-testing-platform/tasks.md)

---

**文档版本**: v1.0
**创建日期**: 2025-11-16
**维护者**: SynapseTest Team
