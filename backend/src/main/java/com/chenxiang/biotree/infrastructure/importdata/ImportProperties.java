/**
 * Catalogue of Life DwC-A 导入配置。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 * Updated: 2026-08-31 增加断点续跑、异名导入与提交批次配置
 */
package com.chenxiang.biotree.infrastructure.importdata;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.import")
public class ImportProperties {

    /** 是否启用导入（启用后启动即执行并退出）。 */
    private boolean enabled;

    /** DwC-A zip 路径。 */
    private String dwcaPath = "";

    /** 是否清空既有分类/多语言/媒体后再导入。 */
    private boolean replace;

    /**
     * 是否断点续跑：存在未完成 checkpoint 时跳过 replace 清空，并跳过已写入的 external_id。
     */
    private boolean resume = true;

    /** 导入界过滤，默认动物界与植物界。 */
    private List<String> kingdoms = new ArrayList<>(List.of("Animalia", "Plantae"));

    /** 每种等级最多导入条数；0 表示不限制。 */
    private int maxPerRank;

    /** 俗名语言：eng→en，zho/zh/chi→zh-CN。 */
    private boolean importVernaculars = true;

    /** 是否导入异名（acceptedNameUsageID 指向已导入接受名）。 */
    private boolean importSynonyms = true;

    /** 每批提交的插入条数（小事务，便于断点）。 */
    private int commitBatchSize = 500;

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
