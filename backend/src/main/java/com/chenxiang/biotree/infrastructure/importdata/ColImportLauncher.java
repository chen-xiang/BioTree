/**
 * 非 Web 导入编排：无端口启动 Spring 上下文并执行 DwC-A 导入。
 *
 * Author: chen-xiang
 * Created: 2026-09-01
 */
package com.chenxiang.biotree.infrastructure.importdata;

import com.chenxiang.biotree.BiotreeApplication;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.StringUtils;

public final class ColImportLauncher {

    private static final Logger log = LoggerFactory.getLogger(ColImportLauncher.class);

    static final String ENABLED_FLAG = "--app.import.enabled=true";

    private ColImportLauncher() {}

    public static int launch(String[] args) {
        SpringApplication app = new SpringApplication(BiotreeApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        ConfigurableApplicationContext context = app.run(ensureImportEnabled(args));
        try {
            execute(context.getBean(ImportProperties.class), context.getBean(ColDwcaImporter.class));
            return SpringApplication.exit(context, () -> 0);
        } catch (Exception ex) {
            log.error("Import failed", ex);
            return SpringApplication.exit(context, () -> 1);
        }
    }

    static String[] ensureImportEnabled(String[] args) {
        String[] source = args == null ? new String[0] : args;
        for (String arg : source) {
            if (arg != null && arg.startsWith("--app.import.enabled=")) {
                return source;
            }
        }
        List<String> next = new ArrayList<>(source.length + 1);
        next.add(ENABLED_FLAG);
        next.addAll(List.of(source));
        return next.toArray(String[]::new);
    }

    static void execute(ImportProperties properties, ColDwcaImporter importer) {
        if (!StringUtils.hasText(properties.getDwcaPath())) {
            throw new IllegalStateException("app.import.dwca-path is required");
        }
        log.info(
                "Starting COL import from {} replace={} resume={} kingdoms={} maxPerRank={}",
                properties.getDwcaPath(),
                properties.isReplace(),
                properties.isResume(),
                properties.getKingdoms(),
                properties.getMaxPerRank());
        ColDwcaImporter.ImportStats stats = importer.importArchive(Path.of(properties.getDwcaPath()), properties);
        log.info("Import completed: {}", stats);
    }
}
