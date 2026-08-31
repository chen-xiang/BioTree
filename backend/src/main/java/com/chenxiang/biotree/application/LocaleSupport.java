/**
 * 多语言回退链：请求语言 → 英语 →（内容仍缺则由调用方回退到学名）。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
package com.chenxiang.biotree.application;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import org.springframework.util.StringUtils;

public final class LocaleSupport {

    public static final String ENGLISH = "en";

    private LocaleSupport() {
    }

    /**
     * 规范化 locale，空则返回默认语言。
     */
    public static String normalize(String locale) {
        if (!StringUtils.hasText(locale)) {
            return AppConstants.DEFAULT_LOCALE;
        }
        return locale.trim();
    }

    /**
     * 回退链（去重、保序）：preferred → en（若不同）。
     */
    public static List<String> fallbackChain(String locale) {
        LinkedHashSet<String> chain = new LinkedHashSet<>();
        chain.add(normalize(locale));
        chain.add(ENGLISH);
        return new ArrayList<>(chain);
    }

    /**
     * 是否中文系 locale（用于文档与调试，不强制映射）。
     */
    public static boolean isChinese(String locale) {
        if (!StringUtils.hasText(locale)) {
            return false;
        }
        String lower = locale.toLowerCase(Locale.ROOT);
        return lower.startsWith("zh");
    }
}
