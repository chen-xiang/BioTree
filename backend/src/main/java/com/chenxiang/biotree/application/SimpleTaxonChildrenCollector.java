/**
 * simple 视图下收集「下一档林奈七级可见子节点」。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
package com.chenxiang.biotree.application;

import com.chenxiang.biotree.domain.taxon.Taxon;
import com.chenxiang.biotree.domain.taxon.TaxonRank;
import com.chenxiang.biotree.infrastructure.persistence.TaxonRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class SimpleTaxonChildrenCollector {

    private static final int MAX_SCAN_NODES = 50_000;

    private final TaxonRepository taxonRepository;

    public SimpleTaxonChildrenCollector(TaxonRepository taxonRepository) {
        this.taxonRepository = taxonRepository;
    }

    /**
     * 广度优先跳过非七级节点，收集下一档可见七级后代（按学名排序后可分页）。
     */
    public List<Taxon> collect(Long parentId) {
        List<Long> frontier = new ArrayList<>();
        if (parentId == null) {
            return taxonRepository.findByParentIsNullOrderByScientificNameAsc().stream()
                    .filter(t -> TaxonRank.LINNAEAN_SEVEN.contains(t.getRank()))
                    .sorted(Comparator.comparing(Taxon::getScientificName, String.CASE_INSENSITIVE_ORDER))
                    .toList();
        }
        frontier.add(parentId);

        Map<Long, Taxon> found = new LinkedHashMap<>();
        Set<Long> visitedParents = new HashSet<>();
        int scanned = 0;

        while (!frontier.isEmpty() && scanned < MAX_SCAN_NODES) {
            List<Long> batch = frontier.stream().filter(visitedParents::add).toList();
            frontier = new ArrayList<>();
            if (batch.isEmpty()) {
                break;
            }
            List<Taxon> children = taxonRepository.findByParentIdInOrderByScientificNameAsc(batch);
            scanned += children.size();
            for (Taxon child : children) {
                if (TaxonRank.LINNAEAN_SEVEN.contains(child.getRank())) {
                    found.putIfAbsent(child.getId(), child);
                } else {
                    frontier.add(child.getId());
                }
            }
        }

        return found.values().stream()
                .sorted(Comparator.comparing(Taxon::getScientificName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public boolean hasVisibleSimpleChildren(Long taxonId) {
        List<Long> frontier = new ArrayList<>();
        frontier.add(taxonId);
        Set<Long> visitedParents = new HashSet<>();
        int scanned = 0;
        while (!frontier.isEmpty() && scanned < MAX_SCAN_NODES) {
            List<Long> batch = frontier.stream().filter(visitedParents::add).toList();
            frontier = new ArrayList<>();
            if (batch.isEmpty()) {
                break;
            }
            List<Taxon> children = taxonRepository.findByParentIdInOrderByScientificNameAsc(batch);
            scanned += children.size();
            for (Taxon child : children) {
                if (TaxonRank.LINNAEAN_SEVEN.contains(child.getRank())) {
                    return true;
                }
                frontier.add(child.getId());
            }
        }
        return false;
    }
}
