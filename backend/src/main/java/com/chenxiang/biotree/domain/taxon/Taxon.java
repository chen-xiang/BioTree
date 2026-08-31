/**
 * 分类单元实体：邻接表树节点。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 * Updated: 2026-08-31 增加外部数据源 ID 字段以支持权威库导入
 * Updated: 2026-08-31 列名改为 taxon_rank 以兼容 MySQL 保留字
 */
package com.chenxiang.biotree.domain.taxon;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "taxon")
public class Taxon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Taxon parent;

    @Enumerated(EnumType.STRING)
    @Column(name = "taxon_rank", nullable = false, length = 32)
    private TaxonRank rank;

    @Column(name = "rank_order", nullable = false)
    private int rankOrder;

    @Column(name = "taxon_rank_raw", length = 64)
    private String rankRaw;

    @Column(name = "scientific_name", nullable = false, length = 255)
    private String scientificName;

    @Column(name = "scientific_name_authorship", length = 255)
    private String scientificNameAuthorship;

    @Column(name = "scientific_name_verbatim", length = 512)
    private String scientificNameVerbatim;

    @Column(name = "name_published_in", length = 512)
    private String namePublishedIn;

    @Column(name = "name_according_to", length = 512)
    private String nameAccordingTo;

    @Column(name = "nomenclatural_code", length = 32)
    private String nomenclaturalCode;

    @Column(name = "nomenclatural_status", length = 64)
    private String nomenclaturalStatus;

    @Column(name = "materialized_path", nullable = false, length = 768)
    private String materializedPath = "/";

    @Column(name = "child_count", nullable = false)
    private int childCount;

    @Column(name = "is_accepted", nullable = false)
    private boolean accepted = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "created_by", length = 64)
    private String createdBy;

    @Column(name = "external_source", length = 32)
    private String externalSource;

    @Column(name = "external_id", length = 64)
    private String externalId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Taxon getParent() {
        return parent;
    }

    public void setParent(Taxon parent) {
        this.parent = parent;
    }

    public TaxonRank getRank() {
        return rank;
    }

    public void setRank(TaxonRank rank) {
        this.rank = rank;
        if (rank != null) {
            this.rankOrder = rank.getRankOrder();
        }
    }

    public int getRankOrder() {
        return rankOrder;
    }

    public void setRankOrder(int rankOrder) {
        this.rankOrder = rankOrder;
    }

    public String getRankRaw() {
        return rankRaw;
    }

    public void setRankRaw(String rankRaw) {
        this.rankRaw = rankRaw;
    }

    public String getScientificName() {
        return scientificName;
    }

    public void setScientificName(String scientificName) {
        this.scientificName = scientificName;
    }

    public String getScientificNameAuthorship() {
        return scientificNameAuthorship;
    }

    public void setScientificNameAuthorship(String scientificNameAuthorship) {
        this.scientificNameAuthorship = scientificNameAuthorship;
    }

    public String getScientificNameVerbatim() {
        return scientificNameVerbatim;
    }

    public void setScientificNameVerbatim(String scientificNameVerbatim) {
        this.scientificNameVerbatim = scientificNameVerbatim;
    }

    public String getNamePublishedIn() {
        return namePublishedIn;
    }

    public void setNamePublishedIn(String namePublishedIn) {
        this.namePublishedIn = namePublishedIn;
    }

    public String getNameAccordingTo() {
        return nameAccordingTo;
    }

    public void setNameAccordingTo(String nameAccordingTo) {
        this.nameAccordingTo = nameAccordingTo;
    }

    public String getNomenclaturalCode() {
        return nomenclaturalCode;
    }

    public void setNomenclaturalCode(String nomenclaturalCode) {
        this.nomenclaturalCode = nomenclaturalCode;
    }

    public String getNomenclaturalStatus() {
        return nomenclaturalStatus;
    }

    public void setNomenclaturalStatus(String nomenclaturalStatus) {
        this.nomenclaturalStatus = nomenclaturalStatus;
    }

    public String getMaterializedPath() {
        return materializedPath;
    }

    public void setMaterializedPath(String materializedPath) {
        this.materializedPath = materializedPath;
    }

    public int getChildCount() {
        return childCount;
    }

    public void setChildCount(int childCount) {
        this.childCount = childCount;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getExternalSource() {
        return externalSource;
    }

    public void setExternalSource(String externalSource) {
        this.externalSource = externalSource;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }
}
