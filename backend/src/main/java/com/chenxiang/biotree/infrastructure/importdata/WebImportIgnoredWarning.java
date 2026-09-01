/**
 * Web 进程忽略 app.import.enabled，避免误用 bootRun 导入并退出。
 *
 * Author: chen-xiang
 * Created: 2026-09-01
 */
package com.chenxiang.biotree.infrastructure.importdata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnWebApplication
public class WebImportIgnoredWarning implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(WebImportIgnoredWarning.class);

    private final ImportProperties properties;

    public WebImportIgnoredWarning(ImportProperties properties) {
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (properties.isEnabled()) {
            log.warn("app.import.enabled is ignored in the web process; run gradle importCol");
        }
    }
}
