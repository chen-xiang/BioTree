/**
 * 同层子节点阶元优先排序单测。
 *
 * Author: chen-xiang
 * Created: 2026-09-01
 */
package com.chenxiang.biotree.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.chenxiang.biotree.domain.taxon.Taxon;
import com.chenxiang.biotree.domain.taxon.TaxonRank;
import java.util.List;
import org.junit.jupiter.api.Test;

class TaxonSiblingSortTest {

    @Test
    void placesExpectedNextRanksBeforeSkippedLowerRanks() {
        Taxon genus = taxon("Aequitriradites", TaxonRank.GENUS);
        Taxon phylum = taxon("Anthocerotophyta", TaxonRank.PHYLUM);
        Taxon family = taxon("Appianaceae", TaxonRank.FAMILY);
        Taxon laterPhylum = taxon("Bryophyta", TaxonRank.PHYLUM);

        List<String> names = List.of(genus, laterPhylum, family, phylum).stream()
                .sorted(TaxonSiblingSort.COMPARATOR)
                .map(Taxon::getScientificName)
                .toList();

        assertEquals(List.of("Anthocerotophyta", "Bryophyta", "Appianaceae", "Aequitriradites"), names);
    }

    private static Taxon taxon(String name, TaxonRank rank) {
        Taxon t = new Taxon();
        t.setScientificName(name);
        t.setRank(rank);
        return t;
    }
}
