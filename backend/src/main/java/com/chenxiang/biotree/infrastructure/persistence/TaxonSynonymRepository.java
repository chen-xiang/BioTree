/**
 * 异名仓储。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
package com.chenxiang.biotree.infrastructure.persistence;

import com.chenxiang.biotree.domain.taxon.TaxonSynonym;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxonSynonymRepository extends JpaRepository<TaxonSynonym, Long> {

    List<TaxonSynonym> findByTaxonIdOrderByScientificNameAsc(Long taxonId);
}
