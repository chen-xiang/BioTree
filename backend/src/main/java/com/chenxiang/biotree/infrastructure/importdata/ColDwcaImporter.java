/**
 * Catalogue of Life DwC-A 导入器：流式写入暂存表，再按等级落库，避免全量节点常驻内存。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 * Updated: 2026-08-31 批量化插入/路径回写/俗名写入以支撑全量导入
 * Updated: 2026-08-31 小事务断点续跑与异名导入
 * Updated: 2026-08-31 暂存表流式导入，去掉全量 nodes Map
 * Updated: 2026-09-01 同批同父同学名合并，避免 uk_taxon_parent_name 冲突
 * Updated: 2026-09-01 界名过滤改为不区分大小写
 * Updated: 2026-09-02 replace/非续跑不再沿用旧 checkpoint 跳过高等级
 * Updated: 2026-09-02 落库改为 keyset 分页、少写 checkpoint，MySQL 导入期间挂起全文索引
 * Updated: 2026-09-02 续跑/落库校验、表头解析、空界异名、按 external_id 回查主键
 * Updated: 2026-09-03 全文索引仅在导入成功后尽力加回，失败不中断、不掩盖落库异常
 * Updated: 2026-09-03 同父学名按去重音折叠，冲突时并入已有节点
 */
package com.chenxiang.biotree.infrastructure.importdata;

import com.chenxiang.biotree.api.common.BusinessException;
import com.chenxiang.biotree.api.common.ErrorCode;
import com.chenxiang.biotree.application.SimpleParentSupport;
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
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Service
public class ColDwcaImporter {

    public static final String SOURCE_COL = "col";

    private static final Logger log = LoggerFactory.getLogger(ColDwcaImporter.class);
    private static final int BATCH_LOG_EVERY = 50_000;
    private static final int CHECKPOINT_EVERY = 50_000;
    private static final String PHASE_STAGE = "STAGE";
    private static final String PHASE_RANK_PREFIX = "RANK_";
    private static final String PHASE_VERNACULARS = "VERNACULARS";
    private static final String PHASE_SYNONYMS = "SYNONYMS";
    private static final String PHASE_DESCRIPTIONS = "DESCRIPTIONS";
    private static final String PHASE_DISTRIBUTIONS = "DISTRIBUTIONS";
    private static final String PHASE_MEDIA = "MEDIA";
    private static final String PHASE_DATASET_META = "DATASET_META";
    private static final String PHASE_COUNTS = "COUNTS";

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final ImportCheckpointRepository checkpointRepository;
    private final ColDwcaContentImporter contentImporter;
    private final Map<String, Long> kingdomIdCache = new HashMap<>();
    private Boolean mysql;

    public ColDwcaImporter(
            JdbcTemplate jdbcTemplate,
            PlatformTransactionManager transactionManager,
            ImportCheckpointRepository checkpointRepository,
            ColDwcaContentImporter contentImporter) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.checkpointRepository = checkpointRepository;
        this.contentImporter = contentImporter;
    }

    public ImportStats importArchive(Path dwcaZip, ImportProperties properties) {
        if (dwcaZip == null || !dwcaZip.toFile().isFile()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "DwC-A zip not found");
        }
        Set<String> kingdoms = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (String k : properties.getKingdoms()) {
            if (StringUtils.hasText(k)) {
                kingdoms.add(k.trim());
            }
        }
        if (kingdoms.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "At least one kingdom is required");
        }
        kingdomIdCache.clear();

        String jobKey = StringUtils.hasText(properties.getJobKey()) ? properties.getJobKey() : SOURCE_COL;
        Optional<ImportCheckpointRepository.Checkpoint> existing = checkpointRepository.find(jobKey);
        boolean resuming = properties.isResume() && existing.isPresent();
        boolean doReplace = properties.isReplace() && !resuming;
        if (doReplace) {
            transactionTemplate.executeWithoutResult(status -> {
                clearTaxonData();
                clearStaging();
            });
            checkpointRepository.delete(jobKey);
        }
        if (resuming) {
            log.info(
                    "Resuming COL import jobKey={} phase={} processed={}",
                    jobKey,
                    existing.get().phase(),
                    existing.get().processedCount());
            assertKingdomsPresent("Cannot resume COL import: no kingdoms in database, run replace=true");
        }

        int batchSize = Math.max(50, properties.getCommitBatchSize());
        Map<TaxonRank, AtomicInteger> rankCounters = new EnumMap<>(TaxonRank.class);
        for (TaxonRank rank : TaxonRank.values()) {
            rankCounters.put(rank, new AtomicInteger());
        }

        boolean droppedFulltext = false;
        boolean completed = false;
        try (ZipFile zip = new ZipFile(dwcaZip.toFile())) {
            // replace 会清库但 existing 仍是删除前读到的旧断点；非续跑必须从头落库
            String phase = resuming
                    ? existing.map(ImportCheckpointRepository.Checkpoint::phase).orElse(PHASE_STAGE)
                    : PHASE_STAGE;
            if (phaseOrder(phase) <= phaseOrder(PHASE_STAGE) || !stagingHasTaxa()) {
                checkpointRepository.upsert(jobKey, PHASE_STAGE, 0, null, "streaming");
                StageCounts stageCounts = stageFromArchive(
                        zip,
                        kingdoms,
                        properties.getMaxPerRank(),
                        properties.isImportSynonyms(),
                        properties.isLegacySevenRanks(),
                        batchSize);
                for (var e : stageCounts.byRank().entrySet()) {
                    rankCounters.get(e.getKey()).set(e.getValue());
                }
                log.info(
                        "Staging finished taxa={} synonyms={} edges={} byRank={}",
                        stageCounts.taxonCount(),
                        stageCounts.synonymCount(),
                        stageCounts.edgeCount(),
                        stageCounts.byRank());
                if (rankCounters.get(TaxonRank.KINGDOM).get() == 0) {
                    throw new BusinessException(
                            ErrorCode.BAD_REQUEST, "Staging produced no kingdoms; check app.import.kingdoms");
                }
            } else {
                fillRankCountersFromStaging(rankCounters);
            }

            droppedFulltext = suspendMysqlFulltextIndexes();

            Map<String, Long> externalToId = new HashMap<>();
            preloadExistingExternalIds(externalToId);
            Map<Long, String> idToPath = new HashMap<>();
            preloadExistingPaths(idToPath);
            Map<Long, TaxonRank> idToRank = new HashMap<>();
            preloadExistingRanks(idToRank);
            Map<String, Long> parentNameToId = new HashMap<>();
            preloadExistingParentNames(parentNameToId);

            for (TaxonRank rank : TaxonRank.values()) {
                String rankPhase = PHASE_RANK_PREFIX + rank.name();
                if (phaseOrder(phase) > phaseOrder(rankPhase)) {
                    continue;
                }
                checkpointRepository.upsert(jobKey, rankPhase, 0, rankCounters.get(rank).get(), null);
                insertRankFromStaging(rank, externalToId, idToPath, idToRank, parentNameToId, jobKey, batchSize);
            }
            validateImportedHierarchy();

            int vernacularCount = 0;
            if (properties.isImportVernaculars() && phaseOrder(phase) <= phaseOrder(PHASE_VERNACULARS)) {
                checkpointRepository.upsert(jobKey, PHASE_VERNACULARS, 0, null, null);
                ZipEntry vernacularEntry = zip.getEntry("VernacularName.tsv");
                if (vernacularEntry != null) {
                    vernacularCount = importVernaculars(zip.getInputStream(vernacularEntry), externalToId, batchSize);
                }
            }

            int synonymCount = 0;
            if (properties.isImportSynonyms() && phaseOrder(phase) <= phaseOrder(PHASE_SYNONYMS)) {
                checkpointRepository.upsert(jobKey, PHASE_SYNONYMS, 0, null, null);
                synonymCount = importSynonymsFromStaging(externalToId, batchSize);
            }

            int descriptionCount = 0;
            if (properties.isImportDescriptions() && phaseOrder(phase) <= phaseOrder(PHASE_DESCRIPTIONS)) {
                checkpointRepository.upsert(jobKey, PHASE_DESCRIPTIONS, 0, null, null);
                ZipEntry descEntry = firstPresent(zip, "Description.tsv", "Descriptions.tsv");
                if (descEntry != null) {
                    descriptionCount =
                            contentImporter.importDescriptions(zip.getInputStream(descEntry), externalToId, batchSize);
                }
            }

            int distributionCount = 0;
            if (properties.isImportDistributions() && phaseOrder(phase) <= phaseOrder(PHASE_DISTRIBUTIONS)) {
                checkpointRepository.upsert(jobKey, PHASE_DISTRIBUTIONS, 0, null, null);
                ZipEntry distEntry = firstPresent(zip, "Distribution.tsv", "Distributions.tsv");
                if (distEntry != null) {
                    distributionCount = contentImporter.importDistributions(
                            zip.getInputStream(distEntry), externalToId, batchSize);
                }
            }

            int mediaCount = 0;
            if (properties.isImportMedia() && phaseOrder(phase) <= phaseOrder(PHASE_MEDIA)) {
                checkpointRepository.upsert(jobKey, PHASE_MEDIA, 0, null, null);
                ZipEntry mediaEntry = firstPresent(zip, "Media.tsv", "Multimedia.tsv", "Image.tsv");
                if (mediaEntry != null) {
                    mediaCount =
                            contentImporter.importMediaLinks(zip.getInputStream(mediaEntry), externalToId, batchSize);
                }
            }

            if (phaseOrder(phase) <= phaseOrder(PHASE_DATASET_META)) {
                checkpointRepository.upsert(jobKey, PHASE_DATASET_META, 0, null, null);
                contentImporter.upsertDatasetMeta(zip, SOURCE_COL);
            }

            checkpointRepository.upsert(jobKey, PHASE_COUNTS, 0, null, null);
            transactionTemplate.executeWithoutResult(status -> rebuildChildCounts());
            transactionTemplate.executeWithoutResult(status -> clearStaging());
            checkpointRepository.delete(jobKey);

            int taxonCount = rankCounters.values().stream().mapToInt(AtomicInteger::get).sum();
            ImportStats stats = new ImportStats(
                    taxonCount,
                    vernacularCount,
                    synonymCount,
                    descriptionCount,
                    distributionCount,
                    mediaCount,
                    Map.copyOf(toIntMap(rankCounters)));
            log.info(
                    "COL import finished taxa={} vernaculars={} synonyms={} descriptions={} distributions={} media={} byRank={}",
                    stats.taxonCount(),
                    stats.vernacularCount(),
                    stats.synonymCount(),
                    stats.descriptionCount(),
                    stats.distributionCount(),
                    stats.mediaCount(),
                    stats.byRank());
            completed = true;
            return stats;
        } catch (IOException ex) {
            log.error("Failed to read DwC-A archive {}", dwcaZip, ex);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Failed to read DwC-A archive");
        } finally {
            if (droppedFulltext && completed) {
                restoreMysqlFulltextIndexes();
            } else if (droppedFulltext) {
                log.warn("Skipped restoring FULLTEXT indexes after a failed import");
            }
        }
    }

    private StageCounts stageFromArchive(
            ZipFile zip,
            Set<String> kingdoms,
            int maxPerRank,
            boolean collectSynonyms,
            boolean legacySeven,
            int batchSize)
            throws IOException {
        transactionTemplate.executeWithoutResult(status -> clearStaging());
        ZipEntry taxonEntry = requireEntry(zip, "Taxon.tsv");
        Map<TaxonRank, AtomicInteger> counters = new EnumMap<>(TaxonRank.class);
        for (TaxonRank rank : TaxonRank.values()) {
            counters.put(rank, new AtomicInteger());
        }
        AtomicInteger edgeCount = new AtomicInteger();
        AtomicInteger taxonCount = new AtomicInteger();
        AtomicInteger synonymCount = new AtomicInteger();

        List<Object[]> edgeBatch = new ArrayList<>(batchSize);
        List<Object[]> taxonBatch = new ArrayList<>(batchSize);
        List<Object[]> synonymBatch = new ArrayList<>(batchSize);

        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(zip.getInputStream(taxonEntry), StandardCharsets.UTF_8), 1 << 20)) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Taxon.tsv is empty");
            }
            Map<String, Integer> header = DwcaTsvHeaders.parse(headerLine);
            boolean headerAware = header.containsKey("taxonid") && header.containsKey("taxonrank");
            String line;
            long scanned = 0;
            while ((line = reader.readLine()) != null) {
                scanned++;
                if (line.isBlank()) {
                    continue;
                }
                String[] cols = line.split("\t", -1);
                if (!headerAware && cols.length < 21) {
                    continue;
                }
                String taxonId = taxonField(cols, header, headerAware, 0, "taxonid", "id");
                String parentId = taxonField(cols, header, headerAware, 1, "parentnameusageid", "parentid");
                String acceptedId = taxonField(cols, header, headerAware, 2, "acceptednameusageid", "acceptedid");
                String status = taxonField(cols, header, headerAware, 6, "taxonomicstatus", "status");
                String rankRaw = taxonField(cols, header, headerAware, 7, "taxonrank", "rank");
                String scientificName = taxonField(cols, header, headerAware, 8, "scientificname");
                String authorship = taxonField(cols, header, headerAware, 9, "scientificnameauthorship");
                String genericName = nz(taxonField(cols, header, headerAware, 11, "genericname"));
                String specificEpithet = nz(taxonField(cols, header, headerAware, 13, "specificepithet"));
                String infraspecificEpithet = nz(taxonField(cols, header, headerAware, 14, "infraspecificepithet"));
                String nameAccordingTo = taxonField(cols, header, headerAware, 16, "nameaccordingto");
                String namePublishedIn = taxonField(cols, header, headerAware, 17, "namepublishedin");
                String nomenclaturalCode = taxonField(cols, header, headerAware, 18, "nomenclaturalcode");
                String nomenclaturalStatus = taxonField(cols, header, headerAware, 19, "nomenclaturalstatus");
                String kingdom = taxonField(cols, header, headerAware, 20, "kingdom");
                if (!StringUtils.hasText(taxonId)) {
                    continue;
                }

                boolean kingdomOk = StringUtils.hasText(kingdom) && kingdoms.contains(kingdom);
                boolean isKingdomRow = rankRaw != null
                        && "kingdom".equalsIgnoreCase(rankRaw)
                        && kingdoms.contains(ColNameUtils.stripAuthorship(scientificName));
                boolean accepted = ColNameUtils.isAcceptedTaxonomicStatus(status);
                if (!accepted) {
                    if (collectSynonyms
                            && acceptedId != null
                            && StringUtils.hasText(scientificName)
                            && scientificName.length() <= 255) {
                        String canonical = ColNameUtils.stripAuthorship(scientificName);
                        if (StringUtils.hasText(canonical)) {
                            synonymBatch.add(new Object[] {taxonId, acceptedId, canonical});
                            synonymCount.incrementAndGet();
                            if (synonymBatch.size() >= batchSize) {
                                flushSynonymStage(synonymBatch);
                                synonymBatch = new ArrayList<>(batchSize);
                            }
                        }
                    }
                    continue;
                }
                if (!kingdomOk && !isKingdomRow) {
                    continue;
                }
                if (isKingdomRow && !StringUtils.hasText(kingdom)) {
                    kingdom = ColNameUtils.stripAuthorship(scientificName);
                }

                edgeBatch.add(new Object[] {taxonId, parentId});
                edgeCount.incrementAndGet();
                if (edgeBatch.size() >= batchSize) {
                    flushEdges(edgeBatch);
                    edgeBatch = new ArrayList<>(batchSize);
                }

                var rankOpt = ColNameUtils.mapRank(rankRaw, legacySeven);
                if (rankOpt.isEmpty()) {
                    continue;
                }
                TaxonRank rank = rankOpt.get();
                if (maxPerRank > 0 && counters.get(rank).get() >= maxPerRank) {
                    continue;
                }
                String canonical = ColNameUtils.canonicalName(
                        rank, scientificName, genericName, specificEpithet, infraspecificEpithet);
                if (!StringUtils.hasText(canonical) || canonical.length() > 255) {
                    continue;
                }
                if (!StringUtils.hasText(authorship) && StringUtils.hasText(scientificName)) {
                    String stripped = ColNameUtils.stripAuthorship(scientificName);
                    if (scientificName.trim().length() > stripped.length()) {
                        authorship = scientificName.trim().substring(stripped.length()).trim();
                        if (authorship.startsWith(",")) {
                            authorship = authorship.substring(1).trim();
                        }
                    }
                }
                String verbatim = StringUtils.hasText(scientificName) ? truncate(scientificName.trim(), 512) : null;
                taxonBatch.add(new Object[] {
                    taxonId,
                    parentId,
                    rank.name(),
                    canonical,
                    kingdom,
                    authorship,
                    rankRaw,
                    verbatim,
                    truncate(namePublishedIn, 512),
                    truncate(nameAccordingTo, 512),
                    truncate(nomenclaturalCode, 32),
                    truncate(nomenclaturalStatus, 64)
                });
                counters.get(rank).incrementAndGet();
                taxonCount.incrementAndGet();
                if (taxonBatch.size() >= batchSize) {
                    flushTaxonStage(taxonBatch);
                    taxonBatch = new ArrayList<>(batchSize);
                }
                if (scanned % BATCH_LOG_EVERY == 0) {
                    log.info("Staged scan rows={} taxa={} synonyms={}", scanned, taxonCount.get(), synonymCount.get());
                }
            }
        }
        if (!edgeBatch.isEmpty()) {
            flushEdges(edgeBatch);
        }
        if (!taxonBatch.isEmpty()) {
            flushTaxonStage(taxonBatch);
        }
        if (!synonymBatch.isEmpty()) {
            flushSynonymStage(synonymBatch);
        }
        Map<TaxonRank, Integer> byRank = new EnumMap<>(TaxonRank.class);
        for (TaxonRank rank : TaxonRank.values()) {
            byRank.put(rank, counters.get(rank).get());
        }
        return new StageCounts(taxonCount.get(), synonymCount.get(), edgeCount.get(), byRank);
    }

    private void flushEdges(List<Object[]> batch) {
        transactionTemplate.executeWithoutResult(status -> jdbcTemplate.batchUpdate(
                "INSERT IGNORE INTO import_col_edge (external_id, parent_external_id) VALUES (?, ?)", batch));
    }

    private void flushTaxonStage(List<Object[]> batch) {
        transactionTemplate.executeWithoutResult(status -> jdbcTemplate.batchUpdate(
                """
                INSERT IGNORE INTO import_col_taxon
                (external_id, parent_external_id, taxon_rank, scientific_name, kingdom,
                 scientific_name_authorship, taxon_rank_raw, scientific_name_verbatim,
                 name_published_in, name_according_to, nomenclatural_code, nomenclatural_status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                batch));
    }

    private void flushSynonymStage(List<Object[]> batch) {
        transactionTemplate.executeWithoutResult(status -> jdbcTemplate.batchUpdate(
                """
                INSERT IGNORE INTO import_col_synonym
                (synonym_external_id, accepted_external_id, scientific_name)
                VALUES (?, ?, ?)
                """,
                batch));
    }

    private void insertRankFromStaging(
            TaxonRank rank,
            Map<String, Long> externalToId,
            Map<Long, String> idToPath,
            Map<Long, TaxonRank> idToRank,
            Map<String, Long> parentNameToId,
            String jobKey,
            int batchSize) {
        AtomicInteger inserted = new AtomicInteger();
        String afterExternalId = "";
        Timestamp ts = Timestamp.from(Instant.now());
        while (true) {
            List<StagedTaxon> page = jdbcTemplate.query(
                    """
                    SELECT external_id, parent_external_id, taxon_rank, scientific_name, kingdom,
                           scientific_name_authorship, taxon_rank_raw, scientific_name_verbatim,
                           name_published_in, name_according_to, nomenclatural_code, nomenclatural_status
                    FROM import_col_taxon
                    WHERE taxon_rank = ? AND external_id > ?
                    ORDER BY external_id
                    LIMIT ?
                    """,
                    (rs, rowNum) -> new StagedTaxon(
                            rs.getString(1),
                            rs.getString(2),
                            TaxonRank.valueOf(rs.getString(3)),
                            rs.getString(4),
                            rs.getString(5),
                            rs.getString(6),
                            rs.getString(7),
                            rs.getString(8),
                            rs.getString(9),
                            rs.getString(10),
                            rs.getString(11),
                            rs.getString(12)),
                    rank.name(),
                    afterExternalId,
                    batchSize);
            if (page.isEmpty()) {
                break;
            }
            afterExternalId = page.getLast().externalId();
            List<PendingInsert> pending = new ArrayList<>();
            Set<String> pendingUniqKeys = new HashSet<>();
            Map<String, List<String>> pendingAliases = new HashMap<>();
            for (StagedTaxon row : page) {
                if (externalToId.containsKey(row.externalId())) {
                    continue;
                }
                Long parentDbId = resolveParentDbId(row.parentExternalId(), row.kingdom(), externalToId);
                String uniqKey = uniquenessKey(parentDbId, row.scientificName());
                Long existingId = parentNameToId.get(uniqKey);
                if (existingId != null) {
                    externalToId.put(row.externalId(), existingId);
                    continue;
                }
                if (!pendingUniqKeys.add(uniqKey)) {
                    pendingAliases.computeIfAbsent(uniqKey, key -> new ArrayList<>()).add(row.externalId());
                    continue;
                }
                pending.add(new PendingInsert(row, parentDbId, uniqKey));
            }
            if (!pending.isEmpty()) {
                flushInsertBatch(
                        pending, externalToId, idToPath, idToRank, parentNameToId, inserted, ts, jobKey, rank);
                attachPendingNameAliases(pendingAliases, parentNameToId, externalToId);
            }
        }
        checkpointRepository.upsert(jobKey, PHASE_RANK_PREFIX + rank.name(), inserted.get(), null, null);
        log.info("Inserted rank={} rows={}", rank, inserted.get());
    }

    private void flushInsertBatch(
            List<PendingInsert> pending,
            Map<String, Long> externalToId,
            Map<Long, String> idToPath,
            Map<Long, TaxonRank> idToRank,
            Map<String, Long> parentNameToId,
            AtomicInteger inserted,
            Timestamp ts,
            String jobKey,
            TaxonRank rank) {
        try {
            writePendingInserts(
                    pending, externalToId, idToPath, idToRank, parentNameToId, inserted, ts, jobKey, rank);
        } catch (BusinessException ex) {
            if (!isDuplicateParentName(ex)) {
                throw ex;
            }
            if (pending.size() == 1) {
                aliasDuplicateParentName(pending.getFirst(), externalToId, idToPath, idToRank, parentNameToId);
                return;
            }
            log.warn("Batch insert hit uk_taxon_parent_name, retrying {} rows individually", pending.size());
            for (PendingInsert item : pending) {
                try {
                    writePendingInserts(
                            List.of(item),
                            externalToId,
                            idToPath,
                            idToRank,
                            parentNameToId,
                            inserted,
                            ts,
                            jobKey,
                            rank);
                } catch (BusinessException rowEx) {
                    if (!isDuplicateParentName(rowEx)) {
                        throw rowEx;
                    }
                    aliasDuplicateParentName(item, externalToId, idToPath, idToRank, parentNameToId);
                }
            }
        }
    }

    private void writePendingInserts(
            List<PendingInsert> pending,
            Map<String, Long> externalToId,
            Map<Long, String> idToPath,
            Map<Long, TaxonRank> idToRank,
            Map<String, Long> parentNameToId,
            AtomicInteger inserted,
            Timestamp ts,
            String jobKey,
            TaxonRank rank) {
        transactionTemplate.executeWithoutResult(status -> {
            String insertSql =
                    """
                    INSERT INTO taxon
                    (parent_id, taxon_rank, scientific_name, materialized_path, child_count, is_accepted,
                     created_at, updated_at, created_by, external_source, external_id,
                     rank_order, taxon_rank_raw, scientific_name_authorship,
                     scientific_name_verbatim, name_published_in, name_according_to,
                     nomenclatural_code, nomenclatural_status)
                    VALUES (?, ?, ?, ?, 0, TRUE, ?, ?, 'col-import', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """;
            jdbcTemplate.execute((java.sql.Connection connection) -> {
                try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
                    for (PendingInsert item : pending) {
                        if (item.parentDbId() == null) {
                            ps.setNull(1, Types.BIGINT);
                        } else {
                            ps.setLong(1, item.parentDbId());
                        }
                        ps.setString(2, item.row().rank().name());
                        ps.setString(3, item.row().scientificName());
                        ps.setString(4, "/");
                        ps.setTimestamp(5, ts);
                        ps.setTimestamp(6, ts);
                        ps.setString(7, SOURCE_COL);
                        ps.setString(8, item.row().externalId());
                        ps.setInt(9, item.row().rank().getRankOrder());
                        ps.setString(10, item.row().rankRaw());
                        setNullable(ps, 11, item.row().authorship());
                        setNullable(ps, 12, item.row().verbatim());
                        setNullable(ps, 13, item.row().namePublishedIn());
                        setNullable(ps, 14, item.row().nameAccordingTo());
                        setNullable(ps, 15, item.row().nomenclaturalCode());
                        setNullable(ps, 16, item.row().nomenclaturalStatus());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                    Map<String, Long> insertedIds = loadIdsByExternal(
                            connection,
                            pending.stream().map(item -> item.row().externalId()).toList());
                    if (insertedIds.size() != pending.size()) {
                        throw new BusinessException(
                                ErrorCode.INTERNAL_ERROR,
                                "Inserted key count mismatch: expected "
                                        + pending.size()
                                        + " got "
                                        + insertedIds.size());
                    }
                    List<Object[]> pathUpdates = new ArrayList<>(pending.size());
                    for (PendingInsert item : pending) {
                        Long generatedId = insertedIds.get(item.row().externalId());
                        if (generatedId == null) {
                            throw new BusinessException(
                                    ErrorCode.INTERNAL_ERROR,
                                    "Missing inserted id for external_id=" + item.row().externalId());
                        }
                        long id = generatedId;
                        Long parentDbId = item.parentDbId();
                        String path = parentDbId == null
                                ? "/" + id + "/"
                                : requireParentPath(idToPath, parentDbId) + id + "/";
                        externalToId.put(item.row().externalId(), id);
                        idToPath.put(id, path);
                        idToRank.put(id, item.row().rank());
                        parentNameToId.put(item.uniqKey(), id);
                        Long simpleParentId = SimpleParentSupport.nearestLinnaeanAncestorId(
                                SimpleParentSupport.ancestorIds(path, id), idToRank);
                        pathUpdates.add(new Object[] {path, simpleParentId, id});
                    }
                    jdbcTemplate.batchUpdate(
                            "UPDATE taxon SET materialized_path = ?, simple_parent_id = ? WHERE id = ?", pathUpdates);
                } catch (SQLException ex) {
                    throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Batch insert taxa failed: " + ex.getMessage());
                }
                return null;
            });
            int total = inserted.addAndGet(pending.size());
            if (total / CHECKPOINT_EVERY > (total - pending.size()) / CHECKPOINT_EVERY) {
                checkpointRepository.upsert(jobKey, PHASE_RANK_PREFIX + rank.name(), total, null, null);
            }
            if (total / BATCH_LOG_EVERY > (total - pending.size()) / BATCH_LOG_EVERY) {
                log.info("Inserted {} taxa for rank {}", total, rank);
            }
        });
    }

    /**
     * 将同批内未落库的重复学名 CoL ID 映射到已插入节点，供子节点解析父级。
     */
    private static void attachPendingNameAliases(
            Map<String, List<String>> pendingAliases,
            Map<String, Long> parentNameToId,
            Map<String, Long> externalToId) {
        if (pendingAliases.isEmpty()) {
            return;
        }
        int aliased = 0;
        for (Map.Entry<String, List<String>> entry : pendingAliases.entrySet()) {
            Long id = parentNameToId.get(entry.getKey());
            if (id == null) {
                continue;
            }
            for (String extraExternalId : entry.getValue()) {
                externalToId.put(extraExternalId, id);
                aliased++;
            }
        }
        if (aliased > 0) {
            log.info("Collapsed {} same-parent duplicate scientific names in current insert batch", aliased);
        }
    }

    private Long resolveParentDbId(String parentExternalId, String kingdom, Map<String, Long> externalToId) {
        String cursor = parentExternalId;
        int guard = 0;
        while (cursor != null && guard++ < 64) {
            Long id = externalToId.get(cursor);
            if (id == null) {
                id = lookupExternalId(cursor);
                if (id != null) {
                    externalToId.put(cursor, id);
                }
            }
            if (id != null) {
                return id;
            }
            cursor = lookupEdgeParent(cursor);
        }
        if (StringUtils.hasText(kingdom)) {
            return kingdomIdCache.computeIfAbsent(kingdom.toLowerCase(Locale.ROOT), this::lookupKingdomId);
        }
        return null;
    }

    private static Map<String, Long> loadIdsByExternal(java.sql.Connection connection, List<String> externalIds)
            throws SQLException {
        if (externalIds.isEmpty()) {
            return Map.of();
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(externalIds.size(), "?"));
        String sql = "SELECT external_id, id FROM taxon WHERE external_source = ? AND external_id IN ("
                + placeholders
                + ")";
        Map<String, Long> ids = new HashMap<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, SOURCE_COL);
            for (int i = 0; i < externalIds.size(); i++) {
                ps.setString(i + 2, externalIds.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.put(rs.getString(1), rs.getLong(2));
                }
            }
        }
        return ids;
    }

    private void assertKingdomsPresent(String message) {
        Integer kingdoms = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM taxon WHERE taxon_rank = 'KINGDOM'", Integer.class);
        if (kingdoms == null || kingdoms == 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, message);
        }
    }

    private void validateImportedHierarchy() {
        assertKingdomsPresent("Import produced no kingdoms");
        Integer species = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM taxon WHERE taxon_rank = 'SPECIES'", Integer.class);
        Integer orphanSpecies = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM taxon
                WHERE taxon_rank = 'SPECIES' AND parent_id IS NULL
                """,
                Integer.class);
        if (species != null && species > 0 && orphanSpecies != null && orphanSpecies == species) {
            throw new BusinessException(
                    ErrorCode.INTERNAL_ERROR, "All species are root nodes; hierarchy is broken");
        }
        if (orphanSpecies != null && orphanSpecies > 0) {
            log.warn("Orphan species remain after rank insert count={}", orphanSpecies);
        }
    }

    private static String taxonField(
            String[] cols, Map<String, Integer> header, boolean headerAware, int fallbackIndex, String... names) {
        if (headerAware) {
            String value = DwcaTsvHeaders.col(cols, header, names);
            if (value != null) {
                return value;
            }
        }
        if (fallbackIndex >= 0 && fallbackIndex < cols.length) {
            return emptyToNull(cols[fallbackIndex]);
        }
        return null;
    }

    private static String nz(String value) {
        return value == null ? "" : value;
    }

    private Long lookupKingdomId(String kingdomLower) {
        List<Long> ids = jdbcTemplate.query(
                """
                SELECT id FROM taxon
                WHERE taxon_rank = 'KINGDOM' AND LOWER(scientific_name) = ?
                """,
                (rs, rowNum) -> rs.getLong(1),
                kingdomLower);
        return ids.isEmpty() ? null : ids.getFirst();
    }

    private Long lookupExternalId(String externalId) {
        List<Long> ids = jdbcTemplate.query(
                "SELECT id FROM taxon WHERE external_source = ? AND external_id = ?",
                (rs, rowNum) -> rs.getLong(1),
                SOURCE_COL,
                externalId);
        return ids.isEmpty() ? null : ids.getFirst();
    }

    private String lookupEdgeParent(String externalId) {
        List<String> parents = jdbcTemplate.query(
                "SELECT parent_external_id FROM import_col_edge WHERE external_id = ?",
                (rs, rowNum) -> rs.getString(1),
                externalId);
        return parents.isEmpty() ? null : parents.getFirst();
    }

    private int importSynonymsFromStaging(Map<String, Long> externalToId, int batchSize) {
        Set<String> existingExt = new HashSet<>();
        jdbcTemplate.query(
                "SELECT external_id FROM taxon_synonym WHERE external_source = ?",
                (org.springframework.jdbc.core.RowCallbackHandler) rs -> existingExt.add(rs.getString(1)),
                SOURCE_COL);
        AtomicInteger count = new AtomicInteger();
        String afterExternalId = "";
        while (true) {
            List<Object[]> page = jdbcTemplate.query(
                    """
                    SELECT synonym_external_id, accepted_external_id, scientific_name
                    FROM import_col_synonym
                    WHERE synonym_external_id > ?
                    ORDER BY synonym_external_id
                    LIMIT ?
                    """,
                    (rs, rowNum) -> new Object[] {rs.getString(1), rs.getString(2), rs.getString(3)},
                    afterExternalId,
                    batchSize);
            if (page.isEmpty()) {
                break;
            }
            afterExternalId = (String) page.getLast()[0];
            List<Object[]> inserts = new ArrayList<>();
            for (Object[] row : page) {
                String synExt = (String) row[0];
                String acceptedExt = (String) row[1];
                String name = (String) row[2];
                if (existingExt.contains(synExt)) {
                    continue;
                }
                Long taxonId = externalToId.get(acceptedExt);
                if (taxonId == null) {
                    taxonId = lookupExternalId(acceptedExt);
                    if (taxonId != null) {
                        externalToId.put(acceptedExt, taxonId);
                    }
                }
                if (taxonId == null) {
                    continue;
                }
                existingExt.add(synExt);
                inserts.add(new Object[] {taxonId, name, SOURCE_COL, synExt});
            }
            if (!inserts.isEmpty()) {
                List<Object[]> finalInserts = inserts;
                transactionTemplate.executeWithoutResult(status -> jdbcTemplate.batchUpdate(
                        """
                        INSERT INTO taxon_synonym (taxon_id, scientific_name, external_source, external_id)
                        VALUES (?, ?, ?, ?)
                        """,
                        finalInserts));
                count.addAndGet(inserts.size());
            }
        }
        log.info("Imported synonym rows={}", count.get());
        return count.get();
    }

    private static String requireParentPath(Map<Long, String> idToPath, Long parentDbId) {
        String parentPath = idToPath.get(parentDbId);
        if (parentPath == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Missing parent path for id=" + parentDbId);
        }
        return parentPath;
    }

    private void preloadExistingExternalIds(Map<String, Long> externalToId) {
        jdbcTemplate.query(
                "SELECT external_id, id FROM taxon WHERE external_source = ? AND external_id IS NOT NULL",
                (org.springframework.jdbc.core.RowCallbackHandler)
                        rs -> externalToId.put(rs.getString(1), rs.getLong(2)),
                SOURCE_COL);
        log.info("Preloaded existing COL external ids={}", externalToId.size());
    }

    private void preloadExistingPaths(Map<Long, String> idToPath) {
        jdbcTemplate.query(
                "SELECT id, materialized_path FROM taxon",
                (org.springframework.jdbc.core.RowCallbackHandler)
                        rs -> idToPath.put(rs.getLong(1), rs.getString(2)));
    }

    private void preloadExistingRanks(Map<Long, TaxonRank> idToRank) {
        jdbcTemplate.query(
                "SELECT id, taxon_rank FROM taxon",
                (org.springframework.jdbc.core.RowCallbackHandler) rs -> {
                    try {
                        idToRank.put(rs.getLong(1), TaxonRank.valueOf(rs.getString(2)));
                    } catch (Exception ignored) {
                        // skip unknown
                    }
                });
    }

    private void preloadExistingParentNames(Map<String, Long> parentNameToId) {
        jdbcTemplate.query(
                "SELECT parent_id, scientific_name, id FROM taxon",
                (org.springframework.jdbc.core.RowCallbackHandler) rs -> {
                    Long parentId = rs.getObject(1) == null ? null : rs.getLong(1);
                    parentNameToId.putIfAbsent(uniquenessKey(parentId, rs.getString(2)), rs.getLong(3));
                });
    }

    private static String uniquenessKey(Long parentId, String scientificName) {
        String parentPart = parentId == null ? "ROOT" : parentId.toString();
        return parentPart + "|" + ColNameUtils.foldForParentNameUnique(scientificName);
    }

    private static boolean isDuplicateParentName(Throwable ex) {
        Throwable cursor = ex;
        while (cursor != null) {
            String message = cursor.getMessage();
            if (message != null && message.toLowerCase(Locale.ROOT).contains("uk_taxon_parent_name")) {
                return true;
            }
            cursor = cursor.getCause();
        }
        return false;
    }

    private void aliasDuplicateParentName(
            PendingInsert item,
            Map<String, Long> externalToId,
            Map<Long, String> idToPath,
            Map<Long, TaxonRank> idToRank,
            Map<String, Long> parentNameToId) {
        Long existingId = lookupIdByParentName(item.parentDbId(), item.row().scientificName());
        if (existingId == null) {
            throw new BusinessException(
                    ErrorCode.INTERNAL_ERROR,
                    "Duplicate parent name but no existing row for " + item.row().scientificName());
        }
        externalToId.put(item.row().externalId(), existingId);
        parentNameToId.put(item.uniqKey(), existingId);
        if (!idToPath.containsKey(existingId) || !idToRank.containsKey(existingId)) {
            jdbcTemplate.query(
                    "SELECT materialized_path, taxon_rank FROM taxon WHERE id = ?",
                    (org.springframework.jdbc.core.RowCallbackHandler) rs -> {
                        idToPath.putIfAbsent(existingId, rs.getString(1));
                        try {
                            idToRank.putIfAbsent(existingId, TaxonRank.valueOf(rs.getString(2)));
                        } catch (Exception ignored) {
                            // skip unknown
                        }
                    },
                    existingId);
        }
        log.info(
                "Aliased duplicate scientific name {} under parent {} to taxon {}",
                item.row().scientificName(),
                item.parentDbId(),
                existingId);
    }

    private Long lookupIdByParentName(Long parentId, String scientificName) {
        List<Long> ids = parentId == null
                ? jdbcTemplate.query(
                        "SELECT id FROM taxon WHERE parent_id IS NULL AND scientific_name = ? LIMIT 1",
                        (rs, rowNum) -> rs.getLong(1),
                        scientificName)
                : jdbcTemplate.query(
                        "SELECT id FROM taxon WHERE parent_id = ? AND scientific_name = ? LIMIT 1",
                        (rs, rowNum) -> rs.getLong(1),
                        parentId,
                        scientificName);
        return ids.isEmpty() ? null : ids.getFirst();
    }

    private int importVernaculars(InputStream in, Map<String, Long> externalToId, int batchSize) throws IOException {
        Map<String, Map<String, VernacularPick>> best = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8), 1 << 20)) {
            String headerLine = reader.readLine();
            Map<String, Integer> header = DwcaTsvHeaders.parse(headerLine);
            boolean headerAware = header.containsKey("vernacularname") || header.containsKey("language");
            String line;
            while ((line = reader.readLine()) != null) {
                String[] cols = line.split("\t", -1);
                String taxonId;
                String language;
                String name;
                boolean preferred;
                if (headerAware) {
                    taxonId = DwcaTsvHeaders.col(cols, header, "taxonid", "id", "coreid");
                    language = DwcaTsvHeaders.col(cols, header, "language", "lang");
                    name = DwcaTsvHeaders.col(cols, header, "vernacularname");
                    preferred = DwcaTsvHeaders.truthy(
                            DwcaTsvHeaders.col(cols, header, "ispreferredname", "preferred", "preferredname"));
                } else {
                    if (cols.length < 3) {
                        continue;
                    }
                    taxonId = cols[0];
                    language = cols[1];
                    name = cols[2] == null ? "" : cols[2].trim();
                    preferred = cols.length > 3 && DwcaTsvHeaders.truthy(cols[3]);
                }
                if (taxonId == null || !externalToId.containsKey(taxonId)) {
                    continue;
                }
                var localeOpt = ColNameUtils.mapLocale(language);
                if (localeOpt.isEmpty()) {
                    continue;
                }
                if (!StringUtils.hasText(name) || name.length() > 255) {
                    continue;
                }
                best.computeIfAbsent(taxonId, k -> new HashMap<>())
                        .merge(localeOpt.get(), new VernacularPick(name.trim(), preferred), (a, b) -> {
                            if (b.preferred() && !a.preferred()) {
                                return b;
                            }
                            if (a.preferred() && !b.preferred()) {
                                return a;
                            }
                            return a;
                        });
            }
        }

        Set<String> existingKeys = new HashSet<>();
        jdbcTemplate.query(
                "SELECT taxon_id, locale FROM taxon_i18n",
                (org.springframework.jdbc.core.RowCallbackHandler)
                        rs -> existingKeys.add(rs.getLong(1) + "|" + rs.getString(2)));

        List<Object[]> inserts = new ArrayList<>();
        List<Object[]> updates = new ArrayList<>();
        AtomicInteger count = new AtomicInteger();
        for (Map.Entry<String, Map<String, VernacularPick>> e : best.entrySet()) {
            Long taxonId = externalToId.get(e.getKey());
            for (Map.Entry<String, VernacularPick> loc : e.getValue().entrySet()) {
                String key = taxonId + "|" + loc.getKey();
                VernacularPick pick = loc.getValue();
                if (existingKeys.contains(key)) {
                    updates.add(new Object[] {pick.name(), pick.preferred(), taxonId, loc.getKey()});
                } else {
                    inserts.add(new Object[] {taxonId, loc.getKey(), pick.name(), pick.preferred()});
                    existingKeys.add(key);
                }
                count.incrementAndGet();
                if (inserts.size() >= batchSize) {
                    flushVernacularInserts(inserts);
                    inserts = new ArrayList<>();
                }
                if (updates.size() >= batchSize) {
                    flushVernacularUpdates(updates);
                    updates = new ArrayList<>();
                }
            }
        }
        if (!inserts.isEmpty()) {
            flushVernacularInserts(inserts);
        }
        if (!updates.isEmpty()) {
            flushVernacularUpdates(updates);
        }
        log.info("Imported vernacular rows={}", count.get());
        return count.get();
    }

    private void flushVernacularInserts(List<Object[]> inserts) {
        transactionTemplate.executeWithoutResult(status -> jdbcTemplate.batchUpdate(
                """
                INSERT INTO taxon_i18n (taxon_id, locale, common_name, preferred)
                VALUES (?, ?, ?, ?)
                """,
                inserts));
    }

    private void flushVernacularUpdates(List<Object[]> updates) {
        transactionTemplate.executeWithoutResult(status -> jdbcTemplate.batchUpdate(
                """
                UPDATE taxon_i18n SET common_name = ?, preferred = ?
                WHERE taxon_id = ? AND locale = ?
                """,
                updates));
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
                    if (batches.size() >= 500) {
                        jdbcTemplate.batchUpdate("UPDATE taxon SET child_count = ? WHERE id = ?", batches);
                        batches.clear();
                    }
                });
        if (!batches.isEmpty()) {
            jdbcTemplate.batchUpdate("UPDATE taxon SET child_count = ? WHERE id = ?", batches);
        }
    }

    private void clearTaxonData() {
        log.info("Clearing existing taxon/media/i18n/synonym/distribution data before import");
        if (isMySql()) {
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS=0");
            try {
                jdbcTemplate.execute("TRUNCATE TABLE taxon_media");
                jdbcTemplate.execute("TRUNCATE TABLE taxon_distribution");
                jdbcTemplate.execute("TRUNCATE TABLE taxon_i18n");
                jdbcTemplate.execute("TRUNCATE TABLE taxon_synonym");
                jdbcTemplate.execute("TRUNCATE TABLE taxon");
            } finally {
                jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS=1");
            }
            return;
        }
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        jdbcTemplate.update("DELETE FROM taxon_media");
        jdbcTemplate.update("DELETE FROM taxon_distribution");
        jdbcTemplate.update("DELETE FROM taxon_i18n");
        jdbcTemplate.update("DELETE FROM taxon_synonym");
        jdbcTemplate.update("UPDATE taxon SET simple_parent_id = NULL");
        jdbcTemplate.update("UPDATE taxon SET parent_id = NULL");
        jdbcTemplate.update("DELETE FROM taxon");
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
    }

    private void clearStaging() {
        if (isMySql()) {
            jdbcTemplate.execute("TRUNCATE TABLE import_col_synonym");
            jdbcTemplate.execute("TRUNCATE TABLE import_col_taxon");
            jdbcTemplate.execute("TRUNCATE TABLE import_col_edge");
            return;
        }
        jdbcTemplate.update("DELETE FROM import_col_synonym");
        jdbcTemplate.update("DELETE FROM import_col_taxon");
        jdbcTemplate.update("DELETE FROM import_col_edge");
    }

    private boolean isMySql() {
        if (mysql != null) {
            return mysql;
        }
        try {
            var dataSource = jdbcTemplate.getDataSource();
            if (dataSource == null) {
                mysql = false;
                return false;
            }
            try (var connection = dataSource.getConnection()) {
                String product = connection.getMetaData().getDatabaseProductName();
                mysql = product != null && product.toLowerCase(Locale.ROOT).contains("mysql");
            }
        } catch (SQLException ex) {
            mysql = false;
        }
        return mysql;
    }

    private boolean suspendMysqlFulltextIndexes() {
        if (!isMySql()) {
            return false;
        }
        boolean dropped = false;
        dropped |= dropMysqlIndexIfPresent("taxon", "ft_taxon_scientific_name");
        dropped |= dropMysqlIndexIfPresent("taxon_i18n", "ft_taxon_i18n_common_name");
        dropped |= dropMysqlIndexIfPresent("taxon_synonym", "ft_taxon_synonym_name");
        if (dropped) {
            log.info("Suspended MySQL FULLTEXT indexes for import");
        }
        return dropped;
    }

    private void restoreMysqlFulltextIndexes() {
        if (!isMySql()) {
            return;
        }
        MysqlFulltextIndexSupport.restoreQuietly(
                () -> {
                    addMysqlFulltextIfMissing("taxon", "ft_taxon_scientific_name", "scientific_name");
                    addMysqlFulltextIfMissing("taxon_i18n", "ft_taxon_i18n_common_name", "common_name");
                    addMysqlFulltextIfMissing("taxon_synonym", "ft_taxon_synonym_name", "scientific_name");
                    log.info("Restored MySQL FULLTEXT indexes after import");
                },
                log);
    }

    private boolean dropMysqlIndexIfPresent(String table, String indexName) {
        if (mysqlIndexCount(table, indexName) == 0) {
            return false;
        }
        jdbcTemplate.execute("ALTER TABLE " + table + " DROP INDEX " + indexName);
        return true;
    }

    private void addMysqlFulltextIfMissing(String table, String indexName, String column) {
        if (mysqlIndexCount(table, indexName) > 0) {
            return;
        }
        try {
            jdbcTemplate.execute("ALTER TABLE " + table + " ADD FULLTEXT INDEX " + indexName + " (" + column + ")");
        } catch (RuntimeException ex) {
            log.warn("Failed to add FULLTEXT {} on {}; search will use LIKE fallback", indexName, table, ex);
        }
    }

    private int mysqlIndexCount(String table, String indexName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM information_schema.statistics
                WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ?
                """,
                Integer.class,
                table,
                indexName);
        return count == null ? 0 : count;
    }

    private boolean stagingHasTaxa() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM import_col_taxon", Integer.class);
        return count != null && count > 0;
    }

    private void fillRankCountersFromStaging(Map<TaxonRank, AtomicInteger> rankCounters) {
        jdbcTemplate.query(
                "SELECT taxon_rank, COUNT(*) FROM import_col_taxon GROUP BY taxon_rank",
                (org.springframework.jdbc.core.RowCallbackHandler) rs -> {
                    TaxonRank rank = TaxonRank.valueOf(rs.getString(1));
                    rankCounters.get(rank).set(rs.getInt(2));
                });
    }

    private static int phaseOrder(String phase) {
        if (PHASE_STAGE.equals(phase)) {
            return 1;
        }
        if (phase != null && phase.startsWith(PHASE_RANK_PREFIX)) {
            try {
                TaxonRank rank = TaxonRank.valueOf(phase.substring(PHASE_RANK_PREFIX.length()));
                return 10 + rank.ordinal();
            } catch (Exception ignored) {
                return 10;
            }
        }
        return switch (phase) {
            case PHASE_VERNACULARS -> 30;
            case PHASE_SYNONYMS -> 40;
            case PHASE_DESCRIPTIONS -> 45;
            case PHASE_DISTRIBUTIONS -> 46;
            case PHASE_MEDIA -> 47;
            case PHASE_DATASET_META -> 48;
            case PHASE_COUNTS -> 50;
            default -> 0;
        };
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

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        String v = value.trim();
        if (!StringUtils.hasText(v)) {
            return null;
        }
        return v.length() <= max ? v : v.substring(0, max);
    }

    private static void setNullable(PreparedStatement ps, int index, String value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.VARCHAR);
        } else {
            ps.setString(index, value);
        }
    }

    private static ZipEntry firstPresent(ZipFile zip, String... names) {
        for (String name : names) {
            ZipEntry entry = zip.getEntry(name);
            if (entry != null) {
                return entry;
            }
        }
        return null;
    }

    private static Map<String, Integer> toIntMap(Map<TaxonRank, AtomicInteger> counters) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (TaxonRank rank : TaxonRank.values()) {
            map.put(rank.name().toLowerCase(Locale.ROOT), counters.get(rank).get());
        }
        return map;
    }

    private record StagedTaxon(
            String externalId,
            String parentExternalId,
            TaxonRank rank,
            String scientificName,
            String kingdom,
            String authorship,
            String rankRaw,
            String verbatim,
            String namePublishedIn,
            String nameAccordingTo,
            String nomenclaturalCode,
            String nomenclaturalStatus) {
    }

    private record PendingInsert(StagedTaxon row, Long parentDbId, String uniqKey) {
    }

    private record StageCounts(
            int taxonCount, int synonymCount, int edgeCount, Map<TaxonRank, Integer> byRank) {
    }

    private record VernacularPick(String name, boolean preferred) {
    }

    public record ImportStats(
            int taxonCount,
            int vernacularCount,
            int synonymCount,
            int descriptionCount,
            int distributionCount,
            int mediaCount,
            Map<String, Integer> byRank) {
    }
}
