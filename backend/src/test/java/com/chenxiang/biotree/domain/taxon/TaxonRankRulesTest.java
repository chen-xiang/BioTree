/**
 * 分类等级规则单元测试。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
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
    void speciesMustBelongToGenus() {
        assertTrue(TaxonRankRules.isValidParent(TaxonRank.SPECIES, TaxonRank.GENUS));
        assertFalse(TaxonRankRules.isValidParent(TaxonRank.SPECIES, TaxonRank.FAMILY));
    }
}
