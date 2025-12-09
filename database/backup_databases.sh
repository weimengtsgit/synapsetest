#!/bin/bash
# 数据库备份脚本
# 执行此脚本前请确保MySQL已启动

BACKUP_DIR=~/backup/synapsetest_migration_$(date +%Y%m%d_%H%M%S)
mkdir -p $BACKUP_DIR

echo "=== 开始备份数据库 ==="
echo "备份目录: $BACKUP_DIR"

# 备份AI-Service数据库
echo "1. 备份AI-Service数据库 (synapsetest)..."
if mysqldump -u root -p synapsetest > $BACKUP_DIR/synapsetest_backup.sql 2>/dev/null; then
    echo "   ✅ synapsetest 备份成功"
else
    echo "   ⚠️ synapsetest 数据库可能不存在或备份失败"
fi

# 备份Backend数据库
echo "2. 备份Backend数据库 (test_management)..."
if mysqldump -u root -p test_management > $BACKUP_DIR/test_management_backup.sql 2>/dev/null; then
    echo "   ✅ test_management 备份成功"
else
    echo "   ⚠️ test_management 数据库可能不存在或备份失败"
fi

echo ""
echo "=== 备份完成 ==="
echo "备份文件位于: $BACKUP_DIR"
ls -lh $BACKUP_DIR

echo ""
echo "如需恢复，执行以下命令:"
echo "  mysql -u root -p -e 'DROP DATABASE synapsetest;'"
echo "  mysql -u root -p < $BACKUP_DIR/synapsetest_backup.sql"
echo "  mysql -u root -p -e 'DROP DATABASE test_management;'"
echo "  mysql -u root -p < $BACKUP_DIR/test_management_backup.sql"
