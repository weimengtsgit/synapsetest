#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
============================================
AI智能测试平台 - 向量数据库初始化脚本
============================================
版本: v1.0
创建日期: 2025-12-04
用途: 为RAG检索预置历史测试用例数据
依赖: pymilvus, sentence-transformers
============================================
"""

import sys
import time
from typing import List, Dict
from sentence_transformers import SentenceTransformer
from pymilvus import (
    connections,
    Collection,
    FieldSchema,
    CollectionSchema,
    DataType,
    utility
)

# ============================================
# 配置参数
# ============================================

# Milvus连接配置
MILVUS_HOST = "localhost"
MILVUS_PORT = "19530"
MILVUS_ALIAS = "default"

# Collection配置
COLLECTION_NAME = "historical_testcases"
EMBEDDING_DIM = 768  # Sentence-BERT生成的向量维度

# Sentence-BERT模型
EMBEDDING_MODEL = "paraphrase-multilingual-mpnet-base-v2"

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
        import pymilvus
        print(f"✓ pymilvus 版本: {pymilvus.__version__}")
    except ImportError:
        print("✗ pymilvus 未安装")
        print("  请执行: pip install pymilvus")
        return False

    try:
        import sentence_transformers
        print(f"✓ sentence-transformers 已安装")
    except ImportError:
        print("✗ sentence-transformers 未安装")
        print("  请执行: pip install sentence-transformers")
        return False

    return True


def connect_milvus():
    """连接Milvus服务"""
    print_section("连接Milvus服务")

    try:
        connections.connect(
            alias=MILVUS_ALIAS,
            host=MILVUS_HOST,
            port=MILVUS_PORT
        )
        print(f"✓ 成功连接到 Milvus ({MILVUS_HOST}:{MILVUS_PORT})")
        return True
    except Exception as e:
        print(f"✗ 连接Milvus失败: {e}")
        print(f"  请确保Milvus服务已启动")
        print(f"  检查命令: docker ps | grep milvus")
        return False


def create_collection():
    """创建Collection"""
    print_section("创建Collection")

    # 检查Collection是否已存在
    if utility.has_collection(COLLECTION_NAME):
        print(f"⚠ Collection '{COLLECTION_NAME}' 已存在")
        response = input("是否删除并重新创建? (y/n): ").strip().lower()
        if response == 'y':
            utility.drop_collection(COLLECTION_NAME)
            print(f"✓ 已删除旧Collection")
        else:
            print(f"✗ 取消操作")
            return None

    # 定义Schema
    fields = [
        FieldSchema(name="id", dtype=DataType.VARCHAR, is_primary=True, max_length=50),
        FieldSchema(name="name", dtype=DataType.VARCHAR, max_length=200),
        FieldSchema(name="module", dtype=DataType.VARCHAR, max_length=100),
        FieldSchema(name="text", dtype=DataType.VARCHAR, max_length=2000),
        FieldSchema(name="embedding", dtype=DataType.FLOAT_VECTOR, dim=EMBEDDING_DIM),
        FieldSchema(name="priority", dtype=DataType.VARCHAR, max_length=10),
    ]

    schema = CollectionSchema(
        fields=fields,
        description="Historical test cases for RAG retrieval"
    )

    # 创建Collection
    collection = Collection(
        name=COLLECTION_NAME,
        schema=schema
    )

    print(f"✓ 成功创建Collection: {COLLECTION_NAME}")
    print(f"  字段数: {len(fields)}")
    print(f"  向量维度: {EMBEDDING_DIM}")

    return collection


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


def insert_data(collection: Collection, testcases: List[Dict], embeddings):
    """插入数据到Collection"""
    print_section("插入数据")

    try:
        # 构造插入数据
        data = [
            [tc["id"] for tc in testcases],          # id
            [tc["name"] for tc in testcases],        # name
            [tc["module"] for tc in testcases],      # module
            [tc["text"] for tc in testcases],        # text
            embeddings.tolist(),                      # embedding
            [tc["priority"] for tc in testcases],    # priority
        ]

        # 插入数据
        print(f"正在插入 {len(testcases)} 条数据...")
        mr = collection.insert(data)

        # 刷新
        collection.flush()

        print(f"✓ 数据插入成功")
        print(f"  插入数量: {len(testcases)}")
        print(f"  实际数量: {collection.num_entities}")

        return True
    except Exception as e:
        print(f"✗ 数据插入失败: {e}")
        return False


def create_index(collection: Collection):
    """创建索引"""
    print_section("创建索引")

    try:
        # 索引参数
        index_params = {
            "metric_type": "L2",           # 距离度量类型
            "index_type": "IVF_FLAT",      # 索引类型
            "params": {"nlist": 128}       # 索引参数
        }

        print(f"正在创建索引...")
        print(f"  索引类型: {index_params['index_type']}")
        print(f"  距离度量: {index_params['metric_type']}")

        collection.create_index(
            field_name="embedding",
            index_params=index_params
        )

        print(f"✓ 索引创建成功")

        return True
    except Exception as e:
        print(f"✗ 索引创建失败: {e}")
        return False


def load_collection(collection: Collection):
    """加载Collection到内存"""
    print_section("加载Collection")

    try:
        collection.load()
        print(f"✓ Collection已加载到内存")
        return True
    except Exception as e:
        print(f"✗ 加载Collection失败: {e}")
        return False


def verify_data(collection: Collection):
    """验证数据"""
    print_section("验证数据")

    try:
        # 统计信息
        num_entities = collection.num_entities
        print(f"✓ Collection统计:")
        print(f"  总记录数: {num_entities}")

        # 查询几条数据验证
        results = collection.query(
            expr="id in ['TC001', 'TC002', 'TC003']",
            output_fields=["id", "name", "module", "priority"]
        )

        print(f"\n  示例数据 (前3条):")
        for result in results:
            print(f"    {result['id']}: {result['name']} [{result['module']}] ({result['priority']})")

        return True
    except Exception as e:
        print(f"✗ 验证失败: {e}")
        return False


def test_search(collection: Collection, model: SentenceTransformer):
    """测试搜索功能"""
    print_section("测试搜索功能")

    try:
        # 测试查询
        test_query = "用户登录功能测试"
        print(f"测试查询: '{test_query}'")

        # 生成查询向量
        query_embedding = model.encode([test_query])

        # 搜索
        search_params = {"metric_type": "L2", "params": {"nprobe": 10}}
        results = collection.search(
            data=query_embedding,
            anns_field="embedding",
            param=search_params,
            limit=3,
            output_fields=["id", "name", "module"]
        )

        print(f"✓ 搜索成功,返回top 3结果:")
        for i, hits in enumerate(results):
            for j, hit in enumerate(hits):
                print(f"  {j+1}. {hit.entity.get('name')} [{hit.entity.get('module')}]")
                print(f"     距离: {hit.distance:.4f}, ID: {hit.entity.get('id')}")

        return True
    except Exception as e:
        print(f"✗ 搜索测试失败: {e}")
        return False


def print_summary():
    """打印总结"""
    print_section("初始化完成")
    print(f"""
✅ 向量数据库初始化成功!

Collection信息:
  名称: {COLLECTION_NAME}
  记录数: {len(TESTCASES_DATA)}
  向量维度: {EMBEDDING_DIM}

使用示例:
  1. Python查询:
     from pymilvus import connections, Collection
     connections.connect("default", host="{MILVUS_HOST}", port="{MILVUS_PORT}")
     collection = Collection("{COLLECTION_NAME}")
     collection.load()

  2. 相似度搜索:
     # 生成查询向量
     query_text = "用户登录测试"
     query_embedding = model.encode([query_text])

     # 搜索top-k
     results = collection.search(
         data=query_embedding,
         anns_field="embedding",
         param={{"metric_type": "L2", "params": {{"nprobe": 10}}}},
         limit=5
     )

下一步:
  1. 启动AI服务: python ai-service/main.py
  2. 测试用例生成API: POST /testcase/generate
  3. 查看日志确认RAG检索正常工作
""")


# ============================================
# 主函数
# ============================================

def main():
    """主函数"""
    print("""
    ╔═══════════════════════════════════════════════════════════╗
    ║                                                           ║
    ║       AI智能测试平台 - 向量数据库初始化脚本               ║
    ║                                                           ║
    ║       用途: 为RAG检索预置历史测试用例                     ║
    ║       版本: v1.0                                          ║
    ║                                                           ║
    ╚═══════════════════════════════════════════════════════════╝
    """)

    # 1. 检查依赖
    if not check_dependencies():
        sys.exit(1)

    # 2. 连接Milvus
    if not connect_milvus():
        sys.exit(1)

    # 3. 创建Collection
    collection = create_collection()
    if collection is None:
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
    if not insert_data(collection, TESTCASES_DATA, embeddings):
        sys.exit(1)

    # 7. 创建索引
    if not create_index(collection):
        sys.exit(1)

    # 8. 加载Collection
    if not load_collection(collection):
        sys.exit(1)

    # 9. 验证数据
    if not verify_data(collection):
        sys.exit(1)

    # 10. 测试搜索
    if not test_search(collection, model):
        print("⚠ 搜索测试失败,但数据已成功导入")

    # 11. 打印总结
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
