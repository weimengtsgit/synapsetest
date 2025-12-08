"""
MySQL Client for AI Service

Integrates with Backend Service's MySQL database
Uses the SAME tables as Backend Service for data consistency
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
    MySQL client for AI Service
    
    ⚠️ IMPORTANT: This client uses the SAME tables as Backend Service
    
    Table: test_cases (shared with Backend)
    - id: CHAR(36) PRIMARY KEY (UUID)
    - title: VARCHAR(200) - 测试用例标题
    - description: TEXT - 描述
    - steps: JSON - 测试步骤
    - expected_result: TEXT - 预期结果
    - priority: INTEGER (0-10) - 优先级
    - type: VARCHAR(20) - 类型 (FUNCTIONAL, PERFORMANCE, SECURITY)
    - status: VARCHAR(20) - 状态 (DRAFT, APPROVED, DEPRECATED)
    - tags: JSON - 标签
    - related_requirement: VARCHAR(200) - 关联需求
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
                import re
                match = re.match(
                    r'mysql://([^:]+):([^@]+)@([^:]+):(\d+)/(.+)',
                    ai_config.MYSQL_URI
                )
                
                if match:
                    user, password, host, port, database = match.groups()
                else:
                    host = ai_config.MYSQL_HOST
                    port = ai_config.MYSQL_PORT
                    user = ai_config.MYSQL_USER
                    password = ai_config.MYSQL_PASSWORD
                    database = ai_config.MYSQL_DATABASE

                # Create connection
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
                
                logger.info(f"Connected to MySQL: {database}@{host}:{port}")
                
                # Initialize AI-specific tables (not test_cases, it's managed by Backend)
                self._init_ai_tables()
                
            except Exception as e:
                logger.error(f"Failed to connect to MySQL: {e}")
                self._connection = None

    def _init_ai_tables(self):
        """
        Initialize AI-specific tables only
        
        Note: test_cases table is managed by Backend Service
        We only create AI-specific auxiliary tables
        """
        if not self._connection:
            return

        try:
            with self._connection.cursor() as cursor:
                # Test case generation history (AI-specific)
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

                # Recommendation history (AI-specific)
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

                # Company testing standards (AI-specific)
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
                logger.info("AI-specific MySQL tables initialized successfully")

        except Exception as e:
            logger.error(f"Failed to initialize AI tables: {e}")
            self._connection.rollback()

    def _get_cursor(self):
        """Get a cursor for database operations"""
        if not self._connection:
            return None
        return self._connection.cursor()

    # ==================== Test Cases Operations (Backend's test_cases table) ====================
    
    def save_testcase(self, testcase_data: Dict[str, Any]) -> Optional[str]:
        """
        Save a new test case to Backend's test_cases table
        
        Args:
            testcase_data: Test case data with fields:
                - name/title: Test case title
                - description: Description
                - steps: List of test steps
                - expected_result: Expected result
                - priority: Priority (0-10 or P0-P3, will be converted)
                - type: Test type
                - module: Module name (stored in related_requirement)
                - tags: List of tags
                
        Returns:
            Inserted test case ID (UUID)
        """
        if not self._connection:
            logger.warning("MySQL not available, skipping save")
            return None

        try:
            with self._get_cursor() as cursor:
                # Generate UUID for id
                testcase_id = str(uuid.uuid4())
                
                # Map AI Service fields to Backend fields
                title = testcase_data.get('name') or testcase_data.get('title', 'Unnamed Test Case')
                description = testcase_data.get('description', '')
                steps = testcase_data.get('steps', [])
                expected_result = testcase_data.get('expected_result', '')
                
                # Convert priority: P0-P3 → 0-10
                priority_value = self._convert_priority_to_int(testcase_data.get('priority', 'P2'))
                
                # Convert type: 功能测试 → FUNCTIONAL
                type_value = self._convert_type_to_enum(testcase_data.get('type', '功能测试'))
                
                # Status: active/deleted → DRAFT/APPROVED/DEPRECATED
                status_value = 'DRAFT'  # AI generated cases start as DRAFT
                
                tags = testcase_data.get('tags', [])
                
                # Use module as related_requirement if provided
                related_requirement = testcase_data.get('module') or testcase_data.get('related_requirement', '')
                
                # Created by AI
                created_by = testcase_data.get('created_by', 'AI-Service')
                
                # Insert into Backend's test_cases table
                cursor.execute("""
                    INSERT INTO test_cases(
                        id, title, description, steps, expected_result,
                        priority, type, status, tags, related_requirement,
                        created_at, updated_at, created_by
                    ) VALUES (
                        %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s
                    )
                """, (
                    testcase_id,
                    title,
                    description,
                    json.dumps(steps, ensure_ascii=False),
                    expected_result,
                    priority_value,
                    type_value,
                    status_value,
                    json.dumps(tags, ensure_ascii=False),
                    related_requirement,
                    datetime.now(),
                    datetime.now(),
                    created_by
                ))
                
                self._connection.commit()
                logger.info(f"Saved test case to Backend's test_cases table: {testcase_id}")
                return testcase_id
                
        except Exception as e:
            logger.error(f"Failed to save testcase: {e}")
            self._connection.rollback()
            return None

    def get_testcase_by_id(self, testcase_id: str) -> Optional[Dict[str, Any]]:
        """
        Get complete test case data by ID from Backend's test_cases table
        
        Args:
            testcase_id: Test case ID (UUID)
            
        Returns:
            Complete test case data with Backend field names
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
                    if result.get('steps') and isinstance(result['steps'], str):
                        result['steps'] = json.loads(result['steps'])
                    if result.get('tags') and isinstance(result['tags'], str):
                        result['tags'] = json.loads(result['tags'])
                    
                    # Map Backend fields to AI Service expected format
                    result['name'] = result.get('title')  # Add alias
                    result['module'] = result.get('related_requirement')  # Add alias
                
                return result
                
        except Exception as e:
            logger.error(f"Failed to get testcase by id: {e}")
            return None

    def get_testcases_by_ids(self, testcase_ids: List[str]) -> List[Dict[str, Any]]:
        """
        Batch get test cases by IDs from Backend's test_cases table
        
        Args:
            testcase_ids: List of test case IDs (UUIDs)
            
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
                
                # Parse JSON fields and add aliases
                for result in results:
                    if result.get('steps') and isinstance(result['steps'], str):
                        result['steps'] = json.loads(result['steps'])
                    if result.get('tags') and isinstance(result['tags'], str):
                        result['tags'] = json.loads(result['tags'])
                    
                    # Add aliases for AI Service compatibility
                    result['name'] = result.get('title')
                    result['module'] = result.get('related_requirement')
                
                return results
                
        except Exception as e:
            logger.error(f"Failed to batch get testcases: {e}")
            return []

    def get_similar_testcases(self, module: str, limit: int = 5) -> List[Dict[str, Any]]:
        """
        Get similar test cases by module from Backend's test_cases table
        
        Note: For semantic similarity search, use Milvus vector search
        This method does simple filtering by related_requirement (module)
        
        Args:
            module: Module name
            limit: Max results
            
        Returns:
            List of test cases
        """
        if not self._connection:
            return []

        try:
            with self._get_cursor() as cursor:
                cursor.execute("""
                    SELECT * FROM test_cases
                    WHERE related_requirement = %s
                    AND status IN ('APPROVED', 'DRAFT')
                    ORDER BY created_at DESC
                    LIMIT %s
                """, (module, limit))
                
                results = cursor.fetchall()
                
                # Parse JSON fields and add aliases
                for result in results:
                    if result.get('steps') and isinstance(result['steps'], str):
                        result['steps'] = json.loads(result['steps'])
                    if result.get('tags') and isinstance(result['tags'], str):
                        result['tags'] = json.loads(result['tags'])
                    
                    # Add aliases
                    result['name'] = result.get('title')
                    result['module'] = result.get('related_requirement')
                
                return results
                
        except Exception as e:
            logger.error(f"Failed to get similar testcases: {e}")
            return []

    def update_testcase(
        self,
        testcase_id: str,
        update_data: Dict[str, Any]
    ) -> bool:
        """
        Update an existing test case in Backend's test_cases table
        
        Args:
            testcase_id: Test case ID (UUID)
            update_data: Data to update
            
        Returns:
            Success status
        """
        if not self._connection:
            return False

        try:
            # Map AI Service fields to Backend fields
            backend_update = {}
            
            if 'name' in update_data:
                backend_update['title'] = update_data['name']
            if 'title' in update_data:
                backend_update['title'] = update_data['title']
            if 'description' in update_data:
                backend_update['description'] = update_data['description']
            if 'steps' in update_data:
                backend_update['steps'] = update_data['steps']
            if 'expected_result' in update_data:
                backend_update['expected_result'] = update_data['expected_result']
            if 'priority' in update_data:
                backend_update['priority'] = self._convert_priority_to_int(update_data['priority'])
            if 'type' in update_data:
                backend_update['type'] = self._convert_type_to_enum(update_data['type'])
            if 'status' in update_data:
                backend_update['status'] = self._convert_status_to_enum(update_data['status'])
            if 'tags' in update_data:
                backend_update['tags'] = update_data['tags']
            if 'module' in update_data:
                backend_update['related_requirement'] = update_data['module']
            if 'related_requirement' in update_data:
                backend_update['related_requirement'] = update_data['related_requirement']
            
            if not backend_update:
                return False
            
            # Build SET clause
            set_parts = []
            params = []
            
            json_fields = ['steps', 'tags']
            
            for key, value in backend_update.items():
                set_parts.append(f"{key} = %s")
                
                # Convert lists/dicts to JSON strings
                if key in json_fields and isinstance(value, (list, dict)):
                    params.append(json.dumps(value, ensure_ascii=False))
                else:
                    params.append(value)
            
            # Add updated_at
            set_parts.append("updated_at = %s")
            params.append(datetime.now())
            
            # Add testcase_id
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

    def get_company_standards(self) -> Dict[str, Any]:
        """Get company testing standards from AI-specific table"""
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
            'test_types': ['FUNCTIONAL', 'PERFORMANCE', 'SECURITY'],
            'required_fields': ['title', 'steps', 'expected_result', 'priority']
        }

    def save_testcase_generation_history(self, data: Dict[str, Any]) -> Optional[int]:
        """Save test case generation history to AI-specific table"""
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

    def save_recommendation_history(self, data: Dict[str, Any]) -> Optional[int]:
        """Save recommendation history to AI-specific table"""
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

    # ==================== Helper Methods ====================
    
    def _convert_priority_to_int(self, priority: Any) -> int:
        """
        Convert AI Service priority (P0-P3) to Backend priority (0-10)
        
        Mapping:
        P0 → 10 (Highest)
        P1 → 7
        P2 → 5 (Default)
        P3 → 3 (Low)
        """
        if isinstance(priority, int):
            return max(0, min(10, priority))
        
        if isinstance(priority, str):
            priority_map = {
                'P0': 10,
                'P1': 7,
                'P2': 5,
                'P3': 3
            }
            return priority_map.get(priority.upper(), 5)
        
        return 5  # Default

    def _convert_priority_to_enum(self, priority: int) -> str:
        """
        Convert Backend priority (0-10) to AI Service priority (P0-P3)
        
        Mapping:
        10-8 → P0
        7-6 → P1
        5-4 → P2
        3-0 → P3
        """
        if priority >= 8:
            return 'P0'
        elif priority >= 6:
            return 'P1'
        elif priority >= 4:
            return 'P2'
        else:
            return 'P3'

    def _convert_type_to_enum(self, test_type: str) -> str:
        """
        Convert AI Service type to Backend type enum
        
        Mapping:
        功能测试 → FUNCTIONAL
        性能测试 → PERFORMANCE
        安全测试 → SECURITY
        """
        type_map = {
            '功能测试': 'FUNCTIONAL',
            '性能测试': 'PERFORMANCE',
            '安全测试': 'SECURITY',
            '兼容性测试': 'FUNCTIONAL',  # Map to FUNCTIONAL
            'FUNCTIONAL': 'FUNCTIONAL',
            'PERFORMANCE': 'PERFORMANCE',
            'SECURITY': 'SECURITY'
        }
        return type_map.get(test_type, 'FUNCTIONAL')

    def _convert_type_to_chinese(self, test_type: str) -> str:
        """
        Convert Backend type enum to AI Service Chinese type
        
        Mapping:
        FUNCTIONAL → 功能测试
        PERFORMANCE → 性能测试
        SECURITY → 安全测试
        """
        type_map = {
            'FUNCTIONAL': '功能测试',
            'PERFORMANCE': '性能测试',
            'SECURITY': '安全测试'
        }
        return type_map.get(test_type, '功能测试')

    def _convert_status_to_enum(self, status: str) -> str:
        """
        Convert AI Service status to Backend status enum
        
        Mapping:
        active → DRAFT (AI generated, pending review)
        approved → APPROVED
        deleted → DEPRECATED
        """
        status_map = {
            'active': 'DRAFT',
            'approved': 'APPROVED',
            'deleted': 'DEPRECATED',
            'DRAFT': 'DRAFT',
            'APPROVED': 'APPROVED',
            'DEPRECATED': 'DEPRECATED'
        }
        return status_map.get(status, 'DRAFT')

    def close(self):
        """Close MySQL connection"""
        if self._connection:
            self._connection.close()
            self._connection = None
            logger.info("MySQL connection closed")


# Singleton instance
mysql_client = MySQLClient()

