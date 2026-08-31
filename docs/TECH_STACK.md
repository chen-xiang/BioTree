# BioTree 技术栈

本文档为 BioTree（生物分类：界 / 门 / 纲 / 目 / 科 / 属 / 种）管理与查看站点的**完整技术栈定稿**。

---

## 1. 总览

| 层级 | 选型 |
| --- | --- |
| 后端 | Java 21 · Spring Boot 3 · Gradle · OpenAPI 3 / SpringDoc · MySQL · Spring Data JPA · Flyway · Spring Security（Session/Cookie）· Local/OSS 可切换文件存储 |
| 前端 | Vue 3 · TypeScript · Vite · Pinia · Vue Router · vue-i18n · 深浅色 · 自建基础设计系统 · openapi-typescript + openapi-fetch |
| 本期不做 | Redis · Docker · Nginx |

---

## 2. 后端

| 项 | 选型 | 说明 |
| --- | --- | --- |
| 语言 / 运行时 | **Java 21** | LTS |
| 应用框架 | **Spring Boot 3** | Web、校验、事务、配置管理 |
| 构建工具 | **Gradle** | 依赖管理与构建（不使用 Maven） |
| API 契约 | **OpenAPI 3 + SpringDoc** | 接口文档与契约唯一来源 |
| 数据库 | **MySQL 8** | 主库；分类树采用邻接表（`parent_id` 自引用） |
| ORM | **Spring Data JPA** | 实体、仓储、事务 |
| Schema 迁移 | **Flyway** | 数据库结构版本化 |
| 参数校验 | **Jakarta Bean Validation** | 请求体与领域约束（随 Boot 标配） |
| 安全框架 | **Spring Security** | 见下方「鉴权模型」 |
| 会话机制 | **Session + Cookie** | 管理端登录态；**不使用 JWT Bearer** |
| 密码哈希 | **BCrypt** | 管理账号口令存储 |
| 文件存储 | **策略模式：Local / OSS 可切换** | 前期 Local，后续切换 OSS，业务只依赖统一接口 |
| 日志 | **Logback**（Spring Boot 默认） | 结构化应用日志 |
| 健康检查（建议） | **Spring Boot Actuator**（`/actuator/health`） | 不做 Docker 也可先保留 |

### 2.1 鉴权模型

| 范围 | 策略 |
| --- | --- |
| 公开页面与只读 API | **免登录**（匿名可访问）：分类树、搜索、详情、公开附件访问等 |
| 管理后台与写操作 API | **必须登录**：分类增删改、附件上传/删除、账号相关等 |

约定建议：

- 后端：公开路径放行；`/api/admin/**`（或等价前缀）要求已认证 Session。
- 前端：公开路由无守卫；`/admin/**` 未登录跳转登录页。
- 前后端分离时启用 **CORS**，并对 Session Cookie 配置合适的 `SameSite` / 跨域凭证（`credentials`）。

### 2.2 文件存储

```text
StorageService（接口）
├── LocalFileStorageService   ← app.storage.type=local（前期）
└── OssFileStorageService     ← app.storage.type=oss（后续）
```

| 能力 | 说明 |
| --- | --- |
| 统一接口 | 上传、删除、生成访问 URL |
| 元数据 | 存 MySQL；二进制文件不进库 |
| 切换方式 | 配置项切换实现，业务代码不感知存储后端 |
| OSS SDK | 实现 OSS 适配时再引入（如阿里云 OSS SDK） |

配置示例（示意）：

```yaml
app:
  storage:
    type: local # local | oss
    local:
      base-path: ./data/files
    oss:
      endpoint: ...
      bucket: ...
      access-key: ...
      secret-key: ...
```

### 2.3 API 与数据约定（建议）

| 项 | 约定 |
| --- | --- |
| 契约 | 全部 HTTP API 纳入 OpenAPI 3，由 SpringDoc 导出 |
| 统一响应 | 固定结构（如 `code` / `message` / `data`）+ 全局异常处理 |
| 环境配置 | `application.yml` + profile（如 `local` / `prod`） |
| 审计字段（建议） | `created_at` / `updated_at` / `created_by` 等 |

---

## 3. 前端

| 项 | 选型 | 说明 |
| --- | --- | --- |
| 框架 | **Vue 3 + TypeScript** | Composition API |
| 构建工具 | **Vite** | 开发服务器与生产构建 |
| 状态管理 | **Pinia** | 主题、语言、会话用户等 |
| 路由 | **Vue Router** | 公开路由与管理路由分离 |
| 多语言 | **vue-i18n** | UI 文案多语言；学名保持拉丁文展示 |
| 主题 | **深浅色模式** | CSS 变量 + 手动切换，持久化（如 localStorage） |
| HTTP 客户端 | **openapi-fetch** | 类型安全请求（**不使用 Axios**） |
| 类型生成 | **openapi-typescript** | 由后端 OpenAPI 文档生成 TS 类型 |
| UI | **自建基础设计系统** | Design Token + 基础组件；不引入 Element Plus / Ant Design Vue 等整套 UI 库 |

### 3.1 契约驱动调用链

```text
SpringDoc 导出 OpenAPI JSON/YAML
        ↓
openapi-typescript 生成 types
        ↓
openapi-fetch 发起类型安全请求（携带 Cookie / credentials）
```

### 3.2 自建设计系统范围（基础）

建议至少覆盖：

- **Token**：颜色（含深浅色语义色）、字体、字号、间距、圆角、层级（z-index）
- **基础组件**：Button、Input、Select、Form、Table、Tree、Dialog/Modal、Toast/Message、Pagination、Breadcrumb 等
- **布局**：公开浏览布局、管理后台布局（侧栏 / 顶栏）

---

## 4. 领域与功能对应

| 能力 | 技术落点 |
| --- | --- |
| 界门纲目科属种树 | MySQL `taxon` 自引用 + JPA；后端树/子节点/祖先链 API |
| 公开查看 | 免登录只读 API + Vue 浏览页（树、搜索、详情、面包屑） |
| 管理维护 | Session 登录 + 管理端 CRUD + 层级（rank）与父节点校验 |
| 附件（如图片） | `StorageService`；元数据入库存路径/URL |
| 多语言 UI | vue-i18n |
| 深浅色 | Pinia 主题 store + CSS 变量 |
| 接口类型对齐 | OpenAPI → openapi-typescript → openapi-fetch |

---

## 5. 建议工程结构

```text
BioTree/
├── backend/                      # Spring Boot（Gradle）
│   ├── src/main/java/.../
│   │   ├── domain/               # 领域模型（如 Taxon）
│   │   ├── application/          # 用例、层级校验
│   │   ├── api/                  # Controller + OpenAPI 注解
│   │   ├── infrastructure/       # JPA、Local/OSS 存储实现
│   │   └── security/             # Spring Security / Session
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   ├── application-local.yml
│   │   └── db/migration/         # Flyway
│   └── build.gradle.kts
├── frontend/                     # Vue 3 + Vite
│   ├── src/
│   │   ├── views/                # 浏览、详情、管理、登录
│   │   ├── components/           # 自建设计系统组件
│   │   ├── stores/               # Pinia
│   │   ├── router/               # Vue Router
│   │   ├── locales/              # vue-i18n
│   │   ├── styles/               # Token / 深浅色
│   │   ├── api/                  # openapi-fetch 客户端与生成类型
│   │   └── ...
│   ├── package.json
│   └── vite.config.ts
├── docs/
│   └── TECH_STACK.md             # 本文档
└── README.md
```

---

## 6. 本期明确不做

| 项 | 状态 |
| --- | --- |
| Redis | 不做 |
| Docker | 不做 |
| Nginx | 不做 |

本地开发：本机（或已有）MySQL + `./gradlew bootRun` + `pnpm/npm run dev`。

---

## 7. 可选增强（非本期必做）

| 项 | 说明 |
| --- | --- |
| MapStruct | Entity ↔ API DTO 映射 |
| Spotless / Checkstyle | 后端代码风格 |
| ESLint + Prettier | 前端代码风格 |
| JUnit 5 + `@DataJpaTest` | 后端单测 / 仓储测 |
| Vitest | 前端单测 |
| 操作审计日志 | 管理端关键写操作留痕 |

---

## 8. 一句话定稿

**后端：** Java 21 · Spring Boot 3 · **Gradle** · OpenAPI 3 / SpringDoc · MySQL · Spring Data JPA · Flyway · Spring Security（**Session + Cookie**，仅管理端需登录）· Local / OSS 可切换文件存储  

**前端：** Vue 3 · TypeScript · Vite · Pinia · Vue Router · vue-i18n · 深浅色 · 自建基础设计系统 · **openapi-typescript + openapi-fetch**  

**不做：** Redis · Docker · Nginx  
**不做（已否决）：** Maven · Axios · JWT Bearer  

---

## 9. 开发规范

编码、文件头、测试、UI 与变更策略见 [CODING_RULES.md](./CODING_RULES.md)（Cursor 同步规则：`.cursor/rules/biotree-basics.mdc`）。

---

## 10. 数据与性能

- 分类 / 多语言内容 / 配图如何存储：[DATA_STORAGE.md](./DATA_STORAGE.md)
- 百万级节点下的加载与展示性能：[PERFORMANCE.md](./PERFORMANCE.md)  
