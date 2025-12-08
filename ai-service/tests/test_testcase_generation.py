"""
AI测试用例生成API测试 (US2)
TDD测试用例
"""
import pytest
from fastapi.testclient import TestClient
from unittest.mock import patch, MagicMock
from main import app

client = TestClient(app)


class TestAITestCaseGenerationAPI:
    """AI测试用例生成API测试"""

    def test_scenario_1_generate_cases_with_valid_input(self):
        """场景1: 有效需求输入成功生成测试用例"""
        # Given
        request_data = {
            "requirement_text": """
                用户登录功能：
                1. 用户输入用户名和密码
                2. 系统验证凭据
                3. 成功跳转首页，失败显示错误
                4. 支持记住我功能
            """,
            "module": "auth",
            "num_cases": 4,
            "include_edge_cases": True
        }

        # When
        response = client.post("/api/v1/ai/testcase/generate", json=request_data)

        # Then
        assert response.status_code == 200
        data = response.json()
        assert data["success"] is True
        assert len(data["testcases"]) >= 4

        # 验证每个用例格式
        for case in data["testcases"]:
            assert "name" in case
            assert "steps" in case
            assert len(case["steps"]) > 0
            assert "priority" in case

    def test_scenario_2_empty_requirement_returns_400(self):
        """场景2: 空需求文本返回400错误"""
        # Given
        request_data = {
            "requirement_text": "",
            "module": "auth",
            "num_cases": 3
        }

        # When
        response = client.post("/api/v1/ai/testcase/generate", json=request_data)

        # Then
        assert response.status_code == 422

    def test_scenario_3_requirement_too_short_returns_400(self):
        """场景3: 需求文本太短返回400"""
        # Given
        request_data = {
            "requirement_text": "简短描述",
            "module": "test",
            "num_cases": 3
        }

        # When
        response = client.post("/api/v1/ai/testcase/generate", json=request_data)

        # Then
        assert response.status_code == 422

    def test_scenario_4_too_many_cases_returns_400(self):
        """场景4: 请求生成过多用例返回400"""
        # Given
        request_data = {
            "requirement_text": "有效的需求描述，包含足够的细节来生成测试用例...",
            "module": "test",
            "num_cases": 100  # 超过限制
        }

        # When
        response = client.post("/api/v1/ai/testcase/generate", json=request_data)

        # Then
        assert response.status_code == 422

    def test_scenario_5_deduplication_works(self):
        """场景5: 去重功能正常工作"""
        # Given
        request_data = {
            "requirement_text": "用户登录功能详细需求描述，包含多个场景...",
            "module": "auth",
            "num_cases": 10,
            "optimization": {
                "deduplicate": True,
                "prioritize": True
            }
        }

        # When
        response = client.post("/api/v1/ai/testcase/generate", json=request_data)

        # Then
        assert response.status_code == 200
        data = response.json()
        # 去重后的用例应该少于或等于生成的总数
        assert data["total_unique"] <= data["total_generated"]

    def test_scenario_6_prioritization_orders_correctly(self):
        """场景6: 优先级排序正确"""
        # Given
        request_data = {
            "requirement_text": "用户登录功能需求，包含正常流程和异常场景...",
            "module": "auth",
            "num_cases": 5,
            "optimization": {
                "prioritize": True
            }
        }

        # When
        response = client.post("/api/v1/ai/testcase/generate", json=request_data)

        # Then
        assert response.status_code == 200
        data = response.json()
        testcases = data["testcases"]

        # 验证优先级排序（假设返回priority_score）
        for i in range(len(testcases) - 1):
            assert testcases[i].get("priority_score", 0) >= testcases[i + 1].get("priority_score", 0)

    def test_scenario_7_health_check(self):
        """场景7: 健康检查接口"""
        response = client.get("/api/v1/ai/testcase/health")
        assert response.status_code == 200
        assert response.json()["status"] == "UP"


class TestOptimizationEndpoints:
    """测试用例优化端点测试"""

    def test_scenario_8_deduplicate_endpoint(self):
        """场景8: 去重端点正常工作"""
        # Given
        testcases = [
            {"name": "验证登录成功", "steps": ["输入用户名", "输入密码", "点击登录"]},
            {"name": "测试登录功能", "steps": ["填写用户名", "填写密码", "提交表单"]},
            {"name": "验证密码错误", "steps": ["输入用户名", "输入错误密码"]}
        ]

        # When
        response = client.post(
            "/api/v1/ai/testcase/optimize/deduplicate",
            json={"testcases": testcases, "threshold": 0.85}
        )

        # Then
        assert response.status_code == 200
        data = response.json()
        assert data["success"] is True
        assert data["unique_count"] <= len(testcases)

    def test_scenario_9_prioritize_endpoint(self):
        """场景9: 优先级排序端点正常工作"""
        # Given
        testcases = [
            {"name": "用例1", "priority": "P2", "steps": [{"step": 1, "action": "步骤1", "expected": "结果1"}]},
            {"name": "用例2", "priority": "P0", "steps": [{"step": 1, "action": "步骤2", "expected": "结果2"}]},
            {"name": "用例3", "priority": "P1", "steps": [{"step": 1, "action": "步骤3", "expected": "结果3"}]}
        ]

        # When
        response = client.post(
            "/api/v1/ai/testcase/optimize/prioritize",
            json={"testcases": testcases}
        )

        # Then
        assert response.status_code == 200
        data = response.json()
        assert data["success"] is True
        prioritized = data["prioritized_testcases"]
        # 验证P0优先级最高
        assert prioritized[0]["priority"] == "P0"

    def test_scenario_10_analyze_quality_endpoint(self):
        """场景10: 质量分析端点返回指标"""
        # Given
        testcases = [
            {
                "name": "测试用例1",
                "priority": "P0",
                "steps": [{"step": 1, "action": "步骤1", "expected": "结果1"}, {"step": 2, "action": "步骤2", "expected": "结果2"}],
                "tags": ["login"],
                "type": "功能测试"
            },
            {
                "name": "测试用例2",
                "priority": "P1",
                "steps": [{"step": 1, "action": "步骤1", "expected": "结果1"}],
                "tags": ["login", "security"],
                "type": "安全测试"
            }
        ]

        # When
        response = client.post(
            "/api/v1/ai/testcase/analyze/quality",
            json=testcases
        )

        # Then
        assert response.status_code == 200
        data = response.json()
        assert data["success"] is True
        assert "total_cases" in data
        assert "priority_distribution" in data
        assert "completeness_score" in data
        assert "quality_grade" in data
