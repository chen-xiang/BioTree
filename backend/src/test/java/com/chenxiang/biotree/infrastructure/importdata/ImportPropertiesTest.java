/**
 * 导入配置默认界列表单测。
 *
 * Author: chen-xiang
 * Created: 2026-09-01
 * Updated: 2026-09-02 覆盖默认提交批次与现行原核界名
 */
package com.chenxiang.biotree.infrastructure.importdata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ImportPropertiesTest {

    @Test
    void defaultsToCatalogueOfLifeSevenKingdoms() {
        ImportProperties properties = new ImportProperties();
        assertEquals(ImportProperties.DEFAULT_KINGDOMS, properties.getKingdoms());
        assertTrue(properties.getKingdoms().contains("Fungi"));
        assertTrue(properties.getKingdoms().contains("Bacillati"));
        assertTrue(properties.getKingdoms().contains("Thermoproteati"));
        assertEquals(2000, properties.getCommitBatchSize());
    }
}
