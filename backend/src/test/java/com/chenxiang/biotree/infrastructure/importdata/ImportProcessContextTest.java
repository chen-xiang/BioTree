/**
 * 导入进程（非 Web）与 Web 进程职责隔离冒烟测试。
 *
 * Author: chen-xiang
 * Created: 2026-09-01
 */
package com.chenxiang.biotree.infrastructure.importdata;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.chenxiang.biotree.security.DefaultAdminGuard;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;

class ImportProcessContextTest {

    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
    @ActiveProfiles("test")
    static class NonWebImportContext {

        @Autowired
        private ApplicationContext context;

        @Test
        void loadsImporterWithoutWebSecurity() {
            assertNotNull(context.getBean(ColDwcaImporter.class));
            assertFalse(context.getBeanNamesForType(SecurityFilterChain.class).length > 0);
            assertFalse(context.getBeanNamesForType(DefaultAdminGuard.class).length > 0);
            assertFalse(context.getBeanNamesForType(WebImportIgnoredWarning.class).length > 0);
        }
    }

    @SpringBootTest(properties = "app.import.enabled=true")
    @ActiveProfiles("test")
    static class WebIgnoresImportEnabled {

        @Autowired
        private ApplicationContext context;

        @Test
        void staysUpAndWarnsInsteadOfImporting() {
            assertTrue(context.getBean(ImportProperties.class).isEnabled());
            assertNotNull(context.getBean(WebImportIgnoredWarning.class));
            assertNotNull(context.getBean(SecurityFilterChain.class));
        }
    }
}
