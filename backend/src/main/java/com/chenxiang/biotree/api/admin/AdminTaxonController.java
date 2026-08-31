/**
 * 管理端分类维护接口。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 * Updated: 2026-08-31 增加节点移动接口
 */
package com.chenxiang.biotree.api.admin;

import com.chenxiang.biotree.api.common.ApiResponse;
import com.chenxiang.biotree.api.taxon.CreateTaxonRequest;
import com.chenxiang.biotree.api.taxon.MoveTaxonRequest;
import com.chenxiang.biotree.api.taxon.TaxonDetailDto;
import com.chenxiang.biotree.api.taxon.UpdateTaxonRequest;
import com.chenxiang.biotree.application.AppConstants;
import com.chenxiang.biotree.application.TaxonService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/taxa")
public class AdminTaxonController {

    private final TaxonService taxonService;

    public AdminTaxonController(TaxonService taxonService) {
        this.taxonService = taxonService;
    }

    @PostMapping
    public ApiResponse<TaxonDetailDto> create(
            @Valid @RequestBody CreateTaxonRequest request, Authentication authentication) {
        String actor = authentication == null ? null : authentication.getName();
        return ApiResponse.ok(taxonService.create(request, actor));
    }

    @PutMapping("/{id}")
    public ApiResponse<TaxonDetailDto> update(
            @PathVariable Long id, @Valid @RequestBody UpdateTaxonRequest request) {
        return ApiResponse.ok(taxonService.update(id, request));
    }

    @PostMapping("/{id}/move")
    public ApiResponse<TaxonDetailDto> move(
            @PathVariable Long id,
            @Valid @RequestBody MoveTaxonRequest request,
            @RequestParam(required = false, defaultValue = AppConstants.DEFAULT_LOCALE) String locale) {
        return ApiResponse.ok(taxonService.move(id, request.newParentId(), locale));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        taxonService.delete(id);
        return ApiResponse.ok();
    }
}
