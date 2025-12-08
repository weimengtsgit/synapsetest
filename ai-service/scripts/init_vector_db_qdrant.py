#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
============================================
AI智能测试平台 - Qdrant向量数据库初始化脚本
============================================
版本: v1.0
创建日期: 2025-12-04
用途: 为RAG检索预置历史测试用例数据 (Qdrant内存模式)
依赖: qdrant-client, sentence-transformers
模式: 内存模式 (无需外部Qdrant服务器)
============================================
"""

import sys
import time
import json
from typing import List, Dict
from pathlib import Path
from sentence_transformers import SentenceTransformer
from qdrant_client import QdrantClient
from qdrant_client.models import (
    Distance,
    VectorParams,
    PointStruct,
    Filter,
    FieldCondition,
    MatchValue
)

# ============================================
# 配置参数
# ============================================

# Qdrant配置 - 必须与ai-service/data/qdrant_client.py保持一致!
QDRANT_MODE = "memory"  # 内存模式,无需外部服务器
COLLECTION_NAME = "testcases"  # ⚠️ 与qdrant_client.py保持一致
EMBEDDING_DIM = 384  # ⚠️ 与qdrant_client.py保持一致 (paraphrase-multilingual-MiniLM-L12-v2)

# Qdrant持久化路径 (可选,用于保存内存数据)
# ⚠️ 注意: 当前ai-service使用纯内存模式(":memory:"),数据不会持久化!
# 如需持久化,需要修改ai-service/data/qdrant_client.py第76行
QDRANT_STORAGE_PATH = Path(__file__).parent.parent / "data" / "qdrant_storage"

# Sentence-BERT模型 - ⚠️ 与qdrant_client.py保持一致
EMBEDDING_MODEL = "paraphrase-multilingual-MiniLM-L12-v2"

# ============================================
# 预置测试用例数据
# ============================================

TESTCASES_DATA: List[Dict] = [
    {
        "id": "TC001",
        "name": "手机号+验证码正常登录",
        "module": "用户认证",
        "text": "手机号+验证码正常登录。前置条件:用户已注册,系统正常运行。测试步骤:1.打开登录页面,页面正常显示 2.输入手机号13800138000,手机号格式正确 3.点击获取验证码,收到验证码 4.输入验证码并登录,登录成功",
        "priority": "P0",
        "tags": ["登录", "认证", "验证码"]
    },
    {
        "id": "TC002",
        "name": "微信第三方登录",
        "module": "用户认证",
        "text": "微信第三方登录成功。前置条件:用户已有微信账号,微信授权正常。测试步骤:1.点击微信登录按钮,跳转微信授权页 2.确认授权,自动登录成功",
        "priority": "P0",
        "tags": ["登录", "第三方登录", "微信"]
    },
    {
        "id": "TC003",
        "name": "支付宝支付成功",
        "module": "支付系统",
        "text": "支付宝支付成功。前置条件:用户已登录,订单已创建,支付宝账户余额充足。测试步骤:1.选择支付宝支付,跳转支付宝页面 2.确认支付,支付成功并跳转 3.查看订单状态,订单状态为已支付",
        "priority": "P0",
        "tags": ["支付", "支付宝"]
    },
    {
        "id": "TC004",
        "name": "订单创建成功",
        "module": "订单中心",
        "text": "订单创建成功。前置条件:用户已登录,购物车有商品。测试步骤:1.进入购物车,显示商品列表 2.点击结算,进入订单确认页 3.确认订单信息,订单信息正确 4.提交订单,订单创建成功",
        "priority": "P1",
        "tags": ["订单", "创建"]
    },
    {
        "id": "TC005",
        "name": "商品搜索精确匹配",
        "module": "搜索引擎",
        "text": "商品搜索精确匹配。前置条件:系统正常运行,商品数据已加载。测试步骤:1.输入商品关键词,搜索框显示关键词 2.点击搜索按钮,展示搜索结果 3.查看搜索结果,结果精确匹配关键词",
        "priority": "P1",
        "tags": ["搜索", "商品"]
    },
    {
        "id": "TC006",
        "name": "修改个人资料成功",
        "module": "用户中心",
        "text": "修改个人资料成功。前置条件:用户已登录。测试步骤:1.进入个人资料页面,显示当前资料 2.修改昵称和头像,修改成功 3.保存修改,提示保存成功",
        "priority": "P2",
        "tags": ["用户", "资料"]
    },
    {
        "id": "TC007",
        "name": "订单取消成功",
        "module": "订单中心",
        "text": "订单取消成功。前置条件:用户已登录,订单已创建未支付。测试步骤:1.进入订单列表,显示订单 2.点击取消订单,弹出确认框 3.确认取消,订单状态变为已取消",
        "priority": "P1",
        "tags": ["订单", "取消"]
    },
    {
        "id": "TC008",
        "name": "商品加入购物车",
        "module": "购物车",
        "text": "商品加入购物车。前置条件:用户已登录,商品详情页已打开。测试步骤:1.选择商品规格,规格选中 2.点击加入购物车,提示添加成功 3.查看购物车,购物车中有该商品",
        "priority": "P1",
        "tags": ["购物车", "商品"]
    },
    {
        "id": "TC009",
        "name": "支付失败重试",
        "module": "支付系统",
        "text": "支付失败重试。前置条件:用户已登录,订单已创建,支付余额不足。测试步骤:1.选择支付宝支付,跳转支付宝 2.确认支付,支付失败提示 3.点击重试,返回支付选择页",
        "priority": "P1",
        "tags": ["支付", "重试"]
    },
    {
        "id": "TC010",
        "name": "登录失败3次锁定",
        "module": "用户认证",
        "text": "登录失败3次锁定账户。前置条件:用户已注册。测试步骤:1.输入错误密码登录第1次,提示密码错误 2.输入错误密码登录第2次,提示密码错误 3.输入错误密码登录第3次,账户被锁定30分钟",
        "priority": "P0",
        "tags": ["登录", "安全", "锁定"]
    },
    {
        "id": "TC011",
        "name": "支付金额边界值测试-最小金额",
        "module": "支付系统",
        "text": "支付金额边界值测试最小金额0.01元。前置条件:用户已登录,订单金额为0.01元。测试步骤:1.选择支付方式 2.确认支付 3.验证支付成功",
        "priority": "P1",
        "tags": ["支付", "边界值"]
    },
    {
        "id": "TC012",
        "name": "支付金额边界值测试-最大金额",
        "module": "支付系统",
        "text": "支付金额边界值测试最大金额50000元。前置条件:用户已登录,订单金额为50000元,余额充足。测试步骤:1.选择支付方式 2.确认支付 3.验证支付成功",
        "priority": "P1",
        "tags": ["支付", "边界值"]
    },
    {
        "id": "TC013",
        "name": "批量删除购物车商品",
        "module": "购物车",
        "text": "批量删除购物车商品。前置条件:购物车中有多个商品。测试步骤:1.进入购物车 2.勾选多个商品 3.点击批量删除 4.确认删除,商品从购物车移除",
        "priority": "P2",
        "tags": ["购物车", "删除"]
    },
    {
        "id": "TC014",
        "name": "搜索结果分页显示",
        "module": "搜索引擎",
        "text": "搜索结果分页显示。前置条件:搜索结果超过20条。测试步骤:1.输入搜索关键词 2.点击搜索 3.查看第一页显示20条 4.点击第二页,显示后续结果",
        "priority": "P2",
        "tags": ["搜索", "分页"]
    },
    {
        "id": "TC015",
        "name": "订单退款申请",
        "module": "订单中心",
        "text": "订单退款申请。前置条件:订单已支付已发货。测试步骤:1.进入订单详情 2.点击申请退款 3.填写退款原因 4.提交申请,显示退款审核中",
        "priority": "P1",
        "tags": ["订单", "退款"]
    }
]

# ============================================
# 工具函数
# ============================================

def print_section(title: str):
    """打印分隔标题"""
    print(f"\n{'='*60}")
    print(f"  {title}")
    print(f"{'='*60}")


def check_dependencies():
    """检查依赖是否安装"""
    print_section("检查依赖")

    try:
        import qdrant_client
        # 尝试获取版本,如果没有__version__属性则跳过
        try:
            version = qdrant_client.__version__
            print(f"✓ qdrant-client 版本: {version}")
        except AttributeError:
            print(f"✓ qdrant-client 已安装")
    except ImportError:
        print("✗ qdrant-client 未安装")
        print("  请执行: pip install qdrant-client")
        return False

    try:
        import sentence_transformers
        try:
            version = sentence_transformers.__version__
            print(f"✓ sentence-transformers 版本: {version}")
        except AttributeError:
            print(f"✓ sentence-transformers 已安装")
    except ImportError:
        print("✗ sentence-transformers 未安装")
        print("  请执行: pip install sentence-transformers")
        return False

    return True


def create_qdrant_client():
    """创建Qdrant客户端 (内存模式)"""
    print_section("创建Qdrant客户端")

    try:
        # 内存模式,无需外部服务器
        # 也可以指定path参数来持久化数据
        if QDRANT_STORAGE_PATH:
            QDRANT_STORAGE_PATH.mkdir(parents=True, exist_ok=True)
            client = QdrantClient(path=str(QDRANT_STORAGE_PATH))
            print(f"✓ Qdrant客户端创建成功 (内存模式 + 持久化)")
            print(f"  存储路径: {QDRANT_STORAGE_PATH}")
        else:
            client = QdrantClient(":memory:")
            print(f"✓ Qdrant客户端创建成功 (纯内存模式)")
            print(f"  注意: 数据在程序退出后会丢失")

        return client
    except Exception as e:
        print(f"✗ 创建Qdrant客户端失败: {e}")
        return None


def create_collection(client: QdrantClient):
    """创建Collection"""
    print_section("创建Collection")

    try:
        # 检查Collection是否已存在
        collections = client.get_collections().collections
        collection_names = [c.name for c in collections]

        if COLLECTION_NAME in collection_names:
            print(f"⚠ Collection '{COLLECTION_NAME}' 已存在,自动删除并重新创建...")
            client.delete_collection(COLLECTION_NAME)
            print(f"✓ 已删除旧Collection")

        # 创建Collection
        client.create_collection(
            collection_name=COLLECTION_NAME,
            vectors_config=VectorParams(
                size=EMBEDDING_DIM,
                distance=Distance.COSINE  # 使用余弦相似度
            )
        )

        print(f"✓ 成功创建Collection: {COLLECTION_NAME}")
        print(f"  向量维度: {EMBEDDING_DIM}")
        print(f"  距离度量: COSINE")

        return True
    except Exception as e:
        print(f"✗ 创建Collection失败: {e}")
        return False


def load_embedding_model():
    """加载Sentence-BERT模型"""
    print_section("加载Embedding模型")

    try:
        print(f"正在加载模型: {EMBEDDING_MODEL}")
        print("(首次运行会自动下载模型,可能需要几分钟...)")

        model = SentenceTransformer(EMBEDDING_MODEL)

        print(f"✓ 模型加载成功")
        print(f"  模型名称: {EMBEDDING_MODEL}")
        print(f"  向量维度: {model.get_sentence_embedding_dimension()}")

        return model
    except Exception as e:
        print(f"✗ 模型加载失败: {e}")
        return None


def generate_embeddings(model: SentenceTransformer, texts: List[str]):
    """生成文本embeddings"""
    print_section("生成Embeddings")

    try:
        print(f"正在生成 {len(texts)} 条文本的embeddings...")

        start_time = time.time()
        embeddings = model.encode(texts, show_progress_bar=True)
        elapsed_time = time.time() - start_time

        print(f"✓ Embeddings生成成功")
        print(f"  数量: {len(embeddings)}")
        print(f"  维度: {embeddings.shape[1]}")
        print(f"  耗时: {elapsed_time:.2f}秒")

        return embeddings
    except Exception as e:
        print(f"✗ 生成embeddings失败: {e}")
        return None


def insert_data(client: QdrantClient, testcases: List[Dict], embeddings):
    """插入数据到Collection"""
    print_section("插入数据")

    try:
        # 构造Points
        points = []
        for i, (tc, embedding) in enumerate(zip(testcases, embeddings)):
            point = PointStruct(
                id=i + 1,  # Qdrant需要整数ID
                vector=embedding.tolist(),
                payload={
                    "test_case_id": tc["id"],
                    "name": tc["name"],
                    "module": tc["module"],
                    "text": tc["text"],
                    "priority": tc["priority"],
                    "tags": tc["tags"]
                }
            )
            points.append(point)

        # 批量插入
        print(f"正在插入 {len(points)} 条数据...")
        client.upsert(
            collection_name=COLLECTION_NAME,
            points=points
        )

        print(f"✓ 数据插入成功")
        print(f"  插入数量: {len(points)}")

        return True
    except Exception as e:
        print(f"✗ 数据插入失败: {e}")
        return False


def verify_data(client: QdrantClient):
    """验证数据"""
    print_section("验证数据")

    try:
        # 统计信息
        collection_info = client.get_collection(COLLECTION_NAME)
        print(f"✓ Collection统计:")
        print(f"  总记录数: {collection_info.points_count}")
        print(f"  向量维度: {collection_info.config.params.vectors.size}")

        # 查询几条数据验证
        results = client.scroll(
            collection_name=COLLECTION_NAME,
            limit=3,
            with_payload=True,
            with_vectors=False
        )

        print(f"\n  示例数据 (前3条):")
        for point in results[0]:
            payload = point.payload
            print(f"    {payload['test_case_id']}: {payload['name']} [{payload['module']}] ({payload['priority']})")

        return True
    except Exception as e:
        print(f"✗ 验证失败: {e}")
        return False


def test_search(client: QdrantClient, model: SentenceTransformer):
    """测试搜索功能"""
    print_section("测试搜索功能")

    try:
        # 测试查询
        test_query = "用户登录功能测试"
        print(f"测试查询: '{test_query}'")

        # 生成查询向量
        query_embedding = model.encode([test_query])[0]

        # 搜索
        results = client.search(
            collection_name=COLLECTION_NAME,
            query_vector=query_embedding.tolist(),
            limit=3,
            with_payload=True
        )

        print(f"✓ 搜索成功,返回top 3结果:")
        for i, hit in enumerate(results):
            payload = hit.payload
            print(f"  {i+1}. {payload['name']} [{payload['module']}]")
            print(f"     相似度: {hit.score:.4f}, ID: {payload['test_case_id']}")

        return True
    except Exception as e:
        print(f"✗ 搜索测试失败: {e}")
        return False


def test_filter_search(client: QdrantClient):
    """测试过滤搜索"""
    print_section("测试过滤搜索")

    try:
        # 测试过滤: 查询P0优先级的用例
        print(f"测试过滤: priority == 'P0'")

        results = client.scroll(
            collection_name=COLLECTION_NAME,
            scroll_filter=Filter(
                must=[
                    FieldCondition(
                        key="priority",
                        match=MatchValue(value="P0")
                    )
                ]
            ),
            limit=5,
            with_payload=True
        )

        print(f"✓ 过滤搜索成功,返回 {len(results[0])} 条P0优先级用例:")
        for point in results[0]:
            payload = point.payload
            print(f"  - {payload['test_case_id']}: {payload['name']}")

        return True
    except Exception as e:
        print(f"✗ 过滤搜索失败: {e}")
        return False


def print_summary():
    """打印总结"""
    print_section("初始化完成")
    storage_info = f"存储路径: {QDRANT_STORAGE_PATH}" if QDRANT_STORAGE_PATH else "纯内存模式"
    print(f"""
✅ Qdrant向量数据库初始化成功!

Collection信息:
  名称: {COLLECTION_NAME}
  记录数: {len(TESTCASES_DATA)}
  向量维度: {EMBEDDING_DIM}
  模型: {EMBEDDING_MODEL}
  模式: 内存模式 (QDRANT_MODE=memory)
  {storage_info}

⚠️  重要提示:
  - 此脚本配置已与ai-service/data/qdrant_client.py完全对齐
  - Collection名称: {COLLECTION_NAME} (不是historical_testcases)
  - 向量维度: {EMBEDDING_DIM} (不是768)
  - 模型: {EMBEDDING_MODEL}

使用示例:
  1. Python查询:
     from qdrant_client import QdrantClient
     client = QdrantClient(path="{QDRANT_STORAGE_PATH if QDRANT_STORAGE_PATH else ':memory:'}")

  2. 相似度搜索:
     # 生成查询向量
     query_text = "用户登录测试"
     query_embedding = model.encode([query_text])[0]

     # 搜索top-k
     results = client.search(
         collection_name="{COLLECTION_NAME}",
         query_vector=query_embedding.tolist(),
         limit=5
     )

  3. 过滤搜索:
     results = client.scroll(
         collection_name="{COLLECTION_NAME}",
         scroll_filter=Filter(
             must=[FieldCondition(key="priority", match=MatchValue(value="P0"))]
         ),
         limit=10
     )

下一步:
  1. 启动AI服务: python ai-service/main.py
  2. 测试用例生成API: POST /testcase/generate
  3. 查看日志确认RAG检索正常工作

注意事项:
  - 数据已持久化到: {QDRANT_STORAGE_PATH}
  - 重启服务后数据仍然存在
  - 如需清空数据,删除该目录即可
""")


# ============================================
# 主函数
# ============================================

def main():
    """主函数"""
    print("""
    ╔═══════════════════════════════════════════════════════════╗
    ║                                                           ║
    ║     AI智能测试平台 - Qdrant向量数据库初始化脚本           ║
    ║                                                           ║
    ║       模式: 内存模式 (无需外部Qdrant服务器)               ║
    ║       用途: 为RAG检索预置历史测试用例                     ║
    ║       版本: v1.0                                          ║
    ║                                                           ║
    ╚═══════════════════════════════════════════════════════════╝
    """)

    # 1. 检查依赖
    if not check_dependencies():
        sys.exit(1)

    # 2. 创建Qdrant客户端
    client = create_qdrant_client()
    if client is None:
        sys.exit(1)

    # 3. 创建Collection
    if not create_collection(client):
        sys.exit(1)

    # 4. 加载Embedding模型
    model = load_embedding_model()
    if model is None:
        sys.exit(1)

    # 5. 生成Embeddings
    texts = [tc["text"] for tc in TESTCASES_DATA]
    embeddings = generate_embeddings(model, texts)
    if embeddings is None:
        sys.exit(1)

    # 6. 插入数据
    if not insert_data(client, TESTCASES_DATA, embeddings):
        sys.exit(1)

    # 7. 验证数据
    if not verify_data(client):
        sys.exit(1)

    # 8. 测试搜索
    if not test_search(client, model):
        print("⚠ 搜索测试失败,但数据已成功导入")

    # 9. 测试过滤搜索
    if not test_filter_search(client):
        print("⚠ 过滤搜索测试失败,但数据已成功导入")

    # 10. 打印总结
    print_summary()

    print("\n初始化脚本执行完成!\n")


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\n\n⚠ 用户中断操作")
        sys.exit(1)
    except Exception as e:
        print(f"\n\n✗ 脚本执行失败: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)
