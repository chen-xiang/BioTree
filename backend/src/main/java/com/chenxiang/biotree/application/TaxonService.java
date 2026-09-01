/**
 * 分类单元应用服务：查询、创建、更新、移动、删除与路径维护。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 * Updated: 2026-08-31 locale 回退、前缀优先搜索、节点移动
 * Updated: 2026-09-01 子节点列表按阶元深度优先排序
 */
package com.chenxiang.biotree.application;

import com.chenxiang.biotree.api.common.BusinessException;
import com.chenxiang.biotree.api.common.ErrorCode;
import com.chenxiang.biotree.api.common.PageResult;
import com.chenxiang.biotree.api.taxon.CreateTaxonRequest;
import com.chenxiang.biotree.api.taxon.TaxonBreadcrumbDto;
import com.chenxiang.biotree.api.taxon.TaxonDetailDto;
import com.chenxiang.biotree.api.taxon.TaxonDistributionDto;
import com.chenxiang.biotree.api.taxon.TaxonListItemDto;
import com.chenxiang.biotree.api.taxon.TaxonMediaDto;
import com.chenxiang.biotree.api.taxon.TaxonSynonymDto;
import com.chenxiang.biotree.api.taxon.TaxonVernacularDto;
import com.chenxiang.biotree.api.taxon.UpdateTaxonRequest;
import com.chenxiang.biotree.domain.taxon.Taxon;
import com.chenxiang.biotree.domain.taxon.TaxonI18n;
import com.chenxiang.biotree.domain.taxon.TaxonMedia;
import com.chenxiang.biotree.domain.taxon.TaxonRank;
import com.chenxiang.biotree.domain.taxon.TaxonRankRules;
import com.chenxiang.biotree.domain.taxon.TaxonView;
import com.chenxiang.biotree.infrastructure.persistence.TaxonDistributionRepository;
import com.chenxiang.biotree.infrastructure.persistence.TaxonI18nRepository;
import com.chenxiang.biotree.infrastructure.persistence.TaxonMediaRepository;
import com.chenxiang.biotree.infrastructure.persistence.TaxonRepository;
import com.chenxiang.biotree.infrastructure.persistence.TaxonSynonymRepository;
import com.chenxiang.biotree.infrastructure.storage.StorageService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final TaxonSynonymRepository taxonSynonymRepository;
    private final TaxonDistributionRepository taxonDistributionRepository;
    private final TaxonSearchDao taxonSearchDao;
    private final StorageService storageService;
    private final SimpleTaxonChildrenCollector simpleChildrenCollector;
    private final SimpleParentRebuilder simpleParentRebuilder;

    public TaxonService(
            TaxonRepository taxonRepository,
            TaxonI18nRepository taxonI18nRepository,
            TaxonMediaRepository taxonMediaRepository,
            TaxonSynonymRepository taxonSynonymRepository,
            TaxonDistributionRepository taxonDistributionRepository,
            TaxonSearchDao taxonSearchDao,
            StorageService storageService,
            SimpleTaxonChildrenCollector simpleChildrenCollector,
            SimpleParentRebuilder simpleParentRebuilder) {
        this.taxonRepository = taxonRepository;
        this.taxonI18nRepository = taxonI18nRepository;
        this.taxonMediaRepository = taxonMediaRepository;
        this.taxonSynonymRepository = taxonSynonymRepository;
        this.taxonDistributionRepository = taxonDistributionRepository;
        this.taxonSearchDao = taxonSearchDao;
        this.storageService = storageService;
        this.simpleChildrenCollector = simpleChildrenCollector;
        this.simpleParentRebuilder = simpleParentRebuilder;
    }

    @Transactional(readOnly = true)
    public PageResult<TaxonListItemDto> listChildren(Long parentId, String locale, int page, int size) {
        return listChildren(parentId, locale, page, size, TaxonView.SIMPLE);
    }

    @Transactional(readOnly = true)
    public PageResult<TaxonListItemDto> listChildren(
            Long parentId, String locale, int page, int size, TaxonView view) {
        String resolvedLocale = LocaleSupport.normalize(locale);
        PageRequest pageable = childrenPageRequest(page, size);
        if (view == TaxonView.FULL) {
            Page<Taxon> taxa = parentId == null
                    ? taxonRepository.findByParentIsNull(pageable)
                    : taxonRepository.findByParentId(parentId, pageable);
            return PageResult.of(
                    toListItems(taxa.getContent(), resolvedLocale, TaxonView.FULL),
                    taxa.getTotalElements(),
                    taxa.getNumber(),
                    taxa.getSize());
        }
        Page<Taxon> simplePage = simpleChildrenCollector.collectPage(parentId, pageable);
        if (simplePage.isEmpty() && simpleChildrenCollector.countVisible(parentId) == 0) {
            List<Taxon> all = simpleChildrenCollector.collect(parentId);
            int from = Math.min(pageable.getPageNumber() * pageable.getPageSize(), all.size());
            int to = Math.min(from + pageable.getPageSize(), all.size());
            List<Taxon> slice = all.subList(from, to);
            return PageResult.of(
                    toListItems(slice, resolvedLocale, TaxonView.SIMPLE),
                    all.size(),
                    pageable.getPageNumber(),
                    pageable.getPageSize());
        }
        return PageResult.of(
                toListItems(simplePage.getContent(), resolvedLocale, TaxonView.SIMPLE),
                simplePage.getTotalElements(),
                simplePage.getNumber(),
                simplePage.getSize());
    }

    @Transactional(readOnly = true)
    public PageResult<TaxonListItemDto> search(String q, String locale, int page, int size) {
        if (!StringUtils.hasText(q) || q.trim().length() < 2) {
            throw new BusinessException(ErrorCode.INVALID_QUERY);
        }
        String resolvedLocale = LocaleSupport.normalize(locale);
        List<String> locales = LocaleSupport.fallbackChain(resolvedLocale);
        PageRequest pageable = pageRequest(page, size);
        String term = q.trim();
        Page<Taxon> taxa = taxonSearchDao.search(term, locales, pageable);
        return PageResult.of(
                toListItems(taxa.getContent(), resolvedLocale, TaxonView.FULL),
                taxa.getTotalElements(),
                taxa.getNumber(),
                taxa.getSize());
    }

    @Transactional(readOnly = true)
    public TaxonDetailDto getDetail(Long id, String locale) {
        return getDetail(id, locale, TaxonView.SIMPLE);
    }

    @Transactional(readOnly = true)
    public TaxonDetailDto getDetail(Long id, String locale, TaxonView view) {
        String preferredLocale = LocaleSupport.normalize(locale);
        Taxon taxon = requireTaxon(id);
        MergedI18n i18n = mergeI18n(id, preferredLocale);
        List<TaxonBreadcrumbDto> breadcrumbs = buildBreadcrumbs(taxon, preferredLocale, view);
        long mediaTotal = taxonMediaRepository.countByTaxonId(id);
        PageRequest mediaPage = PageRequest.of(
                0,
                AppConstants.MEDIA_PREVIEW_SIZE,
                Sort.by(Sort.Direction.ASC, "sortOrder").and(Sort.by(Sort.Direction.ASC, "id")));
        List<TaxonMediaDto> media = taxonMediaRepository
                .findByTaxonIdOrderBySortOrderAscIdAsc(id, mediaPage)
                .getContent()
                .stream()
                .map(this::toMediaDto)
                .toList();
        List<TaxonSynonymDto> synonyms = taxonSynonymRepository.findByTaxonIdOrderByScientificNameAsc(id).stream()
                .map(s -> new TaxonSynonymDto(s.getId(), s.getScientificName()))
                .toList();
        List<TaxonVernacularDto> vernaculars = taxonI18nRepository.findByTaxonId(id).stream()
                .filter(row -> StringUtils.hasText(row.getCommonName()))
                .map(row -> new TaxonVernacularDto(row.getLocale(), row.getCommonName(), row.isPreferred()))
                .toList();
        List<TaxonDistributionDto> distributions =
                taxonDistributionRepository.findByTaxonIdOrderByCountryCodeAscIdAsc(id).stream()
                        .map(d -> new TaxonDistributionDto(
                                d.getId(),
                                d.getCountryCode(),
                                d.getLocality(),
                                d.getEstablishmentMeans(),
                                d.getThreatStatus(),
                                d.getSourceText()))
                        .toList();
        return new TaxonDetailDto(
                taxon.getId(),
                taxon.getParent() == null ? null : taxon.getParent().getId(),
                taxon.getRank(),
                taxon.getScientificName(),
                taxon.getScientificNameAuthorship(),
                taxon.getScientificNameVerbatim(),
                taxon.getNamePublishedIn(),
                taxon.getNameAccordingTo(),
                taxon.getNomenclaturalCode(),
                taxon.getNomenclaturalStatus(),
                i18n.commonName(),
                i18n.summary(),
                i18n.description(),
                i18n.contentLocale() == null ? preferredLocale : i18n.contentLocale(),
                viewChildCount(taxon, view),
                taxon.isAccepted(),
                breadcrumbs,
                media,
                mediaTotal,
                synonyms,
                taxon.getRankRaw(),
                vernaculars,
                distributions,
                taxon.getChildCount(),
                nearestSimpleAncestorId(taxon, view));
    }

    private Long nearestSimpleAncestorId(Taxon taxon, TaxonView view) {
        if (view != TaxonView.SIMPLE || SimpleParentSupport.isLinnaean(taxon.getRank())) {
            return null;
        }
        if (taxon.getSimpleParent() != null) {
            return taxon.getSimpleParent().getId();
        }
        List<Long> ancestors = SimpleParentSupport.ancestorIds(taxon.getMaterializedPath(), taxon.getId());
        if (ancestors.isEmpty()) {
            return null;
        }
        Map<Long, Taxon> byId = taxonRepository.findByIdIn(ancestors).stream()
                .collect(Collectors.toMap(Taxon::getId, t -> t, (a, b) -> a));
        return SimpleParentSupport.nearestLinnaeanAncestorId(taxon, byId);
    }

    private int viewChildCount(Taxon taxon, TaxonView view) {
        if (view == TaxonView.FULL) {
            return taxon.getChildCount();
        }
        long n = simpleChildrenCollector.countVisible(taxon.getId());
        if (n > 0) {
            return (int) Math.min(n, Integer.MAX_VALUE);
        }
        return simpleChildrenCollector.hasVisibleSimpleChildren(taxon.getId()) ? 1 : 0;
    }

    /**
     * 分页列出分类配图（详情首屏之外的加载更多）。
     */
    @Transactional(readOnly = true)
    public PageResult<TaxonMediaDto> listMedia(Long taxonId, int page, int size) {
        requireTaxon(taxonId);
        int safePage = Math.max(page, AppConstants.DEFAULT_PAGE);
        int safeSize = size <= 0 ? AppConstants.MEDIA_PREVIEW_SIZE : Math.min(size, AppConstants.MAX_PAGE_SIZE);
        PageRequest mediaPage = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(Sort.Direction.ASC, "sortOrder").and(Sort.by(Sort.Direction.ASC, "id")));
        Page<TaxonMedia> result = taxonMediaRepository.findByTaxonIdOrderBySortOrderAscIdAsc(taxonId, mediaPage);
        return PageResult.of(
                result.getContent().stream().map(this::toMediaDto).toList(),
                result.getTotalElements(),
                result.getNumber(),
                result.getSize());
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
            throw new BusinessException(ErrorCode.INVALID_PARENT);
        }
        assertUniqueName(parent == null ? null : parent.getId(), request.scientificName());

        Taxon taxon = new Taxon();
        taxon.setParent(parent);
        taxon.setRank(request.rank());
        taxon.setRankRaw(request.rank().name().toLowerCase());
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
        simpleParentRebuilder.assignForNewNode(taxon);
        taxon = taxonRepository.save(taxon);

        if (parent != null) {
            parent.setChildCount(parent.getChildCount() + 1);
            parent.setUpdatedAt(Instant.now());
            taxonRepository.save(parent);
        }

        upsertI18n(taxon, request.locale(), request.commonName(), request.summary(), request.description());
        log.info("Created taxon id={} rank={} name={}", taxon.getId(), taxon.getRank(), taxon.getScientificName());
        return getDetail(taxon.getId(), LocaleSupport.normalize(request.locale()), TaxonView.FULL);
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
        return getDetail(id, LocaleSupport.normalize(request.locale()), TaxonView.FULL);
    }

    /**
     * 将节点移动到新父节点，并批量更新自身与子孙的 materialized_path。
     */
    @Transactional
    public TaxonDetailDto move(Long id, Long newParentId, String locale) {
        Taxon taxon = requireTaxon(id);
        if (taxon.getRank() == TaxonRank.KINGDOM) {
            throw new BusinessException(ErrorCode.INVALID_MOVE);
        }
        if (newParentId == null) {
            throw new BusinessException(ErrorCode.INVALID_MOVE);
        }
        if (newParentId.equals(id)) {
            throw new BusinessException(ErrorCode.INVALID_MOVE);
        }

        Taxon newParent = requireTaxon(newParentId);
        if (!TaxonRankRules.isValidParent(taxon.getRank(), newParent.getRank())) {
            throw new BusinessException(ErrorCode.INVALID_PARENT);
        }

        String oldPath = taxon.getMaterializedPath();
        if (newParent.getMaterializedPath().startsWith(oldPath)) {
            throw new BusinessException(ErrorCode.INVALID_MOVE);
        }

        Taxon oldParent = taxon.getParent();
        Long oldParentId = oldParent == null ? null : oldParent.getId();
        if (newParentId.equals(oldParentId)) {
            return getDetail(id, locale, TaxonView.FULL);
        }

        assertUniqueName(newParentId, taxon.getScientificName());

        String newPath = newParent.getMaterializedPath() + taxon.getId() + "/";
        List<Taxon> subtree = taxonRepository.findByMaterializedPathStartingWith(oldPath);
        Instant now = Instant.now();
        for (Taxon node : subtree) {
            String path = node.getMaterializedPath();
            if (!path.startsWith(oldPath)) {
                continue;
            }
            node.setMaterializedPath(newPath + path.substring(oldPath.length()));
            node.setUpdatedAt(now);
            if (node.getId().equals(taxon.getId())) {
                node.setParent(newParent);
            }
        }
        taxonRepository.saveAll(subtree);

        if (oldParent != null) {
            oldParent.setChildCount(Math.max(0, oldParent.getChildCount() - 1));
            oldParent.setUpdatedAt(now);
            taxonRepository.save(oldParent);
        }
        newParent.setChildCount(newParent.getChildCount() + 1);
        newParent.setUpdatedAt(now);
        taxonRepository.save(newParent);

        simpleParentRebuilder.rebuildSubtree(newPath);
        log.info("Moved taxon id={} to parentId={}", id, newParentId);
        return getDetail(id, LocaleSupport.normalize(locale), TaxonView.FULL);
    }

    @Transactional
    public void delete(Long id) {
        Taxon taxon = requireTaxon(id);
        if (taxon.getChildCount() > 0) {
            throw new BusinessException(ErrorCode.TAXON_HAS_CHILDREN);
        }
        List<TaxonMedia> mediaList = taxonMediaRepository.findByTaxonIdOrderBySortOrderAscIdAsc(id);
        for (TaxonMedia media : mediaList) {
            if (StringUtils.hasText(media.getStorageKey())) {
                storageService.delete(media.getStorageKey());
            }
            taxonMediaRepository.delete(media);
        }
        taxonDistributionRepository.deleteByTaxonId(id);
        taxonI18nRepository.deleteByTaxonId(id);
        taxonSynonymRepository.findByTaxonIdOrderByScientificNameAsc(id).forEach(taxonSynonymRepository::delete);

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
        String resolvedLocale = LocaleSupport.normalize(locale);
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

    private List<TaxonListItemDto> toListItems(List<Taxon> taxa, String locale, TaxonView view) {
        if (taxa.isEmpty()) {
            return List.of();
        }
        Map<Long, String> commonNames = loadCommonNames(
                taxa.stream().map(Taxon::getId).toList(), locale);
        Set<Long> visibleParents = new HashSet<>();
        if (view == TaxonView.SIMPLE) {
            for (Taxon t : taxa) {
                if (simpleChildrenCollector.hasVisibleSimpleChildren(t.getId())) {
                    visibleParents.add(t.getId());
                }
            }
        }
        return taxa.stream()
                .map(t -> {
                    int direct = t.getChildCount();
                    int viewCount = view == TaxonView.FULL
                            ? direct
                            : (visibleParents.contains(t.getId())
                                    ? (int) Math.min(
                                            Math.max(simpleChildrenCollector.countVisible(t.getId()), 1L),
                                            Integer.MAX_VALUE)
                                    : 0);
                    boolean hasChildren = view == TaxonView.FULL ? direct > 0 : viewCount > 0;
                    return new TaxonListItemDto(
                            t.getId(),
                            t.getRank(),
                            t.getScientificName(),
                            commonNames.get(t.getId()),
                            viewCount,
                            hasChildren,
                            t.getRankRaw(),
                            direct);
                })
                .toList();
    }

    private Map<Long, String> loadCommonNames(List<Long> ids, String locale) {
        Map<Long, String> map = new HashMap<>();
        if (ids.isEmpty()) {
            return map;
        }
        List<String> chain = LocaleSupport.fallbackChain(locale);
        List<TaxonI18n> rows = taxonI18nRepository.findByTaxonIdInAndLocaleIn(ids, chain);
        Map<Long, Map<String, String>> byTaxon = new HashMap<>();
        for (TaxonI18n i18n : rows) {
            if (!StringUtils.hasText(i18n.getCommonName())) {
                continue;
            }
            byTaxon
                    .computeIfAbsent(i18n.getTaxon().getId(), k -> new HashMap<>())
                    .put(i18n.getLocale(), i18n.getCommonName());
        }
        for (Long id : ids) {
            Map<String, String> locales = byTaxon.get(id);
            if (locales == null) {
                continue;
            }
            for (String loc : chain) {
                String name = locales.get(loc);
                if (StringUtils.hasText(name)) {
                    map.put(id, name);
                    break;
                }
            }
        }
        return map;
    }

    private MergedI18n mergeI18n(Long taxonId, String preferredLocale) {
        List<String> chain = LocaleSupport.fallbackChain(preferredLocale);
        List<TaxonI18n> rows = taxonI18nRepository.findByTaxonIdAndLocaleIn(taxonId, chain);
        Map<String, TaxonI18n> byLocale = rows.stream()
                .collect(Collectors.toMap(TaxonI18n::getLocale, r -> r, (a, b) -> a));

        String commonName = null;
        String summary = null;
        String description = null;
        String contentLocale = null;
        for (String loc : chain) {
            TaxonI18n row = byLocale.get(loc);
            if (row == null) {
                continue;
            }
            if (commonName == null && StringUtils.hasText(row.getCommonName())) {
                commonName = row.getCommonName();
                if (contentLocale == null) {
                    contentLocale = loc;
                }
            }
            if (summary == null && StringUtils.hasText(row.getSummary())) {
                summary = row.getSummary();
                if (contentLocale == null) {
                    contentLocale = loc;
                }
            }
            if (description == null && StringUtils.hasText(row.getDescription())) {
                description = row.getDescription();
                if (contentLocale == null) {
                    contentLocale = loc;
                }
            }
        }
        return new MergedI18n(commonName, summary, description, contentLocale);
    }

    private List<TaxonBreadcrumbDto> buildBreadcrumbs(Taxon taxon, String locale, TaxonView view) {
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
            if (node == null) {
                continue;
            }
            if (view == TaxonView.SIMPLE && !TaxonRank.LINNAEAN_SEVEN.contains(node.getRank())) {
                continue;
            }
            result.add(new TaxonBreadcrumbDto(
                    node.getId(), node.getRank(), node.getScientificName(), names.get(id)));
        }
        return result;
    }

    private TaxonMediaDto toMediaDto(TaxonMedia media) {
        String url;
        if (StringUtils.hasText(media.getSourceUrl())) {
            url = media.getSourceUrl();
        } else if (StringUtils.hasText(media.getStorageKey())) {
            url = storageService.resolveUrl(media.getStorageKey());
        } else {
            url = "";
        }
        return new TaxonMediaDto(
                media.getId(),
                url,
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
                .orElseThrow(() -> new BusinessException(ErrorCode.TAXON_NOT_FOUND));
    }

    private void assertUniqueName(Long parentId, String scientificName) {
        boolean exists = parentId == null
                ? taxonRepository.existsByParentIsNullAndScientificNameIgnoreCase(scientificName.trim())
                : taxonRepository.existsByParentIdAndScientificNameIgnoreCase(parentId, scientificName.trim());
        if (exists) {
            throw new BusinessException(ErrorCode.DUPLICATE_NAME);
        }
    }

    private static PageRequest pageRequest(int page, int size) {
        return pageRequest(page, size, Sort.by(Sort.Direction.ASC, "scientificName"));
    }

    private static PageRequest childrenPageRequest(int page, int size) {
        return pageRequest(page, size, TaxonSiblingSort.PAGE_SORT);
    }

    private static PageRequest pageRequest(int page, int size, Sort sort) {
        int safePage = Math.max(page, AppConstants.DEFAULT_PAGE);
        int safeSize = size <= 0 ? AppConstants.DEFAULT_PAGE_SIZE : Math.min(size, AppConstants.MAX_PAGE_SIZE);
        return PageRequest.of(safePage, safeSize, sort);
    }

    private record MergedI18n(String commonName, String summary, String description, String contentLocale) {
    }
}
