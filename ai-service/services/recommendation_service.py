"""
Recommendation Service

Business logic for test strategy recommendation
"""
from typing import Dict, Any
import logging
from datetime import datetime

from models.recommendation.strategy_recommender import TestStrategyRecommender
from models.recommendation.risk_predictor import RiskPredictor, EnvironmentRecommender

logger = logging.getLogger(__name__)


class RecommendationService:
    """
    Service for test strategy recommendation

    Orchestrates recommendation models and stores history
    """

    def __init__(self):
        self.strategy_recommender = TestStrategyRecommender()
        self.risk_predictor = RiskPredictor()
        self.env_recommender = EnvironmentRecommender()

    def recommend_strategy(self, request_data: Dict[str, Any]) -> Dict[str, Any]:
        """
        Recommend test strategy for a task

        Args:
            request_data: Request containing task context

        Returns:
            Recommendation result
        """
        task_id = request_data.get('task_id')
        context = request_data.get('context', {})

        logger.info(f"Processing recommendation request for task: {task_id}")

        # Get strategy recommendation
        recommendation = self.strategy_recommender.recommend(context)

        # Get risk assessment
        code_change = context.get('code_change', {})
        risk_assessment = self.risk_predictor.predict(code_change)

        # Get environment recommendations
        env_requirements = {
            'test_scope': recommendation.get('test_scope'),
            'priority': recommendation.get('priority')
        }
        env_recommendations = self.env_recommender.recommend(env_requirements)

        # Combine results
        result = {
            'task_id': task_id,
            'recommendation': recommendation,
            'risk_assessment': risk_assessment,
            'environment_recommendations': env_recommendations,
            'timestamp': datetime.utcnow().isoformat()
        }

        # Note: History not saved (MongoDB removed, using Qdrant/Milvus for vector search)
        
        return result

    def get_recommendation_explanation(
        self,
        recommendation: Dict[str, Any],
        context: Dict[str, Any]
    ) -> Dict[str, Any]:
        """
        Get detailed explanation for recommendation

        Args:
            recommendation: Recommendation result
            context: Original context

        Returns:
            Explanation with insights
        """
        explanation = {
            'summary': self._generate_summary(recommendation),
            'key_factors': self._extract_key_factors(context),
            'reasoning': recommendation.get('reasoning', []),
            'confidence_level': recommendation.get('confidence', 0.0),
            'alternatives': self._suggest_alternatives(recommendation)
        }

        return explanation

    def _generate_summary(self, recommendation: Dict[str, Any]) -> str:
        """Generate human-readable summary"""
        scope = recommendation.get('test_scope', 'CORE')
        env = recommendation.get('environment', 'STAGING')
        priority = recommendation.get('priority', 5)

        return (
            f"推荐执行{scope}范围测试，"
            f"在{env}环境运行，"
            f"优先级为P{priority}"
        )

    def _extract_key_factors(self, context: Dict[str, Any]) -> list:
        """Extract key factors that influenced recommendation"""
        factors = []

        code_change = context.get('code_change', {})
        if code_change.get('changed_files_count', 0) > 10:
            factors.append('代码变更范围较大')

        historical = context.get('historical', {})
        if historical.get('recent_pass_rate', 1.0) < 0.9:
            factors.append('历史通过率偏低')

        business = context.get('business', {})
        if business.get('business_priority') in ['P0', 'P1']:
            factors.append('业务优先级高')

        return factors

    def _suggest_alternatives(self, recommendation: Dict[str, Any]) -> list:
        """Suggest alternative strategies"""
        scope = recommendation.get('test_scope')
        alternatives = []

        if scope == 'FULL':
            alternatives.append({
                'option': 'CORE',
                'description': '如时间紧张，可考虑CORE测试加人工审查'
            })
        elif scope == 'SMOKE':
            alternatives.append({
                'option': 'CORE',
                'description': '如变更涉及关键模块，建议升级到CORE测试'
            })

        return alternatives
