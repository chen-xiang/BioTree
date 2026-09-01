/**
 * CoL 导入独立入口：非 Web 进程，导入结束后退出。
 *
 * Author: chen-xiang
 * Created: 2026-09-01
 */
package com.chenxiang.biotree;

import com.chenxiang.biotree.infrastructure.importdata.ColImportLauncher;

public final class ImportApplication {

    private ImportApplication() {}

    public static void main(String[] args) {
        System.exit(ColImportLauncher.launch(args));
    }
}
