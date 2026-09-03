/**
 * 分类等级枚举：含林奈七级与常见中间级；带可比深度 rankOrder。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 * Updated: 2026-08-31 扩展中间级与 rankOrder（完整阶元方案 R1）
 * Updated: 2026-09-03 林奈七级有序列表与相邻阶元查询
 */
package com.chenxiang.biotree.domain.taxon;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
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

    /** 林奈七级，从界到种。 */
    public static final List<TaxonRank> LINNAEAN_SEVEN_ORDER = List.of(
            KINGDOM, PHYLUM, CLASS, ORDER, FAMILY, GENUS, SPECIES);

    /**
     * 下一档林奈七级；{@code null} 视为根，下一档为界。种之后为空。
     */
    public static Optional<TaxonRank> nextLinnaean(TaxonRank rank) {
        int order = rank == null ? Integer.MIN_VALUE : rank.rankOrder;
        for (TaxonRank candidate : LINNAEAN_SEVEN_ORDER) {
            if (candidate.rankOrder > order) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    /**
     * 上一档林奈七级。
     */
    public static Optional<TaxonRank> previousLinnaean(TaxonRank rank) {
        if (rank == null) {
            return Optional.empty();
        }
        TaxonRank previous = null;
        for (TaxonRank candidate : LINNAEAN_SEVEN_ORDER) {
            if (candidate.rankOrder >= rank.rankOrder) {
                return Optional.ofNullable(previous);
            }
            previous = candidate;
        }
        return Optional.ofNullable(previous);
    }

    /**
     * 严格深于给定阶元的林奈七级（用于缺阶收纳）。
     */
    public static List<TaxonRank> linnaeanDeeperThan(TaxonRank rank) {
        if (rank == null) {
            return LINNAEAN_SEVEN_ORDER;
        }
        return LINNAEAN_SEVEN_ORDER.stream().filter(r -> r.rankOrder > rank.rankOrder).toList();
    }

    /**
     * 两个林奈阶元之间的缺档（不含两端）。
     */
    public static List<TaxonRank> linnaeanBetweenExclusive(TaxonRank from, TaxonRank to) {
        if (to == null) {
            return List.of();
        }
        int low = from == null ? Integer.MIN_VALUE : from.rankOrder;
        return LINNAEAN_SEVEN_ORDER.stream()
                .filter(r -> r.rankOrder > low && r.rankOrder < to.rankOrder)
                .toList();
    }

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
