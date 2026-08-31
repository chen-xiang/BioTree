-- 完整阶元：rank_order / rank_raw / 命名人；taxon_rank 列加长

ALTER TABLE taxon ADD COLUMN rank_order INT NOT NULL DEFAULT 0;
ALTER TABLE taxon ADD COLUMN taxon_rank_raw VARCHAR(64) NULL;
ALTER TABLE taxon ADD COLUMN scientific_name_authorship VARCHAR(255) NULL;

UPDATE taxon SET rank_order = CASE taxon_rank
    WHEN 'KINGDOM' THEN 10
    WHEN 'PHYLUM' THEN 20
    WHEN 'CLASS' THEN 30
    WHEN 'ORDER' THEN 40
    WHEN 'FAMILY' THEN 50
    WHEN 'GENUS' THEN 60
    WHEN 'SPECIES' THEN 70
    ELSE 90
END;

UPDATE taxon SET taxon_rank_raw = LOWER(taxon_rank) WHERE taxon_rank_raw IS NULL;

CREATE INDEX idx_taxon_rank_order ON taxon (rank_order);
