"""
Qdrant Vector Database Client for AI Service

High-performance vector database optimized for Mac compatibility
Alternative to Milvus with better cross-platform support
"""
from typing import List, Dict, Any, Optional
import logging
import uuid
import os

try:
    from qdrant_client import QdrantClient as QdrantSDK
    from qdrant_client.models import (
        Distance,
        VectorParams,
        PointStruct,
        Filter,
        FieldCondition,
        MatchValue
    )
    QDRANT_AVAILABLE = True
except ImportError:
    QDRANT_AVAILABLE = False
    logging.warning("qdrant-client not installed, Qdrant client will be disabled")

try:
    from sentence_transformers import SentenceTransformer
    SENTENCE_TRANSFORMERS_AVAILABLE = True
except ImportError:
    SENTENCE_TRANSFORMERS_AVAILABLE = False
    logging.warning("sentence-transformers not installed, using mock embeddings")

from config import ai_config
from data.vector_db_interface import VectorDBInterface

logger = logging.getLogger(__name__)


class QdrantClient(VectorDBInterface):
    """
    Qdrant Vector Database Client for semantic similarity search
    
    Features:
    - Excellent Mac compatibility (native ARM64 support)
    - In-memory mode for development
    - Persistent storage for production
    - High-performance vector search
    - Easy deployment and management
    """
    
    _instance: Optional['QdrantClient'] = None
    _client: Optional[Any] = None
    _embedding_model: Optional[Any] = None
    _connected: bool = False
    
    # Collection configuration
    COLLECTION_NAME = "testcases"
    EMBEDDING_DIM = 384  # paraphrase-multilingual-MiniLM-L12-v2
    
    def __new__(cls):
        if cls._instance is None:
            cls._instance = super().__new__(cls)
        return cls._instance
    
    def __init__(self):
        if not QDRANT_AVAILABLE:
            logger.warning("Qdrant not available, using mock mode")
            return
        
        if not self._connected:
            try:
                # Connect to Qdrant
                # Support both in-memory mode and server mode
                if ai_config.QDRANT_MODE == 'memory':
                    logger.info("Initializing Qdrant in memory mode with persistence")
                    from pathlib import Path
                    storage_path = Path(__file__).parent.parent / "data" / "qdrant_storage"
                    storage_path.mkdir(parents=True, exist_ok=True)
                    self._client = QdrantSDK(path=str(storage_path))
                    logger.info(f"Qdrant storage path: {storage_path}")
                else:
                    # Connect to Qdrant server using HTTP REST API (port 6333)
                    logger.info(f"Connecting to Qdrant HTTP REST API: {ai_config.QDRANT_HOST}:{ai_config.QDRANT_PORT}")
                    
                    # Disable proxy for direct connection to avoid 502 errors
                    # Many environments have HTTP_PROXY set which causes issues with internal IPs
                    import os
                    old_http_proxy = os.environ.get('HTTP_PROXY')
                    old_https_proxy = os.environ.get('HTTPS_PROXY')
                    old_no_proxy = os.environ.get('NO_PROXY')
                    
                    try:
                        # Temporarily disable proxy
                        if old_http_proxy:
                            os.environ.pop('HTTP_PROXY', None)
                        if old_https_proxy:
                            os.environ.pop('HTTPS_PROXY', None)
                        # Ensure internal IPs bypass proxy
                        os.environ['NO_PROXY'] = '127.0.0.1,localhost,10.*,172.*,192.168.*'
                        
                        logger.info("Proxy disabled for direct connection")
                        
                        # Simple connection using host and port (most compatible)
                        # This uses HTTP REST API by default (no gRPC)
                        self._client = QdrantSDK(
                            host=ai_config.QDRANT_HOST,
                            port=ai_config.QDRANT_PORT,
                            timeout=10
                        )
                        
                        # Test connection
                        logger.info("Testing Qdrant connection...")
                        collections = self._client.get_collections()
                        logger.info(f"Successfully connected to Qdrant (collections: {len(collections.collections)})")
                        
                    finally:
                        # Restore proxy settings
                        if old_http_proxy:
                            os.environ['HTTP_PROXY'] = old_http_proxy
                        if old_https_proxy:
                            os.environ['HTTPS_PROXY'] = old_https_proxy
                        if old_no_proxy:
                            os.environ['NO_PROXY'] = old_no_proxy
                        elif 'NO_PROXY' in os.environ:
                            os.environ.pop('NO_PROXY', None)
                
                # Initialize collection
                self._init_collection()
                
                # Initialize embedding model
                if SENTENCE_TRANSFORMERS_AVAILABLE:
                    # Temporarily disable HuggingFace offline mode to load model from cache
                    # This is necessary because SentenceTransformer checks model metadata online
                    # even when the model is cached locally
                    hf_offline = os.environ.get('HF_HUB_OFFLINE')
                    transformers_offline = os.environ.get('TRANSFORMERS_OFFLINE')
                    hf_datasets_offline = os.environ.get('HF_DATASETS_OFFLINE')

                    # Temporarily unset offline mode
                    if hf_offline:
                        os.environ.pop('HF_HUB_OFFLINE', None)
                    if transformers_offline:
                        os.environ.pop('TRANSFORMERS_OFFLINE', None)
                    if hf_datasets_offline:
                        os.environ.pop('HF_DATASETS_OFFLINE', None)

                    try:
                        self._embedding_model = SentenceTransformer(
                            'paraphrase-multilingual-MiniLM-L12-v2'
                        )
                        logger.info("Loaded embedding model: paraphrase-multilingual-MiniLM-L12-v2")
                    finally:
                        # Restore offline mode settings
                        if hf_offline:
                            os.environ['HF_HUB_OFFLINE'] = hf_offline
                        if transformers_offline:
                            os.environ['TRANSFORMERS_OFFLINE'] = transformers_offline
                        if hf_datasets_offline:
                            os.environ['HF_DATASETS_OFFLINE'] = hf_datasets_offline
                        logger.debug("Restored HuggingFace offline mode after model loading")
                else:
                    logger.warning("Sentence transformers not available, using mock embeddings")
                
                self._connected = True
                logger.info("✅ Qdrant client initialized successfully")
                
            except Exception as e:
                logger.error(f"Failed to connect to Qdrant after retries: {e}")
                logger.warning("Qdrant will operate in degraded mode. Please ensure Qdrant service is running.")
                self._connected = False
                self._client = None
    
    def _init_collection(self):
        """
        Initialize Qdrant collection with schema
        
        Collection stores:
        - Vector embeddings (384 dimensions)
        - Payload: testcase_id, module, priority, type
        """
        try:
            # Check if collection exists
            collections = self._client.get_collections().collections
            collection_names = [col.name for col in collections]
            
            if self.COLLECTION_NAME in collection_names:
                logger.info(f"Loaded existing collection: {self.COLLECTION_NAME}")
            else:
                # Create collection
                self._client.create_collection(
                    collection_name=self.COLLECTION_NAME,
                    vectors_config=VectorParams(
                        size=self.EMBEDDING_DIM,
                        distance=Distance.COSINE  # Cosine similarity
                    )
                )
                
                # Create payload indexes for filtering
                self._client.create_payload_index(
                    collection_name=self.COLLECTION_NAME,
                    field_name="module",
                    field_schema="keyword"
                )
                self._client.create_payload_index(
                    collection_name=self.COLLECTION_NAME,
                    field_name="priority",
                    field_schema="keyword"
                )
                self._client.create_payload_index(
                    collection_name=self.COLLECTION_NAME,
                    field_name="testcase_id",
                    field_schema="keyword"
                )
                
                logger.info(f"Created new collection: {self.COLLECTION_NAME}")
            
        except Exception as e:
            logger.error(f"Failed to initialize collection: {e}")
            raise
    
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
        Add a test case to Qdrant
        
        Args:
            testcase_id: Unique ID of test case
            testcase_data: Test case data (must contain 'name')
            
        Returns:
            Success status
        """
        if not self._client:
            logger.warning("Qdrant not available, skipping vector add")
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
            
            # Create point with payload - store all available fields
            point = PointStruct(
                id=str(uuid.uuid4()),  # Qdrant uses UUID for point IDs
                vector=embedding,
                payload={
                    'testcase_id': testcase_id,
                    'module': testcase_data.get('module', 'unknown'),
                    'priority': testcase_data.get('priority', 'P2'),
                    'type': testcase_data.get('type', '功能测试'),
                    'name': testcase_data.get('name', ''),
                    'description': testcase_data.get('description', ''),
                    'steps': testcase_data.get('steps', []),
                    'preconditions': testcase_data.get('preconditions', []),
                    'tags': testcase_data.get('tags', []),
                    'request_id': testcase_data.get('request_id', ''),
                    'generated_at': testcase_data.get('generated_at', '')
                }
            )
            
            # Insert to Qdrant
            self._client.upsert(
                collection_name=self.COLLECTION_NAME,
                points=[point]
            )
            
            logger.info(f"Added test case to Qdrant: {testcase_id}")
            return True
            
        except Exception as e:
            logger.error(f"Failed to add test case to Qdrant: {e}")
            return False
    
    def add_testcases_batch(
        self,
        testcases: List[Dict[str, Any]]
    ) -> int:
        """
        Batch add test cases to Qdrant (more efficient)
        
        Args:
            testcases: List of test case data
            
        Returns:
            Number of successfully added test cases
        """
        if not self._client:
            return 0
        
        try:
            points = []
            
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
                
                point = PointStruct(
                    id=str(uuid.uuid4()),
                    vector=embedding,
                    payload={
                        'testcase_id': testcase_id,
                        'module': tc.get('module', 'unknown'),
                        'priority': tc.get('priority', 'P2'),
                        'type': tc.get('type', '功能测试'),
                        'name': tc.get('name', ''),
                        'description': tc.get('description', ''),
                        'steps': tc.get('steps', []),
                        'preconditions': tc.get('preconditions', []),
                        'tags': tc.get('tags', []),
                        'request_id': tc.get('request_id', ''),
                        'generated_at': tc.get('generated_at', '')
                    }
                )
                points.append(point)
            
            if not points:
                return 0
            
            # Batch insert
            self._client.upsert(
                collection_name=self.COLLECTION_NAME,
                points=points
            )
            
            success_count = len(points)
            logger.info(f"Batch added {success_count} test cases to Qdrant")
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
        if not self._client:
            logger.warning("Qdrant not available, returning empty results")
            return []
        
        try:
            # Generate query embedding
            query_embedding = self.embed_text(query_text)
            
            # Build filter
            query_filter = None
            if module_filter:
                query_filter = Filter(
                    must=[
                        FieldCondition(
                            key="module",
                            match=MatchValue(value=module_filter)
                        )
                    ]
                )
            
            # Search in Qdrant
            search_results = self._client.search(
                collection_name=self.COLLECTION_NAME,
                query_vector=query_embedding,
                limit=top_k,
                query_filter=query_filter
            )
            
            # Format results
            similar_cases = []
            for result in search_results:
                similar_cases.append({
                    'testcase_id': result.payload.get('testcase_id'),
                    'similarity_score': float(result.score),  # Cosine similarity (0-1)
                    'metadata': {
                        'module': result.payload.get('module'),
                        'priority': result.payload.get('priority'),
                        'type': result.payload.get('type')
                    }
                })
            
            logger.info(f"Found {len(similar_cases)} similar test cases for query")
            return similar_cases
            
        except Exception as e:
            logger.error(f"Failed to search similar test cases: {e}")
            return []
    
    def delete_testcase(self, testcase_id: str) -> bool:
        """
        Delete a test case from Qdrant
        
        Args:
            testcase_id: Test case ID to delete
            
        Returns:
            Success status
        """
        if not self._client:
            return False
        
        try:
            # Qdrant doesn't support direct deletion by payload
            # We need to search first, then delete by point ID
            
            # Search for the testcase
            search_results = self._client.scroll(
                collection_name=self.COLLECTION_NAME,
                scroll_filter=Filter(
                    must=[
                        FieldCondition(
                            key="testcase_id",
                            match=MatchValue(value=testcase_id)
                        )
                    ]
                ),
                limit=100
            )
            
            points_to_delete = [point.id for point in search_results[0]]
            
            if points_to_delete:
                self._client.delete(
                    collection_name=self.COLLECTION_NAME,
                    points_selector=points_to_delete
                )
                logger.info(f"Deleted test case from Qdrant: {testcase_id} ({len(points_to_delete)} points)")
                return True
            else:
                logger.warning(f"Test case not found in Qdrant: {testcase_id}")
                return False
            
        except Exception as e:
            logger.error(f"Failed to delete test case: {e}")
            return False
    
    def update_testcase(
        self,
        testcase_id: str,
        testcase_data: Dict[str, Any]
    ) -> bool:
        """
        Update a test case in Qdrant
        
        Note: We delete and re-add to update
        
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
        Get statistics about the Qdrant collection
        
        Returns:
            Collection statistics
        """
        if not self._client:
            return {'available': False}
        
        try:
            collection_info = self._client.get_collection(self.COLLECTION_NAME)
            
            return {
                'available': True,
                'total_testcases': collection_info.points_count,
                'collection_name': self.COLLECTION_NAME,
                'embedding_dimension': self.EMBEDDING_DIM,
                'distance_metric': 'COSINE',
                'vectors_count': collection_info.vectors_count
            }
        except Exception as e:
            logger.error(f"Failed to get collection stats: {e}")
            return {'available': False, 'error': str(e)}
    
    def clear_collection(self) -> bool:
        """
        Clear all test cases from Qdrant (use with caution!)

        Returns:
            Success status
        """
        if not self._client:
            return False

        try:
            # Delete and recreate collection
            self._client.delete_collection(self.COLLECTION_NAME)
            self._init_collection()

            logger.warning("Cleared all test cases from Qdrant")
            return True
        except Exception as e:
            logger.error(f"Failed to clear collection: {e}")
            return False

    def scroll_testcases(
        self,
        limit: int = 100,
        offset: int = 0,
        module_filter: Optional[str] = None
    ) -> Dict[str, Any]:
        """
        Scroll through test cases in the vector database

        Args:
            limit: Number of records to return
            offset: Number of records to skip
            module_filter: Optional module name to filter results

        Returns:
            Dictionary containing total and records
        """
        if not self._client:
            logger.warning("Qdrant not available, returning empty results")
            return {'total': 0, 'records': []}

        try:
            # Build filter
            scroll_filter = None
            if module_filter:
                scroll_filter = Filter(
                    must=[
                        FieldCondition(
                            key="module",
                            match=MatchValue(value=module_filter)
                        )
                    ]
                )

            # Get all matching records to determine total count
            # This is necessary because Qdrant doesn't provide a separate count API
            # and get_collection() may have compatibility issues with some Qdrant versions
            all_points = []
            scroll_result = self._client.scroll(
                collection_name=self.COLLECTION_NAME,
                scroll_filter=scroll_filter,
                limit=10000,  # Large enough to get all records in most cases
                with_payload=True,
                with_vectors=False  # Don't need vectors for history display
            )

            all_points = scroll_result[0]  # First element is the list of points
            total_count = len(all_points)

            # Apply offset and limit for pagination
            paginated_points = all_points[offset:offset + limit]

            # Format results with all available fields
            records = []
            for point in paginated_points:
                payload = point.payload
                records.append({
                    'id': payload.get('testcase_id'),
                    'testcase_id': payload.get('testcase_id'),
                    'module': payload.get('module'),
                    'priority': payload.get('priority'),
                    'type': payload.get('type'),
                    'name': payload.get('name', ''),
                    'description': payload.get('description', ''),
                    'steps': payload.get('steps', []),
                    'preconditions': payload.get('preconditions', []),
                    'tags': payload.get('tags', []),
                    'request_id': payload.get('request_id', ''),
                    'generated_at': payload.get('generated_at', '')
                })

            logger.info(f"Scrolled {len(records)} test cases from Qdrant (total: {total_count}, offset: {offset}, limit: {limit})")
            return {
                'total': total_count,
                'records': records
            }

        except Exception as e:
            logger.error(f"Failed to scroll test cases: {e}", exc_info=True)
            return {'total': 0, 'records': []}

    def close(self):
        """Close Qdrant connection"""
        try:
            if self._client:
                self._client.close()
            self._connected = False
            logger.info("Qdrant connection closed")
        except Exception as e:
            logger.error(f"Failed to close Qdrant connection: {e}")


# Singleton instance
qdrant_client = QdrantClient()

