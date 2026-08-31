/**
 * 本地存储服务单元测试。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
package com.chenxiang.biotree.infrastructure.storage;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalFileStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void storeAndDeleteShouldWork() throws Exception {
        StorageProperties.Local local = new StorageProperties.Local();
        local.setBasePath(tempDir.toString());
        local.setPublicBaseUrl("/files");
        LocalFileStorageService service = new LocalFileStorageService(local);

        byte[] bytes = "hello".getBytes(StandardCharsets.UTF_8);
        String key = service.store("demo.txt", "text/plain", new ByteArrayInputStream(bytes), bytes.length);
        assertTrue(Files.exists(tempDir.resolve(key)));
        assertTrue(service.resolveUrl(key).endsWith(key));

        service.delete(key);
        assertTrue(Files.notExists(tempDir.resolve(key)));
    }
}
