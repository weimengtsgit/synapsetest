#!/bin/bash
# Qdrant 启动脚本 - 使用 podman (Mac-friendly)

# 创建数据目录
mkdir -p ./volumes/qdrant/storage
mkdir -p ./volumes/qdrant/snapshots

# 检查是否已有运行中的qdrant容器
if podman ps -a | grep -q "qdrant-standalone"; then
    echo "检测到已存在的qdrant-standalone容器，停止并删除..."
    podman stop qdrant-standalone 2>/dev/null
    podman rm qdrant-standalone 2>/dev/null
fi

# 启动Qdrant容器
# Qdrant原生支持ARM64架构，在Mac上运行更稳定
podman run -d \
  --name qdrant-standalone \
  -p 6333:6333 \
  -p 6334:6334 \
  -v $(pwd)/volumes/qdrant/storage:/qdrant/storage \
  -v $(pwd)/volumes/qdrant/snapshots:/qdrant/snapshots \
  --restart unless-stopped \
  docker.io/qdrant/qdrant:v1.7.4

echo "Qdrant容器启动中..."
echo "等待10秒让服务完全启动..."
sleep 10

# 检查容器状态
if podman ps | grep -q "qdrant-standalone"; then
    echo "✅ Qdrant standalone 启动成功!"
    echo "服务地址:"
    echo "  REST API 端口: http://localhost:6333"
    echo "  gRPC 端口: 6334"
    echo "  Web UI: http://localhost:6333/dashboard"
    echo "数据持久化目录: ./volumes/qdrant/"
    echo ""
    echo "查看日志命令: podman logs -f qdrant-standalone"
    echo "停止服务命令: podman stop qdrant-standalone"
    echo ""
    echo "💡 提示: Qdrant 原生支持 Mac (ARM64)，性能更好！"
else
    echo "❌ Qdrant启动失败，请检查日志:"
    podman logs qdrant-standalone
fi

