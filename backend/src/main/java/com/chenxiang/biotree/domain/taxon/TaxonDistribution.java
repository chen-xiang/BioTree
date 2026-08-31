/**
 * 分类分布记录（国家/地区级，来自 DwC Distribution 扩展）。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
package com.chenxiang.biotree.domain.taxon;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "taxon_distribution")
public class TaxonDistribution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "taxon_id", nullable = false)
    private Taxon taxon;

    @Column(name = "country_code", length = 16)
    private String countryCode;

    @Column(length = 512)
    private String locality;

    @Column(name = "establishment_means", length = 64)
    private String establishmentMeans;

    @Column(name = "threat_status", length = 64)
    private String threatStatus;

    @Column(name = "source_text", length = 512)
    private String sourceText;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Taxon getTaxon() {
        return taxon;
    }

    public void setTaxon(Taxon taxon) {
        this.taxon = taxon;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getLocality() {
        return locality;
    }

    public void setLocality(String locality) {
        this.locality = locality;
    }

    public String getEstablishmentMeans() {
        return establishmentMeans;
    }

    public void setEstablishmentMeans(String establishmentMeans) {
        this.establishmentMeans = establishmentMeans;
    }

    public String getThreatStatus() {
        return threatStatus;
    }

    public void setThreatStatus(String threatStatus) {
        this.threatStatus = threatStatus;
    }

    public String getSourceText() {
        return sourceText;
    }

    public void setSourceText(String sourceText) {
        this.sourceText = sourceText;
    }
}
