# 🚀 测试快速启动指南

## ✅ 配置完成

你的测试环境已经配置完毕！现在每个测试方法左侧都会显示运行按钮 ▶️。

## 📝 立即开始

### 方式 1：IDE 运行按钮（推荐）

1. 打开任意测试文件（如 `test_testcase_generation.py`）
2. 找到测试方法，例如：
   ```python
   def test_scenario_1_generate_cases_with_valid_input(self):
   ```
3. 在行号左侧会出现 ▶️ 图标
4. **单击运行** 或 **右键选择调试**

### 方式 2：测试资源管理器

1. 点击 VSCode/Cursor 侧边栏的 🧪 图标（测试）
2. 浏览测试树形结构
3. 点击任何测试用例旁的 ▶️ 运行

### 方式 3：命令面板

1. 按 `Cmd/Ctrl + Shift + P`
2. 输入 "Test: Run"
3. 选择要运行的测试

## 🔧 常用命令

### 命令行运行测试

```bash
cd ai-service

# 运行所有测试
python3 -m pytest

# 运行特定文件
python3 -m pytest tests/test_testcase_generation.py

# 运行特定测试方法
python3 -m pytest tests/test_testcase_generation.py::TestAITestCaseGenerationAPI::test_scenario_1_generate_cases_with_valid_input

# 显示详细输出
python3 -m pytest -v -s

# 只运行失败的测试
python3 -m pytest --lf
```

### VSCode 任务（Cmd/Ctrl + Shift + P -> "Tasks: Run Task"）

- **pytest: Run All Tests** - 运行所有测试
- **pytest: Run Current File** - 运行当前文件
- **pytest: Run with Coverage** - 运行测试并生成覆盖率报告
- **pytest: Run Failed Tests Only** - 只运行失败的测试
- **pytest: Run API Tests** - 只运行 API 测试
- **pytest: Run Model Tests** - 只运行模型测试

## 📊 测试统计

当前项目包含：
- ✅ **94 个测试用例**
- 📁 **7 个测试文件**
- 🎯 **15 个测试类**

### 测试文件列表

| 文件 | 测试类 | 测试方法数 |
|------|--------|-----------|
| `test_testcase_generation.py` | 2 | 10 |
| `test_recommendation_api.py` | 1 | 5 |
| `test_risk_prediction.py` | 2 | 8 |
| `models/test_risk_predictor.py` | 3 | 23 |
| `models/test_semantic_deduplicator.py` | 2 | 15 |
| `models/test_strategy_recommender.py` | 2 | 15 |
| `models/test_testcase_prioritizer.py` | 2 | 18 |

## 🐛 调试测试

### IDE 调试
1. 在测试方法设置断点（点击行号左侧）
2. 右键点击 ▶️ 图标
3. 选择 "Debug Test"

### 命令行调试
```bash
python3 -m pytest tests/test_file.py::TestClass::test_method --pdb
```

## 📈 查看覆盖率

```bash
# 安装覆盖率工具（如果还没安装）
pip install pytest-cov

# 运行测试并生成覆盖率报告
python3 -m pytest --cov=. --cov-report=html --cov-report=term-missing

# 在浏览器中查看报告
open htmlcov/index.html  # macOS
xdg-open htmlcov/index.html  # Linux
start htmlcov/index.html  # Windows
```

## ⚡ 加速测试执行

```bash
# 安装并行执行插件
pip install pytest-xdist

# 使用所有 CPU 核心并行运行
python3 -m pytest -n auto
```

## 🔍 故障排查

### 问题：看不到运行按钮

**解决方法：**
```bash
# 1. 重新加载窗口
Cmd/Ctrl + Shift + P -> "Developer: Reload Window"

# 2. 配置测试
Cmd/Ctrl + Shift + P -> "Python: Configure Tests"
选择 pytest，然后选择 tests 目录

# 3. 检查 Python 解释器
点击左下角 Python 版本，确保选择了正确的解释器
```

### 问题：测试发现失败

**解决方法：**
```bash
# 检查 pytest 是否正确安装
python3 -m pytest --version

# 手动收集测试
python3 -m pytest --collect-only tests/

# 查看测试日志
打开 "输出" 面板 -> 选择 "Python Test Log"
```

### 问题：导入错误

**解决方法：**
```bash
# 确保在正确的目录
cd ai-service

# 设置 PYTHONPATH
export PYTHONPATH="${PYTHONPATH}:$(pwd)"

# 或在 Python 中
import sys
sys.path.insert(0, '/path/to/ai-service')
```

## 📚 更多资源

- **详细配置说明**：查看 `TEST_SETUP.md`
- **测试指南**：查看 `TESTING_GUIDE.md`
- **pytest 文档**：https://docs.pytest.org/

## 🎯 最佳实践

1. ✅ **每次提交前运行测试**
   ```bash
   python3 -m pytest
   ```

2. ✅ **使用描述性的测试名称**
   ```python
   def test_scenario_1_user_login_with_valid_credentials(self):
       """场景1: 使用有效凭据登录成功"""
   ```

3. ✅ **使用 Given-When-Then 模式**
   ```python
   # Given - 准备测试数据
   request_data = {...}
   
   # When - 执行操作
   response = client.post("/api", json=request_data)
   
   # Then - 验证结果
   assert response.status_code == 200
   ```

4. ✅ **编写独立的测试**
   - 每个测试应该可以独立运行
   - 不要依赖其他测试的执行顺序
   - 使用 fixtures 共享测试数据

5. ✅ **保持测试简单**
   - 一个测试只测试一个场景
   - 避免过于复杂的测试逻辑
   - 使用 mock 隔离外部依赖

## 🎉 开始测试吧！

现在你已经准备好了！试着运行第一个测试：

1. 打开 `test_testcase_generation.py`
2. 找到 `test_scenario_1_generate_cases_with_valid_input` 方法
3. 点击左侧的 ▶️ 按钮
4. 观察测试结果！

祝测试愉快！ 🚀

