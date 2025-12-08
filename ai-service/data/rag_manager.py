"""
RAG Manager

Coordinates between Vector DB (Qdrant/Milvus) and MySQL for semantic search
"""
from typing import List, Dict, Any, Optional
import logging

from data.vector_db_factory import vector_db_client  # Automatically uses Qdrant or Milvus
from data.db_factory import db_client  # Uses MySQL for structured data

logger = logging.getLogger(__name__)


class RAGManager:
    """
    RAG Manager that coordinates vector search and detailed data retrieval
    
    Architecture:
    1. Vector DB (Qdrant/Milvus): Fast semantic similarity search
    2. Structured DB (MySQL): Complete structured data storage
    
    Workflow:
    - Search: Vector DB (get IDs) → Structured DB (get full data)
    - Insert: Structured DB (save data) → Vector DB (index vectors)
    - Update: Structured DB (update data) → Vector DB (re-index)
    - Delete: Both databases
    """
    
    def __init__(self):
        self.vector_db = vector_db_client
        self.structured_db = db_client  # MySQL client
    
    def search_similar_testcases(
        self,
        query_text: str,
        top_k: int = 5,
        module_filter: Optional[str] = None
    ) -> List[Dict[str, Any]]:
        """
        Search for similar test cases using semantic similarity
        
        This is the main RAG retrieval method that:
        1. Uses vector DB for semantic search (fast)
        2. Fetches complete data from MySQL (detailed)
        
        Args:
            query_text: Query text (requirement or description)
            top_k: Number of results to return
            module_filter: Optional module name filter
            
        Returns:
            List of complete test case data with similarity scores
        """
        logger.info(f"RAG search: query='{query_text[:50]}...', top_k={top_k}, module={module_filter}")
        
        # Step 1: Vector search to get candidate IDs
        similar_results = self.vector_db.search_similar(
            query_text=query_text,
            top_k=top_k * 2,  # Get more candidates for filtering
            module_filter=module_filter
        )
        
        if not similar_results:
            logger.warning("No similar test cases found in vector DB")
            return []
        
        # Step 2: Extract IDs and scores
        testcase_ids = [r['testcase_id'] for r in similar_results]
        scores_map = {r['testcase_id']: r['similarity_score'] for r in similar_results}
        
        logger.info(f"Vector DB returned {len(testcase_ids)} candidates")
        
        # Step 3: Fetch complete data from structured DB
        complete_testcases = self.structured_db.get_testcases_by_ids(testcase_ids)
        
        if not complete_testcases:
            logger.warning("No test cases found in structured DB for vector DB results")
            return []
        
        # Step 4: Merge similarity scores with complete data
        for tc in complete_testcases:
            tc_id = str(tc.get('_id', tc.get('testcase_id', '')))
            tc['similarity_score'] = scores_map.get(tc_id, 0.0)
        
        # Step 5: Sort by similarity and limit results
        complete_testcases.sort(key=lambda x: x.get('similarity_score', 0), reverse=True)
        result = complete_testcases[:top_k]
        
        logger.info(f"RAG search completed: returned {len(result)} test cases")
        return result
    
    def add_testcase(self, testcase_data: Dict[str, Any]) -> Optional[str]:
        """
        Add a test case to both databases
        
        Args:
            testcase_data: Complete test case data
            
        Returns:
            Test case ID if successful
        """
        logger.info(f"Adding test case: {testcase_data.get('name', 'unnamed')}")
        
        # Step 1: Save to structured DB (MySQL - source of truth)
        testcase_id = self.structured_db.save_testcase(testcase_data)
        
        if not testcase_id:
            logger.error("Failed to save test case to structured database")
            return None
        
        # Step 2: Index in vector DB
        testcase_data['testcase_id'] = testcase_id
        vector_success = self.vector_db.add_testcase(testcase_id, testcase_data)
        
        if not vector_success:
            logger.warning(f"Failed to index test case in vector DB: {testcase_id}")
            # Structured DB save succeeded, so we still return the ID
        
        logger.info(f"Successfully added test case: {testcase_id}")
        return testcase_id
    
    def add_testcases_batch(self, testcases: List[Dict[str, Any]]) -> Dict[str, Any]:
        """
        Batch add test cases to both databases
        
        Args:
            testcases: List of test case data
            
        Returns:
            Summary of operation
        """
        logger.info(f"Batch adding {len(testcases)} test cases")
        
        mysql_success = 0
        vector_success = 0
        testcase_ids = []
        
        for tc in testcases:
            tc_id = self.add_testcase(tc)
            if tc_id:
                testcase_ids.append(tc_id)
                mysql_success += 1
                # Vector success is logged internally
        
        # Get vector DB stats
        vector_stats = self.vector_db.get_collection_stats()
        if vector_stats.get('available'):
            vector_success = vector_stats.get('total_testcases', 0)
        
        result = {
            'total_requested': len(testcases),
            'mysql_success': mysql_success,
            'vector_db_total': vector_success,
            'testcase_ids': testcase_ids
        }
        
        logger.info(f"Batch add completed: {result}")
        return result
    
    def update_testcase(
        self,
        testcase_id: str,
        update_data: Dict[str, Any]
    ) -> bool:
        """
        Update a test case in both databases
        
        Args:
            testcase_id: Test case ID
            update_data: Data to update
            
        Returns:
            Success status
        """
        logger.info(f"Updating test case: {testcase_id}")
        
        # Step 1: Update structured DB
        structured_success = self.structured_db.update_testcase(testcase_id, update_data)
        
        if not structured_success:
            logger.error(f"Failed to update test case in structured DB: {testcase_id}")
            return False
        
        # Step 2: Get updated data
        updated_testcase = self.structured_db.get_testcase_by_id(testcase_id)
        
        if not updated_testcase:
            logger.error(f"Failed to fetch updated test case: {testcase_id}")
            return False
        
        # Step 3: Re-index in vector DB
        vector_success = self.vector_db.update_testcase(testcase_id, updated_testcase)
        
        if not vector_success:
            logger.warning(f"Failed to update test case in vector DB: {testcase_id}")
            # Structured DB update succeeded, so we still return True
        
        logger.info(f"Successfully updated test case: {testcase_id}")
        return True
    
    def delete_testcase(self, testcase_id: str) -> bool:
        """
        Delete a test case from both databases
        
        Args:
            testcase_id: Test case ID
            
        Returns:
            Success status
        """
        logger.info(f"Deleting test case: {testcase_id}")
        
        # Delete from both databases
        vector_success = self.vector_db.delete_testcase(testcase_id)
        
        # Structured DB deletion (mark as deleted instead of hard delete)
        structured_success = self.structured_db.update_testcase(
            testcase_id,
            {'status': 'deleted'}
        )
        
        success = vector_success or structured_success
        logger.info(f"Delete test case result: {testcase_id}, success={success}")
        return success
    
    def sync_databases(self) -> Dict[str, Any]:
        """
        Synchronize structured DB data to Vector DB
        
        Useful for:
        - Initial setup
        - Recovery after vector DB failure
        - Adding historical data
        
        Returns:
            Sync statistics
        """
        logger.info("Starting database synchronization...")
        
        # Get all active test cases from MySQL
        try:
            all_testcases = []
            
            # MySQL - use Backend's test_cases table
            if hasattr(self.structured_db, '_connection'):
                with self.structured_db._get_cursor() as cursor:
                    cursor.execute("""
                        SELECT * FROM test_cases
                        WHERE status != 'DEPRECATED'
                    """)
                    all_testcases = cursor.fetchall()
                    
                    # Parse JSON fields and normalize field names
                    for tc in all_testcases:
                        # Normalize field names
                        if 'title' in tc:
                            tc['name'] = tc['title']
                        
                        # Parse JSON
                        if tc.get('steps') and isinstance(tc['steps'], str):
                            tc['steps'] = json.loads(tc['steps'])
                        if tc.get('tags') and isinstance(tc['tags'], str):
                            tc['tags'] = json.loads(tc['tags'])
            else:
                return {'success': False, 'error': 'MySQL database not available'}
            
            logger.info(f"Found {len(all_testcases)} test cases in structured DB to sync")
            
        except Exception as e:
            logger.error(f"Failed to fetch testcases from structured DB: {e}")
            return {'success': False, 'error': str(e)}
        
        # Add to vector DB in batch
        synced_count = 0
        failed_count = 0
        
        for tc in all_testcases:
            tc_id = str(tc.get('_id', ''))
            if tc_id:
                success = self.vector_db.add_testcase(tc_id, tc)
                if success:
                    synced_count += 1
                else:
                    failed_count += 1
        
        result = {
            'success': True,
            'total_testcases': len(all_testcases),
            'synced': synced_count,
            'failed': failed_count,
            'vector_db_stats': self.vector_db.get_collection_stats()
        }
        
        logger.info(f"Database sync completed: {result}")
        return result
    
    def get_statistics(self) -> Dict[str, Any]:
        """
        Get statistics from both databases
        
        Returns:
            Combined statistics
        """
        structured_stats = {}
        
        try:
            # MySQL statistics - use Backend's test_cases table
            if hasattr(self.structured_db, '_connection'):
                with self.structured_db._get_cursor() as cursor:
                    # Total count
                    cursor.execute("SELECT COUNT(*) as count FROM test_cases")
                    total = cursor.fetchone()['count']
                    
                    # Active count (not deprecated)
                    cursor.execute("""
                        SELECT COUNT(*) as count FROM test_cases
                        WHERE status != 'DEPRECATED'
                    """)
                    active = cursor.fetchone()['count']
                    
                    # By type (Backend doesn't have module, use type instead)
                    cursor.execute("""
                        SELECT type, COUNT(*) as count FROM test_cases
                        WHERE status != 'DEPRECATED'
                        GROUP BY type
                    """)
                    by_type = {row['type']: row['count'] for row in cursor.fetchall()}
                    
                    structured_stats = {
                        'total_testcases': total,
                        'active_testcases': active,
                        'by_type': by_type
                    }
                    
        except Exception as e:
            logger.error(f"Failed to get structured DB statistics: {e}")
        
        vector_stats = self.vector_db.get_collection_stats()
        
        return {
            'structured_db': structured_stats,
            'vector_db': vector_stats,
            'synchronized': structured_stats.get('active_testcases') == vector_stats.get('total_testcases')
        }


# Singleton instance
rag_manager = RAGManager()

