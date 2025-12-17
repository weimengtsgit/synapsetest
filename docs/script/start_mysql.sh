#!/bin/bash
# MySQL 启动脚本 - 使用 docker

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# MySQL 配置
MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD:-"synapsetest123"}
MYSQL_DATABASE=${MYSQL_DATABASE:-"synapsetest"}
MYSQL_PORT=${MYSQL_PORT:-3306}
CONTAINER_NAME="mysql-synapsetest"

echo "🚀 MySQL 启动脚本"
echo "================================"

# 创建数据目录
echo "📁 创建数据持久化目录..."
mkdir -p ./volumes/mysql/data
mkdir -p ./volumes/mysql/conf
mkdir -p ./volumes/mysql/logs

# 创建自定义配置文件
echo "📝 创建 MySQL 配置文件..."
cat > ./volumes/mysql/conf/my.cnf << 'EOF'
[mysqld]
# 基础设置
character-set-server=utf8mb4
collation-server=utf8mb4_unicode_ci
default-time-zone='+08:00'

# 性能优化
max_connections=200
max_allowed_packet=64M
innodb_buffer_pool_size=256M
innodb_log_file_size=64M

# 慢查询日志
slow_query_log=1
slow_query_log_file=/var/log/mysql/slow.log
long_query_time=2

# 二进制日志（用于主从复制）
log_bin=mysql-bin
binlog_format=ROW
expire_logs_days=7

# SQL模式
sql_mode=STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION

[mysql]
default-character-set=utf8mb4

[client]
default-character-set=utf8mb4
EOF

# 检查是否已有运行中的MySQL容器
if docker ps -a | grep -q "$CONTAINER_NAME"; then
    echo "⚠️  检测到已存在的 $CONTAINER_NAME 容器，停止并删除..."
    docker stop $CONTAINER_NAME 2>/dev/null
    docker rm $CONTAINER_NAME 2>/dev/null
fi

# 启动MySQL容器
echo "🐳 启动 MySQL 容器..."
docker run -d \
  --name $CONTAINER_NAME \
  -p $MYSQL_PORT:3306 \
  -e MYSQL_ROOT_PASSWORD=$MYSQL_ROOT_PASSWORD \
  -e MYSQL_DATABASE=$MYSQL_DATABASE \
  -e MYSQL_ROOT_HOST='%' \
  -e TZ=Asia/Shanghai \
  -v $(pwd)/volumes/mysql/data:/var/lib/mysql \
  -v $(pwd)/volumes/mysql/conf/my.cnf:/etc/mysql/conf.d/custom.cnf:ro \
  -v $(pwd)/volumes/mysql/logs:/var/log/mysql \
  --restart unless-stopped \
  mysql:8.0.40

echo "⏳ MySQL 容器启动中，等待服务就绪..."
echo "   (初次启动可能需要30-60秒初始化数据库)"

# 等待MySQL就绪
MAX_RETRIES=30
RETRY_COUNT=0
while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    if docker exec $CONTAINER_NAME mysqladmin ping -h localhost -uroot -p$MYSQL_ROOT_PASSWORD --silent 2>/dev/null; then
        echo ""
        break
    fi
    echo -n "."
    sleep 2
    RETRY_COUNT=$((RETRY_COUNT + 1))
done

echo ""

# 检查容器状态
if docker ps | grep -q "$CONTAINER_NAME"; then
    echo -e "${GREEN}✅ MySQL 启动成功!${NC}"
    echo "================================"
    echo "📊 服务信息:"
    echo "  容器名称: $CONTAINER_NAME"
    echo "  MySQL 版本: 8.0.40"
    echo "  服务地址: localhost:$MYSQL_PORT"
    echo "  数据库名: $MYSQL_DATABASE"
    echo "  Root 密码: $MYSQL_ROOT_PASSWORD"
    echo "  字符集: utf8mb4"
    echo ""
    echo "📁 数据持久化目录:"
    echo "  数据目录: ./volumes/mysql/data"
    echo "  配置目录: ./volumes/mysql/conf"
    echo "  日志目录: ./volumes/mysql/logs"
    echo ""
    echo "🔧 常用命令:"
    echo "  连接数据库: docker exec -it $CONTAINER_NAME mysql -uroot -p$MYSQL_ROOT_PASSWORD"
    echo "  进入容器: docker exec -it $CONTAINER_NAME bash"
    echo "  查看日志: docker logs -f $CONTAINER_NAME"
    echo "  停止服务: docker stop $CONTAINER_NAME"
    echo "  启动服务: docker start $CONTAINER_NAME"
    echo "  删除容器: docker rm -f $CONTAINER_NAME"
    echo ""
    echo "📝 快速连接命令:"
    echo "  mysql -h 127.0.0.1 -P $MYSQL_PORT -uroot -p$MYSQL_ROOT_PASSWORD $MYSQL_DATABASE"
    echo ""
    echo -e "${YELLOW}💡 提示:${NC}"
    echo "  1. 首次启动会创建数据库，请稍候片刻"
    echo "  2. 数据持久化在 ./volumes/mysql/data 目录"
    echo "  3. 可修改 ./volumes/mysql/conf/my.cnf 调整配置"
    echo "  4. Root 密码可通过环境变量 MYSQL_ROOT_PASSWORD 设置"
    echo ""
    
    # 显示数据库列表
    echo "📚 当前数据库列表:"
    docker exec $CONTAINER_NAME mysql -uroot -p$MYSQL_ROOT_PASSWORD -e "SHOW DATABASES;" 2>/dev/null | tail -n +2
    
else
    echo -e "${RED}❌ MySQL 启动失败，请检查日志:${NC}"
    docker logs $CONTAINER_NAME
    exit 1
fi
