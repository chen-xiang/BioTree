/**
 * 分类库统计（基于冗余字段与分组查询，禁止子孙递归 COUNT）。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 * Updated: 2026-08-31 返回导入数据集引用
 */
package com.chenxiang.biotree.application;

import com.chenxiang.biotree.api.stats.StatsSummaryDto;
import com.chenxiang.biotree.domain.taxon.TaxonRank;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StatsService {

    private final JdbcTemplate jdbcTemplate;

    public StatsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public StatsSummaryDto summary() {
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM taxon", Long.class);
        Map<String, Long> byRank = new LinkedHashMap<>();
        for (TaxonRank rank : TaxonRank.values()) {
            byRank.put(rank.name(), 0L);
        }
        jdbcTemplate.query(
                "SELECT taxon_rank, COUNT(*) AS cnt FROM taxon GROUP BY taxon_rank",
                (org.springframework.jdbc.core.RowCallbackHandler) rs ->
                        byRank.put(rs.getString(1), rs.getLong(2)));

        Map<String, Long> byKingdom = new LinkedHashMap<>();
        jdbcTemplate.query(
                """
                SELECT scientific_name, child_count FROM taxon
                WHERE taxon_rank = 'KINGDOM'
                ORDER BY scientific_name
                """,
                (org.springframework.jdbc.core.RowCallbackHandler) rs ->
                        byKingdom.put(rs.getString(1), rs.getLong(2)));

        StatsSummaryDto.DatasetCitationDto dataset = jdbcTemplate.query(
                """
                SELECT dataset_title, dataset_version, source_url, imported_at
                FROM import_dataset_meta
                WHERE source_key = 'col'
                """,
                rs -> {
                    if (!rs.next()) {
                        return null;
                    }
                    Timestamp ts = rs.getTimestamp(4);
                    Instant importedAt = ts == null ? null : ts.toInstant();
                    return new StatsSummaryDto.DatasetCitationDto(
                            rs.getString(1), rs.getString(2), rs.getString(3), importedAt);
                });

        return new StatsSummaryDto(total == null ? 0 : total, byRank, byKingdom, dataset);
    }
}
