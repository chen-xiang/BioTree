/**
 * 创建分类单元请求。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
package com.chenxiang.biotree.api.taxon;

import com.chenxiang.biotree.domain.taxon.TaxonRank;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTaxonRequest(
        Long parentId,
        @NotNull TaxonRank rank,
        @NotBlank @Size(max = 255) String scientificName,
        String locale,
        @Size(max = 255) String commonName,
        @Size(max = 1024) String summary,
        String description) {
}
