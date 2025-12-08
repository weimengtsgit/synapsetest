# 🚀 测试运行按钮 - 开始使用

## ✅ 配置已完成！

恭喜！测试运行按钮已经配置完成。现在每个测试方法左边都会显示 ▶️ 运行按钮。

## 🎯 3 步开始使用

### 步骤 1：重新加载 IDE

按 `Cmd/Ctrl + Shift + P`，输入并选择：
```
Developer: Reload Window
```

或者直接重启 VSCode/Cursor。

### 步骤 2：打开测试文件

打开任意测试文件，例如：
```
ai-service/tests/test_testcase_generation.py
```

### 步骤 3：点击运行按钮

找到任意测试方法，例如：
```python
def test_scenario_1_generate_cases_with_valid_input(self):  # ◀ 这里会显示 ▶️
    """场景1: 有效需求输入成功生成测试用例"""
    # ... 测试代码
```

点击左侧的 ▶️ 按钮即可运行测试！

## 📊 当前测试统计

你的项目包含：
- ✅ **94 个测试用例**
- ✅ **7 个测试文件**  
- ✅ **15 个测试类**

所有测试都已配置好运行按钮！

## 🔍 如何找到测试

### 方法 1：通过文件浏览器
```
ai-service/tests/
├── test_testcase_generation.py    (10 个测试)
├── test_recommendation_api.py     (5 个测试)
├── test_risk_prediction.py        (8 个测试)
└── models/
    ├── test_risk_predictor.py     (23 个测试)
    ├── test_semantic_deduplicator.py (15 个测试)
    ├── test_strategy_recommender.py  (15 个测试)
    └── test_testcase_prioritizer.py  (18 个测试)
```

### 方法 2：通过测试资源管理器
1. 点击 VSCode 侧边栏的 🧪 图标
2. 浏览完整的测试树
3. 点击任意测试旁的运行按钮

## 💡 5 种运行测试的方式

### 1️⃣ IDE 运行按钮（最推荐）⭐⭐⭐⭐⭐
点击测试方法左侧的 ▶️ 按钮

### 2️⃣ 测试资源管理器 ⭐⭐⭐⭐
侧边栏 🧪 图标 -> 选择测试 -> 点击运行

### 3️⃣ VSCode 任务 ⭐⭐⭐⭐
`Cmd/Ctrl + Shift + P` -> "Tasks: Run Task" -> 选择任务

### 4️⃣ 命令面板 ⭐⭐⭐
`Cmd/Ctrl + Shift + P` -> "Test: Run" -> 选择测试

### 5️⃣ 命令行 ⭐⭐⭐
```bash
cd ai-service
python3 -m pytest
```

## 🐛 调试测试

在测试方法左侧：
1. **右键点击** ▶️ 按钮
2. 选择 "**Debug Test**"
3. 在代码中设置断点即可调试

## ⚡ 常用快捷操作

### 运行所有测试
```bash
cd ai-service
python3 -m pytest
```

### 运行特定文件
```bash
python3 -m pytest tests/test_testcase_generation.py
```

### 运行特定测试
```bash
python3 -m pytest tests/test_testcase_generation.py::TestAITestCaseGenerationAPI::test_scenario_1_generate_cases_with_valid_input
```

### 显示详细输出
```bash
python3 -m pytest -v -s
```

### 只运行失败的测试
```bash
python3 -m pytest --lf
```

## 🎓 可用的 VSCode 任务

按 `Cmd/Ctrl + Shift + P`，输入 "Tasks: Run Task"，选择：

1. **pytest: Run All Tests** - 运行所有测试
2. **pytest: Run Current File** - 运行当前文件
3. **pytest: Run with Coverage** - 生成覆盖率报告
4. **pytest: Run Failed Tests Only** - 只运行失败的测试
5. **pytest: Run API Tests** - 只运行 API 测试
6. **pytest: Run Model Tests** - 只运行模型测试
7. **pytest: Collect Tests Only** - 收集测试（不运行）

## 🔧 故障排查

### 问题：看不到运行按钮？

**解决方案 1：** 重新加载窗口
```
Cmd/Ctrl + Shift + P -> "Developer: Reload Window"
```

**解决方案 2：** 配置测试框架
```
Cmd/Ctrl + Shift + P -> "Python: Configure Tests"
选择: pytest
选择: tests
```

**解决方案 3：** 检查 Python 解释器
```
点击左下角的 Python 版本
选择: python3 或你的虚拟环境
```

### 问题：测试运行失败？

**检查 1：** 确保在正确的目录
```bash
cd ai-service
```

**检查 2：** 确保 pytest 已安装
```bash
python3 -m pytest --version
```

**检查 3：** 查看测试日志
```
打开 "输出" 面板 -> 选择 "Python Test Log"
```

## 📚 详细文档

需要更多信息？查看这些文档：

### 快速参考
- 📄 **快速启动指南：** `ai-service/tests/QUICK_START.md`
- 📄 **IDE 配置说明：** `.vscode/README.md`

### 详细文档
- 📄 **详细配置说明：** `ai-service/tests/TEST_SETUP.md`
- 📄 **配置完成总结：** `ai-service/TEST_RUNNER_SETUP_SUMMARY.md`
- 📄 **配置清单：** `TESTING_CONFIGURATION_CHECKLIST.md`
- 📄 **配置摘要：** `CONFIGURATION_SUMMARY.md`

## 🎉 开始测试吧！

现在一切都准备好了！试着：

1. ✅ 打开 `test_testcase_generation.py`
2. ✅ 找到 `test_scenario_1_generate_cases_with_valid_input`
3. ✅ 点击左侧的 ▶️ 按钮
4. ✅ 看到测试通过的绿色✓

**享受高效的测试开发体验！** 🚀

---

**提示：** 如果这是你第一次使用，建议先阅读 `ai-service/tests/QUICK_START.md` 获取更详细的说明。

