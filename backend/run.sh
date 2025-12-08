#!/bin/bash
# 简单的启动脚本 - 直接使用 java 命令运行编译后的类

cd "$(dirname "$0")"

echo "正在编译项目..."
javac -d target/classes -cp "$(find ~/.m2/repository -name '*.jar' | tr '\n' ':')" \
  $(find src/main/java -name '*.java')

if [ $? -eq 0 ]; then
    echo "编译成功！正在启动应用..."
    java -cp "target/classes:$(find ~/.m2/repository -name '*.jar' | tr '\n' ':')" \
      com.synapsetest.testmanagement.TestManagementApplication
else
    echo "编译失败！"
    exit 1
fi

