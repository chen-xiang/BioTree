/**
 * 分类分布 DTO。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
package com.chenxiang.biotree.api.taxon;

public record TaxonDistributionDto(
        Long id,
        String countryCode,
        String locality,
        String establishmentMeans,
        String threatStatus,
        String sourceText) {
}
