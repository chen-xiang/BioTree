/**
 * simple 视图下收集「下一档林奈七级可见子节点」（优先 simple_parent_id）。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 * Updated: 2026-08-31 使用 simple_parent_id；无冗余时回退 BFS
 * Updated: 2026-09-01 同层先按阶元深度再按学名
 * Updated: 2026-09-03 缺阶收入该档统一未分类目录
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

    /**
     * 当前层只返回「下一档」正式类群；缺更深层时在本页末尾带未分类节点。
     */
    public SimpleChildSlice collectSlice(Long parentId, Pageable pageable) {
        Context ctx = resolve(parentId);
        if (ctx.expectedChildRank() == null) {
            return SimpleChildSlice.empty(pageable);
        }
        Pageable sorted = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                TaxonSiblingSort.PAGE_SORT);
        List<Taxon> realPage;
        long realCount;
        boolean hasDeeper;
        if (hasIndexedSimpleChildren(ctx.anchorId())) {
            realCount = countRank(ctx.anchorId(), ctx.expectedChildRank());
            hasDeeper = existsRanks(ctx.anchorId(), TaxonRank.linnaeanDeeperThan(ctx.expectedChildRank()));
            realPage = findRankPage(ctx.anchorId(), ctx.expectedChildRank(), sorted).getContent();
        } else {
            List<Taxon> all = collectByBfs(ctx.anchorId());
            List<Taxon> real = all.stream()
                    .filter(t -> t.getRank() == ctx.expectedChildRank())
                    .sorted(TaxonSiblingSort.COMPARATOR)
                    .toList();
            hasDeeper = all.stream()
                    .anyMatch(t -> t.getRank().getRankOrder() > ctx.expectedChildRank().getRankOrder());
            realCount = real.size();
            int from = (int) Math.min((long) sorted.getPageNumber() * sorted.getPageSize(), real.size());
            int to = Math.min(from + sorted.getPageSize(), real.size());
            realPage = real.subList(from, to);
        }
        long extra = hasDeeper ? 1L : 0L;
        long total = realCount + extra;
        boolean showUnclassified = hasDeeper
                && (long) sorted.getPageNumber() * sorted.getPageSize() <= realCount
                && (sorted.getPageNumber() + 1L) * sorted.getPageSize() > realCount;
        Long unclassifiedId = showUnclassified
                ? SimpleUnclassifiedSupport.encode(ctx.anchorId(), ctx.expectedChildRank())
                : null;
        long unclassifiedHint = unclassifiedId == null ? 0L : countVisible(unclassifiedId);
        return new SimpleChildSlice(
                realPage,
                unclassifiedId,
                ctx.expectedChildRank(),
                ctx.anchorId(),
                unclassifiedHint,
                total,
                sorted.getPageNumber(),
                sorted.getPageSize());
    }

    public Page<Taxon> collectPage(Long parentId, Pageable pageable) {
        return findRankPage(
                resolve(parentId).anchorId(),
                resolve(parentId).expectedChildRank(),
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), TaxonSiblingSort.PAGE_SORT));
    }

    public long countVisible(Long parentId) {
        Context ctx = resolve(parentId);
        if (ctx.expectedChildRank() == null) {
            return 0L;
        }
        if (hasIndexedSimpleChildren(ctx.anchorId())) {
            long real = countRank(ctx.anchorId(), ctx.expectedChildRank());
            boolean deeper = existsRanks(ctx.anchorId(), TaxonRank.linnaeanDeeperThan(ctx.expectedChildRank()));
            return real + (deeper ? 1L : 0L);
        }
        List<Taxon> all = collectByBfs(ctx.anchorId());
        long real = all.stream().filter(t -> t.getRank() == ctx.expectedChildRank()).count();
        boolean deeper = all.stream()
                .anyMatch(t -> t.getRank().getRankOrder() > ctx.expectedChildRank().getRankOrder());
        return real + (deeper ? 1L : 0L);
    }

    /**
     * 兼容旧路径：全量收集下一档正式类群（不含未分类节点）。
     */
    public List<Taxon> collect(Long parentId) {
        Context ctx = resolve(parentId);
        if (ctx.expectedChildRank() == null) {
            return List.of();
        }
        if (hasIndexedSimpleChildren(ctx.anchorId())) {
            long n = countRank(ctx.anchorId(), ctx.expectedChildRank());
            if (n == 0) {
                return List.of();
            }
            return findRankPage(
                            ctx.anchorId(),
                            ctx.expectedChildRank(),
                            PageRequest.of(0, (int) Math.min(n, 100_000), TaxonSiblingSort.PAGE_SORT))
                    .getContent();
        }
        return collectByBfs(ctx.anchorId()).stream()
                .filter(t -> t.getRank() == ctx.expectedChildRank())
                .sorted(TaxonSiblingSort.COMPARATOR)
                .toList();
    }

    public boolean hasVisibleSimpleChildren(Long taxonId) {
        if (SimpleUnclassifiedSupport.isUnclassified(taxonId)) {
            return countVisible(taxonId) > 0;
        }
        if (hasIndexedSimpleChildren(taxonId)) {
            return true;
        }
        return taxonRepository
                .findById(taxonId)
                .filter(t -> t.getChildCount() > 0)
                .map(t -> !collectByBfs(taxonId).isEmpty())
                .orElse(false);
    }

    private Context resolve(Long parentId) {
        if (parentId == null) {
            return new Context(null, null, TaxonRank.KINGDOM);
        }
        var unclassified = SimpleUnclassifiedSupport.decode(parentId);
        if (unclassified.isPresent()) {
            SimpleUnclassifiedSupport.Ref ref = unclassified.get();
            return new Context(ref.anchorId(), ref.rank(), TaxonRank.nextLinnaean(ref.rank()).orElse(null));
        }
        return taxonRepository
                .findById(parentId)
                .map(t -> new Context(parentId, t.getRank(), TaxonRank.nextLinnaean(t.getRank()).orElse(null)))
                .orElseGet(() -> new Context(parentId, null, null));
    }

    private boolean hasIndexedSimpleChildren(Long anchorId) {
        if (anchorId == null) {
            return taxonRepository.existsBySimpleParentIsNullAndRankIn(TaxonRank.LINNAEAN_SEVEN);
        }
        return taxonRepository.existsBySimpleParentIdAndRankIn(anchorId, TaxonRank.LINNAEAN_SEVEN);
    }

    private long countRank(Long anchorId, TaxonRank rank) {
        if (rank == null) {
            return 0L;
        }
        if (anchorId == null) {
            return taxonRepository.countBySimpleParentIsNullAndRank(rank);
        }
        return taxonRepository.countBySimpleParentIdAndRank(anchorId, rank);
    }

    private boolean existsRanks(Long anchorId, List<TaxonRank> ranks) {
        if (ranks == null || ranks.isEmpty()) {
            return false;
        }
        if (anchorId == null) {
            return taxonRepository.existsBySimpleParentIsNullAndRankIn(ranks);
        }
        return taxonRepository.existsBySimpleParentIdAndRankIn(anchorId, ranks);
    }

    private Page<Taxon> findRankPage(Long anchorId, TaxonRank rank, Pageable pageable) {
        if (rank == null) {
            return Page.empty(pageable);
        }
        if (anchorId == null) {
            return taxonRepository.findBySimpleParentIsNullAndRank(rank, pageable);
        }
        return taxonRepository.findBySimpleParentIdAndRank(anchorId, rank, pageable);
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

    private record Context(Long anchorId, TaxonRank parentRank, TaxonRank expectedChildRank) {}

    public record SimpleChildSlice(
            List<Taxon> taxa,
            Long unclassifiedId,
            TaxonRank unclassifiedRank,
            Long unclassifiedAnchorId,
            long unclassifiedChildHint,
            long total,
            int page,
            int size) {

        static SimpleChildSlice empty(Pageable pageable) {
            return new SimpleChildSlice(
                    List.of(),
                    null,
                    null,
                    null,
                    0L,
                    0L,
                    pageable.getPageNumber(),
                    pageable.getPageSize());
        }

        public boolean unclassifiedOnPage() {
            return unclassifiedId != null;
        }
    }
}
