/**
 * 全文索引恢复失败不得向外抛出。
 *
 * Author: chen-xiang
 * Created: 2026-09-03
 */
package com.chenxiang.biotree.infrastructure.importdata;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

class MysqlFulltextIndexSupportTest {

    @Test
    void restoreQuietlySwallowsRuntimeException() {
        Logger log = mock(Logger.class);
        assertDoesNotThrow(() -> MysqlFulltextIndexSupport.restoreQuietly(
                () -> {
                    throw new IllegalStateException("Got error 1000 - InnoDB error");
                },
                log));
    }
}
