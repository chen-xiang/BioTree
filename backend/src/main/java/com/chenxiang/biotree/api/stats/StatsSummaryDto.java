/**
 * 统计摘要 DTO。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
package com.chenxiang.biotree.api.stats;

import java.util.Map;

public record StatsSummaryDto(long totalTaxa, Map<String, Long> byRank, Map<String, Long> byKingdom) {
}
