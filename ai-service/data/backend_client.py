"""
Backend API Client
Handles HTTP communication with Backend service
"""
import requests
import logging
from typing import Optional, Dict, Any
from config import config

logger = logging.getLogger(__name__)


class BackendAPIClient:
    """
    Client for calling Backend REST APIs

    Used to fetch environment and version information from Backend
    """

    def __init__(self):
        self.backend_url = config.BACKEND_URL if hasattr(config, 'BACKEND_URL') else 'http://localhost:8080'
        self.timeout = 5  # 5 seconds timeout

    def get_environment(self, environment_id: str) -> Optional[Dict[str, Any]]:
        """
        Get test environment information from Backend

        Args:
            environment_id: Environment ID

        Returns:
            Environment details or None if failed
        """
        if not environment_id:
            return None

        url = f"{self.backend_url}/api/v1/test-environments/{environment_id}"

        try:
            logger.info(f"Fetching environment from Backend: {environment_id}")
            response = requests.get(url, timeout=self.timeout)
            response.raise_for_status()

            result = response.json()
            if result.get('success') and result.get('data'):
                logger.info(f"Successfully fetched environment: {environment_id}")
                return result['data']
            else:
                logger.warning(f"Backend returned unsuccessful response for environment: {environment_id}")
                return None

        except requests.exceptions.Timeout:
            logger.warning(f"Timeout fetching environment {environment_id} from Backend")
            return None
        except requests.exceptions.RequestException as e:
            logger.warning(f"Failed to fetch environment {environment_id}: {e}")
            return None
        except Exception as e:
            logger.error(f"Unexpected error fetching environment {environment_id}: {e}")
            return None

    def get_version(self, version_id: str) -> Optional[Dict[str, Any]]:
        """
        Get test version information from Backend

        Args:
            version_id: Version ID

        Returns:
            Version details or None if failed
        """
        if not version_id:
            return None

        url = f"{self.backend_url}/api/v1/test-versions/{version_id}"

        try:
            logger.info(f"Fetching version from Backend: {version_id}")
            response = requests.get(url, timeout=self.timeout)
            response.raise_for_status()

            result = response.json()
            if result.get('success') and result.get('data'):
                logger.info(f"Successfully fetched version: {version_id}")
                return result['data']
            else:
                logger.warning(f"Backend returned unsuccessful response for version: {version_id}")
                return None

        except requests.exceptions.Timeout:
            logger.warning(f"Timeout fetching version {version_id} from Backend")
            return None
        except requests.exceptions.RequestException as e:
            logger.warning(f"Failed to fetch version {version_id}: {e}")
            return None
        except Exception as e:
            logger.error(f"Unexpected error fetching version {version_id}: {e}")
            return None

    def health_check(self) -> bool:
        """
        Check if Backend service is healthy

        Returns:
            True if Backend is healthy, False otherwise
        """
        url = f"{self.backend_url}/api/v1/health"

        try:
            response = requests.get(url, timeout=self.timeout)
            return response.status_code == 200
        except Exception:
            return False


# Singleton instance
backend_client = BackendAPIClient()
