/**
 * 分类节点移动集成测试。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
package com.chenxiang.biotree.api.admin;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminTaxonMoveControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldMoveSpeciesUnderSiblingGenus() throws Exception {
        // 在 Hominidae(6) 下新建姊妹属 Pan，再把 Homo sapiens(8) 从 Homo(7) 移到 Pan
        String createGenus = mockMvc.perform(post("/api/admin/taxa")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"parentId":6,"rank":"GENUS","scientificName":"Pan","locale":"en","commonName":"Pan"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scientificName").value("Pan"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long panId = Long.parseLong(createGenus.replaceAll("(?s).*\"id\":(\\d+).*", "$1"));

        mockMvc.perform(post("/api/admin/taxa/{id}/move", 8)
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newParentId\":" + panId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.parentId").value(panId));

        mockMvc.perform(get("/api/taxa/{id}", 8).param("locale", "zh-CN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.parentId").value(panId))
                .andExpect(jsonPath("$.data.commonName").value("智人"));
    }

    @Test
    void shouldRejectMoveUnderDescendant() throws Exception {
        mockMvc.perform(post("/api/admin/taxa/{id}/move", 7)
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newParentId\":8}"))
                .andExpect(status().isBadRequest());
    }
}
