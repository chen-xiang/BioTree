# 真实分类数据导入（Catalogue of Life）

本文说明如何从 **Catalogue of Life（CoL）DwC-A** 导入动物界与植物界的界门纲目科属种数据及中英俗名。

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
| 等级 | 仅七级：kingdom / phylum / class / order / family / genus / species |
| 父级 | 若直系父级为亚属等中间等级，则沿 `parentNameUsageID` 向上找到已导入的七级节点 |
| 学名 | 种优先 `genericName + specificEpithet`；高等级去掉命名人信息 |
| 俗名 | 读取 `VernacularName.tsv`：`eng→en`，`zho/zh/chi→zh-CN` |
| 外部 ID | 写入 `taxon.external_source=col` + `taxon.external_id` |
| replace | `true` 时清空既有 `taxon` / `taxon_i18n` / `taxon_media`（管理员账号保留） |
| 写入路径 | 按等级排序后 **JDBC 批量 INSERT**（可配置 `commit-batch-size`）+ 批量回写 `materialized_path`；每批独立事务 |
| 断点续跑 | `import_checkpoint` 记录阶段与进度；`resume=true` 时跳过已写入 external_id，中断后可续跑（勿再 `replace=true`） |
| 异名 | 导入非 accepted 且带 `acceptedNameUsageID` 的记录到 `taxon_synonym` |
| child_count | 导入结束按 `parent_id` 分组批量回写 |

## 3. 运行导入

前置：MySQL 8 已创建库 `biotree`，账号与 `application.yml` 一致。

### 全量（动物界 + 植物界）

**Windows（推荐）：** 双击或在仓库根目录执行：

```bat
scripts\import-col-full.bat
```

脚本会：

1. 若不存在则下载 `data\import\col_latest_dwca.zip`
2. 确认后执行全量导入（`replace=true`，`max-per-rank=0`）
3. 导入结束自动退出

**命令行（Linux / macOS / Windows Git Bash）：**

```bash
cd backend
./gradlew bootRun --args='\
  --app.import.enabled=true \
  --app.import.dwca-path=../data/import/col_latest_dwca.zip \
  --app.import.replace=true \
  --app.import.max-per-rank=0 \
  --app.import.import-vernaculars=true'
```

导入进程结束后会自动退出。全量可达百万级节点，首次导入耗时取决于磁盘与 MySQL 配置，建议加大 `innodb_buffer_pool_size`。

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

公开 API `GET /api/taxa/children` 可返回 Animalia / Plantae。

全量将 `max-per-rank=0`，节点量约 **200 万+**（仅七级 accepted），请预留磁盘与导入时间。

## 4. 配置项

见 `application.yml` 中 `app.import`：

- `enabled`：是否启动即导入  
- `dwca-path`：zip 路径  
- `replace`：是否先清空分类数据  
- `kingdoms`：界过滤列表  
- `max-per-rank`：每级上限，`0` 不限制  
- `import-vernaculars`：是否导入俗名  

## 5. 验证

```bash
# 公开 API
curl 'http://localhost:8080/api/taxa/children?locale=zh-CN'
curl 'http://localhost:8080/api/taxa/search?q=Homo&locale=en'
```

前端 `/browse` 懒加载树应能从界展开到种；有俗名的节点会显示中文/英文俗名。

## 6. 说明

- 详细介绍（长描述）与配图不在 CoL DwC-A 默认核心中，需后续运营补录或另接数据源。  
- 同物异名已导入至 `taxon_synonym`（搜索与详情可展示）。  
- 亚种及更低等级当前跳过，与产品七级模型一致。  
