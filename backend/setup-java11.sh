#!/bin/bash
# 设置 Java 11 环境

# 查找 Java 11 安装路径
JAVA11_HOME=$(/usr/libexec/java_home -v 11 2>/dev/null)

if [ -z "$JAVA11_HOME" ]; then
    echo "错误：未找到 Java 11 安装"
    echo "请先安装 Java 11："
    echo "  brew install openjdk@11"
    exit 1
fi

echo "找到 Java 11: $JAVA11_HOME"
export JAVA_HOME="$JAVA11_HOME"
export PATH="$JAVA_HOME/bin:$PATH"

echo "当前 Java 版本："
java -version

echo ""
echo "环境已设置！现在可以运行："
echo "  cd /Users/mengwei/ww/github/synapsetest/backend"
echo "  mvn clean compile"
echo "  mvn spring-boot:run"

