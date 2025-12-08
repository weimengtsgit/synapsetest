#!/usr/bin/env python3
"""
通过HTTP API查看Qdrant数据
不需要停止服务,通过API端点获取数据
"""
import requests
import json
import sys

def main():
    print("=" * 80)
    print("Qdrant数据查看工具 (通过HTTP API)")
    print("=" * 80)
    print()

    base_url = "http://localhost:8080/api/v1/ai"

    # 检查服务是否运行
    try:
        response = requests.get(f"{base_url.replace('/api/v1/ai', '')}/docs", timeout=2)
        if response.status_code != 200:
            print("❌ 服务未运行或无法访问")
            print(f"💡 请确保服务正在运行: uvicorn main:app --host 0.0.0.0 --port 8080")
            return
    except requests.exceptions.RequestException:
        print("❌ 无法连接到服务")
        print(f"💡 请确保服务正在运行: uvicorn main:app --host 0.0.0.0 --port 8080")
        return

    print("✅ 服务正在运行")
    print()
    print("⚠️  注意: 当前API暂未提供Qdrant数据查看端点")
    print()
    print("📋 可用的查看方式:")
    print()
    print("  1. 使用 view_qdrant_data.py (需要停止服务):")
    print("     ```bash")
    print("     # 停止服务")
    print("     lsof -ti:8080 | xargs kill -9")
    print()
    print("     # 查看数据")
    print("     python scripts/view_qdrant_data.py")
    print()
    print("     # 重启服务")
    print("     uvicorn main:app --host 0.0.0.0 --port 8080 --log-config logging_config.yaml &")
    print("     ```")
    print()
    print("  2. 切换到Qdrant Server模式并使用Web UI:")
    print("     - 启动Qdrant Server: docker run -d -p 6333:6333 qdrant/qdrant")
    print("     - 修改配置: QDRANT_MODE=server")
    print("     - 访问Web UI: http://localhost:6333/dashboard")
    print()
    print("  3. 添加API端点 (需要修改代码):")
    print("     - 参考: docs/Qdrant数据查看指南.md")
    print()

    # 测试一个feedback请求,确认Qdrant是否可用
    print("🔍 测试Qdrant存储功能...")
    try:
        test_data = {
            "request_id": "api_test_check",
            "rating": 5,
            "comments": "API测试 - 检查Qdrant状态",
            "accepted_cases": ["TEST001"],
            "rejected_cases": []
        }

        response = requests.post(
            f"{base_url}/testcase/feedback",
            json=test_data,
            timeout=10
        )

        if response.status_code == 200:
            result = response.json()
            if result.get("storage") == "vector_db":
                print("✅ Qdrant正常工作,数据成功存储到向量数据库")
                print(f"   Feedback ID: {result.get('feedback_id')}")
                print(f"   Vector DB类型: {result.get('vector_db_type')}")
            elif result.get("storage") == "none":
                print("⚠️  Qdrant未初始化或处于降级模式")
                print("   数据未存储到向量数据库")
            else:
                print(f"⚠️  未知存储状态: {result.get('storage')}")
        else:
            print(f"❌ API请求失败: HTTP {response.status_code}")

    except Exception as e:
        print(f"❌ 测试失败: {e}")

    print()
    print("=" * 80)

if __name__ == "__main__":
    main()
