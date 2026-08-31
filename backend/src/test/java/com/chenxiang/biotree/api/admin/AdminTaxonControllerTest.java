/**
 * 管理端分类写接口集成测试。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
package com.chenxiang.biotree.api.admin;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminTaxonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/admin/taxa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rank":"KINGDOM","scientificName":"Fungi","locale":"zh-CN","commonName":"真菌界"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminCanCreateAndDeleteLeafKingdom() throws Exception {
        String body = mockMvc.perform(post("/api/admin/taxa")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rank":"KINGDOM","scientificName":"Fungi","locale":"zh-CN","commonName":"真菌界"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scientificName").value("Fungi"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long id = Long.parseLong(body.replaceAll("(?s).*\"id\":(\\d+).*", "$1"));

        mockMvc.perform(delete("/api/admin/taxa/{id}", id).with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void cannotDeleteTaxonWithChildren() throws Exception {
        mockMvc.perform(delete("/api/admin/taxa/{id}", 1).with(user("admin").roles("ADMIN")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40900));
    }
}
