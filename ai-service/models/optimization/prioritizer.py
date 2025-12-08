"""
Test Case Prioritizer

Multi-factor weighted scoring algorithm for test case prioritization
"""
from typing import List, Dict, Any
import logging

logger = logging.getLogger(__name__)


class PriorityFactors:
    """Priority factors configuration"""
    
    def __init__(
        self,
        ai_confidence: float = 0.3,
        priority: float = 0.3,
        coverage: float = 0.2,
        risk: float = 0.2
    ):
        self.ai_confidence = ai_confidence
        self.priority = priority
        self.coverage = coverage
        self.risk = risk
        
    def normalize(self) -> 'PriorityFactors':
        """Normalize weights to sum to 1.0"""
        total = self.ai_confidence + self.priority + self.coverage + self.risk
        if total == 0:
            return self
        return PriorityFactors(
            ai_confidence=self.ai_confidence / total,
            priority=self.priority / total,
            coverage=self.coverage / total,
            risk=self.risk / total
        )


class TestCasePrioritizer:
    """
    Intelligent test case prioritization using multi-factor scoring

    Factors:
    - Business value
    - Risk level
    - Execution cost
    - Historical failure rate
    - Coverage impact
    """

    def __init__(self):
        self.weights = {
            'business_value': 0.30,
            'risk_level': 0.25,
            'execution_cost': 0.20,
            'failure_history': 0.15,
            'coverage_impact': 0.10
        }

    def prioritize(self, testcases: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """
        Prioritize test cases using multi-factor scoring

        Args:
            testcases: List of test case dictionaries

        Returns:
            Sorted list of test cases with priority scores
        """
        scored_cases = []

        for tc in testcases:
            score = self.calculate_priority_score(tc)
            tc_with_score = tc.copy()
            tc_with_score['priority_score'] = score
            tc_with_score['score_breakdown'] = self._get_score_breakdown(tc)
            scored_cases.append(tc_with_score)

        # Sort by score descending
        scored_cases.sort(key=lambda x: x['priority_score'], reverse=True)

        # Add rank
        for i, tc in enumerate(scored_cases):
            tc['rank'] = i + 1

        logger.info(f"Prioritized {len(scored_cases)} test cases")
        return scored_cases

    def calculate_priority_score(self, testcase: Dict[str, Any]) -> float:
        """
        Calculate composite priority score

        Args:
            testcase: Test case dictionary

        Returns:
            Priority score (0-1)
        """
        business_score = self._get_business_value_score(testcase)
        risk_score = self._get_risk_score(testcase)
        cost_score = self._get_execution_cost_score(testcase)
        failure_score = self._get_failure_history_score(testcase)
        coverage_score = self._get_coverage_impact_score(testcase)

        total_score = (
            self.weights['business_value'] * business_score +
            self.weights['risk_level'] * risk_score +
            self.weights['execution_cost'] * cost_score +
            self.weights['failure_history'] * failure_score +
            self.weights['coverage_impact'] * coverage_score
        )

        return round(total_score, 4)

    def _get_business_value_score(self, testcase: Dict[str, Any]) -> float:
        """
        Calculate business value score from priority

        P0 = 1.0, P1 = 0.8, P2 = 0.5, P3 = 0.3
        """
        priority = testcase.get('priority', 'P3')
        priority_map = {'P0': 1.0, 'P1': 0.8, 'P2': 0.5, 'P3': 0.3}
        return priority_map.get(priority, 0.3)

    def _get_risk_score(self, testcase: Dict[str, Any]) -> float:
        """
        Calculate risk score based on test type and tags

        Higher risk = higher score
        """
        score = 0.5  # Base score

        test_type = testcase.get('type', '')
        tags = testcase.get('tags', [])

        # High-risk test types
        if test_type == '安全测试':
            score = 0.9
        elif test_type == '性能测试':
            score = 0.7
        elif test_type == '功能测试':
            score = 0.6

        # High-risk tags
        high_risk_tags = ['支付', '认证', '安全', 'payment', 'auth', 'security']
        for tag in tags:
            if any(risk_tag in tag.lower() for risk_tag in high_risk_tags):
                score = min(1.0, score + 0.1)

        return score

    def _get_execution_cost_score(self, testcase: Dict[str, Any]) -> float:
        """
        Calculate execution cost score

        Lower cost = higher score (we prefer cheaper tests when other factors equal)
        """
        steps = testcase.get('steps', [])
        num_steps = len(steps)

        # Inverse relationship: more steps = lower score
        if num_steps == 0:
            return 0.5
        elif num_steps <= 3:
            return 0.9
        elif num_steps <= 5:
            return 0.7
        elif num_steps <= 10:
            return 0.5
        else:
            return 0.3

    def _get_failure_history_score(self, testcase: Dict[str, Any]) -> float:
        """
        Calculate failure history score

        Higher historical failure rate = higher priority
        """
        # If historical data available, use it
        failure_rate = testcase.get('historical_failure_rate', None)

        if failure_rate is not None:
            return min(1.0, failure_rate * 2)  # Scale up failure rate

        # Estimate based on test type and priority
        priority = testcase.get('priority', 'P3')
        if priority in ['P0', 'P1']:
            return 0.6  # Important tests likely have history
        else:
            return 0.4

    def _get_coverage_impact_score(self, testcase: Dict[str, Any]) -> float:
        """
        Estimate coverage impact

        Tests covering more code paths score higher
        """
        tags = testcase.get('tags', [])
        steps = testcase.get('steps', [])

        # Base score from test type
        test_type = testcase.get('type', '')
        if test_type == '功能测试':
            base_score = 0.6
        else:
            base_score = 0.5

        # Bonus for comprehensive coverage tags
        coverage_tags = ['边界', '异常', '集成', 'edge', 'integration', 'boundary']
        for tag in tags:
            if any(cov_tag in tag.lower() for cov_tag in coverage_tags):
                base_score = min(1.0, base_score + 0.1)

        # Bonus for multiple assertions (steps with expected results)
        expected_count = sum(
            1 for step in steps
            if step.get('expected', '')
        )
        if expected_count >= 5:
            base_score = min(1.0, base_score + 0.1)

        return base_score

    def _get_score_breakdown(self, testcase: Dict[str, Any]) -> Dict[str, float]:
        """
        Get detailed score breakdown for transparency

        Args:
            testcase: Test case dictionary

        Returns:
            Dictionary with individual factor scores
        """
        return {
            'business_value': {
                'score': self._get_business_value_score(testcase),
                'weight': self.weights['business_value']
            },
            'risk_level': {
                'score': self._get_risk_score(testcase),
                'weight': self.weights['risk_level']
            },
            'execution_cost': {
                'score': self._get_execution_cost_score(testcase),
                'weight': self.weights['execution_cost']
            },
            'failure_history': {
                'score': self._get_failure_history_score(testcase),
                'weight': self.weights['failure_history']
            },
            'coverage_impact': {
                'score': self._get_coverage_impact_score(testcase),
                'weight': self.weights['coverage_impact']
            }
        }

    def adjust_weights(self, new_weights: Dict[str, float]):
        """
        Adjust scoring weights

        Args:
            new_weights: New weight dictionary
        """
        # Validate weights sum to 1.0
        total = sum(new_weights.values())
        if abs(total - 1.0) > 0.01:
            raise ValueError(f"Weights must sum to 1.0, got {total}")

        self.weights.update(new_weights)
        logger.info(f"Updated prioritization weights: {self.weights}")

    def reorder_by_criteria(
        self,
        testcases: List[Dict[str, Any]],
        criteria: str = 'score'
    ) -> List[Dict[str, Any]]:
        """
        Reorder test cases by specific criteria

        Args:
            testcases: List of test cases
            criteria: 'score', 'priority', 'type', or 'cost'

        Returns:
            Reordered list
        """
        if criteria == 'score':
            return self.prioritize(testcases)
        elif criteria == 'priority':
            priority_order = {'P0': 0, 'P1': 1, 'P2': 2, 'P3': 3}
            return sorted(
                testcases,
                key=lambda x: priority_order.get(x.get('priority', 'P3'), 4)
            )
        elif criteria == 'type':
            return sorted(testcases, key=lambda x: x.get('type', ''))
        elif criteria == 'cost':
            return sorted(testcases, key=lambda x: len(x.get('steps', [])))
        else:
            raise ValueError(f"Unknown criteria: {criteria}")
