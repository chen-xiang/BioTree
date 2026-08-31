/**
 * locale 回退与前缀搜索行为测试。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
package com.chenxiang.biotree.api.taxon;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TaxonLocaleAndSearchTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void detailShouldFallbackCommonNameToEnglish() throws Exception {
        jdbcTemplate.update(
                """
                INSERT INTO taxon (id, parent_id, taxon_rank, scientific_name, materialized_path, child_count, is_accepted)
                VALUES (900, 1, 'PHYLUM', 'FallbackPhylum', '/1/900/', 0, 1)
                """);
        jdbcTemplate.update(
                """
                INSERT INTO taxon_i18n (taxon_id, locale, common_name)
                VALUES (900, 'en', 'Fallback English Name')
                """);

        mockMvc.perform(get("/api/taxa/{id}", 900).param("locale", "zh-CN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scientificName").value("FallbackPhylum"))
                .andExpect(jsonPath("$.data.commonName").value("Fallback English Name"))
                .andExpect(jsonPath("$.data.locale").value("en"));
    }

    @Test
    void searchShouldMatchPrefixScientificName() throws Exception {
        mockMvc.perform(get("/api/taxa/search").param("q", "Homo").param("locale", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.items[0].scientificName").value(Matchers.containsString("Homo")));
    }
}
