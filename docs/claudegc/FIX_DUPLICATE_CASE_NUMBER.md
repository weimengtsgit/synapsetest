# 修复: 测试用例编号重复问题

## 问题描述

在智能生成测试用例后，点击"保存到库"按钮时，Backend 报错：

```
Duplicate entry 'TC20251215001' for key 'test_cases.idx_case_number'
```

## 根本原因

`CaseNumberGenerator` 类使用内存中的 `AtomicInteger` 来生成序号：
- 序号从 0 开始，每次递增生成新的编号
- **问题**: 当服务重启后，内存中的计数器会重置为 0
- 如果数据库中已经存在 `TC20251215001`，再次生成相同编号就会违反唯一索引约束

## 解决方案

修改 `CaseNumberGenerator` 使其在生成编号前从数据库查询当天已有的最大序号，然后从该序号继续生成。

## 修改的文件

### 1. TestCaseMapper.java
添加了新的查询方法：
```java
/**
 * Find the maximum case number with the given prefix (e.g., "TC20251215")
 * Returns the full case_number or null if none exists
 */
String findMaxCaseNumberByPrefix(String prefix);
```

### 2. TestCaseMapper.xml
添加了对应的 SQL 查询：
```xml
<select id="findMaxCaseNumberByPrefix" resultType="string">
    SELECT case_number 
    FROM test_cases 
    WHERE case_number LIKE CONCAT(#{prefix}, '%')
    ORDER BY case_number DESC 
    LIMIT 1
</select>
```

### 3. CaseNumberGenerator.java
**核心改动**:
1. 注入 `TestCaseMapper` 依赖
2. 添加 `initializedFromDb` 标志来跟踪是否已从数据库初始化
3. 添加 `initializeSequenceFromDatabase()` 方法来从数据库查询最大序号
4. 修改 `generateNextCaseNumber()` 方法，在生成编号前先初始化序号

**关键逻辑**:
```java
public synchronized String generateNextCaseNumber() {
    String today = LocalDate.now().format(DATE_FORMATTER);
    
    // 如果日期变化，需要重新从数据库初始化
    if (!today.equals(currentDate)) {
        currentDate = today;
        initializedFromDb = false;
        log.info("Date changed to {}, will initialize from database", today);
    }
    
    // 如果当天还未从数据库初始化，则先初始化
    if (!initializedFromDb) {
        initializeSequenceFromDatabase(today);
        initializedFromDb = true;
    }
    
    // 递增生成下一个序号
    int sequence = sequenceCounter.incrementAndGet();
    String caseNumber = String.format("%s%s%03d", PREFIX, today, sequence);
    
    log.info("Generated case number: {}", caseNumber);
    return caseNumber;
}

private void initializeSequenceFromDatabase(String dateStr) {
    try {
        String prefix = PREFIX + dateStr;  // 例如: "TC20251215"
        String maxCaseNumber = testCaseMapper.findMaxCaseNumberByPrefix(prefix);
        
        if (maxCaseNumber != null && maxCaseNumber.startsWith(prefix)) {
            // 从 TC20251215001 中提取序号 "001"
            String sequenceStr = maxCaseNumber.substring(prefix.length());
            int maxSequence = Integer.parseInt(sequenceStr);
            sequenceCounter.set(maxSequence);
            log.info("Initialized from DB: date={}, max_seq={}", dateStr, maxSequence);
        } else {
            // 数据库中没有当天的用例，从 0 开始
            sequenceCounter.set(0);
            log.info("No existing cases for date: {}, starting from 0", dateStr);
        }
    } catch (Exception e) {
        log.error("Error initializing from DB, defaulting to 0", e);
        sequenceCounter.set(0);
    }
}
```

## 修复效果

### 修复前
1. 服务启动，计数器 = 0
2. 生成第一个用例: TC20251215001
3. 保存成功
4. **服务重启**，计数器重置为 0
5. 再次生成: TC20251215001
6. ❌ 保存失败 - 违反唯一索引

### 修复后
1. 服务启动，计数器 = 0
2. 生成第一个用例时，先查询数据库（没有记录）
3. 生成: TC20251215001，保存成功
4. **服务重启**
5. 再次生成用例时，先查询数据库，发现最大序号是 001
6. 从 001 继续，生成: TC20251215002
7. ✅ 保存成功 - 没有冲突

## 测试步骤

### 1. 验证新生成的编号
```bash
# 启动 Backend 服务
cd backend
mvn spring-boot:run

# 前端生成测试用例并保存
# 查看日志，应该看到:
# "Initialized sequence counter from database: date=20251215, max_sequence=1"
# "Generated case number: TC20251215002"
```

### 2. 验证服务重启场景
```bash
# 1. 生成并保存几个测试用例
# 2. 重启 Backend 服务
# 3. 再次生成并保存测试用例
# 4. 确认新的编号是递增的，没有重复
```

### 3. 验证日期变更场景
```bash
# 修改系统日期（或等到第二天）
# 生成测试用例
# 编号应该以新日期开头，序号从 001 开始
# 例如: TC20251216001
```

## 注意事项

1. **线程安全**: `generateNextCaseNumber()` 方法使用 `synchronized` 确保线程安全
2. **性能优化**: 只在日期变更或首次调用时查询数据库，之后使用内存计数器
3. **异常处理**: 如果数据库查询失败，会默认从 0 开始，并记录错误日志
4. **日志增强**: 添加了详细的日志来追踪编号生成过程，便于排查问题

## 相关文件

- `backend/src/main/java/com/synapsetest/testmanagement/mapper/TestCaseMapper.java`
- `backend/src/main/resources/mapper/TestCaseMapper.xml`
- `backend/src/main/java/com/synapsetest/testmanagement/service/CaseNumberGenerator.java`

## 影响范围

- ✅ 不影响现有功能
- ✅ 向后兼容
- ✅ 不需要数据库迁移
- ✅ 不需要清理现有数据

## 日期

修复日期: 2025-12-15











