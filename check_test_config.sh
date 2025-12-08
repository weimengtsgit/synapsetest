#!/bin/bash

echo "========================================="
echo "测试配置诊断工具"
echo "========================================="
echo ""

# 进入 ai-service 目录
cd ai-service

echo "1. 检查 Python 版本..."
.venv/bin/python --version
echo ""

echo "2. 检查 pytest 是否安装..."
.venv/bin/python -m pytest --version
echo ""

echo "3. 检查测试发现..."
.venv/bin/python -m pytest --collect-only tests/ -q | head -20
echo ""

echo "4. 检查虚拟环境路径..."
echo "虚拟环境位置: $(pwd)/.venv"
ls -la .venv/bin/python
echo ""

echo "5. 检查配置文件..."
if [ -f "../.vscode/settings.json" ]; then
    echo "✓ .vscode/settings.json 存在"
else
    echo "✗ .vscode/settings.json 不存在"
fi

if [ -f "pytest.ini" ]; then
    echo "✓ pytest.ini 存在"
else
    echo "✗ pytest.ini 不存在"
fi
echo ""

echo "6. 运行一个简单测试..."
.venv/bin/python -m pytest tests/test_testcase_generation.py::TestAITestCaseGenerationAPI::test_scenario_7_health_check -v
echo ""

echo "========================================="
echo "诊断完成！"
echo "========================================="
echo ""
echo "接下来的步骤："
echo "1. 完全退出并重新打开 Cursor/VSCode"
echo "2. 打开项目文件夹"
echo "3. 选择 Python 解释器: ai-service/.venv/bin/python"
echo "4. 按 Cmd+Shift+P -> 'Python: Configure Tests' -> 选择 pytest"
echo "5. 打开测试文件，应该能看到运行按钮"
echo ""


