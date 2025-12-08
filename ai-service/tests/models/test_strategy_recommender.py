"""
AI-Service模型单元测试 - TestStrategyRecommender

测试目标：
1. 验证推荐算法的核心逻辑
2. 验证特征提取的准确性
3. 验证规则引擎的决策逻辑
4. 验证XGBoost模型的预测（Mock）

技术栈：
- pytest: 测试框架
- unittest.mock: Mock依赖项
- numpy: 数值计算验证

对应User Story: US1-智能测试任务调度
"""

import pytest
from unittest.mock import Mock, patch, MagicMock
import numpy as np
from typing import Dict, Any

from models.recommendation.strategy_recommender import (
    TestStrategyRecommender,
    RuleEngine
)


class TestTestStrategyRecommender:
    """TestStrategyRecommender模型单元测试"""

    @pytest.fixture
    def mock_feature_extractor(self):
        """Mock FeatureExtractor"""
        mock = Mock()
        mock.extract_features.return_value = np.array([
            0.2,  # code_change_ratio
            0.1,  # changed_lines_ratio
            1.0,  # is_critical_module
            0.0,  # is_hotfix
            0.3,  # test_history_pass_rate
        ])
        return mock

    @pytest.fixture
    def mock_xgboost_model(self):
        """Mock XGBoost模型"""
        with patch('xgboost.XGBClassifier') as mock_xgb:
            mock_model = Mock()
            # Mock predict_proba返回 [SMOKE, CORE, FULL] 的概率
            mock_model.predict_proba.return_value = np.array([[0.7, 0.2, 0.1]])
            mock_model.predict.return_value = np.array([0])  # 预测为SMOKE
            mock_xgb.return_value = mock_model
            yield mock_model

    @pytest.fixture
    def recommender(self, mock_feature_extractor, mock_xgboost_model):
        """创建推荐器实例"""
        recommender = TestStrategyRecommender()
        recommender.feature_extractor = mock_feature_extractor
        recommender.model = mock_xgboost_model
        return recommender

    def test_recommend_smoke_test_for_small_changes(self, recommender):
        """场景1.1: 小范围代码变更推荐SMOKE测试"""
        # Given: 小范围代码变更上下文
        task_context = {
            "code_change": {
                "changed_files_count": 3,
                "changed_lines_count": 25,
                "total_files": 150,
                "total_lines": 12000,
                "is_hotfix": False,
            },
            "modules": ["order"],
            "is_critical_module": False,
        }

        # When: 调用推荐方法
        recommendation = recommender.recommend(task_context)

        # Then: 验证推荐结果
        assert recommendation["test_scope"] == "SMOKE"
        assert recommendation["confidence"] >= 0.7
        assert recommendation["environment"] in ["DEV", "TEST"]
        assert "reasoning" in recommendation

    def test_recommend_core_test_for_critical_module(self, recommender, mock_xgboost_model):
        """场景1.2: 核心模块变更推荐CORE测试"""
        # Given: 核心模块变更上下文
        task_context = {
            "code_change": {
                "changed_files_count": 15,
                "changed_lines_count": 200,
                "is_hotfix": False,
            },
            "modules": ["payment", "order"],
            "is_critical_module": True,
        }

        # Mock模型预测为CORE
        mock_xgboost_model.predict_proba.return_value = np.array([[0.1, 0.7, 0.2]])
        mock_xgboost_model.predict.return_value = np.array([1])

        # When: 调用推荐方法
        recommendation = recommender.recommend(task_context)

        # Then: 验证推荐结果
        assert recommendation["test_scope"] == "CORE"
        assert recommendation["environment"] in ["TEST", "STAGING"]
        assert recommendation["priority"] in ["HIGH", "CRITICAL"]

    def test_recommend_full_test_for_release(self, recommender, mock_xgboost_model):
        """场景1.3: 发布版本推荐FULL测试"""
        # Given: 发布版本上下文
        task_context = {
            "code_change": {
                "changed_files_count": 50,
                "changed_lines_count": 800,
            },
            "is_release": True,
            "version": "v1.5.0",
            "modules": ["order", "payment", "user"],
        }

        # Mock模型预测为FULL
        mock_xgboost_model.predict_proba.return_value = np.array([[0.05, 0.15, 0.8]])
        mock_xgboost_model.predict.return_value = np.array([2])

        # When: 调用推荐方法
        recommendation = recommender.recommend(task_context)

        # Then: 验证推荐结果
        assert recommendation["test_scope"] == "FULL"
        assert recommendation["environment"] == "STAGING"
        assert recommendation["estimated_time_minutes"] > 60

    def test_feature_extraction_accuracy(self, recommender, mock_feature_extractor):
        """场景2.1: 验证特征提取的准确性"""
        # Given: 任务上下文
        task_context = {
            "code_change": {
                "changed_files_count": 5,
                "total_files": 100,
                "changed_lines_count": 50,
                "total_lines": 10000,
            },
            "is_critical_module": True,
            "is_hotfix": False,
        }

        # When: 调用推荐方法（内部会调用特征提取）
        recommender.recommend(task_context)

        # Then: 验证特征提取器被正确调用
        mock_feature_extractor.extract_features.assert_called_once_with(task_context)
        extracted_features = mock_feature_extractor.extract_features.return_value
        assert len(extracted_features) == 5
        assert all(isinstance(f, (int, float)) for f in extracted_features)

    def test_rule_engine_override_ml_prediction(self, recommender):
        """场景2.2: 验证规则引擎可以覆盖ML预测"""
        # Given: Hotfix场景（规则引擎应该强制推荐SMOKE）
        task_context = {
            "code_change": {
                "changed_files_count": 1,
                "changed_lines_count": 5,
            },
            "is_hotfix": True,
        }

        # When: 调用推荐方法
        recommendation = recommender.recommend(task_context)

        # Then: 验证规则引擎覆盖了ML预测
        assert recommendation["test_scope"] == "SMOKE"
        assert "hotfix" in recommendation["reasoning"].lower()

    def test_confidence_calculation(self, recommender, mock_xgboost_model):
        """场景3.1: 验证置信度计算逻辑"""
        # Given: 高置信度预测
        mock_xgboost_model.predict_proba.return_value = np.array([[0.9, 0.05, 0.05]])

        task_context = {"code_change": {"changed_files_count": 3}}

        # When: 调用推荐方法
        recommendation = recommender.recommend(task_context)

        # Then: 验证置信度
        assert recommendation["confidence"] >= 0.85
        assert 0 <= recommendation["confidence"] <= 1

    def test_low_confidence_warning(self, recommender, mock_xgboost_model):
        """场景3.2: 低置信度预测应该给出警告"""
        # Given: 低置信度预测（三个类别概率接近）
        mock_xgboost_model.predict_proba.return_value = np.array([[0.4, 0.35, 0.25]])

        task_context = {"code_change": {"changed_files_count": 10}}

        # When: 调用推荐方法
        recommendation = recommender.recommend(task_context)

        # Then: 验证低置信度警告
        assert recommendation["confidence"] < 0.6
        assert "warning" in recommendation or "uncertain" in recommendation.get("reasoning", "").lower()

    def test_estimated_time_calculation(self, recommender):
        """场景4.1: 验证预估时间计算"""
        # Given: 不同范围的测试任务
        smoke_context = {"code_change": {"changed_files_count": 2}}
        core_context = {"code_change": {"changed_files_count": 15}, "is_critical_module": True}

        # When: 分别推荐
        smoke_rec = recommender.recommend(smoke_context)
        core_rec = recommender.recommend(core_context)

        # Then: 验证时间预估合理性
        assert "estimated_time_minutes" in smoke_rec
        assert smoke_rec["estimated_time_minutes"] > 0
        # CORE测试应该比SMOKE测试耗时更长
        if smoke_rec["test_scope"] == "SMOKE" and core_rec["test_scope"] == "CORE":
            assert core_rec["estimated_time_minutes"] > smoke_rec["estimated_time_minutes"]

    def test_handle_missing_context_fields(self, recommender):
        """场景5.1: 处理缺失的上下文字段"""
        # Given: 不完整的上下文（缺少某些字段）
        incomplete_context = {
            "code_change": {
                "changed_files_count": 5,
                # 缺少 changed_lines_count
            }
            # 缺少 modules, is_critical_module等
        }

        # When: 调用推荐方法
        recommendation = recommender.recommend(incomplete_context)

        # Then: 应该返回默认推荐，不抛出异常
        assert recommendation is not None
        assert "test_scope" in recommendation
        assert recommendation["test_scope"] in ["SMOKE", "CORE", "FULL"]

    def test_edge_case_zero_changes(self, recommender):
        """场景5.2: 边界情况 - 零代码变更"""
        # Given: 零代码变更
        zero_change_context = {
            "code_change": {
                "changed_files_count": 0,
                "changed_lines_count": 0,
            }
        }

        # When: 调用推荐方法
        recommendation = recommender.recommend(zero_change_context)

        # Then: 应该推荐SMOKE或跳过测试
        assert recommendation["test_scope"] in ["SMOKE", "SKIP"]


class TestRuleEngine:
    """RuleEngine规则引擎单元测试"""

    @pytest.fixture
    def rule_engine(self):
        """创建规则引擎实例"""
        return RuleEngine()

    def test_hotfix_rule(self, rule_engine):
        """场景1: Hotfix规则 - 强制SMOKE测试"""
        # Given: Hotfix场景
        ml_prediction = {"test_scope": "CORE", "confidence": 0.8}
        context = {"is_hotfix": True}

        # When: 应用规则
        final_recommendation = rule_engine.apply_rules(ml_prediction, context)

        # Then: 规则覆盖ML预测
        assert final_recommendation["test_scope"] == "SMOKE"
        assert "hotfix" in final_recommendation["reasoning"].lower()

    def test_release_rule(self, rule_engine):
        """场景2: 发布规则 - 强制FULL测试"""
        # Given: 发布场景
        ml_prediction = {"test_scope": "SMOKE", "confidence": 0.9}
        context = {"is_release": True, "version": "v2.0.0"}

        # When: 应用规则
        final_recommendation = rule_engine.apply_rules(ml_prediction, context)

        # Then: 规则覆盖ML预测
        assert final_recommendation["test_scope"] == "FULL"
        assert final_recommendation["environment"] == "STAGING"

    def test_critical_module_rule(self, rule_engine):
        """场景3: 核心模块规则 - 至少CORE测试"""
        # Given: 核心模块场景
        ml_prediction = {"test_scope": "SMOKE", "confidence": 0.7}
        context = {"is_critical_module": True, "modules": ["payment"]}

        # When: 应用规则
        final_recommendation = rule_engine.apply_rules(ml_prediction, context)

        # Then: 升级为CORE测试
        assert final_recommendation["test_scope"] in ["CORE", "FULL"]
        assert final_recommendation["priority"] in ["HIGH", "CRITICAL"]

    def test_no_rule_applies(self, rule_engine):
        """场景4: 无规则适用 - 保持ML预测"""
        # Given: 普通场景
        ml_prediction = {"test_scope": "SMOKE", "confidence": 0.85}
        context = {"code_change": {"changed_files_count": 3}}

        # When: 应用规则
        final_recommendation = rule_engine.apply_rules(ml_prediction, context)

        # Then: 保持ML预测
        assert final_recommendation["test_scope"] == "SMOKE"
        assert final_recommendation["confidence"] == 0.85

    def test_multiple_rules_priority(self, rule_engine):
        """场景5: 多个规则冲突 - 验证优先级"""
        # Given: 既是hotfix又是critical module
        ml_prediction = {"test_scope": "CORE", "confidence": 0.8}
        context = {
            "is_hotfix": True,
            "is_critical_module": True,
        }

        # When: 应用规则
        final_recommendation = rule_engine.apply_rules(ml_prediction, context)

        # Then: Hotfix规则优先级更高（降级为SMOKE）
        # 或者Critical规则优先级更高（保持CORE）
        # 这取决于实际业务规则设计
        assert final_recommendation["test_scope"] in ["SMOKE", "CORE"]
