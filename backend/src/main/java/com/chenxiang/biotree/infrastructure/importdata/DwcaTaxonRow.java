/**
 * DwC 分类行的内存表示。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
package com.chenxiang.biotree.infrastructure.importdata;

import com.chenxiang.biotree.domain.taxon.TaxonRank;

public record DwcaTaxonRow(
        String taxonId,
        String parentId,
        TaxonRank rank,
        String scientificName,
        String kingdom) {
}
