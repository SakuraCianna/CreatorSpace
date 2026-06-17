# CI/CD 工作流

CreatorSpace 当前使用 GitHub Actions 承担持续集成和持续交付产物打包。现阶段还没有配置真实服务器部署, 因为 Docker 与部署方案会在后续独立确认。

## 工作流文件

```text
.github/workflows/ci.yml
.github/workflows/cd.yml
```

## CI

CI 会在以下场景触发:

- 向 `main` 提交 Pull Request
- 推送到 `main`
- 推送到 `codex/CreatorSpace`
- 在 GitHub Actions 页面手动触发

CI 包含两个并行 job:

- `backend-tests`: 使用 Java 21 和 Maven 运行 `mvn -B -f backend/pom.xml test`
- `frontend-build`: 使用 Node.js 24 运行 `npm ci`, `npm audit --audit-level=high` 和 `npm run build`

后端测试依赖 Testcontainers, 所以 GitHub runner 需要可用的 Docker 环境。当前固定使用 `ubuntu-24.04` 托管 runner, 默认可执行 Docker CLI。

CI 会上传以下临时产物:

- `backend-test-reports`: Maven Surefire 测试报告, 保留 7 天
- `frontend-dist`: Vite 构建产物, 保留 7 天

## CD

CD 会在以下场景触发:

- 推送到 `main`
- 推送 `v*` 标签
- 在 GitHub Actions 页面手动触发

CD 会重新构建可交付产物:

- `creatorspace-backend-jar`: Spring Boot 后端 jar, 保留 14 天
- `creatorspace-frontend-dist`: 前端静态构建产物, 保留 14 天
- `creatorspace-delivery-summary`: 本次交付摘要, 保留 14 天

CD 当前属于 continuous delivery, 只负责生成可下载交付包, 不会远程登录服务器, 不会修改生产环境, 也不需要 GitHub Secrets。

## 本地等价检查

在项目根目录执行:

```powershell
mvn -f backend/pom.xml test
npm ci --prefix frontend --registry=https://registry.npmjs.org/
npm audit --prefix frontend --registry=https://registry.npmjs.org/ --audit-level=high
npm run build --prefix frontend
```

## 后续接入真实部署时需要确认

- 部署目标是单机 Docker Compose, 还是其他容器平台
- 前端静态文件由 Spring Boot 托管, Nginx 托管, 还是单独容器托管
- 后端 jar 是否继续直接运行, 还是构建后端镜像
- 是否需要 GitHub Environments 审批生产部署
- 是否需要 SSH 部署密钥或容器镜像仓库 token

确认前不要把服务器地址、私钥、token 或生产数据库配置写入仓库。

## 维护规则

- 升级 GitHub Actions 官方 action 前, 先查看对应官方 Release
- workflow 权限默认保持最小化, 当前只需要 `contents: read`
- runner 镜像固定为 `ubuntu-24.04`, 避免 `ubuntu-latest` 自动迁移带来的环境漂移
- 涉及部署的 job 默认串行化, 避免同一 ref 的交付流程互相覆盖
- 添加真实部署前, 必须同步更新本文档和 README

## 参考资料

- [GitHub Actions workflow syntax](https://docs.github.com/en/actions/reference/workflows-and-actions/workflow-syntax)
- [actions/checkout](https://github.com/actions/checkout)
- [actions/setup-java](https://github.com/actions/setup-java)
- [actions/setup-node](https://github.com/actions/setup-node)
- [actions/upload-artifact](https://github.com/actions/upload-artifact)
