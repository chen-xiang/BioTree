/**
 * 更新分类单元请求。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
package com.chenxiang.biotree.api.taxon;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateTaxonRequest(
        @NotBlank @Size(max = 255) String scientificName,
        Boolean accepted,
        String locale,
        @Size(max = 255) String commonName,
        @Size(max = 1024) String summary,
        String description) {
}
