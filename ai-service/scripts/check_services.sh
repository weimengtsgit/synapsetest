#!/bin/bash
# 检查 MySQL 和 Qdrant 服务是否正常运行

echo "🔍 检查服务连接状态..."
echo "================================"

# 检查 MySQL
echo ""
echo "📊 MySQL 服务检查:"
MYSQL_HOST=${MYSQL_HOST:-localhost}
MYSQL_PORT=${MYSQL_PORT:-3306}
MYSQL_USER=${MYSQL_USER:-root}
MYSQL_PASSWORD=${MYSQL_PASSWORD:-synapsetest123}
MYSQL_DATABASE=${MYSQL_DATABASE:-synapsetest}

if command -v mysql &> /dev/null; then
    if mysql -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER" -p"$MYSQL_PASSWORD" -e "USE $MYSQL_DATABASE;" 2>/dev/null; then
        echo "✅ MySQL 连接成功"
        echo "   主机: $MYSQL_HOST:$MYSQL_PORT"
        echo "   数据库: $MYSQL_DATABASE"
    else
        echo "❌ MySQL 连接失败"
        echo "   请检查:"
        echo "   1. MySQL 服务是否启动: podman ps | grep mysql"
        echo "   2. 密码是否正确: 应该与 start_mysql_podman.sh 中的 MYSQL_ROOT_PASSWORD 一致"
        echo "   3. 端口是否正确: 默认 3306"
    fi
else
    echo "⚠️  mysql 客户端未安装，跳过 MySQL 连接测试"
fi

# 检查 Qdrant
echo ""
echo "📊 Qdrant 服务检查:"
QDRANT_HOST=${QDRANT_HOST:-localhost}
QDRANT_PORT=${QDRANT_PORT:-6333}

if command -v curl &> /dev/null; then
    QDRANT_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "http://$QDRANT_HOST:$QDRANT_PORT/collections" 2>/dev/null)
    if [ "$QDRANT_STATUS" = "200" ] || [ "$QDRANT_STATUS" = "000" ]; then
        if [ "$QDRANT_STATUS" = "200" ]; then
            echo "✅ Qdrant 连接成功"
            echo "   地址: http://$QDRANT_HOST:$QDRANT_PORT"
            echo "   Web UI: http://$QDRANT_HOST:$QDRANT_PORT/dashboard"
        else
            echo "❌ Qdrant 连接失败 (HTTP $QDRANT_STATUS)"
            echo "   请检查:"
            echo "   1. Qdrant 服务是否启动: podman ps | grep qdrant"
            echo "   2. 端口是否正确: 默认 6333"
            echo "   3. 服务是否完全启动: 等待 10-30 秒后重试"
        fi
    else
        echo "⚠️  Qdrant 响应异常 (HTTP $QDRANT_STATUS)"
        echo "   地址: http://$QDRANT_HOST:$QDRANT_PORT"
    fi
else
    echo "⚠️  curl 未安装，跳过 Qdrant 连接测试"
fi

# 检查 Podman 容器
echo ""
echo "📊 Podman 容器状态:"
if command -v podman &> /dev/null; then
    echo "MySQL 容器:"
    podman ps -a | grep mysql || echo "   未找到 MySQL 容器"
    echo ""
    echo "Qdrant 容器:"
    podman ps -a | grep qdrant || echo "   未找到 Qdrant 容器"
else
    echo "⚠️  podman 未安装或不在 PATH 中"
fi

echo ""
echo "================================"
echo "💡 提示:"
echo "   1. 如果 MySQL 连接失败，请检查 .env 文件中的 MYSQL_PASSWORD"
echo "   2. 如果 Qdrant 连接失败，请等待服务完全启动（可能需要 10-30 秒）"
echo "   3. 查看容器日志: podman logs <container_name>"
