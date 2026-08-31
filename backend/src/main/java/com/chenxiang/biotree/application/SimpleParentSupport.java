/**
 * 计算「最近林奈七级祖先」simple_parent_id。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
package com.chenxiang.biotree.application;

import com.chenxiang.biotree.domain.taxon.Taxon;
import com.chenxiang.biotree.domain.taxon.TaxonRank;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;

public final class SimpleParentSupport {

    private SimpleParentSupport() {
    }

    /**
     * 从 materialized_path 解析祖先 id（不含自身）。
     */
    public static List<Long> ancestorIds(String materializedPath, Long selfId) {
        List<Long> ids = new ArrayList<>();
        if (!StringUtils.hasText(materializedPath)) {
            return ids;
        }
        for (String part : materializedPath.split("/")) {
            if (part.isBlank()) {
                continue;
            }
            try {
                long id = Long.parseLong(part);
                if (selfId == null || id != selfId) {
                    ids.add(id);
                }
            } catch (NumberFormatException ignored) {
                // skip
            }
        }
        return ids;
    }

    /**
     * 在祖先链（根→父）上取最近的林奈七级节点 id；无则 null。
     */
    public static Long nearestLinnaeanAncestorId(List<Long> ancestorIdsRootToParent, Map<Long, TaxonRank> ranks) {
        Long nearest = null;
        for (Long id : ancestorIdsRootToParent) {
            TaxonRank rank = ranks.get(id);
            if (rank != null && TaxonRank.LINNAEAN_SEVEN.contains(rank)) {
                nearest = id;
            }
        }
        return nearest;
    }

    public static Long nearestLinnaeanAncestorId(Taxon taxon, Map<Long, Taxon> byId) {
        if (taxon == null) {
            return null;
        }
        List<Long> ancestors = ancestorIds(taxon.getMaterializedPath(), taxon.getId());
        Long nearest = null;
        for (Long id : ancestors) {
            Taxon node = byId.get(id);
            if (node != null && TaxonRank.LINNAEAN_SEVEN.contains(node.getRank())) {
                nearest = id;
            }
        }
        return nearest;
    }

    public static boolean isLinnaean(TaxonRank rank) {
        return rank != null && TaxonRank.LINNAEAN_SEVEN.contains(rank);
    }
}
