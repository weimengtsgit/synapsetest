"""
Vector Database Interface - Abstract Base Class

Defines common interface for all vector database implementations
Supports: Milvus, Qdrant, and other vector databases
"""
from abc import ABC, abstractmethod
from typing import List, Dict, Any, Optional


class VectorDBInterface(ABC):
    """
    Abstract interface for vector database operations
    
    All vector database implementations must implement these methods
    to ensure consistent behavior across different backends.
    """
    
    @abstractmethod
    def embed_text(self, text: str) -> List[float]:
        """
        Convert text to embedding vector
        
        Args:
            text: Input text to embed
            
        Returns:
            Embedding vector
        """
        pass
    
    @abstractmethod
    def add_testcase(
        self,
        testcase_id: str,
        testcase_data: Dict[str, Any]
    ) -> bool:
        """
        Add a test case to vector database
        
        Args:
            testcase_id: Unique ID of test case
            testcase_data: Test case data (must contain 'name')
            
        Returns:
            Success status
        """
        pass
    
    @abstractmethod
    def add_testcases_batch(
        self,
        testcases: List[Dict[str, Any]]
    ) -> int:
        """
        Batch add test cases to vector database (more efficient)
        
        Args:
            testcases: List of test case data
            
        Returns:
            Number of successfully added test cases
        """
        pass
    
    @abstractmethod
    def search_similar(
        self,
        query_text: str,
        top_k: int = 5,
        module_filter: Optional[str] = None
    ) -> List[Dict[str, Any]]:
        """
        Search for similar test cases using semantic similarity
        
        Args:
            query_text: Query text (requirement or description)
            top_k: Number of results to return
            module_filter: Optional module name to filter results
            
        Returns:
            List of similar test cases with similarity scores
            Format: [
                {
                    'testcase_id': str,
                    'similarity_score': float,
                    'metadata': {
                        'module': str,
                        'priority': str,
                        'type': str
                    }
                }
            ]
        """
        pass
    
    @abstractmethod
    def delete_testcase(self, testcase_id: str) -> bool:
        """
        Delete a test case from vector database
        
        Args:
            testcase_id: Test case ID to delete
            
        Returns:
            Success status
        """
        pass
    
    @abstractmethod
    def update_testcase(
        self,
        testcase_id: str,
        testcase_data: Dict[str, Any]
    ) -> bool:
        """
        Update a test case in vector database
        
        Args:
            testcase_id: Test case ID
            testcase_data: Updated test case data
            
        Returns:
            Success status
        """
        pass
    
    @abstractmethod
    def get_collection_stats(self) -> Dict[str, Any]:
        """
        Get statistics about the vector database collection
        
        Returns:
            Collection statistics
            Format: {
                'available': bool,
                'total_testcases': int,
                'collection_name': str,
                'embedding_dimension': int,
                ...
            }
        """
        pass
    
    @abstractmethod
    def clear_collection(self) -> bool:
        """
        Clear all test cases from vector database (use with caution!)
        
        Returns:
            Success status
        """
        pass
    
    @abstractmethod
    def close(self):
        """Close vector database connection"""
        pass

