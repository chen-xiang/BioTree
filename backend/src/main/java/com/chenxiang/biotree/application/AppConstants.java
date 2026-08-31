/**
 * 默认语言与分页常量。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
package com.chenxiang.biotree.application;

public final class AppConstants {

    public static final String DEFAULT_LOCALE = "zh-CN";
    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_PAGE_SIZE = 30;
    public static final int MAX_PAGE_SIZE = 100;
    /** 详情接口首屏配图数量，避免多图节点 payload 膨胀 */
    public static final int MEDIA_PREVIEW_SIZE = 12;

    private AppConstants() {
    }
}
