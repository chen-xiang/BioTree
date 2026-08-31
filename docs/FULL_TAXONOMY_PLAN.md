# BioTree 完整阶元与 DwC 信息补齐实施方案

本文落实产品决策：**库内存含中间级的完整阶元体系**；**默认按林奈七级（simple）展示**；用户可切换 **完整阶元（full）**。并同步规划命名人/文献、分布与描述、媒体、多语言俗名等 DwC 缺口的补齐。

配套文档：[TECH_STACK.md](./TECH_STACK.md) · [DATA_STORAGE.md](./DATA_STORAGE.md) · [DATA_IMPORT.md](./DATA_IMPORT.md) · [PERFORMANCE.md](./PERFORMANCE.md) · [IMPROVEMENT_PLAN.md](./IMPROVEMENT_PLAN.md)

> **实施进度（`cursor/implement-full-taxonomy-60ae` / `cursor/dwc-fields-enrich-60ae`）**：R1–R4、R5（命名人 + 发表文献/命名法/原文名/数据集元数据）、R6（多语言俗名 + 其它语言区）、R7（Description）、R8（Distribution）、R9（Media 外链，须 license）已落地。R10（simple 投影加速等）后续。

**难度表述**：用「改动面 / 依赖 / 风险」描述，不以日历工期估算。

---

## 0. 决策摘要

| 议题 | 结论 |
| --- | --- |
| 数据源阶元 | CoL DwC-A / Darwin Core **本身含中间级**；七级是本站曾做的裁剪 |
| 是否入库完整阶元 | **是** —— 更贴近权威源，可支撑专业浏览与正确挂接 |
| 默认 UI | **`view=simple`**：只展示界门纲目科属种七级 |
| 完整 UI | **`view=full`**：按真实父子展开全部阶元 |
| 存储策略 | **单树**（真实 `parent_id`）；展示层投影，**不维护两套树** |
| 其它 DwC 缺口 | 命名人、描述、俗名扩语言优先；分布/媒体分阶段；文献可后置精简版 |

---

## 1. 现状与差距

### 1.1 已具备

- 七级枚举 `TaxonRank` + 严格「恰好高一级」的 `TaxonRankRules`
- CoL 导入：仅 `mapRank` 命中的七级 accepted 落库；中间级被跳过并上溯父级
- 浏览：懒加载 children、搜索、详情、面包屑、配图（管理端上传）
- 俗名：仅 `en` / `zh-CN`
- 异名：`taxon_synonym` 基础表

### 1.2 主要差距

| 差距 | 影响 |
| --- | --- |
| 中间级不入库 | 与 DwC 不一致；无法专业浏览完整层级 |
| 等级规则过严 | 无法表达「种→亚属→属」等真实拓扑 |
| 无 view 投影 | 一旦全量入库，默认树会对普通用户过深 |
| 命名人/原文名缺失 | 学名不完整 |
| 描述/分布/媒体扩展未导入 | 内容层依赖人工 |
| 俗名语言过窄 | 国际化不足 |
| 来源版本元数据弱 | 难对账、难引用 |

---

## 2. 目标与原则

### 2.1 目标

1. **数据完整**：动物界/植物界 accepted 分类按权威源阶元入库（可配置黑名单排除极冷阶元）。
2. **体验分层**：默认七级浏览不增加心智负担；一键切换完整阶元。
3. **契约清晰**：所有树/列表 API 显式 `view=simple|full`（默认 `simple`）。
4. **内容可增长**：命名人、多语言俗名、描述优先自动灌入；分布/媒体可渐进。
5. **性能底线不变**：永不整树下发；继续分页、懒加载、虚拟列表（见 PERFORMANCE）。

### 2.2 原则

- **单树真源**：只存一份真实父子；simple 是查询投影，不是第二份数据。
- **导入可重跑**：全量 replace / resume 语义与现有 checkpoint 兼容；等级模型变更要可迁移。
- **管理端偏完整**：编辑、移动、校验以 full 拓扑为准；可提供「七级预览」。
- **小 PR、可回滚**：先模型与导入，再 API 投影，再 UI 开关，再内容扩展。
- **不做**：Redis / ES；不引入 Occurrence 观测主线；不一次做全球精细 GIS。

---

## 3. 概念模型

### 3.1 两种视图

```text
真实树（DB）:
  Kingdom → Phylum → Subphylum → Class → … → Family → Subfamily → Genus → Subgenus → Species → Subspecies

simple 投影（默认）:
  Kingdom → Phylum → Class → … → Family → Genus → Species
  （Sub* 等对用户隐藏；展开 Family 时直接看到「下属的属」，即使中间有 Subfamily）

full 展示:
  按真实 parent_id 逐级展开
```

### 3.2 simple 下「子节点」定义

对节点 `N`，在 `view=simple` 时：

- 可见等级集合 `S = {KINGDOM, PHYLUM, CLASS, ORDER, FAMILY, GENUS, SPECIES}`
- 返回：所有满足下列条件的节点集合（分页）：
  - 自身 `rank ∈ S`
  - 在 materialized_path 上以 `N` 为祖先
  - 从 `N` 到该节点的路径上，**不存在其它属于 `S` 的严格中间祖先**  
    （即：下一档「可见」后代）

实现备选（按侵入性排序，方案阶段选定一种）：

| 方案 | 做法 | 优点 | 风险 |
| --- | --- | --- | --- |
| A. 查询时 BFS/受限递归 | 按真实 children 扩展，跳过非 S，收集 S | 无需新表 | 宽扇出科属下要注意批查与上限 |
| B. 冗余 `simple_parent_id` | 导入/写时维护「最近七级祖先」 | children 查询简单 | 双写一致性 |
| C. 闭包表 | `taxon_closure(ancestor, descendant, depth)` | 任意祖先查询快 | 表大、写放大 |

**推荐首期：A（正确性优先）+ path 前缀辅助；若热点科/属测出过慢，再对高频路径加 B。**

### 3.3 等级模型

扩展 `TaxonRank`（或「规范枚举 + raw 字符串」双轨）：

| 字段 | 说明 |
| --- | --- |
| `taxon_rank` | 规范枚举（含中间级）；未知新值可映射 `OTHER` 并保留 raw |
| `taxon_rank_raw` | DwC 原始 `taxonRank` 字符串 |
| `rank_order` | 可比深度（整数），用于排序与「是否高于」校验，替代「恰好差一级」 |

父子合法性改为：

- `child.rank_order > parent.rank_order`（严格更深）
- 可选：同界系、禁止环、禁止跨过大跨度的配置策略
- KINGDOM 仍允许 `parent_id IS NULL`

**首批建议纳入的中间级（可配置）：**  
`subkingdom, infrakingdom, superphylum, subphylum, infraphylum, superclass, subclass, infraclass, superorder, suborder, infraorder, superfamily, subfamily, tribe, subtribe, subgenus, species_aggregate, subspecies, variety, form`  
（名称以 CoL 实际取值为准做映射表。）

---

## 4. 工作波次总览

| 波次 | 主题 | 改动面 | 主要风险 |
| --- | --- | --- | --- |
| **R0** | 文档与契约草案 | 文档、OpenAPI 草图 | 低 |
| **R1** | 等级模型与校验重构 | 枚举、Flyway、RankRules、管理 CRUD | 高（破坏旧「恰好高一级」） |
| **R2** | 导入完整阶元 | ColDwcaImporter、checkpoint、夹具 | 高（数据量、续跑） |
| **R3** | API `view=simple\|full` | TaxonService children/面包屑/详情 | 高（投影正确性） |
| **R4** | 浏览/管理 UI 双模式 | Browse、Admin、i18n、偏好记忆 | 中 |
| **R5** | 命名人 + 学名原文 + 来源元数据 | 表字段、导入、详情展示 | 中 |
| **R6** | 俗名多语言扩展 | locale 映射、i18n UI | 低～中 |
| **R7** | 描述导入 | Description 扩展或精选字段 → `taxon_i18n` | 中（质量不均） |
| **R8** | 分布（国家/地区级） | 新表或 JSON、详情区块 | 中 |
| **R9** | 媒体 URL 导入 | media 元数据 + 外链/可选拉平 | 中（版权） |
| **R10** | 性能与运维抛光 | simple 投影加速、统计、文档 | 中 |

并行：**R5∥R6** 可在 R3 后与 R4 交错；**R7–R9** 依赖 R2 稳定；**R10** 贯穿测压后。

---

## 5. 详细方案

### R0 — 文档与契约草案

| 任务 | 验收 |
| --- | --- |
| 本文定稿；`DATA_STORAGE` / `DATA_IMPORT` / `PERFORMANCE` 增加交叉引用 | 文档互链 |
| 约定 API：`view` 默认 `simple`；OpenAPI 草图 | 评审无歧义 |
| 明确「首批中间级白名单」与 `OTHER` 策略 | 写入 DATA_IMPORT |

**不改运行时行为。**

---

### R1 — 等级模型与校验

| 任务 | 内容 | 验收 |
| --- | --- | --- |
| R1.1 | Flyway：`taxon_rank` 放开长度/枚举；新增 `taxon_rank_raw`、`rank_order` | 迁移可逆说明 |
| R1.2 | 扩展 Java 枚举 + `rank_order` 映射表；未知 raw → OTHER | 单测覆盖映射 |
| R1.3 | `TaxonRankRules` 改为基于 `rank_order` 的深度校验 | 旧七级夹具仍绿；新增亚属→属用例 |
| R1.4 | 管理端创建/移动：允许合法中间级；表单等级下拉分组（主级/中间级） | 非法挂接仍 400 |
| R1.5 | 种子/演示数据兼容 | test profile 绿 |

**风险**：已有七级数据 `rank_order` 回填脚本必须幂等。

---

### R2 — 导入完整阶元

| 任务 | 内容 | 验收 |
| --- | --- | --- |
| R2.1 | `ColNameUtils.mapRank` 扩展白名单；保留 raw | 夹具含亚种/亚科 |
| R2.2 | 去掉「非七级 continue」；仍过滤 kingdom、accepted | 试导入节点数 > 仅七级基线 |
| R2.3 | 父级解析：真实 parent；不再为七级视图上溯丢弃节点 | path 与 parent 一致 |
| R2.4 | checkpoint 兼容；`replace` 全量重导说明 | resume 测绿 |
| R2.5 | 配置：`app.import.rank-mode=full\|legacy7`（过渡期） | 可回退 legacy7 |
| R2.6 | 更新 `import-col-*.sh/.bat` 与 DATA_IMPORT | 文档可执行 |

**明确**：全量体积上升；试跑先用 `max-per-rank` 或抽样包对比 simple/full 子节点 API。

---

### R3 — 公开/管理查询的 view 投影

| 任务 | 内容 | 验收 |
| --- | --- | --- |
| R3.1 | `GET /api/taxa/children?view=simple\|full` | simple 默认；契约测 |
| R3.2 | 详情 breadcrumbs：simple 只含七级；full 含全部 | 两种快照测 |
| R3.3 | `hasChildren` / `childCount`：按 **当前 view** 语义返回或拆字段 `childCountSimple` / `childCountFull` | 前端不误判空枝 |
| R3.4 | 搜索：默认可命中中间级；响应带 `rank`；可选 `ranks=` 过滤 | 文档说明 |
| R3.5 | 管理列表：默认 `full`（或显式参数） | 编辑所见即真树 |

**推荐 childCount 策略（首期）：**  
保留物理 `child_count`（真实直接子节点数）；API 增加：

- `directChildCount`（真实）
- `hasVisibleChildren`（相对 view）  
避免为两种视图维护两套冗余计数，除非测压证明必要。

---

### R4 — 前端双模式

| 任务 | 内容 | 验收 |
| --- | --- | --- |
| R4.1 | 浏览页「简易 / 完整」开关；写入 `localStorage` + 可选 query `?view=` | 刷新保持 |
| R4.2 | 树、分页、详情、面包屑随 view 请求 | 切换后重载当前枝 |
| R4.3 | 等级 i18n：中间级中英文案 | 无裸枚举 |
| R4.4 | 管理端：默认完整；提供「按七级预览」只读 | 移动仍按真树 |
| R4.5 | 移动端 Tab 与长面包屑：完整模式可折叠中间段 | 可点开 |

---

### R5 — 命名人、学名原文、来源元数据

| 任务 | 内容 | 验收 |
| --- | --- | --- |
| R5.1 | `taxon.scientific_name_authorship`、`scientific_name_with_authorship`（或存原文 `scientific_name_verbatim`） | 导入填充 |
| R5.2 | `external_dataset` / `source_version` / `imported_at`（库级或节点级，至少库级配置表） | 关于/页脚可引用 CoL |
| R5.3 | 详情展示：规范名 + 命名人；简易模式可不强调 | UI 验收 |
| R5.4 | 文献：首期仅 `name_published_in` 短文本字段；完整 Reference 扩展表延后 | 有则显示 |

---

### R6 — 俗名多语言

| 任务 | 内容 | 验收 |
| --- | --- | --- |
| R6.1 | 扩展 `ColNameUtils.mapLocale`：至少 ja / es / fr / de / ru / pt / ko（可配置） | 导入计数上升 |
| R6.2 | 前端语言与内容 locale 策略：UI 语言优先，回退 en → zh-CN → 任意 | 与现有 LocaleSupport 对齐 |
| R6.3 | 详情「其它语言俗名」折叠区 | 不撑爆首屏 |

---

### R7 — 描述导入

| 任务 | 内容 | 验收 |
| --- | --- | --- |
| R7.1 | 探测 CoL 包内 Description（或同类）扩展；无则跳过 | 导入健壮 |
| R7.2 | 映射到 `taxon_i18n.description` / `summary`（按语言）；不覆盖人工非空（策略可配） | 策略测 |
| R7.3 | 质量：截断过长、剥危险 HTML；Markdown 友好 | 安全测 |

---

### R8 — 分布（国家/地区级）

| 任务 | 内容 | 验收 |
| --- | --- | --- |
| R8.1 | 表 `taxon_distribution(taxon_id, country_code, locality, status, source)` | 迁移 |
| R8.2 | 导入 Distribution 扩展（有则） | 抽样正确 |
| R8.3 | 详情「分布」列表；不做地图多边形 | UI 克制 |

---

### R9 — 媒体 URL 导入

| 任务 | 内容 | 验收 |
| --- | --- | --- |
| R9.1 | `taxon_media` 支持 `source_url` + `storage_key` 可空（外链模式） | 兼容本地上传 |
| R9.2 | 导入 Media 扩展：仅保留含许可/可识别来源的记录 | 版权字段必填策略 |
| R9.3 | 浏览画廊：外链与本地混排；失败占位 | 体验可接受 |

---

### R10 — 性能与运维

| 任务 | 内容 | 验收 |
| --- | --- | --- |
| R10.1 | simple children 热点用批查/可选 `simple_parent_id` | 慢查询可接受 |
| R10.2 | stats：按 rank 分布含中间级；simple 下可另给七级汇总 | 管理概览 |
| R10.3 | 全量导入磁盘/时间基线写入 DATA_IMPORT | 运维可预期 |
| R10.4 | 回归：七级夹具 + 含亚种夹具 + view 矩阵 | CI 绿 |

---

## 6. API / 数据契约变更清单

| 变更 | 波次 | 兼容策略 |
| --- | --- | --- |
| `taxon_rank` 扩展 + `taxon_rank_raw` + `rank_order` | R1 | 旧七级数据回填 order |
| 导入默认 full ranks（可 `legacy7`） | R2 | 配置开关 |
| `GET .../children?view=` | R3 | 默认 `simple`，旧客户端行为接近「只要七级」 |
| 详情 `breadcrumbs` 随 view 变化 | R3 | 文档说明 |
| 列表项增加 `rankRaw`（可选） | R3 | 可选字段 |
| 命名人/原文名字段 | R5 | 可空 |
| 俗名更多 locale | R6 | 已有表结构 |
| `taxon_distribution` | R8 | 新表 |
| media 外链 | R9 | 可空 storage_key |

---

## 7. 前端信息架构

```text
公开 /browse：
  [简易 ▼| 完整]   ← 默认简易
  搜索（可命中中间级，结果标注等级）
  树（随 view） + 详情（面包屑随 view）

管理 /admin/taxa：
  默认完整阶元编辑
  可选「七级预览」只读
  等级下拉：主级 / 中间级分组
```

交互要点：

- 切换 view 时保留当前选中节点 id；若该节点在 simple 下「不可见」，跳到最近可见祖先并 Toast 提示。
- 完整模式面包屑过长：首尾 + 中间折叠。

---

## 8. 测试策略

| 层级 | 范围 |
| --- | --- |
| 单测 | rank 映射、rank_order 校验、simple 可见后代算法（纯函数） |
| 集成 | 导入夹具（含亚属/亚种）；children view 矩阵；移动跨中间级 |
| 前端 | view 切换状态、localStorage、面包屑折叠 |
| 试压 | 全量或大抽样：simple 展开动物高多样性科的耗时与堆 |

---

## 9. 风险与回滚

| 风险 | 缓解 | 回滚 |
| --- | --- | --- |
| 全量节点暴涨导致导入/磁盘压力 | `max-per-rank`、分界导入、磁盘基线 | `rank-mode=legacy7` 重导 |
| simple 投影错误（丢孩/多重） | 算法单测 + 夹具金样 | 临时强制 `view=full` |
| 管理端误把中间级当七级移动 | 校验基于 rank_order；UI 标注 | 限制仅管理员 full |
| 描述/媒体版权噪声 | 白名单许可、不覆盖人工 | 关闭对应 import 开关 |
| 旧前端假设仅七级 | 默认 simple；枚举向前兼容 | API 版本或字段渐进 |

---

## 10. 明确延后 / 不做（本方案范围外）

| 项 | 说明 |
| --- | --- |
| 全球精细分布几何 / 地图产品 | 仅国家或文本到即可 |
| GBIF Occurrence 观测主库 | 另一产品线 |
| 完整文献图谱、模式标本专项 | 仅短文字段 |
| 双树物化（simple 树物理拷贝） | 否决；最多冗余 `simple_parent_id` |
| Redis / 独立搜索引擎 | 仍延后 |

---

## 11. 建议 PR 切片

| PR 主题 | 波次 | 建议 base |
| --- | --- | --- |
| docs: 完整阶元与 DwC 补齐方案 | R0 | 当前主干 tip |
| feat: 扩展等级模型与校验 | R1 | 合入 R0 后 |
| feat: CoL 导入完整阶元 | R2 | R1 |
| feat: taxa API view=simple\|full | R3 | R2 |
| feat: 浏览/管理双模式 UI | R4 | R3 |
| feat: 命名人与来源元数据 | R5 | R3+ |
| feat: 俗名多语言 | R6 | R2+ |
| feat: 描述导入 | R7 | R2+ |
| feat: 分布表与导入 | R8 | R2+ |
| feat: 媒体外链导入 | R9 | R2+ |
| perf: simple 投影与文档基线 | R10 | 视测压 |

---

## 12. 与 IMPROVEMENT_PLAN 的关系

- [IMPROVEMENT_PLAN.md](./IMPROVEMENT_PLAN.md) 解决平台硬化、导入内存、工程化等 **W0–W8**（多数已落地）。
- **本方案**在其之上扩展「分类学深度与 DwC 内容完整度」，波次前缀 **R***，避免与 W* 混淆。
- 实施前置：建议已具备流式导入、分页、CSRF、OpenAPI 客户端等能力（见改进方案进度说明）。

---

## 13. 一句话路线

**先把完整阶元正确存进单树并可用配置回退，再把默认体验锁在七级投影上，然后按命名人 → 俗名 → 描述 → 分布 → 媒体的顺序补齐 DwC 内容；全程不破坏「不整树下发」的性能约束。**
