"""
Milvus Vector Database Client for AI Service

Enterprise-grade vector database for semantic similarity search
Replaces ChromaDB with better performance and scalability
"""
from typing import List, Dict, Any, Optional
import logging
import numpy as np

try:
    from pymilvus import (
        connections,
        Collection,
        CollectionSchema,
        FieldSchema,
        DataType,
        utility
    )
    PYMILVUS_AVAILABLE = True
except ImportError:
    PYMILVUS_AVAILABLE = False
    logging.warning("pymilvus not installed, Milvus client will be disabled")

try:
    from sentence_transformers import SentenceTransformer
    SENTENCE_TRANSFORMERS_AVAILABLE = True
except ImportError:
    SENTENCE_TRANSFORMERS_AVAILABLE = False
    logging.warning("sentence-transformers not installed, using mock embeddings")

from config import ai_config
from data.vector_db_interface import VectorDBInterface

logger = logging.getLogger(__name__)


class MilvusClient(VectorDBInterface):
    """
    Milvus Vector Database Client for semantic similarity search
    
    Features:
    - Enterprise-grade vector search
    - Billion-scale vector indexing
    - GPU acceleration support
    - High-performance ANN search
    - Production-ready scalability
    """
    
    _instance: Optional['MilvusClient'] = None
    _collection: Optional[Any] = None
    _embedding_model: Optional[Any] = None
    _connected: bool = False
    
    # Collection configuration
    COLLECTION_NAME = "testcases"
    EMBEDDING_DIM = 384  # paraphrase-multilingual-MiniLM-L12-v2
    INDEX_TYPE = "IVF_FLAT"  # Can also use: HNSW, IVF_SQ8, ANNOY
    METRIC_TYPE = "L2"  # L2 (Euclidean) or IP (Inner Product) or COSINE
    
    def __new__(cls):
        if cls._instance is None:
            cls._instance = super().__new__(cls)
        return cls._instance
    
    def __init__(self):
        if not PYMILVUS_AVAILABLE:
            logger.warning("Milvus not available, using mock mode")
            return
        
        if not self._connected:
            try:
                # Connect to Milvus
                connections.connect(
                    alias="default",
                    host=ai_config.MILVUS_HOST,
                    port=ai_config.MILVUS_PORT
                )
                
                logger.info(f"Connected to Milvus: {ai_config.MILVUS_HOST}:{ai_config.MILVUS_PORT}")
                
                # Initialize collection
                self._init_collection()
                
                # Initialize embedding model
                if SENTENCE_TRANSFORMERS_AVAILABLE:
                    self._embedding_model = SentenceTransformer(
                        'paraphrase-multilingual-MiniLM-L12-v2'
                    )
                    logger.info("Loaded embedding model: paraphrase-multilingual-MiniLM-L12-v2")
                else:
                    logger.warning("Sentence transformers not available, using mock embeddings")
                
                self._connected = True
                
            except Exception as e:
                logger.error(f"Failed to connect to Milvus: {e}")
                self._connected = False
    
    def _init_collection(self):
        """
        Initialize Milvus collection with schema
        
        Schema:
        - id: INT64 (primary key)
        - testcase_id: VARCHAR (unique identifier)
        - embedding: FLOAT_VECTOR (384 dimensions)
        - module: VARCHAR (for filtering)
        - priority: VARCHAR (for filtering)
        - type: VARCHAR (for filtering)
        """
        try:
            # Check if collection exists
            if utility.has_collection(self.COLLECTION_NAME):
                self._collection = Collection(self.COLLECTION_NAME)
                logger.info(f"Loaded existing collection: {self.COLLECTION_NAME}")
            else:
                # Create collection with schema
                fields = [
                    FieldSchema(
                        name="id",
                        dtype=DataType.INT64,
                        is_primary=True,
                        auto_id=True,
                        description="Auto-generated primary key"
                    ),
                    FieldSchema(
                        name="testcase_id",
                        dtype=DataType.VARCHAR,
                        max_length=100,
                        description="Test case unique identifier"
                    ),
                    FieldSchema(
                        name="embedding",
                        dtype=DataType.FLOAT_VECTOR,
                        dim=self.EMBEDDING_DIM,
                        description="Text embedding vector"
                    ),
                    FieldSchema(
                        name="module",
                        dtype=DataType.VARCHAR,
                        max_length=100,
                        description="Module name for filtering"
                    ),
                    FieldSchema(
                        name="priority",
                        dtype=DataType.VARCHAR,
                        max_length=10,
                        description="Priority level (P0-P3)"
                    ),
                    FieldSchema(
                        name="type",
                        dtype=DataType.VARCHAR,
                        max_length=50,
                        description="Test case type"
                    )
                ]
                
                schema = CollectionSchema(
                    fields=fields,
                    description="Test cases vector collection for RAG"
                )
                
                self._collection = Collection(
                    name=self.COLLECTION_NAME,
                    schema=schema
                )
                
                logger.info(f"Created new collection: {self.COLLECTION_NAME}")
                
                # Create index for vector search
                self._create_index()
            
            # Load collection to memory for search
            self._collection.load()
            logger.info("Collection loaded to memory")
            
        except Exception as e:
            logger.error(f"Failed to initialize collection: {e}")
            raise
    
    def _create_index(self):
        """
        Create vector index for fast similarity search
        
        Index types:
        - FLAT: Exact search, slowest but most accurate
        - IVF_FLAT: Good balance of speed and accuracy
        - IVF_SQ8: Faster but less accurate (quantized)
        - HNSW: Fast and accurate, more memory
        - ANNOY: Fast, good for read-heavy scenarios
        """
        try:
            index_params = {
                "metric_type": self.METRIC_TYPE,
                "index_type": self.INDEX_TYPE,
                "params": {"nlist": 128}  # Number of clusters for IVF
            }
            
            self._collection.create_index(
                field_name="embedding",
                index_params=index_params
            )
            
            logger.info(f"Created index: {self.INDEX_TYPE} with {self.METRIC_TYPE} metric")
            
        except Exception as e:
            logger.warning(f"Failed to create index: {e}")
    
    def embed_text(self, text: str) -> List[float]:
        """
        Convert text to embedding vector
        
        Args:
            text: Input text to embed
            
        Returns:
            Embedding vector (384 dimensions)
        """
        if not SENTENCE_TRANSFORMERS_AVAILABLE or self._embedding_model is None:
            # Mock embedding for testing
            return [0.0] * self.EMBEDDING_DIM
        
        try:
            embedding = self._embedding_model.encode(text, convert_to_numpy=True)
            return embedding.tolist()
        except Exception as e:
            logger.error(f"Failed to embed text: {e}")
            return [0.0] * self.EMBEDDING_DIM
    
    def add_testcase(
        self,
        testcase_id: str,
        testcase_data: Dict[str, Any]
    ) -> bool:
        """
        Add a test case to Milvus
        
        Args:
            testcase_id: Unique ID of test case
            testcase_data: Test case data (must contain 'name')
            
        Returns:
            Success status
        """
        if not self._collection:
            logger.warning("Milvus not available, skipping vector add")
            return False
        
        try:
            # Create text representation for embedding
            text_parts = [testcase_data.get('name', '')]
            
            if 'description' in testcase_data:
                text_parts.append(testcase_data['description'])
            
            # Add steps summary
            if 'steps' in testcase_data:
                steps = testcase_data['steps']
                if isinstance(steps, list):
                    step_texts = [s.get('action', '') for s in steps if isinstance(s, dict)]
                    text_parts.extend(step_texts[:3])  # First 3 steps
            
            text = " ".join(text_parts)
            
            # Generate embedding
            embedding = self.embed_text(text)
            
            # Prepare data
            entities = [
                [testcase_id],  # testcase_id
                [embedding],  # embedding
                [testcase_data.get('module', 'unknown')],  # module
                [testcase_data.get('priority', 'P2')],  # priority
                [testcase_data.get('type', '功能测试')]  # type
            ]
            
            # Insert to Milvus
            self._collection.insert(entities)
            self._collection.flush()  # Ensure data is persisted
            
            logger.info(f"Added test case to Milvus: {testcase_id}")
            return True
            
        except Exception as e:
            logger.error(f"Failed to add test case to Milvus: {e}")
            return False
    
    def add_testcases_batch(
        self,
        testcases: List[Dict[str, Any]]
    ) -> int:
        """
        Batch add test cases to Milvus (more efficient)
        
        Args:
            testcases: List of test case data
            
        Returns:
            Number of successfully added test cases
        """
        if not self._collection:
            return 0
        
        try:
            testcase_ids = []
            embeddings = []
            modules = []
            priorities = []
            types = []
            
            for tc in testcases:
                testcase_id = str(tc.get('_id') or tc.get('id', ''))
                if not testcase_id:
                    continue
                
                # Create text representation
                text_parts = [tc.get('name', '')]
                if 'description' in tc:
                    text_parts.append(tc['description'])
                if 'steps' in tc and isinstance(tc['steps'], list):
                    step_texts = [s.get('action', '') for s in tc['steps'] if isinstance(s, dict)]
                    text_parts.extend(step_texts[:3])
                
                text = " ".join(text_parts)
                embedding = self.embed_text(text)
                
                testcase_ids.append(testcase_id)
                embeddings.append(embedding)
                modules.append(tc.get('module', 'unknown'))
                priorities.append(tc.get('priority', 'P2'))
                types.append(tc.get('type', '功能测试'))
            
            if not testcase_ids:
                return 0
            
            # Batch insert
            entities = [testcase_ids, embeddings, modules, priorities, types]
            self._collection.insert(entities)
            self._collection.flush()
            
            success_count = len(testcase_ids)
            logger.info(f"Batch added {success_count} test cases to Milvus")
            return success_count
            
        except Exception as e:
            logger.error(f"Failed to batch add test cases: {e}")
            return 0
    
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
        """
        if not self._collection:
            logger.warning("Milvus not available, returning empty results")
            return []
        
        try:
            # Generate query embedding
            query_embedding = self.embed_text(query_text)
            
            # Build search parameters
            search_params = {
                "metric_type": self.METRIC_TYPE,
                "params": {"nprobe": 10}  # Number of clusters to search
            }
            
            # Build filter expression
            expr = None
            if module_filter:
                expr = f'module == "{module_filter}"'
            
            # Search in Milvus
            results = self._collection.search(
                data=[query_embedding],
                anns_field="embedding",
                param=search_params,
                limit=top_k,
                expr=expr,
                output_fields=["testcase_id", "module", "priority", "type"]
            )
            
            # Format results
            similar_cases = []
            if results and len(results) > 0:
                for hit in results[0]:
                    # Convert distance to similarity score
                    # For L2: similarity = 1 / (1 + distance)
                    # For IP: similarity = distance (already normalized)
                    if self.METRIC_TYPE == "L2":
                        similarity_score = 1 / (1 + hit.distance)
                    else:
                        similarity_score = hit.distance
                    
                    similar_cases.append({
                        'testcase_id': hit.entity.get('testcase_id'),
                        'similarity_score': float(similarity_score),
                        'metadata': {
                            'module': hit.entity.get('module'),
                            'priority': hit.entity.get('priority'),
                            'type': hit.entity.get('type')
                        }
                    })
            
            logger.info(f"Found {len(similar_cases)} similar test cases for query")
            return similar_cases
            
        except Exception as e:
            logger.error(f"Failed to search similar test cases: {e}")
            return []
    
    def delete_testcase(self, testcase_id: str) -> bool:
        """
        Delete a test case from Milvus
        
        Args:
            testcase_id: Test case ID to delete
            
        Returns:
            Success status
        """
        if not self._collection:
            return False
        
        try:
            expr = f'testcase_id == "{testcase_id}"'
            self._collection.delete(expr)
            self._collection.flush()
            
            logger.info(f"Deleted test case from Milvus: {testcase_id}")
            return True
        except Exception as e:
            logger.error(f"Failed to delete test case: {e}")
            return False
    
    def update_testcase(
        self,
        testcase_id: str,
        testcase_data: Dict[str, Any]
    ) -> bool:
        """
        Update a test case in Milvus
        
        Note: Milvus doesn't support direct update, so we delete and re-add
        
        Args:
            testcase_id: Test case ID
            testcase_data: Updated test case data
            
        Returns:
            Success status
        """
        self.delete_testcase(testcase_id)
        return self.add_testcase(testcase_id, testcase_data)
    
    def get_collection_stats(self) -> Dict[str, Any]:
        """
        Get statistics about the Milvus collection
        
        Returns:
            Collection statistics
        """
        if not self._collection:
            return {'available': False}
        
        try:
            stats = self._collection.num_entities
            
            return {
                'available': True,
                'total_testcases': stats,
                'collection_name': self.COLLECTION_NAME,
                'embedding_dimension': self.EMBEDDING_DIM,
                'index_type': self.INDEX_TYPE,
                'metric_type': self.METRIC_TYPE
            }
        except Exception as e:
            logger.error(f"Failed to get collection stats: {e}")
            return {'available': False, 'error': str(e)}
    
    def clear_collection(self) -> bool:
        """
        Clear all test cases from Milvus (use with caution!)
        
        Returns:
            Success status
        """
        if not self._collection:
            return False
        
        try:
            # Drop and recreate collection
            self._collection.drop()
            self._init_collection()
            
            logger.warning("Cleared all test cases from Milvus")
            return True
        except Exception as e:
            logger.error(f"Failed to clear collection: {e}")
            return False
    
    def close(self):
        """Close Milvus connection"""
        try:
            if self._collection:
                self._collection.release()
            connections.disconnect("default")
            self._connected = False
            logger.info("Milvus connection closed")
        except Exception as e:
            logger.error(f"Failed to close Milvus connection: {e}")


# Singleton instance
milvus_client = MilvusClient()

