-- 按阶元 keyset 分页读取暂存表，避免 OFFSET 随进度二次方变慢

CREATE INDEX idx_import_col_taxon_rank_ext ON import_col_taxon (taxon_rank, external_id);
