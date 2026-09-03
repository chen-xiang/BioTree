/**
 * 未分类节点 id 编码测试。
 *
 * Author: chen-xiang
 * Created: 2026-09-03
 */
package com.chenxiang.biotree.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.chenxiang.biotree.domain.taxon.TaxonRank;
import org.junit.jupiter.api.Test;

class SimpleUnclassifiedSupportTest {

    @Test
    void encodeDecodeRoundTrip() {
        long id = SimpleUnclassifiedSupport.encode(12L, TaxonRank.PHYLUM);
        assertTrue(id < 0);
        SimpleUnclassifiedSupport.Ref ref = SimpleUnclassifiedSupport.decode(id).orElseThrow();
        assertEquals(12L, ref.anchorId());
        assertEquals(TaxonRank.PHYLUM, ref.rank());
    }

    @Test
    void rootUnclassifiedKingdomHasNullAnchor() {
        long id = SimpleUnclassifiedSupport.encode(null, TaxonRank.KINGDOM);
        SimpleUnclassifiedSupport.Ref ref = SimpleUnclassifiedSupport.decode(id).orElseThrow();
        assertEquals(null, ref.anchorId());
        assertEquals(TaxonRank.KINGDOM, ref.rank());
    }

    @Test
    void parentIdWalksUnclassifiedChain() {
        SimpleUnclassifiedSupport.Ref classBucket = new SimpleUnclassifiedSupport.Ref(5L, TaxonRank.CLASS);
        assertEquals(
                SimpleUnclassifiedSupport.encode(5L, TaxonRank.PHYLUM),
                SimpleUnclassifiedSupport.parentId(classBucket, TaxonRank.KINGDOM));
        SimpleUnclassifiedSupport.Ref phylumBucket = new SimpleUnclassifiedSupport.Ref(5L, TaxonRank.PHYLUM);
        assertEquals(5L, SimpleUnclassifiedSupport.parentId(phylumBucket, TaxonRank.KINGDOM));
    }
}
