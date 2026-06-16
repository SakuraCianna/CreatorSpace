# CreatorSpace

CreatorSpace 是一个个人主题风格博客与创意作品展示平台。当前仓库处于基础框架阶段，已包含 Spring Boot 后端骨架、Flyway 数据库迁移体系和数据库变更前备份规则。

## 当前结构

```text
CreatorSpace
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
