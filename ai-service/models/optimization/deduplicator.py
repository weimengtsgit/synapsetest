"""
Semantic Deduplicator for Test Cases

Uses Sentence-BERT to identify and remove duplicate test cases
"""
from typing import List, Dict, Any, Tuple
import logging
import numpy as np

logger = logging.getLogger(__name__)


class TestCaseEmbedding:
    """Test case embedding representation"""
    
    def __init__(self, testcase: Dict[str, Any]):
        self.case_name = testcase.get('name', '')
        self.steps = testcase.get('steps', [])
        self.expected_result = testcase.get('expected_result', '')
        self.module = testcase.get('module', '')
        self.combined_text = self._generate_combined_text(testcase)
        
    def _generate_combined_text(self, testcase: Dict[str, Any]) -> str:
        """Generate combined text representation"""
        parts = [
            testcase.get('name', ''),
            ' '.join(str(s) for s in testcase.get('steps', [])),
            testcase.get('expected_result', ''),
            testcase.get('module', '')
        ]
        return ' '.join(p for p in parts if p)


class SemanticDeduplicator:
    """
    Test case semantic deduplication using Sentence-BERT

    Identifies semantically similar test cases and keeps the best one
    """

    def __init__(self, similarity_threshold: float = 0.85):
        self.similarity_threshold = similarity_threshold
        self.model = None
        self._initialize_model()

    def _initialize_model(self):
        """Initialize Sentence-BERT model"""
        try:
            from sentence_transformers import SentenceTransformer

            self.model = SentenceTransformer('paraphrase-multilingual-mpnet-base-v2')
            logger.info("Sentence-BERT model loaded successfully")
        except ImportError:
            logger.warning("sentence-transformers not available, using fallback")
            self.model = None
        except Exception as e:
            logger.error(f"Failed to load Sentence-BERT: {e}")
            self.model = None

    def deduplicate(
        self,
        testcases: List[Dict[str, Any]]
    ) -> Tuple[List[Dict[str, Any]], List[Dict[str, Any]]]:
        """
        Deduplicate test cases based on semantic similarity

        Args:
            testcases: List of test case dictionaries

        Returns:
            Tuple of (unique_cases, duplicate_groups)
        """
        if len(testcases) <= 1:
            return testcases, []

        if self.model is None:
            logger.warning("Using text-based fallback for deduplication")
            return self._fallback_deduplicate(testcases)

        # Extract text representations
        case_texts = [self._extract_case_text(tc) for tc in testcases]

        # Compute embeddings
        logger.info(f"Computing embeddings for {len(testcases)} test cases")
        embeddings = self.model.encode(case_texts)

        # Compute similarity matrix
        similarity_matrix = self._compute_similarity_matrix(embeddings)

        # Find duplicates
        unique_cases, duplicate_groups = self._cluster_and_deduplicate(
            testcases, similarity_matrix
        )

        logger.info(
            f"Deduplication complete: {len(testcases)} -> {len(unique_cases)} "
            f"({len(testcases) - len(unique_cases)} duplicates removed)"
        )

        return unique_cases, duplicate_groups

    def _extract_case_text(self, testcase: Dict[str, Any]) -> str:
        """Extract text representation of test case"""
        name = testcase.get('name', '')
        steps = testcase.get('steps', [])
        step_texts = [s.get('action', '') for s in steps]

        combined = f"{name} {' '.join(step_texts)}"
        return combined

    def _compute_similarity_matrix(self, embeddings: np.ndarray) -> np.ndarray:
        """Compute cosine similarity matrix"""
        from sklearn.metrics.pairwise import cosine_similarity
        return cosine_similarity(embeddings)

    def _cluster_and_deduplicate(
        self,
        testcases: List[Dict[str, Any]],
        similarity_matrix: np.ndarray
    ) -> Tuple[List[Dict[str, Any]], List[Dict[str, Any]]]:
        """
        Cluster similar cases and keep best representative

        Args:
            testcases: Original test cases
            similarity_matrix: Pairwise similarity scores

        Returns:
            Tuple of (unique_cases, duplicate_groups)
        """
        unique_cases = []
        duplicate_groups = []
        visited = set()

        for i in range(len(testcases)):
            if i in visited:
                continue

            # Find all similar cases
            similar_indices = np.where(
                similarity_matrix[i] > self.similarity_threshold
            )[0]

            if len(similar_indices) > 1:
                # Multiple similar cases - select best one
                similar_cases = [testcases[j] for j in similar_indices]
                best_case = self._select_best_case(similar_cases)
                unique_cases.append(best_case)

                # Record duplicate group
                duplicate_groups.append({
                    'representative': best_case.get('name', 'Unknown'),
                    'duplicates': [
                        testcases[j].get('name', 'Unknown')
                        for j in similar_indices if j != i
                    ],
                    'similarity_scores': [
                        float(similarity_matrix[i][j])
                        for j in similar_indices if j != i
                    ]
                })
            else:
                # Unique case
                unique_cases.append(testcases[i])

            visited.update(similar_indices)

        return unique_cases, duplicate_groups

    def _select_best_case(self, similar_cases: List[Dict[str, Any]]) -> Dict[str, Any]:
        """
        Select best representative from similar cases

        Scoring criteria:
        - Number of steps (more complete)
        - Preconditions count
        - High priority
        """
        scores = []
        for case in similar_cases:
            score = (
                len(case.get('steps', [])) * 2.0 +
                len(case.get('preconditions', [])) * 1.5 +
                self._priority_score(case.get('priority', 'P3')) * 3.0
            )
            scores.append(score)

        best_index = int(np.argmax(scores))
        return similar_cases[best_index]

    def _priority_score(self, priority: str) -> float:
        """Convert priority to numeric score"""
        priority_map = {'P0': 1.0, 'P1': 0.8, 'P2': 0.5, 'P3': 0.3}
        return priority_map.get(priority, 0.3)

    def _fallback_deduplicate(
        self,
        testcases: List[Dict[str, Any]]
    ) -> Tuple[List[Dict[str, Any]], List[Dict[str, Any]]]:
        """
        Simple text-based deduplication fallback

        Uses exact name matching and step comparison
        """
        seen_names = {}
        unique_cases = []
        duplicate_groups = []

        for tc in testcases:
            name = tc.get('name', '')
            name_key = name.lower().strip()

            if name_key in seen_names:
                # Found potential duplicate
                existing_idx = seen_names[name_key]
                if self._compare_steps(unique_cases[existing_idx], tc):
                    # True duplicate
                    duplicate_groups.append({
                        'representative': unique_cases[existing_idx].get('name'),
                        'duplicates': [name],
                        'similarity_scores': [1.0]
                    })
                else:
                    # Same name but different steps
                    unique_cases.append(tc)
                    seen_names[name_key + str(len(unique_cases))] = len(unique_cases) - 1
            else:
                seen_names[name_key] = len(unique_cases)
                unique_cases.append(tc)

        return unique_cases, duplicate_groups

    def _compare_steps(self, case1: Dict[str, Any], case2: Dict[str, Any]) -> bool:
        """Compare if two cases have similar steps"""
        steps1 = case1.get('steps', [])
        steps2 = case2.get('steps', [])

        if len(steps1) != len(steps2):
            return False

        for s1, s2 in zip(steps1, steps2):
            if s1.get('action', '') != s2.get('action', ''):
                return False

        return True

    def compute_pairwise_similarity(
        self,
        testcases: List[Dict[str, Any]]
    ) -> np.ndarray:
        """
        Compute pairwise similarity for all test cases

        Useful for analysis and visualization
        """
        if self.model is None:
            # Return identity matrix as fallback
            return np.eye(len(testcases))

        case_texts = [self._extract_case_text(tc) for tc in testcases]
        embeddings = self.model.encode(case_texts)
        return self._compute_similarity_matrix(embeddings)
