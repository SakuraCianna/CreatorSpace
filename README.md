# CreatorSpace

CreatorSpace 是一个个人主题风格博客与创意作品展示平台。当前仓库已围绕“个人博客 + 作品展厅 + 灵感墙 + 主题化 CMS”的终极目标扩展到完整内容骨架，包含 Spring Boot 后端、Flyway 数据库迁移体系、数据库变更前备份规则、产品主页、公开内容浏览、站内搜索、灵感展示、主题配置、注册登录以及基础 CMS 内容管理接口。

## 当前结构

```text
CreatorSpace
├── .github
│   └── workflows
├── backend
│   ├── pom.xml
│   └── src
│       ├── main
│       │   ├── java/com/creatorspace
│       │   └── resources/db/migration
│       └── test
├── frontend
│   ├── package.json
│   ├── vite.config.ts
│   └── src
├── docs
│   ├── database
│   ├── product
│   └── superpowers
├── .env.example
├── .gitignore
└── 需求文档.md
```

## 后端技术栈

- Java 21
- Spring Boot 4.0.7
- PostgreSQL 17
- Redis 7
- MyBatis-Plus Spring Boot 4 starter
- Flyway
- Maven

## 前端技术栈

- Node.js 24
- Vue 3
- TypeScript
- Vite 8
- Vue Router
- Pinia
- GSAP
- Three.js
- anime.js
- markdown-it
- highlight.js
- @lucide/vue

## 本地配置

真实配置写在项目根目录 `.env`，该文件不会提交仓库。模板见 `.env.example`。

后端 `application.yml` 会尝试读取:

```text
../.env
.env
```

这样无论在项目根目录执行 Maven，还是进入 `backend` 目录执行，都可以读取本地配置。

## 运行检查

在项目根目录执行:

```powershell
mvn -f backend/pom.xml test
npm run build --prefix frontend
```

## 当前已实现的核心接口

后端当前已具备以下核心业务闭环:

```text
POST   /api/auth/register
POST   /api/auth/login
POST   /api/admin/auth/login
GET    /api/site/config
GET    /api/theme/current
GET    /api/search
POST   /api/admin/categories
GET    /api/categories
POST   /api/admin/tags
GET    /api/tags
POST   /api/admin/articles
GET    /api/admin/articles
GET    /api/admin/articles/{id}
PUT    /api/admin/articles/{id}
PUT    /api/admin/articles/{id}/publish
PUT    /api/admin/articles/{id}/unpublish
PUT    /api/admin/articles/{id}/top
PUT    /api/admin/articles/{id}/recommend
DELETE /api/admin/articles/{id}
GET    /api/articles
GET    /api/articles/slug/{slug}
POST   /api/admin/projects
GET    /api/admin/projects
GET    /api/admin/projects/{id}
PUT    /api/admin/projects/{id}
PUT    /api/admin/projects/{id}/status
PUT    /api/admin/projects/{id}/recommend
DELETE /api/admin/projects/{id}
GET    /api/projects
GET    /api/projects/slug/{slug}
GET    /api/inspirations
GET    /api/admin/inspirations
POST   /api/admin/inspirations
PUT    /api/admin/inspirations/{id}
DELETE /api/admin/inspirations/{id}
GET    /api/comments
POST   /api/comments
GET    /api/admin/comments
PUT    /api/admin/comments/{id}/approve
PUT    /api/admin/comments/{id}/reject
POST   /api/admin/files/upload
GET    /api/admin/files
GET    /api/admin/dashboard/overview
```

后台接口需要管理员 JWT。前台注册、访客登录、管理员登录、文章、文章详情、作品、作品详情、公开评论、灵感墙、搜索和关于页已通过统一 `requestJson` 封装接入对应接口，并提供 loading、empty、error 和本地展示数据兜底状态。后台文章、作品、灵感、评论审核和文件资源模块已接入真实接口；主题与站点设置仍处于工作台骨架阶段。

## 当前前端页面

```text
/                 产品主界面
/articles         文章档案
/articles/:slug   文章详情
/projects         作品展厅
/projects/:slug   作品详情
/inspirations     灵感墙
/search           站内搜索
/about            关于作者与平台
/login            访客登录 / 管理员登录
/register         访客注册
/admin            CMS 概览
/admin/*          CMS 模块工作台
```

## 快速启动

双击或在项目根目录执行:

```powershell
.\快速启动.bat
```

脚本会分别启动前端和后端。

## CI/CD

GitHub Actions 工作流:

```text
.github/workflows/ci.yml
.github/workflows/cd.yml
```

CI 会在 Pull Request、`main` 和 `codex/CreatorSpace` 分支推送时运行后端测试、前端依赖审计和前端构建。CD 会在 `main` 或 `v*` 标签推送时生成后端 jar、前端静态产物和交付摘要。

详细规则见:

```text
docs/ci-cd.md
```

## 启动后端

启动前确认 `.env` 中 PostgreSQL 配置可用。

```powershell
mvn -f backend/pom.xml spring-boot:run
```

后端默认端口:

```text
8080
```

健康检查:

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/health"
```

## 启动前端

首次安装依赖:

```powershell
npm install --prefix frontend
```

启动 Vite 开发服务器:

```powershell
npm run dev --prefix frontend
```

前端默认端口:

```text
5173
```

## 配置说明

配置模板见 `.env.example`。后端会通过 `application.yml` 读取数据库、Redis 基础信息、上传限制、CORS、JWT、本地文件存储和 AI 预留配置。前端会通过 Vite 读取 `VITE_` 前缀配置, 包括 API 地址、开发代理和开发服务器端口。

后端启动必须显式配置 `JWT_SECRET`，且长度不少于 32 个字符；未配置或过短时服务会拒绝启动，避免默认弱密钥进入本地联调或部署环境。初始化管理员密码哈希仍通过 `ADMIN_PASSWORD_HASH` 写入 `.env`。

## 数据库迁移

迁移脚本目录:

```text
backend/src/main/resources/db/migration
```

当前迁移:

```text
V1__initialize_creator_space_schema.sql
V2__seed_showcase_data.sql
```

应用启动时 Flyway 会自动执行未运行的迁移。`V1` 当前覆盖用户、资料、文章、作品、灵感、评论、留言、主题、导航、内容块、统计、搜索日志、AI 任务和后台审计等终极目标所需的基础表结构。项目正式产生持久数据前，初始化结构可以合并在 `V1` 中维护；一旦有需要保留的真实数据，不要修改已经执行过的迁移脚本，新增结构变更时创建新的 `V版本号__描述.sql`。

`V2__seed_showcase_data.sql` 是正式迁移，用于写入产品演示和本地联调所需的展示数据。它会随 `classpath:db/migration` 在所有启用 Flyway 的环境执行，数据命名尽量使用 `demo-`、`demo_` 或 `demo.*` 前缀，避免覆盖已有真实配置和内容。生产环境如果不希望内置展示数据，需要在上线前调整迁移策略或使用不包含该迁移的数据库初始化流程。

数据库变更前备份规则见:

```text
docs/database/backup-and-migration-rules.md
```

## 业务细节待确认

第一阶段接口设计前需要继续确认的业务细节见:

```text
docs/product/business-details-to-confirm.md
```
