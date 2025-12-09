"""
MySQL Client for AI Service

Replaces MongoDB as the structured data storage layer in RAG architecture
Integrates with Backend Service's MySQL database
"""
from typing import Optional, List, Dict, Any
from datetime import datetime
import logging
import json

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
    
    Integrates with Backend Service's MySQL database
    Provides the same interface as MongoDB client for easy migration
    """

    _instance: Optional['MySQLClient'] = None
    _connection_pool: Optional[Any] = None

    def __new__(cls):
        if cls._instance is None:
            cls._instance = super().__new__(cls)
        return cls._instance

    def __init__(self):
        if not PYMYSQL_AVAILABLE:
            logger.warning("pymysql not available, using mock mode")
            return

        if self._connection_pool is None:
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
                
                logger.info(f"Connected to MySQL: {database}@{host}:{port}")
                
                # Initialize tables if needed
                self._init_tables()
                
            except Exception as e:
                logger.error(f"Failed to connect to MySQL: {e}")
                self._connection = None

    def _init_tables(self):
        """
        Initialize required tables for AI Service
        
        Note: Main test case tables should already exist in Backend Service
        This creates additional AI-specific tables
        """
        if not self._connection:
            return

        try:
            with self._connection.cursor() as cursor:
                # Historical test cases table (for RAG reference)
                # This stores high-quality historical cases for training
                cursor.execute("""
                    CREATE TABLE IF NOT EXISTS historical_testcases (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        testcase_id VARCHAR(100) UNIQUE,
                        name VARCHAR(500) NOT NULL,
                        module VARCHAR(100),
                        priority ENUM('P0', 'P1', 'P2', 'P3') DEFAULT 'P2',
                        type VARCHAR(50) DEFAULT '功能测试',
                        description TEXT,
                        steps JSON,
                        preconditions JSON,
                        expected_result TEXT,
                        tags JSON,
                        status VARCHAR(20) DEFAULT 'active',
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        INDEX idx_module (module),
                        INDEX idx_priority (priority),
                        INDEX idx_status (status),
                        INDEX idx_created_at (created_at)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """)

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
                logger.info("MySQL tables initialized successfully")

        except Exception as e:
            logger.error(f"Failed to initialize tables: {e}")
            self._connection.rollback()

    def _get_cursor(self):
        """Get a cursor for database operations"""
        if not self._connection:
            return None
        return self._connection.cursor()

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

    def get_similar_testcases(self, module: str, limit: int = 5) -> List[Dict[str, Any]]:
        """
        Get similar historical test cases for a module
        
        Note: This method returns cases by module match only.
        For semantic similarity search, use VectorDBClient.search_similar()
        combined with this method to get full data.
        """
        if not self._connection:
            return []

        try:
            with self._get_cursor() as cursor:
                cursor.execute("""
                    SELECT * FROM historical_testcases
                    WHERE module = %s AND status = 'active'
                    ORDER BY created_at DESC
                    LIMIT %s
                """, (module, limit))
                
                results = cursor.fetchall()
                
                # Parse JSON fields
                for result in results:
                    if result.get('steps'):
                        result['steps'] = json.loads(result['steps']) if isinstance(result['steps'], str) else result['steps']
                    if result.get('preconditions'):
                        result['preconditions'] = json.loads(result['preconditions']) if isinstance(result['preconditions'], str) else result['preconditions']
                    if result.get('tags'):
                        result['tags'] = json.loads(result['tags']) if isinstance(result['tags'], str) else result['tags']
                
                return results
                
        except Exception as e:
            logger.error(f"Failed to get similar testcases: {e}")
            return []

    def get_testcase_by_id(self, testcase_id: str) -> Optional[Dict[str, Any]]:
        """
        Get complete test case data by ID
        
        Args:
            testcase_id: Test case ID
            
        Returns:
            Complete test case data
        """
        if not self._connection:
            return None

        try:
            with self._get_cursor() as cursor:
                # Try by primary key first
                if testcase_id.isdigit():
                    cursor.execute("""
                        SELECT * FROM historical_testcases WHERE id = %s
                    """, (int(testcase_id),))
                else:
                    cursor.execute("""
                        SELECT * FROM historical_testcases WHERE testcase_id = %s
                    """, (testcase_id,))
                
                result = cursor.fetchone()
                
                if result:
                    # Parse JSON fields
                    if result.get('steps'):
                        result['steps'] = json.loads(result['steps']) if isinstance(result['steps'], str) else result['steps']
                    if result.get('preconditions'):
                        result['preconditions'] = json.loads(result['preconditions']) if isinstance(result['preconditions'], str) else result['preconditions']
                    if result.get('tags'):
                        result['tags'] = json.loads(result['tags']) if isinstance(result['tags'], str) else result['tags']
                
                return result
                
        except Exception as e:
            logger.error(f"Failed to get testcase by id: {e}")
            return None

    def get_testcases_by_ids(self, testcase_ids: List[str]) -> List[Dict[str, Any]]:
        """
        Batch get test cases by IDs
        
        Args:
            testcase_ids: List of test case IDs
            
        Returns:
            List of complete test case data
        """
        if not self._connection or not testcase_ids:
            return []

        try:
            with self._get_cursor() as cursor:
                # Separate numeric and string IDs
                numeric_ids = [int(tid) for tid in testcase_ids if tid.isdigit()]
                string_ids = [tid for tid in testcase_ids if not tid.isdigit()]
                
                conditions = []
                params = []
                
                if numeric_ids:
                    placeholders = ','.join(['%s'] * len(numeric_ids))
                    conditions.append(f"id IN ({placeholders})")
                    params.extend(numeric_ids)
                
                if string_ids:
                    placeholders = ','.join(['%s'] * len(string_ids))
                    conditions.append(f"testcase_id IN ({placeholders})")
                    params.extend(string_ids)
                
                if not conditions:
                    return []
                
                query = f"""
                    SELECT * FROM historical_testcases
                    WHERE {' OR '.join(conditions)}
                """
                
                cursor.execute(query, params)
                results = cursor.fetchall()
                
                # Parse JSON fields
                for result in results:
                    if result.get('steps'):
                        result['steps'] = json.loads(result['steps']) if isinstance(result['steps'], str) else result['steps']
                    if result.get('preconditions'):
                        result['preconditions'] = json.loads(result['preconditions']) if isinstance(result['preconditions'], str) else result['preconditions']
                    if result.get('tags'):
                        result['tags'] = json.loads(result['tags']) if isinstance(result['tags'], str) else result['tags']
                
                return results
                
        except Exception as e:
            logger.error(f"Failed to batch get testcases: {e}")
            return []

    def save_testcase(self, testcase_data: Dict[str, Any]) -> Optional[str]:
        """
        Save a new test case to MySQL
        
        Args:
            testcase_data: Test case data
            
        Returns:
            Inserted test case ID
        """
        if not self._connection:
            logger.warning("MySQL not available, skipping save")
            return None

        try:
            with self._get_cursor() as cursor:
                cursor.execute("""
                    INSERT INTO historical_testcases
                    (testcase_id, name, module, priority, type, description,
                     steps, preconditions, expected_result, tags, status)
                    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                """, (
                    testcase_data.get('testcase_id'),
                    testcase_data.get('name'),
                    testcase_data.get('module'),
                    testcase_data.get('priority', 'P2'),
                    testcase_data.get('type', '功能测试'),
                    testcase_data.get('description'),
                    json.dumps(testcase_data.get('steps', []), ensure_ascii=False),
                    json.dumps(testcase_data.get('preconditions', []), ensure_ascii=False),
                    testcase_data.get('expected_result'),
                    json.dumps(testcase_data.get('tags', []), ensure_ascii=False),
                    testcase_data.get('status', 'active')
                ))
                self._connection.commit()
                return str(cursor.lastrowid)
                
        except Exception as e:
            logger.error(f"Failed to save testcase: {e}")
            self._connection.rollback()
            return None

    def update_testcase(
        self,
        testcase_id: str,
        update_data: Dict[str, Any]
    ) -> bool:
        """
        Update an existing test case
        
        Args:
            testcase_id: Test case ID
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
            
            json_fields = ['steps', 'preconditions', 'tags']
            
            for key, value in update_data.items():
                if key in ['id', 'created_at']:  # Skip these fields
                    continue
                
                set_parts.append(f"{key} = %s")
                
                # Convert lists/dicts to JSON strings
                if key in json_fields and (isinstance(value, (list, dict))):
                    params.append(json.dumps(value, ensure_ascii=False))
                else:
                    params.append(value)
            
            if not set_parts:
                return False
            
            # Add testcase_id to params
            params.append(testcase_id)
            
            with self._get_cursor() as cursor:
                # Try numeric ID first
                if testcase_id.isdigit():
                    query = f"""
                        UPDATE historical_testcases
                        SET {', '.join(set_parts)}
                        WHERE id = %s
                    """
                else:
                    query = f"""
                        UPDATE historical_testcases
                        SET {', '.join(set_parts)}
                        WHERE testcase_id = %s
                    """
                
                cursor.execute(query, params)
                self._connection.commit()
                
                return cursor.rowcount > 0
                
        except Exception as e:
            logger.error(f"Failed to update testcase: {e}")
            self._connection.rollback()
            return False

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

