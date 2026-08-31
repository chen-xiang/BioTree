# BioTree

生物分类（界 / 门 / 纲 / 目 / 科 / 属 / 种）管理与查看站点。

## 文档

- 技术栈：[docs/TECH_STACK.md](docs/TECH_STACK.md)
- 开发规范：[docs/CODING_RULES.md](docs/CODING_RULES.md)
- 数据存储：[docs/DATA_STORAGE.md](docs/DATA_STORAGE.md)
- 性能设计：[docs/PERFORMANCE.md](docs/PERFORMANCE.md)
- 真实数据导入：[docs/DATA_IMPORT.md](docs/DATA_IMPORT.md)
- Windows 全量导入脚本：`scripts/import-col-full.bat`

## 工程结构

```text
backend/    Spring Boot 3（Gradle）
frontend/   Vue 3 + Vite + TypeScript
docs/       设计与规范文档
```

## 本地运行（骨架）

### 前置

- JDK 21
- Node.js 20+ / pnpm
- MySQL 8（库名建议 `biotree`，用户/密码与 `backend/src/main/resources/application.yml` 一致，或复制 `application-local.yml.example`）

### 后端

```bash
cd backend
./gradlew bootRun
```

- API：`http://localhost:8080`
- OpenAPI：`http://localhost:8080/v3/api-docs`
- Swagger UI：`http://localhost:8080/swagger-ui.html`
- 健康检查：`GET /api/health`
- 默认管理员（Flyway 种子）：`admin` / `admin123`（仅开发）

### 前端

```bash
cd frontend
pnpm install
pnpm dev
```

- 站点：`http://localhost:5173`
- 开发期通过 Vite 将 `/api`、`/files` 代理到 `8080`，Session Cookie 同站

### 从 OpenAPI 生成前端类型

后端启动后：

```bash
cd frontend
pnpm openapi:generate
```

## 当前骨架范围

- 后端：统一响应、Security Session、Flyway 初始表与演示分类树、Local/OSS 存储切换接口、公开分类 API（children/详情/搜索）、管理端 CRUD、配图上传（Local `/files`）
- 前端：Router / Pinia / vue-i18n / 深浅色 Token、浏览页懒加载树与搜索、登录与分类管理、配图上传/展示
- 尚未实现：OSS SDK 实装、权威库全量导入、列表虚拟滚动优化
