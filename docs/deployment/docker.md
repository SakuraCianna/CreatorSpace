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
backend/Dockerfile
frontend/Dockerfile
frontend/nginx.conf
deploy/docker/production.env.example
```

## 首次部署

1. 复制部署环境模板。

```powershell
Copy-Item .\deploy\docker\production.env.example .\deploy\docker\production.env
```

2. 编辑 `deploy/docker/production.env`。

必须替换:

- `JWT_SECRET`
- `ADMIN_PASSWORD_HASH`
- `POSTGRES_PASSWORD`
- `REDIS_PASSWORD`
- `WEB_BASE_URL`
- `API_BASE_URL`
- `CORS_ALLOWED_ORIGINS`

3. 运行生产环境预检，确认没有占位密钥。

```powershell
.\deploy\docker\Test-ProductionEnv.ps1 -EnvFile .\deploy\docker\production.env
```

4. 解析 Compose 配置。

```powershell
docker compose --env-file .\deploy\docker\production.env config --quiet
```

不要把 `docker compose config` 的完整输出粘贴到日志、PR 或工单中，它会展开环境变量中的敏感值。

5. 构建镜像。

```powershell
docker compose --env-file .\deploy\docker\production.env build
```

6. 启动服务。

```powershell
docker compose --env-file .\deploy\docker\production.env up -d
```

7. 查看状态。

```powershell
docker compose --env-file .\deploy\docker\production.env ps
```

8. 验证健康检查。

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
docker compose --env-file .\deploy\docker\production.env build
```

3. 增量重启服务。

```powershell
docker compose --env-file .\deploy\docker\production.env up -d
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
$envValues = Get-Content .\deploy\docker\production.env | Where-Object { $_ -match "^[A-Z0-9_]+=" } | ConvertFrom-StringData
docker compose --env-file .\deploy\docker\production.env exec postgres pg_dump -U $envValues.POSTGRES_USERNAME -d $envValues.POSTGRES_DB -Fc -f /tmp/creatorspace.dump
docker compose --env-file .\deploy\docker\production.env cp postgres:/tmp/creatorspace.dump .\backups\creatorspace.dump
```

备份上传文件:

```powershell
New-Item -ItemType Directory -Force .\backups\uploads
docker compose --env-file .\deploy\docker\production.env cp backend:/app/storage/uploads .\backups\uploads
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
docker compose --env-file .\deploy\docker\production.env build
docker compose --env-file .\deploy\docker\production.env up -d
```

3. 如果数据库迁移已经写入不可逆结构变更，按 `docs/database/backup-and-migration-rules.md` 从备份恢复。

## 停止服务

仅停止容器，不删除卷:

```powershell
docker compose --env-file .\deploy\docker\production.env stop
```

停止并删除容器和网络，但保留卷:

```powershell
docker compose --env-file .\deploy\docker\production.env down
```

不要在生产环境执行 `docker compose down -v`，除非已经确认要删除数据库、Redis 和上传文件。
