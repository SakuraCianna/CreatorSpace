# 数据库备份与迁移规则

本文档约束 CreatorSpace 的数据库结构变更流程。任何会影响已有数据的结构变更，都必须先备份，再执行迁移。

## 1. 迁移工具

后端使用 Flyway 管理 PostgreSQL 结构变更。

迁移脚本目录:

```text
backend/src/main/resources/db/migration
```

命名规则:

```text
V版本号__英文描述.sql
```

示例:

```text
V1__initialize_creator_space_schema.sql
V2__seed_default_roles_and_admin.sql
```

已执行过的迁移脚本不能修改内容。需要调整结构时，新增下一版迁移脚本。

## 2. 必须备份的场景

出现以下任一情况，必须先备份数据库:

- 新增、删除、重命名表。
- 新增、删除、重命名列。
- 修改列类型、长度、默认值或非空约束。
- 新增唯一约束、外键约束、检查约束。
- 批量更新或清洗已有数据。
- 删除索引或重建大型索引。
- 执行 `truncate`、`delete` 或可能删除用户内容的脚本。
- 执行 Flyway `repair`、`clean`、`baseline` 等可能影响迁移历史的操作。

仅新增不影响已有数据的普通索引时，可以不备份，但 PR 描述必须说明原因。

## 3. 本地备份目录

本地备份统一放在项目根目录:

```text
backups/database
```

该目录已加入 `.gitignore`，不得提交备份文件。

备份文件命名:

```text
yyyyMMdd-HHmmss-before-迁移名.dump
```

示例:

```text
20260616-210000-before-V3-add-private-article-fields.dump
```

## 4. PowerShell 备份命令

在项目根目录执行。执行前确认 `.env` 已填写 PostgreSQL 连接信息。

```powershell
$envFile = Get-Content -LiteralPath ".env"
$envMap = @{}
foreach ($line in $envFile) {
  if ($line -match "^\s*#" -or [string]::IsNullOrWhiteSpace($line)) { continue }
  $idx = $line.IndexOf("=")
  if ($idx -gt 0) { $envMap[$line.Substring(0, $idx)] = $line.Substring($idx + 1) }
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$migrationName = "before-next-migration"
$backupDir = Join-Path (Get-Location) "backups\database"
New-Item -ItemType Directory -Force -Path $backupDir | Out-Null

$env:PGPASSWORD = $envMap["POSTGRES_PASSWORD"]
pg_dump `
  --host $envMap["POSTGRES_HOST"] `
  --port $envMap["POSTGRES_PORT"] `
  --username $envMap["POSTGRES_USERNAME"] `
  --format custom `
  --blobs `
  --file (Join-Path $backupDir "$timestamp-$migrationName.dump") `
  $envMap["POSTGRES_DB"]
Remove-Item Env:\PGPASSWORD -ErrorAction SilentlyContinue
```

备份完成后，确认文件存在:

```powershell
Get-ChildItem -LiteralPath "backups\database" | Sort-Object LastWriteTime -Descending | Select-Object -First 5
```

## 5. PowerShell 恢复命令

恢复会覆盖目标库中的对象。执行前必须确认目标库和备份文件路径。

```powershell
$envFile = Get-Content -LiteralPath ".env"
$envMap = @{}
foreach ($line in $envFile) {
  if ($line -match "^\s*#" -or [string]::IsNullOrWhiteSpace($line)) { continue }
  $idx = $line.IndexOf("=")
  if ($idx -gt 0) { $envMap[$line.Substring(0, $idx)] = $line.Substring($idx + 1) }
}

$backupFile = "backups\database\替换为实际备份文件.dump"

$env:PGPASSWORD = $envMap["POSTGRES_PASSWORD"]
pg_restore `
  --host $envMap["POSTGRES_HOST"] `
  --port $envMap["POSTGRES_PORT"] `
  --username $envMap["POSTGRES_USERNAME"] `
  --dbname $envMap["POSTGRES_DB"] `
  --clean `
  --if-exists `
  --no-owner `
  $backupFile
Remove-Item Env:\PGPASSWORD -ErrorAction SilentlyContinue
```

## 6. 迁移前检查清单

提交或运行迁移前，必须确认:

- 已经新增迁移脚本，没有修改已执行脚本。
- 脚本文件名符合 Flyway 命名规则。
- 迁移脚本可以重复构建测试库。
- 涉及破坏性变更时，已生成 `backups/database` 备份。
- PR 描述写明迁移内容、影响范围、回滚方式和验证方式。

## 7. 回滚规则

Flyway Community 不依赖自动 down migration。回滚优先级:

1. 使用备份恢复数据库。
2. 编写新的正向修复迁移脚本。
3. 仅在本地或测试环境允许清库重建，生产或内测数据环境不允许直接清空。
