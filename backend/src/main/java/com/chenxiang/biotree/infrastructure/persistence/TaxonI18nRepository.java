/**
 * 分类多语言内容仓储。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 * Updated: 2026-08-31 支持多 locale 批量查询以做回退
 */
package com.chenxiang.biotree.infrastructure.persistence;

import com.chenxiang.biotree.domain.taxon.TaxonI18n;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxonI18nRepository extends JpaRepository<TaxonI18n, Long> {

    Optional<TaxonI18n> findByTaxonIdAndLocale(Long taxonId, String locale);

    List<TaxonI18n> findByTaxonId(Long taxonId);

    List<TaxonI18n> findByTaxonIdInAndLocale(Collection<Long> taxonIds, String locale);

    List<TaxonI18n> findByTaxonIdInAndLocaleIn(Collection<Long> taxonIds, Collection<String> locales);

    List<TaxonI18n> findByTaxonIdAndLocaleIn(Long taxonId, Collection<String> locales);

    void deleteByTaxonId(Long taxonId);
}
