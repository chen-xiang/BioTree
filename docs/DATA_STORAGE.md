# BioTree 数据存储设计

本文约定分类数据、多语言内容与配图的存储方式。与 [TECH_STACK.md](./TECH_STACK.md)、[PERFORMANCE.md](./PERFORMANCE.md) 配套。

---

## 1. 数据范围

- 尽量覆盖完整的 **界 / 门 / 纲 / 目 / 科 / 属 / 种**。
- 重点覆盖 **动物界** 与 **植物界**。
- 节点需支持 **详细介绍**（多语言）及 **配图**。
- 结构数据可接近全量或按权威库导入；介绍与配图允许按优先级逐步补全（常见类群优先）。

---

## 2. 总原则

| 原则 | 说明 |
| --- | --- |
| 能查询、能关联、要权限编辑的 | 存 **MySQL** |
| 大二进制（图片等） | 存 **文件存储**（Local → 可切换 OSS） |
| 外部开放数据（CoL / GBIF 等） | 仅作 **导入原料**，不是运行时主存 |
| 界面文案 ≠ 物种内容多语言 | UI 用 vue-i18n；名称/介绍/图注用数据库 |

**禁止**将整棵分类树以单一大 JSON / Markdown 文件作为主存储。

---

## 3. 存什么、存哪里

| 数据 | 存储位置 | 说明 |
| --- | --- | --- |
| 层级、学名、父节点、rank | **MySQL** `taxon` | 树形结构、校验、事务 |
| 各语言俗名、摘要、详细介绍 | **MySQL** `taxon_i18n` | 按 `locale` 绑定节点 |
| 配图 / 附件二进制 | **Local / OSS** | 经 `StorageService` |
| 配图元数据（路径、排序、版权、语种等） | **MySQL** `taxon_media` | 与节点关联，便于管理 |
| 管理端账号等 | **MySQL** | 与 Session 鉴权配合 |

```text
MySQL                                文件存储（Local / OSS）
─────────────────────────            ─────────────────────
taxon（树 + 学名 + rank + 路径等）
taxon_i18n（名称 / 介绍 / 语言）
taxon_media（path/key、排序…）  ──→  实际图片文件
admin_user …
```

---

## 4. 多语言模型

### 4.1 两类多语言

| 类型 | 方案 |
| --- | --- |
| **界面**（按钮、菜单、提示） | 前端 **vue-i18n**（代码侧语言包） |
| **内容**（俗名、详细介绍、图注） | **数据库** 多语言行，按 `locale` 读取 |

学名（拉丁名）通常 **不翻译**，放在 `taxon` 主表。

### 4.2 内容表示意

```text
taxon_i18n
  id
  taxon_id          -- FK → taxon
  locale            -- 如 zh-CN、en-US
  common_name       -- 俗名
  summary           -- 短摘要（列表/预览可用）
  description       -- 详细介绍（建议 Markdown，TEXT/MEDIUMTEXT）
  UNIQUE(taxon_id, locale)
```

---

## 5. 表结构草案（核心）

字段可在实现时微调；语义应保持稳定。

### 5.1 `taxon`

| 字段 | 说明 |
| --- | --- |
| `id` | 主键 |
| `parent_id` | 父节点，根为 `NULL` |
| `rank` | 枚举：kingdom / phylum / class / order / family / genus / species 等 |
| `scientific_name` | 学名 |
| `materialized_path` | 如 `/1/5/23/`，加速祖先链与面包屑（见性能文档） |
| `child_count` | 直接子节点数，冗余字段，减少 COUNT |
| `is_accepted` | 是否接受名（为异名预留，可二期启用） |
| `created_at` / `updated_at` | 审计 |
| `created_by` | 可选 |

约束建议：

- 同一 `parent_id` 下 `scientific_name` 唯一。
- 写入时校验 parent 的 `rank` 与当前 `rank` 的合法父子关系。
- 存在子节点时默认 **禁止删除**（或明确级联策略并写测试）。

### 5.2 `taxon_i18n`

见 §4.2。详情接口按 `taxon_id + locale` 取 **一行**；列表仅取 `common_name` / `summary` 等短字段。

### 5.3 `taxon_media`

| 字段 | 说明 |
| --- | --- |
| `id` | 主键 |
| `taxon_id` | FK → taxon |
| `storage_key` | Local 相对路径或 OSS object key |
| `url` 或访问方式 | 由存储服务生成/约定 |
| `mime_type` / `width` / `height` / `size_bytes` | 可选元数据 |
| `sort_order` | 展示顺序 |
| `locale` | 可选；图注语种或仅某语言展示 |
| `caption` | 图注（若需多语言图注，可再拆表或 JSON，首期单语/随 locale 字段即可） |
| `license` / `attribution` | 版权与署名 |
| `created_at` | 审计 |

**禁止**将图片二进制以 BLOB 形式存入 MySQL。

---

## 6. 文件存储

与技术栈一致：策略模式，配置切换。

```text
StorageService
├── LocalFileStorageService   ← app.storage.type=local（前期）
└── OssFileStorageService     ← app.storage.type=oss（后续）
```

- 业务只依赖接口：上传、删除、生成访问 URL。
- 元数据进 `taxon_media`；对象进 Local/OSS。
- 本地根目录（如 `./data/files`）须纳入 `.gitignore`。

---

## 7. 数据进入路径

```text
权威分类库 dump（文件）
        ↓ 导入任务 / 脚本
     MySQL taxon（+ 可选初始 i18n）
        ↓ 编辑与运营
  taxon_i18n 介绍补全、taxon_media 配图上传
```

- 全量结构靠 **导入**；介绍与配图靠 **管理端维护** 与后续批量补充。
- 导入逻辑与在线读写分离，避免用同步 HTTP 请求吞下整个 dump。

---

## 8. 一句话

**树 + 多语言名称/介绍 + 媒体元数据 = MySQL；图 = Local/OSS；开放数据文件只是导入原料。**
