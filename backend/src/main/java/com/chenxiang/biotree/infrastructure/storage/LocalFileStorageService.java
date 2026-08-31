/**
 * 本地磁盘文件存储实现。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
package com.chenxiang.biotree.infrastructure.storage;

import com.chenxiang.biotree.api.common.BusinessException;
import com.chenxiang.biotree.api.common.ErrorCode;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

public class LocalFileStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalFileStorageService.class);

    private final StorageProperties.Local local;

    public LocalFileStorageService(StorageProperties.Local local) {
        this.local = local;
    }

    @Override
    public String store(String suggestedFileName, String contentType, InputStream content, long sizeBytes) {
        String extension = extractExtension(suggestedFileName);
        String key = UUID.randomUUID().toString().replace("-", "") + extension;
        Path target = Path.of(local.getBasePath()).resolve(key).normalize();
        ensureWithinBase(target);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
            log.info("Stored local file key={} sizeBytes={}", key, sizeBytes);
            return key;
        } catch (IOException ex) {
            log.error("Failed to store local file key={}", key, ex);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Failed to store file");
        }
    }

    @Override
    public void delete(String storageKey) {
        Path target = Path.of(local.getBasePath()).resolve(storageKey).normalize();
        ensureWithinBase(target);
        try {
            Files.deleteIfExists(target);
            log.info("Deleted local file key={}", storageKey);
        } catch (IOException ex) {
            log.error("Failed to delete local file key={}", storageKey, ex);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Failed to delete file");
        }
    }

    @Override
    public String resolveUrl(String storageKey) {
        String base = local.getPublicBaseUrl();
        if (base.endsWith("/")) {
            return base + storageKey;
        }
        return base + "/" + storageKey;
    }

    private void ensureWithinBase(Path target) {
        Path base = Path.of(local.getBasePath()).toAbsolutePath().normalize();
        if (!target.toAbsolutePath().normalize().startsWith(base)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Invalid storage key");
        }
    }

    private static String extractExtension(String fileName) {
        if (!StringUtils.hasText(fileName) || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.'));
    }
}
