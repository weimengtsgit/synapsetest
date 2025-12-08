# 测试配置说明

## IDE 测试运行按钮配置

本项目已配置好 VSCode/Cursor 的测试运行功能，每个测试方法左侧会显示运行按钮（▶️）。

### 配置文件说明

1. **`.vscode/settings.json`** - VSCode/Cursor 测试配置
   - 启用 pytest 作为测试框架
   - 配置测试路径为 `ai-service/tests`
   - 启用自动测试发现

2. **`.vscode/launch.json`** - 调试配置
   - 支持调试单个测试文件
   - 支持调试所有测试
   - 支持调试 FastAPI 应用

3. **`pytest.ini`** - pytest 配置文件
   - 定义测试发现规则
   - 配置测试输出格式
   - 定义自定义标记

### 使用方法

#### 方法 1：使用 IDE 运行按钮
1. 打开任何测试文件（如 `test_testcase_generation.py`）
2. 在每个测试方法左侧会出现 ▶️ 图标
3. 点击图标可以：
   - **运行测试**：单击 ▶️
   - **调试测试**：右键选择 "Debug Test"

#### 方法 2：使用测试资源管理器
1. 打开 VSCode/Cursor 的测试视图（侧边栏的烧杯图标 🧪）
2. 浏览所有测试用例树形结构
3. 点击任意测试用例旁的运行按钮

#### 方法 3：使用命令行
```bash
# 进入 ai-service 目录
cd ai-service

# 运行所有测试
pytest

# 运行特定文件
pytest tests/test_testcase_generation.py

# 运行特定测试方法
pytest tests/test_testcase_generation.py::TestAITestCaseGenerationAPI::test_scenario_1_generate_cases_with_valid_input

# 运行特定测试类
pytest tests/test_testcase_generation.py::TestAITestCaseGenerationAPI

# 显示详细输出
pytest -v -s

# 仅运行失败的测试
pytest --lf
```

### 测试标记使用

可以使用 pytest 标记来组织和筛选测试：

```python
@pytest.mark.slow
def test_large_dataset():
    pass

@pytest.mark.integration
def test_api_integration():
    pass
```

运行时筛选：
```bash
# 只运行标记为 integration 的测试
pytest -m integration

# 跳过标记为 slow 的测试
pytest -m "not slow"
```

### 故障排除

#### 问题 1：测试按钮不显示
**解决方法**：
1. 确保已安装 Python 扩展
2. 重新加载 VSCode/Cursor 窗口（Cmd/Ctrl + Shift + P -> "Reload Window"）
3. 打开命令面板，运行 "Python: Configure Tests"
4. 选择 pytest，选择 tests 目录

#### 问题 2：测试发现失败
**解决方法**：
1. 检查 Python 解释器是否正确：点击左下角 Python 版本
2. 确保在 ai-service 目录下有虚拟环境
3. 查看输出面板的 "Python Test Log" 了解详细错误

#### 问题 3：导入错误
**解决方法**：
1. 确保 `PYTHONPATH` 包含 ai-service 目录
2. 检查 `.vscode/settings.json` 中的路径配置
3. 在测试运行前设置环境变量：
```bash
export PYTHONPATH="${PYTHONPATH}:$(pwd)"
```

### 推荐的 VSCode 扩展

运行以下命令自动安装推荐扩展：
```bash
# VSCode 会自动提示安装 .vscode/extensions.json 中推荐的扩展
```

必需扩展：
- `ms-python.python` - Python 支持
- `ms-python.vscode-pylance` - Python 语言服务器
- `ms-python.debugpy` - Python 调试器

可选扩展：
- `littlefoxteam.vscode-python-test-adapter` - 增强测试适配器

### 测试覆盖率

启用覆盖率报告：
```bash
# 安装覆盖率工具
pip install pytest-cov

# 运行测试并生成覆盖率报告
pytest --cov=. --cov-report=html --cov-report=term-missing

# 在浏览器中查看报告
open htmlcov/index.html
```

### 并行测试执行

加速测试执行：
```bash
# 安装并行执行插件
pip install pytest-xdist

# 使用多个 CPU 核心运行测试
pytest -n auto
```

## 测试文件结构规范

所有测试文件应遵循以下结构：

```python
"""
测试模块说明
"""
import pytest
from fastapi.testclient import TestClient
from main import app

client = TestClient(app)


class TestFeatureName:
    """测试类说明"""

    def test_scenario_1_description(self):
        """场景1: 测试场景描述"""
        # Given - 准备测试数据
        
        # When - 执行操作
        
        # Then - 验证结果
        pass
```

### 命名规范

- **文件**：`test_*.py`
- **类**：`Test*`（首字母大写）
- **方法**：`test_*`（小写，使用下划线）
- **场景命名**：`test_scenario_N_description` 其中 N 是场景编号

### 最佳实践

1. **使用 Given-When-Then 模式**组织测试代码
2. **每个测试方法只测试一个场景**
3. **使用描述性的测试方法名**
4. **添加清晰的文档字符串**
5. **使用 fixtures 共享测试数据**
6. **使用 mock 隔离外部依赖**

## 更多信息

参考 `TESTING_GUIDE.md` 获取完整的测试指南。

