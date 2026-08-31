/**
 * 分类搜索：MySQL FULLTEXT 优先，否则前缀/包含 LIKE；结果含异名命中。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
package com.chenxiang.biotree.application;

import com.chenxiang.biotree.domain.taxon.Taxon;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class TaxonSearchDao {

    @PersistenceContext
    private EntityManager entityManager;

    private final JdbcTemplate jdbcTemplate;
    private final boolean mysql;

    public TaxonSearchDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.mysql = detectMysql(jdbcTemplate);
    }

    public boolean isMysql() {
        return mysql;
    }

    public Page<Taxon> search(String term, Collection<String> locales, Pageable pageable) {
        if (mysql && term.trim().length() >= 2) {
            Page<Taxon> fulltext = searchFulltext(term, locales, pageable);
            if (fulltext.getTotalElements() > 0) {
                return fulltext;
            }
        }
        Page<Taxon> prefix = searchLike(term, locales, pageable, true);
        if (prefix.getTotalElements() > 0) {
            return prefix;
        }
        return searchLike(term, locales, pageable, false);
    }

    private Page<Taxon> searchFulltext(String term, Collection<String> locales, Pageable pageable) {
        String booleanQuery = toBooleanQuery(term);
        String localePlaceholders = String.join(",", locales.stream().map(l -> "?").toList());
        String sql =
                """
                SELECT DISTINCT t.id FROM taxon t
                LEFT JOIN taxon_i18n i ON i.taxon_id = t.id AND i.locale IN (%s)
                LEFT JOIN taxon_synonym s ON s.taxon_id = t.id
                WHERE MATCH(t.scientific_name) AGAINST (? IN BOOLEAN MODE)
                   OR (i.common_name IS NOT NULL AND MATCH(i.common_name) AGAINST (? IN BOOLEAN MODE))
                   OR (s.scientific_name IS NOT NULL AND MATCH(s.scientific_name) AGAINST (? IN BOOLEAN MODE))
                ORDER BY t.scientific_name
                LIMIT ? OFFSET ?
                """
                        .formatted(localePlaceholders);
        String countSql =
                """
                SELECT COUNT(DISTINCT t.id) FROM taxon t
                LEFT JOIN taxon_i18n i ON i.taxon_id = t.id AND i.locale IN (%s)
                LEFT JOIN taxon_synonym s ON s.taxon_id = t.id
                WHERE MATCH(t.scientific_name) AGAINST (? IN BOOLEAN MODE)
                   OR (i.common_name IS NOT NULL AND MATCH(i.common_name) AGAINST (? IN BOOLEAN MODE))
                   OR (s.scientific_name IS NOT NULL AND MATCH(s.scientific_name) AGAINST (? IN BOOLEAN MODE))
                """
                        .formatted(localePlaceholders);

        List<Object> args = new java.util.ArrayList<>(locales);
        args.add(booleanQuery);
        args.add(booleanQuery);
        args.add(booleanQuery);
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, args.toArray());
        List<Object> pageArgs = new java.util.ArrayList<>(args);
        pageArgs.add(pageable.getPageSize());
        pageArgs.add(pageable.getOffset());
        List<Long> ids = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getLong(1), pageArgs.toArray());
        if (ids.isEmpty()) {
            return Page.empty(pageable);
        }
        List<Taxon> taxa = loadByIdsPreserveOrder(ids);
        return new PageImpl<>(taxa, pageable, total == null ? 0 : total);
    }

    private Page<Taxon> searchLike(String term, Collection<String> locales, Pageable pageable, boolean prefixOnly) {
        String pattern = prefixOnly ? term.toLowerCase(Locale.ROOT) + "%" : "%" + term.toLowerCase(Locale.ROOT) + "%";
        Query query = entityManager.createQuery(
                """
                SELECT DISTINCT t FROM Taxon t
                LEFT JOIN TaxonI18n i ON i.taxon = t AND i.locale IN :locales
                LEFT JOIN TaxonSynonym s ON s.taxon = t
                WHERE LOWER(t.scientificName) LIKE :pattern
                   OR LOWER(COALESCE(i.commonName, '')) LIKE :pattern
                   OR LOWER(COALESCE(s.scientificName, '')) LIKE :pattern
                ORDER BY t.scientificName
                """);
        query.setParameter("locales", locales);
        query.setParameter("pattern", pattern);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());
        @SuppressWarnings("unchecked")
        List<Taxon> content = query.getResultList();

        Query countQuery = entityManager.createQuery(
                """
                SELECT COUNT(DISTINCT t.id) FROM Taxon t
                LEFT JOIN TaxonI18n i ON i.taxon = t AND i.locale IN :locales
                LEFT JOIN TaxonSynonym s ON s.taxon = t
                WHERE LOWER(t.scientificName) LIKE :pattern
                   OR LOWER(COALESCE(i.commonName, '')) LIKE :pattern
                   OR LOWER(COALESCE(s.scientificName, '')) LIKE :pattern
                """);
        countQuery.setParameter("locales", locales);
        countQuery.setParameter("pattern", pattern);
        long total = (Long) countQuery.getSingleResult();
        return new PageImpl<>(content, pageable, total);
    }

    private List<Taxon> loadByIdsPreserveOrder(List<Long> ids) {
        List<Taxon> found = entityManager
                .createQuery("SELECT t FROM Taxon t WHERE t.id IN :ids", Taxon.class)
                .setParameter("ids", ids)
                .getResultList();
        java.util.Map<Long, Taxon> map = new java.util.HashMap<>();
        for (Taxon t : found) {
            map.put(t.getId(), t);
        }
        List<Taxon> ordered = new java.util.ArrayList<>(ids.size());
        for (Long id : ids) {
            Taxon t = map.get(id);
            if (t != null) {
                ordered.add(t);
            }
        }
        return ordered;
    }

    private static String toBooleanQuery(String term) {
        String cleaned = term.trim().replaceAll("[+\\-><()~*\"@]+", " ").trim();
        if (cleaned.isEmpty()) {
            return term.trim() + "*";
        }
        String[] parts = cleaned.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.length() < 2) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append('+').append(part).append('*');
        }
        return sb.isEmpty() ? cleaned + "*" : sb.toString();
    }

    private static boolean detectMysql(JdbcTemplate jdbcTemplate) {
        try {
            var ds = jdbcTemplate.getDataSource();
            if (ds == null) {
                return false;
            }
            try (var connection = ds.getConnection()) {
                String product = connection.getMetaData().getDatabaseProductName();
                return product != null && product.toLowerCase(Locale.ROOT).contains("mysql");
            }
        } catch (Exception ex) {
            return false;
        }
    }
}
