/**
 * 分类详情 DTO（含当前语言内容、面包屑、异名、分布与多语言俗名）。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 * Updated: 2026-08-31 增加异名列表
 * Updated: 2026-08-31 配图首屏预览 + mediaTotal
 * Updated: 2026-08-31 DwC 命名学元数据、分布、多语言俗名
 * Updated: 2026-09-03 simple 视图未分类占位节点
 */
package com.chenxiang.biotree.api.taxon;

import com.chenxiang.biotree.domain.taxon.TaxonRank;
import java.util.List;

public record TaxonDetailDto(
        Long id,
        Long parentId,
        TaxonRank rank,
        String scientificName,
        String scientificNameAuthorship,
        String scientificNameVerbatim,
        String namePublishedIn,
        String nameAccordingTo,
        String nomenclaturalCode,
        String nomenclaturalStatus,
        String commonName,
        String summary,
        String description,
        String locale,
        int childCount,
        boolean accepted,
        List<TaxonBreadcrumbDto> breadcrumbs,
        List<TaxonMediaDto> media,
        long mediaTotal,
        List<TaxonSynonymDto> synonyms,
        String rankRaw,
        List<TaxonVernacularDto> vernaculars,
        List<TaxonDistributionDto> distributions,
        int directChildCount,
        Long nearestSimpleAncestorId,
        boolean placeholder) {
}
