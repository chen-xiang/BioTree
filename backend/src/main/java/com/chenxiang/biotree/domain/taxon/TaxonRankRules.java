/**
 * 分类等级父子合法性规则（基于 rankOrder 深度）。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 * Updated: 2026-08-31 改为 rankOrder 校验，支持中间级（R1）
 */
package com.chenxiang.biotree.domain.taxon;

import java.util.Optional;

public final class TaxonRankRules {

    private TaxonRankRules() {
    }

    /**
     * 校验父子等级是否合法：界无父；其余父级必须存在且深度严格更浅。
     */
    public static boolean isValidParent(TaxonRank childRank, TaxonRank parentRankOrNull) {
        if (childRank == TaxonRank.KINGDOM) {
            return parentRankOrNull == null;
        }
        if (parentRankOrNull == null) {
            return false;
        }
        return childRank.getRankOrder() > parentRankOrNull.getRankOrder();
    }

    /**
     * 七级视图下「理想」父级（兼容旧 API 提示）；中间级无单一答案。
     */
    public static Optional<TaxonRank> requiredParentRank(TaxonRank childRank) {
        return switch (childRank) {
            case PHYLUM -> Optional.of(TaxonRank.KINGDOM);
            case CLASS -> Optional.of(TaxonRank.PHYLUM);
            case ORDER -> Optional.of(TaxonRank.CLASS);
            case FAMILY -> Optional.of(TaxonRank.ORDER);
            case GENUS -> Optional.of(TaxonRank.FAMILY);
            case SPECIES -> Optional.of(TaxonRank.GENUS);
            default -> Optional.empty();
        };
    }
}
