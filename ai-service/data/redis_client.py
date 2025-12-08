"""
Redis Client for AI Service Caching
"""
from typing import Optional, Any
import json
import logging

try:
    import redis
    REDIS_AVAILABLE = True
except ImportError:
    REDIS_AVAILABLE = False

from config import ai_config

logger = logging.getLogger(__name__)


class RedisClient:
    """Redis client for caching AI inference results"""

    _instance: Optional['RedisClient'] = None
    _client: Optional[Any] = None

    def __new__(cls):
        if cls._instance is None:
            cls._instance = super().__new__(cls)
        return cls._instance

    def __init__(self):
        # Initialize memory cache as fallback
        if not hasattr(self, '_memory_cache'):
            self._memory_cache = {}
        
        # Check if Redis is enabled via environment variable
        if not ai_config.ENABLE_REDIS:
            logger.info("Redis disabled by configuration (ENABLE_REDIS=false), using in-memory cache")
            self._client = None
            return
        
        if not REDIS_AVAILABLE:
            logger.warning("redis package not available, using in-memory cache")
            self._client = None
            return

        if self._client is None:
            try:
                self._client = redis.Redis(
                    host=ai_config.REDIS_HOST,
                    port=ai_config.REDIS_PORT,
                    password=ai_config.REDIS_PASSWORD,
                    db=ai_config.REDIS_DB,
                    decode_responses=True
                )
                # Test connection
                self._client.ping()
                logger.info(f"Connected to Redis: {ai_config.REDIS_HOST}:{ai_config.REDIS_PORT}")
            except Exception as e:
                logger.warning(f"Failed to connect to Redis: {e}, using in-memory cache")
                self._client = None

    def get(self, key: str) -> Optional[Any]:
        """Get cached value"""
        if self._client:
            try:
                value = self._client.get(key)
                if value:
                    return json.loads(value)
                return None
            except Exception as e:
                logger.error(f"Redis get error: {e}")
                return None
        else:
            return self._memory_cache.get(key)

    def set(self, key: str, value: Any, ttl: int = None) -> bool:
        """Set cached value with optional TTL"""
        if ttl is None:
            ttl = ai_config.CACHE_TTL

        if self._client:
            try:
                self._client.setex(key, ttl, json.dumps(value))
                return True
            except Exception as e:
                logger.error(f"Redis set error: {e}")
                return False
        else:
            self._memory_cache[key] = value
            return True

    def delete(self, key: str) -> bool:
        """Delete cached value"""
        if self._client:
            try:
                self._client.delete(key)
                return True
            except Exception as e:
                logger.error(f"Redis delete error: {e}")
                return False
        else:
            self._memory_cache.pop(key, None)
            return True

    def exists(self, key: str) -> bool:
        """Check if key exists"""
        if self._client:
            try:
                return self._client.exists(key) > 0
            except Exception as e:
                logger.error(f"Redis exists error: {e}")
                return False
        else:
            return key in self._memory_cache

    def get_recommendation_cache(self, context_hash: str) -> Optional[dict]:
        """Get cached recommendation result"""
        key = f"recommendation:{context_hash}"
        return self.get(key)

    def set_recommendation_cache(self, context_hash: str, recommendation: dict) -> bool:
        """Cache recommendation result"""
        key = f"recommendation:{context_hash}"
        return self.set(key, recommendation)

    def get_environment_status(self) -> dict:
        """Get cached environment status"""
        status = self.get("env_status")
        if status:
            return status
        return {
            'dev': {'available': True, 'load': 0.3, 'stability': 0.95},
            'staging': {'available': True, 'load': 0.5, 'stability': 0.98},
            'prod': {'available': True, 'load': 0.7, 'stability': 0.99}
        }

    def close(self):
        """Close Redis connection"""
        if self._client:
            self._client.close()
            self._client = None


# Singleton instance
redis_client = RedisClient()
