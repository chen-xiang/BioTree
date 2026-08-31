/**
 * SimpleParentSupport 单元测试。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
package com.chenxiang.biotree.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.chenxiang.biotree.domain.taxon.TaxonRank;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SimpleParentSupportTest {

    @Test
    void picksNearestLinnaeanAncestor() {
        Long nearest = SimpleParentSupport.nearestLinnaeanAncestorId(
                List.of(1L, 2L, 3L),
                Map.of(
                        1L, TaxonRank.KINGDOM,
                        2L, TaxonRank.SUBPHYLUM,
                        3L, TaxonRank.FAMILY));
        assertEquals(3L, nearest);
    }

    @Test
    void returnsNullWhenNoLinnaeanAncestor() {
        assertNull(SimpleParentSupport.nearestLinnaeanAncestorId(
                List.of(2L), Map.of(2L, TaxonRank.SUBFAMILY)));
    }

    @Test
    void parsesAncestorIdsExcludingSelf() {
        assertEquals(List.of(1L, 5L), SimpleParentSupport.ancestorIds("/1/5/9/", 9L));
    }
}
