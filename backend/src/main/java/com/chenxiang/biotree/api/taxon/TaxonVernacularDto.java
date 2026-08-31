/**
 * 其它语言俗名条目。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
package com.chenxiang.biotree.api.taxon;

public record TaxonVernacularDto(String locale, String commonName, boolean preferred) {
}
