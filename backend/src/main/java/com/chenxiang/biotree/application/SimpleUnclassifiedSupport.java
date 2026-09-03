/**
 * simple 视图缺阶「未分类」节点：负 id 编码，不入库。
 *
 * Author: chen-xiang
 * Created: 2026-09-03
 */
package com.chenxiang.biotree.application;

import com.chenxiang.biotree.domain.taxon.TaxonRank;
import java.util.Optional;

public final class SimpleUnclassifiedSupport {

    public static final String SCIENTIFIC_NAME = "incertae sedis";

    private static final int RANK_SLOT = 8;

    private SimpleUnclassifiedSupport() {}

    /**
     * 编码未分类节点 id。{@code anchorId} 为最近真实林奈祖先，根下缺界时为 null。
     */
    public static long encode(Long anchorId, TaxonRank bucketRank) {
        int index = linnaeanIndex(bucketRank);
        long anchor = anchorId == null ? 0L : anchorId;
        if (anchor < 0) {
            throw new IllegalArgumentException("anchorId must be a real taxon id");
        }
        return -(anchor * RANK_SLOT + index);
    }

    public static boolean isUnclassified(Long id) {
        return id != null && id < 0;
    }

    public static Optional<Ref> decode(Long id) {
        if (!isUnclassified(id)) {
            return Optional.empty();
        }
        long raw = -id;
        int index = (int) (raw % RANK_SLOT);
        long anchor = raw / RANK_SLOT;
        return linnaeanByIndex(index).map(rank -> new Ref(anchor == 0L ? null : anchor, rank));
    }

    public static String commonName(String locale) {
        return LocaleSupport.isChinese(locale) ? "未分类" : "Unclassified";
    }

    public static String summary(String locale) {
        if (LocaleSupport.isChinese(locale)) {
            return "缺少该阶元的类群收在此目录，避免与上一阶的正式类群平铺。";
        }
        return "Taxa missing this rank are grouped here so they are not listed beside the previous rank.";
    }

    public static Long parentId(Ref ref, TaxonRank anchorRank) {
        Optional<TaxonRank> previous = TaxonRank.previousLinnaean(ref.rank());
        if (previous.isEmpty()) {
            return null;
        }
        TaxonRank prev = previous.get();
        if (anchorRank == prev) {
            return ref.anchorId();
        }
        return encode(ref.anchorId(), prev);
    }

    private static int linnaeanIndex(TaxonRank rank) {
        int index = TaxonRank.LINNAEAN_SEVEN_ORDER.indexOf(rank);
        if (index < 0) {
            throw new IllegalArgumentException("bucketRank must be Linnaean");
        }
        return index + 1;
    }

    private static Optional<TaxonRank> linnaeanByIndex(int index) {
        if (index < 1 || index > TaxonRank.LINNAEAN_SEVEN_ORDER.size()) {
            return Optional.empty();
        }
        return Optional.of(TaxonRank.LINNAEAN_SEVEN_ORDER.get(index - 1));
    }

    public record Ref(Long anchorId, TaxonRank rank) {}
}
