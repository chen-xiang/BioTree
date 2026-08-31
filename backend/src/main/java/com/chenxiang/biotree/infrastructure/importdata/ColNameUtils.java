/**
 * Catalogue of Life 学名清洗与等级映射。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
package com.chenxiang.biotree.infrastructure.importdata;

import com.chenxiang.biotree.domain.taxon.TaxonRank;
import java.util.Locale;
import java.util.Optional;
import org.springframework.util.StringUtils;

public final class ColNameUtils {

    private ColNameUtils() {
    }

    public static Optional<TaxonRank> mapRank(String raw) {
        if (!StringUtils.hasText(raw)) {
            return Optional.empty();
        }
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "kingdom" -> Optional.of(TaxonRank.KINGDOM);
            case "phylum" -> Optional.of(TaxonRank.PHYLUM);
            case "class" -> Optional.of(TaxonRank.CLASS);
            case "order" -> Optional.of(TaxonRank.ORDER);
            case "family" -> Optional.of(TaxonRank.FAMILY);
            case "genus" -> Optional.of(TaxonRank.GENUS);
            case "species" -> Optional.of(TaxonRank.SPECIES);
            default -> Optional.empty();
        };
    }

    /**
     * 生成规范学名：种用属名+种加词；其余尽量去掉命名人信息。
     */
    public static String canonicalName(
            TaxonRank rank, String scientificName, String genericName, String specificEpithet) {
        if (rank == TaxonRank.SPECIES
                && StringUtils.hasText(genericName)
                && StringUtils.hasText(specificEpithet)) {
            return genericName.trim() + " " + specificEpithet.trim();
        }
        if (rank == TaxonRank.GENUS && StringUtils.hasText(genericName)) {
            return genericName.trim();
        }
        return stripAuthorship(scientificName);
    }

    public static String stripAuthorship(String scientificName) {
        if (!StringUtils.hasText(scientificName)) {
            return "";
        }
        String name = scientificName.trim();
        // 形如 "Homo sapiens Linnaeus, 1758" → 取前两个词（种）或第一个词（高等级）
        String[] parts = name.split("\\s+");
        if (parts.length >= 2 && Character.isUpperCase(parts[0].charAt(0)) && Character.isLowerCase(parts[1].charAt(0))) {
            return parts[0] + " " + parts[1];
        }
        return parts[0];
    }

    public static Optional<String> mapLocale(String language) {
        if (!StringUtils.hasText(language)) {
            return Optional.empty();
        }
        String lang = language.trim().toLowerCase(Locale.ROOT);
        return switch (lang) {
            case "eng", "en" -> Optional.of("en");
            case "zho", "zh", "chi", "zh-cn", "zh_cn" -> Optional.of("zh-CN");
            default -> Optional.empty();
        };
    }
}
