/**
 * 分类单元应用服务：查询、创建、更新、删除与路径维护。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
package com.chenxiang.biotree.application;

import com.chenxiang.biotree.api.common.BusinessException;
import com.chenxiang.biotree.api.common.ErrorCode;
import com.chenxiang.biotree.api.common.PageResult;
import com.chenxiang.biotree.api.taxon.CreateTaxonRequest;
import com.chenxiang.biotree.api.taxon.TaxonBreadcrumbDto;
import com.chenxiang.biotree.api.taxon.TaxonDetailDto;
import com.chenxiang.biotree.api.taxon.TaxonListItemDto;
import com.chenxiang.biotree.api.taxon.TaxonMediaDto;
import com.chenxiang.biotree.api.taxon.UpdateTaxonRequest;
import com.chenxiang.biotree.domain.taxon.Taxon;
import com.chenxiang.biotree.domain.taxon.TaxonI18n;
import com.chenxiang.biotree.domain.taxon.TaxonMedia;
import com.chenxiang.biotree.domain.taxon.TaxonRank;
import com.chenxiang.biotree.domain.taxon.TaxonRankRules;
import com.chenxiang.biotree.infrastructure.persistence.TaxonI18nRepository;
import com.chenxiang.biotree.infrastructure.persistence.TaxonMediaRepository;
import com.chenxiang.biotree.infrastructure.persistence.TaxonRepository;
import com.chenxiang.biotree.infrastructure.storage.StorageService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class TaxonService {

    private static final Logger log = LoggerFactory.getLogger(TaxonService.class);

    private final TaxonRepository taxonRepository;
    private final TaxonI18nRepository taxonI18nRepository;
    private final TaxonMediaRepository taxonMediaRepository;
    private final StorageService storageService;

    public TaxonService(
            TaxonRepository taxonRepository,
            TaxonI18nRepository taxonI18nRepository,
            TaxonMediaRepository taxonMediaRepository,
            StorageService storageService) {
        this.taxonRepository = taxonRepository;
        this.taxonI18nRepository = taxonI18nRepository;
        this.taxonMediaRepository = taxonMediaRepository;
        this.storageService = storageService;
    }

    @Transactional(readOnly = true)
    public PageResult<TaxonListItemDto> listChildren(Long parentId, String locale, int page, int size) {
        String resolvedLocale = resolveLocale(locale);
        PageRequest pageable = pageRequest(page, size);
        Page<Taxon> taxa = parentId == null
                ? taxonRepository.findByParentIsNull(pageable)
                : taxonRepository.findByParentId(parentId, pageable);
        return PageResult.of(toListItems(taxa.getContent(), resolvedLocale), taxa.getTotalElements(), taxa.getNumber(), taxa.getSize());
    }

    @Transactional(readOnly = true)
    public PageResult<TaxonListItemDto> search(String q, String locale, int page, int size) {
        if (!StringUtils.hasText(q) || q.trim().length() < 2) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Query must be at least 2 characters");
        }
        String resolvedLocale = resolveLocale(locale);
        PageRequest pageable = pageRequest(page, size);
        Page<Taxon> taxa = taxonRepository.search(q.trim(), resolvedLocale, pageable);
        return PageResult.of(toListItems(taxa.getContent(), resolvedLocale), taxa.getTotalElements(), taxa.getNumber(), taxa.getSize());
    }

    @Transactional(readOnly = true)
    public TaxonDetailDto getDetail(Long id, String locale) {
        String resolvedLocale = resolveLocale(locale);
        Taxon taxon = requireTaxon(id);
        TaxonI18n i18n = taxonI18nRepository.findByTaxonIdAndLocale(id, resolvedLocale).orElse(null);
        List<TaxonBreadcrumbDto> breadcrumbs = buildBreadcrumbs(taxon, resolvedLocale);
        List<TaxonMediaDto> media = taxonMediaRepository.findByTaxonIdOrderBySortOrderAscIdAsc(id).stream()
                .map(this::toMediaDto)
                .toList();
        return new TaxonDetailDto(
                taxon.getId(),
                taxon.getParent() == null ? null : taxon.getParent().getId(),
                taxon.getRank(),
                taxon.getScientificName(),
                i18n == null ? null : i18n.getCommonName(),
                i18n == null ? null : i18n.getSummary(),
                i18n == null ? null : i18n.getDescription(),
                resolvedLocale,
                taxon.getChildCount(),
                taxon.isAccepted(),
                breadcrumbs,
                media);
    }

    @Transactional
    public TaxonDetailDto create(CreateTaxonRequest request, String actor) {
        Taxon parent = null;
        TaxonRank parentRank = null;
        if (request.parentId() != null) {
            parent = requireTaxon(request.parentId());
            parentRank = parent.getRank();
        }
        if (!TaxonRankRules.isValidParent(request.rank(), parentRank)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Invalid parent/child rank combination");
        }
        assertUniqueName(parent == null ? null : parent.getId(), request.scientificName());

        Taxon taxon = new Taxon();
        taxon.setParent(parent);
        taxon.setRank(request.rank());
        taxon.setScientificName(request.scientificName().trim());
        taxon.setMaterializedPath("/");
        taxon.setChildCount(0);
        taxon.setAccepted(true);
        taxon.setCreatedAt(Instant.now());
        taxon.setUpdatedAt(Instant.now());
        taxon.setCreatedBy(actor);
        taxon = taxonRepository.save(taxon);

        String path = parent == null
                ? "/" + taxon.getId() + "/"
                : parent.getMaterializedPath() + taxon.getId() + "/";
        taxon.setMaterializedPath(path);
        taxon = taxonRepository.save(taxon);

        if (parent != null) {
            parent.setChildCount(parent.getChildCount() + 1);
            parent.setUpdatedAt(Instant.now());
            taxonRepository.save(parent);
        }

        upsertI18n(taxon, request.locale(), request.commonName(), request.summary(), request.description());
        log.info("Created taxon id={} rank={} name={}", taxon.getId(), taxon.getRank(), taxon.getScientificName());
        return getDetail(taxon.getId(), resolveLocale(request.locale()));
    }

    @Transactional
    public TaxonDetailDto update(Long id, UpdateTaxonRequest request) {
        Taxon taxon = requireTaxon(id);
        String newName = request.scientificName().trim();
        Long parentId = taxon.getParent() == null ? null : taxon.getParent().getId();
        if (!taxon.getScientificName().equalsIgnoreCase(newName)) {
            assertUniqueName(parentId, newName);
        }
        taxon.setScientificName(newName);
        if (request.accepted() != null) {
            taxon.setAccepted(request.accepted());
        }
        taxon.setUpdatedAt(Instant.now());
        taxonRepository.save(taxon);
        upsertI18n(taxon, request.locale(), request.commonName(), request.summary(), request.description());
        log.info("Updated taxon id={}", id);
        return getDetail(id, resolveLocale(request.locale()));
    }

    @Transactional
    public void delete(Long id) {
        Taxon taxon = requireTaxon(id);
        if (taxon.getChildCount() > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "Cannot delete taxon with children");
        }
        List<TaxonMedia> mediaList = taxonMediaRepository.findByTaxonIdOrderBySortOrderAscIdAsc(id);
        for (TaxonMedia media : mediaList) {
            storageService.delete(media.getStorageKey());
            taxonMediaRepository.delete(media);
        }
        taxonI18nRepository.deleteByTaxonId(id);

        Taxon parent = taxon.getParent();
        taxonRepository.delete(taxon);
        if (parent != null) {
            parent.setChildCount(Math.max(0, parent.getChildCount() - 1));
            parent.setUpdatedAt(Instant.now());
            taxonRepository.save(parent);
        }
        log.info("Deleted taxon id={}", id);
    }

    private void upsertI18n(Taxon taxon, String locale, String commonName, String summary, String description) {
        if (!StringUtils.hasText(commonName) && !StringUtils.hasText(summary) && !StringUtils.hasText(description)) {
            return;
        }
        String resolvedLocale = resolveLocale(locale);
        TaxonI18n i18n = taxonI18nRepository
                .findByTaxonIdAndLocale(taxon.getId(), resolvedLocale)
                .orElseGet(TaxonI18n::new);
        i18n.setTaxon(taxon);
        i18n.setLocale(resolvedLocale);
        if (commonName != null) {
            i18n.setCommonName(commonName);
        }
        if (summary != null) {
            i18n.setSummary(summary);
        }
        if (description != null) {
            i18n.setDescription(description);
        }
        taxonI18nRepository.save(i18n);
    }

    private List<TaxonListItemDto> toListItems(List<Taxon> taxa, String locale) {
        if (taxa.isEmpty()) {
            return List.of();
        }
        Map<Long, String> commonNames = loadCommonNames(taxa.stream().map(Taxon::getId).toList(), locale);
        return taxa.stream()
                .map(t -> new TaxonListItemDto(
                        t.getId(),
                        t.getRank(),
                        t.getScientificName(),
                        commonNames.get(t.getId()),
                        t.getChildCount(),
                        t.getChildCount() > 0))
                .toList();
    }

    private Map<Long, String> loadCommonNames(List<Long> ids, String locale) {
        Map<Long, String> map = new HashMap<>();
        if (ids.isEmpty()) {
            return map;
        }
        for (TaxonI18n i18n : taxonI18nRepository.findByTaxonIdInAndLocale(ids, locale)) {
            map.put(i18n.getTaxon().getId(), i18n.getCommonName());
        }
        return map;
    }

    private List<TaxonBreadcrumbDto> buildBreadcrumbs(Taxon taxon, String locale) {
        List<Long> ids = Arrays.stream(taxon.getMaterializedPath().split("/"))
                .filter(StringUtils::hasText)
                .map(Long::valueOf)
                .collect(Collectors.toCollection(ArrayList::new));
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<Long, Taxon> byId = taxonRepository.findByIdIn(ids).stream()
                .collect(Collectors.toMap(Taxon::getId, t -> t));
        Map<Long, String> names = loadCommonNames(ids, locale);
        List<TaxonBreadcrumbDto> result = new ArrayList<>();
        for (Long id : ids) {
            Taxon node = byId.get(id);
            if (node != null) {
                result.add(new TaxonBreadcrumbDto(node.getId(), node.getRank(), node.getScientificName(), names.get(id)));
            }
        }
        return result;
    }

    private TaxonMediaDto toMediaDto(TaxonMedia media) {
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

    private Taxon requireTaxon(Long id) {
        return taxonRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Taxon not found"));
    }

    private void assertUniqueName(Long parentId, String scientificName) {
        boolean exists = parentId == null
                ? taxonRepository.existsByParentIsNullAndScientificNameIgnoreCase(scientificName.trim())
                : taxonRepository.existsByParentIdAndScientificNameIgnoreCase(parentId, scientificName.trim());
        if (exists) {
            throw new BusinessException(ErrorCode.CONFLICT, "Scientific name already exists under the same parent");
        }
    }

    private static String resolveLocale(String locale) {
        return StringUtils.hasText(locale) ? locale : AppConstants.DEFAULT_LOCALE;
    }

    private static PageRequest pageRequest(int page, int size) {
        int safePage = Math.max(page, AppConstants.DEFAULT_PAGE);
        int safeSize = size <= 0 ? AppConstants.DEFAULT_PAGE_SIZE : Math.min(size, AppConstants.MAX_PAGE_SIZE);
        return PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.ASC, "scientificName"));
    }
}
