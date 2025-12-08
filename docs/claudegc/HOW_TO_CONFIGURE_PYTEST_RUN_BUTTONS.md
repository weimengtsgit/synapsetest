# 如何配置 VSCode/Cursor 的 pytest 测试运行按钮

## 📋 问题描述

在 Python 项目中使用 pytest 测试框架时，希望在 VSCode/Cursor IDE 中每个测试方法左侧显示运行按钮（▶️），以便快速运行单个测试而无需使用命令行。

## 🎯 预期效果

配置完成后：
- ✅ 每个测试方法（`def test_*`）左侧显示 ▶️ 运行按钮
- ✅ 单击运行按钮可以运行单个测试
- ✅ 右键可以选择调试测试
- ✅ 测试资源管理器（侧边栏 🧪）显示所有测试的树形结构
- ✅ 自动发现新增的测试

## 🛠️ 解决方案

### 方案概述

通过配置 VSCode/Cursor 的测试框架设置，启用 pytest 并配置相应的路径和参数，让 IDE 能够自动发现并显示测试运行按钮。

### 核心配置文件

需要创建/修改以下配置文件：

1. `.vscode/settings.json` - VSCode 项目设置（必需）
2. `.vscode/launch.json` - 调试配置（可选，推荐）
3. `.vscode/tasks.json` - 任务配置（可选，推荐）
4. `pytest.ini` - pytest 配置优化（可选，推荐）

## 📝 配置步骤

### 步骤 1：创建 `.vscode/settings.json`

在项目根目录创建 `.vscode/settings.json`（如果已存在则修改）：

```json
{
  "python.testing.pytestEnabled": true,
  "python.testing.unittestEnabled": false,
  "python.testing.pytestArgs": [
    "ai-service/tests"
  ],
  "python.testing.autoTestDiscoverOnSaveEnabled": true,
  "python.testing.cwd": "${workspaceFolder}/ai-service",
  "python.defaultInterpreterPath": "python3",
  "python.envFile": "${workspaceFolder}/ai-service/.env",
  
  "testExplorer.useNativeTesting": true,
  
  "python.analysis.extraPaths": [
    "${workspaceFolder}/ai-service"
  ],
  
  "python.testing.promptToConfigure": false
}
```

**关键配置说明：**
- `python.testing.pytestEnabled: true` - 启用 pytest
- `python.testing.pytestArgs` - 设置测试目录路径（相对于工作目录）
- `python.testing.cwd` - 设置测试运行的工作目录
- `python.testing.autoTestDiscoverOnSaveEnabled: true` - 自动发现测试

**⚠️ 注意：** 根据你的项目结构调整路径：
- 如果测试在项目根目录的 `tests/`，改为 `"pytestArgs": ["tests"]`
- 如果测试在子目录，相应调整路径

### 步骤 2：创建 `.vscode/launch.json`（可选）

创建调试配置以支持调试测试：

```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "name": "Python: pytest - Current File",
      "type": "debugpy",
      "request": "launch",
      "module": "pytest",
      "args": [
        "${file}",
        "-v",
        "-s"
      ],
      "console": "integratedTerminal",
      "justMyCode": false,
      "cwd": "${workspaceFolder}/ai-service",
      "env": {
        "PYTHONPATH": "${workspaceFolder}/ai-service"
      }
    },
    {
      "name": "Python: pytest - All Tests",
      "type": "debugpy",
      "request": "launch",
      "module": "pytest",
      "args": [
        "tests/",
        "-v",
        "-s"
      ],
      "console": "integratedTerminal",
      "justMyCode": false,
      "cwd": "${workspaceFolder}/ai-service",
      "env": {
        "PYTHONPATH": "${workspaceFolder}/ai-service"
      }
    }
  ]
}
```

**根据项目结构调整：**
- 修改 `cwd` 为你的项目目录
- 修改 `PYTHONPATH` 为正确的模块路径

### 步骤 3：创建 `.vscode/tasks.json`（可选）

创建快速任务以便通过命令面板运行测试：

```json
{
  "version": "2.0.0",
  "tasks": [
    {
      "label": "pytest: Run All Tests",
      "type": "shell",
      "command": "python3",
      "args": [
        "-m",
        "pytest",
        "tests/",
        "-v"
      ],
      "options": {
        "cwd": "${workspaceFolder}/ai-service"
      },
      "group": {
        "kind": "test",
        "isDefault": true
      },
      "presentation": {
        "reveal": "always",
        "panel": "new"
      },
      "problemMatcher": []
    },
    {
      "label": "pytest: Run Current File",
      "type": "shell",
      "command": "python3",
      "args": [
        "-m",
        "pytest",
        "${relativeFile}",
        "-v",
        "-s"
      ],
      "options": {
        "cwd": "${workspaceFolder}/ai-service"
      },
      "group": "test",
      "presentation": {
        "reveal": "always",
        "panel": "new"
      },
      "problemMatcher": []
    }
  ]
}
```

### 步骤 4：优化 `pytest.ini`（可选）

在测试目录或项目根目录创建或修改 `pytest.ini`：

```ini
[pytest]
# pytest 配置文件
testpaths = tests
python_files = test_*.py
python_classes = Test*
python_functions = test_*

# 命令行选项
addopts = 
    -v
    -s
    --tb=short
    --strict-markers
    --disable-warnings
    --color=yes

# 测试发现模式
norecursedirs = .git .venv venv __pycache__ *.egg-info node_modules

# 日志配置
log_cli = false
log_cli_level = INFO
log_cli_format = %(asctime)s [%(levelname)8s] %(message)s
log_cli_date_format = %Y-%m-%d %H:%M:%S

# 标记（markers）定义
markers =
    slow: marks tests as slow (deselect with '-m "not slow"')
    integration: marks tests as integration tests
    unit: marks tests as unit tests
    api: marks tests as API tests

# 最小Python版本
minversion = 3.8
```

### 步骤 5：重新加载 IDE

完成配置后，必须重新加载 IDE：

**方法 1：** 使用命令面板
```
Cmd/Ctrl + Shift + P -> "Developer: Reload Window"
```

**方法 2：** 重启 VSCode/Cursor

### 步骤 6：配置测试框架（首次使用）

如果是首次配置，IDE 可能提示配置测试：

1. 按 `Cmd/Ctrl + Shift + P`
2. 输入并选择 "**Python: Configure Tests**"
3. 选择 "**pytest**"
4. 选择测试目录（如 `tests`）

### 步骤 7：选择 Python 解释器

确保选择了正确的 Python 解释器：

1. 点击 IDE 左下角的 Python 版本
2. 选择项目使用的 Python 解释器（python3 或虚拟环境）

## ✅ 验证配置

### 验证步骤 1：检查测试发现

在终端运行：
```bash
cd <你的项目目录>
python3 -m pytest --collect-only tests/
```

应该看到类似输出：
```
collected 94 items

<Module test_example.py>
  <Class TestExample>
    <Function test_method_1>
    <Function test_method_2>
    ...
```

### 验证步骤 2：检查 IDE 显示

1. 打开任意测试文件
2. 在测试方法左侧应该看到 ▶️ 图标
3. 点击侧边栏的 🧪 图标，应该看到测试树形结构

### 验证步骤 3：运行单个测试

1. 点击某个测试方法左侧的 ▶️
2. 查看输出面板，确认测试运行成功

## 🔧 常见问题及解决方案

### 问题 1：看不到运行按钮

**可能原因：**
- IDE 未识别测试框架
- Python 解释器配置错误
- 配置文件路径错误

**解决方案：**

1. **重新加载窗口**
   ```
   Cmd/Ctrl + Shift + P -> "Developer: Reload Window"
   ```

2. **手动配置测试**
   ```
   Cmd/Ctrl + Shift + P -> "Python: Configure Tests"
   选择: pytest
   选择: tests 目录
   ```

3. **检查 Python 解释器**
   ```
   点击左下角 Python 版本
   选择正确的解释器
   ```

4. **查看测试日志**
   ```
   打开 "输出" 面板
   从下拉菜单选择 "Python Test Log"
   查看错误信息
   ```

### 问题 2：测试发现失败

**可能原因：**
- pytest 未安装
- PYTHONPATH 配置错误
- 工作目录配置错误

**解决方案：**

1. **检查 pytest 是否安装**
   ```bash
   python3 -m pytest --version
   ```
   如果未安装：
   ```bash
   pip install pytest
   ```

2. **检查工作目录**
   在 `settings.json` 中确认 `python.testing.cwd` 路径正确

3. **设置 PYTHONPATH**
   在测试目录创建 `conftest.py`：
   ```python
   import sys
   from pathlib import Path
   
   # 添加项目根目录到 Python 路径
   project_root = Path(__file__).parent.parent
   if str(project_root) not in sys.path:
       sys.path.insert(0, str(project_root))
   ```

4. **手动收集测试**
   ```bash
   cd <项目目录>
   python3 -m pytest --collect-only tests/ -v
   ```

### 问题 3：导入错误（ModuleNotFoundError）

**可能原因：**
- Python 路径配置不正确
- 缺少 `__init__.py` 文件

**解决方案：**

1. **检查 `conftest.py`**
   确保在测试目录有 `conftest.py` 并正确配置路径

2. **检查 `__init__.py`**
   确保每个测试目录都有 `__init__.py` 文件（可以为空）

3. **在 settings.json 中添加路径**
   ```json
   {
     "python.analysis.extraPaths": [
       "${workspaceFolder}/your-module-path"
     ]
   }
   ```

4. **在命令行设置 PYTHONPATH**
   ```bash
   export PYTHONPATH="${PYTHONPATH}:$(pwd)"
   python3 -m pytest
   ```

### 问题 4：多个 Python 版本冲突

**解决方案：**

1. **明确指定 Python 版本**
   在 `settings.json` 中：
   ```json
   {
     "python.defaultInterpreterPath": "/path/to/your/python3"
   }
   ```

2. **使用虚拟环境**
   ```bash
   python3 -m venv .venv
   source .venv/bin/activate  # macOS/Linux
   .venv\Scripts\activate     # Windows
   pip install pytest
   ```
   然后在 IDE 中选择虚拟环境的 Python 解释器

### 问题 5：测试按钮显示但点击无反应

**解决方案：**

1. **检查输出日志**
   打开 "输出" 面板 -> "Python Test Log" 查看错误

2. **检查测试语法**
   确保测试文件符合 pytest 规范：
   - 文件名：`test_*.py` 或 `*_test.py`
   - 类名：`Test*`（首字母大写）
   - 方法名：`test_*`

3. **检查测试依赖**
   确保所有测试依赖都已安装：
   ```bash
   pip install -r requirements.txt
   ```

## 📚 最佳实践

### 1. 项目结构规范

推荐的项目结构：
```
project/
├── .vscode/
│   ├── settings.json
│   ├── launch.json
│   └── tasks.json
├── src/
│   └── your_module/
│       └── __init__.py
├── tests/
│   ├── __init__.py
│   ├── conftest.py
│   └── test_*.py
├── pytest.ini
└── requirements.txt
```

### 2. 测试文件命名规范

- **文件名：** `test_*.py` 或 `*_test.py`
- **类名：** `Test*`（首字母大写，无下划线）
- **方法名：** `test_*`（小写，使用下划线）

示例：
```python
# test_example.py
import pytest

class TestUserLogin:
    """用户登录测试"""
    
    def test_scenario_1_login_with_valid_credentials(self):
        """场景1: 使用有效凭据登录"""
        # Given
        username = "testuser"
        password = "testpass"
        
        # When
        result = login(username, password)
        
        # Then
        assert result.success is True
```

### 3. 使用 fixtures 共享测试数据

在 `conftest.py` 中定义：
```python
import pytest

@pytest.fixture
def test_client():
    """测试客户端 fixture"""
    from fastapi.testclient import TestClient
    from main import app
    return TestClient(app)

@pytest.fixture
def sample_data():
    """示例数据 fixture"""
    return {
        "username": "testuser",
        "password": "testpass"
    }
```

使用 fixtures：
```python
def test_with_fixtures(test_client, sample_data):
    response = test_client.post("/login", json=sample_data)
    assert response.status_code == 200
```

### 4. 使用标记组织测试

定义标记：
```python
import pytest

@pytest.mark.slow
def test_large_dataset():
    pass

@pytest.mark.integration
def test_api_integration():
    pass

@pytest.mark.unit
def test_unit_function():
    pass
```

运行特定标记的测试：
```bash
pytest -m integration      # 只运行集成测试
pytest -m "not slow"       # 跳过慢速测试
```

## 🎯 快速参考

### 配置文件模板

**最小配置（`.vscode/settings.json`）：**
```json
{
  "python.testing.pytestEnabled": true,
  "python.testing.pytestArgs": ["tests"],
  "python.testing.cwd": "${workspaceFolder}"
}
```

### 常用命令

```bash
# 运行所有测试
python3 -m pytest

# 运行特定文件
python3 -m pytest tests/test_example.py

# 运行特定测试
python3 -m pytest tests/test_example.py::TestClass::test_method

# 显示详细输出
python3 -m pytest -v -s

# 只运行失败的测试
python3 -m pytest --lf

# 收集测试（不运行）
python3 -m pytest --collect-only
```

### IDE 快捷操作

- **运行测试：** 点击测试方法左侧的 ▶️
- **调试测试：** 右键 ▶️ -> "Debug Test"
- **打开测试资源管理器：** 侧边栏 🧪 图标
- **运行任务：** `Cmd/Ctrl + Shift + P` -> "Tasks: Run Task"
- **配置测试：** `Cmd/Ctrl + Shift + P` -> "Python: Configure Tests"
- **重新加载：** `Cmd/Ctrl + Shift + P` -> "Developer: Reload Window"

## 📖 相关资源

### 官方文档
- **pytest 官方文档：** https://docs.pytest.org/
- **VSCode Python 测试：** https://code.visualstudio.com/docs/python/testing
- **pytest fixtures：** https://docs.pytest.org/en/stable/fixture.html

### 插件推荐
- **ms-python.python** - Python 扩展
- **ms-python.vscode-pylance** - Python 语言服务器
- **ms-python.debugpy** - Python 调试器

### 扩展工具
- **pytest-cov** - 测试覆盖率
- **pytest-xdist** - 并行测试执行
- **pytest-mock** - Mock 功能增强

## 📝 总结

### 核心步骤回顾

1. ✅ 创建 `.vscode/settings.json` 并启用 pytest
2. ✅ 配置测试路径和工作目录
3. ✅ 重新加载 IDE
4. ✅ 配置测试框架（首次使用）
5. ✅ 选择正确的 Python 解释器
6. ✅ 验证测试发现和运行

### 关键配置项

- `python.testing.pytestEnabled: true` - 启用 pytest
- `python.testing.pytestArgs` - 测试目录路径
- `python.testing.cwd` - 工作目录
- `python.testing.autoTestDiscoverOnSaveEnabled: true` - 自动发现

### 故障排查优先级

1. 重新加载 IDE
2. 配置测试框架
3. 检查 Python 解释器
4. 查看测试日志
5. 验证 pytest 安装
6. 检查路径配置

---

**文档版本：** 1.0  
**最后更新：** 2025-11-26  
**适用范围：** VSCode/Cursor + pytest  
**Python 版本：** 3.8+  
**pytest 版本：** 6.0+

