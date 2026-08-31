# BioTree 改进优化实施方案

本文是在现有骨架、CoL 导入与性能硬化（含 PR #4 / #5 能力）之上的**完整落地路线**。目标：支撑动物界/植物界近乎完整数据量、可安全上线管理，并补齐体验、一致性与工程能力。

> **实施进度（`cursor/implement-improvement-plan-60ae`）**：W0–W8 主干已落地——导入暂存流式、管理分页/登出、prod 加固、OpenAPI+openapi-fetch 闭环、首页品牌动效、错误码/等级 i18n、stats、Vitest/ESLint、详情 media 分页。Spotless 与导入类进一步拆分可后续小 PR。

配套文档：[TECH_STACK.md](./TECH_STACK.md) · [CODING_RULES.md](./CODING_RULES.md) · [DATA_STORAGE.md](./DATA_STORAGE.md) · [PERFORMANCE.md](./PERFORMANCE.md) · [DATA_IMPORT.md](./DATA_IMPORT.md) · [FULL_TAXONOMY_PLAN.md](./FULL_TAXONOMY_PLAN.md)

**难度表述**：用「改动面 / 依赖 / 风险」描述，不以日历工期估算。

---

## 0. 现状基线（已完成，勿重复建设）

| 领域 | 已具备能力 |
| --- | --- |
| 后端核心 | Spring Boot 3 · JPA · Flyway · Session+Cookie · CSRF · Local/OSS · 统一 `ApiResponse` / 全局异常 |
| 分类 API | children 分页、详情、搜索（前缀/FULLTEXT/异名）、管理 CRUD/移动/配图 |
| 导入 | CoL DwC-A 批写、断点 checkpoint、异名、俗名；Windows 全量脚本 |
| 前端 | Vue3 浏览树懒加载、防抖搜索、Abort、`/browse/:id`、Markdown 介绍、虚拟列表、基础 Bt* 组件、深浅色 + i18n |
| 测试 / CI | 后端集成测一批；GitHub Actions：`gradlew test` + `pnpm build` |

**已知过时描述**：`README.md` 仍写「骨架 / OSS 与虚拟滚动未实现」等，须在本方案 **W0** 纠正。

**前置合并建议**：先合入性能硬化与断点/异名相关 PR（#4 → #5），再按本方案分批开 PR。

---

## 1. 目标与原则

### 1.1 目标

1. **全量数据可用**：导入不 OOM；浏览/管理不静默丢页；搜索可承受百万级。
2. **可上线安全**：生产配置、默认口令、Swagger、Cookie Secure、登出闭环。
3. **契约一致**：OpenAPI 为唯一 API 真源；前后端类型与手写 fetch 脱钩。
4. **体验达标**：设计系统可用、首页品牌成立、错误多语言、关键动效有层次。
5. **可维护**：测试覆盖关键路径；规范与 CI 能拦住回潮。

### 1.2 原则

- **永不整树下发**（见 PERFORMANCE）；任何新功能不得破坏该约束。
- **小 PR、可回滚**：每波一个主题；先后端契约/数据，再前端消费。
- **测试同步**：行为变更必须带测；前端从关键工具函数/组件测起步。
- **规范优先**：文件头、中文注释、英文日志；默认删除无用/deprecated 代码。
- **不做清单不变**：本期仍不引入 Redis / Docker / Nginx（除非方案明确修订 TECH_STACK）。

---

## 2. 工作波次总览

| 波次 | 主题 | 改动面 | 主要风险 |
| --- | --- | --- | --- |
| **W0** | 文档对齐 + 合并基线 | 文档、README | 低 |
| **W1** | 导入去全量内存 | 导入管线、可能拆类 | 高（数据正确性、续跑语义） |
| **W2** | 管理端分页 + 登出 | Admin UI、少量 API 消费 | 中（截断数据被掩盖） |
| **W3** | 生产加固 | 配置、Security、种子策略 | 中（误关开发便利） |
| **W4** | OpenAPI 闭环 | SpringDoc、生成脚本、客户端重构 | 中（接口漂移） |
| **W5** | 设计系统与首页 | UI 组件、Home、动效、字体 | 低～中（视觉回归） |
| **W6** | i18n / 异常 / 统计 | 错误码映射、stats API | 中 |
| **W7** | 测试与工程能力 | Vitest、ESLint、Spotless、CI | 低 |
| **W8** | 性能与一致性抛光 | media 分页、缓存、架构拆分 | 中 |

波次可并行程度：**W2∥W3** 可并行；**W1 宜单独**；**W4 依赖稳定 API**；**W5–W8** 可在 W2–W4 之后交错。

---

## 3. 详细方案

### W0 — 文档与基线对齐

**目的**：消除「文档说未做、代码已做」的信任债。

| 任务 | 动作 | 验收 |
| --- | --- | --- |
| W0.1 | 更新 `README.md`：本地运行、导入、已实现能力（OSS/虚拟滚动/断点/异名/CSRF） | README 与代码一致 |
| W0.2 | `TECH_STACK` / `PERFORMANCE` / `DATA_IMPORT` 交叉引用本方案 | 文档互相可跳转 |
| W0.3 | 合并开放中的硬化 PR，固定 `main` 基线 | CI 绿 |

**不改代码行为。**

---

### W1 — 导入去全量内存（P0 · 最高优先级）

**问题**：`ColDwcaImporter` 扫描阶段将大量 accepted 节点放入内存 Map，全量 CoL 存在堆溢出风险。

**目标架构**：

```text
Zip 流式扫描
  → 按 rank 分阶段（或固定批）写出 staging / 直接写 taxon
  → 父级解析：依赖已落库 external_id 索引 + 有界内存缓存（当前批/当前界）
  → checkpoint 按阶段+批号更新
  → vernacular / synonym 二次流式扫描
  → rebuild child_count
```

| 任务 | 内容 | 验收 |
| --- | --- | --- |
| W1.1 | 去掉「全量 `nodes` LinkedHashMap」；按等级或滚动窗口处理 | 试导入峰值堆显著下降（相对基线记录数字） |
| W1.2 | 父级解析改为「库查 + 小缓存」，禁止依赖全图内存 | 夹具测试：层级与 path 正确 |
| W1.3 | 强化断点：阶段枚举 `PARSE_RANK_*` / `VERNACULAR` / `SYNONYM` / `COUNTS`；中断可续 | 集成测：中途停再跑不重复、不丢 |
| W1.4 | 导入进度可观测：日志结构化字段 + 可选 `GET /api/admin/import/status`（只读 checkpoint） | 运维能看到 phase/processed |
| W1.5 | 拆分 `ColDwcaImporter`：Parse / Writer / Synonym / Checkpoint 高内聚类 | 单类行数可控；规则符合 CODING_RULES |

**依赖**：现有 `import_checkpoint`、批事务。  
**风险**：父级指向中间等级时的上溯逻辑需回归夹具与小样全量。  
**明确不做**：本波不引入消息队列/独立 Worker 进程（仍是 `bootRun --app.import.enabled`）。

---

### W2 — 管理端可用性（P0）

| 任务 | 内容 | 验收 |
| --- | --- | --- |
| W2.1 | `AdminTaxaView`：children **分页 / 加载更多**（复用 `BtPagination` 或 loadMore） | 超过一页的节点全部可达 |
| W2.2 | 列表总数、当前页提示；空页与错误态 | UI 不静默截断 |
| W2.3 | 管理端 **登出**：Topbar/侧栏按钮 → `POST /api/admin/auth/logout` + CSRF + 清 Pinia | 登出后进 `/admin` 跳登录 |
| W2.4 | 登录后拉取 `/me` 展示当前用户 | 侧栏可见用户名 |

**依赖**：CSRF 头工具已有。  
**风险**：低。

---

### W3 — 生产加固（P0）

| 任务 | 内容 | 验收 |
| --- | --- | --- |
| W3.1 | 增加 `application-prod.yml`（或文档化必改项）：`cookie.secure=true`、CORS、OSS、关/限 swagger、日志级别 | 有可复制清单 |
| W3.2 | 种子管理员：仅 `local`/`dev` profile 注入；prod 强制环境变量初始化或禁止默认口令 | 无「admin/admin123」进生产 |
| W3.3 | Security：prod 收紧 `/swagger-ui/**`、`/v3/api-docs/**` | 未认证不可扫文档（或 IP/Basic 保护，二选一写清） |
| W3.4 | `AccessDeniedHandler` → 统一 `ApiResponse`；与 EntryPoint 对称 | 403 体格式一致 |
| W3.5 | 校验异常：对外消息白名单/字段错误码，避免堆栈或内部细节泄漏 | 单测覆盖 |

**依赖**：无。  
**风险**：开发体验变化 → `application-local.yml.example` 同步更新。

---

### W4 — OpenAPI 闭环（P0）

**目标**：SpringDoc 导出 = 契约真源；前端 `openapi-typescript` + `openapi-fetch` 为默认调用路径。

| 任务 | 内容 | 验收 |
| --- | --- | --- |
| W4.1 | 补全控制器注解（auth、CRUD、media、move、search 分页参数） | `/v3/api-docs` 覆盖全部对外 API |
| W4.2 | 生成流程：`pnpm openapi:generate:live`（开发）+ CI 可选 artifact；保留 `openapi/openapi.yaml` 作离线快照或改为「导出提交」 | `schema.d.ts` 与后端一致 |
| W4.3 | 重构 `api/taxon.ts` / login：基于 `apiClient`；删除重复手写 DTO（或由生成类型推导） | 无双轨类型 |
| W4.4 | CI：契约漂移检查（启动临时上下文导出 vs 提交的 yaml/schema，或仅文档约定人工） | 至少文档规定「改 API 必重生」 |

**依赖**：W2/W3 接口稳定更佳。  
**风险**：生成类型与现有 Vue 用法磨合。

---

### W5 — 设计系统、布局、动效（P1）

对照 TECH_STACK §3.2 与产品设计约束。

| 任务 | 内容 | 验收 |
| --- | --- | --- |
| W5.1 | 组件：`BtTextarea`、`BtDialog`、`BtToast`（或轻量 notify）、`BtBreadcrumb`；Form 字段统一 | Login/Admin 去掉游离原生控件（file 除外） |
| W5.2 | 替换 `window.confirm` → `BtDialog` | 删除/危险操作走组件 |
| W5.3 | **首页**：品牌英雄级呈现；全幅视觉锚（分类/自然意象）；首屏去掉 API 状态等次要信息 | 去掉 nav 后仍可辨 BioTree |
| W5.4 | 字体：`index.html` 正确加载 Token 所用家族（如 Noto Serif SC + 配套无衬线） | 无回落系统默认堆 |
| W5.5 | 动效：首页 2–3 个有意图动效（入场、CTA、背景微动）；Browse 保持克制 | 不阻塞数据加载 |
| W5.6 | 浏览/管理排版：树区与详情区层次、间距 Token 化；避免无意义卡片堆叠 | 移动端可一列使用 |

**依赖**：无硬依赖。  
**风险**：视觉主观；以「品牌测试」与 CODING_RULES §5 为验收。

---

### W6 — 多语言、异常体验、统计（P1）

| 任务 | 内容 | 验收 |
| --- | --- | --- |
| W6.1 | 前端错误：`failed` / 后端英文 message → `error.*` i18n；按 `code` 映射 | 中英切换错误文案正确 |
| W6.2 | 等级名本地化（`rank.KINGDOM` 等） | 树/详情/管理一致 |
| W6.3 | `stores/locale` 等硬编码语言名收入 locales | 无硬编码 UI 串 |
| W6.4 | **统计 API**（建议）：`GET /api/stats/summary`（总节点、按界、按 rank）；管理端可同权或公开只读 | 禁止同步「子孙全量 COUNT」；用冗余字段/预聚合 |
| W6.5 | 可选：导入结束后写 `stats_snapshot` 表，页面读快照 | 全量下统计仍快 |

**依赖**：W3 错误码稳定更佳。

---

### W7 — 测试与工程能力（P1–P2）

| 任务 | 内容 | 验收 |
| --- | --- | --- |
| W7.1 | 后端补测：Auth login/me/logout、CSRF 拒写、checkpoint resume、AccessDenied、Cache-Control 头 | 新测稳定绿 |
| W7.2 | 前端 Vitest：`debounce`、`csrf`、`markdown`、LocaleSupport 对等逻辑 | `pnpm test` |
| W7.3 | ESLint + Prettier（Vue/TS）；脚本 `lint` | CI 可跑 |
| W7.4 | Spotless（或 Checkstyle）进 Gradle | `./gradlew spotlessCheck` |
| W7.5 | CI 扩展：`typecheck`、`lint`、Spotless；保留 test/build | workflow 全绿 |
| W7.6 | 清理双重文件头（Vue 顶注释 + script 内重复） | 符合 CODING_RULES |

**依赖**：W1 resume 测依赖 W1 完成。

---

### W8 — 性能与架构抛光（P2）

| 任务 | 内容 | 验收 |
| --- | --- | --- |
| W8.1 | 详情 media 分页或「首屏 N 张 + 加载更多」 | 多图 taxon 不膨胀 payload |
| W8.2 | 公开短缓存与管理写后的一致性说明（短 TTL 即可） | 文档写清 |
| W8.3 | 搜索：监控慢查询；必要时调整 FULLTEXT 维护窗口（导入后 `OPTIMIZE` 运维笔记） | DATA_IMPORT 补充 |
| W8.4 | 进程内可选：根层（界）极小缓存（TECH_STACK 允许的无 Redis 策略） | 多实例说明「可不一致」 |
| W8.5 | 包结构：`importdata` 拆分；`TaxonSearchDao` 与 Repository 边界文档化 | 新成员可定位 |

---

## 4. API / 数据契约变更清单（跨波次）

| 变更 | 波次 | 说明 |
| --- | --- | --- |
| `GET /api/admin/import/status` | W1 可选 | 读 checkpoint |
| Admin children 仍用现有 `GET /api/taxa/children` | W2 | 前端补 page |
| `POST /api/admin/auth/logout` 前端接通 | W2 | 已有后端 |
| Security / prod 配置 | W3 | 行为随 profile |
| OpenAPI 全量路径 | W4 | 无行为变或仅文档 |
| `GET /api/stats/summary` | W6 | 新只读 |
| Media 分页查询参数 | W8 | 兼容：默认仍返回首屏 |

错误码：尽量沿用现有 `ErrorCode`；新增统计/导入状态用独立 code 段，并在 W6 做 i18n 表。

---

## 5. 前端信息架构与交互要点

```text
公开：
  /                 品牌首页（W5）
  /browse/:id?      搜索 + 树 + 详情（已有；W5/W6 抛光）
  /login            统一表单组件（W5）

管理：
  /admin            概览 + 可选统计卡片（W6）
  /admin/taxa       分页树状维护 + 移动 + 配图（W2）
  全局：用户名 + 登出（W2）
```

交互：危险操作 Dialog 确认；成功/失败 Toast；长列表虚拟滚动保持。

---

## 6. 测试策略矩阵

| 层级 | 范围 | 波次 |
| --- | --- | --- |
| 后端集成 | 导入夹具、resume、Auth、CSRF、搜索含异名、move、media | W1/W3/W7 |
| 后端单测 | LocaleSupport、RankRules、Markdown 无关的纯函数；导入解析小函数 | W1/W7 |
| 前端单测 | utils + 纯组件 | W7 |
| 手工/脚本 | 小样全量（`max-per-rank` 放开到可接受规模）看堆与耗时 | W1 后 |
| CI | test + build + lint/typecheck/spotless | W7 |

---

## 7. 风险与回滚

| 风险 | 缓解 | 回滚 |
| --- | --- | --- |
| W1 导入写坏 path/父子 | 夹具 + 小样；先 `replace=false` 试跑 | 保留旧 importer 一版 tag；DB 从备份恢复 |
| FULLTEXT 与 H2 双轨 | 测试保持 LIKE；MySQL 专用 migration | 关闭 fulltext 仅用 LIKE |
| OpenAPI 重构前端回归 | 分模块替换 fetch；先 taxon 读再写 | 保留旧 `taxon.ts` 直至绿 |
| 生产关 Swagger 影响联调 | local profile 仍开放 | 配置开关 |

---

## 8. 明确延后 / 不做

| 项 | 状态 |
| --- | --- |
| Redis、Docker、Nginx | 不做（TECH_STACK） |
| 独立搜索引擎（ES/OpenSearch） | 延后；FULLTEXT 不足再开题 |
| JWT Bearer | 否决 |
| 用户注册/多管理员复杂 RBAC | 延后；先单管理员加固 |
| 操作审计完整产品 | TECH_STACK 可选；可放 W8+ |
| MapStruct | 可选，不阻塞 |

---

## 9. 建议 PR 切片（实施时）

| PR 主题 | 含波次 | base |
| --- | --- | --- |
| docs: 改进方案与 README 对齐 | W0 | `main`（或当前最新） |
| feat: 流式/分阶段导入与 resume 强化 | W1 | `main` |
| feat: 管理端分页与登出 | W2 | `main` |
| chore: production 配置与安全收紧 | W3 | `main` |
| feat: OpenAPI 闭环与客户端切换 | W4 | `main` |
| feat: 设计系统与首页视觉 | W5 | `main` |
| feat: i18n 错误映射与 stats | W6 | `main` |
| chore: ESLint/Spotless/Vitest/CI | W7 | `main` |
| feat: media 分页与导入拆类抛光 | W8 | `main` |

每 PR：实现 → 测试 → push → 更新 PR 描述 → CI 绿再请审。

---

## 10. 验收总清单（上线门禁）

- [ ] 在目标机器上完成一次接近全量（或约定抽样规模）导入：**无 OOM**、可断点续跑  
- [ ] 公开浏览：懒加载、搜索、详情、异名、多语言回退正常  
- [ ] 管理端：任意深度 children **可翻页到达**；可登出；配图/移动可用  
- [ ] prod profile：无默认口令、Secure Cookie、文档口收敛  
- [ ] OpenAPI 与前端生成类型一致；关键写路径走统一客户端 + CSRF  
- [ ] 首页通过「去 nav 仍可辨品牌」；核心组件来自设计系统  
- [ ] CI：后端测试 + 前端 build +（W7 后）lint/typecheck  
- [ ] README / 本方案 / 专项文档无矛盾  

---

## 11. 一句话

**先让全量导入与管理端「在数据上正确且安全」（W1–W3），再锁契约（W4），然后补体验与国际化（W5–W6），最后用测试与工具链把质量钉死（W7–W8）。**
