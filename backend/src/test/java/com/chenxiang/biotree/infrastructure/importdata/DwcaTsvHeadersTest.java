/**
 * DwcaTsvHeaders 单元测试。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
package com.chenxiang.biotree.infrastructure.importdata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class DwcaTsvHeadersTest {

    @Test
    void parseStripsNamespaceAndReadsColumns() {
        Map<String, Integer> header =
                DwcaTsvHeaders.parse("dwc:taxonID\tdcterms:language\tdwc:vernacularName");
        String[] row = {"SP1", "eng", "Human"};
        assertEquals("SP1", DwcaTsvHeaders.col(row, header, "taxonid"));
        assertEquals("eng", DwcaTsvHeaders.col(row, header, "language"));
        assertEquals("Human", DwcaTsvHeaders.col(row, header, "vernacularname"));
        assertTrue(DwcaTsvHeaders.truthy("true"));
    }
}
