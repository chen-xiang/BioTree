/**
 * Catalogue of Life DwC-A 导入配置。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
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

    /** 导入界过滤，默认动物界与植物界。 */
    private List<String> kingdoms = new ArrayList<>(List.of("Animalia", "Plantae"));

    /** 每种等级最多导入条数；0 表示不限制。 */
    private int maxPerRank;

    /** 俗名语言：eng→en，zho/zh/chi→zh-CN。 */
    private boolean importVernaculars = true;

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
}
