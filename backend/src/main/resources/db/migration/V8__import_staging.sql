-- CoL 导入暂存表：流式落库，避免全量节点常驻堆内存

CREATE TABLE import_col_edge (
    external_id        VARCHAR(64) NOT NULL PRIMARY KEY,
    parent_external_id VARCHAR(64) NULL
);

CREATE INDEX idx_import_col_edge_parent ON import_col_edge (parent_external_id);

CREATE TABLE import_col_taxon (
    external_id        VARCHAR(64) NOT NULL PRIMARY KEY,
    parent_external_id VARCHAR(64) NULL,
    taxon_rank         VARCHAR(32) NOT NULL,
    scientific_name    VARCHAR(255) NOT NULL,
    kingdom            VARCHAR(64) NULL
);

CREATE INDEX idx_import_col_taxon_rank ON import_col_taxon (taxon_rank);

CREATE TABLE import_col_synonym (
    synonym_external_id  VARCHAR(64) NOT NULL PRIMARY KEY,
    accepted_external_id VARCHAR(64) NOT NULL,
    scientific_name      VARCHAR(255) NOT NULL
);

CREATE INDEX idx_import_col_synonym_accepted ON import_col_synonym (accepted_external_id);
