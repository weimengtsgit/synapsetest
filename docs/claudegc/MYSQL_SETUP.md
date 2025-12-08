# MySQL Docker 快速启动指南

本项目提供了便捷的 MySQL Docker 启动脚本，用于本地开发和测试。

## 快速开始

### 1. 启动 MySQL

```bash
./start_mysql.sh
```

**默认配置：**
- **镜像版本**: mysql:8.0.40
- **端口**: 3306
- **Root 密码**: synapsetest123
- **默认数据库**: synapsetest
- **字符集**: utf8mb4
- **时区**: Asia/Shanghai

### 2. 连接数据库

#### 方式一：使用 docker exec
```bash
docker exec -it mysql-synapsetest mysql -uroot -psynapsetest123
```

#### 方式二：使用 MySQL 客户端
```bash
mysql -h 127.0.0.1 -P 3306 -uroot -psynapsetest123 synapsetest
```

#### 方式三：使用配置文件连接（推荐）
```bash
# Backend 配置 (application.yml)
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/synapsetest?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Shanghai
    username: root
    password: synapsetest123

# AI Service 配置 (.env)
MYSQL_HOST=localhost
MYSQL_PORT=3306
MYSQL_USER=root
MYSQL_PASSWORD=synapsetest123
MYSQL_DATABASE=synapsetest
```

### 3. 停止 MySQL

```bash
./stop_mysql.sh
```

## 自定义配置

### 修改默认密码和数据库

启动前设置环境变量：

```bash
export MYSQL_ROOT_PASSWORD=your_password
export MYSQL_DATABASE=your_database
export MYSQL_PORT=3307

./start_mysql.sh
```

### 修改 MySQL 配置

编辑配置文件（启动后生成）：

```bash
vim ./volumes/mysql/conf/my.cnf
```

修改后重启容器生效：

```bash
docker restart mysql-synapsetest
```

## 数据持久化

所有数据持久化在 `./volumes/mysql/` 目录：

```
volumes/mysql/
├── data/          # 数据库文件
├── conf/          # 配置文件
│   └── my.cnf     # 自定义配置
└── logs/          # MySQL 日志
    └── slow.log   # 慢查询日志
```

**注意：** 删除 `./volumes/mysql/data/` 将清空所有数据！

## 常用操作

### 查看日志

```bash
# 查看实时日志
docker logs -f mysql-synapsetest

# 查看慢查询日志
cat ./volumes/mysql/logs/slow.log
```

### 备份数据库

```bash
# 备份单个数据库
docker exec mysql-synapsetest mysqldump -uroot -psynapsetest123 synapsetest > backup.sql

# 备份所有数据库
docker exec mysql-synapsetest mysqldump -uroot -psynapsetest123 --all-databases > all_backup.sql
```

### 恢复数据库

```bash
# 恢复数据库
docker exec -i mysql-synapsetest mysql -uroot -psynapsetest123 synapsetest < backup.sql
```

### 执行 SQL 文件

```bash
# 方式一：从容器外执行
docker exec -i mysql-synapsetest mysql -uroot -psynapsetest123 synapsetest < schema.sql

# 方式二：复制到容器内执行
docker cp schema.sql mysql-synapsetest:/tmp/
docker exec mysql-synapsetest mysql -uroot -psynapsetest123 synapsetest -e "source /tmp/schema.sql"
```

### 创建新用户

```bash
docker exec -it mysql-synapsetest mysql -uroot -psynapsetest123 -e "
CREATE USER 'testuser'@'%' IDENTIFIED BY 'testpass';
GRANT ALL PRIVILEGES ON synapsetest.* TO 'testuser'@'%';
FLUSH PRIVILEGES;
"
```

## 性能优化配置

默认配置已针对开发环境优化，主要配置项：

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `max_connections` | 200 | 最大连接数 |
| `max_allowed_packet` | 64M | 最大数据包大小 |
| `innodb_buffer_pool_size` | 256M | InnoDB 缓冲池大小 |
| `slow_query_log` | 开启 | 慢查询日志 |
| `long_query_time` | 2秒 | 慢查询阈值 |

## 故障排查

### 容器启动失败

```bash
# 查看详细日志
docker logs mysql-synapsetest

# 常见问题：
# 1. 端口被占用 -> 修改 MYSQL_PORT 环境变量
# 2. 数据目录权限问题 -> rm -rf ./volumes/mysql/data && 重新启动
# 3. 配置文件语法错误 -> 检查 ./volumes/mysql/conf/my.cnf
```

### 无法连接数据库

```bash
# 检查容器状态
docker ps | grep mysql-synapsetest

# 检查网络连接
docker exec mysql-synapsetest mysqladmin ping -h localhost -uroot -psynapsetest123

# 检查端口绑定
docker port mysql-synapsetest
```

### 重置 MySQL

完全重置（**警告：会删除所有数据**）：

```bash
# 停止并删除容器
docker stop mysql-synapsetest
docker rm mysql-synapsetest

# 删除数据目录
rm -rf ./volumes/mysql/

# 重新启动
./start_mysql.sh
```

## 与项目集成

### Backend 服务配置

在 `backend/src/main/resources/application.yml` 中：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/synapsetest?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Shanghai
    username: root
    password: synapsetest123
    driver-class-name: com.mysql.cj.jdbc.Driver
```

### AI Service 配置

在 `ai-service/.env` 中：

```bash
MYSQL_HOST=localhost
MYSQL_PORT=3306
MYSQL_USER=root
MYSQL_PASSWORD=synapsetest123
MYSQL_DATABASE=synapsetest
```

### 初始化数据库表

```bash
# Backend 会自动创建表（Mybatis/JPA）
# 或手动执行 SQL 脚本
docker exec -i mysql-synapsetest mysql -uroot -psynapsetest123 synapsetest < backend/src/main/resources/schema.sql
```

## 生产环境注意事项

⚠️ **本脚本仅用于开发和测试环境！**

生产环境建议：
1. 使用强密码
2. 限制 root 远程访问
3. 配置主从复制
4. 定期备份
5. 监控性能指标
6. 使用云服务或 Kubernetes 部署

## 相关脚本

- `start_mysql.sh` - 启动 MySQL
- `stop_mysql.sh` - 停止 MySQL  
- `start_qdrant.sh` - 启动 Qdrant 向量数据库
- `start_milvus.sh` - 启动 Milvus 向量数据库

## 帮助

遇到问题？
1. 查看容器日志：`docker logs mysql-synapsetest`
2. 检查配置文件：`cat ./volumes/mysql/conf/my.cnf`
3. 测试连接：`docker exec mysql-synapsetest mysqladmin ping`

---

**版本**: MySQL 8.0.40  
**最后更新**: 2024-12-02
