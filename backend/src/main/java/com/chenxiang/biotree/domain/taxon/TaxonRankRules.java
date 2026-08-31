/**
 * 分类等级父子合法性规则。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
package com.chenxiang.biotree.domain.taxon;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public final class TaxonRankRules {

    private static final Map<TaxonRank, TaxonRank> PARENT_OF = new EnumMap<>(TaxonRank.class);

    static {
        PARENT_OF.put(TaxonRank.PHYLUM, TaxonRank.KINGDOM);
        PARENT_OF.put(TaxonRank.CLASS, TaxonRank.PHYLUM);
        PARENT_OF.put(TaxonRank.ORDER, TaxonRank.CLASS);
        PARENT_OF.put(TaxonRank.FAMILY, TaxonRank.ORDER);
        PARENT_OF.put(TaxonRank.GENUS, TaxonRank.FAMILY);
        PARENT_OF.put(TaxonRank.SPECIES, TaxonRank.GENUS);
    }

    private TaxonRankRules() {
    }

    /**
     * 校验父子等级是否合法：界无父；其余必须恰好高一级。
     */
    public static boolean isValidParent(TaxonRank childRank, TaxonRank parentRankOrNull) {
        if (childRank == TaxonRank.KINGDOM) {
            return parentRankOrNull == null;
        }
        TaxonRank expected = PARENT_OF.get(childRank);
        return expected != null && expected == parentRankOrNull;
    }

    public static Optional<TaxonRank> requiredParentRank(TaxonRank childRank) {
        return Optional.ofNullable(PARENT_OF.get(childRank));
    }
}
