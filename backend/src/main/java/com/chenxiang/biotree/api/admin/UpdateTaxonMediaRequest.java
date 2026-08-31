/**
 * 更新配图元数据请求。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
package com.chenxiang.biotree.api.admin;

public record UpdateTaxonMediaRequest(String caption, Integer sortOrder) {
}
