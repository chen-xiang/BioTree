/**
 * simple 视图下收集「下一档林奈七级可见子节点」（优先 simple_parent_id）。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 * Updated: 2026-08-31 使用 simple_parent_id；无冗余时回退 BFS
 * Updated: 2026-09-01 同层先按阶元深度再按学名
 */
package com.chenxiang.biotree.application;

import com.chenxiang.biotree.domain.taxon.Taxon;
import com.chenxiang.biotree.domain.taxon.TaxonRank;
import com.chenxiang.biotree.infrastructure.persistence.TaxonRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class SimpleTaxonChildrenCollector {

    private static final int MAX_SCAN_NODES = 50_000;

    private final TaxonRepository taxonRepository;

    public SimpleTaxonChildrenCollector(TaxonRepository taxonRepository) {
        this.taxonRepository = taxonRepository;
    }

    public Page<Taxon> collectPage(Long parentId, Pageable pageable) {
        Pageable sorted = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                TaxonSiblingSort.PAGE_SORT);
        if (parentId == null) {
            return taxonRepository.findBySimpleParentIsNullAndRankIn(TaxonRank.LINNAEAN_SEVEN, sorted);
        }
        return taxonRepository.findBySimpleParentIdAndRankIn(parentId, TaxonRank.LINNAEAN_SEVEN, sorted);
    }

    public long countVisible(Long parentId) {
        if (parentId == null) {
            return taxonRepository.countBySimpleParentIsNullAndRankIn(TaxonRank.LINNAEAN_SEVEN);
        }
        return taxonRepository.countBySimpleParentIdAndRankIn(parentId, TaxonRank.LINNAEAN_SEVEN);
    }

    /**
     * 兼容旧路径：全量收集；有 simple_parent 索引则用之，否则 BFS。
     */
    public List<Taxon> collect(Long parentId) {
        long indexed = countVisible(parentId);
        if (indexed > 0) {
            return collectPage(parentId, PageRequest.of(0, (int) Math.min(indexed, 100_000))).getContent();
        }
        return collectByBfs(parentId);
    }

    public boolean hasVisibleSimpleChildren(Long taxonId) {
        if (taxonRepository.existsBySimpleParentIdAndRankIn(taxonId, TaxonRank.LINNAEAN_SEVEN)) {
            return true;
        }
        return taxonRepository
                .findById(taxonId)
                .filter(t -> t.getChildCount() > 0)
                .map(t -> !collectByBfs(taxonId).isEmpty())
                .orElse(false);
    }

    private List<Taxon> collectByBfs(Long parentId) {
        List<Long> frontier = new ArrayList<>();
        if (parentId == null) {
            return taxonRepository.findByParentIsNullOrderByScientificNameAsc().stream()
                    .filter(t -> TaxonRank.LINNAEAN_SEVEN.contains(t.getRank()))
                    .sorted(TaxonSiblingSort.COMPARATOR)
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
                .sorted(TaxonSiblingSort.COMPARATOR)
                .toList();
    }
}
