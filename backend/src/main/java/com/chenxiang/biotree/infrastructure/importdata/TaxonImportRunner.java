/**
 * 启动时按配置执行 CoL DwC-A 导入并退出。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
package com.chenxiang.biotree.infrastructure.importdata;

import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Order(0)
public class TaxonImportRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TaxonImportRunner.class);

    private final ImportProperties properties;
    private final ColDwcaImporter importer;
    private final ConfigurableApplicationContext context;

    public TaxonImportRunner(
            ImportProperties properties, ColDwcaImporter importer, ConfigurableApplicationContext context) {
        this.properties = properties;
        this.importer = importer;
        this.context = context;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            return;
        }
        if (!StringUtils.hasText(properties.getDwcaPath())) {
            throw new IllegalStateException("app.import.dwca-path is required when import is enabled");
        }
        log.info(
                "Starting COL import from {} replace={} kingdoms={} maxPerRank={}",
                properties.getDwcaPath(),
                properties.isReplace(),
                properties.getKingdoms(),
                properties.getMaxPerRank());
        ColDwcaImporter.ImportStats stats =
                importer.importArchive(Path.of(properties.getDwcaPath()), properties);
        log.info("Import completed: {}", stats);
        int code = SpringApplication.exit(context, () -> 0);
        System.exit(code);
    }
}
