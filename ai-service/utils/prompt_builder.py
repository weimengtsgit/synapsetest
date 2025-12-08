"""
Prompt Builder for LLM Test Case Generation
"""
from typing import List, Dict, Any
import logging

logger = logging.getLogger(__name__)


TESTCASE_GENERATION_PROMPT = """你是一个专业的测试工程师，擅长编写高质量的测试用例。

【任务】
基于以下需求文档，生成详细的测试用例。

【需求文档】
{requirement_text}

【历史用例参考】（相似需求的测试用例）
{similar_testcases}

【企业测试标准】
{company_standards}

【输出格式要求】
请生成 {num_cases} 个测试用例，每个用例包含：
1. 用例名称：简明扼要描述测试目标
2. 前置条件：执行测试前需要满足的条件
3. 测试步骤：详细的操作步骤（编号）
4. 预期结果：每个步骤的预期输出
5. 优先级：P0(最高)/P1(高)/P2(中)/P3(低)
6. 测试类型：功能测试/性能测试/安全测试/兼容性测试

【输出格式】（JSON）
```json
[
  {{
    "name": "用例名称",
    "priority": "P0",
    "type": "功能测试",
    "preconditions": ["前置条件1", "前置条件2"],
    "steps": [
      {{"step": 1, "action": "操作描述", "expected": "预期结果"}}
    ],
    "tags": ["标签1", "标签2"]
  }}
]
```

【注意事项】
- 确保测试用例覆盖正常流程、异常流程和边界条件
- 优先级根据业务重要性和风险程度判断
- 测试步骤要具体可执行
- 预期结果要明确可验证

请开始生成测试用例：
"""


class PromptBuilder:
    """Build prompts for LLM test case generation"""

    def __init__(self):
        self.base_template = TESTCASE_GENERATION_PROMPT

    def build_generation_prompt(
        self,
        requirement_text: str,
        similar_testcases: List[Dict[str, Any]],
        company_standards: Dict[str, Any],
        num_cases: int = 5
    ) -> str:
        """
        Build complete prompt for test case generation

        Args:
            requirement_text: The requirement document text
            similar_testcases: List of similar historical test cases
            company_standards: Company testing standards
            num_cases: Number of test cases to generate

        Returns:
            Complete prompt string
        """
        formatted_cases = self._format_similar_cases(similar_testcases)
        formatted_standards = self._format_company_standards(company_standards)

        prompt = self.base_template.format(
            requirement_text=requirement_text,
            similar_testcases=formatted_cases,
            company_standards=formatted_standards,
            num_cases=num_cases
        )

        return prompt

    def _format_similar_cases(self, cases: List[Dict[str, Any]]) -> str:
        """Format similar test cases for prompt"""
        if not cases:
            return "无相似历史用例"

        formatted = []
        for i, case in enumerate(cases[:3], 1):  # Limit to 3 cases
            case_str = f"""
用例{i}: {case.get('name', 'Unnamed')}
- 优先级: {case.get('priority', 'P2')}
- 类型: {case.get('type', '功能测试')}
- 步骤数: {len(case.get('steps', []))}
"""
            formatted.append(case_str.strip())

        return "\n".join(formatted)

    def _format_company_standards(self, standards: Dict[str, Any]) -> str:
        """Format company standards for prompt"""
        if not standards:
            return "遵循行业标准测试规范"

        formatted = f"""
- 命名规范: {standards.get('naming_convention', '描述性命名')}
- 优先级分类: {', '.join(standards.get('priority_levels', ['P0', 'P1', 'P2', 'P3']))}
- 测试类型: {', '.join(standards.get('test_types', ['功能测试']))}
- 必填字段: {', '.join(standards.get('required_fields', ['name', 'steps']))}
"""
        return formatted.strip()

    def build_edge_case_prompt(self, requirement_text: str) -> str:
        """Build prompt specifically for edge case generation"""
        return f"""你是一个专业的测试工程师，专注于边界条件和异常场景测试。

【任务】
针对以下需求，生成边界条件和异常场景的测试用例。

【需求文档】
{requirement_text}

【重点关注】
1. 输入边界值（最大值、最小值、空值、特殊字符）
2. 异常流程（网络错误、超时、并发）
3. 权限和安全边界
4. 性能边界（大数据量、高并发）

【输出要求】
1. 生成 5-10 个边界测试用例（控制数量以确保完整性）
2. 直接返回 JSON 数组，不要使用 markdown 代码块（不要用 ```json）
3. 严格按照以下 JSON 格式：

[
  {{
    "name": "测试用例名称",
    "priority": "P0/P1/P2",
    "type": "边界测试",
    "preconditions": ["前置条件1", "前置条件2"],
    "steps": [
      {{"step": 1, "action": "操作步骤", "expected": "预期结果"}},
      {{"step": 2, "action": "操作步骤", "expected": "预期结果"}}
    ],
    "tags": ["边界条件", "相关标签"]
  }}
]

【注意】
- 只返回 JSON 数组，不要添加任何说明文字
- 不要使用 ```json 或 ``` 包裹
- 确保 JSON 格式完整正确，所有括号和引号都要闭合
- 如果内容较多，优先保证 JSON 完整性，可以适当减少测试用例数量

现在请直接输出 JSON 数组："""

    def build_api_test_prompt(self, api_spec: Dict[str, Any]) -> str:
        """Build prompt for API test case generation"""
        return f"""你是一个专业的API测试工程师。

【任务】
基于以下API规范，生成API测试用例。

【API规范】
- 端点: {api_spec.get('endpoint', '')}
- 方法: {api_spec.get('method', 'GET')}
- 参数: {api_spec.get('parameters', {})}
- 响应: {api_spec.get('response', {})}

【测试覆盖】
1. 正常请求测试
2. 参数验证测试
3. 认证授权测试
4. 错误处理测试
5. 性能基准测试

请以JSON格式输出测试用例：
"""


class DocumentParser:
    """Parse requirement documents"""

    def parse(self, content: str, doc_type: str = "text") -> str:
        """
        Parse document content

        Args:
            content: Raw document content
            doc_type: Document type (text, markdown, html)

        Returns:
            Parsed text content
        """
        if doc_type == "markdown":
            return self._parse_markdown(content)
        elif doc_type == "html":
            return self._parse_html(content)
        else:
            return self._clean_text(content)

    def _parse_markdown(self, content: str) -> str:
        """Parse markdown content"""
        # Simple markdown parsing - remove common markers
        lines = content.split('\n')
        cleaned = []
        for line in lines:
            # Remove headers markers but keep text
            if line.startswith('#'):
                line = line.lstrip('#').strip()
            # Remove bold/italic markers
            line = line.replace('**', '').replace('*', '').replace('__', '')
            # Remove links but keep text
            # [text](url) -> text
            import re
            line = re.sub(r'\[([^\]]+)\]\([^\)]+\)', r'\1', line)
            cleaned.append(line)
        return '\n'.join(cleaned)

    def _parse_html(self, content: str) -> str:
        """Parse HTML content"""
        # Simple HTML tag removal
        import re
        clean = re.compile('<.*?>')
        text = re.sub(clean, '', content)
        return self._clean_text(text)

    def _clean_text(self, content: str) -> str:
        """Clean and normalize text"""
        # Remove excessive whitespace
        lines = content.split('\n')
        cleaned = []
        for line in lines:
            line = line.strip()
            if line:
                cleaned.append(line)
        return '\n'.join(cleaned)

    def extract_sections(self, content: str) -> List[Dict[str, str]]:
        """
        Extract sections from document

        Returns:
            List of sections with title and content
        """
        sections = []
        lines = content.split('\n')
        current_section = {'title': 'Introduction', 'content': []}

        for line in lines:
            # Check if this is a section header (starts with # or number)
            if line.startswith('#') or (line and line[0].isdigit() and '.' in line):
                # Save previous section
                if current_section['content']:
                    current_section['content'] = '\n'.join(current_section['content'])
                    sections.append(current_section)
                # Start new section
                title = line.lstrip('#').strip()
                if title and title[0].isdigit():
                    # Remove number prefix like "1. " or "1.1 "
                    parts = title.split(' ', 1)
                    if len(parts) > 1:
                        title = parts[1]
                current_section = {'title': title, 'content': []}
            else:
                current_section['content'].append(line)

        # Add last section
        if current_section['content']:
            current_section['content'] = '\n'.join(current_section['content'])
            sections.append(current_section)

        return sections
