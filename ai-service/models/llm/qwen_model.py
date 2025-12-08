"""
Qwen Model Wrapper

Wraps Qwen-7B-Chat model for local inference
"""
from typing import Optional
import logging
import json

from .base_model import BaseLLMModel

logger = logging.getLogger(__name__)


class QwenModel(BaseLLMModel):
    """
    Qwen-7B-Chat Model Wrapper

    Supports local GPU inference with optional quantization
    """

    def __init__(self, model_path: Optional[str] = None):
        self.model_name = "Qwen-7B-Chat"
        self.version = "1.0"
        self.model = None
        self.tokenizer = None
        self._initialized = False

        if model_path:
            self._load_model(model_path)

    def _load_model(self, model_path: str):
        """
        Load Qwen model from path

        Args:
            model_path: Path to model files
        """
        try:
            from transformers import AutoTokenizer, AutoModelForCausalLM
            import torch

            logger.info(f"Loading Qwen model from {model_path}")

            self.tokenizer = AutoTokenizer.from_pretrained(
                model_path,
                trust_remote_code=True
            )

            # Check GPU availability
            if torch.cuda.is_available():
                logger.info("GPU available, loading model with GPU support")
                self.model = AutoModelForCausalLM.from_pretrained(
                    model_path,
                    torch_dtype=torch.float16,
                    device_map="auto",
                    trust_remote_code=True
                )
            else:
                logger.info("GPU not available, loading model on CPU")
                self.model = AutoModelForCausalLM.from_pretrained(
                    model_path,
                    torch_dtype=torch.float32,
                    device_map="cpu",
                    trust_remote_code=True
                )

            self.model.eval()
            self._initialized = True
            logger.info("Qwen model loaded successfully")

        except ImportError as e:
            logger.error(f"Required packages not available: {e}")
            self._initialized = False
        except Exception as e:
            logger.error(f"Failed to load Qwen model: {e}")
            self._initialized = False

    def generate(
        self,
        prompt: str,
        max_tokens: int = 2048,
        temperature: float = 0.7,
        top_p: float = 0.9
    ) -> str:
        """
        Generate text using Qwen model

        Args:
            prompt: Input prompt
            max_tokens: Maximum new tokens to generate
            temperature: Sampling temperature
            top_p: Top-p sampling

        Returns:
            Generated text
        """
        if not self._initialized:
            logger.error("Model not initialized")
            raise RuntimeError("Qwen model not initialized")

        import time
        start_time = time.time()

        # Log full request
        logger.info(f"[QWEN LOCAL - REQUEST] max_tokens={max_tokens}, temperature={temperature}, top_p={top_p}")
        logger.info(f"[QWEN LOCAL - REQUEST PROMPT - FULL]\n{prompt}")

        try:
            import torch

            # Tokenize input
            inputs = self.tokenizer(prompt, return_tensors="pt")
            inputs = {k: v.to(self.model.device) for k, v in inputs.items()}

            # Generate
            with torch.no_grad():
                outputs = self.model.generate(
                    **inputs,
                    max_new_tokens=max_tokens,
                    temperature=temperature,
                    top_p=top_p,
                    do_sample=True,
                    pad_token_id=self.tokenizer.pad_token_id
                )

            # Decode response
            response = self.tokenizer.decode(outputs[0], skip_special_tokens=True)

            # Remove input prompt from response
            if response.startswith(prompt):
                response = response[len(prompt):].strip()

            elapsed = time.time() - start_time
            logger.info(f"[QWEN LOCAL - RESPONSE] Success, Time: {elapsed:.2f}s")
            logger.info(f"[QWEN LOCAL - RESPONSE CONTENT - FULL]\n{response}")

            return response

        except Exception as e:
            elapsed = time.time() - start_time
            logger.error(f"[QWEN LOCAL - RESPONSE] Failed after {elapsed:.2f}s: {e}")
            raise

    def batch_generate(
        self,
        prompts: list,
        max_tokens: int = 2048,
        temperature: float = 0.7
    ) -> list:
        """
        Generate responses for multiple prompts

        Args:
            prompts: List of input prompts
            max_tokens: Maximum tokens per response
            temperature: Sampling temperature

        Returns:
            List of generated responses
        """
        responses = []
        for prompt in prompts:
            response = self.generate(prompt, max_tokens, temperature)
            responses.append(response)
        return responses

    def get_model_info(self) -> dict:
        """Get model information"""
        return {
            'model_name': self.model_name,
            'version': self.version,
            'provider': 'local',
            'initialized': self._initialized,
            'capabilities': ['text-generation', 'chat']
        }


class APILLMModel(BaseLLMModel):
    """
    API-based LLM Model

    Uses OpenAI-compatible API for inference (for SaaS deployment)
    """

    def __init__(self, api_key: str, api_base: str):
        self.api_key = api_key
        self.api_base = api_base
        self.model_name = "qwen-plus"
        self.client = None
        self._setup_client()

    def _setup_client(self):
        """Setup OpenAI client"""
        try:
            import openai

            self.client = openai.OpenAI(
                api_key=self.api_key,
                base_url=self.api_base
            )
            logger.info(f"API LLM client configured for {self.api_base}")
        except ImportError:
            logger.error("openai package not available")
            self.client = None
        except Exception as e:
            logger.error(f"Failed to setup API client: {e}")
            self.client = None

    def generate(
        self,
        prompt: str,
        max_tokens: int = 2048,
        temperature: float = 0.7,
        top_p: float = 0.9
    ) -> str:
        """
        Generate text via API

        Args:
            prompt: Input prompt
            max_tokens: Maximum tokens
            temperature: Sampling temperature
            top_p: Top-p sampling

        Returns:
            Generated text
        """
        if not self.client:
            raise RuntimeError("API client not initialized")

        import time
        start_time = time.time()

        # Prepare messages
        messages = [
            {
                "role": "system",
                "content": "你是一个专业的测试工程师，擅长编写高质量的测试用例。"
            },
            {"role": "user", "content": prompt}
        ]

        # Log full request
        logger.info(f"[QWEN API - REQUEST] model={self.model_name}, max_tokens={max_tokens}, temperature={temperature}, top_p={top_p}")
        logger.info(f"[QWEN API - REQUEST MESSAGES]\n{json.dumps(messages, ensure_ascii=False, indent=2)}")

        try:
            response = self.client.chat.completions.create(
                model=self.model_name,
                messages=messages,
                max_tokens=max_tokens,
                temperature=temperature,
                top_p=top_p
            )

            content = response.choices[0].message.content
            elapsed = time.time() - start_time

            # Log token usage if available
            if hasattr(response, 'usage'):
                usage = response.usage
                logger.info(f"[QWEN API - RESPONSE] Success, Time: {elapsed:.2f}s, "
                           f"Tokens: {usage.prompt_tokens} prompt + {usage.completion_tokens} completion = {usage.total_tokens} total")
            else:
                logger.info(f"[QWEN API - RESPONSE] Success, Time: {elapsed:.2f}s")

            # Log full response content
            logger.info(f"[QWEN API - RESPONSE CONTENT - FULL]\n{content}")

            return content

        except Exception as e:
            elapsed = time.time() - start_time
            logger.error(f"[QWEN API - RESPONSE] Failed after {elapsed:.2f}s: {e}")
            raise

    def get_model_info(self) -> dict:
        """Get model information"""
        return {
            'model_name': self.model_name,
            'version': '1.0',
            'provider': 'api',
            'api_base': self.api_base,
            'capabilities': ['text-generation', 'chat']
        }


def create_llm_model(provider: str = "mock", **kwargs) -> BaseLLMModel:
    """
    Factory function to create LLM model

    Args:
        provider: Provider type
            - 'mock': Mock model for testing
            - 'qwen-local': Local Qwen model
            - 'qwen-api': Qwen API (OpenAI-compatible)
            - 'deepseek-local': Local DeepSeek model
            - 'deepseek-api': DeepSeek official API
        **kwargs: Additional arguments for specific providers
            - model_path: Path to local model
            - api_key: API key for API providers
            - api_base: API base URL for API providers
            - model_name: Model name for API providers

    Returns:
        LLM model instance
    """
    if provider == "mock":
        from .base_model import MockLLMModel
        return MockLLMModel()

    elif provider == "qwen-local":
        model_path = kwargs.get('model_path')
        if not model_path:
            raise ValueError("model_path required for qwen-local provider")
        return QwenModel(model_path)

    elif provider == "qwen-api":
        api_key = kwargs.get('api_key')
        api_base = kwargs.get('api_base')
        if not api_key or not api_base:
            raise ValueError("api_key and api_base required for qwen-api provider")
        return APILLMModel(api_key, api_base)

    elif provider == "deepseek-local":
        from .deepseek_model import DeepSeekModel
        model_path = kwargs.get('model_path')
        model_name = kwargs.get('model_name', 'deepseek-coder')
        if not model_path:
            raise ValueError("model_path required for deepseek-local provider")
        return DeepSeekModel(model_path, model_name)

    elif provider == "deepseek-api":
        from .deepseek_model import DeepSeekAPIModel
        api_key = kwargs.get('api_key')
        api_base = kwargs.get('api_base', 'https://api.deepseek.com/v1')
        model_name = kwargs.get('model_name', 'deepseek-chat')
        if not api_key:
            raise ValueError("api_key required for deepseek-api provider")
        return DeepSeekAPIModel(api_key, api_base, model_name)

    # Backward compatibility with old provider names
    elif provider == "local":
        logger.warning("Provider 'local' is deprecated, use 'qwen-local' instead")
        model_path = kwargs.get('model_path')
        if not model_path:
            raise ValueError("model_path required for local provider")
        return QwenModel(model_path)

    elif provider == "api":
        logger.warning("Provider 'api' is deprecated, use 'qwen-api' or 'deepseek-api' instead")
        api_key = kwargs.get('api_key')
        api_base = kwargs.get('api_base')
        if not api_key or not api_base:
            raise ValueError("api_key and api_base required for API provider")
        return APILLMModel(api_key, api_base)

    else:
        raise ValueError(
            f"Unknown provider: {provider}. "
            f"Supported providers: mock, qwen-local, qwen-api, deepseek-local, deepseek-api"
        )
