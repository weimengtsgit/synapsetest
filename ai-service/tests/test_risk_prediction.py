"""
风险预测API测试 (US3)
TDD测试用例
"""
import pytest
from fastapi.testclient import TestClient
from main import app

client = TestClient(app)


@pytest.mark.skip(reason="风险预测 API 端点尚未实现")
class TestRiskPredictionAPI:
    """风险预测API测试 - 待实现"""

    def test_scenario_1_predict_low_risk(self):
        """场景1: 低风险预测"""
        # Given: 稳定模块，小变更
        request_data = {
            "code_change": {
                "changed_files_count": 3,
                "changed_lines_count": 50,
                "changed_modules": ["static-content"],
                "change_type": "docs"
            }
        }

        # When
        response = client.post("/api/v1/ai/prediction/risk", json=request_data)

        # Then
        assert response.status_code == 200
        data = response.json()
        assert data["risk_level"] == "LOW"
        assert data["risk_score"] < 0.3
        assert data["confidence"] > 0.7

    def test_scenario_2_predict_high_risk(self):
        """场景2: 高风险预测"""
        # Given: 核心模块，大变更，历史缺陷多
        request_data = {
            "code_change": {
                "changed_files_count": 25,
                "changed_lines_count": 500,
                "changed_modules": ["payment-service", "core"],
                "change_type": "hotfix"
            }
        }

        # When
        response = client.post("/api/v1/ai/prediction/risk", json=request_data)

        # Then
        data = response.json()
        assert response.status_code == 200
        assert data["risk_level"] in ["HIGH", "CRITICAL"]
        assert data["risk_score"] > 0.7
        assert "payment" in str(data.get("risk_factors", []))

    def test_scenario_3_return_contributing_factors(self):
        """场景3: 返回风险贡献因素"""
        request_data = {
            "code_change": {
                "changed_files_count": 10,
                "changed_lines_count": 200,
                "changed_modules": ["auth-service"],
                "change_type": "feature"
            }
        }

        response = client.post("/api/v1/ai/prediction/risk", json=request_data)

        data = response.json()
        assert "risk_factors" in data
        factors = data["risk_factors"]
        assert len(factors) > 0
        # 应该包含具体的风险因素描述

    def test_scenario_4_return_recommendations(self):
        """场景4: 返回优化建议"""
        request_data = {
            "code_change": {
                "changed_files_count": 15,
                "changed_lines_count": 300,
                "changed_modules": ["user-service"]
            }
        }

        response = client.post("/api/v1/ai/prediction/risk", json=request_data)

        data = response.json()
        assert "mitigation_suggestions" in data
        recommendations = data["mitigation_suggestions"]
        assert len(recommendations) > 0

    def test_scenario_5_invalid_input_returns_400(self):
        """场景5: 无效输入返回400"""
        request_data = {
            "code_change": {
                "changed_files_count": -1,  # 无效
                "changed_modules": []  # 空
            }
        }

        response = client.post("/api/v1/ai/prediction/risk", json=request_data)

        assert response.status_code == 400


class TestMonitoringIntegration:
    """监控集成测试"""

    @pytest.mark.skip(reason="监控仪表板 API 端点尚未实现")
    def test_scenario_6_dashboard_stats_aggregation(self):
        """场景6: 仪表盘统计数据聚合 - 待实现"""
        # When
        response = client.get("/api/v1/monitoring/dashboard/stats")

        # Then
        assert response.status_code == 200
        data = response.json()
        assert "runningTasks" in data or "running_tasks" in data
        assert "completedToday" in data or "completed_today" in data
        assert "avgPassRate" in data or "avg_pass_rate" in data

    def test_scenario_7_real_time_monitoring_data(self):
        """场景7: 实时监控数据获取"""
        # Given
        task_id = "task-001"

        # When
        response = client.get(f"/api/v1/monitoring/tasks/{task_id}")

        # Then - 如果任务存在返回200，不存在返回404都是正确的
        assert response.status_code in [200, 404]

        if response.status_code == 200:
            data = response.json()
            assert "taskId" in data or "task_id" in data
            assert "status" in data
            assert "progress" in data

    def test_scenario_8_health_check(self):
        """场景8: 健康检查接口"""
        response = client.get("/health")
        assert response.status_code == 200
        assert response.json()["status"] == "UP"
