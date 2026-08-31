/**
 * 分类详情 DTO（含当前语言内容、面包屑与异名）。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 * Updated: 2026-08-31 增加异名列表
 */
package com.chenxiang.biotree.api.taxon;

import com.chenxiang.biotree.domain.taxon.TaxonRank;
import java.util.List;

public record TaxonDetailDto(
        Long id,
        Long parentId,
        TaxonRank rank,
        String scientificName,
        String commonName,
        String summary,
        String description,
        String locale,
        int childCount,
        boolean accepted,
        List<TaxonBreadcrumbDto> breadcrumbs,
        List<TaxonMediaDto> media,
        List<TaxonSynonymDto> synonyms) {
}
