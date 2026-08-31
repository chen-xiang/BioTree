/**
 * 统计接口测试。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
package com.chenxiang.biotree.api.stats;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void summaryShouldReturnTotals() throws Exception {
        mockMvc.perform(get("/api/stats/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.totalTaxa").value(Matchers.greaterThanOrEqualTo(14)))
                .andExpect(jsonPath("$.data.byRank.KINGDOM").value(Matchers.greaterThanOrEqualTo(2)));
    }
}
