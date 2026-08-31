/**
 * 分类等级枚举：含林奈七级与常见中间级；带可比深度 rankOrder。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 * Updated: 2026-08-31 扩展中间级与 rankOrder（完整阶元方案 R1）
 */
package com.chenxiang.biotree.domain.taxon;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public enum TaxonRank {
    KINGDOM(10, true),
    SUBKINGDOM(15, false),
    PHYLUM(20, true),
    SUBPHYLUM(25, false),
    CLASS(30, true),
    SUBCLASS(35, false),
    ORDER(40, true),
    SUBORDER(45, false),
    SUPERFAMILY(48, false),
    FAMILY(50, true),
    SUBFAMILY(55, false),
    TRIBE(58, false),
    GENUS(60, true),
    SUBGENUS(65, false),
    SPECIES(70, true),
    SUBSPECIES(75, false),
    VARIETY(80, false),
    FORM(85, false),
    /** 未识别但已入库的阶元 */
    OTHER(90, false);

    private final int rankOrder;
    private final boolean linnaeanSeven;

    TaxonRank(int rankOrder, boolean linnaeanSeven) {
        this.rankOrder = rankOrder;
        this.linnaeanSeven = linnaeanSeven;
    }

    public int getRankOrder() {
        return rankOrder;
    }

    public boolean isLinnaeanSeven() {
        return linnaeanSeven;
    }

    public static final Set<TaxonRank> LINNAEAN_SEVEN = Collections.unmodifiableSet(
            EnumSet.of(KINGDOM, PHYLUM, CLASS, ORDER, FAMILY, GENUS, SPECIES));

    /**
     * 将 DwC taxonRank 原文映射为规范枚举；无法识别时为 OTHER。
     */
    public static Optional<TaxonRank> fromDwca(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String key = raw.trim().toLowerCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
        return Optional.of(switch (key) {
            case "kingdom" -> KINGDOM;
            case "subkingdom" -> SUBKINGDOM;
            case "phylum", "division" -> PHYLUM;
            case "subphylum", "subdivision" -> SUBPHYLUM;
            case "class" -> CLASS;
            case "subclass" -> SUBCLASS;
            case "order" -> ORDER;
            case "suborder" -> SUBORDER;
            case "superfamily" -> SUPERFAMILY;
            case "family" -> FAMILY;
            case "subfamily" -> SUBFAMILY;
            case "tribe", "subtribe" -> TRIBE;
            case "genus" -> GENUS;
            case "subgenus" -> SUBGENUS;
            case "species" -> SPECIES;
            case "subspecies" -> SUBSPECIES;
            case "variety", "varietas" -> VARIETY;
            case "form", "forma" -> FORM;
            default -> OTHER;
        });
    }
}
