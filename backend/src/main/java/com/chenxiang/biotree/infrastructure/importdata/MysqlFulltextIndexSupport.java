/**
 * 导入结束后尽力恢复 MySQL FULLTEXT；失败只记日志，不掩盖落库异常。
 *
 * Author: chen-xiang
 * Created: 2026-09-03
 */
package com.chenxiang.biotree.infrastructure.importdata;

import org.slf4j.Logger;

final class MysqlFulltextIndexSupport {

    private MysqlFulltextIndexSupport() {}

    static void restoreQuietly(Runnable restore, Logger log) {
        try {
            restore.run();
        } catch (RuntimeException ex) {
            log.warn("Could not restore MySQL FULLTEXT indexes; search will use LIKE fallback", ex);
        }
    }
}
