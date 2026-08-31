/**
 * 分类多语言内容仓储。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
package com.chenxiang.biotree.infrastructure.persistence;

import com.chenxiang.biotree.domain.taxon.TaxonI18n;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxonI18nRepository extends JpaRepository<TaxonI18n, Long> {

    Optional<TaxonI18n> findByTaxonIdAndLocale(Long taxonId, String locale);
}
