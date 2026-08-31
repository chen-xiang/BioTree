-- 导入断点与异名表（H2 / MySQL 通用）

CREATE TABLE import_checkpoint (
    job_key         VARCHAR(64)  NOT NULL PRIMARY KEY,
    phase           VARCHAR(32)  NOT NULL,
    processed_count INT          NOT NULL DEFAULT 0,
    total_hint      INT          NULL,
    detail_json     VARCHAR(1024) NULL,
    updated_at      TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
);

CREATE TABLE taxon_synonym (
    id               BIGINT PRIMARY KEY AUTO_INCREMENT,
    taxon_id         BIGINT       NOT NULL,
    scientific_name  VARCHAR(255) NOT NULL,
    external_source  VARCHAR(32)  NULL,
    external_id      VARCHAR(64)  NULL,
    created_at       TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_taxon_synonym_taxon FOREIGN KEY (taxon_id) REFERENCES taxon (id),
    CONSTRAINT uk_taxon_synonym_ext UNIQUE (external_source, external_id)
);

CREATE INDEX idx_taxon_synonym_taxon_id ON taxon_synonym (taxon_id);
CREATE INDEX idx_taxon_synonym_name ON taxon_synonym (scientific_name);
