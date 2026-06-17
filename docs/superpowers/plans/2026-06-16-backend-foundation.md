# Backend Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the initial Spring Boot 4 backend foundation with Flyway migrations, PostgreSQL schema, and database backup rules.

**Architecture:** Use a monorepo layout with `backend/` for the Spring Boot service and later `frontend/` for Vue. Flyway owns schema changes under `backend/src/main/resources/db/migration`, and database-changing work must follow a documented backup checklist before migrations are run against persistent databases.

**Tech Stack:** Java 21, Spring Boot 4.0.x, Maven, PostgreSQL 17, Redis 7, MyBatis-Plus Spring Boot 4 starter, Flyway, Testcontainers, JUnit 5.

---

### Task 1: Backend Maven Skeleton And Smoke Tests

**Files:**
- Create: `backend/pom.xml`
- Create: `backend/src/test/java/com/creatorspace/CreatorSpaceApplicationTests.java`
- Create: `backend/src/main/java/com/creatorspace/CreatorSpaceApplication.java`
- Create: `backend/src/main/resources/application.yml`

- [x] **Step 1: Write the failing Spring context test**

Create a test that references `CreatorSpaceApplication` before the application class exists.

- [x] **Step 2: Run test to verify it fails**

Run: `mvn -f backend/pom.xml test -Dtest=CreatorSpaceApplicationTests`

Expected: FAIL because the application class does not exist.

- [x] **Step 3: Add the minimal Spring Boot application and config**

Create `CreatorSpaceApplication.java` and `application.yml` with `.env` import and basic app settings.

- [x] **Step 4: Run test to verify it passes**

Run: `mvn -f backend/pom.xml test -Dtest=CreatorSpaceApplicationTests`

Expected: PASS.

### Task 2: Health API And Shared Response Envelope

**Files:**
- Create: `backend/src/main/java/com/creatorspace/common/result/ApiResponse.java`
- Create: `backend/src/main/java/com/creatorspace/config/SecurityConfig.java`
- Create: `backend/src/main/java/com/creatorspace/module/health/controller/HealthController.java`
- Create: `backend/src/test/java/com/creatorspace/module/health/controller/HealthControllerTests.java`

- [x] **Step 1: Write the failing API test**

Test `GET /api/health` returns a JSON response with `success=true` and app name `CreatorSpace`.

- [x] **Step 2: Run test to verify it fails**

Run: `mvn -f backend/pom.xml test -Dtest=HealthControllerTests`

Expected: FAIL because the controller does not exist.

- [x] **Step 3: Add response envelope, security permit rules, and health controller**

Implement only the public health endpoint and leave business auth for later tasks.

- [x] **Step 4: Run test to verify it passes**

Run: `mvn -f backend/pom.xml test -Dtest=HealthControllerTests`

Expected: PASS.

### Task 3: MyBatis-Plus And Flyway Migration Foundation

**Files:**
- Create: `backend/src/main/java/com/creatorspace/config/MybatisPlusConfig.java`
- Create: `backend/src/main/resources/db/migration/V1__initialize_creator_space_schema.sql`
- Create: `backend/src/test/java/com/creatorspace/database/FlywayMigrationTests.java`

- [x] **Step 1: Write the failing migration test**

Use Testcontainers PostgreSQL and Flyway to migrate the schema, then assert core tables, extensions, and seeded admin role exist.

- [x] **Step 2: Run test to verify it fails**

Run: `mvn -f backend/pom.xml test -Dtest=FlywayMigrationTests`

Expected: FAIL because migration files do not exist.

- [x] **Step 3: Add MyBatis-Plus pagination config and Flyway SQL migrations**

Add PostgreSQL extensions, core tables, indexes, seed roles, and admin placeholder insert.

- [x] **Step 4: Run migration test to verify it passes**

Run: `mvn -f backend/pom.xml test -Dtest=FlywayMigrationTests`

Expected: PASS.

### Task 4: Database Change Backup Rules And Project Docs

**Files:**
- Modify: `.gitignore`
- Create: `docs/database/backup-and-migration-rules.md`
- Create: `README.md`

- [x] **Step 1: Write documentation checklist**

Document PowerShell-safe backup commands, restore commands, migration naming, and when backup is mandatory.

- [x] **Step 2: Ignore local backup artifacts**

Add `backups/` to `.gitignore`.

- [x] **Step 3: Update README**

Document backend layout, commands, migration behavior, and the fact that real `.env` is local-only.

- [x] **Step 4: Run documentation and build checks**

Run: `git diff --check`

Run: `mvn -f backend/pom.xml test`

Expected: Both commands exit 0.

### Task 5: Review, Commit, Push, And PR Update

**Files:**
- Review all changed files.

- [x] **Step 1: Run final checks**

Run: `git status --short --branch --ignored`

Run: `git diff --check`

Run: `mvn -f backend/pom.xml test`

- [x] **Step 2: Dispatch independent reviewer**

Reviewer must check schema safety, secret handling, migration rules, and whether the work matches the request.

- [x] **Step 3: Commit**

Run: `git add -- .`

Run: `git commit -m "搭建后端基础框架和数据库迁移体系"`

- [x] **Step 4: Push and update PR**

Run: `git push`

Update PR #1 with the new backend foundation changes.
