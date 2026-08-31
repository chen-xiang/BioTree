/**
 * 分类分布仓储。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
package com.chenxiang.biotree.infrastructure.persistence;

import com.chenxiang.biotree.domain.taxon.TaxonDistribution;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxonDistributionRepository extends JpaRepository<TaxonDistribution, Long> {

    List<TaxonDistribution> findByTaxonIdOrderByCountryCodeAscIdAsc(Long taxonId);

    void deleteByTaxonId(Long taxonId);
}
