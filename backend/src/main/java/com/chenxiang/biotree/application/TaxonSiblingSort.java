/**
 * 同层子节点排序：先按阶元深度（门、纲在前），再按学名。
 *
 * Author: chen-xiang
 * Created: 2026-09-01
 */
package com.chenxiang.biotree.application;

import com.chenxiang.biotree.domain.taxon.Taxon;
import java.util.Comparator;
import org.springframework.data.domain.Sort;

public final class TaxonSiblingSort {

    public static final Sort PAGE_SORT = Sort.by(Sort.Direction.ASC, "rankOrder")
            .and(Sort.by(Sort.Direction.ASC, "scientificName"));

    public static final Comparator<Taxon> COMPARATOR = Comparator.comparingInt(Taxon::getRankOrder)
            .thenComparing(Taxon::getScientificName, String.CASE_INSENSITIVE_ORDER);

    private TaxonSiblingSort() {
    }
}
