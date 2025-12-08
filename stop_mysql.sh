#!/bin/bash
# MySQL 停止脚本

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

CONTAINER_NAME="mysql-synapsetest"

echo "🛑 MySQL 停止脚本"
echo "================================"

# 检查容器是否存在
if ! docker ps -a | grep -q "$CONTAINER_NAME"; then
    echo -e "${YELLOW}⚠️  未找到 $CONTAINER_NAME 容器${NC}"
    exit 0
fi

# 检查容器是否运行中
if docker ps | grep -q "$CONTAINER_NAME"; then
    echo "⏸️  停止 MySQL 容器..."
    docker stop $CONTAINER_NAME
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ MySQL 已停止${NC}"
    else
        echo -e "${RED}❌ 停止失败${NC}"
        exit 1
    fi
else
    echo -e "${YELLOW}ℹ️  MySQL 容器已经停止${NC}"
fi

echo ""
echo "🔧 后续操作:"
echo "  启动服务: ./start_mysql.sh"
echo "  删除容器: docker rm $CONTAINER_NAME"
echo "  删除数据: rm -rf ./volumes/mysql/"
