/**
 * Catalogue of Life 学名清洗与等级/语言映射。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 * Updated: 2026-08-31 完整阶元映射与多语言俗名扩展（R1/R2/R6）
 * Updated: 2026-09-02 接受 provisionally accepted 为有效分类地位
 * Updated: 2026-09-03 同父学名唯一键按去重音折叠，对齐 MySQL ai_ci
 */
package com.chenxiang.biotree.infrastructure.importdata;

import com.chenxiang.biotree.domain.taxon.TaxonRank;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;
import org.springframework.util.StringUtils;

public final class ColNameUtils {

    private ColNameUtils() {
    }

    /**
     * @param legacySevenOnly true 时仅返回林奈七级（旧行为）
     */
    public static Optional<TaxonRank> mapRank(String raw, boolean legacySevenOnly) {
        Optional<TaxonRank> mapped = TaxonRank.fromDwca(raw);
        if (mapped.isEmpty()) {
            return Optional.empty();
        }
        TaxonRank rank = mapped.get();
        if (legacySevenOnly && !rank.isLinnaeanSeven()) {
            return Optional.empty();
        }
        if (rank == TaxonRank.OTHER && legacySevenOnly) {
            return Optional.empty();
        }
        return mapped;
    }

    public static Optional<TaxonRank> mapRank(String raw) {
        return mapRank(raw, true);
    }

    /**
     * 折叠学名以便与 MySQL {@code utf8mb4_0900_ai_ci} 的同父唯一约束对齐（大小写、重音不敏感）。
     */
    public static String foldForParentNameUnique(String scientificName) {
        if (!StringUtils.hasText(scientificName)) {
            return "";
        }
        String nfd = Normalizer.normalize(scientificName.trim(), Normalizer.Form.NFD);
        StringBuilder folded = new StringBuilder(nfd.length());
        for (int i = 0; i < nfd.length(); i++) {
            char ch = nfd.charAt(i);
            if (Character.getType(ch) != Character.NON_SPACING_MARK) {
                folded.append(ch);
            }
        }
        return folded.toString().toLowerCase(Locale.ROOT);
    }

    /** accepted / provisionally accepted 视为接受名。 */
    public static boolean isAcceptedTaxonomicStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return false;
        }
        String normalized = status.trim().toLowerCase(Locale.ROOT);
        return "accepted".equals(normalized) || "provisionally accepted".equals(normalized);
    }

    /**
     * 生成规范学名：种用属名+种加词；种下阶元附加种下加词；其余尽量去掉命名人信息。
     */
    public static String canonicalName(
            TaxonRank rank, String scientificName, String genericName, String specificEpithet) {
        return canonicalName(rank, scientificName, genericName, specificEpithet, null);
    }

    public static String canonicalName(
            TaxonRank rank,
            String scientificName,
            String genericName,
            String specificEpithet,
            String infraspecificEpithet) {
        if ((rank == TaxonRank.SPECIES
                        || rank == TaxonRank.SUBSPECIES
                        || rank == TaxonRank.VARIETY
                        || rank == TaxonRank.FORM)
                && StringUtils.hasText(genericName)
                && StringUtils.hasText(specificEpithet)) {
            String base = genericName.trim() + " " + specificEpithet.trim();
            if (rank != TaxonRank.SPECIES && StringUtils.hasText(infraspecificEpithet)) {
                return base + " " + infraspecificEpithet.trim();
            }
            return base;
        }
        if ((rank == TaxonRank.GENUS || rank == TaxonRank.SUBGENUS) && StringUtils.hasText(genericName)) {
            return genericName.trim();
        }
        return stripAuthorship(scientificName);
    }

    public static String stripAuthorship(String scientificName) {
        if (!StringUtils.hasText(scientificName)) {
            return "";
        }
        String name = scientificName.trim();
        String[] parts = name.split("\\s+");
        if (parts.length >= 2
                && Character.isUpperCase(parts[0].charAt(0))
                && Character.isLowerCase(parts[1].charAt(0))) {
            return parts[0] + " " + parts[1];
        }
        return parts[0];
    }

    public static Optional<String> mapLocale(String language) {
        if (!StringUtils.hasText(language)) {
            return Optional.empty();
        }
        String lang = language.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        return switch (lang) {
            case "eng", "en", "en-us", "en-gb" -> Optional.of("en");
            case "zho", "zh", "chi", "zh-cn", "zh-hans" -> Optional.of("zh-CN");
            case "zh-tw", "zh-hant" -> Optional.of("zh-TW");
            case "jpn", "ja", "jap" -> Optional.of("ja");
            case "spa", "es" -> Optional.of("es");
            case "fra", "fre", "fr" -> Optional.of("fr");
            case "deu", "ger", "de" -> Optional.of("de");
            case "rus", "ru" -> Optional.of("ru");
            case "por", "pt", "pt-br" -> Optional.of("pt");
            case "kor", "ko" -> Optional.of("ko");
            default -> Optional.empty();
        };
    }
}
