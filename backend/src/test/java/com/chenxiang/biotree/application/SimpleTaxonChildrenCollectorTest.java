/**
 * simple 视图可见子节点收集器单元测试。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
package com.chenxiang.biotree.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.chenxiang.biotree.domain.taxon.Taxon;
import com.chenxiang.biotree.domain.taxon.TaxonRank;
import com.chenxiang.biotree.infrastructure.persistence.TaxonRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SimpleTaxonChildrenCollectorTest {

    @Mock
    private TaxonRepository taxonRepository;

    private SimpleTaxonChildrenCollector collector;

    @BeforeEach
    void setUp() {
        collector = new SimpleTaxonChildrenCollector(taxonRepository);
    }

    @Test
    void skipsIntermediateRanksAndReturnsNextLinnaeanSeven() {
        Taxon family = taxon(1L, "Felidae", TaxonRank.FAMILY, null);
        Taxon subfamily = taxon(2L, "Felinae", TaxonRank.SUBFAMILY, family);
        Taxon genus = taxon(3L, "Felis", TaxonRank.GENUS, subfamily);

        when(taxonRepository.findByParentIdInOrderByScientificNameAsc(List.of(1L)))
                .thenReturn(List.of(subfamily));
        when(taxonRepository.findByParentIdInOrderByScientificNameAsc(List.of(2L)))
                .thenReturn(List.of(genus));

        List<Taxon> visible = collector.collect(1L);
        assertEquals(1, visible.size());
        assertEquals("Felis", visible.getFirst().getScientificName());
        assertEquals(TaxonRank.GENUS, visible.getFirst().getRank());
    }

    @Test
    void hasVisibleSimpleChildrenTrueWhenGenusUnderSubfamily() {
        Taxon family = taxon(1L, "Felidae", TaxonRank.FAMILY, null);
        Taxon subfamily = taxon(2L, "Felinae", TaxonRank.SUBFAMILY, family);
        Taxon genus = taxon(3L, "Felis", TaxonRank.GENUS, subfamily);

        when(taxonRepository.findByParentIdInOrderByScientificNameAsc(org.mockito.ArgumentMatchers.<Long>anyList()))
                .thenReturn(List.of(subfamily), List.of(genus));

        assertTrue(collector.hasVisibleSimpleChildren(1L));
    }

    private static Taxon taxon(Long id, String name, TaxonRank rank, Taxon parent) {
        Taxon t = new Taxon();
        t.setId(id);
        t.setScientificName(name);
        t.setRank(rank);
        t.setParent(parent);
        return t;
    }
}
