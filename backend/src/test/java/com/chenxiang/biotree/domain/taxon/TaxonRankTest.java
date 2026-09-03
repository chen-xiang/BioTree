/**
 * 分类等级枚举单元测试。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 * Updated: 2026-08-31 适配完整阶元
 * Updated: 2026-09-03 林奈相邻阶元与缺档
 */
package com.chenxiang.biotree.domain.taxon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class TaxonRankTest {

    @Test
    void linnaeanSevenStillPresent() {
        assertEquals(7, TaxonRank.LINNAEAN_SEVEN.size());
        assertTrue(TaxonRank.KINGDOM.isLinnaeanSeven());
        assertTrue(TaxonRank.SPECIES.isLinnaeanSeven());
        assertTrue(!TaxonRank.SUBSPECIES.isLinnaeanSeven());
    }

    @Test
    void fromDwcaMapsIntermediateRanks() {
        assertEquals(TaxonRank.SUBGENUS, TaxonRank.fromDwca("subgenus").orElseThrow());
        assertEquals(TaxonRank.OTHER, TaxonRank.fromDwca("unknown_rank_xyz").orElseThrow());
    }

    @Test
    void nextAndBetweenLinnaeanRanks() {
        assertEquals(TaxonRank.KINGDOM, TaxonRank.nextLinnaean(null).orElseThrow());
        assertEquals(TaxonRank.PHYLUM, TaxonRank.nextLinnaean(TaxonRank.KINGDOM).orElseThrow());
        assertTrue(TaxonRank.nextLinnaean(TaxonRank.SPECIES).isEmpty());
        assertEquals(
                List.of(TaxonRank.PHYLUM, TaxonRank.CLASS, TaxonRank.ORDER, TaxonRank.FAMILY, TaxonRank.GENUS),
                TaxonRank.linnaeanBetweenExclusive(TaxonRank.KINGDOM, TaxonRank.SPECIES));
        assertEquals(
                List.of(TaxonRank.CLASS, TaxonRank.ORDER, TaxonRank.FAMILY, TaxonRank.GENUS, TaxonRank.SPECIES),
                TaxonRank.linnaeanDeeperThan(TaxonRank.PHYLUM));
    }
}
