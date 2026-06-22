# CreatorSpace

CreatorSpace 是一个个人主题风格博客与创意作品展示平台，定位为“有艺术感的个人博客 + 像作品展厅一样展示个人项目和创意成果的平台”。项目同时覆盖公开浏览体验、内容管理后台、主题配置、站点设置、文件资产、评论审核、搜索与基础部署链路。

## 项目状态

| 模块 | 状态 | 说明 |
| --- | --- | --- |
| 产品主界面 | 已实现 | 沉浸式首页，使用 GSAP、Lenis、Three.js 等动效能力 |
| 公开内容页 | 已接入 API | 文章、作品、灵感、搜索、关于页、评论 |
| 认证体系 | 已接入 API | 普通用户注册/登录，管理员登录 |
| 创作中心 | 已接入 API | 普通用户可写博客、上传作品、管理创作图片并查看审核状态 |
| CMS 后台 | 持续完善 | 管理员负责内容审核、运营推荐、灵感、评论、文件、主题和站点设置 |
| 数据库迁移 | 已接入 | Flyway 管理 PostgreSQL schema 和演示数据 |
| Docker 部署 | 已配置 | Compose 编排前端、后端、PostgreSQL pgvector、Redis |
| CI/CD | 已配置基础链路 | CI 运行后端测试、前端审计和构建，CD 生成交付产物 |

仍在推进的能力包括内容规则、主题预览、高级运营辅助和更完整的生产级可观测性。

## 技术栈

### 后端

- Java 21
- Spring Boot 3.5.7
- Spring Security
- Spring Web MVC
- Spring Actuator
- MyBatis-Plus
- Flyway
- PostgreSQL 17 / pgvector
- Redis 7
- Maven
- Testcontainers

### 前端

- Node.js 24
- Vue 3
- TypeScript
- Vite 8
- Vue Router
- Pinia
- GSAP
- Lenis
- Three.js
- anime.js
- markdown-it
- highlight.js
- @lucide/vue

### 部署

- Docker
- Docker Compose
- Nginx
- PostgreSQL pgvector
- Redis AOF

## 目录结构

```text
CreatorSpace
├── .github
│   └── workflows
├── backend
│   ├── Dockerfile
│   ├── pom.xml
│   └── src
│       ├── main
│       │   ├── java/com/creatorspace
│       │   └── resources/db/migration
│       └── test
├── docs
│   ├── database
│   ├── deployment
│   ├── product
│   └── superpowers
├── frontend
│   ├── Dockerfile
│   ├── nginx.conf
│   ├── package.json
│   ├── vite.config.ts
│   └── src
├── docker-compose.yml
├── .env.example
├── .gitignore
├── README.md
└── 需求文档.md
```

## 架构概览

```text
Browser
  |
  v
Nginx frontend container
  |-- /             -> Vue static files
  |-- /api/**       -> Spring Boot backend
  |-- /uploads/**   -> Spring Boot local upload mapping

Spring Boot backend
  |-- PostgreSQL pgvector
  |-- Redis
  |-- Docker volume for uploaded assets
```

前端生产构建默认使用同源 `/api`，Docker 部署时由 Nginx 统一反向代理到后端，避免浏览器跨域配置漂移。后端不直接暴露到公网端口，数据库、Redis 和上传文件均使用 Docker 卷持久化。

## 快速开始

### 环境要求

| 工具 | 推荐版本 |
| --- | --- |
| JDK | 21 |
| Maven | 3.9+ |
| Node.js | 24 |
| npm | 11+ |
| Docker Desktop | 26+ |
| Docker Compose | v2 |
| PostgreSQL | 17 |
| Redis | 7 |

### 本地开发

1. 创建本地环境文件。

```powershell
Copy-Item .\.env.example .\.env
```

2. 编辑 `.env`，至少配置:

- `JWT_SECRET`
- `ADMIN_PASSWORD_HASH`
- `POSTGRES_PASSWORD`
- `REDIS_PASSWORD`

3. 安装前端依赖。

```powershell
npm install --prefix frontend
```

4. 启动后端。

```powershell
mvn -f backend/pom.xml spring-boot:run
```

5. 启动前端。

```powershell
npm run dev --prefix frontend
```

后端已接入 Spring Boot DevTools，本地通过 `spring-boot:run` 启动时，代码变更会触发开发重启。

前端保持 Vite 本机开发服务的简单配置，未额外增加局域网访问配置。

6. 验证健康检查。

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/health"
```

也可以在项目根目录执行:

```powershell
.\快速启动.bat
```

## Docker 部署

Docker 部署使用四个服务:

- `frontend`: Nginx + Vue 静态资源
- `backend`: Spring Boot API
- `postgres`: PostgreSQL pgvector
- `redis`: Redis AOF

### 首次部署

1. 在项目根目录创建 `.env`。

```powershell
Copy-Item .\.env.example .\.env
```

也可以手动创建 `.env`，Docker Compose 会自动读取项目根目录下的该文件。

2. 编辑根目录 `.env`，替换所有占位值。

必须重点配置:

- `JWT_SECRET`
- `ADMIN_PASSWORD_HASH`
- `POSTGRES_PASSWORD`
- `REDIS_PASSWORD`
- `WEB_BASE_URL`
- `API_BASE_URL`
- `CORS_ALLOWED_ORIGINS`

3. 校验 Compose 配置。

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

6. 查看服务状态。

```powershell
docker compose ps
```

7. 验证入口健康检查。

```powershell
Invoke-RestMethod -Uri "http://localhost/api/health"
```

如果 `HTTP_PORT` 不是 `80`，请改为对应端口。

详细部署、备份、回滚和停止服务说明见 [Docker 部署手册](docs/deployment/docker.md)。

## 环境变量

根目录 `.env.example` 是本地开发和 Docker 部署的统一模板。真实 `.env` 由使用者在项目根目录自行创建，不提交仓库。

核心变量:

| 变量 | 说明 |
| --- | --- |
| `JWT_SECRET` | JWT 签名密钥，必须使用强随机字符串 |
| `ADMIN_USERNAME` | 初始化管理员用户名 |
| `ADMIN_PASSWORD_HASH` | 初始化管理员 BCrypt 密码哈希 |
| `POSTGRES_DB` | PostgreSQL 数据库名 |
| `POSTGRES_USERNAME` | PostgreSQL 用户名 |
| `POSTGRES_PASSWORD` | PostgreSQL 密码 |
| `REDIS_PASSWORD` | Redis 密码 |
| `WEB_BASE_URL` | 站点公网地址 |
| `API_BASE_URL` | API 公网地址 |
| `CORS_ALLOWED_ORIGINS` | 允许跨域来源 |
| `LOCAL_FILE_STORAGE_ROOT` | 后端上传文件存储目录 |
| `AI_ENABLED` | AI 功能开关 |

`.env` 中不要写入明文管理员密码，只写 BCrypt 哈希。

Docker Compose 会读取根目录 `.env` 做变量插值。BCrypt 哈希包含 `$` 时，建议用单引号包住完整值，例如 `ADMIN_PASSWORD_HASH='$2a$10$...'`；也可以将 `$` 写成 `$$` 来转义。

## 数据库迁移

迁移脚本位于:

```text
backend/src/main/resources/db/migration
```

当前迁移:

```text
V1__initialize_creator_space_schema.sql
V2__seed_showcase_data.sql
```

后端启动时 Flyway 会自动执行未应用迁移。项目产生真实持久数据后，不要修改已经执行过的迁移脚本；任何结构变更都应新增 `V版本号__描述.sql`。

数据库变更前备份规则见 [数据库备份与迁移规则](docs/database/backup-and-migration-rules.md)。

## 核心 API

### 公开接口

```text
POST   /api/auth/register
POST   /api/auth/login
GET    /api/site/config
GET    /api/theme/current
GET    /api/themes
GET    /api/search
GET    /api/categories
GET    /api/tags
GET    /api/articles
GET    /api/articles/slug/{slug}
GET    /api/projects
GET    /api/projects/slug/{slug}
GET    /api/inspirations
GET    /api/comments
POST   /api/comments
GET    /api/health
```

### 管理接口

```text
POST   /api/admin/auth/login
GET    /api/admin/dashboard/overview
GET    /api/admin/articles
POST   /api/admin/articles
GET    /api/admin/articles/{id}
PUT    /api/admin/articles/{id}
PUT    /api/admin/articles/{id}/publish
PUT    /api/admin/articles/{id}/unpublish
PUT    /api/admin/articles/{id}/approve
PUT    /api/admin/articles/{id}/reject
PUT    /api/admin/articles/{id}/top
PUT    /api/admin/articles/{id}/recommend
DELETE /api/admin/articles/{id}
GET    /api/admin/projects
POST   /api/admin/projects
GET    /api/admin/projects/{id}
PUT    /api/admin/projects/{id}
PUT    /api/admin/projects/{id}/status
PUT    /api/admin/projects/{id}/approve
PUT    /api/admin/projects/{id}/reject
PUT    /api/admin/projects/{id}/recommend
DELETE /api/admin/projects/{id}
GET    /api/admin/inspirations
POST   /api/admin/inspirations
PUT    /api/admin/inspirations/{id}
DELETE /api/admin/inspirations/{id}
GET    /api/admin/comments
PUT    /api/admin/comments/{id}/approve
PUT    /api/admin/comments/{id}/reject
GET    /api/admin/files
POST   /api/admin/files/upload
GET    /api/admin/themes
PUT    /api/admin/themes/{id}
PUT    /api/admin/themes/{id}/switch
GET    /api/admin/site/settings
PUT    /api/admin/site/settings
```

管理接口需要管理员 JWT。

### 登录用户接口

```text
GET    /api/creator/articles
POST   /api/creator/articles
GET    /api/creator/articles/{id}
PUT    /api/creator/articles/{id}
PUT    /api/creator/articles/{id}/submit
DELETE /api/creator/articles/{id}
GET    /api/creator/projects
POST   /api/creator/projects
GET    /api/creator/projects/{id}
PUT    /api/creator/projects/{id}
PUT    /api/creator/projects/{id}/submit
DELETE /api/creator/projects/{id}
GET    /api/creator/files
POST   /api/creator/files/upload
GET    /api/me/likes
POST   /api/me/likes
DELETE /api/me/likes
GET    /api/me/favorites
POST   /api/me/favorites
DELETE /api/me/favorites
```

普通用户创建的文章和作品默认进入草稿，提交后变为待审核；管理员审核通过后才进入公开文章列表或作品展厅。普通用户上传仅开放图片类创作资产，文档、附件等非图片文件由管理员后台维护。

## 质量门禁

本地提交前建议执行:

```powershell
mvn -f backend/pom.xml test
npm run build --prefix frontend
docker compose config --quiet
```

CI 当前覆盖:

- 后端 Maven 测试
- 前端 npm audit
- 前端 TypeScript 检查和生产构建
- 后端 Jar 产物
- 前端 dist 产物

工作流文件:

```text
.github/workflows/ci.yml
.github/workflows/cd.yml
```

## 安全与运维约束

- 不提交 `.env`、密钥、Token、Cookie、私有证书
- 不在 README、PR 描述或 commit message 中写入真实密钥
- 生产环境更新前先备份 PostgreSQL 和上传文件
- 不在生产环境执行 `docker compose down -v`，除非确认要删除数据库、Redis 和上传文件
- 后端启动必须配置不少于 32 位的 `JWT_SECRET`
- 管理员密码只以 BCrypt 哈希形式进入配置
- 已运行过的 Flyway 迁移不要原地修改

## 常用命令

查看 Git 状态:

```powershell
git status --short --branch
```

运行后端测试:

```powershell
mvn -f backend/pom.xml test
```

运行前端构建:

```powershell
npm run build --prefix frontend
```

解析 Docker Compose:

```powershell
docker compose config
```

完整输出会展开敏感变量，只在本机排查时使用，不要粘贴到日志、PR 或工单中。

启动 Docker 服务:

```powershell
docker compose up -d
```

查看 Docker 日志:

```powershell
docker compose logs -f backend
```

停止 Docker 服务并保留数据:

```powershell
docker compose down
```

## 产品方向

CreatorSpace 后续继续围绕以下方向演进:

- 更完整的主题编辑、预览和应用链路
- 更成熟的内容规则与审核策略
- 更有艺术感的作品展厅交互
- 更完整的 AI 辅助创作工作流
- 更细的权限模型和后台操作审计
- 更完整的可观测性、备份和发布流水线

当前未完成能力会明确标注为规划或待完善，不会写成已完成能力。
