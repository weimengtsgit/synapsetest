"""
Feature Extractor for AI Models
"""
from typing import Dict, Any, List
import hashlib
import json
import logging

logger = logging.getLogger(__name__)


class FeatureExtractor:
    """Extract features from task context for AI models"""

    def __init__(self):
        self.feature_names = [
            'changed_files_count',
            'changed_lines_count',
            'code_complexity_delta',
            'test_coverage_delta',
            'recent_pass_rate',
            'avg_execution_time',
            'recent_defect_count',
            'failure_frequency',
            'module_importance',
            'business_priority_score',
            'release_urgency_score',
            'available_resources',
            'queue_length',
            'env_stability_score',
            'current_load'
        ]

    def extract_features(self, context: Dict[str, Any]) -> List[float]:
        """
        Extract feature vector from task context

        Args:
            context: Task context containing code_change, historical, business, environment

        Returns:
            Feature vector as list of floats
        """
        features = []

        # Code change features
        code_change = context.get('code_change', {})
        features.append(float(code_change.get('changed_files_count', 0)))
        features.append(float(code_change.get('changed_lines_count', 0)))
        features.append(float(code_change.get('code_complexity_delta', 0.0)))
        features.append(float(code_change.get('test_coverage_delta', 0.0)))

        # Historical features
        historical = context.get('historical', {})
        features.append(float(historical.get('recent_pass_rate', 0.95)))
        features.append(float(historical.get('avg_execution_time', 30.0)))
        features.append(float(historical.get('recent_defect_count', 0)))
        features.append(float(historical.get('failure_frequency', 0.05)))

        # Business features
        business = context.get('business', {})
        features.append(float(business.get('module_importance', 0.5)))
        features.append(self._convert_priority_to_score(
            business.get('business_priority', 'P2')
        ))
        features.append(self._convert_urgency_to_score(
            business.get('release_urgency', 'normal')
        ))

        # Environment features
        environment = context.get('environment', {})
        features.append(float(environment.get('available_resources', 5)))
        features.append(float(environment.get('queue_length', 0)))
        features.append(float(environment.get('env_stability_score', 0.95)))
        features.append(float(environment.get('current_load', 0.5)))

        return features

    def _convert_priority_to_score(self, priority: str) -> float:
        """Convert priority string to numeric score"""
        priority_map = {
            'P0': 1.0,
            'P1': 0.8,
            'P2': 0.5,
            'P3': 0.3
        }
        return priority_map.get(priority, 0.5)

    def _convert_urgency_to_score(self, urgency: str) -> float:
        """Convert urgency string to numeric score"""
        urgency_map = {
            'urgent': 1.0,
            'normal': 0.5,
            'low': 0.2
        }
        return urgency_map.get(urgency, 0.5)

    def compute_context_hash(self, context: Dict[str, Any]) -> str:
        """
        Compute hash of context for caching

        Args:
            context: Task context

        Returns:
            Hash string
        """
        # Sort keys for consistent hashing
        context_str = json.dumps(context, sort_keys=True, default=str)
        return hashlib.md5(context_str.encode()).hexdigest()

    def get_feature_names(self) -> List[str]:
        """Get list of feature names"""
        return self.feature_names.copy()

    def validate_context(self, context: Dict[str, Any]) -> bool:
        """
        Validate that context has required fields

        Args:
            context: Task context to validate

        Returns:
            True if valid, False otherwise
        """
        required_keys = ['code_change', 'historical', 'business', 'environment']
        for key in required_keys:
            if key not in context:
                logger.warning(f"Missing required key in context: {key}")
                return False
        return True


class RiskFeatureExtractor:
    """Extract features specifically for risk prediction"""

    def extract_risk_features(self, code_change: Dict[str, Any]) -> List[float]:
        """
        Extract features for risk assessment

        Args:
            code_change: Code change information

        Returns:
            Feature vector for risk prediction
        """
        features = []

        # File-level features
        features.append(float(code_change.get('changed_files_count', 0)))
        features.append(float(code_change.get('changed_lines_count', 0)))

        # Complexity features
        features.append(float(code_change.get('code_complexity_delta', 0.0)))
        features.append(float(code_change.get('cyclomatic_complexity', 1.0)))

        # Historical risk indicators
        features.append(float(code_change.get('past_bug_density', 0.0)))
        features.append(float(code_change.get('recent_revisions', 0)))

        # Module characteristics
        features.append(self._get_module_risk_score(
            code_change.get('changed_modules', [])
        ))

        # Change type risk
        features.append(self._get_change_type_risk(
            code_change.get('change_type', 'feature')
        ))

        return features

    def _get_module_risk_score(self, modules: List[str]) -> float:
        """Calculate risk score based on modules affected"""
        high_risk_modules = ['payment', 'auth', 'security', 'core', 'database']

        if not modules:
            return 0.3

        risk_count = sum(
            1 for module in modules
            if any(risk_module in module.lower() for risk_module in high_risk_modules)
        )

        return min(1.0, risk_count * 0.3 + 0.2)

    def _get_change_type_risk(self, change_type: str) -> float:
        """Get risk score based on change type"""
        risk_map = {
            'hotfix': 0.9,
            'bugfix': 0.7,
            'feature': 0.5,
            'refactor': 0.6,
            'docs': 0.1
        }
        return risk_map.get(change_type.lower(), 0.5)
