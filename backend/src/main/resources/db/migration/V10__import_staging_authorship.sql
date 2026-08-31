-- 导入暂存表补充命名人与原始等级

ALTER TABLE import_col_taxon ADD COLUMN scientific_name_authorship VARCHAR(255) NULL;
ALTER TABLE import_col_taxon ADD COLUMN taxon_rank_raw VARCHAR(64) NULL;
