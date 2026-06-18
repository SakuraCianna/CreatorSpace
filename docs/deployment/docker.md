# CreatorSpace Docker 部署手册

本文档面向单机 Docker Compose 部署。默认拓扑为 Nginx 前端入口、Spring Boot 后端、PostgreSQL pgvector、Redis，并使用 Docker 卷持久化数据库、Redis AOF 和上传文件。

## 部署架构

```text
Browser
  |
  v
frontend nginx :80
  |-- /             -> Vue 静态资源
  |-- /api/**       -> backend:8080
  |-- /uploads/**   -> backend:8080

backend
  |-- PostgreSQL pgvector
  |-- Redis
  |-- uploads volume
```

## 文件说明

```text
docker-compose.yml
.env.example
backend/Dockerfile
frontend/Dockerfile
frontend/nginx.conf
```

## 首次部署

1. 在项目根目录创建 `.env`。

```powershell
Copy-Item .\.env.example .\.env
```

也可以手动创建 `.env`，Docker Compose 会自动读取项目根目录下的该文件。

2. 编辑根目录 `.env`。

必须替换:

- `JWT_SECRET`
- `ADMIN_PASSWORD_HASH`
- `POSTGRES_PASSWORD`
- `REDIS_PASSWORD`
- `WEB_BASE_URL`
- `API_BASE_URL`
- `CORS_ALLOWED_ORIGINS`

BCrypt 哈希包含 `$` 时，建议用单引号包住完整值，例如 `ADMIN_PASSWORD_HASH='$2a$10$...'`；也可以将 `$` 写成 `$$` 来转义，避免 Docker Compose 把哈希片段当成变量插值。

3. 解析 Compose 配置。

```powershell
docker compose config --quiet
```

不要把 `docker compose config` 的完整输出粘贴到日志、PR 或工单中，它会展开环境变量中的敏感值。

4. 构建镜像。

```powershell
docker compose build
```

5. 启动服务。

```powershell
docker compose up -d
```

6. 查看状态。

```powershell
docker compose ps
```

7. 验证健康检查。

```powershell
Invoke-RestMethod -Uri "http://localhost/api/health"
```

如果 `HTTP_PORT` 不是 `80`，请把 URL 改成对应端口。

## 日常更新

1. 拉取最新代码。

```powershell
git pull --ff-only origin main
```

2. 重新构建镜像。

```powershell
docker compose build
```

3. 增量重启服务。

```powershell
docker compose up -d
```

4. 验证健康检查。

```powershell
Invoke-RestMethod -Uri "http://localhost/api/health"
```

## 数据与备份

Docker Compose 使用以下持久卷:

```text
creatorspace_postgres_data
creatorspace_redis_data
creatorspace_uploads
```

数据库结构由 Flyway 在后端启动时自动迁移。生产环境更新前必须先备份 PostgreSQL 数据和上传文件。

备份 PostgreSQL:

```powershell
$envValues = Get-Content .\.env | Where-Object { $_ -match "^[A-Z0-9_]+=" } | ConvertFrom-StringData
docker compose exec postgres pg_dump -U $envValues.POSTGRES_USERNAME -d $envValues.POSTGRES_DB -Fc -f /tmp/creatorspace.dump
docker compose cp postgres:/tmp/creatorspace.dump .\backups\creatorspace.dump
```

备份上传文件:

```powershell
New-Item -ItemType Directory -Force .\backups\uploads
docker compose cp backend:/app/storage/uploads .\backups\uploads
```

## 回滚方式

1. 回退代码到上一个已知可用 commit。

```powershell
git checkout main
git pull --ff-only origin main
git revert <commit-sha>
```

2. 重新构建并启动。

```powershell
docker compose build
docker compose up -d
```

3. 如果数据库迁移已经写入不可逆结构变更，按 `docs/database/backup-and-migration-rules.md` 从备份恢复。

## 停止服务

仅停止容器，不删除卷:

```powershell
docker compose stop
```

停止并删除容器和网络，但保留卷:

```powershell
docker compose down
```

不要在生产环境执行 `docker compose down -v`，除非已经确认要删除数据库、Redis 和上传文件。
