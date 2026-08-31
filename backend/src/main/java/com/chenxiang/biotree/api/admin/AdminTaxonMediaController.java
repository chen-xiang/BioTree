/**
 * 管理端分类配图上传与删除。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
package com.chenxiang.biotree.api.admin;

import com.chenxiang.biotree.api.common.ApiResponse;
import com.chenxiang.biotree.api.taxon.TaxonMediaDto;
import com.chenxiang.biotree.application.TaxonMediaService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/taxa/{taxonId}/media")
public class AdminTaxonMediaController {

    private final TaxonMediaService taxonMediaService;

    public AdminTaxonMediaController(TaxonMediaService taxonMediaService) {
        this.taxonMediaService = taxonMediaService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<TaxonMediaDto> upload(
            @PathVariable Long taxonId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String locale,
            @RequestParam(required = false) String caption,
            @RequestParam(required = false) String license,
            @RequestParam(required = false) String attribution) {
        return ApiResponse.ok(taxonMediaService.upload(taxonId, file, locale, caption, license, attribution));
    }

    @DeleteMapping("/{mediaId}")
    public ApiResponse<Void> delete(@PathVariable Long taxonId, @PathVariable Long mediaId) {
        taxonMediaService.delete(taxonId, mediaId);
        return ApiResponse.ok();
    }
}
