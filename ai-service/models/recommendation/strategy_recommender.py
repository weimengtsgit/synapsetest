"""
Test Strategy Recommender Model

Uses XGBoost classifier + Rule Engine to recommend optimal test strategy.
"""
from typing import Dict, Any, List, Optional
import logging
import json

from utils.feature_extractor import FeatureExtractor
from data.redis_client import redis_client

logger = logging.getLogger(__name__)


class RuleEngine:
    """Rule-based adjustments for recommendations"""

    def apply_rules(
        self,
        prediction: Dict[str, Any],
        context: Dict[str, Any]
    ) -> Dict[str, Any]:
        """
        Apply business rules to adjust ML predictions

        Args:
            prediction: Raw ML model prediction
            context: Task context

        Returns:
            Adjusted recommendation
        """
        recommendation = prediction.copy()

        # Rule 1: Critical modules require FULL scope
        critical_modules = ['payment', 'auth', 'security', 'core']
        changed_modules = context.get('code_change', {}).get('changed_modules', [])

        for module in changed_modules:
            if any(critical in module.lower() for critical in critical_modules):
                recommendation['test_scope'] = 'FULL'
                recommendation['priority'] = max(recommendation.get('priority', 5), 8)
                logger.info(f"Rule applied: Critical module {module} -> FULL scope")
                break

        # Rule 2: Hotfix always requires urgent testing
        change_type = context.get('code_change', {}).get('change_type', '')
        if change_type == 'hotfix':
            recommendation['priority'] = 10
            recommendation['environment'] = 'STAGING'
            logger.info("Rule applied: Hotfix -> Priority 10, STAGING env")

        # Rule 3: Large changes require comprehensive testing
        changed_files = context.get('code_change', {}).get('changed_files_count', 0)
        changed_lines = context.get('code_change', {}).get('changed_lines_count', 0)

        if changed_files > 20 or changed_lines > 500:
            if recommendation.get('test_scope') == 'SMOKE':
                recommendation['test_scope'] = 'CORE'
            logger.info(f"Rule applied: Large change ({changed_files} files) -> Upgraded scope")

        # Rule 4: Low pass rate requires thorough testing
        pass_rate = context.get('historical', {}).get('recent_pass_rate', 1.0)
        if pass_rate < 0.9:
            recommendation['test_scope'] = 'FULL'
            recommendation['priority'] = max(recommendation.get('priority', 5), 7)
            logger.info(f"Rule applied: Low pass rate ({pass_rate}) -> FULL scope")

        # Rule 5: High load environment should be avoided
        current_load = context.get('environment', {}).get('current_load', 0.5)
        if current_load > 0.8:
            # Prefer less loaded environment
            recommendation['reasoning'].append(
                f"当前环境负载较高 ({current_load:.0%})，建议等待或使用备用环境"
            )

        return recommendation


class TestStrategyRecommender:
    """
    Main test strategy recommendation model

    Combines XGBoost ML model with rule-based adjustments
    """

    def __init__(self):
        self.feature_extractor = FeatureExtractor()
        self.rule_engine = RuleEngine()
        self.model = self._load_model()
        self._cache_enabled = True

    def _load_model(self) -> Optional[Any]:
        """
        Load XGBoost model from file

        Returns None if model file not found (uses heuristic fallback)
        """
        try:
            import xgboost as xgb
            from config import ai_config
            import os

            model_path = os.path.join(
                ai_config.XGBOOST_MODEL_PATH,
                'test_strategy_recommender.json'
            )

            if os.path.exists(model_path):
                model = xgb.XGBClassifier()
                model.load_model(model_path)
                logger.info(f"Loaded XGBoost model from {model_path}")
                return model
            else:
                logger.warning(f"Model file not found at {model_path}, using heuristic mode")
                return None
        except ImportError:
            logger.warning("XGBoost not available, using heuristic mode")
            return None
        except Exception as e:
            logger.error(f"Failed to load model: {e}")
            return None

    def recommend(self, task_context: Dict[str, Any]) -> Dict[str, Any]:
        """
        Generate test strategy recommendation

        Args:
            task_context: Contains code_change, historical, business, environment info

        Returns:
            Recommendation with test_scope, environment, priority, etc.
        """
        # Validate input
        if not self.feature_extractor.validate_context(task_context):
            logger.warning("Invalid context, using default recommendation")
            return self._get_default_recommendation()

        # Check cache
        if self._cache_enabled:
            context_hash = self.feature_extractor.compute_context_hash(task_context)
            cached = redis_client.get_recommendation_cache(context_hash)
            if cached:
                logger.info("Returning cached recommendation")
                return cached

        # Extract features
        features = self.feature_extractor.extract_features(task_context)

        # Model prediction
        if self.model is not None:
            prediction = self._ml_predict(features, task_context)
        else:
            prediction = self._heuristic_predict(features, task_context)

        # Apply rules
        recommendation = self.rule_engine.apply_rules(prediction, task_context)

        # Generate explanation
        explanation = self._generate_explanation(task_context, recommendation)
        recommendation['reasoning'] = explanation

        # Calculate confidence
        recommendation['confidence'] = self._calculate_confidence(
            features, task_context, recommendation
        )

        # Cache result
        if self._cache_enabled:
            redis_client.set_recommendation_cache(context_hash, recommendation)

        return recommendation

    def _ml_predict(
        self,
        features: List[float],
        context: Dict[str, Any]
    ) -> Dict[str, Any]:
        """Use ML model for prediction"""
        import numpy as np

        X = np.array([features])
        probabilities = self.model.predict_proba(X)[0]

        # Map class indices to scopes
        scope_map = {0: 'SMOKE', 1: 'CORE', 2: 'FULL'}
        predicted_class = int(np.argmax(probabilities))

        return {
            'test_scope': scope_map.get(predicted_class, 'CORE'),
            'environment': self._recommend_environment(context),
            'priority': self._calculate_priority(features),
            'estimated_duration': self._estimate_duration(features),
            'resource_requirement': self._estimate_resources(features),
            'reasoning': []
        }

    def _heuristic_predict(
        self,
        features: List[float],
        context: Dict[str, Any]
    ) -> Dict[str, Any]:
        """Use heuristic rules when ML model unavailable"""
        # Simple heuristic based on key features
        changed_files = features[0]
        changed_lines = features[1]
        pass_rate = features[4]
        module_importance = features[8]
        priority_score = features[9]

        # Determine test scope
        if changed_files > 10 or changed_lines > 300 or pass_rate < 0.85:
            test_scope = 'FULL'
        elif changed_files > 3 or changed_lines > 100 or module_importance > 0.7:
            test_scope = 'CORE'
        else:
            test_scope = 'SMOKE'

        # Determine priority
        base_priority = int(priority_score * 10)
        if module_importance > 0.8:
            base_priority = min(10, base_priority + 2)

        return {
            'test_scope': test_scope,
            'environment': self._recommend_environment(context),
            'priority': base_priority,
            'estimated_duration': self._estimate_duration(features),
            'resource_requirement': self._estimate_resources(features),
            'reasoning': []
        }

    def _recommend_environment(self, context: Dict[str, Any]) -> str:
        """Recommend testing environment"""
        env_status = redis_client.get_environment_status()

        # Score each environment
        scores = {}
        for env_name, status in env_status.items():
            if not status.get('available', False):
                continue

            score = (
                0.3 * (1 - status.get('load', 0.5)) +
                0.4 * status.get('stability', 0.95) +
                0.3 * (1 if env_name == 'staging' else 0.7)
            )
            scores[env_name] = score

        if not scores:
            return 'STAGING'

        # Return highest scoring environment
        best_env = max(scores.items(), key=lambda x: x[1])
        return best_env[0].upper()

    def _calculate_priority(self, features: List[float]) -> int:
        """Calculate task priority from features"""
        # Weighted sum of priority-related features
        module_importance = features[8]
        business_priority = features[9]
        urgency = features[10]

        priority = int(
            (module_importance * 0.3 + business_priority * 0.4 + urgency * 0.3) * 10
        )
        return max(1, min(10, priority))

    def _estimate_duration(self, features: List[float]) -> int:
        """Estimate test execution duration in minutes"""
        changed_files = features[0]
        changed_lines = features[1]
        avg_historical_time = features[5]

        # Base estimate from historical data
        base_time = avg_historical_time if avg_historical_time > 0 else 30

        # Adjust based on change size
        multiplier = 1.0 + (changed_files * 0.1) + (changed_lines / 500)

        return int(base_time * multiplier)

    def _estimate_resources(self, features: List[float]) -> int:
        """Estimate required resources (number of executors)"""
        changed_files = features[0]
        queue_length = features[12]

        # Base requirement
        resources = 2

        # Scale with change size
        if changed_files > 10:
            resources = 4
        elif changed_files > 5:
            resources = 3

        # Consider queue length
        if queue_length > 10:
            resources = min(resources + 1, 6)

        return resources

    def _generate_explanation(
        self,
        context: Dict[str, Any],
        recommendation: Dict[str, Any]
    ) -> List[str]:
        """Generate human-readable explanation for recommendation"""
        explanations = []

        code_change = context.get('code_change', {})
        historical = context.get('historical', {})
        business = context.get('business', {})

        # Explain test scope
        scope = recommendation.get('test_scope', 'CORE')
        if scope == 'FULL':
            if code_change.get('changed_files_count', 0) > 10:
                explanations.append(
                    f"变更文件数 ({code_change.get('changed_files_count')}) 较多，建议FULL测试"
                )
            if historical.get('recent_pass_rate', 1.0) < 0.9:
                explanations.append(
                    f"近期通过率 ({historical.get('recent_pass_rate'):.0%}) 偏低，建议全面测试"
                )
        elif scope == 'CORE':
            explanations.append("变更范围适中，建议CORE测试覆盖主要功能")
        else:
            explanations.append("小范围变更，SMOKE测试足够验证基本功能")

        # Explain environment
        env = recommendation.get('environment', 'STAGING')
        explanations.append(f"{env}环境当前负载较低，可用性高")

        # Explain priority
        priority = recommendation.get('priority', 5)
        if priority >= 8:
            explanations.append(f"高优先级任务 (P{priority})，建议优先执行")
        elif priority >= 5:
            explanations.append(f"中等优先级任务 (P{priority})")

        # Explain duration
        duration = recommendation.get('estimated_duration', 30)
        explanations.append(f"预计执行时间{duration}分钟")

        return explanations

    def _calculate_confidence(
        self,
        features: List[float],
        context: Dict[str, Any],
        recommendation: Dict[str, Any]
    ) -> float:
        """Calculate confidence score for recommendation"""
        confidence = 0.85  # Base confidence

        # Adjust based on data completeness
        if context.get('historical', {}).get('recent_pass_rate') is None:
            confidence -= 0.1

        # Adjust based on feature quality
        if features[0] == 0:  # No file change info
            confidence -= 0.1

        # Adjust based on model availability
        if self.model is None:
            confidence -= 0.15

        return max(0.5, min(1.0, confidence))

    def _get_default_recommendation(self) -> Dict[str, Any]:
        """Return default recommendation when context is invalid"""
        return {
            'test_scope': 'CORE',
            'environment': 'STAGING',
            'priority': 5,
            'estimated_duration': 45,
            'resource_requirement': 3,
            'confidence': 0.5,
            'reasoning': ['使用默认推荐配置（缺少完整上下文信息）']
        }
