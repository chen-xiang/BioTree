/**
 * 分类树展示视图：简易七级 / 完整阶元。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
package com.chenxiang.biotree.domain.taxon;

import java.util.Locale;

public enum TaxonView {
    SIMPLE,
    FULL;

    public static TaxonView fromParam(String raw) {
        if (raw == null || raw.isBlank()) {
            return SIMPLE;
        }
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "full" -> FULL;
            default -> SIMPLE;
        };
    }
}
