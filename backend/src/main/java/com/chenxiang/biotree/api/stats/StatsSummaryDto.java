/**
 * 统计摘要 DTO。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 * Updated: 2026-08-31 增加数据集引用
 */
package com.chenxiang.biotree.api.stats;

import java.time.Instant;
import java.util.Map;

public record StatsSummaryDto(
        long totalTaxa,
        Map<String, Long> byRank,
        Map<String, Long> byKingdom,
        DatasetCitationDto dataset) {

    public record DatasetCitationDto(String title, String version, String sourceUrl, Instant importedAt) {
    }
}
