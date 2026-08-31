/**
 * Catalogue of Life DwC-A 导入器：动物界/植物界七级分类 + 中英俗名。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
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
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ColDwcaImporter {

    public static final String SOURCE_COL = "col";

    private static final Logger log = LoggerFactory.getLogger(ColDwcaImporter.class);
    private static final int BATCH_LOG_EVERY = 50_000;

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

            Map<String, Long> externalToId = insertTaxa(nodes, parentIndex);
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

    private Map<String, Long> insertTaxa(Map<String, DwcaTaxonRow> nodes, Map<String, String> parentIndex) {
        List<DwcaTaxonRow> ordered = new ArrayList<>(nodes.values());
        ordered.sort(Comparator.comparingInt(r -> r.rank().ordinal()));

        Map<String, Long> externalToId = new HashMap<>(ordered.size() * 2);
        Map<Long, String> idToPath = new HashMap<>();
        Map<Long, Integer> childCount = new HashMap<>();
        Instant now = Instant.now();

        String insertSql =
                """
                INSERT INTO taxon
                (parent_id, taxon_rank, scientific_name, materialized_path, child_count, is_accepted,
                 created_at, updated_at, created_by, external_source, external_id)
                VALUES (?, ?, ?, ?, 0, TRUE, ?, ?, 'col-import', ?, ?)
                """;

        int inserted = 0;
        for (DwcaTaxonRow row : ordered) {
            Long parentDbId = resolveParentDbId(row, nodes, parentIndex, externalToId);
            // 同父同学名去重：保留先插入的外部记录
            if (parentDbId == null) {
                Long existingRoot = findExisting(null, row.scientificName());
                if (existingRoot != null) {
                    externalToId.put(row.taxonId(), existingRoot);
                    continue;
                }
            } else {
                Long existing = findExisting(parentDbId, row.scientificName());
                if (existing != null) {
                    externalToId.put(row.taxonId(), existing);
                    continue;
                }
            }

            GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
            Long finalParentDbId = parentDbId;
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(insertSql, new String[] {"id"});
                if (finalParentDbId == null) {
                    ps.setObject(1, null);
                } else {
                    ps.setLong(1, finalParentDbId);
                }
                ps.setString(2, row.rank().name());
                ps.setString(3, row.scientificName());
                ps.setString(4, "/");
                ps.setObject(5, java.sql.Timestamp.from(now));
                ps.setObject(6, java.sql.Timestamp.from(now));
                ps.setString(7, SOURCE_COL);
                ps.setString(8, row.taxonId());
                return ps;
            }, keyHolder);

            Number key = keyHolder.getKey();
            if (key == null && keyHolder.getKeys() != null && keyHolder.getKeys().get("id") != null) {
                key = (Number) keyHolder.getKeys().get("id");
            }
            if (key == null) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Failed to obtain generated taxon id");
            }
            long id = key.longValue();
            String path = parentDbId == null ? "/" + id + "/" : idToPath.get(parentDbId) + id + "/";
            jdbcTemplate.update("UPDATE taxon SET materialized_path = ? WHERE id = ?", path, id);
            externalToId.put(row.taxonId(), id);
            idToPath.put(id, path);
            if (parentDbId != null) {
                childCount.merge(parentDbId, 1, Integer::sum);
            }
            inserted++;
            if (inserted % BATCH_LOG_EVERY == 0) {
                log.info("Inserted {} taxa", inserted);
            }
        }

        for (Map.Entry<Long, Integer> e : childCount.entrySet()) {
            jdbcTemplate.update("UPDATE taxon SET child_count = ? WHERE id = ?", e.getValue(), e.getKey());
        }
        log.info("Inserted taxa rows={}", inserted);
        return externalToId;
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

    private Long findExisting(Long parentId, String scientificName) {
        if (parentId == null) {
            List<Long> ids = jdbcTemplate.query(
                    "SELECT id FROM taxon WHERE parent_id IS NULL AND scientific_name = ?",
                    (rs, rowNum) -> rs.getLong(1),
                    scientificName);
            return ids.isEmpty() ? null : ids.getFirst();
        }
        List<Long> ids = jdbcTemplate.query(
                "SELECT id FROM taxon WHERE parent_id = ? AND scientific_name = ?",
                (rs, rowNum) -> rs.getLong(1),
                parentId,
                scientificName);
        return ids.isEmpty() ? null : ids.getFirst();
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

        String sql =
                """
                INSERT INTO taxon_i18n (taxon_id, locale, common_name, summary, description)
                VALUES (?, ?, ?, NULL, NULL)
                ON DUPLICATE KEY UPDATE common_name = VALUES(common_name)
                """;
        // H2 兼容：先查再插
        int count = 0;
        for (Map.Entry<String, Map<String, String>> e : best.entrySet()) {
            Long taxonId = externalToId.get(e.getKey());
            for (Map.Entry<String, String> loc : e.getValue().entrySet()) {
                Integer exists = jdbcTemplate.query(
                        "SELECT id FROM taxon_i18n WHERE taxon_id = ? AND locale = ?",
                        rs -> rs.next() ? rs.getInt(1) : null,
                        taxonId,
                        loc.getKey());
                if (exists == null) {
                    jdbcTemplate.update(
                            "INSERT INTO taxon_i18n (taxon_id, locale, common_name) VALUES (?, ?, ?)",
                            taxonId,
                            loc.getKey(),
                            loc.getValue());
                } else {
                    jdbcTemplate.update(
                            "UPDATE taxon_i18n SET common_name = ? WHERE taxon_id = ? AND locale = ?",
                            loc.getValue(),
                            taxonId,
                            loc.getKey());
                }
                count++;
            }
        }
        log.info("Imported vernacular rows={}", count);
        return count;
    }

    private void rebuildChildCounts() {
        jdbcTemplate.update("UPDATE taxon SET child_count = 0");
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                """
                SELECT parent_id AS pid, COUNT(*) AS cnt
                FROM taxon
                WHERE parent_id IS NOT NULL
                GROUP BY parent_id
                """);
        for (Map<String, Object> row : rows) {
            jdbcTemplate.update(
                    "UPDATE taxon SET child_count = ? WHERE id = ?",
                    ((Number) row.get("cnt")).intValue(),
                    ((Number) row.get("pid")).longValue());
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

    public record ImportStats(int taxonCount, int vernacularCount, Map<String, Integer> byRank) {
    }
}
