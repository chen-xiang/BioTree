/**
 * 导入断点仓储：记录阶段与已处理数量，支持续跑。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
package com.chenxiang.biotree.infrastructure.importdata;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ImportCheckpointRepository {

    private final JdbcTemplate jdbcTemplate;

    public ImportCheckpointRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<Checkpoint> find(String jobKey) {
        return jdbcTemplate
                .query(
                        """
                        SELECT job_key, phase, processed_count, total_hint, detail_json, updated_at
                        FROM import_checkpoint WHERE job_key = ?
                        """,
                        (rs, rowNum) -> new Checkpoint(
                                rs.getString("job_key"),
                                rs.getString("phase"),
                                rs.getInt("processed_count"),
                                rs.getObject("total_hint") == null ? null : rs.getInt("total_hint"),
                                rs.getString("detail_json")),
                        jobKey)
                .stream()
                .findFirst();
    }

    public void upsert(String jobKey, String phase, int processedCount, Integer totalHint, String detail) {
        Instant now = Instant.now();
        int updated = jdbcTemplate.update(
                """
                UPDATE import_checkpoint
                SET phase = ?, processed_count = ?, total_hint = ?, detail_json = ?, updated_at = ?
                WHERE job_key = ?
                """,
                phase,
                processedCount,
                totalHint,
                detail,
                Timestamp.from(now),
                jobKey);
        if (updated == 0) {
            jdbcTemplate.update(
                    """
                    INSERT INTO import_checkpoint
                    (job_key, phase, processed_count, total_hint, detail_json, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """,
                    jobKey,
                    phase,
                    processedCount,
                    totalHint,
                    detail,
                    Timestamp.from(now));
        }
    }

    public void delete(String jobKey) {
        jdbcTemplate.update("DELETE FROM import_checkpoint WHERE job_key = ?", jobKey);
    }

    public record Checkpoint(String jobKey, String phase, int processedCount, Integer totalHint, String detailJson) {
    }
}
