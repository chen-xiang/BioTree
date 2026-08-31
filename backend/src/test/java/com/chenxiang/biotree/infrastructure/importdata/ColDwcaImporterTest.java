/**
 * CoL DwC-A 导入集成测试（使用精简夹具）。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 * Updated: 2026-08-31 DwC 描述/分布/媒体与命名学字段
 * Updated: 2026-09-01 覆盖同父同名属在同一批次中的合并
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
    void importFixtureShouldCreateHierarchyAndDwcaExtras() throws Exception {
        Path zip = tempDir.resolve("fixture_dwca.zip");
        writeFixture(zip);

        ImportProperties properties = new ImportProperties();
        properties.setReplace(true);
        properties.setResume(false);
        properties.setImportVernaculars(true);
        properties.setImportSynonyms(true);
        properties.setImportDescriptions(true);
        properties.setImportDistributions(true);
        properties.setImportMedia(true);
        properties.setCommitBatchSize(50);
        properties.setKingdoms(List.of("Animalia", "Plantae"));
        properties.setRankMode("full");

        ColDwcaImporter.ImportStats stats = importer.importArchive(zip, properties);
        assertTrue(stats.taxonCount() >= 10);
        assertTrue(stats.vernacularCount() >= 2);
        assertTrue(stats.synonymCount() >= 1);
        assertTrue(stats.descriptionCount() >= 1);
        assertTrue(stats.distributionCount() >= 1);
        assertTrue(stats.mediaCount() >= 1);

        Integer kingdoms = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM taxon WHERE taxon_rank = 'KINGDOM'", Integer.class);
        assertEquals(2, kingdoms);

        Integer subgenus = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM taxon WHERE taxon_rank = 'SUBGENUS'", Integer.class);
        assertEquals(1, subgenus);

        String ssp = jdbcTemplate.queryForObject(
                "SELECT scientific_name FROM taxon WHERE taxon_rank = 'SUBSPECIES'", String.class);
        assertEquals("Homo sapiens sapiens", ssp);

        Integer published = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM taxon
                WHERE scientific_name = 'Homo sapiens' AND name_published_in IS NOT NULL
                """,
                Integer.class);
        assertEquals(1, published);

        Integer human = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM taxon t
                JOIN taxon_i18n i ON i.taxon_id = t.id
                WHERE t.scientific_name = 'Homo sapiens' AND i.locale = 'zh-CN' AND i.common_name = '智人'
                """,
                Integer.class);
        assertEquals(1, human);

        Integer desc = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM taxon_i18n
                WHERE locale = 'en' AND description LIKE '%bipedal%'
                """,
                Integer.class);
        assertEquals(1, desc);

        Integer dist = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM taxon_distribution", Integer.class);
        assertTrue(dist != null && dist >= 1);

        Integer media = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM taxon_media WHERE source_url IS NOT NULL", Integer.class);
        assertTrue(media != null && media >= 1);

        Integer meta = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM import_dataset_meta WHERE source_key = 'col'", Integer.class);
        assertEquals(1, meta);
    }

    @Test
    void sameParentDuplicateScientificNameShouldCollapseInBatch() throws Exception {
        Path zip = tempDir.resolve("duplicate_genus_dwca.zip");
        writeDuplicateGenusFixture(zip);

        ImportProperties properties = new ImportProperties();
        properties.setReplace(true);
        properties.setResume(false);
        properties.setImportVernaculars(false);
        properties.setImportSynonyms(false);
        properties.setImportDescriptions(false);
        properties.setImportDistributions(false);
        properties.setImportMedia(false);
        properties.setCommitBatchSize(50);
        properties.setKingdoms(List.of("Animalia"));
        properties.setRankMode("full");

        importer.importArchive(zip, properties);

        Integer genera = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM taxon
                WHERE taxon_rank = 'GENUS' AND scientific_name = 'Receptaculites'
                """,
                Integer.class);
        assertEquals(1, genera);

        Integer species = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM taxon
                WHERE taxon_rank = 'SPECIES' AND scientific_name = 'Receptaculites neptuni'
                """,
                Integer.class);
        assertEquals(1, species);
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
                "SP1\tSG1\t\t\t\t\taccepted\tspecies\tHomo sapiens Linnaeus, 1758\tLinnaeus, 1758\t\tHomo\t\tsapiens\t\t\tITIS\tSyst. Nat.\tICZN\t\tAnimalia\n");
        taxa.append(
                "SSP1\tSP1\t\t\t\t\taccepted\tsubspecies\tHomo sapiens sapiens\t\t\tHomo\t\tsapiens\tsapiens\t\t\t\t\t\tAnimalia\n");
        taxa.append(
                "SP1S\tGE1\tSP1\t\t\t\tsynonym\tspecies\tHomo sapien\t\t\tHomo\t\tsapien\t\t\t\t\t\t\tAnimalia\n");
        taxa.append("PH2\tP\t\t\t\t\taccepted\tphylum\tMagnoliophyta\t\t\t\t\t\t\t\t\t\t\t\tPlantae\n");

        String vernacular =
                """
                dwc:taxonID\tdcterms:language\tdwc:vernacularName\tdwc:isPreferredName
                SP1\tzho\t智人\ttrue
                SP1\teng\tHuman\ttrue
                SP1\tjpn\tヒト\tfalse
                N\teng\tAnimals\ttrue
                """;

        String description =
                """
                taxonID\tdescription\tlanguage\ttype
                SP1\tA bipedal primate.\teng\tgeneral
                """;

        String distribution =
                """
                taxonID\tcountryCode\tlocality\testablishmentMeans
                SP1\tCN\tEast Asia\tnative
                """;

        String media =
                """
                taxonID\tidentifier\tlicense\tcreator\ttitle\tformat
                SP1\thttps://example.com/homo.jpg\tCC-BY\tExample\tHuman\timage/jpeg
                """;

        String eml =
                """
                <?xml version="1.0"?>
                <eml:eml xmlns:eml="eml://ecoinformatics.org/eml-2.1.1">
                  <dataset>
                    <title>Catalogue of Life Test Fixture</title>
                    <pubDate>2026-08-01</pubDate>
                  </dataset>
                </eml:eml>
                """;

        try (OutputStream out = Files.newOutputStream(zip);
                ZipOutputStream zipOut = new ZipOutputStream(out)) {
            zipOut.putNextEntry(new ZipEntry("Taxon.tsv"));
            zipOut.write(taxa.toString().getBytes(StandardCharsets.UTF_8));
            zipOut.closeEntry();
            zipOut.putNextEntry(new ZipEntry("VernacularName.tsv"));
            zipOut.write(vernacular.getBytes(StandardCharsets.UTF_8));
            zipOut.closeEntry();
            zipOut.putNextEntry(new ZipEntry("Description.tsv"));
            zipOut.write(description.getBytes(StandardCharsets.UTF_8));
            zipOut.closeEntry();
            zipOut.putNextEntry(new ZipEntry("Distribution.tsv"));
            zipOut.write(distribution.getBytes(StandardCharsets.UTF_8));
            zipOut.closeEntry();
            zipOut.putNextEntry(new ZipEntry("Media.tsv"));
            zipOut.write(media.getBytes(StandardCharsets.UTF_8));
            zipOut.closeEntry();
            zipOut.putNextEntry(new ZipEntry("eml.xml"));
            zipOut.write(eml.getBytes(StandardCharsets.UTF_8));
            zipOut.closeEntry();
        }
    }

    private static void writeDuplicateGenusFixture(Path zip) throws IOException {
        String taxonHeader =
                "dwc:taxonID\tdwc:parentNameUsageID\tdwc:acceptedNameUsageID\ta\tb\tc\tdwc:taxonomicStatus\tdwc:taxonRank\tdwc:scientificName\tdwc:scientificNameAuthorship\tcol:notho\tdwc:genericName\tdwc:infragenericEpithet\tdwc:specificEpithet\tdwc:infraspecificEpithet\tdwc:cultivarEpithet\tdwc:nameAccordingTo\tdwc:namePublishedIn\tdwc:nomenclaturalCode\tdwc:nomenclaturalStatus\tdwc:kingdom\n";
        String taxa = taxonHeader
                + "N\t\t\t\t\t\taccepted\tkingdom\tAnimalia\t\t\t\t\t\t\t\t\t\t\t\tAnimalia\n"
                + "PH1\tN\t\t\t\t\taccepted\tphylum\tPorifera\t\t\t\t\t\t\t\t\t\t\t\tAnimalia\n"
                + "CL1\tPH1\t\t\t\t\taccepted\tclass\tReceptaculitida\t\t\t\t\t\t\t\t\t\t\t\tAnimalia\n"
                + "OR1\tCL1\t\t\t\t\taccepted\torder\tReceptaculitales\t\t\t\t\t\t\t\t\t\t\t\tAnimalia\n"
                + "FA1\tOR1\t\t\t\t\taccepted\tfamily\tReceptaculitidae\t\t\t\t\t\t\t\t\t\t\t\tAnimalia\n"
                + "GE1\tFA1\t\t\t\t\taccepted\tgenus\tReceptaculites\tDefrance, 1827\t\tReceptaculites\t\t\t\t\t\t\t\t\tAnimalia\n"
                + "GE2\tFA1\t\t\t\t\taccepted\tgenus\tReceptaculites\tBlumenbach, 1805\t\tReceptaculites\t\t\t\t\t\t\t\t\tAnimalia\n"
                + "SP1\tGE2\t\t\t\t\taccepted\tspecies\tReceptaculites neptuni\t\t\tReceptaculites\t\tneptuni\t\t\t\t\t\t\tAnimalia\n";
        try (OutputStream out = Files.newOutputStream(zip);
                ZipOutputStream zipOut = new ZipOutputStream(out)) {
            zipOut.putNextEntry(new ZipEntry("Taxon.tsv"));
            zipOut.write(taxa.getBytes(StandardCharsets.UTF_8));
            zipOut.closeEntry();
        }
    }
}
