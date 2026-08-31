/**
 * DwC-A TSV 表头解析：去掉命名空间前缀，按小写本地名取列下标。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
package com.chenxiang.biotree.infrastructure.importdata;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class DwcaTsvHeaders {

    private DwcaTsvHeaders() {
    }

    public static Map<String, Integer> parse(String headerLine) {
        Map<String, Integer> map = new HashMap<>();
        if (headerLine == null || headerLine.isBlank()) {
            return map;
        }
        String[] cols = headerLine.split("\t", -1);
        for (int i = 0; i < cols.length; i++) {
            String raw = cols[i] == null ? "" : cols[i].trim();
            int colon = Math.max(raw.lastIndexOf(':'), raw.lastIndexOf('/'));
            String local = colon >= 0 ? raw.substring(colon + 1) : raw;
            if (!local.isBlank()) {
                map.put(local.toLowerCase(Locale.ROOT), i);
            }
        }
        return map;
    }

    public static String col(String[] cols, Map<String, Integer> header, String... names) {
        for (String name : names) {
            Integer idx = header.get(name.toLowerCase(Locale.ROOT));
            if (idx != null && idx >= 0 && idx < cols.length) {
                String v = cols[idx];
                if (v != null && !v.isBlank()) {
                    return v.trim();
                }
            }
        }
        return null;
    }

    public static boolean truthy(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String v = value.trim().toLowerCase(Locale.ROOT);
        return "true".equals(v) || "1".equals(v) || "yes".equals(v) || "y".equals(v);
    }
}
