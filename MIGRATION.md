# Mentor System 迁移部署指南

## 前提条件

新主机已安装：
- Docker
- Docker Compose
- frpc（内网穿透，自行配置）

## 部署步骤

### 1. 克隆代码

```bash
git clone git@github.com:fal1winter/mentorsys.git
cd mentorsys
```

### 2. 配置环境变量

```bash
export LLM_API_KEY=your_actual_api_key
```

或者创建 `.env` 文件：

```bash
echo "LLM_API_KEY=your_actual_api_key" > .env
```

### 3. 启动所有服务

```bash
docker compose up -d
```

首次启动会构建镜像，需要一段时间。可以查看进度：

```bash
docker compose logs -f
```

### 3. 等待服务就绪

MySQL、Milvus、语义服务都有健康检查，等待全部变为 healthy：

```bash
docker compose ps
```

### 4. 重新索引向量数据

数据库数据会从 SQL 文件自动导入，但向量索引需要手动触发：

```bash
pip install pymysql requests
python deploy/reindex.py
```

### 5. 验证

```bash
# 检查各服务状态
docker compose ps

# 测试语义服务
curl http://localhost:5050/health
curl http://localhost:5050/stats

# 测试后端
curl http://localhost:7020/api/health

# 前端访问
# http://localhost
```

## 服务端口

| 服务 | 端口 |
|------|------|
| 前端 (Nginx) | 80 |
| 后端 (Spring Boot) | 7020 |
| 语义服务 (Python) | 5050 |
| MySQL | 3306 |
| Redis | 6379 |
| Milvus | 19530 |

## frpc 配置参考

根据你的 frps 服务器配置，将 80 端口（或 7020）穿透出去：

```ini
[common]
server_addr = your_frps_server
server_port = 7000
token = your_token

[mentor-web]
type = http
local_ip = 127.0.0.1
local_port = 80
custom_domains = your_domain
```

## 常用命令

```bash
# 停止所有服务
docker compose down

# 停止并删除数据卷（慎用，会清空数据）
docker compose down -v

# 重新构建某个服务
docker compose build backend
docker compose up -d backend

# 查看日志
docker compose logs -f backend
docker compose logs -f semantic
```

## 注意事项

- `deploy/mysql/init/mentor_system_dump.sql.gz` 包含初始数据，首次启动 MySQL 时自动导入
- 向量数据不在 SQL 中，启动后必须运行 `deploy/reindex.py`
- LLM API Key 在 `backend/src/main/resources/application.yml` 中，如需更换请修改后重新构建后端镜像
