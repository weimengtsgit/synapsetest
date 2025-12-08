"""
RAG Test Case Generator

Combines LLM generation with RAG (Retrieval Augmented Generation)
"""
from typing import List, Dict, Any, Optional
import logging
import json
import time

from models.llm.qwen_model import create_llm_model
from models.optimization.deduplicator import SemanticDeduplicator
from models.optimization.prioritizer import TestCasePrioritizer
from utils.prompt_builder import PromptBuilder, DocumentParser
from data.db_factory import db_client  # Automatically uses MySQL
from data.rag_manager import rag_manager
from config import ai_config

logger = logging.getLogger(__name__)


class RAGTestCaseGenerator:
    """
    AI-powered test case generator using RAG approach

    Pipeline:
    1. Parse requirement document
    2. Retrieve similar historical test cases (RAG)
    3. Load company testing standards
    4. Build prompt with context
    5. Generate test cases using LLM
    6. Post-process and validate
    7. Deduplicate and prioritize
    """

    def __init__(self):
        # Prepare kwargs for LLM model creation based on provider
        llm_kwargs = {
            'api_key': ai_config.LLM_API_KEY,
            'api_base': ai_config.LLM_API_BASE,
            'model_name': ai_config.LLM_MODEL_NAME
        }

        # Add model path based on provider type
        provider = ai_config.LLM_PROVIDER
        if provider == 'qwen-local':
            llm_kwargs['model_path'] = ai_config.QWEN_MODEL_PATH
        elif provider == 'deepseek-local':
            llm_kwargs['model_path'] = ai_config.DEEPSEEK_MODEL_PATH
            llm_kwargs['model_name'] = ai_config.DEEPSEEK_MODEL_NAME
        # For backward compatibility
        elif provider == 'local':
            llm_kwargs['model_path'] = ai_config.QWEN_MODEL_PATH

        self.llm = create_llm_model(provider=provider, **llm_kwargs)
        self.prompt_builder = PromptBuilder()
        self.doc_parser = DocumentParser()
        self.deduplicator = SemanticDeduplicator(
            similarity_threshold=ai_config.SIMILARITY_THRESHOLD
        )
        self.prioritizer = TestCasePrioritizer()

    def generate(
        self,
        requirement_text: str,
        module: str,
        num_cases: int = 5,
        include_edge_cases: bool = True
    ) -> Dict[str, Any]:
        """
        Generate test cases from requirement document   

        Args:
            requirement_text: Requirement document text
            module: Module name for context retrieval
            num_cases: Number of test cases to generate
            include_edge_cases: Whether to generate edge cases

        Returns:
            Generation result with test cases and metadata
        """
        logger.info(f"Generating {num_cases} test cases for module: {module}")

        # Step 1: Parse document
        parsed_text = self.doc_parser.parse(requirement_text)

        # Step 2: Retrieve similar cases using RAG (Vector DB + MySQL)
        # This combines semantic search (Qdrant/Milvus) with complete data (MySQL)
        similar_cases = rag_manager.search_similar_testcases(
            query_text=parsed_text,
            top_k=3,
            module_filter=module if module != 'unknown' else None
        )
        logger.info(f"Retrieved {len(similar_cases)} similar historical cases via RAG")

        # Step 3: Load company standards from structured DB (MySQL)
        company_standards = db_client.get_company_standards()

        # Step 4: Build prompt
        prompt = self.prompt_builder.build_generation_prompt(
            requirement_text=parsed_text,
            similar_testcases=similar_cases,
            company_standards=company_standards,
            num_cases=num_cases
        )

        # Step 5: Generate using LLM
        logger.info("Calling LLM for test case generation")

        # Log LLM request details - Full prompt
        logger.info(f"[LLM REQUEST] max_tokens={ai_config.MAX_TOKENS}, temperature={ai_config.TEMPERATURE}, top_p={ai_config.TOP_P}")
        logger.info(f"[LLM PROMPT - FULL]\n{prompt}")

        llm_start_time = time.time()

        try:
            response = self.llm.generate(
                prompt=prompt,
                max_tokens=ai_config.MAX_TOKENS,
                temperature=ai_config.TEMPERATURE,
                top_p=ai_config.TOP_P
            )

            llm_elapsed = time.time() - llm_start_time
            logger.info(f"[LLM RESPONSE] Success, Time: {llm_elapsed:.2f}s, Length: {len(response)} chars")
            logger.info(f"[LLM RESPONSE - FULL]\n{response}")

        except Exception as e:
            llm_elapsed = time.time() - llm_start_time
            logger.error(f"[LLM RESPONSE] Failed, Time: {llm_elapsed:.2f}s, Error: {e}")
            return {
                'success': False,
                'error': str(e),
                'testcases': []
            }

        # Step 6: Parse and validate response
        testcases = self._parse_llm_response(response)
        validated_cases = self._validate_testcases(testcases)

        # Step 7: Generate edge cases if requested
        if include_edge_cases:
            edge_cases = self._generate_edge_cases(parsed_text)
            validated_cases.extend(edge_cases)

        # Step 8: Deduplicate
        unique_cases, duplicate_groups = self.deduplicator.deduplicate(validated_cases)
        logger.info(f"After deduplication: {len(unique_cases)} unique cases")

        # Step 9: Prioritize
        prioritized_cases = self.prioritizer.prioritize(unique_cases)

        # Step 10: Prepare result
        result = {
            'success': True,
            'total_generated': len(validated_cases),
            'total_unique': len(unique_cases),
            'total_duplicates': len(validated_cases) - len(unique_cases),
            'testcases': prioritized_cases,
            'duplicate_groups': duplicate_groups,
            'metadata': {
                'module': module,
                'num_requested': num_cases,
                'num_delivered': len(prioritized_cases),
                'include_edge_cases': include_edge_cases,
                'llm_model': self.llm.get_model_info()
            }
        }

        return result

    def _parse_llm_response(self, response: str) -> List[Dict[str, Any]]:
        """
        Parse LLM response to extract test cases

        Handles JSON, markdown code blocks, truncated responses, and plain text
        """
        if not response or not response.strip():
            logger.warning("Empty LLM response")
            return []

        # Clean the response
        response = response.strip()

        # Step 1: Try direct JSON parse (fastest path)
        try:
            testcases = json.loads(response)
            if isinstance(testcases, list):
                logger.info(f"Parsed {len(testcases)} test cases (direct JSON)")
                return testcases
            elif isinstance(testcases, dict) and 'testcases' in testcases:
                logger.info(f"Parsed {len(testcases['testcases'])} test cases (wrapped JSON)")
                return testcases['testcases']
        except json.JSONDecodeError:
            pass

        # Step 2: Extract JSON from markdown code blocks
        import re

        # Pattern for markdown code blocks (greedy to capture full content)
        markdown_patterns = [
            r'```json\s*(\[[\s\S]*?\])\s*```',  # ```json [...] ```
            r'```\s*(\[[\s\S]*?\])\s*```',      # ``` [...] ```
        ]

        for pattern in markdown_patterns:
            matches = re.findall(pattern, response, re.DOTALL)
            if matches:
                for match in matches:
                    try:
                        testcases = json.loads(match)
                        if isinstance(testcases, list):
                            logger.info(f"Parsed {len(testcases)} test cases (markdown block)")
                            return testcases
                    except json.JSONDecodeError:
                        continue

        # Step 3: Handle truncated markdown block (missing closing ```)
        # Extract content between ```json and end of string
        truncated_pattern = r'```json\s*(\[[\s\S]*?)$'
        matches = re.findall(truncated_pattern, response)
        if matches:
            json_str = matches[0].strip()
            # Try to fix truncated JSON
            fixed_json = self._fix_truncated_json(json_str)
            if fixed_json:
                try:
                    testcases = json.loads(fixed_json)
                    if isinstance(testcases, list):
                        logger.warning(f"Parsed {len(testcases)} test cases from truncated response (repaired)")
                        return testcases
                except json.JSONDecodeError:
                    pass

        # Step 4: Find any JSON array in the response
        array_pattern = r'\[\s*\{[\s\S]*?\}\s*\]'
        matches = re.findall(array_pattern, response, re.DOTALL)

        for match in matches:
            try:
                testcases = json.loads(match)
                if isinstance(testcases, list):
                    logger.info(f"Parsed {len(testcases)} test cases (extracted array)")
                    return testcases
            except json.JSONDecodeError:
                continue

        # Step 5: Try to fix and parse any JSON-like content
        # Extract from first [ to last ]
        start_idx = response.find('[')
        end_idx = response.rfind(']')

        if start_idx != -1 and end_idx != -1 and end_idx > start_idx:
            json_str = response[start_idx:end_idx + 1]

            # Try direct parse
            try:
                testcases = json.loads(json_str)
                if isinstance(testcases, list):
                    logger.info(f"Parsed {len(testcases)} test cases (extracted bounds)")
                    return testcases
            except json.JSONDecodeError:
                # Try to fix truncated JSON
                fixed_json = self._fix_truncated_json(json_str)
                if fixed_json:
                    try:
                        testcases = json.loads(fixed_json)
                        if isinstance(testcases, list):
                            logger.warning(f"Parsed {len(testcases)} test cases (repaired)")
                            return testcases
                    except json.JSONDecodeError:
                        pass

        logger.error(f"Failed to parse LLM response. First 200 chars: {response[:200]}...")
        return []

    def _fix_truncated_json(self, json_str: str) -> Optional[str]:
        """
        Attempt to fix truncated JSON by closing unclosed structures

        Args:
            json_str: Potentially truncated JSON string

        Returns:
            Fixed JSON string or None if cannot be fixed
        """
        if not json_str:
            return None

        try:
            # Count opening and closing brackets/braces
            open_brackets = json_str.count('[')
            close_brackets = json_str.count(']')
            open_braces = json_str.count('{')
            close_braces = json_str.count('}')

            # If already balanced, return as is
            if open_brackets == close_brackets and open_braces == close_braces:
                return json_str

            # Make a copy to fix
            fixed = json_str.rstrip()

            # Remove trailing incomplete string or value
            # Find the last complete object
            last_complete = fixed.rfind('}')
            if last_complete == -1:
                return None

            # Truncate to last complete object, remove any trailing comma
            fixed = fixed[:last_complete + 1].rstrip(',').rstrip()

            # Close missing braces
            while fixed.count('{') > fixed.count('}'):
                fixed += '}'

            # Close missing brackets
            while fixed.count('[') > fixed.count(']'):
                fixed += ']'

            # Validate the fixed JSON
            json.loads(fixed)
            logger.info("Successfully repaired truncated JSON")
            return fixed

        except Exception as e:
            logger.debug(f"Could not repair JSON: {e}")
            return None

    def _validate_testcases(self, testcases: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """
        Validate and clean test cases

        Ensures required fields are present and well-formed
        """
        validated = []

        for tc in testcases:
            # Check required fields
            if not tc.get('name'):
                logger.warning("Skipping test case without name")
                continue

            if not tc.get('steps'):
                logger.warning(f"Skipping test case without steps: {tc.get('name')}")
                continue

            # Ensure priority is valid
            if 'priority' not in tc or tc['priority'] not in ['P0', 'P1', 'P2', 'P3']:
                tc['priority'] = 'P2'  # Default priority

            # Ensure type is valid
            if 'type' not in tc:
                tc['type'] = '功能测试'

            # Ensure preconditions is a list
            if 'preconditions' not in tc:
                tc['preconditions'] = []
            elif isinstance(tc['preconditions'], str):
                tc['preconditions'] = [tc['preconditions']]

            # Ensure tags is a list
            if 'tags' not in tc:
                tc['tags'] = []
            elif isinstance(tc['tags'], str):
                tc['tags'] = [tc['tags']]

            # Validate steps format
            if isinstance(tc['steps'], list):
                validated_steps = []
                for i, step in enumerate(tc['steps']):
                    if isinstance(step, dict):
                        validated_steps.append({
                            'step': step.get('step', i + 1),
                            'action': step.get('action', ''),
                            'expected': step.get('expected', '')
                        })
                tc['steps'] = validated_steps

            validated.append(tc)

        logger.info(f"Validated {len(validated)}/{len(testcases)} test cases")
        return validated

    def _generate_edge_cases(self, requirement_text: str) -> List[Dict[str, Any]]:
        """
        Generate edge case test cases

        Uses specialized prompt for boundary and exception scenarios
        """
        logger.info("Generating edge cases")

        edge_prompt = self.prompt_builder.build_edge_case_prompt(requirement_text)

        # Log LLM request details - Full prompt
        logger.info(f"[LLM REQUEST - EDGE CASES] max_tokens={ai_config.EDGE_CASE_MAX_TOKENS}, temperature={ai_config.EDGE_CASE_TEMPERATURE}, top_p={ai_config.TOP_P}")
        logger.info(f"[LLM PROMPT - EDGE CASES - FULL]\n{edge_prompt}")

        edge_start_time = time.time()

        try:
            response = self.llm.generate(
                prompt=edge_prompt,
                max_tokens=ai_config.EDGE_CASE_MAX_TOKENS,
                temperature=ai_config.EDGE_CASE_TEMPERATURE,
                top_p=ai_config.TOP_P
            )

            edge_elapsed = time.time() - edge_start_time
            logger.info(f"[LLM RESPONSE - EDGE CASES] Success, Time: {edge_elapsed:.2f}s, Length: {len(response)} chars")
            logger.info(f"[LLM RESPONSE - EDGE CASES - FULL]\n{response}")

            edge_cases = self._parse_llm_response(response)
            validated = self._validate_testcases(edge_cases)

            logger.info(f"Generated {len(validated)} edge cases")
            return validated

        except Exception as e:
            edge_elapsed = time.time() - edge_start_time
            logger.error(f"[LLM RESPONSE - EDGE CASES] Failed, Time: {edge_elapsed:.2f}s, Error: {e}")
            return []

    def batch_generate(
        self,
        requirements: List[Dict[str, Any]]
    ) -> List[Dict[str, Any]]:
        """
        Generate test cases for multiple requirements

        Args:
            requirements: List of requirement dicts with 'text' and 'module'

        Returns:
            List of generation results
        """
        results = []

        for req in requirements:
            result = self.generate(
                requirement_text=req.get('text', ''),
                module=req.get('module', 'unknown'),
                num_cases=req.get('num_cases', 5),
                include_edge_cases=req.get('include_edge_cases', True)
            )
            results.append(result)

        return results

    def optimize_generated_cases(
        self,
        testcases: List[Dict[str, Any]],
        optimization_config: Optional[Dict[str, Any]] = None
    ) -> List[Dict[str, Any]]:
        """
        Optimize generated test cases based on configuration

        Args:
            testcases: Test cases to optimize
            optimization_config: Optimization settings

        Returns:
            Optimized test cases
        """
        config = optimization_config or {}

        # Apply deduplication
        if config.get('deduplicate', True):
            testcases, _ = self.deduplicator.deduplicate(testcases)

        # Apply prioritization
        if config.get('prioritize', True):
            testcases = self.prioritizer.prioritize(testcases)

        # Filter by priority if specified
        min_priority = config.get('min_priority')
        if min_priority:
            priority_order = {'P0': 0, 'P1': 1, 'P2': 2, 'P3': 3}
            min_level = priority_order.get(min_priority, 3)
            testcases = [
                tc for tc in testcases
                if priority_order.get(tc.get('priority', 'P3'), 3) <= min_level
            ]

        # Limit number of cases
        max_cases = config.get('max_cases')
        if max_cases and len(testcases) > max_cases:
            testcases = testcases[:max_cases]

        return testcases
