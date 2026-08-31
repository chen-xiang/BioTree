/**
 * 分类媒体展示 DTO。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
package com.chenxiang.biotree.api.taxon;

public record TaxonMediaDto(
        Long id,
        String url,
        String mimeType,
        Integer width,
        Integer height,
        int sortOrder,
        String locale,
        String caption,
        String license,
        String attribution) {
}
