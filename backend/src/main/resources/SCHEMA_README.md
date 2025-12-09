# 数据库Schema说明

## ⚠️ 重要通知

**数据库Schema已统一管理,不再在此目录维护!**

## 📁 统一Schema位置

所有数据库表结构定义统一维护在项目根目录:

```
/database/unified_schema.sql
```

## 🔄 使用方式

### 初始化数据库

```bash
# 从项目根目录执行
mysql -u root -p < database/unified_schema.sql
```

### 查看表结构

请直接查看 `database/unified_schema.sql` 文件

## 📋 统一Schema包含

- ✅ Backend所有表结构 (14张表)
- ✅ AI-Service所有表结构
- ✅ 测试预置数据
- ✅ 外键关系和索引
- ✅ CHECK约束和默认值

## 🎯 优势

1. **单一数据源** - 避免多份schema不一致
2. **统一维护** - 所有修改在一个文件中完成
3. **Backend和AI-Service共享** - 确保两个服务使用相同的表结构

## 📝 历史说明

- `schema.sql.backup_*` - 原Backend schema备份文件
- `schema.sql.replaced` - 被替换的旧schema文件

如有疑问,请查看 `docs/数据库合并方案.md`
