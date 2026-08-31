/**
 * 公开分类查询接口：子节点、详情、搜索、配图分页。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 * Updated: 2026-08-31 配图分页接口
 */
package com.chenxiang.biotree.api.taxon;

import com.chenxiang.biotree.api.common.ApiResponse;
import com.chenxiang.biotree.api.common.PageResult;
import com.chenxiang.biotree.application.AppConstants;
import com.chenxiang.biotree.application.TaxonService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/taxa")
public class TaxonController {

    private final TaxonService taxonService;

    public TaxonController(TaxonService taxonService) {
        this.taxonService = taxonService;
    }

    @GetMapping("/children")
    public ApiResponse<PageResult<TaxonListItemDto>> children(
            @RequestParam(required = false) Long parentId,
            @RequestParam(required = false, defaultValue = AppConstants.DEFAULT_LOCALE) String locale,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "30") int size) {
        return ApiResponse.ok(taxonService.listChildren(parentId, locale, page, size));
    }

    @GetMapping("/search")
    public ApiResponse<PageResult<TaxonListItemDto>> search(
            @RequestParam String q,
            @RequestParam(required = false, defaultValue = AppConstants.DEFAULT_LOCALE) String locale,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "30") int size) {
        return ApiResponse.ok(taxonService.search(q, locale, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<TaxonDetailDto> detail(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = AppConstants.DEFAULT_LOCALE) String locale) {
        return ApiResponse.ok(taxonService.getDetail(id, locale));
    }

    @GetMapping("/{id}/media")
    public ApiResponse<PageResult<TaxonMediaDto>> media(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "12") int size) {
        return ApiResponse.ok(taxonService.listMedia(id, page, size));
    }
}
