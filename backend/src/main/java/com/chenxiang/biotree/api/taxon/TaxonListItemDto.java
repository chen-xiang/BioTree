/**
 * 分类列表/子节点瘦 DTO。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
package com.chenxiang.biotree.api.taxon;

import com.chenxiang.biotree.domain.taxon.TaxonRank;

public record TaxonListItemDto(
        Long id,
        TaxonRank rank,
        String scientificName,
        String commonName,
        int childCount,
        boolean hasChildren) {
}
