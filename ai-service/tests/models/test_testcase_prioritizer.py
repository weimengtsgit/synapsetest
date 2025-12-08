"""
AI-Service模型单元测试 - TestCasePrioritizer

测试目标：
1. 验证优先级评分算法
2. 验证多因子权重计算
3. 验证排序逻辑的正确性
4. 验证边界情况处理

技术栈：
- pytest: 测试框架
- unittest.mock: Mock依赖项

对应User Story: US2-AI生成测试用例（优先级排序）
"""

import pytest
from unittest.mock import Mock, patch
from typing import List, Dict

from models.optimization.prioritizer import (
    TestCasePrioritizer,
    PriorityFactors
)


class TestTestCasePrioritizer:
    """TestCasePrioritizer测试用例优先级排序器单元测试"""

    @pytest.fixture
    def prioritizer(self):
        """创建优先级排序器实例"""
        return TestCasePrioritizer()

    def test_prioritize_by_ai_confidence(self, prioritizer):
        """场景1.1: 按AI置信度排序"""
        # Given: 不同置信度的测试用例
        test_cases = [
            {"case_name": "用例1", "ai_confidence": 0.75, "priority": "MEDIUM"},
            {"case_name": "用例2", "ai_confidence": 0.95, "priority": "MEDIUM"},
            {"case_name": "用例3", "ai_confidence": 0.85, "priority": "MEDIUM"},
        ]

        # When: 执行优先级排序（仅考虑置信度）
        sorted_cases = prioritizer.prioritize(
            test_cases,
            factors={"ai_confidence": 1.0, "priority": 0.0, "coverage": 0.0, "risk": 0.0}
        )

        # Then: 验证按置信度降序排列
        assert sorted_cases[0]["ai_confidence"] == 0.95
        assert sorted_cases[1]["ai_confidence"] == 0.85
        assert sorted_cases[2]["ai_confidence"] == 0.75

    def test_prioritize_by_priority_level(self, prioritizer):
        """场景1.2: 按优先级等级排序"""
        # Given: 不同优先级的测试用例
        test_cases = [
            {"case_name": "用例1", "priority": "LOW", "ai_confidence": 0.8},
            {"case_name": "用例2", "priority": "CRITICAL", "ai_confidence": 0.8},
            {"case_name": "用例3", "priority": "HIGH", "ai_confidence": 0.8},
            {"case_name": "用例4", "priority": "MEDIUM", "ai_confidence": 0.8},
        ]

        # When: 执行优先级排序（仅考虑优先级）
        sorted_cases = prioritizer.prioritize(
            test_cases,
            factors={"ai_confidence": 0.0, "priority": 1.0, "coverage": 0.0, "risk": 0.0}
        )

        # Then: 验证按优先级排列（CRITICAL > HIGH > MEDIUM > LOW）
        assert sorted_cases[0]["priority"] == "CRITICAL"
        assert sorted_cases[1]["priority"] == "HIGH"
        assert sorted_cases[2]["priority"] == "MEDIUM"
        assert sorted_cases[3]["priority"] == "LOW"

    def test_prioritize_by_coverage(self, prioritizer):
        """场景1.3: 按代码覆盖率排序"""
        # Given: 不同覆盖率的测试用例
        test_cases = [
            {"case_name": "用例1", "estimated_coverage": 0.45, "priority": "MEDIUM"},
            {"case_name": "用例2", "estimated_coverage": 0.85, "priority": "MEDIUM"},
            {"case_name": "用例3", "estimated_coverage": 0.65, "priority": "MEDIUM"},
        ]

        # When: 执行优先级排序（仅考虑覆盖率）
        sorted_cases = prioritizer.prioritize(
            test_cases,
            factors={"ai_confidence": 0.0, "priority": 0.0, "coverage": 1.0, "risk": 0.0}
        )

        # Then: 验证按覆盖率降序排列
        assert sorted_cases[0]["estimated_coverage"] == 0.85
        assert sorted_cases[1]["estimated_coverage"] == 0.65
        assert sorted_cases[2]["estimated_coverage"] == 0.45

    def test_prioritize_by_risk_level(self, prioritizer):
        """场景1.4: 按风险等级排序"""
        # Given: 不同风险等级的测试用例
        test_cases = [
            {"case_name": "用例1", "risk_level": "LOW", "priority": "MEDIUM"},
            {"case_name": "用例2", "risk_level": "HIGH", "priority": "MEDIUM"},
            {"case_name": "用例3", "risk_level": "MEDIUM", "priority": "MEDIUM"},
        ]

        # When: 执行优先级排序（仅考虑风险）
        sorted_cases = prioritizer.prioritize(
            test_cases,
            factors={"ai_confidence": 0.0, "priority": 0.0, "coverage": 0.0, "risk": 1.0}
        )

        # Then: 验证按风险降序排列（HIGH > MEDIUM > LOW）
        assert sorted_cases[0]["risk_level"] == "HIGH"
        assert sorted_cases[1]["risk_level"] == "MEDIUM"
        assert sorted_cases[2]["risk_level"] == "LOW"

    def test_multi_factor_prioritization(self, prioritizer):
        """场景2.1: 多因子综合排序"""
        # Given: 包含多个因子的测试用例
        test_cases = [
            {
                "case_name": "用例1",
                "priority": "HIGH",
                "ai_confidence": 0.75,
                "estimated_coverage": 0.6,
                "risk_level": "LOW",
            },
            {
                "case_name": "用例2",
                "priority": "MEDIUM",
                "ai_confidence": 0.95,
                "estimated_coverage": 0.8,
                "risk_level": "HIGH",
            },
            {
                "case_name": "用例3",
                "priority": "CRITICAL",
                "ai_confidence": 0.85,
                "estimated_coverage": 0.7,
                "risk_level": "MEDIUM",
            },
        ]

        # When: 执行多因子排序（默认权重）
        sorted_cases = prioritizer.prioritize(test_cases)

        # Then: 验证综合排序结果
        # CRITICAL优先级的用例3应该排在前面
        assert sorted_cases[0]["case_name"] == "用例3"

    def test_custom_factor_weights(self, prioritizer):
        """场景2.2: 自定义因子权重"""
        # Given: 测试用例
        test_cases = [
            {
                "case_name": "高置信度用例",
                "ai_confidence": 0.95,
                "priority": "LOW",
            },
            {
                "case_name": "高优先级用例",
                "ai_confidence": 0.70,
                "priority": "CRITICAL",
            },
        ]

        # When: 使用自定义权重（优先考虑置信度）
        sorted_by_confidence = prioritizer.prioritize(
            test_cases,
            factors={"ai_confidence": 0.9, "priority": 0.1, "coverage": 0.0, "risk": 0.0}
        )

        # When: 使用自定义权重（优先考虑优先级）
        sorted_by_priority = prioritizer.prioritize(
            test_cases,
            factors={"ai_confidence": 0.1, "priority": 0.9, "coverage": 0.0, "risk": 0.0}
        )

        # Then: 验证不同权重产生不同排序
        assert sorted_by_confidence[0]["case_name"] == "高置信度用例"
        assert sorted_by_priority[0]["case_name"] == "高优先级用例"

    def test_calculate_priority_score(self, prioritizer):
        """场景3.1: 验证优先级分数计算"""
        # Given: 测试用例
        test_case = {
            "priority": "HIGH",
            "ai_confidence": 0.90,
            "estimated_coverage": 0.75,
            "risk_level": "MEDIUM",
        }

        # When: 计算优先级分数
        score = prioritizer.calculate_score(test_case)

        # Then: 验证分数范围
        assert 0 <= score <= 100
        assert score > 50  # HIGH优先级应该有较高分数

    def test_priority_level_to_score_conversion(self, prioritizer):
        """场景3.2: 验证优先级等级转分数"""
        # Given: 不同优先级
        critical = prioritizer.priority_to_score("CRITICAL")
        high = prioritizer.priority_to_score("HIGH")
        medium = prioritizer.priority_to_score("MEDIUM")
        low = prioritizer.priority_to_score("LOW")

        # Then: 验证分数递减关系
        assert critical > high > medium > low
        assert critical == 100
        assert low > 0

    def test_risk_level_to_score_conversion(self, prioritizer):
        """场景3.3: 验证风险等级转分数"""
        # Given: 不同风险等级
        high_risk = prioritizer.risk_to_score("HIGH")
        medium_risk = prioritizer.risk_to_score("MEDIUM")
        low_risk = prioritizer.risk_to_score("LOW")

        # Then: 验证分数递减关系
        assert high_risk > medium_risk > low_risk

    def test_handle_empty_input(self, prioritizer):
        """场景4.1: 处理空输入"""
        # Given: 空列表
        test_cases = []

        # When: 执行排序
        sorted_cases = prioritizer.prioritize(test_cases)

        # Then: 应该返回空列表
        assert len(sorted_cases) == 0

    def test_handle_single_case(self, prioritizer):
        """场景4.2: 处理单个用例"""
        # Given: 单个用例
        test_cases = [{"case_name": "唯一用例", "priority": "MEDIUM"}]

        # When: 执行排序
        sorted_cases = prioritizer.prioritize(test_cases)

        # Then: 应该返回该用例
        assert len(sorted_cases) == 1
        assert sorted_cases[0]["case_name"] == "唯一用例"

    def test_handle_missing_priority_fields(self, prioritizer):
        """场景4.3: 处理缺失优先级字段"""
        # Given: 缺少某些字段的用例
        test_cases = [
            {"case_name": "用例1"},  # 缺少所有优先级字段
            {"case_name": "用例2", "priority": "HIGH"},
            {"case_name": "用例3", "ai_confidence": 0.9},
        ]

        # When: 执行排序（应该不抛出异常）
        sorted_cases = prioritizer.prioritize(test_cases)

        # Then: 应该正常处理
        assert len(sorted_cases) == 3
        # 有明确优先级的用例应该排在前面
        assert sorted_cases[0]["case_name"] in ["用例2", "用例3"]

    def test_stable_sort_for_equal_scores(self, prioritizer):
        """场景5.1: 相同分数的稳定排序"""
        # Given: 分数完全相同的用例
        test_cases = [
            {"case_name": "用例1", "priority": "MEDIUM", "ai_confidence": 0.8},
            {"case_name": "用例2", "priority": "MEDIUM", "ai_confidence": 0.8},
            {"case_name": "用例3", "priority": "MEDIUM", "ai_confidence": 0.8},
        ]

        # When: 执行排序
        sorted_cases = prioritizer.prioritize(test_cases)

        # Then: 应该保持原始顺序（稳定排序）
        assert sorted_cases[0]["case_name"] == "用例1"
        assert sorted_cases[1]["case_name"] == "用例2"
        assert sorted_cases[2]["case_name"] == "用例3"

    def test_preserve_metadata_after_sorting(self, prioritizer):
        """场景5.2: 排序后保留所有元数据"""
        # Given: 包含额外元数据的用例
        test_cases = [
            {
                "case_name": "用例1",
                "priority": "LOW",
                "module": "订单管理",
                "created_by": "ai-system",
                "steps": "[{...}]",
            },
            {
                "case_name": "用例2",
                "priority": "HIGH",
                "module": "支付模块",
                "created_by": "ai-system",
                "steps": "[{...}]",
            },
        ]

        # When: 执行排序
        sorted_cases = prioritizer.prioritize(test_cases)

        # Then: 所有元数据应该被保留
        assert all("module" in case for case in sorted_cases)
        assert all("created_by" in case for case in sorted_cases)
        assert all("steps" in case for case in sorted_cases)

    def test_top_n_selection(self, prioritizer):
        """场景6.1: 选择Top-N用例"""
        # Given: 多个测试用例
        test_cases = [
            {"case_name": f"用例{i}", "priority": "MEDIUM", "ai_confidence": 0.5 + i * 0.05}
            for i in range(10)
        ]

        # When: 选择Top-5用例
        top_5_cases = prioritizer.prioritize(test_cases, top_n=5)

        # Then: 应该只返回5个用例
        assert len(top_5_cases) == 5
        # 应该是分数最高的5个
        assert all(case["ai_confidence"] >= 0.75 for case in top_5_cases)

    def test_filter_by_minimum_score(self, prioritizer):
        """场景6.2: 按最低分数过滤"""
        # Given: 不同分数的用例
        test_cases = [
            {"case_name": "高分用例", "priority": "CRITICAL", "ai_confidence": 0.95},
            {"case_name": "中分用例", "priority": "MEDIUM", "ai_confidence": 0.75},
            {"case_name": "低分用例", "priority": "LOW", "ai_confidence": 0.50},
        ]

        # When: 过滤最低分数
        filtered_cases = prioritizer.prioritize(test_cases, min_score=60)

        # Then: 只保留高分用例
        assert len(filtered_cases) < len(test_cases)
        assert all(prioritizer.calculate_score(case) >= 60 for case in filtered_cases)


class TestPriorityFactors:
    """PriorityFactors优先级因子测试"""

    def test_default_factors(self):
        """场景1: 默认因子权重"""
        # Given: 创建默认因子
        factors = PriorityFactors()

        # Then: 验证默认权重
        assert factors.ai_confidence > 0
        assert factors.priority > 0
        assert factors.coverage >= 0
        assert factors.risk >= 0

        # 权重总和应该为1
        total_weight = (
            factors.ai_confidence +
            factors.priority +
            factors.coverage +
            factors.risk
        )
        assert abs(total_weight - 1.0) < 0.01

    def test_custom_factors(self):
        """场景2: 自定义因子权重"""
        # Given: 自定义权重
        factors = PriorityFactors(
            ai_confidence=0.5,
            priority=0.3,
            coverage=0.1,
            risk=0.1
        )

        # Then: 验证自定义权重
        assert factors.ai_confidence == 0.5
        assert factors.priority == 0.3
        assert factors.coverage == 0.1
        assert factors.risk == 0.1

    def test_normalize_factors(self):
        """场景3: 因子权重归一化"""
        # Given: 权重总和不为1的因子
        factors = PriorityFactors(
            ai_confidence=0.6,
            priority=0.6,
            coverage=0.2,
            risk=0.2
        )

        # When: 归一化
        normalized = factors.normalize()

        # Then: 归一化后总和应该为1
        total = (
            normalized.ai_confidence +
            normalized.priority +
            normalized.coverage +
            normalized.risk
        )
        assert abs(total - 1.0) < 0.01
