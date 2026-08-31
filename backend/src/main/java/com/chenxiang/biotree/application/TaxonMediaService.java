/**
 * 分类配图应用服务。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
package com.chenxiang.biotree.application;

import com.chenxiang.biotree.api.common.BusinessException;
import com.chenxiang.biotree.api.common.ErrorCode;
import com.chenxiang.biotree.api.taxon.TaxonMediaDto;
import com.chenxiang.biotree.domain.taxon.Taxon;
import com.chenxiang.biotree.domain.taxon.TaxonMedia;
import com.chenxiang.biotree.infrastructure.persistence.TaxonMediaRepository;
import com.chenxiang.biotree.infrastructure.persistence.TaxonRepository;
import com.chenxiang.biotree.infrastructure.storage.StorageService;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class TaxonMediaService {

    private static final Logger log = LoggerFactory.getLogger(TaxonMediaService.class);

    private final TaxonRepository taxonRepository;
    private final TaxonMediaRepository taxonMediaRepository;
    private final StorageService storageService;

    public TaxonMediaService(
            TaxonRepository taxonRepository,
            TaxonMediaRepository taxonMediaRepository,
            StorageService storageService) {
        this.taxonRepository = taxonRepository;
        this.taxonMediaRepository = taxonMediaRepository;
        this.storageService = storageService;
    }

    @Transactional
    public TaxonMediaDto upload(
            Long taxonId, MultipartFile file, String locale, String caption, String license, String attribution) {
        Taxon taxon = taxonRepository
                .findById(taxonId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TAXON_NOT_FOUND));
        validateFile(file);

        String contentType = file.getContentType();
        String storageKey;
        Integer width = null;
        Integer height = null;
        try (InputStream in = file.getInputStream()) {
            storageKey = storageService.store(file.getOriginalFilename(), contentType, in, file.getSize());
        } catch (IOException ex) {
            log.error("Failed to read upload stream for taxonId={}", taxonId, ex);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Failed to read upload");
        }

        try (InputStream probe = file.getInputStream()) {
            BufferedImage image = ImageIO.read(probe);
            if (image != null) {
                width = image.getWidth();
                height = image.getHeight();
            }
        } catch (IOException ex) {
            log.warn("Unable to probe image dimensions for taxonId={}", taxonId);
        }

        int nextOrder = taxonMediaRepository.findByTaxonIdOrderBySortOrderAscIdAsc(taxonId).stream()
                .mapToInt(TaxonMedia::getSortOrder)
                .max()
                .orElse(-1)
                + 1;

        TaxonMedia media = new TaxonMedia();
        media.setTaxon(taxon);
        media.setStorageKey(storageKey);
        media.setMimeType(contentType);
        media.setWidth(width);
        media.setHeight(height);
        media.setSizeBytes(file.getSize());
        media.setSortOrder(nextOrder);
        media.setLocale(StringUtils.hasText(locale) ? locale : null);
        media.setCaption(caption);
        media.setLicense(license);
        media.setAttribution(attribution);
        media.setCreatedAt(Instant.now());
        media = taxonMediaRepository.save(media);
        log.info("Uploaded media id={} taxonId={} key={}", media.getId(), taxonId, storageKey);
        return toDto(media);
    }

    @Transactional
    public void delete(Long taxonId, Long mediaId) {
        TaxonMedia media = taxonMediaRepository
                .findById(mediaId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEDIA_NOT_FOUND));
        if (!media.getTaxon().getId().equals(taxonId)) {
            throw new BusinessException(ErrorCode.MEDIA_NOT_FOUND);
        }
        storageService.delete(media.getStorageKey());
        taxonMediaRepository.delete(media);
        log.info("Deleted media id={} taxonId={}", mediaId, taxonId);
    }

    /**
     * 更新图注与排序（不改二进制）。
     */
    @Transactional
    public TaxonMediaDto update(Long taxonId, Long mediaId, String caption, Integer sortOrder) {
        TaxonMedia media = taxonMediaRepository
                .findById(mediaId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEDIA_NOT_FOUND));
        if (!media.getTaxon().getId().equals(taxonId)) {
            throw new BusinessException(ErrorCode.MEDIA_NOT_FOUND);
        }
        if (caption != null) {
            media.setCaption(caption.isBlank() ? null : caption.trim());
        }
        if (sortOrder != null) {
            media.setSortOrder(sortOrder);
        }
        media = taxonMediaRepository.save(media);
        log.info("Updated media id={} taxonId={}", mediaId, taxonId);
        return toDto(media);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_UPLOAD);
        }
        if (file.getSize() > MediaConstants.MAX_BYTES) {
            throw new BusinessException(ErrorCode.INVALID_UPLOAD);
        }
        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !MediaConstants.ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessException(ErrorCode.INVALID_UPLOAD);
        }
    }

    private TaxonMediaDto toDto(TaxonMedia media) {
        return new TaxonMediaDto(
                media.getId(),
                storageService.resolveUrl(media.getStorageKey()),
                media.getMimeType(),
                media.getWidth(),
                media.getHeight(),
                media.getSortOrder(),
                media.getLocale(),
                media.getCaption(),
                media.getLicense(),
                media.getAttribution());
    }
}
