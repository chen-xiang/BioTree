# BioTree

生物分类（界 / 门 / 纲 / 目 / 科 / 属 / 种）管理与查看站点。

## 文档

- 技术栈：[docs/TECH_STACK.md](docs/TECH_STACK.md)
- 开发规范：[docs/CODING_RULES.md](docs/CODING_RULES.md)
- 数据存储：[docs/DATA_STORAGE.md](docs/DATA_STORAGE.md)
- 性能设计：[docs/PERFORMANCE.md](docs/PERFORMANCE.md)
- 真实数据导入：[docs/DATA_IMPORT.md](docs/DATA_IMPORT.md)
- 改进优化实施方案：[docs/IMPROVEMENT_PLAN.md](docs/IMPROVEMENT_PLAN.md)
- 完整阶元与 DwC 补齐方案：[docs/FULL_TAXONOMY_PLAN.md](docs/FULL_TAXONOMY_PLAN.md)
- Windows / Linux 脚本：见下方「脚本」；全量导入 `scripts/import-col-full.{bat,sh}`

## 工程结构

```text
backend/    Spring Boot 3（Gradle）· MySQL · Flyway · Session/CSRF
frontend/   Vue 3 · Vite · TypeScript · Pinia · vue-i18n
docs/       设计与规范文档
scripts/    开发 / 导入 / 统计脚本（.bat + .sh）
```

## 脚本

仓库根目录执行。`./scripts/help.sh` 或 `scripts\help.bat` 可打印完整列表。

| 作用 | Linux / macOS | Windows |
| --- | --- | --- |
| 启动前端 Vite | `./scripts/start-web-dev.sh` | `scripts\start-web-dev.bat` |
| 启动后端 Spring Boot | `./scripts/start-server-dev.sh` | `scripts\start-server-dev.bat` |
| 安装前端依赖 | `./scripts/install-frontend.sh` | `scripts\install-frontend.bat` |
| 后端测试 | `./scripts/test-backend.sh` | `scripts\test-backend.bat` |
| 前端检查（typecheck/lint/test/build） | `./scripts/test-frontend.sh` | `scripts\test-frontend.bat` |
| 全部检查 | `./scripts/test-all.sh` | `scripts\test-all.bat` |
| 生成 OpenAPI 类型 | `./scripts/openapi-generate.sh` [`live`] | `scripts\openapi-generate.bat` [`live`] |
| 源码行数统计 | `./scripts/count-loc.sh` | `scripts\count-loc.bat` |
| CoL 全量导入 | `./scripts/import-col-full.sh` | `scripts\import-col-full.bat` |
| CoL 断点续跑 | `./scripts/import-col-resume.sh` | `scripts\import-col-resume.bat` |

开发时请开两个终端分别跑 `start-server-dev` 与 `start-web-dev`。导入用 `import-col-*`，**不**占用 8080，可与后端服务同时跑。
## 已实现能力

- 公开浏览：懒加载分类树、搜索（前缀 / MySQL FULLTEXT）、详情、异名、locale 回退、`/browse/:id`
- 管理端：登录 Session、CSRF、分类 CRUD / 移动、配图上传（Local 或阿里云 OSS）
- 导入：Catalogue of Life DwC-A（动物界/植物界七级 + 俗名 + 异名）、批写与断点续跑
- 前端：深浅色 Token、基础设计系统组件、虚拟列表、Markdown 介绍

## 本地运行

### 前置

- JDK 21、Node.js 20+ / pnpm、MySQL 8  
- 库名建议 `biotree`；账号可与 `backend/src/main/resources/application.yml` 一致，或复制 `application-local.yml.example`

### 后端

```bash
cd backend
./gradlew bootRun
```

- API：`http://localhost:8080`
- OpenAPI：`http://localhost:8080/v3/api-docs`
- 健康检查：`GET /api/health`
- 开发管理员（非 prod 种子）：`admin` / `admin123`

### 前端

```bash
cd frontend
pnpm install
pnpm dev
```

- 站点：`http://localhost:5173`（Vite 代理 `/api`、`/files`）

### 生成前端 OpenAPI 类型

```bash
cd frontend
pnpm openapi:generate          # 离线 openapi/openapi.yaml
pnpm openapi:generate:live     # 需后端已启动
```

### CoL 导入（摘要）

见 [docs/DATA_IMPORT.md](docs/DATA_IMPORT.md)。使用 `scripts/import-col-full.*` 或 `cd backend && ./gradlew importCol --args='...'`，不要用 `bootRun` 导入。全量前请确认 JVM 堆与 `app.import.*`；中断后续跑请使用 `resume=true` 且勿再次 `replace=true`。

## 生产注意

使用 `spring.profiles.active=prod`（见 `application-prod.yml`）：强制改密、Secure Cookie、收紧 Swagger、配置 CORS/OSS。细则见改进方案 W3。
