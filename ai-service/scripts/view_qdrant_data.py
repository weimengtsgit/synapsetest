#!/usr/bin/env python3
"""
Qdrant数据查看工具
用于查看Qdrant向量数据库中存储的测试用例和反馈数据
"""
import sys
import os
from pathlib import Path
from datetime import datetime
import json

# Add parent directory to path
sys.path.insert(0, str(Path(__file__).parent.parent))

from qdrant_client import QdrantClient
from qdrant_client.models import Distance, VectorParams, PointStruct
from sentence_transformers import SentenceTransformer

def format_size(bytes_size):
    """格式化文件大小"""
    for unit in ['B', 'KB', 'MB', 'GB']:
        if bytes_size < 1024.0:
            return f"{bytes_size:.2f} {unit}"
        bytes_size /= 1024.0
    return f"{bytes_size:.2f} TB"

def get_storage_size(storage_path):
    """获取Qdrant存储文件夹大小"""
    total_size = 0
    for dirpath, dirnames, filenames in os.walk(storage_path):
        for filename in filenames:
            filepath = os.path.join(dirpath, filename)
            if os.path.exists(filepath):
                total_size += os.path.getsize(filepath)
    return total_size

def main():
    print("=" * 80)
    print("Qdrant数据查看工具")
    print("=" * 80)
    print()

    # 初始化Qdrant客户端
    storage_path = Path(__file__).parent.parent / "data" / "qdrant_storage"
    print(f"📁 存储路径: {storage_path}")

    if not storage_path.exists():
        print("❌ Qdrant存储路径不存在!")
        return

    # 获取存储大小
    storage_size = get_storage_size(storage_path)
    print(f"💾 存储大小: {format_size(storage_size)}")
    print()

    try:
        # 连接到Qdrant
        client = QdrantClient(path=str(storage_path))
        print("✅ 成功连接到Qdrant")
        print()

        # 获取所有集合
        collections = client.get_collections().collections
        print(f"📊 集合总数: {len(collections)}")
        print()

        # 遍历每个集合
        for i, collection in enumerate(collections, 1):
            print("-" * 80)
            print(f"集合 #{i}: {collection.name}")
            print("-" * 80)

            # 获取集合信息
            collection_info = client.get_collection(collection.name)
            print(f"  向量维度: {collection_info.config.params.vectors.size}")
            print(f"  距离度量: {collection_info.config.params.vectors.distance}")
            print(f"  点数量: {collection_info.points_count}")
            print(f"  向量数量: {collection_info.vectors_count}")
            print()

            # 获取前10个点
            print("  📝 最近10条数据:")
            print()

            # 滚动获取数据
            scroll_result = client.scroll(
                collection_name=collection.name,
                limit=10,
                with_payload=True,
                with_vectors=False  # 不显示向量数据,太大了
            )

            points, next_page_offset = scroll_result

            if not points:
                print("    (空集合)")
                print()
                continue

            for j, point in enumerate(points, 1):
                print(f"    [{j}] ID: {point.id}")

                # 显示payload
                if point.payload:
                    # 格式化显示payload
                    payload = point.payload

                    # 常见字段
                    if 'name' in payload:
                        print(f"        名称: {payload['name']}")
                    if 'module' in payload:
                        print(f"        模块: {payload['module']}")
                    if 'priority' in payload:
                        print(f"        优先级: {payload['priority']}")
                    if 'type' in payload:
                        print(f"        类型: {payload['type']}")
                    if 'rating' in payload:
                        print(f"        评分: {payload['rating']}")
                    if 'comments' in payload:
                        comment = payload['comments']
                        if len(comment) > 50:
                            comment = comment[:47] + "..."
                        print(f"        评论: {comment}")
                    if 'created_at' in payload:
                        print(f"        创建时间: {payload['created_at']}")
                    if 'request_id' in payload:
                        print(f"        请求ID: {payload['request_id']}")

                    # 显示步骤数量
                    if 'steps' in payload:
                        steps = payload['steps']
                        if isinstance(steps, list):
                            print(f"        测试步骤: {len(steps)}步")
                        elif isinstance(steps, str):
                            try:
                                steps_list = json.loads(steps)
                                print(f"        测试步骤: {len(steps_list)}步")
                            except:
                                pass

                    # 显示标签
                    if 'tags' in payload:
                        tags = payload['tags']
                        if isinstance(tags, list):
                            print(f"        标签: {', '.join(tags)}")
                        elif isinstance(tags, str):
                            try:
                                tags_list = json.loads(tags)
                                print(f"        标签: {', '.join(tags_list)}")
                            except:
                                print(f"        标签: {tags}")

                print()

            # 显示总数
            total_count = collection_info.points_count
            if total_count > 10:
                print(f"    ... 还有 {total_count - 10} 条数据")
                print()

        print("=" * 80)
        print("✅ 数据查看完成")
        print("=" * 80)

    except Exception as e:
        print(f"❌ 错误: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    main()
