-- BioTree 初始 Schema：分类树、多语言、媒体、管理员

CREATE TABLE admin_user (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    username      VARCHAR(64)  NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    enabled       TINYINT(1)   NOT NULL DEFAULT 1,
    created_at    TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT uk_admin_user_username UNIQUE (username)
);

CREATE TABLE taxon (
    id                 BIGINT PRIMARY KEY AUTO_INCREMENT,
    parent_id          BIGINT       NULL,
    taxon_rank         VARCHAR(32)  NOT NULL,
    scientific_name    VARCHAR(255) NOT NULL,
    materialized_path  VARCHAR(768) NOT NULL,
    child_count        INT          NOT NULL DEFAULT 0,
    is_accepted        TINYINT(1)   NOT NULL DEFAULT 1,
    created_at         TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at         TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by         VARCHAR(64)  NULL,
    CONSTRAINT fk_taxon_parent FOREIGN KEY (parent_id) REFERENCES taxon (id),
    CONSTRAINT uk_taxon_parent_name UNIQUE (parent_id, scientific_name)
);

CREATE INDEX idx_taxon_parent_id ON taxon (parent_id);
CREATE INDEX idx_taxon_scientific_name ON taxon (scientific_name);
CREATE INDEX idx_taxon_path ON taxon (materialized_path);

CREATE TABLE taxon_i18n (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    taxon_id     BIGINT       NOT NULL,
    locale       VARCHAR(16)  NOT NULL,
    common_name  VARCHAR(255) NULL,
    summary      VARCHAR(1024) NULL,
    description  MEDIUMTEXT   NULL,
    CONSTRAINT fk_taxon_i18n_taxon FOREIGN KEY (taxon_id) REFERENCES taxon (id),
    CONSTRAINT uk_taxon_locale UNIQUE (taxon_id, locale)
);

CREATE INDEX idx_taxon_i18n_locale_name ON taxon_i18n (locale, common_name);

CREATE TABLE taxon_media (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    taxon_id     BIGINT       NOT NULL,
    storage_key  VARCHAR(512) NOT NULL,
    mime_type    VARCHAR(128) NULL,
    width        INT          NULL,
    height       INT          NULL,
    size_bytes   BIGINT       NULL,
    sort_order   INT          NOT NULL DEFAULT 0,
    locale       VARCHAR(16)  NULL,
    caption      VARCHAR(512) NULL,
    license      VARCHAR(255) NULL,
    attribution  VARCHAR(255) NULL,
    created_at   TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_taxon_media_taxon FOREIGN KEY (taxon_id) REFERENCES taxon (id)
);

CREATE INDEX idx_taxon_media_taxon_id ON taxon_media (taxon_id);
