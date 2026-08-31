-- R10：冗余最近七级祖先，加速 view=simple 的 children 查询

ALTER TABLE taxon ADD COLUMN simple_parent_id BIGINT NULL;
CREATE INDEX idx_taxon_simple_parent_id ON taxon (simple_parent_id);
ALTER TABLE taxon ADD CONSTRAINT fk_taxon_simple_parent
    FOREIGN KEY (simple_parent_id) REFERENCES taxon (id);
