/**
 * 媒体上传约束常量。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
package com.chenxiang.biotree.application;

import java.util.Set;

public final class MediaConstants {

    public static final long MAX_BYTES = 5L * 1024 * 1024;
    public static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif");

    private MediaConstants() {
    }
}
