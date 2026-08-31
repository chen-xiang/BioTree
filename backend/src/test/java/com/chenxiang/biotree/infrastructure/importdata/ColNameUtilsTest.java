/**
 * 学名清洗工具单元测试。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 * Updated: 2026-08-31 完整阶元与多语言
 */
package com.chenxiang.biotree.infrastructure.importdata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.chenxiang.biotree.domain.taxon.TaxonRank;
import org.junit.jupiter.api.Test;

class ColNameUtilsTest {

    @Test
    void mapRankAndLocale() {
        assertEquals(TaxonRank.SPECIES, ColNameUtils.mapRank("species").orElseThrow());
        assertTrue(ColNameUtils.mapRank("subspecies", true).isEmpty());
        assertEquals(TaxonRank.SUBSPECIES, ColNameUtils.mapRank("subspecies", false).orElseThrow());
        assertEquals("en", ColNameUtils.mapLocale("eng").orElseThrow());
        assertEquals("zh-CN", ColNameUtils.mapLocale("zho").orElseThrow());
        assertEquals("ja", ColNameUtils.mapLocale("jpn").orElseThrow());
    }

    @Test
    void canonicalSpeciesNamePrefersEpithets() {
        String name = ColNameUtils.canonicalName(
                TaxonRank.SPECIES, "Homo sapiens Linnaeus, 1758", "Homo", "sapiens");
        assertEquals("Homo sapiens", name);
    }

    @Test
    void stripAuthorshipForHigherRanks() {
        assertEquals("Chordata", ColNameUtils.stripAuthorship("Chordata Bateson, 1885"));
    }
}
