/**
 * simple 视图可见子节点收集器单元测试。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 * Updated: 2026-09-01 补充同层阶元优先排序
 */
package com.chenxiang.biotree.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chenxiang.biotree.domain.taxon.Taxon;
import com.chenxiang.biotree.domain.taxon.TaxonRank;
import com.chenxiang.biotree.infrastructure.persistence.TaxonRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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

        when(taxonRepository.countBySimpleParentIdAndRankIn(1L, TaxonRank.LINNAEAN_SEVEN))
                .thenReturn(0L);
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
        family.setChildCount(1);
        Taxon subfamily = taxon(2L, "Felinae", TaxonRank.SUBFAMILY, family);
        Taxon genus = taxon(3L, "Felis", TaxonRank.GENUS, subfamily);

        when(taxonRepository.existsBySimpleParentIdAndRankIn(1L, TaxonRank.LINNAEAN_SEVEN)).thenReturn(false);
        when(taxonRepository.findById(1L)).thenReturn(java.util.Optional.of(family));
        when(taxonRepository.findByParentIdInOrderByScientificNameAsc(org.mockito.ArgumentMatchers.<Long>anyList()))
                .thenReturn(List.of(subfamily), List.of(genus));

        assertTrue(collector.hasVisibleSimpleChildren(1L));
    }

    @Test
    void collectPageOrdersByRankThenName() {
        when(taxonRepository.findBySimpleParentIdAndRankIn(
                        eq(1L), eq(TaxonRank.LINNAEAN_SEVEN), org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(Page.empty());

        collector.collectPage(1L, PageRequest.of(0, 20));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(taxonRepository)
                .findBySimpleParentIdAndRankIn(eq(1L), eq(TaxonRank.LINNAEAN_SEVEN), captor.capture());
        assertEquals(TaxonSiblingSort.PAGE_SORT, captor.getValue().getSort());
    }

    @Test
    void bfsOrdersHigherRanksBeforeSkippedLowerRanks() {
        Taxon kingdom = taxon(1L, "Plantae", TaxonRank.KINGDOM, null);
        Taxon genus = taxon(2L, "Aequitriradites", TaxonRank.GENUS, kingdom);
        Taxon phylum = taxon(3L, "Anthocerotophyta", TaxonRank.PHYLUM, kingdom);
        Taxon family = taxon(4L, "Appianaceae", TaxonRank.FAMILY, kingdom);

        when(taxonRepository.countBySimpleParentIdAndRankIn(1L, TaxonRank.LINNAEAN_SEVEN))
                .thenReturn(0L);
        when(taxonRepository.findByParentIdInOrderByScientificNameAsc(List.of(1L)))
                .thenReturn(List.of(genus, family, phylum));

        List<Taxon> visible = collector.collect(1L);
        assertEquals(
                List.of("Anthocerotophyta", "Appianaceae", "Aequitriradites"),
                visible.stream().map(Taxon::getScientificName).toList());
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
