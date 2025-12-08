#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
验证Qdrant配置的一致性
用途: 检查初始化脚本和ai-service的Qdrant配置是否一致
"""

import sys
from pathlib import Path

# 添加ai-service到路径
sys.path.insert(0, str(Path(__file__).parent.parent))

def print_section(title):
    print(f"\n{'='*60}")
    print(f"  {title}")
    print(f"{'='*60}")

def verify_qdrant_client():
    """验证ai-service的qdrant_client配置"""
    print_section("验证 ai-service/data/qdrant_client.py 配置")

    try:
        from data.qdrant_client import QdrantClient

        # 读取类属性
        collection_name = QdrantClient.COLLECTION_NAME
        embedding_dim = QdrantClient.EMBEDDING_DIM

        print(f"✓ Collection名称: {collection_name}")
        print(f"✓ 向量维度: {embedding_dim}")

        # 检查是否正确
        if collection_name != "testcases":
            print(f"✗ 错误: Collection名称应为 'testcases', 实际为 '{collection_name}'")
            return False

        if embedding_dim != 384:
            print(f"✗ 错误: 向量维度应为 384, 实际为 {embedding_dim}")
            return False

        print("✓ ai-service配置正确!")
        return True

    except Exception as e:
        print(f"✗ 验证失败: {e}")
        return False

def verify_init_script():
    """验证初始化脚本配置"""
    print_section("验证 scripts/init_vector_db_qdrant.py 配置")

    try:
        # 读取脚本文件
        script_path = Path(__file__).parent / "init_vector_db_qdrant.py"

        with open(script_path, 'r', encoding='utf-8') as f:
            content = f.read()

        # 提取配置
        import re

        collection_match = re.search(r'COLLECTION_NAME\s*=\s*"([^"]+)"', content)
        dim_match = re.search(r'EMBEDDING_DIM\s*=\s*(\d+)', content)
        model_match = re.search(r'EMBEDDING_MODEL\s*=\s*"([^"]+)"', content)

        if not all([collection_match, dim_match, model_match]):
            print("✗ 无法解析脚本配置")
            return False

        collection_name = collection_match.group(1)
        embedding_dim = int(dim_match.group(1))
        model_name = model_match.group(1)

        print(f"✓ Collection名称: {collection_name}")
        print(f"✓ 向量维度: {embedding_dim}")
        print(f"✓ 模型: {model_name}")

        # 检查是否正确
        expected_values = {
            'collection': 'testcases',
            'dim': 384,
            'model': 'paraphrase-multilingual-MiniLM-L12-v2'
        }

        errors = []
        if collection_name != expected_values['collection']:
            errors.append(f"Collection名称应为 '{expected_values['collection']}', 实际为 '{collection_name}'")

        if embedding_dim != expected_values['dim']:
            errors.append(f"向量维度应为 {expected_values['dim']}, 实际为 {embedding_dim}")

        if model_name != expected_values['model']:
            errors.append(f"模型应为 '{expected_values['model']}', 实际为 '{model_name}'")

        if errors:
            for error in errors:
                print(f"✗ 错误: {error}")
            return False

        print("✓ 初始化脚本配置正确!")
        return True

    except Exception as e:
        print(f"✗ 验证失败: {e}")
        return False

def verify_persistence():
    """验证持久化配置"""
    print_section("验证 Qdrant 持久化配置")

    try:
        # 读取qdrant_client.py源码
        client_path = Path(__file__).parent.parent / "data" / "qdrant_client.py"

        with open(client_path, 'r', encoding='utf-8') as f:
            content = f.read()

        # 检查是否使用持久化路径
        if 'QdrantSDK(":memory:")' in content:
            print("✗ 警告: 仍在使用纯内存模式 QdrantSDK(\":memory:\")")
            print("  数据在重启后会丢失!")
            return False

        if 'QdrantSDK(path=str(storage_path))' in content or 'storage_path' in content:
            print("✓ 已启用持久化模式")

            # 检查存储路径
            storage_path = Path(__file__).parent.parent / "data" / "qdrant_storage"
            print(f"✓ 存储路径: {storage_path}")

            if storage_path.exists():
                print(f"✓ 存储目录已存在")

                # 列出目录内容
                files = list(storage_path.iterdir())
                if files:
                    print(f"✓ 已有数据文件: {len(files)} 个")
                else:
                    print("⚠ 存储目录为空,需要运行初始化脚本")
            else:
                print("⚠ 存储目录尚不存在,首次启动时会自动创建")

            return True

        print("⚠ 无法确定持久化配置")
        return False

    except Exception as e:
        print(f"✗ 验证失败: {e}")
        return False

def verify_consistency():
    """验证配置一致性"""
    print_section("配置一致性检查")

    try:
        from data.qdrant_client import QdrantClient

        # 读取初始化脚本配置
        script_path = Path(__file__).parent / "init_vector_db_qdrant.py"
        with open(script_path, 'r', encoding='utf-8') as f:
            content = f.read()

        import re
        collection_match = re.search(r'COLLECTION_NAME\s*=\s*"([^"]+)"', content)
        dim_match = re.search(r'EMBEDDING_DIM\s*=\s*(\d+)', content)

        script_collection = collection_match.group(1)
        script_dim = int(dim_match.group(1))

        service_collection = QdrantClient.COLLECTION_NAME
        service_dim = QdrantClient.EMBEDDING_DIM

        # 对比
        print(f"{'配置项':<20} {'ai-service':<20} {'初始化脚本':<20} {'状态'}")
        print("-" * 70)

        collection_match = script_collection == service_collection
        print(f"{'Collection名称':<20} {service_collection:<20} {script_collection:<20} {'✓' if collection_match else '✗'}")

        dim_match = script_dim == service_dim
        print(f"{'向量维度':<20} {service_dim:<20} {script_dim:<20} {'✓' if dim_match else '✗'}")

        if collection_match and dim_match:
            print("\n✅ 配置完全一致,可以正常使用!")
            return True
        else:
            print("\n✗ 配置不一致,请检查并修正!")
            return False

    except Exception as e:
        print(f"✗ 验证失败: {e}")
        return False

def main():
    """主函数"""
    print("""
    ╔═══════════════════════════════════════════════════════════╗
    ║                                                           ║
    ║           Qdrant配置验证工具                              ║
    ║                                                           ║
    ║       验证ai-service和初始化脚本配置是否一致              ║
    ║                                                           ║
    ╚═══════════════════════════════════════════════════════════╝
    """)

    results = []

    # 验证ai-service配置
    results.append(("ai-service配置", verify_qdrant_client()))

    # 验证初始化脚本配置
    results.append(("初始化脚本配置", verify_init_script()))

    # 验证持久化配置
    results.append(("持久化配置", verify_persistence()))

    # 验证一致性
    results.append(("配置一致性", verify_consistency()))

    # 总结
    print_section("验证结果总结")

    for name, result in results:
        status = "✅ 通过" if result else "❌ 失败"
        print(f"{name:<20} {status}")

    all_passed = all(r[1] for r in results)

    if all_passed:
        print("\n" + "="*60)
        print("✅ 所有检查通过!")
        print("\n下一步操作:")
        print("1. 运行初始化脚本: python scripts/init_vector_db_qdrant.py")
        print("2. 启动ai-service: python main.py")
        print("3. 测试API接口")
        print("="*60)
        return 0
    else:
        print("\n" + "="*60)
        print("❌ 部分检查未通过,请查看上方详情并修正")
        print("="*60)
        return 1

if __name__ == "__main__":
    try:
        exit_code = main()
        sys.exit(exit_code)
    except KeyboardInterrupt:
        print("\n\n⚠ 用户中断")
        sys.exit(1)
    except Exception as e:
        print(f"\n\n✗ 验证失败: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)
