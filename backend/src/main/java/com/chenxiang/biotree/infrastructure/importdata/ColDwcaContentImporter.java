/**
 * CoL DwC-A 内容扩展导入：描述、分布、媒体外链、数据集元数据。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
package com.chenxiang.biotree.infrastructure.importdata;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
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
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Component
public class ColDwcaContentImporter {

    private static final Logger log = LoggerFactory.getLogger(ColDwcaContentImporter.class);
    private static final int MAX_DESCRIPTION = 50_000;
    private static final Set<String> ALLOWED_LICENSES = Set.of(
            "cc0",
            "cc-by",
            "cc-by-sa",
            "cc-by-nc",
            "cc-by-nc-sa",
            "cc-by-nd",
            "cc-by-nc-nd",
            "public domain",
            "pd");

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public ColDwcaContentImporter(JdbcTemplate jdbcTemplate, PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public int importDescriptions(InputStream in, Map<String, Long> externalToId, int batchSize) throws IOException {
        AtomicInteger count = new AtomicInteger();
        List<Object[]> updates = new ArrayList<>();
        List<Object[]> inserts = new ArrayList<>();
        Set<String> touched = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8), 1 << 20)) {
            String header = reader.readLine();
            Map<String, Integer> cols = DwcaTsvHeaders.parse(header);
            String line;
            while ((line = reader.readLine()) != null) {
                String[] row = line.split("\t", -1);
                String taxonExt = DwcaTsvHeaders.col(row, cols, "taxonid", "id", "coreid");
                if (taxonExt == null || !externalToId.containsKey(taxonExt)) {
                    continue;
                }
                String description = DwcaTsvHeaders.col(row, cols, "description", "abstract");
                if (!StringUtils.hasText(description)) {
                    continue;
                }
                if (description.length() > MAX_DESCRIPTION) {
                    description = description.substring(0, MAX_DESCRIPTION);
                }
                description = stripDangerousHtml(description);
                String locale = ColNameUtils.mapLocale(DwcaTsvHeaders.col(row, cols, "language", "lang"))
                        .orElse("en");
                String key = externalToId.get(taxonExt) + "|" + locale;
                if (!touched.add(key)) {
                    continue;
                }
                Long taxonId = externalToId.get(taxonExt);
                Integer existing = jdbcTemplate.query(
                        """
                        SELECT id, description FROM taxon_i18n WHERE taxon_id = ? AND locale = ?
                        """,
                        rs -> rs.next()
                                ? (StringUtils.hasText(rs.getString(2)) ? -1 : rs.getInt(1))
                                : 0,
                        taxonId,
                        locale);
                if (existing != null && existing == -1) {
                    continue; // 不覆盖人工/已有非空描述
                }
                if (existing != null && existing > 0) {
                    updates.add(new Object[] {description, existing});
                } else {
                    inserts.add(new Object[] {taxonId, locale, description});
                }
                count.incrementAndGet();
                if (updates.size() + inserts.size() >= batchSize) {
                    flushDescriptions(updates, inserts);
                    updates = new ArrayList<>();
                    inserts = new ArrayList<>();
                }
            }
        }
        flushDescriptions(updates, inserts);
        log.info("Imported description rows={}", count.get());
        return count.get();
    }

    private void flushDescriptions(List<Object[]> updates, List<Object[]> inserts) {
        if (updates.isEmpty() && inserts.isEmpty()) {
            return;
        }
        transactionTemplate.executeWithoutResult(status -> {
            if (!updates.isEmpty()) {
                jdbcTemplate.batchUpdate(
                        "UPDATE taxon_i18n SET description = ? WHERE id = ? AND (description IS NULL OR description = '')",
                        updates);
            }
            if (!inserts.isEmpty()) {
                jdbcTemplate.batchUpdate(
                        """
                        INSERT INTO taxon_i18n (taxon_id, locale, description, preferred)
                        VALUES (?, ?, ?, FALSE)
                        """,
                        inserts);
            }
        });
    }

    public int importDistributions(InputStream in, Map<String, Long> externalToId, int batchSize) throws IOException {
        AtomicInteger count = new AtomicInteger();
        List<Object[]> batch = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8), 1 << 20)) {
            String header = reader.readLine();
            Map<String, Integer> cols = DwcaTsvHeaders.parse(header);
            String line;
            while ((line = reader.readLine()) != null) {
                String[] row = line.split("\t", -1);
                String taxonExt = DwcaTsvHeaders.col(row, cols, "taxonid", "id", "coreid");
                if (taxonExt == null || !externalToId.containsKey(taxonExt)) {
                    continue;
                }
                String country = DwcaTsvHeaders.col(row, cols, "countrycode", "country");
                String locality = DwcaTsvHeaders.col(row, cols, "locality", "locationid", "stateprovince");
                String means = DwcaTsvHeaders.col(row, cols, "establishmentmeans", "occurrencestatus");
                String threat = DwcaTsvHeaders.col(row, cols, "threatstatus", "threatStatus");
                String source = DwcaTsvHeaders.col(row, cols, "source", "datasetname", "references");
                if (!StringUtils.hasText(country) && !StringUtils.hasText(locality)) {
                    continue;
                }
                if (country != null && country.length() > 16) {
                    country = country.substring(0, 16);
                }
                batch.add(new Object[] {
                    externalToId.get(taxonExt),
                    truncate(country, 16),
                    truncate(locality, 512),
                    truncate(means, 64),
                    truncate(threat, 64),
                    truncate(source, 512)
                });
                count.incrementAndGet();
                if (batch.size() >= batchSize) {
                    flushDistributions(batch);
                    batch = new ArrayList<>();
                }
            }
        }
        flushDistributions(batch);
        log.info("Imported distribution rows={}", count.get());
        return count.get();
    }

    private void flushDistributions(List<Object[]> batch) {
        if (batch.isEmpty()) {
            return;
        }
        transactionTemplate.executeWithoutResult(status -> jdbcTemplate.batchUpdate(
                """
                INSERT INTO taxon_distribution
                (taxon_id, country_code, locality, establishment_means, threat_status, source_text)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                batch));
    }

    public int importMediaLinks(InputStream in, Map<String, Long> externalToId, int batchSize) throws IOException {
        AtomicInteger count = new AtomicInteger();
        List<Object[]> batch = new ArrayList<>();
        Timestamp ts = Timestamp.from(Instant.now());
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8), 1 << 20)) {
            String header = reader.readLine();
            Map<String, Integer> cols = DwcaTsvHeaders.parse(header);
            String line;
            while ((line = reader.readLine()) != null) {
                String[] row = line.split("\t", -1);
                String taxonExt = DwcaTsvHeaders.col(row, cols, "taxonid", "id", "coreid");
                if (taxonExt == null || !externalToId.containsKey(taxonExt)) {
                    continue;
                }
                String url = DwcaTsvHeaders.col(row, cols, "identifier", "accessuri", "source", "references");
                if (!StringUtils.hasText(url) || !(url.startsWith("http://") || url.startsWith("https://"))) {
                    continue;
                }
                if (url.length() > 1024) {
                    continue;
                }
                String license = DwcaTsvHeaders.col(row, cols, "license", "rights", "rightsholder");
                if (!isAllowedLicense(license)) {
                    continue;
                }
                String attribution = DwcaTsvHeaders.col(row, cols, "creator", "attribution", "rightsHolder", "owner");
                String caption = DwcaTsvHeaders.col(row, cols, "title", "description", "caption");
                String mime = DwcaTsvHeaders.col(row, cols, "format", "mimetype", "type");
                String locale = ColNameUtils.mapLocale(DwcaTsvHeaders.col(row, cols, "language", "lang")).orElse(null);
                batch.add(new Object[] {
                    externalToId.get(taxonExt),
                    url,
                    truncate(mime, 128),
                    truncate(caption, 512),
                    truncate(license, 255),
                    truncate(attribution, 255),
                    locale,
                    ts
                });
                count.incrementAndGet();
                if (batch.size() >= batchSize) {
                    flushMedia(batch);
                    batch = new ArrayList<>();
                }
            }
        }
        flushMedia(batch);
        log.info("Imported media link rows={}", count.get());
        return count.get();
    }

    private void flushMedia(List<Object[]> batch) {
        if (batch.isEmpty()) {
            return;
        }
        transactionTemplate.executeWithoutResult(status -> jdbcTemplate.batchUpdate(
                """
                INSERT INTO taxon_media
                (taxon_id, storage_key, source_url, mime_type, sort_order, caption, license, attribution, locale, created_at)
                VALUES (?, NULL, ?, ?, 0, ?, ?, ?, ?, ?)
                """,
                batch));
    }

    public void upsertDatasetMeta(ZipFile zip, String sourceKey) {
        String title = "Catalogue of Life";
        String version = null;
        String sourceUrl = "https://www.catalogueoflife.org/";
        ZipEntry eml = zip.getEntry("eml.xml");
        if (eml == null) {
            eml = zip.getEntry("metadata/eml.xml");
        }
        if (eml != null) {
            try (InputStream in = zip.getInputStream(eml)) {
                String xml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                String t = extractXmlTag(xml, "title");
                if (StringUtils.hasText(t)) {
                    title = truncate(t, 255);
                }
                String pub = extractXmlTag(xml, "pubDate");
                if (!StringUtils.hasText(pub)) {
                    pub = extractXmlTag(xml, "packageId");
                }
                if (StringUtils.hasText(pub)) {
                    version = truncate(pub, 64);
                }
            } catch (Exception e) {
                log.warn("Failed to parse eml.xml for dataset meta: {}", e.toString());
            }
        }
        String finalTitle = title;
        String finalVersion = version;
        String finalUrl = sourceUrl;
        transactionTemplate.executeWithoutResult(status -> {
            jdbcTemplate.update("DELETE FROM import_dataset_meta WHERE source_key = ?", sourceKey);
            jdbcTemplate.update(
                    """
                    INSERT INTO import_dataset_meta
                    (source_key, dataset_title, dataset_version, source_url, imported_at)
                    VALUES (?, ?, ?, ?, ?)
                    """,
                    sourceKey,
                    finalTitle,
                    finalVersion,
                    finalUrl,
                    Timestamp.from(Instant.now()));
        });
        log.info("Upserted dataset meta source={} title={} version={}", sourceKey, finalTitle, finalVersion);
    }

    private static boolean isAllowedLicense(String license) {
        if (!StringUtils.hasText(license)) {
            return false;
        }
        String normalized = license.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        for (String allowed : ALLOWED_LICENSES) {
            if (normalized.contains(allowed)) {
                return true;
            }
        }
        return normalized.contains("creative commons") || normalized.startsWith("http");
    }

    private static String stripDangerousHtml(String input) {
        String s = input.replaceAll("(?i)<script[^>]*>.*?</script>", " ");
        s = s.replaceAll("(?i)<iframe[^>]*>.*?</iframe>", " ");
        s = s.replaceAll("(?i)\\son\\w+\\s*=\\s*(\"[^\"]*\"|'[^']*'|[^\\s>]+)", " ");
        return s.trim();
    }

    private static String extractXmlTag(String xml, String tag) {
        String open = "<" + tag;
        int start = xml.indexOf(open);
        if (start < 0) {
            return null;
        }
        int gt = xml.indexOf('>', start);
        if (gt < 0) {
            return null;
        }
        int end = xml.indexOf("</" + tag + ">", gt);
        if (end < 0) {
            return null;
        }
        return xml.substring(gt + 1, end).replaceAll("<[^>]+>", "").trim();
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
