/**
 * 分类单元仓储。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
package com.chenxiang.biotree.infrastructure.persistence;

import com.chenxiang.biotree.domain.taxon.Taxon;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxonRepository extends JpaRepository<Taxon, Long> {

    Page<Taxon> findByParentId(Long parentId, Pageable pageable);

    List<Taxon> findByParentIsNullOrderByScientificNameAsc();
}
