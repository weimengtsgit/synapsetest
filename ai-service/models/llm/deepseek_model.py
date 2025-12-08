"""
DeepSeek Model Wrapper

Wraps DeepSeek models for local inference or API access
"""
from typing import Optional
import logging
import json

from .base_model import BaseLLMModel

logger = logging.getLogger(__name__)


class DeepSeekModel(BaseLLMModel):
    """
    DeepSeek Model Wrapper
    
    Supports local GPU inference with optional quantization
    """
    
    def __init__(self, model_path: Optional[str] = None, model_name: str = "deepseek-coder"):
        self.model_name = model_name
        self.version = "1.0"
        self.model = None
        self.tokenizer = None
        self._initialized = False
        
        if model_path:
            self._load_model(model_path)
    
    def _load_model(self, model_path: str):
        """
        Load DeepSeek model from path
        
        Args:
            model_path: Path to model files
        """
        try:
            from transformers import AutoTokenizer, AutoModelForCausalLM
            import torch
            
            logger.info(f"Loading DeepSeek model from {model_path}")
            
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
            logger.info("DeepSeek model loaded successfully")
            
        except ImportError as e:
            logger.error(f"Required packages not available: {e}")
            self._initialized = False
        except Exception as e:
            logger.error(f"Failed to load DeepSeek model: {e}")
            self._initialized = False
    
    def generate(
        self,
        prompt: str,
        max_tokens: int = 2048,
        temperature: float = 0.7,
        top_p: float = 0.9
    ) -> str:
        """
        Generate text using DeepSeek model

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
            raise RuntimeError("DeepSeek model not initialized")

        import time
        start_time = time.time()

        # Log full request
        logger.info(f"[DEEPSEEK LOCAL - REQUEST] max_tokens={max_tokens}, temperature={temperature}, top_p={top_p}")
        logger.info(f"[DEEPSEEK LOCAL - REQUEST PROMPT - FULL]\n{prompt}")

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
                    pad_token_id=self.tokenizer.pad_token_id,
                    eos_token_id=self.tokenizer.eos_token_id
                )

            # Decode response
            response = self.tokenizer.decode(outputs[0], skip_special_tokens=True)

            # Remove input prompt from response
            if response.startswith(prompt):
                response = response[len(prompt):].strip()

            elapsed = time.time() - start_time
            logger.info(f"[DEEPSEEK LOCAL - RESPONSE] Success, Time: {elapsed:.2f}s")
            logger.info(f"[DEEPSEEK LOCAL - RESPONSE CONTENT - FULL]\n{response}")

            return response

        except Exception as e:
            elapsed = time.time() - start_time
            logger.error(f"[DEEPSEEK LOCAL - RESPONSE] Failed after {elapsed:.2f}s: {e}")
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
            'capabilities': ['text-generation', 'code-generation', 'chat']
        }


class DeepSeekAPIModel(BaseLLMModel):
    """
    DeepSeek API Model
    
    Uses DeepSeek API for inference
    """
    
    def __init__(self, api_key: str, api_base: str = "https://api.deepseek.com/v1", model_name: str = "deepseek-chat"):
        self.api_key = api_key
        self.api_base = api_base
        self.model_name = model_name
        self.client = None
        self._setup_client()
    
    def _setup_client(self):
        """Setup DeepSeek API client"""
        try:
            import openai
            
            self.client = openai.OpenAI(
                api_key=self.api_key,
                base_url=self.api_base
            )
            logger.info(f"DeepSeek API client configured for {self.api_base}")
        except ImportError:
            logger.error("openai package not available")
            self.client = None
        except Exception as e:
            logger.error(f"Failed to setup DeepSeek API client: {e}")
            self.client = None
    
    def generate(
        self,
        prompt: str,
        max_tokens: int = 2048,
        temperature: float = 0.7,
        top_p: float = 0.9
    ) -> str:
        """
        Generate text via DeepSeek API

        Args:
            prompt: Input prompt
            max_tokens: Maximum tokens
            temperature: Sampling temperature
            top_p: Top-p sampling

        Returns:
            Generated text
        """
        if not self.client:
            raise RuntimeError("DeepSeek API client not initialized")

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
        logger.info(f"[DEEPSEEK API - REQUEST] model={self.model_name}, max_tokens={max_tokens}, temperature={temperature}, top_p={top_p}")
        logger.info(f"[DEEPSEEK API - REQUEST MESSAGES]\n{json.dumps(messages, ensure_ascii=False, indent=2)}")

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
                logger.info(f"[DEEPSEEK API - RESPONSE] Success, Time: {elapsed:.2f}s, "
                           f"Tokens: {usage.prompt_tokens} prompt + {usage.completion_tokens} completion = {usage.total_tokens} total")
            else:
                logger.info(f"[DEEPSEEK API - RESPONSE] Success, Time: {elapsed:.2f}s")

            # Log full response content
            logger.info(f"[DEEPSEEK API - RESPONSE CONTENT - FULL]\n{content}")

            return content

        except Exception as e:
            elapsed = time.time() - start_time
            logger.error(f"[DEEPSEEK API - RESPONSE] Failed after {elapsed:.2f}s: {e}")
            raise
    
    def get_model_info(self) -> dict:
        """Get model information"""
        return {
            'model_name': self.model_name,
            'version': '1.0',
            'provider': 'deepseek-api',
            'api_base': self.api_base,
            'capabilities': ['text-generation', 'code-generation', 'chat']
        }




