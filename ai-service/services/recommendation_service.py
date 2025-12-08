"""
Recommendation Service

Business logic for test strategy recommendation
"""
from typing import Dict, Any, Optional
import logging
from datetime import datetime

from models.recommendation.strategy_recommender import TestStrategyRecommender
from models.recommendation.risk_predictor import RiskPredictor, EnvironmentRecommender
from data.backend_client import backend_client

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
            request_data: Request containing task context, environment_id, version_id

        Returns:
            Recommendation result
        """
        task_id = request_data.get('task_id')
        environment_id = request_data.get('environment_id')
        version_id = request_data.get('version_id')
        context = request_data.get('context', {})

        logger.info(f"Processing recommendation request for task: {task_id}, "
                   f"environment: {environment_id}, version: {version_id}")

        # Fetch environment and version information from Backend
        environment_info = None
        version_info = None

        if environment_id:
            environment_info = backend_client.get_environment(environment_id)
            if environment_info:
                logger.info(f"Fetched environment info: {environment_info.get('name')}")

        if version_id:
            version_info = backend_client.get_version(version_id)
            if version_info:
                logger.info(f"Fetched version info: {version_info.get('versionNumber')}")

        # Enhance context with environment and version information
        enhanced_context = self._enhance_context_with_env_version(
            context, environment_info, version_info
        )

        # Get strategy recommendation with enhanced context
        recommendation = self.strategy_recommender.recommend(enhanced_context)

        # Adjust recommendation based on environment and version
        recommendation = self._adjust_recommendation_by_environment(
            recommendation, environment_info
        )
        recommendation = self._adjust_recommendation_by_version(
            recommendation, version_info
        )

        # Get risk assessment
        code_change = context.get('code_change', {})
        risk_assessment = self.risk_predictor.predict(code_change)

        # Adjust risk assessment based on environment
        if environment_info:
            risk_assessment = self._adjust_risk_by_environment(
                risk_assessment, environment_info
            )

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

    def _enhance_context_with_env_version(
        self,
        context: Dict[str, Any],
        environment_info: Optional[Dict[str, Any]],
        version_info: Optional[Dict[str, Any]]
    ) -> Dict[str, Any]:
        """
        Enhance context with environment and version information

        Args:
            context: Original context
            environment_info: Environment details from Backend
            version_info: Version details from Backend

        Returns:
            Enhanced context
        """
        enhanced = context.copy()

        # Add environment metadata
        if environment_info:
            enhanced['environment_metadata'] = {
                'name': environment_info.get('name'),
                'type': environment_info.get('type'),
                'is_production': environment_info.get('name', '').upper() == 'PROD',
                'stability_score': environment_info.get('config', {}).get('stability_score', 0.95),
                'is_critical': environment_info.get('config', {}).get('is_critical', False)
            }

        # Add version metadata
        if version_info:
            version_number = version_info.get('versionNumber', '0.0.0')
            enhanced['version_metadata'] = {
                'version_number': version_number,
                'description': version_info.get('description', ''),
                'is_major_release': self._is_major_release(version_number),
                'is_hotfix': self._is_hotfix(version_info.get('description', '')),
                'release_date': version_info.get('releaseDate')
            }

        return enhanced

    def _adjust_recommendation_by_environment(
        self,
        recommendation: Dict[str, Any],
        environment_info: Optional[Dict[str, Any]]
    ) -> Dict[str, Any]:
        """
        Adjust recommendation based on environment characteristics

        Args:
            recommendation: Original recommendation
            environment_info: Environment details

        Returns:
            Adjusted recommendation
        """
        if not environment_info:
            return recommendation

        env_name = environment_info.get('name', '').upper()

        # Adjust test scope based on environment
        if env_name == 'PROD':
            # Production environment requires full regression
            recommendation['test_scope'] = 'FULL'
            recommendation['priority'] = min(10, recommendation.get('priority', 5) + 2)
            recommendation['reasoning'].append(
                f"环境为生产环境({env_name})，推荐全量回归测试以确保稳定性"
            )
        elif env_name == 'STAGING':
            # Staging environment - core regression
            if recommendation.get('test_scope') == 'SMOKE':
                recommendation['test_scope'] = 'CORE'
            recommendation['reasoning'].append(
                f"环境为预发环境({env_name})，推荐核心回归测试"
            )
        elif env_name == 'DEV':
            # Development environment - smoke test acceptable
            if recommendation.get('test_scope') == 'FULL':
                recommendation['test_scope'] = 'CORE'
            recommendation['reasoning'].append(
                f"环境为开发环境({env_name})，可进行冒烟或核心测试"
            )

        # Adjust based on environment criticality
        if environment_info.get('config', {}).get('is_critical'):
            recommendation['priority'] = min(10, recommendation.get('priority', 5) + 1)
            recommendation['reasoning'].append(
                "该环境被标记为关键环境，优先级提升"
            )

        return recommendation

    def _adjust_recommendation_by_version(
        self,
        recommendation: Dict[str, Any],
        version_info: Optional[Dict[str, Any]]
    ) -> Dict[str, Any]:
        """
        Adjust recommendation based on version characteristics

        Args:
            recommendation: Original recommendation
            version_info: Version details

        Returns:
            Adjusted recommendation
        """
        if not version_info:
            return recommendation

        version_number = version_info.get('versionNumber', '')
        description = version_info.get('description', '').lower()

        # Check if it's a major release (e.g., 1.0.0 -> 2.0.0)
        if self._is_major_release(version_number):
            recommendation['test_scope'] = 'FULL'
            recommendation['priority'] = min(10, recommendation.get('priority', 5) + 2)
            recommendation['reasoning'].append(
                f"版本{version_number}为大版本升级，建议全面回归测试"
            )

        # Check if it's a hotfix or emergency release
        if self._is_hotfix(description):
            recommendation['priority'] = min(10, recommendation.get('priority', 5) + 3)
            recommendation['reasoning'].append(
                f"版本{version_number}为紧急修复版本，优先级提升"
            )
        else:
            # Regular minor release
            recommendation['reasoning'].append(
                f"版本{version_number}为常规版本升级，风险可控"
            )

        return recommendation

    def _adjust_risk_by_environment(
        self,
        risk_assessment: Dict[str, Any],
        environment_info: Dict[str, Any]
    ) -> Dict[str, Any]:
        """
        Adjust risk assessment based on environment

        Args:
            risk_assessment: Original risk assessment
            environment_info: Environment details

        Returns:
            Adjusted risk assessment
        """
        env_name = environment_info.get('name', '').upper()

        # Lower risk tolerance for production
        if env_name == 'PROD':
            current_level = risk_assessment.get('risk_level', 'MEDIUM')
            if current_level == 'MEDIUM':
                risk_assessment['risk_level'] = 'HIGH'
            risk_assessment['risk_factors'].append(
                '生产环境部署，风险容忍度降低'
            )

        return risk_assessment

    def _is_major_release(self, version_number: str) -> bool:
        """
        Check if it's a major release

        Args:
            version_number: Version number (e.g., "2.0.0")

        Returns:
            True if major release, False otherwise
        """
        try:
            parts = version_number.split('.')
            if len(parts) >= 1:
                major = int(parts[0])
                return major >= 2
        except (ValueError, IndexError):
            pass
        return False

    def _is_hotfix(self, description: str) -> bool:
        """
        Check if it's a hotfix/emergency release

        Args:
            description: Version description

        Returns:
            True if hotfix, False otherwise
        """
        hotfix_keywords = ['hotfix', 'emergency', '紧急', '修复', 'urgent', 'critical']
        return any(keyword in description for keyword in hotfix_keywords)

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
