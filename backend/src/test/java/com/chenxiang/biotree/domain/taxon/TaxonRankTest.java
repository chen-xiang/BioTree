/**
 * 分类等级父子合法性校验（骨架单元测试）。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
package com.chenxiang.biotree.domain.taxon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TaxonRankTest {

    @Test
    void ranksShouldCoverSevenLevels() {
        assertEquals(7, TaxonRank.values().length);
        assertTrue(TaxonRank.valueOf("KINGDOM") == TaxonRank.KINGDOM);
        assertTrue(TaxonRank.valueOf("SPECIES") == TaxonRank.SPECIES);
    }
}
