"""
AI Service Configuration Management
"""
import os
from typing import Optional
from pathlib import Path
from dotenv import load_dotenv

# Load .env file if it exists
env_file = Path(__file__).parent / '.env'
if env_file.exists():
    load_dotenv(env_file)
    print(f"✓ Loaded configuration from {env_file}")
else:
    print("ℹ No .env file found, using system environment variables and defaults")

class AIConfig:
    """AI Service Configuration"""

    # Model paths
    QWEN_MODEL_PATH: str = os.getenv('QWEN_MODEL_PATH', '/models/Qwen-7B-Chat')
    DEEPSEEK_MODEL_PATH: str = os.getenv('DEEPSEEK_MODEL_PATH', '/models/deepseek-coder-6.7b-instruct')
    DEEPSEEK_MODEL_NAME: str = os.getenv('DEEPSEEK_MODEL_NAME', 'deepseek-coder')
    SENTENCE_BERT_MODEL: str = 'paraphrase-multilingual-mpnet-base-v2'
    XGBOOST_MODEL_PATH: str = os.getenv('XGBOOST_MODEL_PATH', '/models/xgboost_models/')

    # Model runtime configuration
    USE_GPU: bool = os.getenv('USE_GPU', 'false').lower() == 'true'
    MAX_GPU_MEMORY: str = os.getenv('MAX_GPU_MEMORY', '16GB')
    DEVICE_MAP: str = 'auto'

    # Inference configuration
    MAX_TOKENS: int = int(os.getenv('MAX_TOKENS', '2048'))
    TEMPERATURE: float = float(os.getenv('TEMPERATURE', '0.7'))
    TOP_P: float = float(os.getenv('TOP_P', '0.9'))

    # Edge case generation specific configuration
    # Edge cases typically need more tokens due to detailed boundary scenarios
    EDGE_CASE_MAX_TOKENS: int = int(os.getenv('EDGE_CASE_MAX_TOKENS', '3072'))
    EDGE_CASE_TEMPERATURE: float = float(os.getenv('EDGE_CASE_TEMPERATURE', '0.8'))

    # Vector database configuration
    # Supported types: 'milvus', 'qdrant'
    VECTOR_DB_TYPE: str = os.getenv('VECTOR_DB_TYPE', 'qdrant')
    
    # Milvus configuration
    MILVUS_HOST: str = os.getenv('MILVUS_HOST', 'localhost')
    MILVUS_PORT: int = int(os.getenv('MILVUS_PORT', '19530'))
    
    # Qdrant configuration
    QDRANT_HOST: str = os.getenv('QDRANT_HOST', 'localhost')
    QDRANT_PORT: int = int(os.getenv('QDRANT_PORT', '6333'))
    QDRANT_GRPC_PORT: int = int(os.getenv('QDRANT_GRPC_PORT', '6334'))
    QDRANT_MODE: str = os.getenv('QDRANT_MODE', 'server')  # 'server' or 'memory'

    # MySQL configuration (Recommended, shared with Backend Service)
    MYSQL_URI: str = os.getenv('MYSQL_URI', 'mysql://root:password@localhost:3306/synapsetest')
    MYSQL_HOST: str = os.getenv('MYSQL_HOST', 'localhost')
    MYSQL_PORT: int = int(os.getenv('MYSQL_PORT', '3306'))
    MYSQL_USER: str = os.getenv('MYSQL_USER', 'root')
    MYSQL_PASSWORD: str = os.getenv('MYSQL_PASSWORD', 'password')
    MYSQL_DATABASE: str = os.getenv('MYSQL_DATABASE', 'synapsetest')
    
    # Database type (fixed to mysql)
    DATABASE_TYPE: str = 'mysql'

    # Redis configuration
    ENABLE_REDIS: bool = os.getenv('ENABLE_REDIS', 'false').lower() in ('true', '1', 'yes')
    REDIS_HOST: str = os.getenv('REDIS_HOST', 'localhost')
    REDIS_PORT: int = int(os.getenv('REDIS_PORT', '6379'))
    REDIS_PASSWORD: Optional[str] = os.getenv('REDIS_PASSWORD', None)
    REDIS_DB: int = int(os.getenv('REDIS_DB', '0'))

    # API configuration
    API_PREFIX: str = '/api/v1/ai'

    # Semantic deduplication
    SIMILARITY_THRESHOLD: float = float(os.getenv('SIMILARITY_THRESHOLD', '0.85'))

    # Cache configuration
    CACHE_TTL: int = int(os.getenv('CACHE_TTL', '300'))  # 5 minutes
    CACHE_MAX_SIZE: int = int(os.getenv('CACHE_MAX_SIZE', '1000'))

    # LLM Provider configuration
    # Supported providers: mock, qwen-local, qwen-api, deepseek-local, deepseek-api
    LLM_PROVIDER: str = os.getenv('LLM_PROVIDER', 'mock')

    # API configuration
    LLM_API_KEY: Optional[str] = os.getenv('LLM_API_KEY', None)
    LLM_API_BASE: Optional[str] = os.getenv('LLM_API_BASE', None)

    # Model name for API providers (e.g., qwen-plus, deepseek-chat, deepseek-coder)
    LLM_MODEL_NAME: Optional[str] = os.getenv('LLM_MODEL_NAME', None)

    @classmethod
    def get_mysql_settings(cls) -> dict:
        """Get MySQL connection settings"""
        return {
            'uri': cls.MYSQL_URI,
            'host': cls.MYSQL_HOST,
            'port': cls.MYSQL_PORT,
            'user': cls.MYSQL_USER,
            'password': cls.MYSQL_PASSWORD,
            'database': cls.MYSQL_DATABASE
        }

    @classmethod
    def get_redis_settings(cls) -> dict:
        """Get Redis connection settings"""
        return {
            'host': cls.REDIS_HOST,
            'port': cls.REDIS_PORT,
            'password': cls.REDIS_PASSWORD,
            'db': cls.REDIS_DB
        }

    @classmethod
    def get_milvus_settings(cls) -> dict:
        """Get Milvus connection settings"""
        return {
            'host': cls.MILVUS_HOST,
            'port': cls.MILVUS_PORT
        }
    
    @classmethod
    def get_qdrant_settings(cls) -> dict:
        """Get Qdrant connection settings"""
        return {
            'host': cls.QDRANT_HOST,
            'port': cls.QDRANT_PORT,
            'grpc_port': cls.QDRANT_GRPC_PORT,
            'mode': cls.QDRANT_MODE
        }


class Settings:
    """Application settings"""

    app_name: str = "SynapseTest AI Service"
    version: str = "1.0.0"
    debug: bool = os.getenv('DEBUG', 'false').lower() == 'true'

    # CORS settings
    cors_origins: list = os.getenv('CORS_ORIGINS', '*').split(',')

    # Logging
    log_level: str = os.getenv('LOG_LEVEL', 'INFO')


# Singleton instances
ai_config = AIConfig()
settings = Settings()
