"""
MySQL Client for AI Service - UNIFIED WITH BACKEND

⚠️ CRITICAL: Uses Backend Service's test_cases table for data consistency
This replaces MongoDB as the structured data storage layer in RAG architecture
"""
from typing import Optional, List, Dict, Any
from datetime import datetime
import logging
import json
import uuid

try:
    import pymysql
    from pymysql.cursors import DictCursor
    PYMYSQL_AVAILABLE = True
except ImportError:
    PYMYSQL_AVAILABLE = False
    logging.warning("pymysql not installed, MySQL client will be disabled")

from config import ai_config

logger = logging.getLogger(__name__)


class MySQLClient:
    """
    MySQL client for AI training data and test case storage
    
    ⚠️ CRITICAL: Uses Backend Service's test_cases table for consistency
    
    Backend's test_cases table structure (schema.sql):
    - id: CHAR(36) PRIMARY KEY (UUID)
    - title: VARCHAR(200) - Test case title
    - description: TEXT
    - steps: JSON - Test steps
    - expected_result: TEXT - Expected result (SINGULAR!)
    - priority: INTEGER (0-10)
    - type: VARCHAR(20) - FUNCTIONAL, PERFORMANCE, SECURITY
    - status: VARCHAR(20) - DRAFT, APPROVED, DEPRECATED
    - tags: JSON
    - related_requirement: VARCHAR(200)
    - created_at: TIMESTAMP
    - updated_at: TIMESTAMP
    - created_by: VARCHAR(100)
    """

    _instance: Optional['MySQLClient'] = None
    _connection: Optional[Any] = None

    def __new__(cls):
        if cls._instance is None:
            cls._instance = super().__new__(cls)
        return cls._instance

    def __init__(self):
        if not PYMYSQL_AVAILABLE:
            logger.warning("pymysql not available, using mock mode")
            return

        if self._connection is None:
            try:
                # Parse MySQL URI
                # Format: mysql://user:password@host:port/database
                import re
                match = re.match(
                    r'mysql://([^:]+):([^@]+)@([^:]+):(\d+)/(.+)',
                    ai_config.MYSQL_URI
                )
                
                if match:
                    user, password, host, port, database = match.groups()
                else:
                    # Fallback to default config
                    host = ai_config.MYSQL_HOST
                    port = ai_config.MYSQL_PORT
                    user = ai_config.MYSQL_USER
                    password = ai_config.MYSQL_PASSWORD
                    database = ai_config.MYSQL_DATABASE

                # Create connection (simple connection, can upgrade to pool later)
                self._connection = pymysql.connect(
                    host=host,
                    port=int(port),
                    user=user,
                    password=password,
                    database=database,
                    charset='utf8mb4',
                    cursorclass=DictCursor,
                    autocommit=False
                )
                
                logger.info(f"✅ Connected to MySQL: {database}@{host}:{port}")
                
                # Initialize AI-specific tables only
                self._init_tables()
                
            except Exception as e:
                logger.error(f"❌ Failed to connect to MySQL: {e}")
                self._connection = None

    def _init_tables(self):
        """
        Initialize AI-specific tables only
        
        ⚠️ IMPORTANT: We do NOT create test_cases table here!
        Backend Service manages the test_cases table.
        We only create AI-specific tables for history tracking.
        """
        if not self._connection:
            return

        try:
            with self._connection.cursor() as cursor:
                # Test case generation history
                cursor.execute("""
                    CREATE TABLE IF NOT EXISTS testcase_generation_history (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        request_id VARCHAR(100) UNIQUE NOT NULL,
                        module VARCHAR(100),
                        num_cases_requested INT,
                        num_cases_generated INT,
                        include_edge_cases BOOLEAN DEFAULT FALSE,
                        optimization_config JSON,
                        success BOOLEAN,
                        metadata JSON,
                        duplicate_count INT DEFAULT 0,
                        user_feedback JSON,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        INDEX idx_request_id (request_id),
                        INDEX idx_module (module),
                        INDEX idx_created_at (created_at)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """)

                # Recommendation history
                cursor.execute("""
                    CREATE TABLE IF NOT EXISTS recommendation_history (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        task_id VARCHAR(100),
                        recommendation JSON,
                        context JSON,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        INDEX idx_task_id (task_id),
                        INDEX idx_created_at (created_at)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """)

                # Company testing standards
                cursor.execute("""
                    CREATE TABLE IF NOT EXISTS company_standards (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        standard_name VARCHAR(100),
                        standard_data JSON,
                        active BOOLEAN DEFAULT TRUE,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        INDEX idx_active (active)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """)

                self._connection.commit()
                logger.info("✅ AI-specific MySQL tables initialized successfully")

        except Exception as e:
            logger.error(f"❌ Failed to initialize tables: {e}")
            self._connection.rollback()

    def _get_cursor(self):
        """Get a cursor for database operations"""
        if not self._connection:
            return None
        return self._connection.cursor()

    # ========================================================================
    # Field Mapping Methods - Convert between AI Service and Backend formats
    # ========================================================================

    def _map_priority_to_backend(self, priority: Any) -> int:
        """
        Map AI Service priority (P0-P3 or string) to Backend priority (0-10)
        
        Mapping:
        - P0 / 'P0' / 'highest' -> 10
        - P1 / 'P1' / 'high' -> 7
        - P2 / 'P2' / 'medium' -> 5
        - P3 / 'P3' / 'low' -> 3
        - numeric value -> use as-is (0-10)
        """
        if isinstance(priority, int):
            # Already numeric, clamp to 0-10
            return max(0, min(10, priority))
        
        priority_str = str(priority).upper()
        
        mapping = {
            'P0': 10,
            'P1': 7,
            'P2': 5,
            'P3': 3,
            'HIGHEST': 10,
            'HIGH': 7,
            'MEDIUM': 5,
            'LOW': 3
        }
        
        return mapping.get(priority_str, 5)  # Default to 5 (medium)

    def _map_priority_from_backend(self, priority: int) -> str:
        """
        Map Backend priority (0-10) to AI Service priority (P0-P3)
        
        Mapping:
        - 9-10 -> P0
        - 7-8 -> P1
        - 4-6 -> P2
        - 0-3 -> P3
        """
        if priority >= 9:
            return 'P0'
        elif priority >= 7:
            return 'P1'
        elif priority >= 4:
            return 'P2'
        else:
            return 'P3'

    def _map_type_to_backend(self, test_type: str) -> str:
        """
        Map AI Service test type (Chinese) to Backend type (English)
        
        Mapping:
        - 功能测试 -> FUNCTIONAL
        - 性能测试 -> PERFORMANCE
        - 安全测试 -> SECURITY
        - Already English -> use as-is
        """
        mapping = {
            '功能测试': 'FUNCTIONAL',
            '性能测试': 'PERFORMANCE',
            '安全测试': 'SECURITY',
            'FUNCTIONAL': 'FUNCTIONAL',
            'PERFORMANCE': 'PERFORMANCE',
            'SECURITY': 'SECURITY'
        }
        
        return mapping.get(test_type, 'FUNCTIONAL')  # Default to FUNCTIONAL

    def _map_type_from_backend(self, test_type: str) -> str:
        """
        Map Backend type (English) to AI Service type (Chinese)
        """
        mapping = {
            'FUNCTIONAL': '功能测试',
            'PERFORMANCE': '性能测试',
            'SECURITY': '安全测试'
        }
        
        return mapping.get(test_type, '功能测试')

    def _map_status_to_backend(self, status: str) -> str:
        """
        Map AI Service status to Backend status
        
        Mapping:
        - active -> APPROVED
        - draft -> DRAFT
        - deleted / deprecated -> DEPRECATED
        - Already Backend format -> use as-is
        """
        mapping = {
            'active': 'APPROVED',
            'draft': 'DRAFT',
            'deleted': 'DEPRECATED',
            'deprecated': 'DEPRECATED',
            'DRAFT': 'DRAFT',
            'APPROVED': 'APPROVED',
            'DEPRECATED': 'DEPRECATED'
        }
        
        return mapping.get(status, 'DRAFT')  # Default to DRAFT

    def _map_status_from_backend(self, status: str) -> str:
        """
        Map Backend status to AI Service status
        """
        mapping = {
            'DRAFT': 'draft',
            'APPROVED': 'active',
            'DEPRECATED': 'deprecated'
        }
        
        return mapping.get(status, 'draft')

    # ========================================================================
    # Test Case Operations - Using Backend's test_cases table
    # ========================================================================

    def get_similar_testcases(self, module: str, limit: int = 5) -> List[Dict[str, Any]]:
        """
        Get similar test cases for a module from Backend's test_cases table
        
        Uses tags field to match module (since Backend doesn't have module field)
        
        Note: This method returns cases by module match only.
        For semantic similarity search, use VectorDBClient.search_similar()
        combined with this method to get full data.
        """
        if not self._connection:
            return []

        try:
            with self._get_cursor() as cursor:
                # Search in tags JSON field for module
                cursor.execute("""
                    SELECT * FROM test_cases
                    WHERE JSON_CONTAINS(tags, JSON_QUOTE(%s))
                       OR description LIKE %s
                    ORDER BY created_at DESC
                    LIMIT %s
                """, (module, f'%{module}%', limit))
                
                results = cursor.fetchall()
                
                # Parse JSON fields and map to AI Service format
                for result in results:
                    if result.get('steps'):
                        result['steps'] = json.loads(result['steps']) if isinstance(result['steps'], str) else result['steps']
                    if result.get('tags'):
                        result['tags'] = json.loads(result['tags']) if isinstance(result['tags'], str) else result['tags']
                    
                    # Map fields for backward compatibility
                    result['name'] = result.get('title')
                    result['module'] = module
                    result['priority_level'] = self._map_priority_from_backend(result.get('priority', 5))
                    result['test_type'] = self._map_type_from_backend(result.get('type', 'FUNCTIONAL'))
                
                return results
                
        except Exception as e:
            logger.error(f"Failed to get similar testcases: {e}")
            return []

    def get_testcase_by_id(self, testcase_id: str) -> Optional[Dict[str, Any]]:
        """
        Get complete test case data by ID from Backend's test_cases table
        
        Args:
            testcase_id: Test case UUID
            
        Returns:
            Complete test case data
        """
        if not self._connection:
            return None

        try:
            with self._get_cursor() as cursor:
                cursor.execute("""
                    SELECT * FROM test_cases WHERE id = %s
                """, (testcase_id,))
                
                result = cursor.fetchone()
                
                if result:
                    # Parse JSON fields
                    if result.get('steps'):
                        result['steps'] = json.loads(result['steps']) if isinstance(result['steps'], str) else result['steps']
                    if result.get('tags'):
                        result['tags'] = json.loads(result['tags']) if isinstance(result['tags'], str) else result['tags']
                    
                    # Map fields for backward compatibility
                    result['name'] = result.get('title')
                    result['priority_level'] = self._map_priority_from_backend(result.get('priority', 5))
                    result['test_type'] = self._map_type_from_backend(result.get('type', 'FUNCTIONAL'))
                
                return result
                
        except Exception as e:
            logger.error(f"Failed to get testcase by id: {e}")
            return None

    def get_testcases_by_ids(self, testcase_ids: List[str]) -> List[Dict[str, Any]]:
        """
        Batch get test cases by IDs from Backend's test_cases table
        
        Args:
            testcase_ids: List of test case UUIDs
            
        Returns:
            List of complete test case data
        """
        if not self._connection or not testcase_ids:
            return []

        try:
            with self._get_cursor() as cursor:
                placeholders = ','.join(['%s'] * len(testcase_ids))
                query = f"""
                    SELECT * FROM test_cases
                    WHERE id IN ({placeholders})
                """
                
                cursor.execute(query, testcase_ids)
                results = cursor.fetchall()
                
                # Parse JSON fields and map to AI Service format
                for result in results:
                    if result.get('steps'):
                        result['steps'] = json.loads(result['steps']) if isinstance(result['steps'], str) else result['steps']
                    if result.get('tags'):
                        result['tags'] = json.loads(result['tags']) if isinstance(result['tags'], str) else result['tags']
                    
                    # Map fields for backward compatibility
                    result['name'] = result.get('title')
                    result['priority_level'] = self._map_priority_from_backend(result.get('priority', 5))
                    result['test_type'] = self._map_type_from_backend(result.get('type', 'FUNCTIONAL'))
                
                return results
                
        except Exception as e:
            logger.error(f"Failed to batch get testcases: {e}")
            return []

    def save_testcase(self, testcase_data: Dict[str, Any]) -> Optional[str]:
        """
        Save a new test case to Backend's test_cases table
        
        ⚠️ IMPORTANT: This inserts into Backend's test_cases table
        
        Args:
            testcase_data: Test case data (can be in AI Service format)
                - name or title: Test case name
                - description: Description
                - steps: Test steps (list or JSON)
                - expected_result: Expected result
                - priority: Priority (P0-P3 or 0-10)
                - type: Type (Chinese or English)
                - status: Status (active/draft or DRAFT/APPROVED)
                - tags: Tags (list or JSON)
                - module: Module name (will be added to tags)
                - related_requirement: Related requirement
            
        Returns:
            Inserted test case UUID
        """
        if not self._connection:
            logger.warning("MySQL not available, skipping save")
            return None

        try:
            # Generate UUID for test case
            testcase_id = str(uuid.uuid4())
            
            # Extract and map fields
            title = testcase_data.get('name') or testcase_data.get('title', 'Untitled Test Case')
            description = testcase_data.get('description', '')
            
            # Handle steps - ensure it's JSON
            steps = testcase_data.get('steps', [])
            if isinstance(steps, list):
                steps_json = json.dumps(steps, ensure_ascii=False)
            elif isinstance(steps, str):
                try:
                    json.loads(steps)  # Validate JSON
                    steps_json = steps
                except:
                    steps_json = json.dumps([steps], ensure_ascii=False)
            else:
                steps_json = json.dumps([str(steps)], ensure_ascii=False)
            
            expected_result = testcase_data.get('expected_result', '')
            
            # Map priority
            priority = self._map_priority_to_backend(testcase_data.get('priority', 'P2'))
            
            # Map type
            test_type = self._map_type_to_backend(testcase_data.get('type', '功能测试'))
            
            # Map status
            status = self._map_status_to_backend(testcase_data.get('status', 'draft'))
            
            # Handle tags - add module to tags if present
            tags = testcase_data.get('tags', [])
            if not isinstance(tags, list):
                tags = [tags] if tags else []
            
            # Add module to tags if provided
            module = testcase_data.get('module')
            if module and module not in tags:
                tags.append(module)
            
            tags_json = json.dumps(tags, ensure_ascii=False)
            
            related_requirement = testcase_data.get('related_requirement', '')
            created_by = testcase_data.get('created_by', 'ai-service')
            
            with self._get_cursor() as cursor:
                # Insert into Backend's test_cases table
                cursor.execute("""
                    INSERT INTO test_cases
                    (id, title, description, steps, expected_result, priority,
                     type, status, tags, related_requirement, created_by)
                    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                """, (
                    testcase_id,
                    title,
                    description,
                    steps_json,
                    expected_result,
                    priority,
                    test_type,
                    status,
                    tags_json,
                    related_requirement,
                    created_by
                ))
                self._connection.commit()
                
                logger.info(f"✅ Saved test case to Backend's test_cases table: {testcase_id}")
                return testcase_id
                
        except Exception as e:
            logger.error(f"❌ Failed to save testcase: {e}")
            self._connection.rollback()
            return None

    def update_testcase(
        self,
        testcase_id: str,
        update_data: Dict[str, Any]
    ) -> bool:
        """
        Update an existing test case in Backend's test_cases table
        
        Args:
            testcase_id: Test case UUID
            update_data: Data to update
            
        Returns:
            Success status
        """
        if not self._connection:
            return False

        try:
            # Build SET clause dynamically
            set_parts = []
            params = []
            
            # Map fields
            field_mapping = {
                'name': 'title',
                'title': 'title',
                'description': 'description',
                'steps': 'steps',
                'expected_result': 'expected_result',
                'priority': 'priority',
                'type': 'type',
                'status': 'status',
                'tags': 'tags',
                'related_requirement': 'related_requirement'
            }
            
            for key, value in update_data.items():
                if key in ['id', 'created_at', 'created_by']:  # Skip these fields
                    continue
                
                backend_field = field_mapping.get(key)
                if not backend_field:
                    continue
                
                set_parts.append(f"{backend_field} = %s")
                
                # Convert and validate values
                if backend_field == 'priority':
                    params.append(self._map_priority_to_backend(value))
                elif backend_field == 'type':
                    params.append(self._map_type_to_backend(value))
                elif backend_field == 'status':
                    params.append(self._map_status_to_backend(value))
                elif backend_field in ['steps', 'tags']:
                    if isinstance(value, (list, dict)):
                        params.append(json.dumps(value, ensure_ascii=False))
                    else:
                        params.append(value)
                else:
                    params.append(value)
            
            if not set_parts:
                return False
            
            # Add updated_at
            set_parts.append("updated_at = NOW()")
            
            # Add testcase_id to params
            params.append(testcase_id)
            
            with self._get_cursor() as cursor:
                query = f"""
                    UPDATE test_cases
                    SET {', '.join(set_parts)}
                    WHERE id = %s
                """
                
                cursor.execute(query, params)
                self._connection.commit()
                
                return cursor.rowcount > 0
                
        except Exception as e:
            logger.error(f"Failed to update testcase: {e}")
            self._connection.rollback()
            return False

    # ========================================================================
    # AI Service History Operations
    # ========================================================================

    def save_recommendation_history(self, data: Dict[str, Any]) -> Optional[int]:
        """Save recommendation history"""
        if not self._connection:
            logger.warning("MySQL not available, skipping save")
            return None

        try:
            with self._get_cursor() as cursor:
                cursor.execute("""
                    INSERT INTO recommendation_history (task_id, recommendation, context)
                    VALUES (%s, %s, %s)
                """, (
                    data.get('task_id'),
                    json.dumps(data.get('recommendation', {})),
                    json.dumps(data.get('context', {}))
                ))
                self._connection.commit()
                return cursor.lastrowid
        except Exception as e:
            logger.error(f"Failed to save recommendation history: {e}")
            self._connection.rollback()
            return None

    def save_testcase_generation_history(self, data: Dict[str, Any]) -> Optional[int]:
        """Save test case generation history"""
        if not self._connection:
            logger.warning("MySQL not available, skipping save")
            return None

        try:
            with self._get_cursor() as cursor:
                cursor.execute("""
                    INSERT INTO testcase_generation_history 
                    (request_id, module, num_cases_requested, num_cases_generated,
                     include_edge_cases, optimization_config, success, metadata, duplicate_count)
                    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
                """, (
                    data.get('requestId'),
                    data.get('module'),
                    data.get('num_cases_requested'),
                    data.get('num_cases_generated'),
                    data.get('include_edge_cases', False),
                    json.dumps(data.get('optimization_config', {})),
                    data.get('success', False),
                    json.dumps(data.get('metadata', {})),
                    data.get('duplicate_count', 0)
                ))
                self._connection.commit()
                return cursor.lastrowid
        except Exception as e:
            logger.error(f"Failed to save generation history: {e}")
            self._connection.rollback()
            return None

    def get_company_standards(self) -> Dict[str, Any]:
        """Get company testing standards"""
        if not self._connection:
            return self._get_default_standards()

        try:
            with self._get_cursor() as cursor:
                cursor.execute("""
                    SELECT standard_data FROM company_standards
                    WHERE active = TRUE
                    ORDER BY created_at DESC
                    LIMIT 1
                """)
                
                result = cursor.fetchone()
                
                if result and result.get('standard_data'):
                    standards = result['standard_data']
                    if isinstance(standards, str):
                        standards = json.loads(standards)
                    return standards
                
                return self._get_default_standards()
                
        except Exception as e:
            logger.error(f"Failed to get company standards: {e}")
            return self._get_default_standards()

    def _get_default_standards(self) -> Dict[str, Any]:
        """Return default testing standards"""
        return {
            'naming_convention': 'descriptive_action_expected',
            'priority_levels': ['P0', 'P1', 'P2', 'P3'],
            'test_types': ['功能测试', '性能测试', '安全测试', '兼容性测试'],
            'required_fields': ['name', 'steps', 'expected_result', 'priority']
        }

    def update_user_feedback(
        self,
        request_id: str,
        feedback: Dict[str, Any]
    ) -> bool:
        """Update user feedback for a generation request"""
        if not self._connection:
            return False

        try:
            with self._get_cursor() as cursor:
                cursor.execute("""
                    UPDATE testcase_generation_history
                    SET user_feedback = %s
                    WHERE request_id = %s
                """, (
                    json.dumps(feedback, ensure_ascii=False),
                    request_id
                ))
                self._connection.commit()
                return cursor.rowcount > 0
                
        except Exception as e:
            logger.error(f"Failed to update user feedback: {e}")
            self._connection.rollback()
            return False

    def close(self):
        """Close MySQL connection"""
        if self._connection:
            self._connection.close()
            self._connection = None
            logger.info("MySQL connection closed")


# Singleton instance
mysql_client = MySQLClient()

