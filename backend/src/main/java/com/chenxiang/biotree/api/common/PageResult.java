/**
 * 分页结果封装。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
package com.chenxiang.biotree.api.common;

import java.util.List;
import org.springframework.data.domain.Page;

public record PageResult<T>(List<T> items, long total, int page, int size) {

    public static <T> PageResult<T> from(Page<T> page) {
        return new PageResult<>(page.getContent(), page.getTotalElements(), page.getNumber(), page.getSize());
    }

    public static <T> PageResult<T> of(List<T> items, long total, int page, int size) {
        return new PageResult<>(items, total, page, size);
    }
}
