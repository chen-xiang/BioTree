-- DwC 内容补齐：命名学元数据、分布、媒体外链、数据集元数据、俗名偏好

ALTER TABLE taxon ADD COLUMN scientific_name_verbatim VARCHAR(512) NULL;
ALTER TABLE taxon ADD COLUMN name_published_in VARCHAR(512) NULL;
ALTER TABLE taxon ADD COLUMN name_according_to VARCHAR(512) NULL;
ALTER TABLE taxon ADD COLUMN nomenclatural_code VARCHAR(32) NULL;
ALTER TABLE taxon ADD COLUMN nomenclatural_status VARCHAR(64) NULL;

ALTER TABLE import_col_taxon ADD COLUMN scientific_name_verbatim VARCHAR(512) NULL;
ALTER TABLE import_col_taxon ADD COLUMN name_published_in VARCHAR(512) NULL;
ALTER TABLE import_col_taxon ADD COLUMN name_according_to VARCHAR(512) NULL;
ALTER TABLE import_col_taxon ADD COLUMN nomenclatural_code VARCHAR(32) NULL;
ALTER TABLE import_col_taxon ADD COLUMN nomenclatural_status VARCHAR(64) NULL;

ALTER TABLE taxon_i18n ADD COLUMN preferred TINYINT(1) NOT NULL DEFAULT 0;

ALTER TABLE taxon_media MODIFY COLUMN storage_key VARCHAR(512) NULL;
ALTER TABLE taxon_media ADD COLUMN source_url VARCHAR(1024) NULL;

CREATE TABLE taxon_distribution (
    id                   BIGINT PRIMARY KEY AUTO_INCREMENT,
    taxon_id             BIGINT       NOT NULL,
    country_code         VARCHAR(16)  NULL,
    locality             VARCHAR(512) NULL,
    establishment_means  VARCHAR(64)  NULL,
    threat_status        VARCHAR(64)  NULL,
    source_text          VARCHAR(512) NULL,
    CONSTRAINT fk_taxon_distribution_taxon FOREIGN KEY (taxon_id) REFERENCES taxon (id)
);

CREATE INDEX idx_taxon_distribution_taxon_id ON taxon_distribution (taxon_id);

CREATE TABLE import_dataset_meta (
    source_key      VARCHAR(64)  NOT NULL PRIMARY KEY,
    dataset_title   VARCHAR(255) NULL,
    dataset_version VARCHAR(64)  NULL,
    source_url      VARCHAR(512) NULL,
    imported_at     TIMESTAMP(3) NOT NULL
);
