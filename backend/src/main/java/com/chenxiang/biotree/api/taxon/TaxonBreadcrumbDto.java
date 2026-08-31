/**
 * 面包屑节点 DTO。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
package com.chenxiang.biotree.api.taxon;

import com.chenxiang.biotree.domain.taxon.TaxonRank;

public record TaxonBreadcrumbDto(Long id, TaxonRank rank, String scientificName, String commonName) {
}
