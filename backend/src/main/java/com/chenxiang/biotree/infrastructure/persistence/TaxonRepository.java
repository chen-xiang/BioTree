/**
 * 分类单元仓储。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 * Updated: 2026-08-31 增加根节点分页与学名/俗名搜索
 */
package com.chenxiang.biotree.infrastructure.persistence;

import com.chenxiang.biotree.domain.taxon.Taxon;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaxonRepository extends JpaRepository<Taxon, Long> {

    Page<Taxon> findByParentId(Long parentId, Pageable pageable);

    Page<Taxon> findByParentIsNull(Pageable pageable);

    List<Taxon> findByParentIsNullOrderByScientificNameAsc();

    List<Taxon> findByIdIn(Collection<Long> ids);

    boolean existsByParentIdAndScientificNameIgnoreCase(Long parentId, String scientificName);

    boolean existsByParentIsNullAndScientificNameIgnoreCase(String scientificName);

    @Query(
            """
            SELECT DISTINCT t FROM Taxon t
            LEFT JOIN TaxonI18n i ON i.taxon = t AND i.locale = :locale
            WHERE LOWER(t.scientificName) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(COALESCE(i.commonName, '')) LIKE LOWER(CONCAT('%', :q, '%'))
            """)
    Page<Taxon> search(@Param("q") String q, @Param("locale") String locale, Pageable pageable);
}
