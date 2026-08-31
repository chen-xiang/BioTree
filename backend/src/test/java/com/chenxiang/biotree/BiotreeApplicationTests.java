/**
 * 应用上下文加载冒烟测试。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
package com.chenxiang.biotree;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class BiotreeApplicationTests {

    @Test
    void contextLoads() {
        // 冒烟：上下文与 Flyway 可在 H2(MySQL 模式) 下启动
    }
}
