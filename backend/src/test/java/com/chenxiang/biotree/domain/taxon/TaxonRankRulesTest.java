/**
 * 分类等级规则单元测试。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 * Updated: 2026-08-31 适配 rankOrder 规则
 */
package com.chenxiang.biotree.domain.taxon;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TaxonRankRulesTest {

    @Test
    void kingdomMustHaveNoParent() {
        assertTrue(TaxonRankRules.isValidParent(TaxonRank.KINGDOM, null));
        assertFalse(TaxonRankRules.isValidParent(TaxonRank.KINGDOM, TaxonRank.PHYLUM));
    }

    @Test
    void deeperRankMayHangUnderShallower() {
        assertTrue(TaxonRankRules.isValidParent(TaxonRank.SPECIES, TaxonRank.GENUS));
        assertTrue(TaxonRankRules.isValidParent(TaxonRank.SPECIES, TaxonRank.SUBGENUS));
        assertTrue(TaxonRankRules.isValidParent(TaxonRank.SUBSPECIES, TaxonRank.SPECIES));
        assertFalse(TaxonRankRules.isValidParent(TaxonRank.GENUS, TaxonRank.SPECIES));
    }
}
