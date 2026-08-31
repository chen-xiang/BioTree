/**
 * 全库或子树回填 simple_parent_id。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
package com.chenxiang.biotree.application;

import com.chenxiang.biotree.domain.taxon.Taxon;
import com.chenxiang.biotree.domain.taxon.TaxonRank;
import com.chenxiang.biotree.infrastructure.persistence.TaxonRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SimpleParentRebuilder {

    private static final Logger log = LoggerFactory.getLogger(SimpleParentRebuilder.class);

    private final TaxonRepository taxonRepository;
    private final JdbcTemplate jdbcTemplate;

    public SimpleParentRebuilder(TaxonRepository taxonRepository, JdbcTemplate jdbcTemplate) {
        this.taxonRepository = taxonRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void backfillIfNeeded() {
        Integer pending = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM taxon
                WHERE parent_id IS NOT NULL AND simple_parent_id IS NULL
                """,
                Integer.class);
        if (pending == null || pending == 0) {
            return;
        }
        log.info("Backfilling simple_parent_id for {} taxa", pending);
        rebuildAll();
    }

    @Transactional
    public void rebuildAll() {
        List<Taxon> all = taxonRepository.findAll();
        Map<Long, Taxon> byId = new HashMap<>();
        for (Taxon t : all) {
            byId.put(t.getId(), t);
        }
        List<Object[]> updates = new ArrayList<>();
        for (Taxon t : all) {
            Long simpleId = SimpleParentSupport.nearestLinnaeanAncestorId(t, byId);
            updates.add(new Object[] {simpleId, t.getId()});
        }
        for (int i = 0; i < updates.size(); i += 500) {
            List<Object[]> chunk = updates.subList(i, Math.min(i + 500, updates.size()));
            jdbcTemplate.batchUpdate("UPDATE taxon SET simple_parent_id = ? WHERE id = ?", chunk);
        }
        log.info("Rebuilt simple_parent_id for {} taxa", updates.size());
    }

    @Transactional
    public void rebuildSubtree(String pathPrefix) {
        List<Taxon> subtree = taxonRepository.findByMaterializedPathStartingWith(pathPrefix);
        if (subtree.isEmpty()) {
            return;
        }
        Map<Long, Taxon> byId = new HashMap<>();
        for (Taxon t : subtree) {
            byId.put(t.getId(), t);
        }
        // 祖先可能在子树外
        for (Taxon t : subtree) {
            for (Long aid : SimpleParentSupport.ancestorIds(t.getMaterializedPath(), t.getId())) {
                if (!byId.containsKey(aid)) {
                    taxonRepository.findById(aid).ifPresent(a -> byId.put(a.getId(), a));
                }
            }
        }
        for (Taxon t : subtree) {
            Long simpleId = SimpleParentSupport.nearestLinnaeanAncestorId(t, byId);
            if (simpleId == null) {
                t.setSimpleParent(null);
            } else {
                t.setSimpleParent(byId.get(simpleId));
            }
        }
        taxonRepository.saveAll(subtree);
    }

    public void assignForNewNode(Taxon taxon) {
        Taxon cursor = taxon.getParent();
        while (cursor != null) {
            if (TaxonRank.LINNAEAN_SEVEN.contains(cursor.getRank())) {
                taxon.setSimpleParent(cursor);
                return;
            }
            cursor = cursor.getParent();
        }
        taxon.setSimpleParent(null);
    }
}
