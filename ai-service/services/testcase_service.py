"""
Test Case Generation Service

Business logic for AI test case generation
"""
from typing import Dict, Any, List
import logging
from datetime import datetime
import uuid
import json

from models.llm.rag_generator import RAGTestCaseGenerator
from data.vector_db_factory import vector_db_client  # Auto-selected based on VECTOR_DB_TYPE

logger = logging.getLogger(__name__)


class TestCaseGenerationService:
    """
    Service for AI test case generation

    Orchestrates generation pipeline and manages history
    """

    def __init__(self):
        self.generator = RAGTestCaseGenerator()

    def generate_testcases(self, request_data: Dict[str, Any]) -> Dict[str, Any]:
        """
        Generate test cases from requirement document

        Args:
            request_data: Request containing requirement and options

        Returns:
            Generation result with test cases
        """
        request_id = str(uuid.uuid4())
        requirement_text = request_data.get('requirement_text', '')
        module = request_data.get('module', 'unknown')
        num_cases = request_data.get('num_cases', 5)
        include_edge_cases = request_data.get('include_edge_cases', True)
        optimization_config = request_data.get('optimization', {})

        logger.info(
            f"Processing generation request {request_id} "
            f"for module: {module}, num_cases: {num_cases}"
        )

        # Validate input
        if not requirement_text:
            return {
                'success': False,
                'error': 'requirement_text is required',
                'request_id': request_id
            }

        # Generate test cases
        try:
            generation_result = self.generator.generate(
                requirement_text=requirement_text,
                module=module,
                num_cases=num_cases,
                include_edge_cases=include_edge_cases
            )

            # Apply optimization if requested
            if optimization_config:
                testcases_before_optimization = generation_result.get('testcases', [])
                num_before_optimization = len(testcases_before_optimization)

                optimized = self.generator.optimize_generated_cases(
                    testcases_before_optimization,
                    optimization_config
                )

                num_after_optimization = len(optimized)
                num_filtered = num_before_optimization - num_after_optimization

                generation_result['testcases'] = optimized
                generation_result['optimized'] = True
                generation_result['total_after_optimization'] = num_after_optimization
                generation_result['total_filtered'] = num_filtered

            # Add request info
            generation_result['request_id'] = request_id
            generation_result['timestamp'] = datetime.utcnow().isoformat()

            # Save to history
            self._save_generation_history(
                request_id=request_id,
                request_data=request_data,
                result=generation_result
            )

            return generation_result

        except Exception as e:
            logger.error(f"Test case generation failed: {e}", exc_info=True)
            return {
                'success': False,
                'error': str(e),
                'request_id': request_id
            }

    def batch_generate(self, batch_request: Dict[str, Any]) -> Dict[str, Any]:
        """
        Batch generate test cases for multiple requirements

        Args:
            batch_request: Batch request data

        Returns:
            Batch results
        """
        requirements = batch_request.get('requirements', [])

        if not requirements:
            return {
                'success': False,
                'error': 'requirements list is empty'
            }

        logger.info(f"Processing batch generation for {len(requirements)} requirements")

        results = []
        for req in requirements:
            result = self.generate_testcases(req)
            results.append(result)

        return {
            'success': True,
            'total_requests': len(requirements),
            'successful': sum(1 for r in results if r.get('success')),
            'failed': sum(1 for r in results if not r.get('success')),
            'results': results
        }

    def update_user_feedback(
        self,
        request_id: str,
        feedback: Dict[str, Any]
    ) -> Dict[str, Any]:
        """
        Update user feedback for generated test cases

        Stores feedback in vector database for future model improvement.
        Supports both Qdrant and Milvus backends (auto-selected via VECTOR_DB_TYPE).

        Args:
            request_id: Generation request ID
            feedback: User feedback data containing:
                - rating: int (1-5)
                - comments: str (optional)
                - accepted_cases: List[str] (optional)
                - rejected_cases: List[str] (optional)

        Returns:
            Update result with storage status
        """
        logger.info(f"Updating feedback for request: {request_id}")

        try:
            # Prepare feedback document for vector storage
            feedback_doc = {
                'request_id': request_id,
                'rating': feedback.get('rating'),
                'comments': feedback.get('comments', ''),
                'accepted_cases': feedback.get('accepted_cases', []),
                'rejected_cases': feedback.get('rejected_cases', []),
                'timestamp': datetime.utcnow().isoformat(),
                'type': 'user_feedback'  # Document type for filtering
            }

            # Create searchable text for embedding
            # This allows finding similar feedback patterns
            feedback_text_parts = [
                f"Request: {request_id}",
                f"Rating: {feedback.get('rating')}/5"
            ]

            if feedback.get('comments'):
                feedback_text_parts.append(f"Comments: {feedback.get('comments')}")

            if feedback.get('accepted_cases'):
                feedback_text_parts.append(f"Accepted: {len(feedback.get('accepted_cases'))} cases")

            if feedback.get('rejected_cases'):
                feedback_text_parts.append(f"Rejected: {len(feedback.get('rejected_cases'))} cases")

            feedback_text = " | ".join(feedback_text_parts)

            # Store in vector database
            # This enables semantic search for similar feedback patterns
            feedback_id = f"feedback_{request_id}_{int(datetime.utcnow().timestamp())}"

            success = vector_db_client.add_testcase(
                testcase_id=feedback_id,
                testcase_data={
                    'name': feedback_text,
                    'description': json.dumps(feedback_doc),
                    'module': 'feedback',  # Special module for feedback
                    'priority': self._rating_to_priority(feedback.get('rating', 3)),
                    'type': 'user_feedback'
                }
            )

            if success:
                logger.info(f"✅ Feedback stored in vector database: {feedback_id}")
                return {
                    'success': True,
                    'request_id': request_id,
                    'feedback_id': feedback_id,
                    'message': 'Feedback received and stored successfully',
                    'storage': 'vector_db',
                    'vector_db_type': type(vector_db_client).__name__
                }
            else:
                logger.warning(f"⚠️ Failed to store feedback in vector database")
                return {
                    'success': True,  # Still success from API perspective
                    'request_id': request_id,
                    'message': 'Feedback received but not persisted',
                    'storage': 'none'
                }

        except Exception as e:
            logger.error(f"Error storing feedback: {e}", exc_info=True)
            # Don't fail the API call if storage fails
            return {
                'success': True,
                'request_id': request_id,
                'message': f'Feedback received but storage failed: {str(e)}',
                'storage': 'error'
            }

    def _rating_to_priority(self, rating: int) -> str:
        """
        Convert user rating to priority level for filtering

        Args:
            rating: User rating (1-5)

        Returns:
            Priority level (P0-P3)
        """
        if rating >= 5:
            return 'P0'  # Excellent feedback
        elif rating >= 4:
            return 'P1'  # Good feedback
        elif rating >= 3:
            return 'P2'  # Average feedback
        else:
            return 'P3'  # Poor feedback

    def _save_generation_history(
        self,
        request_id: str,
        request_data: Dict[str, Any],
        result: Dict[str, Any]
    ):
        """
        Save generation history to vector database

        Stores generated test cases in vector database for:
        1. RAG (Retrieval Augmented Generation) - Learn from historical cases
        2. Semantic search - Find similar test cases
        3. Quality improvement - Analyze patterns in successful cases

        Supports both Qdrant and Milvus backends (auto-selected via VECTOR_DB_TYPE).

        Args:
            request_id: Generation request ID
            request_data: Original request data
            result: Generation result containing testcases
        """
        try:
            # Extract generated testcases
            testcases = result.get('testcases', [])

            if not testcases:
                logger.debug(f"No testcases to save for request {request_id}")
                return

            module = request_data.get('module', 'unknown')
            total_added = 0
            failed_count = 0

            # Add each testcase to vector database
            for idx, testcase in enumerate(testcases):
                try:
                    # Generate unique ID for this testcase
                    testcase_id = f"{request_id}_tc_{idx}_{int(datetime.utcnow().timestamp())}"

                    # Prepare testcase data with metadata
                    testcase_data = {
                        'name': testcase.get('name', f'Testcase {idx + 1}'),
                        'description': self._build_testcase_description(testcase),
                        'module': module,
                        'priority': testcase.get('priority', 'P2'),
                        'type': testcase.get('type', '功能测试'),
                        'steps': testcase.get('steps', []),
                        'preconditions': testcase.get('preconditions', []),
                        'tags': testcase.get('tags', []),
                        'request_id': request_id,  # Link back to generation request
                        'generated_at': datetime.utcnow().isoformat() + 'Z'  # Add Z suffix for UTC timezone
                    }

                    # Add to vector database
                    success = vector_db_client.add_testcase(
                        testcase_id=testcase_id,
                        testcase_data=testcase_data
                    )

                    if success:
                        total_added += 1
                        logger.debug(f"Added testcase to vector DB: {testcase_id}")
                    else:
                        failed_count += 1
                        logger.warning(f"Failed to add testcase: {testcase_id}")

                except Exception as e:
                    failed_count += 1
                    logger.error(f"Error adding testcase {idx} to vector DB: {e}", exc_info=True)

            # Log summary
            if total_added > 0:
                logger.info(
                    f"✅ Saved {total_added}/{len(testcases)} testcases to vector database "
                    f"(request: {request_id}, module: {module}, DB: {type(vector_db_client).__name__})"
                )

            if failed_count > 0:
                logger.warning(
                    f"⚠️ Failed to save {failed_count}/{len(testcases)} testcases to vector database"
                )

        except Exception as e:
            logger.error(f"Error saving generation history: {e}", exc_info=True)
            # Don't fail the generation request if history save fails
            logger.warning("Generation history not persisted due to error")

    def _build_testcase_description(self, testcase: Dict[str, Any]) -> str:
        """
        Build searchable description text for testcase embedding

        Creates a rich text representation of the testcase for semantic search.
        This enables RAG to find similar test cases effectively.

        Args:
            testcase: Testcase data

        Returns:
            Formatted description text for embedding
        """
        description_parts = []

        # Add testcase name
        name = testcase.get('name', '')
        if name:
            description_parts.append(f"Test: {name}")

        # Add preconditions
        preconditions = testcase.get('preconditions', [])
        if preconditions:
            if isinstance(preconditions, list):
                precond_text = ", ".join(str(p) for p in preconditions)
                description_parts.append(f"Preconditions: {precond_text}")

        # Add test steps summary
        steps = testcase.get('steps', [])
        if steps and isinstance(steps, list):
            step_count = len(steps)
            description_parts.append(f"Steps: {step_count}")

            # Add first 3 step actions for better semantic search
            for i, step in enumerate(steps[:3]):
                if isinstance(step, dict):
                    action = step.get('action', '')
                    expected = step.get('expected', '')
                    if action:
                        description_parts.append(f"Step {i+1}: {action}")
                    if expected:
                        description_parts.append(f"Expected: {expected}")

        # Add tags
        tags = testcase.get('tags', [])
        if tags:
            if isinstance(tags, list):
                tags_text = ", ".join(str(t) for t in tags)
                description_parts.append(f"Tags: {tags_text}")

        # Add priority and type
        priority = testcase.get('priority', '')
        test_type = testcase.get('type', '')
        if priority:
            description_parts.append(f"Priority: {priority}")
        if test_type:
            description_parts.append(f"Type: {test_type}")

        return " | ".join(description_parts)

    def get_generation_history(
        self,
        limit: int = 50,
        offset: int = 0,
        module: str = None
    ) -> Dict[str, Any]:
        """
        Get test case generation history from vector database

        Args:
            limit: Number of records to return
            offset: Number of records to skip
            module: Filter by module name (optional)

        Returns:
            History records with pagination info
        """
        try:
            logger.info(f"Fetching generation history: limit={limit}, offset={offset}, module={module}")

            # Use scroll_testcases to get records from vector database
            scroll_result = vector_db_client.scroll_testcases(
                limit=limit,
                offset=offset,
                module_filter=module
            )

            total_count = scroll_result.get('total', 0)
            records = scroll_result.get('records', [])

            # Format results for response
            history_records = []
            for record in records:
                # Normalize generated_at to ensure it has timezone info
                generated_at = record.get('generated_at', '')
                if generated_at and not generated_at.endswith('Z') and not generated_at.endswith('+00:00'):
                    # Add Z suffix for UTC timezone if missing
                    generated_at = generated_at + 'Z'
                
                history_records.append({
                    'id': record.get('testcase_id', record.get('id')),
                    'request_id': record.get('request_id', ''),
                    'module': record.get('module', ''),
                    'testcase_name': record.get('name', ''),
                    'type': record.get('type', ''),
                    'priority': record.get('priority', ''),
                    'tags': record.get('tags', []),
                    'generated_at': generated_at,
                    'description': record.get('description', ''),
                    'steps': record.get('steps', []),
                    'preconditions': record.get('preconditions', []),
                })

            result = {
                'success': True,
                'total': total_count,
                'limit': limit,
                'offset': offset,
                'records': history_records
            }

            logger.info(f"Retrieved {len(history_records)} history records (total: {total_count})")
            return result

        except Exception as e:
            logger.error(f"Failed to fetch generation history: {e}", exc_info=True)
            return {
                'success': False,
                'error': str(e),
                'total': 0,
                'records': []
            }


class TestCaseOptimizationService:
    """
    Service for test case optimization operations

    Provides deduplication, prioritization, and quality improvement
    """

    def __init__(self):
        self.generator = RAGTestCaseGenerator()

    def deduplicate(
        self,
        testcases: List[Dict[str, Any]],
        threshold: float = 0.85
    ) -> Dict[str, Any]:
        """
        Deduplicate test cases and return detailed duplicate groups for user review

        Args:
            testcases: Test cases to deduplicate
            threshold: Similarity threshold

        Returns:
            Deduplication result with detailed duplicate groups
        """
        logger.info(f"Deduplicating {len(testcases)} test cases with threshold {threshold}")

        from models.optimization.deduplicator import SemanticDeduplicator

        deduplicator = SemanticDeduplicator(similarity_threshold=threshold)
        unique_cases, duplicate_groups_raw = deduplicator.deduplicate(testcases)

        # Enhance duplicate groups with detailed case information
        enhanced_groups = []
        group_id = 1

        for group in duplicate_groups_raw:
            # Find all cases in this group
            group_cases = []
            representative_name = group.get('representative', '')
            duplicates = group.get('duplicates', [])
            similarity_scores = group.get('similarity_scores', [])

            # Add representative case
            # Support both 'name' and 'caseName' fields
            for tc in testcases:
                tc_name = tc.get('name') or tc.get('caseName', '')
                tc_title = tc.get('title', '')
                if tc_name == representative_name or tc_title == representative_name:
                    case_info = self._enrich_case_metadata(tc, selected=True)
                    group_cases.append(case_info)
                    break

            # Add duplicate cases
            for i, dup_name in enumerate(duplicates):
                for tc in testcases:
                    tc_name = tc.get('name') or tc.get('caseName', '')
                    tc_title = tc.get('title', '')
                    if tc_name == dup_name or tc_title == dup_name:
                        case_info = self._enrich_case_metadata(tc, selected=False)
                        case_info['similarity'] = round(similarity_scores[i] * 100) if i < len(similarity_scores) else 85
                        group_cases.append(case_info)
                        break

            if group_cases:
                # Calculate average similarity for this group
                avg_similarity = sum(c.get('similarity', 100.0) for c in group_cases) / len(group_cases)

                enhanced_groups.append({
                    'id': group_id,
                    'similarity': round(avg_similarity),  # Already in percentage
                    'cases': group_cases,
                    'reason': self._generate_similarity_reason(group_cases)
                })
                group_id += 1

        # Calculate statistics
        total_duplicates = sum(len(g['cases']) - 1 for g in enhanced_groups)
        time_saved = round(total_duplicates * 0.5, 1)  # Assume 30 minutes per case

        return {
            'success': True,
            'statistics': {
                'original_count': len(testcases),
                'unique_count': len(testcases) - total_duplicates,
                'duplicate_count': total_duplicates,
                'duplicate_groups': len(enhanced_groups),
                'time_saved_hours': time_saved
            },
            'duplicate_groups': enhanced_groups,
            'unique_testcases': unique_cases
        }

    def _enrich_case_metadata(self, testcase: Dict[str, Any], selected: bool = False) -> Dict[str, Any]:
        """
        Enrich test case with metadata for duplicate analysis

        Args:
            testcase: Original test case
            selected: Whether this case is selected as representative

        Returns:
            Enriched case information
        """
        import json

        # Parse steps if they are JSON strings
        steps = testcase.get('steps', [])
        parsed_steps = self._parse_steps(steps)

        # Calculate quality score based on completeness
        quality_score = self._calculate_quality_score(testcase)

        # Support both 'name' and 'caseName' fields
        case_name = testcase.get('name') or testcase.get('caseName', '')
        case_title = testcase.get('title', '')

        return {
            'id': testcase.get('id', case_name),
            'testcase_id': testcase.get('id', case_name),
            'case_number': testcase.get('case_number', testcase.get('caseNumber', '')),
            'title': case_title or case_name,
            'name': case_name or case_title,
            'module': testcase.get('module', ''),
            'type': testcase.get('type', ''),
            'priority': testcase.get('priority', 'P2'),
            'description': testcase.get('description', ''),
            'steps': parsed_steps,
            'preconditions': testcase.get('preconditions', []),
            'expected_result': testcase.get('expected_result', testcase.get('expectedResult', '')),
            'quality_score': quality_score,
            'last_executed': testcase.get('last_executed', '未执行'),
            'defects_found': testcase.get('defects_found', 0),
            'selected': selected,
            'similarity': 100 if selected else 85
        }

    def _parse_steps(self, steps: Any) -> List[Dict[str, Any]]:
        """
        Parse steps field which may be in different formats

        Args:
            steps: Steps data (may be list of dicts, list of JSON strings, or JSON string)

        Returns:
            List of step dictionaries
        """
        import json

        if not steps:
            return []

        parsed_steps = []

        if isinstance(steps, str):
            # Single JSON string
            try:
                parsed = json.loads(steps)
                if isinstance(parsed, list):
                    return self._parse_steps(parsed)
                elif isinstance(parsed, dict):
                    return [parsed]
            except (json.JSONDecodeError, TypeError):
                return [{'action': steps}]

        if isinstance(steps, list):
            for step in steps:
                if isinstance(step, str):
                    try:
                        # Try to parse JSON string
                        parsed = json.loads(step)
                        if isinstance(parsed, list):
                            parsed_steps.extend(parsed)
                        elif isinstance(parsed, dict):
                            parsed_steps.append(parsed)
                        else:
                            parsed_steps.append({'action': str(parsed)})
                    except (json.JSONDecodeError, TypeError):
                        # If not JSON, treat as plain text
                        parsed_steps.append({'action': step})
                elif isinstance(step, dict):
                    parsed_steps.append(step)
                else:
                    parsed_steps.append({'action': str(step)})

        return parsed_steps

    def _calculate_quality_score(self, testcase: Dict[str, Any]) -> int:
        """
        Calculate quality score for a test case based on completeness

        Args:
            testcase: Test case dictionary

        Returns:
            Quality score (0-100)
        """
        score = 0

        # Title (10 points)
        if testcase.get('title') or testcase.get('name'):
            score += 10

        # Description (15 points)
        description = testcase.get('description', '')
        if description and len(description) > 20:
            score += 15
        elif description:
            score += 8

        # Steps (30 points)
        steps = testcase.get('steps', [])
        parsed_steps = self._parse_steps(steps)
        if parsed_steps and len(parsed_steps) > 0:
            score += min(30, len(parsed_steps) * 6)

        # Expected result (15 points)
        expected = testcase.get('expected_result', testcase.get('expectedResult', ''))
        if expected and len(expected) > 10:
            score += 15
        elif expected:
            score += 8

        # Preconditions (10 points)
        preconditions = testcase.get('preconditions', [])
        if preconditions and len(preconditions) > 0:
            score += 10

        # Priority (10 points)
        priority = testcase.get('priority', '')
        if priority:
            score += 10

        # Module/Type (10 points)
        if testcase.get('module') or testcase.get('type'):
            score += 10

        return min(100, score)

    def _generate_similarity_reason(self, cases: List[Dict[str, Any]]) -> str:
        """
        Generate human-readable explanation for why cases are similar

        Args:
            cases: List of similar test cases

        Returns:
            Explanation string
        """
        if len(cases) < 2:
            return "单个用例"

        # Analyze similarity factors
        factors = []

        # Check title similarity
        titles = [c.get('title', '') for c in cases]
        if len(set(titles)) == 1:
            factors.append("标题完全相同")
        else:
            factors.append("标题高度相似")

        # Check steps similarity (use parsed steps)
        steps_counts = []
        for c in cases:
            steps = c.get('steps', [])
            parsed_steps = self._parse_steps(steps)
            steps_counts.append(len(parsed_steps))

        if len(set(steps_counts)) == 1 and steps_counts[0] > 0:
            factors.append(f"测试步骤数量相同({steps_counts[0]}个)")
        elif any(c > 0 for c in steps_counts):
            factors.append("测试步骤部分重叠")

        # Check module
        modules = [c.get('module', '') for c in cases if c.get('module')]
        if len(set(modules)) == 1 and modules:
            factors.append(f"同属于'{modules[0]}'模块")

        return "，".join(factors) if factors else "语义相似度较高"

    def prioritize(
        self,
        testcases: List[Dict[str, Any]],
        custom_weights: Dict[str, float] = None
    ) -> Dict[str, Any]:
        """
        Prioritize test cases

        Args:
            testcases: Test cases to prioritize
            custom_weights: Optional custom weights

        Returns:
            Prioritization result
        """
        logger.info(f"Prioritizing {len(testcases)} test cases")

        from models.optimization.prioritizer import TestCasePrioritizer

        prioritizer = TestCasePrioritizer()

        if custom_weights:
            prioritizer.adjust_weights(custom_weights)

        prioritized = prioritizer.prioritize(testcases)

        return {
            'success': True,
            'total_cases': len(testcases),
            'prioritized_testcases': prioritized,
            'weights_used': prioritizer.weights
        }

    def analyze_quality(
        self,
        testcases: List[Dict[str, Any]]
    ) -> Dict[str, Any]:
        """
        Analyze test case quality metrics

        Args:
            testcases: Test cases to analyze

        Returns:
            Quality analysis result
        """
        logger.info(f"Analyzing quality of {len(testcases)} test cases")

        # Calculate metrics
        total = len(testcases)
        priority_dist = self._calculate_priority_distribution(testcases)
        type_dist = self._calculate_type_distribution(testcases)
        avg_steps = sum(len(tc.get('steps', [])) for tc in testcases) / max(total, 1)
        completeness = self._calculate_completeness(testcases)

        return {
            'success': True,
            'total_cases': total,
            'priority_distribution': priority_dist,
            'type_distribution': type_dist,
            'average_steps_per_case': round(avg_steps, 2),
            'completeness_score': round(completeness, 2),
            'quality_grade': self._calculate_quality_grade(completeness)
        }

    def _calculate_priority_distribution(
        self,
        testcases: List[Dict[str, Any]]
    ) -> Dict[str, int]:
        """Calculate priority distribution"""
        dist = {'P0': 0, 'P1': 0, 'P2': 0, 'P3': 0}
        for tc in testcases:
            priority = tc.get('priority', 5)
            
            # Convert integer priority (0-10) to P0-P3 format
            if isinstance(priority, int):
                if priority >= 9:
                    priority = 'P0'
                elif priority >= 7:
                    priority = 'P1'
                elif priority >= 4:
                    priority = 'P2'
                else:
                    priority = 'P3'
            elif isinstance(priority, str):
                priority = priority.upper()
            else:
                priority = 'P3'
            
            if priority in dist:
                dist[priority] += 1
        return dist

    def _calculate_type_distribution(
        self,
        testcases: List[Dict[str, Any]]
    ) -> Dict[str, int]:
        """Calculate test type distribution"""
        dist = {}
        for tc in testcases:
            test_type = tc.get('type') or '功能测试'
            # Ensure type is a string
            if not isinstance(test_type, str):
                test_type = str(test_type) if test_type else '功能测试'
            dist[test_type] = dist.get(test_type, 0) + 1
        return dist

    def _calculate_completeness(self, testcases: List[Dict[str, Any]]) -> float:
        """Calculate completeness score"""
        if not testcases:
            return 0.0

        scores = []
        for tc in testcases:
            score = 0.0
            # Has name or title
            if tc.get('name') or tc.get('title'):
                score += 0.2
            # Has steps
            if tc.get('steps'):
                score += 0.3
            # Has expected results (check both expectedResult field and steps structure)
            has_expected = False
            steps = tc.get('steps', [])
            if steps:
                # Check if steps are dictionaries with 'expected' field
                if isinstance(steps[0], dict):
                    has_expected = any(s.get('expected') for s in steps)
                # If steps are strings, check for expectedResult at test case level
                elif tc.get('expectedResult') or tc.get('expected_result'):
                    has_expected = True
            if has_expected:
                score += 0.2
            # Has preconditions
            if tc.get('preconditions'):
                score += 0.15
            # Has tags
            if tc.get('tags'):
                score += 0.15

            scores.append(score)

        return sum(scores) / len(scores)

    def _calculate_quality_grade(self, completeness: float) -> str:
        """Calculate quality grade from completeness score"""
        if completeness >= 0.9:
            return 'A'
        elif completeness >= 0.75:
            return 'B'
        elif completeness >= 0.6:
            return 'C'
        else:
            return 'D'
