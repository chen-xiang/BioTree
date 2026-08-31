/**
 * 分类单元仓储。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 * Updated: 2026-08-31 增加根节点分页与学名/俗名搜索
 * Updated: 2026-08-31 前缀优先搜索、多 locale、路径子树查询
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

    List<Taxon> findByParentIdOrderByScientificNameAsc(Long parentId);

    List<Taxon> findByParentIdInOrderByScientificNameAsc(Collection<Long> parentIds);

    List<Taxon> findByParentIsNullOrderByScientificNameAsc();

    List<Taxon> findByIdIn(Collection<Long> ids);

    List<Taxon> findByMaterializedPathStartingWith(String pathPrefix);

    boolean existsByParentIdAndScientificNameIgnoreCase(Long parentId, String scientificName);

    boolean existsByParentIsNullAndScientificNameIgnoreCase(String scientificName);

    /**
     * 前缀匹配（可走 scientific_name / locale+common_name 索引前缀扫描）。
     */
    @Query(
            """
            SELECT DISTINCT t FROM Taxon t
            LEFT JOIN TaxonI18n i ON i.taxon = t AND i.locale IN :locales
            WHERE LOWER(t.scientificName) LIKE LOWER(CONCAT(:q, '%'))
               OR LOWER(COALESCE(i.commonName, '')) LIKE LOWER(CONCAT(:q, '%'))
            """)
    Page<Taxon> searchPrefix(
            @Param("q") String q, @Param("locales") Collection<String> locales, Pageable pageable);

    /**
     * 包含匹配：前缀无结果时的回退；避免短词全表模糊。
     */
    @Query(
            """
            SELECT DISTINCT t FROM Taxon t
            LEFT JOIN TaxonI18n i ON i.taxon = t AND i.locale IN :locales
            WHERE LOWER(t.scientificName) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(COALESCE(i.commonName, '')) LIKE LOWER(CONCAT('%', :q, '%'))
            """)
    Page<Taxon> searchContains(
            @Param("q") String q, @Param("locales") Collection<String> locales, Pageable pageable);
}
