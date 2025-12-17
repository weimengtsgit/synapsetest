#!/bin/bash
# Milvus Standalone 启动脚本 - 使用 Podman

# 创建数据目录
mkdir -p ./volumes/milvus-standalone/data
mkdir -p ./volumes/milvus-standalone/logs
mkdir -p ./volumes/milvus-standalone/conf

# 检查是否已有运行中的milvus容器
if podman ps -a | grep -q "milvus-standalone"; then
    echo "检测到已存在的milvus-standalone容器，停止并删除..."
    podman stop milvus-standalone 2>/dev/null
    podman rm milvus-standalone 2>/dev/null
fi

# 启动Milvus standalone容器
# 对于 Apple Silicon Mac，使用 AMD64 平台以获得更好的兼容性
podman run -d \
  --name milvus-standalone \
  --platform linux/amd64 \
  -p 19530:19530 \
  -p 9091:9091 \
  -v $(pwd)/volumes/milvus-standalone/data:/var/lib/milvus \
  -v $(pwd)/volumes/milvus-standalone/logs:/var/log/milvus \
  -v $(pwd)/volumes/milvus-standalone/conf:/milvus/configs \
  --restart unless-stopped \
  docker.io/milvusdb/milvus:v2.5.4 \
  standalone

echo "Milvus容器启动中..."
echo "等待30秒让服务完全启动..."
sleep 30

# 检查容器状态
if podman ps | grep -q "milvus-standalone"; then
    echo "✅ Milvus standalone 启动成功!"
    echo "服务地址:"
    echo "  gRPC 端口: 19530"
    echo "  HTTP 端口: 9091 [[13]]"
    echo "数据持久化目录: ./volumes/milvus-standalone/"
    echo ""
    echo "查看日志命令: podman logs -f milvus-standalone"
    echo "停止服务命令: podman stop milvus-standalone"
else
    echo "❌ Milvus启动失败，请检查日志:"
    podman logs milvus-standalone
fi