/**
 * OSS 对象存储实现占位（后续接入具体 SDK）。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
package com.chenxiang.biotree.infrastructure.storage;

import com.chenxiang.biotree.api.common.BusinessException;
import com.chenxiang.biotree.api.common.ErrorCode;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OssFileStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(OssFileStorageService.class);

    private final StorageProperties.Oss oss;

    public OssFileStorageService(StorageProperties.Oss oss) {
        this.oss = oss;
    }

    @Override
    public String store(String suggestedFileName, String contentType, InputStream content, long sizeBytes) {
        log.error("OSS storage is not implemented yet");
        throw new BusinessException(ErrorCode.INTERNAL_ERROR, "OSS storage is not implemented yet");
    }

    @Override
    public void delete(String storageKey) {
        log.error("OSS storage is not implemented yet, key={}", storageKey);
        throw new BusinessException(ErrorCode.INTERNAL_ERROR, "OSS storage is not implemented yet");
    }

    @Override
    public String resolveUrl(String storageKey) {
        String base = oss.getPublicBaseUrl();
        if (base == null || base.isBlank()) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "OSS public base URL is not configured");
        }
        if (base.endsWith("/")) {
            return base + storageKey;
        }
        return base + "/" + storageKey;
    }
}
