"""
Vector Database Factory

Factory pattern for creating vector database clients
Supports multiple backends: Milvus, Qdrant
Backend selection via environment variable VECTOR_DB_TYPE
"""
from typing import Optional
import logging

from config import ai_config
from data.vector_db_interface import VectorDBInterface

logger = logging.getLogger(__name__)


def create_vector_db_client() -> VectorDBInterface:
    """
    Create vector database client based on configuration
    
    Environment variable VECTOR_DB_TYPE controls which backend to use:
    - 'milvus': Milvus vector database (enterprise-grade, requires server)
    - 'qdrant': Qdrant vector database (Mac-friendly, can run in-memory)
    
    Returns:
        Vector database client instance
    """
    vector_db_type = ai_config.VECTOR_DB_TYPE.lower()
    
    if vector_db_type == 'milvus':
        try:
            from data.milvus_client import milvus_client
            logger.info("✅ Using Milvus as vector database (enterprise-grade)")
            return milvus_client
        except Exception as e:
            logger.error(f"Failed to initialize Milvus client: {e}")
            logger.warning("Falling back to Qdrant...")
            vector_db_type = 'qdrant'
    
    if vector_db_type == 'qdrant':
        try:
            from data.qdrant_client import qdrant_client
            logger.info("✅ Using Qdrant as vector database (Mac-friendly)")
            return qdrant_client
        except Exception as e:
            logger.error(f"Failed to initialize Qdrant client: {e}")
            raise RuntimeError("No vector database available")
    
    # Invalid type
    logger.error(f"Invalid VECTOR_DB_TYPE: {vector_db_type}")
    logger.warning("Supported types: 'milvus', 'qdrant'")
    logger.warning("Falling back to Qdrant...")
    from data.qdrant_client import qdrant_client
    return qdrant_client


# Singleton vector database client
# Will be initialized based on VECTOR_DB_TYPE environment variable
vector_db_client = create_vector_db_client()

logger.info(f"Vector database client initialized: {type(vector_db_client).__name__}")
