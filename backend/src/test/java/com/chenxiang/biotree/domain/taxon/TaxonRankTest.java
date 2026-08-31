/**
 * 分类等级枚举单元测试。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 * Updated: 2026-08-31 适配完整阶元
 */
package com.chenxiang.biotree.domain.taxon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
