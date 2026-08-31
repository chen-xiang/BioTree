-- 为权威库导入增加外部 ID 映射字段

ALTER TABLE taxon ADD COLUMN external_source VARCHAR(32) NULL;
ALTER TABLE taxon ADD COLUMN external_id VARCHAR(64) NULL;

CREATE UNIQUE INDEX uk_taxon_external ON taxon (external_source, external_id);
CREATE INDEX idx_taxon_external_id ON taxon (external_id);
