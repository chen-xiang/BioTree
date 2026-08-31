/**
 * 管理端分类维护接口。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
package com.chenxiang.biotree.api.admin;

import com.chenxiang.biotree.api.common.ApiResponse;
import com.chenxiang.biotree.api.taxon.CreateTaxonRequest;
import com.chenxiang.biotree.api.taxon.TaxonDetailDto;
import com.chenxiang.biotree.api.taxon.UpdateTaxonRequest;
import com.chenxiang.biotree.application.TaxonService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        taxonService.delete(id);
        return ApiResponse.ok();
    }
}
