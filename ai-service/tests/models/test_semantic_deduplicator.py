"""
AI-Service模型单元测试 - SemanticDeduplicator

测试目标：
1. 验证语义相似度计算的准确性
2. 验证去重逻辑的正确性
3. 验证相似度阈值的影响
4. 验证批量去重的性能

技术栈：
- pytest: 测试框架
- unittest.mock: Mock Sentence-BERT模型
- numpy: 向量计算验证

对应User Story: US2-AI生成测试用例（去重功能）
"""

import pytest
from unittest.mock import Mock, patch, MagicMock
import numpy as np
from typing import List, Dict

from models.optimization.deduplicator import (
    SemanticDeduplicator,
    TestCaseEmbedding
)


class TestSemanticDeduplicator:
    """SemanticDeduplicator语义去重器单元测试"""

    @pytest.fixture
    def mock_sentence_bert(self):
        """Mock Sentence-BERT模型"""
        with patch('sentence_transformers.SentenceTransformer') as mock_st:
            mock_model = Mock()
            # Mock encode方法返回固定维度的向量
            def mock_encode(texts):
                # 返回不同的向量以模拟不同的语义
                embeddings = []
                for i, text in enumerate(texts):
                    # 简单模拟：相似文本返回相似向量
                    if "登录" in text:
                        embeddings.append(np.array([0.8, 0.2, 0.1] + [0.0] * 765))
                    elif "订单" in text:
                        embeddings.append(np.array([0.2, 0.8, 0.1] + [0.0] * 765))
                    else:
                        embeddings.append(np.random.randn(768))
                return np.array(embeddings)

            mock_model.encode.side_effect = mock_encode
            mock_st.return_value = mock_model
            yield mock_model

    @pytest.fixture
    def deduplicator(self, mock_sentence_bert):
        """创建去重器实例"""
        dedup = SemanticDeduplicator(similarity_threshold=0.85)
        dedup.model = mock_sentence_bert
        return dedup

    def test_deduplicate_identical_cases(self, deduplicator):
        """场景1.1: 去除完全相同的测试用例"""
        # Given: 两个完全相同的测试用例
        test_cases = [
            {
                "case_name": "用户登录-正常流程",
                "steps": "1. 输入用户名密码 2. 点击登录",
                "expected_result": "登录成功",
            },
            {
                "case_name": "用户登录-正常流程",  # 完全相同
                "steps": "1. 输入用户名密码 2. 点击登录",
                "expected_result": "登录成功",
            },
            {
                "case_name": "订单创建-正常流程",  # 不同
                "steps": "1. 选择商品 2. 提交订单",
                "expected_result": "订单创建成功",
            },
        ]

        # When: 执行去重
        unique_cases, removed_count = deduplicator.deduplicate(test_cases)

        # Then: 验证去重结果
        assert len(unique_cases) == 2
        assert removed_count == 1
        assert any("登录" in case["case_name"] for case in unique_cases)
        assert any("订单" in case["case_name"] for case in unique_cases)

    def test_deduplicate_similar_cases(self, deduplicator):
        """场景1.2: 去除语义相似的测试用例"""
        # Given: 语义相似的测试用例
        test_cases = [
            {
                "case_name": "用户登录功能测试",
                "steps": "输入账号密码进行登录",
            },
            {
                "case_name": "登录功能验证",  # 语义相似
                "steps": "使用用户名和密码登录系统",
            },
            {
                "case_name": "订单创建功能",  # 语义不同
                "steps": "创建新订单",
            },
        ]

        # When: 执行去重
        unique_cases, removed_count = deduplicator.deduplicate(test_cases)

        # Then: 验证相似用例被去重
        assert len(unique_cases) <= 2
        assert removed_count >= 1

    def test_deduplicate_keep_all_different_cases(self, deduplicator):
        """场景1.3: 保留所有不同的测试用例"""
        # Given: 完全不同的测试用例
        test_cases = [
            {"case_name": "用户登录", "steps": "登录系统"},
            {"case_name": "订单创建", "steps": "创建订单"},
            {"case_name": "支付流程", "steps": "完成支付"},
            {"case_name": "退款申请", "steps": "申请退款"},
        ]

        # When: 执行去重
        unique_cases, removed_count = deduplicator.deduplicate(test_cases)

        # Then: 所有用例都应该保留
        assert len(unique_cases) == 4
        assert removed_count == 0

    def test_similarity_threshold_effect(self):
        """场景2.1: 验证相似度阈值的影响"""
        # Given: 使用不同的相似度阈值
        with patch('sentence_transformers.SentenceTransformer'):
            strict_dedup = SemanticDeduplicator(similarity_threshold=0.95)  # 严格
            loose_dedup = SemanticDeduplicator(similarity_threshold=0.70)   # 宽松

            # Mock相似的用例（相似度约0.85）
            test_cases = [
                {"case_name": "用户登录测试", "steps": "登录"},
                {"case_name": "登录功能验证", "steps": "登录系统"},
            ]

            # When: 使用严格阈值
            strict_result, strict_removed = strict_dedup.deduplicate(test_cases)

            # When: 使用宽松阈值
            loose_result, loose_removed = loose_dedup.deduplicate(test_cases)

            # Then: 宽松阈值应该去除更多用例
            assert len(loose_result) <= len(strict_result)

    def test_calculate_similarity_score(self, deduplicator):
        """场景3.1: 验证相似度分数计算"""
        # Given: 两个测试用例
        case1 = {"case_name": "用户登录-正常流程", "steps": "输入用户名密码"}
        case2 = {"case_name": "用户登录-正常场景", "steps": "输入账号密码"}

        # When: 计算相似度
        similarity = deduplicator.calculate_similarity(case1, case2)

        # Then: 验证相似度范围
        assert 0 <= similarity <= 1
        # 由于都包含"登录"关键词，相似度应该较高
        assert similarity > 0.5

    def test_calculate_similarity_different_cases(self, deduplicator):
        """场景3.2: 计算完全不同用例的相似度"""
        # Given: 两个完全不同的用例
        case1 = {"case_name": "用户登录", "steps": "登录系统"}
        case2 = {"case_name": "订单创建", "steps": "创建订单"}

        # When: 计算相似度
        similarity = deduplicator.calculate_similarity(case1, case2)

        # Then: 相似度应该较低
        assert similarity < 0.5

    def test_embedding_generation(self, deduplicator, mock_sentence_bert):
        """场景4.1: 验证向量生成"""
        # Given: 测试用例
        test_case = {
            "case_name": "用户登录测试",
            "steps": "1. 输入用户名 2. 输入密码 3. 点击登录",
            "expected_result": "登录成功",
        }

        # When: 生成向量
        embedding = deduplicator.generate_embedding(test_case)

        # Then: 验证向量
        assert isinstance(embedding, np.ndarray)
        assert embedding.shape[0] == 768  # Sentence-BERT默认维度
        mock_sentence_bert.encode.assert_called_once()

    def test_batch_deduplication_performance(self, deduplicator):
        """场景4.2: 批量去重性能测试"""
        # Given: 大量测试用例
        test_cases = [
            {
                "case_name": f"测试用例{i}",
                "steps": f"执行步骤{i % 10}",  # 每10个用例会有重复
            }
            for i in range(50)
        ]

        # When: 执行批量去重
        import time
        start_time = time.time()
        unique_cases, removed_count = deduplicator.deduplicate(test_cases)
        elapsed_time = time.time() - start_time

        # Then: 验证去重结果和性能
        assert len(unique_cases) > 0
        assert removed_count >= 0
        assert elapsed_time < 10  # 应该在10秒内完成

    def test_handle_empty_input(self, deduplicator):
        """场景5.1: 处理空输入"""
        # Given: 空列表
        test_cases = []

        # When: 执行去重
        unique_cases, removed_count = deduplicator.deduplicate(test_cases)

        # Then: 应该返回空列表
        assert len(unique_cases) == 0
        assert removed_count == 0

    def test_handle_single_case(self, deduplicator):
        """场景5.2: 处理单个用例"""
        # Given: 单个用例
        test_cases = [{"case_name": "唯一用例", "steps": "测试步骤"}]

        # When: 执行去重
        unique_cases, removed_count = deduplicator.deduplicate(test_cases)

        # Then: 应该保留该用例
        assert len(unique_cases) == 1
        assert removed_count == 0

    def test_handle_missing_fields(self, deduplicator):
        """场景5.3: 处理缺失字段的用例"""
        # Given: 缺少某些字段的用例
        test_cases = [
            {"case_name": "用例1"},  # 缺少steps
            {"case_name": "用例2", "steps": "步骤2"},
            {"steps": "步骤3"},  # 缺少case_name
        ]

        # When: 执行去重（应该不抛出异常）
        unique_cases, removed_count = deduplicator.deduplicate(test_cases)

        # Then: 应该正常处理
        assert unique_cases is not None
        assert isinstance(removed_count, int)

    def test_preserve_metadata_after_deduplication(self, deduplicator):
        """场景6.1: 去重后保留元数据"""
        # Given: 包含额外元数据的测试用例
        test_cases = [
            {
                "case_name": "用户登录",
                "steps": "登录系统",
                "priority": "HIGH",
                "ai_confidence": 0.92,
                "module": "用户认证",
            },
            {
                "case_name": "登录测试",  # 相似用例
                "steps": "用户登录",
                "priority": "MEDIUM",
                "ai_confidence": 0.88,
                "module": "用户认证",
            },
        ]

        # When: 执行去重
        unique_cases, removed_count = deduplicator.deduplicate(test_cases)

        # Then: 保留的用例应该包含所有元数据
        assert len(unique_cases) > 0
        kept_case = unique_cases[0]
        assert "priority" in kept_case
        assert "ai_confidence" in kept_case
        assert "module" in kept_case

    def test_keep_higher_confidence_case(self, deduplicator):
        """场景6.2: 保留置信度更高的用例"""
        # Given: 相似但置信度不同的用例
        test_cases = [
            {
                "case_name": "用户登录测试",
                "steps": "登录",
                "ai_confidence": 0.85,
            },
            {
                "case_name": "登录功能验证",
                "steps": "登录系统",
                "ai_confidence": 0.95,  # 更高置信度
            },
        ]

        # When: 执行去重
        unique_cases, removed_count = deduplicator.deduplicate(test_cases)

        # Then: 应该保留高置信度的用例
        if len(unique_cases) == 1:
            # 如果去重了，应该保留高置信度的
            assert unique_cases[0]["ai_confidence"] == 0.95


class TestTestCaseEmbedding:
    """TestCaseEmbedding测试用例向量化测试"""

    def test_create_embedding_from_case(self):
        """场景1: 从测试用例创建向量表示"""
        # Given: 测试用例
        test_case = {
            "case_name": "订单创建测试",
            "steps": "1. 选择商品 2. 提交订单",
            "expected_result": "订单创建成功",
        }

        # When: 创建向量
        embedding = TestCaseEmbedding(test_case)

        # Then: 验证向量对象
        assert embedding.case_name == "订单创建测试"
        assert "选择商品" in embedding.combined_text
        assert "订单创建成功" in embedding.combined_text

    def test_combined_text_generation(self):
        """场景2: 验证组合文本生成"""
        # Given: 测试用例
        test_case = {
            "case_name": "支付测试",
            "steps": "完成支付",
            "expected_result": "支付成功",
            "module": "支付模块",
        }

        # When: 创建向量
        embedding = TestCaseEmbedding(test_case)

        # Then: 组合文本应该包含所有重要字段
        combined = embedding.combined_text
        assert "支付测试" in combined
        assert "完成支付" in combined
        assert "支付成功" in combined
