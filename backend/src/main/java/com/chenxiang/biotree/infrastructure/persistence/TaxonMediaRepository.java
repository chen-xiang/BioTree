/**
 * 分类媒体元数据仓储。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
package com.chenxiang.biotree.infrastructure.persistence;

import com.chenxiang.biotree.domain.taxon.TaxonMedia;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxonMediaRepository extends JpaRepository<TaxonMedia, Long> {

    List<TaxonMedia> findByTaxonIdOrderBySortOrderAscIdAsc(Long taxonId);

    Page<TaxonMedia> findByTaxonIdOrderBySortOrderAscIdAsc(Long taxonId, Pageable pageable);

    long countByTaxonId(Long taxonId);
}
