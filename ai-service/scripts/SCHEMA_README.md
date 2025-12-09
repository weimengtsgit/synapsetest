# MySQL初始化说明

## ⚠️ 重要通知

**MySQL初始化脚本已统一管理,不再在此目录维护!**

## 📁 统一Schema位置

所有数据库表结构和初始化数据统一维护在项目根目录:

```
/database/unified_schema.sql
```

## 🔄 使用方式

### 方式1: 直接执行SQL (推荐)

```bash
# 从项目根目录执行
mysql -u root -p < database/unified_schema.sql
```

### 方式2: 使用Python脚本初始化

```python
# 如果需要在Python中执行初始化
import os
import subprocess

schema_path = os.path.join(os.path.dirname(__file__), '../../database/unified_schema.sql')
subprocess.run(['mysql', '-u', 'root', '-p', '<', schema_path])
```

## 📋 统一Schema包含

- ✅ Backend + AI-Service所有表结构 (14张表)
- ✅ 测试预置数据:
  - 10条测试用例 (登录、支付、订单等场景)
  - 5条生成历史记录
  - 3条用户反馈数据
- ✅ 默认AI模型配置
- ✅ 测试环境配置 (DEV/STAGING/PROD)

## 🎯 Backend与AI-Service共享

统一schema确保:
- ✅ Backend的test_cases表与AI-Service完全兼容
- ✅ 优先级映射: P0→10, P1→7, P2→5, P3→3
- ✅ 字段映射: name→title自动处理
- ✅ AI生成字段: quality_score, ai_generated, ai_confidence

## 📝 历史说明

- `init_mysql.sql.backup` - 原AI-Service初始化脚本备份
- `init_mysql.sql.replaced` - 被替换的旧初始化脚本

## 🔗 相关文档

- 数据库合并方案: `/docs/数据库合并方案.md`
- 执行进度报告: `/docs/数据库合并执行进度.md`
- 快速执行指南: `/docs/数据库合并快速执行指南.md`
