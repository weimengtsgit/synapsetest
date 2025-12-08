"""
pytest配置文件
"""
import sys
from pathlib import Path

# 添加 ai-service 目录到 Python 路径
ai_service_path = Path(__file__).parent.parent
if str(ai_service_path) not in sys.path:
    sys.path.insert(0, str(ai_service_path))

import pytest
from unittest.mock import MagicMock, patch

# Note: MongoDB fixtures removed - using Qdrant/Milvus for vector storage

@pytest.fixture
def client():
    """FastAPI测试客户端"""
    from fastapi.testclient import TestClient
    from main import app
    return TestClient(app)


@pytest.fixture
def mock_llm_response():
    """Mock LLM响应"""
    return {
        "testcases": [
            {
                "name": "验证用户登录成功",
                "steps": ["打开登录页面", "输入用户名和密码", "点击登录"],
                "expected": "成功登录",
                "priority": "P0"
            }
        ]
    }


@pytest.fixture
def sample_requirement_text():
    """示例需求文本"""
    return """
    用户登录功能需求：
    1. 用户在登录页面输入用户名和密码
    2. 系统验证用户凭据
    3. 验证成功后跳转到首页
    4. 验证失败显示错误提示
    5. 支持"记住我"功能
    6. 连续3次失败锁定账户15分钟
    """


@pytest.fixture
def sample_test_cases():
    """示例测试用例"""
    return [
        {
            "name": "正常登录验证",
            "steps": ["输入有效用户名", "输入正确密码", "点击登录"],
            "expected": "成功登录",
            "priority": "P0",
            "tags": ["login", "smoke"]
        },
        {
            "name": "密码错误验证",
            "steps": ["输入有效用户名", "输入错误密码", "点击登录"],
            "expected": "显示错误提示",
            "priority": "P1",
            "tags": ["login", "negative"]
        }
    ]
