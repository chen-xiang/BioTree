/**
 * 分类媒体元数据仓储。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
package com.chenxiang.biotree.infrastructure.persistence;

import com.chenxiang.biotree.domain.taxon.TaxonMedia;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxonMediaRepository extends JpaRepository<TaxonMedia, Long> {

    List<TaxonMedia> findByTaxonIdOrderBySortOrderAscIdAsc(Long taxonId);
}
