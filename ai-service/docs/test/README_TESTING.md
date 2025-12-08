# 🧪 AI-Service 测试配置

## ✅ 配置完成

测试运行按钮已配置完成！每个测试方法左侧都会显示 ▶️ 运行按钮。

## 🚀 快速开始

### 3 步启动

1️⃣ **重新加载 IDE**
```
Cmd/Ctrl + Shift + P -> "Developer: Reload Window"
```

2️⃣ **打开测试文件**
```
ai-service/tests/test_testcase_generation.py
```

3️⃣ **点击运行按钮**
```
点击测试方法左侧的 ▶️
```

详细说明请查看：**[GET_STARTED_WITH_TESTING.md](GET_STARTED_WITH_TESTING.md)**

## 📊 测试概览

- ✅ **94 个测试用例**
- ✅ **7 个测试文件**
- ✅ **15 个测试类**
- ✅ **所有测试都有运行按钮**

## 📚 文档索引

### 🎯 新手入门
1. **[GET_STARTED_WITH_TESTING.md](GET_STARTED_WITH_TESTING.md)** - 3 步开始使用
2. **[ai-service/tests/QUICK_START.md](ai-service/tests/QUICK_START.md)** - 快速启动指南

### 📖 详细文档
3. **[ai-service/tests/TEST_SETUP.md](ai-service/tests/TEST_SETUP.md)** - 详细配置说明
4. **[.vscode/README.md](.vscode/README.md)** - IDE 配置说明

### 📋 参考资料
5. **[TESTING_CONFIGURATION_CHECKLIST.md](TESTING_CONFIGURATION_CHECKLIST.md)** - 配置清单
6. **[CONFIGURATION_SUMMARY.md](CONFIGURATION_SUMMARY.md)** - 配置摘要
7. **[ai-service/TEST_RUNNER_SETUP_SUMMARY.md](ai-service/TEST_RUNNER_SETUP_SUMMARY.md)** - 配置总结

## 💡 使用方式

### IDE 运行按钮（推荐）
```
点击测试方法左侧的 ▶️ 按钮
右键选择 "Debug Test" 调试
```

### 测试资源管理器
```
侧边栏 🧪 图标 -> 浏览测试树 -> 点击运行
```

### 命令行
```bash
cd ai-service
python3 -m pytest                    # 运行所有测试
python3 -m pytest tests/test_*.py    # 运行特定文件
python3 -m pytest -v -s              # 详细输出
```

### VSCode 任务
```
Cmd/Ctrl + Shift + P -> "Tasks: Run Task" -> 选择任务
```

## 🔧 可用任务

- pytest: Run All Tests
- pytest: Run Current File
- pytest: Run with Coverage
- pytest: Run Failed Tests Only
- pytest: Run API Tests
- pytest: Run Model Tests

## 🐛 调试

右键点击 ▶️ -> "Debug Test" -> 设置断点即可

## ⚡ 常用命令

```bash
# 进入测试目录
cd ai-service

# 运行所有测试
python3 -m pytest

# 运行特定测试
python3 -m pytest tests/test_testcase_generation.py::TestAITestCaseGenerationAPI::test_scenario_1_generate_cases_with_valid_input

# 显示详细输出
python3 -m pytest -v -s

# 只运行失败的测试
python3 -m pytest --lf

# 生成覆盖率报告
python3 -m pytest --cov=. --cov-report=html
open htmlcov/index.html
```

## 🛠️ 故障排查

### 看不到运行按钮？
1. 重新加载窗口：`Cmd/Ctrl + Shift + P` -> "Developer: Reload Window"
2. 配置测试：`Cmd/Ctrl + Shift + P` -> "Python: Configure Tests"
3. 选择 Python 解释器：点击左下角 Python 版本

### 测试运行失败？
1. 确保在 `ai-service` 目录
2. 检查 pytest 版本：`python3 -m pytest --version`
3. 查看测试日志：输出面板 -> "Python Test Log"

详细故障排查请查看：**[ai-service/tests/TEST_SETUP.md](ai-service/tests/TEST_SETUP.md)**

## 📈 测试覆盖率

```bash
# 安装覆盖率工具
pip install pytest-cov

# 生成报告
python3 -m pytest --cov=. --cov-report=html --cov-report=term-missing

# 查看报告
open htmlcov/index.html
```

## ⚡ 并行测试

```bash
# 安装并行执行插件
pip install pytest-xdist

# 并行运行
python3 -m pytest -n auto
```

## 🎓 最佳实践

1. ✅ 提交前运行所有测试
2. ✅ 使用描述性的测试名称
3. ✅ 使用 Given-When-Then 模式
4. ✅ 保持测试独立可运行
5. ✅ 使用 fixtures 共享测试数据

## 📞 获取帮助

遇到问题？按顺序查看：

1. **[GET_STARTED_WITH_TESTING.md](GET_STARTED_WITH_TESTING.md)** - 入门指南
2. **[ai-service/tests/QUICK_START.md](ai-service/tests/QUICK_START.md)** - 快速解决方案
3. **[ai-service/tests/TEST_SETUP.md](ai-service/tests/TEST_SETUP.md)** - 详细故障排查

## 🎉 享受测试！

现在你可以：
- ✅ 快速运行单个测试
- ✅ 方便地调试测试
- ✅ 使用测试资源管理器
- ✅ 生成覆盖率报告
- ✅ 并行执行测试

**开始你的高效测试之旅吧！** 🚀

---

**配置日期：** 2025-11-26  
**pytest 版本：** 8.4.1  
**Python 版本：** 3.10.11

