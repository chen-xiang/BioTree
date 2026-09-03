/**
 * Catalogue of Life DwC-A 导入配置。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 * Updated: 2026-08-31 增加断点续跑、异名导入与提交批次配置
 * Updated: 2026-09-01 enabled 仅对 importCol / ImportApplication 生效
 * Updated: 2026-09-01 默认导入 CoL 七界
 * Updated: 2026-09-02 默认提交批次改为 2000
 * Updated: 2026-09-02 默认界名含 CoL 现行原核界（Bacillati 等）
 */
package com.chenxiang.biotree.infrastructure.importdata;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.import")
public class ImportProperties {

    /** 是否执行导入（仅 ImportApplication / gradle importCol；Web 进程忽略）。 */
    private boolean enabled;

    /** DwC-A zip 路径。 */
    private String dwcaPath = "";

    /** 是否清空既有分类/多语言/媒体后再导入。 */
    private boolean replace;

    /**
     * 是否断点续跑：存在未完成 checkpoint 时跳过 replace 清空，并跳过已写入的 external_id。
     */
    private boolean resume = true;

    /**
     * 默认导入的界：林奈/CoL 传统七界，外加 2024 年后 CoL 使用的原核界名。
     * 现行包里已无 Bacteria/Archaea 列值，仍保留以便旧包兼容。
     */
    public static final List<String> DEFAULT_KINGDOMS = List.of(
            "Animalia",
            "Archaea",
            "Bacteria",
            "Chromista",
            "Fungi",
            "Plantae",
            "Protozoa",
            "Bacillati",
            "Fusobacteriati",
            "Pseudomonadati",
            "Thermotogati",
            "Methanobacteriati",
            "Nanobdellati",
            "Promethearchaeati",
            "Thermoproteati");

    /** 导入界过滤，默认 CoL 七界。 */
    private List<String> kingdoms = new ArrayList<>(DEFAULT_KINGDOMS);

    /** 每种等级最多导入条数；0 表示不限制。 */
    private int maxPerRank;

    /** 俗名语言：eng→en，zho/zh/chi→zh-CN。 */
    private boolean importVernaculars = true;

    /** 是否导入异名（acceptedNameUsageID 指向已导入接受名）。 */
    private boolean importSynonyms = true;

    /** 是否导入 Description 扩展到 taxon_i18n（不覆盖已有非空 description）。 */
    private boolean importDescriptions = true;

    /** 是否导入 Distribution 扩展。 */
    private boolean importDistributions = true;

    /** 是否导入 Multimedia/Media 外链（须含 license）。 */
    private boolean importMedia = true;

    /** 每批提交的插入条数（小事务，便于断点）。 */
    private int commitBatchSize = 2000;

    /** checkpoint 任务键。 */
    private String jobKey = "col";

    /**
     * 等级模式：full=含中间级；legacy7=仅林奈七级（旧行为）。
     */
    private String rankMode = "full";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDwcaPath() {
        return dwcaPath;
    }

    public void setDwcaPath(String dwcaPath) {
        this.dwcaPath = dwcaPath;
    }

    public boolean isReplace() {
        return replace;
    }

    public void setReplace(boolean replace) {
        this.replace = replace;
    }

    public boolean isResume() {
        return resume;
    }

    public void setResume(boolean resume) {
        this.resume = resume;
    }

    public List<String> getKingdoms() {
        return kingdoms;
    }

    public void setKingdoms(List<String> kingdoms) {
        this.kingdoms = kingdoms;
    }

    public int getMaxPerRank() {
        return maxPerRank;
    }

    public void setMaxPerRank(int maxPerRank) {
        this.maxPerRank = maxPerRank;
    }

    public boolean isImportVernaculars() {
        return importVernaculars;
    }

    public void setImportVernaculars(boolean importVernaculars) {
        this.importVernaculars = importVernaculars;
    }

    public boolean isImportSynonyms() {
        return importSynonyms;
    }

    public void setImportSynonyms(boolean importSynonyms) {
        this.importSynonyms = importSynonyms;
    }

    public boolean isImportDescriptions() {
        return importDescriptions;
    }

    public void setImportDescriptions(boolean importDescriptions) {
        this.importDescriptions = importDescriptions;
    }

    public boolean isImportDistributions() {
        return importDistributions;
    }

    public void setImportDistributions(boolean importDistributions) {
        this.importDistributions = importDistributions;
    }

    public boolean isImportMedia() {
        return importMedia;
    }

    public void setImportMedia(boolean importMedia) {
        this.importMedia = importMedia;
    }

    public int getCommitBatchSize() {
        return commitBatchSize;
    }

    public void setCommitBatchSize(int commitBatchSize) {
        this.commitBatchSize = commitBatchSize;
    }

    public String getJobKey() {
        return jobKey;
    }

    public void setJobKey(String jobKey) {
        this.jobKey = jobKey;
    }

    public String getRankMode() {
        return rankMode;
    }

    public void setRankMode(String rankMode) {
        this.rankMode = rankMode;
    }

    public boolean isLegacySevenRanks() {
        return rankMode != null && rankMode.trim().equalsIgnoreCase("legacy7");
    }
}
