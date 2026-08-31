/**
 * 分类公开 API 集成测试。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
package com.chenxiang.biotree.api.taxon;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaxonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldListRootKingdoms() throws Exception {
        mockMvc.perform(get("/api/taxa/children").param("locale", "zh-CN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.items[0].scientificName").exists());
    }

    @Test
    void shouldGetDetailWithBreadcrumbs() throws Exception {
        mockMvc.perform(get("/api/taxa/{id}", 8).param("locale", "zh-CN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scientificName").value("Homo sapiens"))
                .andExpect(jsonPath("$.data.commonName").value("智人"))
                .andExpect(jsonPath("$.data.breadcrumbs.length()").value(7))
                .andExpect(jsonPath("$.data.mediaTotal").exists());
    }

    @Test
    void shouldListMediaPaged() throws Exception {
        mockMvc.perform(get("/api/taxa/{id}/media", 8).param("page", "0").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.items").isArray());
    }

    @Test
    void shouldSearchByCommonName() throws Exception {
        mockMvc.perform(get("/api/taxa/search").param("q", "智人").param("locale", "zh-CN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].scientificName").value("Homo sapiens"));
    }
}
