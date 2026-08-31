/**
 * CoL DwC-A 导入集成测试（使用精简夹具）。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
package com.chenxiang.biotree.infrastructure.importdata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ColDwcaImporterTest {

    @Autowired
    private ColDwcaImporter importer;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @TempDir
    Path tempDir;

    @Test
    void importFixtureShouldCreateHierarchyAndVernaculars() throws Exception {
        Path zip = tempDir.resolve("fixture_dwca.zip");
        writeFixture(zip);

        ImportProperties properties = new ImportProperties();
        properties.setReplace(true);
        properties.setResume(false);
        properties.setImportVernaculars(true);
        properties.setImportSynonyms(true);
        properties.setCommitBatchSize(50);
        properties.setKingdoms(List.of("Animalia", "Plantae"));
        properties.setRankMode("full");

        ColDwcaImporter.ImportStats stats = importer.importArchive(zip, properties);
        assertTrue(stats.taxonCount() >= 10);
        assertTrue(stats.vernacularCount() >= 2);
        assertTrue(stats.synonymCount() >= 1);

        Integer kingdoms = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM taxon WHERE taxon_rank = 'KINGDOM'", Integer.class);
        assertEquals(2, kingdoms);

        Integer subgenus = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM taxon WHERE taxon_rank = 'SUBGENUS'", Integer.class);
        assertEquals(1, subgenus);

        Integer human = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM taxon t
                JOIN taxon_i18n i ON i.taxon_id = t.id
                WHERE t.scientific_name = 'Homo sapiens' AND i.locale = 'zh-CN' AND i.common_name = '智人'
                """,
                Integer.class);
        assertEquals(1, human);
    }

    private static void writeFixture(Path zip) throws IOException {
        String taxonHeader =
                "dwc:taxonID\tdwc:parentNameUsageID\tdwc:acceptedNameUsageID\ta\tb\tc\tdwc:taxonomicStatus\tdwc:taxonRank\tdwc:scientificName\tdwc:scientificNameAuthorship\tcol:notho\tdwc:genericName\tdwc:infragenericEpithet\tdwc:specificEpithet\tdwc:infraspecificEpithet\tdwc:cultivarEpithet\tdwc:nameAccordingTo\tdwc:namePublishedIn\tdwc:nomenclaturalCode\tdwc:nomenclaturalStatus\tdwc:kingdom\n";
        StringBuilder taxa = new StringBuilder(taxonHeader);
        taxa.append("N\t\t\t\t\t\taccepted\tkingdom\tAnimalia\t\t\t\t\t\t\t\t\t\t\t\tAnimalia\n");
        taxa.append("P\t\t\t\t\t\taccepted\tkingdom\tPlantae\t\t\t\t\t\t\t\t\t\t\t\tPlantae\n");
        taxa.append("PH1\tN\t\t\t\t\taccepted\tphylum\tChordata\t\t\t\t\t\t\t\t\t\t\t\tAnimalia\n");
        taxa.append("CL1\tPH1\t\t\t\t\taccepted\tclass\tMammalia\t\t\t\t\t\t\t\t\t\t\t\tAnimalia\n");
        taxa.append("OR1\tCL1\t\t\t\t\taccepted\torder\tPrimates\t\t\t\t\t\t\t\t\t\t\t\tAnimalia\n");
        taxa.append("FA1\tOR1\t\t\t\t\taccepted\tfamily\tHominidae\t\t\t\t\t\t\t\t\t\t\t\tAnimalia\n");
        taxa.append("GE1\tFA1\t\t\t\t\taccepted\tgenus\tHomo Linnaeus, 1758\tLinnaeus, 1758\t\tHomo\t\t\t\t\t\t\t\t\tAnimalia\n");
        taxa.append(
                "SG1\tGE1\t\t\t\t\taccepted\tsubgenus\tHomo\t\t\tHomo\t\t\t\t\t\t\t\t\tAnimalia\n");
        taxa.append(
                "SP1\tSG1\t\t\t\t\taccepted\tspecies\tHomo sapiens Linnaeus, 1758\tLinnaeus, 1758\t\tHomo\t\tsapiens\t\t\t\t\t\t\tAnimalia\n");
        taxa.append(
                "SSP1\tSP1\t\t\t\t\taccepted\tsubspecies\tHomo sapiens sapiens\t\t\tHomo\t\tsapiens\tsapiens\t\t\t\t\t\tAnimalia\n");
        taxa.append(
                "SP1S\tGE1\tSP1\t\t\t\tsynonym\tspecies\tHomo sapien\t\t\tHomo\t\tsapien\t\t\t\t\t\t\tAnimalia\n");
        taxa.append("PH2\tP\t\t\t\t\taccepted\tphylum\tMagnoliophyta\t\t\t\t\t\t\t\t\t\t\t\tPlantae\n");

        String vernacular =
                """
                dwc:taxonID\tdcterms:language\tdwc:vernacularName\tclb:merged
                SP1\tzho\t智人\tfalse
                SP1\teng\tHuman\tfalse
                N\teng\tAnimals\tfalse
                """;

        try (OutputStream out = Files.newOutputStream(zip);
                ZipOutputStream zipOut = new ZipOutputStream(out)) {
            zipOut.putNextEntry(new ZipEntry("Taxon.tsv"));
            zipOut.write(taxa.toString().getBytes(StandardCharsets.UTF_8));
            zipOut.closeEntry();
            zipOut.putNextEntry(new ZipEntry("VernacularName.tsv"));
            zipOut.write(vernacular.getBytes(StandardCharsets.UTF_8));
            zipOut.closeEntry();
        }
    }
}
