/**
 * 移动分类节点请求：指定新的父节点。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
package com.chenxiang.biotree.api.taxon;

import jakarta.validation.constraints.NotNull;

public record MoveTaxonRequest(@NotNull Long newParentId) {
}
