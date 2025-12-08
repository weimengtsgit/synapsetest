"""
Base LLM Model Interface

Supports multiple LLM backends: local, API, mock
"""
from abc import ABC, abstractmethod
from typing import Optional
import logging

logger = logging.getLogger(__name__)


class BaseLLMModel(ABC):
    """Abstract base class for LLM models"""

    @abstractmethod
    def generate(
        self,
        prompt: str,
        max_tokens: int = 2048,
        temperature: float = 0.7,
        top_p: float = 0.9
    ) -> str:
        """
        Generate text response from prompt

        Args:
            prompt: Input prompt
            max_tokens: Maximum tokens to generate
            temperature: Sampling temperature
            top_p: Top-p sampling parameter

        Returns:
            Generated text
        """
        pass

    @abstractmethod
    def get_model_info(self) -> dict:
        """Get model information"""
        pass


class MockLLMModel(BaseLLMModel):
    """
    Mock LLM for testing and development

    Returns predefined responses based on prompt keywords
    """

    def __init__(self):
        self.model_name = "mock-llm"
        self.version = "1.0.0"
        logger.info("Initialized Mock LLM Model")

    def generate(
        self,
        prompt: str,
        max_tokens: int = 2048,
        temperature: float = 0.7,
        top_p: float = 0.9
    ) -> str:
        """Generate mock test cases"""
        logger.info(f"Mock LLM generating response for prompt ({len(prompt)} chars)")

        # Return mock test cases based on prompt content
        if "测试用例" in prompt or "test case" in prompt.lower():
            return self._generate_mock_testcases()
        elif "边界" in prompt or "edge" in prompt.lower():
            return self._generate_mock_edge_cases()
        else:
            return self._generate_generic_response()

    def _generate_mock_testcases(self) -> str:
        """Generate mock test case JSON"""
        return '''[
  {
    "name": "验证用户登录功能-正常流程",
    "priority": "P0",
    "type": "功能测试",
    "preconditions": [
      "用户已注册账号",
      "系统正常运行"
    ],
    "steps": [
      {"step": 1, "action": "打开登录页面", "expected": "登录页面正常显示"},
      {"step": 2, "action": "输入有效用户名和密码", "expected": "输入框接受输入"},
      {"step": 3, "action": "点击登录按钮", "expected": "显示加载状态"},
      {"step": 4, "action": "等待系统响应", "expected": "成功登录并跳转到主页"}
    ],
    "tags": ["登录", "核心功能", "P0"]
  },
  {
    "name": "验证用户登录功能-密码错误",
    "priority": "P1",
    "type": "功能测试",
    "preconditions": [
      "用户已注册账号",
      "系统正常运行"
    ],
    "steps": [
      {"step": 1, "action": "打开登录页面", "expected": "登录页面正常显示"},
      {"step": 2, "action": "输入正确用户名和错误密码", "expected": "输入框接受输入"},
      {"step": 3, "action": "点击登录按钮", "expected": "显示加载状态"},
      {"step": 4, "action": "等待系统响应", "expected": "显示密码错误提示，不跳转"}
    ],
    "tags": ["登录", "异常流程", "安全"]
  },
  {
    "name": "验证用户登录功能-账号不存在",
    "priority": "P1",
    "type": "功能测试",
    "preconditions": [
      "系统正常运行"
    ],
    "steps": [
      {"step": 1, "action": "打开登录页面", "expected": "登录页面正常显示"},
      {"step": 2, "action": "输入不存在的用户名", "expected": "输入框接受输入"},
      {"step": 3, "action": "点击登录按钮", "expected": "显示加载状态"},
      {"step": 4, "action": "等待系统响应", "expected": "显示账号不存在提示"}
    ],
    "tags": ["登录", "异常流程"]
  },
  {
    "name": "验证用户登录功能-空输入验证",
    "priority": "P2",
    "type": "功能测试",
    "preconditions": [
      "系统正常运行"
    ],
    "steps": [
      {"step": 1, "action": "打开登录页面", "expected": "登录页面正常显示"},
      {"step": 2, "action": "不输入任何内容，直接点击登录", "expected": "显示必填字段提示"},
      {"step": 3, "action": "只输入用户名，不输入密码", "expected": "显示密码必填提示"},
      {"step": 4, "action": "只输入密码，不输入用户名", "expected": "显示用户名必填提示"}
    ],
    "tags": ["登录", "输入验证", "边界条件"]
  },
  {
    "name": "验证用户登录功能-记住密码",
    "priority": "P2",
    "type": "功能测试",
    "preconditions": [
      "用户已注册账号",
      "系统支持记住密码功能"
    ],
    "steps": [
      {"step": 1, "action": "打开登录页面", "expected": "登录页面正常显示"},
      {"step": 2, "action": "输入用户名和密码", "expected": "输入框接受输入"},
      {"step": 3, "action": "勾选记住密码选项", "expected": "选项被选中"},
      {"step": 4, "action": "点击登录按钮", "expected": "登录成功"},
      {"step": 5, "action": "退出并重新打开登录页面", "expected": "用户名自动填充"}
    ],
    "tags": ["登录", "用户体验"]
  }
]'''

    def _generate_mock_edge_cases(self) -> str:
        """Generate mock edge case test cases"""
        return '''[
  {
    "name": "边界测试-超长用户名输入",
    "priority": "P2",
    "type": "功能测试",
    "preconditions": ["系统正常运行"],
    "steps": [
      {"step": 1, "action": "输入256个字符的用户名", "expected": "系统应截断或拒绝"},
      {"step": 2, "action": "尝试提交", "expected": "显示长度超限错误"}
    ],
    "tags": ["边界测试", "输入验证"]
  },
  {
    "name": "边界测试-特殊字符输入",
    "priority": "P2",
    "type": "安全测试",
    "preconditions": ["系统正常运行"],
    "steps": [
      {"step": 1, "action": "输入包含SQL注入字符的内容", "expected": "系统应过滤或转义"},
      {"step": 2, "action": "尝试提交", "expected": "正常处理，无安全漏洞"}
    ],
    "tags": ["边界测试", "安全测试", "注入防护"]
  }
]'''

    def _generate_generic_response(self) -> str:
        """Generate generic response"""
        return "[]"

    def get_model_info(self) -> dict:
        """Get mock model information"""
        return {
            'model_name': self.model_name,
            'version': self.version,
            'provider': 'mock',
            'capabilities': ['text-generation']
        }
