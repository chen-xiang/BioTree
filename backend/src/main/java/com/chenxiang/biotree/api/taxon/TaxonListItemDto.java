/**
 * 分类列表/子节点瘦 DTO。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 * Updated: 2026-08-31 childCount 随 view；增加 directChildCount
 */
package com.chenxiang.biotree.api.taxon;

import com.chenxiang.biotree.domain.taxon.TaxonRank;

public record TaxonListItemDto(
        Long id,
        TaxonRank rank,
        String scientificName,
        String commonName,
        int childCount,
        boolean hasChildren,
        String rankRaw,
        int directChildCount) {
}
