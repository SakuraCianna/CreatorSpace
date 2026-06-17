# CreatorSpace

CreatorSpace 是一个个人主题风格博客与创意作品展示平台。当前仓库处于第一阶段业务闭环建设阶段，已包含 Spring Boot 后端骨架、Flyway 数据库迁移体系、数据库变更前备份规则、产品主页、注册接口、公开文章/作品列表接口以及基础 CMS 内容管理接口。

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

## 当前已实现的第一阶段接口

后端当前已具备以下最小业务闭环:

```text
POST   /api/auth/register
POST   /api/admin/auth/login
POST   /api/admin/categories
GET    /api/categories
POST   /api/admin/tags
GET    /api/tags
POST   /api/admin/articles
PUT    /api/admin/articles/{id}/publish
GET    /api/articles
GET    /api/articles/slug/{slug}
POST   /api/admin/projects
GET    /api/projects
```

后台接口需要管理员 JWT。前台注册页、文章页和作品页已通过统一 `requestJson` 封装接入对应接口，并提供基础 loading、empty 和 error 状态。

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
```

应用启动时 Flyway 会自动执行未运行的迁移。项目正式产生持久数据前，初始化结构可以合并在 `V1` 中维护；一旦有需要保留的真实数据，不要修改已经执行过的迁移脚本，新增结构变更时创建新的 `V版本号__描述.sql`。

数据库变更前备份规则见:

```text
docs/database/backup-and-migration-rules.md
```

## 业务细节待确认

第一阶段接口设计前需要继续确认的业务细节见:

```text
docs/product/business-details-to-confirm.md
```
