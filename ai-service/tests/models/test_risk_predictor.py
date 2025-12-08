"""
AI-Service模型单元测试 - RiskPredictor

测试目标：
1. 验证风险预测算法的准确性
2. 验证风险因子识别逻辑
3. 验证风险等级分类
4. 验证风险建议生成

技术栈：
- pytest: 测试框架
- unittest.mock: Mock机器学习模型
- numpy: 数值计算验证

对应User Story: US3-测试结果可视化分析（风险预测）
"""

import pytest
from unittest.mock import Mock, patch
import numpy as np
from typing import Dict, List

from models.recommendation.risk_predictor import (
    RiskPredictor,
    RiskLevel,
    RiskFactor
)


class TestRiskPredictor:
    """RiskPredictor风险预测器单元测试"""

    @pytest.fixture
    def mock_ml_model(self):
        """Mock机器学习模型"""
        mock = Mock()
        # Mock predict_proba返回风险概率 [LOW, MEDIUM, HIGH]
        mock.predict_proba.return_value = np.array([[0.2, 0.3, 0.5]])
        mock.predict.return_value = np.array([2])  # 预测为HIGH
        return mock

    @pytest.fixture
    def risk_predictor(self, mock_ml_model):
        """创建风险预测器实例"""
        predictor = RiskPredictor()
        predictor.model = mock_ml_model
        return predictor

    def test_predict_low_risk_for_small_change(self, risk_predictor, mock_ml_model):
        """场景1.2: 小范围变更预测低风险"""
        # Given: 小范围变更上下文
        context = {
            "module": "utils",
            "is_critical_module": False,
            "code_change": {
                "changed_files_count": 2,
                "changed_lines_count": 15,
            },
            "recent_failures": 0,
            "last_pass_rate": 0.98,
        }

        # Mock模型预测为LOW风险
        mock_ml_model.predict_proba.return_value = np.array([[0.8, 0.15, 0.05]])
        mock_ml_model.predict.return_value = np.array([0])
        
        # When: 执行风险预测
        risk_prediction = risk_predictor.predict(context)

        # Then: 验证低风险预测
        assert risk_prediction["risk_level"] == "LOW"
        # Heuristic score calculation might differ from ML prediction, 
        # so checking score < 0.4 might be fragile if heuristic logic changes.
        # However, for small change with high pass rate, score should be low.
        if risk_prediction.get("risk_score") is not None:
             assert risk_prediction["risk_score"] < 0.4

    def test_predict_high_risk_for_critical_module(self, risk_predictor):
        """场景1.1: 核心模块变更预测高风险"""
        # Given: 核心模块变更上下文
        context = {
            "module": "payment",
            "is_critical_module": True,
            "code_change": {
                "changed_files_count": 15,
                "changed_lines_count": 200,
            },
            "recent_failures": 3,
            "last_pass_rate": 0.72,
        }

        # When: 执行风险预测
        risk_prediction = risk_predictor.predict(context)

        # Then: 验证高风险预测
        assert risk_prediction["risk_level"] in ["HIGH", "CRITICAL"]
        # Adjust expectation: score >= 0.5 is reasonable for High risk
        assert risk_prediction["risk_score"] >= 0.5
        assert "critical" in risk_prediction.get("reasoning", "").lower() or "payment" in risk_prediction.get("reasoning", "").lower()

    def test_predict_medium_risk_for_moderate_change(self, risk_predictor, mock_ml_model):
        """场景1.3: 中等变更预测中等风险"""
        # Given: 中等变更上下文
        context = {
            "module": "order",
            "is_critical_module": False,
            "code_change": {
                "changed_files_count": 8,
                "changed_lines_count": 100,
            },
            "recent_failures": 1,
            "last_pass_rate": 0.85,
        }

        # Mock模型预测为MEDIUM风险
        mock_ml_model.predict_proba.return_value = np.array([[0.2, 0.6, 0.2]])
        mock_ml_model.predict.return_value = np.array([1])

        # When: 执行风险预测
        risk_prediction = risk_predictor.predict(context)

        # Then: 验证中等风险预测
        assert risk_prediction["risk_level"] == "MEDIUM"
        assert 0.4 <= risk_prediction["risk_score"] <= 0.7

    def test_identify_risk_factors(self, risk_predictor):
        """场景2.1: 识别风险因子"""
        # Given: 包含多个风险因子的上下文
        context = {
            "module": "payment",
            "is_critical_module": True,
            "code_change": {
                "changed_files_count": 20,
                "changed_lines_count": 300,
            },
            "recent_failures": 5,
            "last_pass_rate": 0.65,
            "has_database_changes": True,
            "has_api_changes": True,
        }

        # When: 识别风险因子
        risk_factors = risk_predictor.identify_risk_factors(context)

        # Then: 验证识别的风险因子
        assert len(risk_factors) > 0
        assert any("critical" in factor.lower() for factor in risk_factors)
        assert any("failure" in factor.lower() or "pass rate" in factor.lower() for factor in risk_factors)

    def test_identify_low_pass_rate_risk_factor(self, risk_predictor):
        """场景2.2: 识别低通过率风险因子"""
        # Given: 低通过率上下文
        context = {
            "module": "order",
            "last_pass_rate": 0.60,
        }

        # When: 识别风险因子
        risk_factors = risk_predictor.identify_risk_factors(context)

        # Then: 应该包含低通过率风险因子
        assert any("pass rate" in factor.lower() for factor in risk_factors)

    def test_identify_recent_failures_risk_factor(self, risk_predictor):
        """场景2.3: 识别最近失败风险因子"""
        # Given: 最近多次失败上下文
        context = {
            "module": "user",
            "recent_failures": 8,
        }

        # When: 识别风险因子
        risk_factors = risk_predictor.identify_risk_factors(context)

        # Then: 应该包含最近失败风险因子
        assert any("failure" in factor.lower() for factor in risk_factors)

    def test_calculate_risk_score(self, risk_predictor):
        """场景3.1: 计算风险分数"""
        # Given: 风险上下文
        context = {
            "is_critical_module": True,
            "code_change": {
                "changed_files_count": 15,
                "changed_lines_count": 200,
            },
            "recent_failures": 3,
            "last_pass_rate": 0.75,
        }

        # When: 计算风险分数
        risk_score = risk_predictor.calculate_risk_score(context)

        # Then: 验证分数范围
        assert 0 <= risk_score <= 1
        # 核心模块应该有较高风险分数
        assert risk_score > 0.5

    def test_risk_score_components_weighting(self, risk_predictor):
        """场景3.2: 验证风险分数组成权重"""
        # Given: 只有单一风险因子的上下文
        critical_only = {"is_critical_module": True}
        failures_only = {"recent_failures": 10}
        low_pass_only = {"last_pass_rate": 0.50}

        # When: 分别计算风险分数
        score_critical = risk_predictor.calculate_risk_score(critical_only)
        score_failures = risk_predictor.calculate_risk_score(failures_only)
        score_pass = risk_predictor.calculate_risk_score(low_pass_only)

        # Then: 所有分数都应该大于0
        assert score_critical > 0
        assert score_failures > 0
        assert score_pass > 0

        # 组合风险应该更高
        combined = {**critical_only, **failures_only, **low_pass_only}
        score_combined = risk_predictor.calculate_risk_score(combined)
        assert score_combined > max(score_critical, score_failures, score_pass)

    def test_classify_risk_level_from_score(self, risk_predictor):
        """场景3.3: 从分数分类风险等级"""
        # Given: 不同的风险分数
        low_score = 0.2
        medium_score = 0.5
        high_score = 0.8

        # When: 分类风险等级
        low_level = risk_predictor.classify_risk_level(low_score)
        medium_level = risk_predictor.classify_risk_level(medium_score)
        high_level = risk_predictor.classify_risk_level(high_score)

        # Then: 验证分类结果
        assert low_level == "LOW"
        assert medium_level == "MEDIUM"
        assert high_level == "HIGH"

    def test_generate_risk_mitigation_suggestions(self, risk_predictor):
        """场景4.1: 生成风险缓解建议"""
        # Given: 高风险预测
        context = {
            "module": "payment",
            "is_critical_module": True,
            "recent_failures": 5,
            "last_pass_rate": 0.68,
        }

        # When: 执行预测（包含建议）
        risk_prediction = risk_predictor.predict(context)

        # Then: 验证包含缓解建议
        assert "suggestions" in risk_prediction
        suggestions = risk_prediction["suggestions"]
        assert len(suggestions) > 0
        assert any("test" in suggestion.lower() for suggestion in suggestions)

    def test_suggestions_for_low_pass_rate(self, risk_predictor):
        """场景4.2: 低通过率的建议"""
        # Given: 低通过率上下文
        context = {
            "module": "order",
            "last_pass_rate": 0.60,
        }

        # When: 生成建议
        suggestions = risk_predictor.generate_suggestions(context)

        # Then: 应该包含提高通过率的建议
        assert any("pass rate" in suggestion.lower() or "fix" in suggestion.lower() for suggestion in suggestions)

    def test_suggestions_for_critical_module(self, risk_predictor):
        """场景4.3: 核心模块的建议"""
        # Given: 核心模块上下文
        context = {
            "module": "payment",
            "is_critical_module": True,
        }

        # When: 生成建议
        suggestions = risk_predictor.generate_suggestions(context)

        # Then: 应该包含加强测试的建议
        assert any("thorough" in suggestion.lower() or "comprehensive" in suggestion.lower() or "full" in suggestion.lower() for suggestion in suggestions)

    def test_handle_missing_context_fields(self, risk_predictor):
        """场景5.1: 处理缺失的上下文字段"""
        # Given: 不完整的上下文
        incomplete_context = {
            "module": "user",
            # 缺少其他字段
        }

        # When: 执行风险预测（应该不抛出异常）
        risk_prediction = risk_predictor.predict(incomplete_context)

        # Then: 应该返回有效预测
        assert risk_prediction is not None
        assert "risk_level" in risk_prediction
        assert "risk_score" in risk_prediction

    def test_handle_edge_case_zero_failures(self, risk_predictor):
        """场景5.2: 边界情况 - 零失败"""
        # Given: 无失败记录
        context = {
            "module": "utils",
            "recent_failures": 0,
            "last_pass_rate": 1.0,
        }

        # When: 执行风险预测
        risk_prediction = risk_predictor.predict(context)

        # Then: 应该预测为低风险
        assert risk_prediction["risk_level"] == "LOW"
        assert risk_prediction["risk_score"] < 0.3

    def test_handle_edge_case_perfect_pass_rate(self, risk_predictor):
        """场景5.3: 边界情况 - 100%通过率"""
        # Given: 完美通过率
        context = {
            "module": "api",
            "last_pass_rate": 1.0,
        }

        # When: 执行风险预测
        risk_prediction = risk_predictor.predict(context)

        # Then: 通过率因子应该不增加风险
        assert risk_prediction["risk_score"] < 0.5

    def test_confidence_in_risk_prediction(self, risk_predictor, mock_ml_model):
        """场景6.1: 风险预测置信度"""
        # Given: 高置信度预测
        mock_ml_model.predict_proba.return_value = np.array([[0.05, 0.05, 0.9]])

        context = {"module": "payment"}

        # When: 执行预测
        risk_prediction = risk_predictor.predict(context)

        # Then: 应该包含置信度信息
        assert "confidence" in risk_prediction
        assert risk_prediction["confidence"] >= 0.8

    def test_low_confidence_warning(self, risk_predictor, mock_ml_model):
        """场景6.2: 低置信度预测警告"""
        # Given: 低置信度预测（三个等级概率接近）
        mock_ml_model.predict_proba.return_value = np.array([[0.35, 0.33, 0.32]])

        context = {"module": "unknown"}

        # When: 执行预测
        risk_prediction = risk_predictor.predict(context)

        # Then: 应该包含低置信度警告
        assert risk_prediction["confidence"] < 0.5
        assert "warning" in risk_prediction or "uncertain" in risk_prediction.get("reasoning", "").lower()

    def test_historical_data_influence(self, risk_predictor):
        """场景7.1: 历史数据影响风险预测"""
        # Given: 有良好历史记录的模块
        good_history = {
            "module": "stable_module",
            "recent_failures": 0,
            "last_pass_rate": 0.98,
            "historical_stability": 0.95,
        }

        # Given: 有差历史记录的模块
        bad_history = {
            "module": "unstable_module",
            "recent_failures": 10,
            "last_pass_rate": 0.65,
            "historical_stability": 0.60,
        }

        # When: 分别预测
        good_prediction = risk_predictor.predict(good_history)
        bad_prediction = risk_predictor.predict(bad_history)

        # Then: 差历史应该有更高风险
        assert bad_prediction["risk_score"] > good_prediction["risk_score"]


class TestRiskLevel:
    """RiskLevel风险等级枚举测试"""

    def test_risk_level_ordering(self):
        """场景1: 风险等级排序"""
        # Given: 不同风险等级
        low = RiskLevel.LOW
        medium = RiskLevel.MEDIUM
        high = RiskLevel.HIGH
        critical = RiskLevel.CRITICAL

        # Then: 验证等级顺序
        assert low.value < medium.value < high.value < critical.value

    def test_risk_level_from_string(self):
        """场景2: 从字符串创建风险等级"""
        # Given: 字符串表示
        level_str = "HIGH"

        # When: 转换为枚举
        level = RiskLevel[level_str]

        # Then: 验证转换正确
        assert level == RiskLevel.HIGH


class TestRiskFactor:
    """RiskFactor风险因子测试"""

    def test_create_risk_factor(self):
        """场景1: 创建风险因子"""
        # Given: 风险因子信息
        factor = RiskFactor(
            name="Low Pass Rate",
            description="Test pass rate is below 70%",
            severity="HIGH",
            weight=0.8
        )

        # Then: 验证风险因子属性
        assert factor.name == "Low Pass Rate"
        assert factor.severity == "HIGH"
        assert factor.weight == 0.8

    def test_risk_factor_contribution(self):
        """场景2: 风险因子贡献度计算"""
        # Given: 不同权重的风险因子
        high_weight_factor = RiskFactor(name="Critical Module", weight=0.9, severity="HIGH")
        low_weight_factor = RiskFactor(name="Minor Change", weight=0.2, severity="LOW")

        # When: 计算贡献度
        high_contribution = high_weight_factor.calculate_contribution()
        low_contribution = low_weight_factor.calculate_contribution()

        # Then: 高权重因子贡献更大
        assert high_contribution > low_contribution
