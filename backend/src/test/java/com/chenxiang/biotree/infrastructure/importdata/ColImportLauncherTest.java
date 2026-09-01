/**
 * 非 Web 导入编排单测。
 *
 * Author: chen-xiang
 * Created: 2026-09-01
 */
package com.chenxiang.biotree.infrastructure.importdata;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ColImportLauncherTest {

    @Test
    void ensureImportEnabledPrependsFlagWhenMissing() {
        String[] next = ColImportLauncher.ensureImportEnabled(new String[] {"--app.import.replace=true"});
        assertArrayEquals(
                new String[] {ColImportLauncher.ENABLED_FLAG, "--app.import.replace=true"}, next);
    }

    @Test
    void ensureImportEnabledKeepsExplicitFlag() {
        String[] source = {"--app.import.enabled=false", "--app.import.replace=true"};
        assertArrayEquals(source, ColImportLauncher.ensureImportEnabled(source));
    }

    @Test
    void executeRequiresDwcaPath() {
        ImportProperties properties = new ImportProperties();
        properties.setDwcaPath(" ");
        assertThrows(
                IllegalStateException.class,
                () -> ColImportLauncher.execute(properties, mock(ColDwcaImporter.class)));
    }

    @Test
    void executeDelegatesToImporter() {
        ImportProperties properties = new ImportProperties();
        properties.setDwcaPath("data/import/col.zip");
        ColDwcaImporter importer = mock(ColDwcaImporter.class);
        when(importer.importArchive(any(Path.class), eq(properties)))
                .thenReturn(new ColDwcaImporter.ImportStats(1, 0, 0, 0, 0, 0, java.util.Map.of()));

        ColImportLauncher.execute(properties, importer);

        verify(importer).importArchive(Path.of("data/import/col.zip"), properties);
        assertEquals("data/import/col.zip", properties.getDwcaPath());
    }
}
