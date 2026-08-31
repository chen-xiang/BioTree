-- MySQL 全文索引（仅在 flyway locations 含 migration-mysql 时执行）

ALTER TABLE taxon ADD FULLTEXT INDEX ft_taxon_scientific_name (scientific_name);
ALTER TABLE taxon_i18n ADD FULLTEXT INDEX ft_taxon_i18n_common_name (common_name);
ALTER TABLE taxon_synonym ADD FULLTEXT INDEX ft_taxon_synonym_name (scientific_name);
