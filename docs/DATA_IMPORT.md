# 真实分类数据导入（Catalogue of Life）

本文说明如何从 **Catalogue of Life（CoL）DwC-A** 导入动物界与植物界分类数据及俗名。

> **演进**：默认 **`app.import.rank-mode=full`** 入库完整阶元（含中间级）；可设 `legacy7` 回退旧行为。公开 API / 浏览默认 `view=simple`（七级投影），可切 `view=full`。另可导入 Description / Distribution / Media 扩展与命名学字段（见 [FULL_TAXONOMY_PLAN.md](./FULL_TAXONOMY_PLAN.md)）。

## 1. 数据源

- 官方下载页：https://www.catalogueoflife.org/data/download  
- 最新 DwC-A 直链：`https://download.checklistbank.org/col/latest_dwca.zip`（约 500MB+）
- 许可与引用：请遵循 CoL / ChecklistBank 使用条款，并在站点声明数据来源。

本地建议路径（已被 `.gitignore` 忽略）：

```bash
mkdir -p data/import
curl -L -o data/import/col_latest_dwca.zip \
  https://download.checklistbank.org/col/latest_dwca.zip
```

## 2. 导入行为

| 项 | 说明 |
| --- | --- |
| 过滤 | 仅 `taxonomicStatus=accepted`，且 `kingdom` 为配置的界（默认 Animalia、Plantae） |
| 等级 | 默认完整阶元（亚门/亚科/亚属/亚种等）；`rank-mode=legacy7` 时仅七级 |
| 父级 | full：按真实 `parentNameUsageID` 挂接；legacy7：中间级上溯到已入库七级 |
| 学名 | 种/种下用属名+加词拼规范名；写入命名人、原文名、`namePublishedIn`、命名法字段（有则） |
| 俗名 | `VernacularName.tsv`：多语言 + `isPreferredName`；写入 `taxon_i18n` |
| 描述 | `Description.tsv`（有则）→ `taxon_i18n.description`（不覆盖已有非空） |
| 分布 | `Distribution.tsv`（有则）→ `taxon_distribution` |
| 媒体 | `Media.tsv`/`Multimedia.tsv`（有则）外链；**须可识别 license** |
| 数据集 | `eml.xml` 标题/版本 → `import_dataset_meta`（页脚/管理端引用） |
| 外部 ID | 写入 `taxon.external_source=col` + `taxon.external_id` |
| replace | `true` 时清空既有 `taxon` / `taxon_i18n` / `taxon_media` / `taxon_distribution`（管理员账号保留） |
| 写入路径 | **暂存表流式写入**（`import_col_*`）后按等级落库；JVM 不常驻全量节点 Map |
| 断点续跑 | `import_checkpoint` 记录 STAGE / RANK_* / VERNACULAR / SYNONYM / DESCRIPTIONS…；`GET /api/admin/import/status` 可查 |
| 异名 | 导入非 accepted 且带 `acceptedNameUsageID` 的记录到 `taxon_synonym` |
| child_count | 导入结束按 `parent_id` 分组批量回写 |

## 3. 运行导入

前置：MySQL 8 已创建库 `biotree`，账号与 `application.yml` 一致。

### 全量（动物界 + 植物界）

**Windows（推荐）：** 双击或在仓库根目录执行：

```bat
scripts\import-col-full.bat
```

**Linux / macOS：**

```bash
./scripts/import-col-full.sh
```

脚本会：

1. 若不存在则下载 `data/import/col_latest_dwca.zip`（Windows 路径为 `data\import\...`）
2. 确认后执行全量导入（`replace=true`，`rank-mode=full`，`max-per-rank=0`，并开启俗名/异名/描述/分布/媒体）
3. 导入结束自动退出

**断点续跑：**

```bat
scripts\import-col-resume.bat
```

```bash
./scripts/import-col-resume.sh
```

在已有 checkpoint 且 zip 仍在时使用；`replace=false` + `resume=true`，勿与全量 replace 混用。

**命令行（手动 gradlew）：**

```bash
cd backend
./gradlew bootRun --args='\
  --app.import.enabled=true \
  --app.import.dwca-path=../data/import/col_latest_dwca.zip \
  --app.import.replace=true \
  --app.import.resume=false \
  --app.import.max-per-rank=0 \
  --app.import.rank-mode=full \
  --app.import.import-vernaculars=true \
  --app.import.import-synonyms=true \
  --app.import.import-descriptions=true \
  --app.import.import-distributions=true \
  --app.import.import-media=true'
```

导入进程结束后会自动退出。全量可达百万级节点（完整阶元更多），首次导入耗时取决于磁盘与 MySQL 配置，建议加大 `innodb_buffer_pool_size`。

### 试跑（每级限条数）

```bash
cd backend
./gradlew bootRun --args='\
  --app.import.enabled=true \
  --app.import.dwca-path=../data/import/col_latest_dwca.zip \
  --app.import.replace=true \
  --app.import.max-per-rank=500 \
  --app.import.import-vernaculars=true'
```

### 试跑结果（本仓库开发环境已验证）

对 `col_latest_dwca.zip` 使用 `max-per-rank=300` 时约导入：

- 扫描 Taxon 行约 540 万
- 写入约 1414 个分类节点（界 2 / 门 46 / 纲 166 / 目科属种各 300）
- 写入约 175 条中英俗名

公开 API `GET /api/taxa/children` 可返回 Animalia / Plantae（默认 `view=simple` 为七级投影）。

全量将 `max-per-rank=0` + `rank-mode=full`，节点量显著高于仅七级，请预留磁盘与导入时间。

## 4. 配置项

见 `application.yml` 中 `app.import`：

- `enabled`：是否启动即导入  
- `dwca-path`：zip 路径  
- `replace`：是否先清空分类数据  
- `resume`：是否断点续跑  
- `kingdoms`：界过滤列表  
- `max-per-rank`：每级上限，`0` 不限制  
- `rank-mode`：`full`（完整阶元）或 `legacy7`  
- `import-vernaculars` / `import-synonyms`：俗名与异名  
- `import-descriptions` / `import-distributions` / `import-media`：DwC 扩展（缺文件则跳过）  

## 5. 验证

```bash
# 公开 API
curl 'http://localhost:8080/api/taxa/children?locale=zh-CN'
curl 'http://localhost:8080/api/taxa/search?q=Homo&locale=en'
curl 'http://localhost:8080/api/stats/summary'
```

前端 `/browse` 懒加载树应能从界展开；可切换「完整阶元」；有俗名/描述/分布/外链配图时详情区可见。

## 6. 说明

- Description / Distribution / Media 扩展在 CoL 包内**有则导入**；缺文件则跳过。无 license 的媒体外链会被丢弃。人工编辑的非空 description 不会被导入覆盖。  
- 同物异名已导入至 `taxon_synonym`（搜索与详情可展示）。  
- 默认完整阶元入库；浏览默认七级投影（`view=simple`）。若只要旧七级数据，设 `rank-mode=legacy7` 后重导。  
