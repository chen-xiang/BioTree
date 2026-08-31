/**
 * Catalogue of Life DwC-A 导入器：动物界/植物界七级分类 + 中英俗名。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 * Updated: 2026-08-31 批量化插入/路径回写/俗名写入以支撑全量导入
 */
package com.chenxiang.biotree.infrastructure.importdata;

import com.chenxiang.biotree.api.common.BusinessException;
import com.chenxiang.biotree.api.common.ErrorCode;
import com.chenxiang.biotree.domain.taxon.TaxonRank;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ColDwcaImporter {

    public static final String SOURCE_COL = "col";

    private static final Logger log = LoggerFactory.getLogger(ColDwcaImporter.class);
    private static final int BATCH_LOG_EVERY = 50_000;
    private static final int INSERT_BATCH_SIZE = 500;
    private static final int VERNACULAR_BATCH_SIZE = 500;

    private final JdbcTemplate jdbcTemplate;

    public ColDwcaImporter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public ImportStats importArchive(Path dwcaZip, ImportProperties properties) {
        if (dwcaZip == null || !dwcaZip.toFile().isFile()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "DwC-A zip not found");
        }
        Set<String> kingdoms = new HashSet<>();
        for (String k : properties.getKingdoms()) {
            if (StringUtils.hasText(k)) {
                kingdoms.add(k.trim());
            }
        }
        if (kingdoms.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "At least one kingdom is required");
        }

        if (properties.isReplace()) {
            clearTaxonData();
        }

        Map<String, String> parentIndex = new HashMap<>();
        Map<String, DwcaTaxonRow> nodes = new LinkedHashMap<>();
        Map<TaxonRank, AtomicInteger> rankCounters = new EnumMap<>(TaxonRank.class);
        for (TaxonRank rank : TaxonRank.values()) {
            rankCounters.put(rank, new AtomicInteger());
        }

        try (ZipFile zip = new ZipFile(dwcaZip.toFile())) {
            ZipEntry taxonEntry = requireEntry(zip, "Taxon.tsv");
            parseTaxa(zip.getInputStream(taxonEntry), kingdoms, properties.getMaxPerRank(), parentIndex, nodes, rankCounters);

            Map<String, Long> externalToId = insertTaxa(nodes, parentIndex, properties.isReplace());
            int vernacularCount = 0;
            if (properties.isImportVernaculars()) {
                ZipEntry vernacularEntry = zip.getEntry("VernacularName.tsv");
                if (vernacularEntry != null) {
                    vernacularCount = importVernaculars(zip.getInputStream(vernacularEntry), externalToId);
                }
            }
            rebuildChildCounts();
            ImportStats stats = new ImportStats(nodes.size(), vernacularCount, Map.copyOf(toIntMap(rankCounters)));
            log.info(
                    "COL import finished taxa={} vernaculars={} byRank={}",
                    stats.taxonCount(),
                    stats.vernacularCount(),
                    stats.byRank());
            return stats;
        } catch (IOException ex) {
            log.error("Failed to read DwC-A archive {}", dwcaZip, ex);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Failed to read DwC-A archive");
        }
    }

    private void parseTaxa(
            InputStream in,
            Set<String> kingdoms,
            int maxPerRank,
            Map<String, String> parentIndex,
            Map<String, DwcaTaxonRow> nodes,
            Map<TaxonRank, AtomicInteger> rankCounters)
            throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8), 1 << 20)) {
            String header = reader.readLine();
            if (header == null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Taxon.tsv is empty");
            }
            String line;
            long scanned = 0;
            while ((line = reader.readLine()) != null) {
                scanned++;
                if (line.isBlank()) {
                    continue;
                }
                String[] cols = line.split("\t", -1);
                if (cols.length < 21) {
                    continue;
                }
                String taxonId = cols[0];
                String parentId = emptyToNull(cols[1]);
                String status = cols[6];
                String rankRaw = cols[7];
                String scientificName = cols[8];
                String genericName = cols.length > 11 ? cols[11] : "";
                String specificEpithet = cols.length > 13 ? cols[13] : "";
                String kingdom = cols[20];

                if (!"accepted".equalsIgnoreCase(status)) {
                    continue;
                }
                if (!StringUtils.hasText(kingdom) || !kingdoms.contains(kingdom)) {
                    // 界本身的 kingdom 列可能为空，回退到学名
                    if (!("kingdom".equalsIgnoreCase(rankRaw) && kingdoms.contains(ColNameUtils.stripAuthorship(scientificName)))) {
                        continue;
                    }
                    kingdom = ColNameUtils.stripAuthorship(scientificName);
                }

                parentIndex.put(taxonId, parentId);

                var rankOpt = ColNameUtils.mapRank(rankRaw);
                if (rankOpt.isEmpty()) {
                    continue;
                }
                TaxonRank rank = rankOpt.get();
                if (maxPerRank > 0 && rankCounters.get(rank).get() >= maxPerRank) {
                    continue;
                }
                String canonical = ColNameUtils.canonicalName(rank, scientificName, genericName, specificEpithet);
                if (!StringUtils.hasText(canonical) || canonical.length() > 255) {
                    continue;
                }
                if (nodes.containsKey(taxonId)) {
                    continue;
                }
                nodes.put(taxonId, new DwcaTaxonRow(taxonId, parentId, rank, canonical, kingdom));
                rankCounters.get(rank).incrementAndGet();
                if (scanned % BATCH_LOG_EVERY == 0) {
                    log.info("Scanned {} taxon rows, selected {}", scanned, nodes.size());
                }
            }
            log.info("Finished scanning taxa rows={}, selected={}", scanned, nodes.size());
        }
    }

    private Map<String, Long> insertTaxa(
            Map<String, DwcaTaxonRow> nodes, Map<String, String> parentIndex, boolean replaced) {
        List<DwcaTaxonRow> ordered = new ArrayList<>(nodes.values());
        ordered.sort(Comparator.comparingInt(r -> r.rank().ordinal()));

        Map<String, Long> externalToId = new HashMap<>(Math.max(16, ordered.size() * 2));
        if (!replaced) {
            preloadExistingExternalIds(externalToId);
        }

        Map<Long, String> idToPath = new HashMap<>(Math.max(16, ordered.size() * 2));
        if (!replaced) {
            preloadExistingPaths(idToPath);
        }

        // 同父同学名 → 已有 id（去重并回填 external 映射）
        Map<String, Long> parentNameToId = new HashMap<>();
        if (!replaced) {
            preloadExistingParentNames(parentNameToId);
        }

        Instant now = Instant.now();
        Timestamp ts = Timestamp.from(now);

        String insertSql =
                """
                INSERT INTO taxon
                (parent_id, taxon_rank, scientific_name, materialized_path, child_count, is_accepted,
                 created_at, updated_at, created_by, external_source, external_id)
                VALUES (?, ?, ?, ?, 0, TRUE, ?, ?, 'col-import', ?, ?)
                """;

        AtomicInteger inserted = new AtomicInteger();
        jdbcTemplate.execute((java.sql.Connection connection) -> {
            try (PreparedStatement ps = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                List<PendingInsert> pending = new ArrayList<>(INSERT_BATCH_SIZE);
                for (DwcaTaxonRow row : ordered) {
                    if (externalToId.containsKey(row.taxonId())) {
                        continue;
                    }
                    Long parentDbId = resolveParentDbId(row, nodes, parentIndex, externalToId);
                    String uniqKey = uniquenessKey(parentDbId, row.scientificName());
                    Long existingId = parentNameToId.get(uniqKey);
                    if (existingId != null) {
                        externalToId.put(row.taxonId(), existingId);
                        continue;
                    }

                    if (parentDbId == null) {
                        ps.setNull(1, Types.BIGINT);
                    } else {
                        ps.setLong(1, parentDbId);
                    }
                    ps.setString(2, row.rank().name());
                    ps.setString(3, row.scientificName());
                    ps.setString(4, "/");
                    ps.setTimestamp(5, ts);
                    ps.setTimestamp(6, ts);
                    ps.setString(7, SOURCE_COL);
                    ps.setString(8, row.taxonId());
                    ps.addBatch();
                    pending.add(new PendingInsert(row, parentDbId, uniqKey));

                    if (pending.size() >= INSERT_BATCH_SIZE) {
                        flushInsertBatch(ps, pending, externalToId, idToPath, parentNameToId, inserted);
                        pending.clear();
                    }
                }
                if (!pending.isEmpty()) {
                    flushInsertBatch(ps, pending, externalToId, idToPath, parentNameToId, inserted);
                }
            } catch (SQLException ex) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Batch insert taxa failed: " + ex.getMessage());
            }
            return null;
        });

        log.info("Inserted taxa rows={}", inserted.get());
        return externalToId;
    }

    private void flushInsertBatch(
            PreparedStatement ps,
            List<PendingInsert> pending,
            Map<String, Long> externalToId,
            Map<Long, String> idToPath,
            Map<String, Long> parentNameToId,
            AtomicInteger inserted)
            throws SQLException {
        ps.executeBatch();
        List<Long> generatedIds = new ArrayList<>(pending.size());
        try (ResultSet keys = ps.getGeneratedKeys()) {
            while (keys.next()) {
                generatedIds.add(keys.getLong(1));
            }
        }
        if (generatedIds.size() != pending.size()) {
            throw new BusinessException(
                    ErrorCode.INTERNAL_ERROR,
                    "Generated key count mismatch: expected " + pending.size() + " got " + generatedIds.size());
        }

        List<Object[]> pathUpdates = new ArrayList<>(pending.size());
        for (int i = 0; i < pending.size(); i++) {
            PendingInsert item = pending.get(i);
            long id = generatedIds.get(i);
            Long parentDbId = item.parentDbId();
            String path;
            if (parentDbId == null) {
                path = "/" + id + "/";
            } else {
                String parentPath = idToPath.get(parentDbId);
                if (parentPath == null) {
                    throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Missing parent path for id=" + parentDbId);
                }
                path = parentPath + id + "/";
            }
            externalToId.put(item.row().taxonId(), id);
            idToPath.put(id, path);
            parentNameToId.put(item.uniqKey(), id);
            pathUpdates.add(new Object[] {path, id});
        }
        jdbcTemplate.batchUpdate("UPDATE taxon SET materialized_path = ? WHERE id = ?", pathUpdates);
        int total = inserted.addAndGet(pending.size());
        if (total / BATCH_LOG_EVERY > (total - pending.size()) / BATCH_LOG_EVERY) {
            log.info("Inserted {} taxa", total);
        }
        ps.clearBatch();
    }

    private void preloadExistingExternalIds(Map<String, Long> externalToId) {
        jdbcTemplate.query(
                "SELECT external_id, id FROM taxon WHERE external_source = ? AND external_id IS NOT NULL",
                rs -> {
                    externalToId.put(rs.getString(1), rs.getLong(2));
                },
                SOURCE_COL);
        log.info("Preloaded existing COL external ids={}", externalToId.size());
    }

    private void preloadExistingPaths(Map<Long, String> idToPath) {
        jdbcTemplate.query("SELECT id, materialized_path FROM taxon", rs -> {
            idToPath.put(rs.getLong(1), rs.getString(2));
        });
    }

    private void preloadExistingParentNames(Map<String, Long> parentNameToId) {
        jdbcTemplate.query("SELECT parent_id, scientific_name, id FROM taxon", rs -> {
            Long parentId = rs.getObject(1) == null ? null : rs.getLong(1);
            parentNameToId.putIfAbsent(uniquenessKey(parentId, rs.getString(2)), rs.getLong(3));
        });
    }

    private static String uniquenessKey(Long parentId, String scientificName) {
        String parentPart = parentId == null ? "ROOT" : parentId.toString();
        return parentPart + "|" + scientificName.toLowerCase(Locale.ROOT);
    }

    private Long resolveParentDbId(
            DwcaTaxonRow row,
            Map<String, DwcaTaxonRow> nodes,
            Map<String, String> parentIndex,
            Map<String, Long> externalToId) {
        if (row.rank() == TaxonRank.KINGDOM) {
            return null;
        }
        String cursor = row.parentId();
        int guard = 0;
        while (cursor != null && guard++ < 64) {
            if (nodes.containsKey(cursor) && externalToId.containsKey(cursor)) {
                return externalToId.get(cursor);
            }
            // 父级是被跳过的中间等级时继续向上
            if (externalToId.containsKey(cursor)) {
                return externalToId.get(cursor);
            }
            cursor = parentIndex.get(cursor);
        }
        // 回退：按界名挂到对应界
        if (StringUtils.hasText(row.kingdom())) {
            for (Map.Entry<String, DwcaTaxonRow> e : nodes.entrySet()) {
                if (e.getValue().rank() == TaxonRank.KINGDOM
                        && e.getValue().scientificName().equalsIgnoreCase(row.kingdom())
                        && externalToId.containsKey(e.getKey())) {
                    return externalToId.get(e.getKey());
                }
            }
        }
        return null;
    }

    private int importVernaculars(InputStream in, Map<String, Long> externalToId) throws IOException {
        Map<String, Map<String, String>> best = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8), 1 << 20)) {
            reader.readLine(); // header
            String line;
            while ((line = reader.readLine()) != null) {
                String[] cols = line.split("\t", -1);
                if (cols.length < 3) {
                    continue;
                }
                String taxonId = cols[0];
                if (!externalToId.containsKey(taxonId)) {
                    continue;
                }
                var localeOpt = ColNameUtils.mapLocale(cols[1]);
                if (localeOpt.isEmpty()) {
                    continue;
                }
                String name = cols[2] == null ? "" : cols[2].trim();
                if (!StringUtils.hasText(name) || name.length() > 255) {
                    continue;
                }
                best.computeIfAbsent(taxonId, k -> new HashMap<>()).putIfAbsent(localeOpt.get(), name);
            }
        }

        // 已有 (taxon_id, locale) 键，避免逐条 SELECT
        Set<String> existingKeys = new HashSet<>();
        jdbcTemplate.query("SELECT taxon_id, locale FROM taxon_i18n", rs -> {
            existingKeys.add(rs.getLong(1) + "|" + rs.getString(2));
        });

        List<Object[]> inserts = new ArrayList<>();
        List<Object[]> updates = new ArrayList<>();
        int count = 0;
        for (Map.Entry<String, Map<String, String>> e : best.entrySet()) {
            Long taxonId = externalToId.get(e.getKey());
            for (Map.Entry<String, String> loc : e.getValue().entrySet()) {
                String key = taxonId + "|" + loc.getKey();
                if (existingKeys.contains(key)) {
                    updates.add(new Object[] {loc.getValue(), taxonId, loc.getKey()});
                } else {
                    inserts.add(new Object[] {taxonId, loc.getKey(), loc.getValue()});
                    existingKeys.add(key);
                }
                count++;
                if (inserts.size() >= VERNACULAR_BATCH_SIZE) {
                    jdbcTemplate.batchUpdate(
                            "INSERT INTO taxon_i18n (taxon_id, locale, common_name) VALUES (?, ?, ?)", inserts);
                    inserts.clear();
                }
                if (updates.size() >= VERNACULAR_BATCH_SIZE) {
                    jdbcTemplate.batchUpdate(
                            "UPDATE taxon_i18n SET common_name = ? WHERE taxon_id = ? AND locale = ?", updates);
                    updates.clear();
                }
            }
        }
        if (!inserts.isEmpty()) {
            jdbcTemplate.batchUpdate(
                    "INSERT INTO taxon_i18n (taxon_id, locale, common_name) VALUES (?, ?, ?)", inserts);
        }
        if (!updates.isEmpty()) {
            jdbcTemplate.batchUpdate(
                    "UPDATE taxon_i18n SET common_name = ? WHERE taxon_id = ? AND locale = ?", updates);
        }
        log.info("Imported vernacular rows={}", count);
        return count;
    }

    private void rebuildChildCounts() {
        jdbcTemplate.update("UPDATE taxon SET child_count = 0");
        List<Object[]> batches = new ArrayList<>();
        jdbcTemplate.query(
                """
                SELECT parent_id AS pid, COUNT(*) AS cnt
                FROM taxon
                WHERE parent_id IS NOT NULL
                GROUP BY parent_id
                """,
                rs -> {
                    batches.add(new Object[] {rs.getInt("cnt"), rs.getLong("pid")});
                    if (batches.size() >= INSERT_BATCH_SIZE) {
                        jdbcTemplate.batchUpdate("UPDATE taxon SET child_count = ? WHERE id = ?", batches);
                        batches.clear();
                    }
                });
        if (!batches.isEmpty()) {
            jdbcTemplate.batchUpdate("UPDATE taxon SET child_count = ? WHERE id = ?", batches);
        }
    }

    private void clearTaxonData() {
        log.info("Clearing existing taxon/media/i18n data before import");
        jdbcTemplate.update("DELETE FROM taxon_media");
        jdbcTemplate.update("DELETE FROM taxon_i18n");
        // 自引用表：先断开父级再删
        jdbcTemplate.update("UPDATE taxon SET parent_id = NULL");
        jdbcTemplate.update("DELETE FROM taxon");
    }

    private static ZipEntry requireEntry(ZipFile zip, String name) {
        ZipEntry entry = zip.getEntry(name);
        if (entry == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, name + " missing in DwC-A");
        }
        return entry;
    }

    private static String emptyToNull(String value) {
        return StringUtils.hasText(value) ? value : null;
    }

    private static Map<String, Integer> toIntMap(Map<TaxonRank, AtomicInteger> counters) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (TaxonRank rank : TaxonRank.values()) {
            map.put(rank.name().toLowerCase(Locale.ROOT), counters.get(rank).get());
        }
        return map;
    }

    private record PendingInsert(DwcaTaxonRow row, Long parentDbId, String uniqKey) {
    }

    public record ImportStats(int taxonCount, int vernacularCount, Map<String, Integer> byRank) {
    }
}
