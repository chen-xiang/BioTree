/**
 * 阿里云 OSS 对象存储实现。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 * Updated: 2026-08-31 接入阿里云 OSS SDK 完成上传/删除/URL 解析
 */
package com.chenxiang.biotree.infrastructure.storage;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.chenxiang.biotree.api.common.BusinessException;
import com.chenxiang.biotree.api.common.ErrorCode;
import java.io.InputStream;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

public class OssFileStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(OssFileStorageService.class);

    private final StorageProperties.Oss oss;

    public OssFileStorageService(StorageProperties.Oss oss) {
        this.oss = oss;
        validateConfig();
    }

    @Override
    public String store(String suggestedFileName, String contentType, InputStream content, long sizeBytes) {
        String extension = extractExtension(suggestedFileName);
        String key = "taxa/" + UUID.randomUUID().toString().replace("-", "") + extension;
        OSS client = createClient();
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            if (sizeBytes > 0) {
                metadata.setContentLength(sizeBytes);
            }
            if (StringUtils.hasText(contentType)) {
                metadata.setContentType(contentType);
            }
            client.putObject(oss.getBucket(), key, content, metadata);
            log.info("Stored OSS object key={} sizeBytes={}", key, sizeBytes);
            return key;
        } catch (Exception ex) {
            log.error("Failed to store OSS object key={}", key, ex);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Failed to store file to OSS");
        } finally {
            client.shutdown();
        }
    }

    @Override
    public void delete(String storageKey) {
        if (!StringUtils.hasText(storageKey)) {
            return;
        }
        OSS client = createClient();
        try {
            client.deleteObject(oss.getBucket(), storageKey);
            log.info("Deleted OSS object key={}", storageKey);
        } catch (Exception ex) {
            log.error("Failed to delete OSS object key={}", storageKey, ex);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Failed to delete file from OSS");
        } finally {
            client.shutdown();
        }
    }

    @Override
    public String resolveUrl(String storageKey) {
        String base = oss.getPublicBaseUrl();
        if (!StringUtils.hasText(base)) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "OSS public base URL is not configured");
        }
        if (base.endsWith("/")) {
            return base + storageKey;
        }
        return base + "/" + storageKey;
    }

    private OSS createClient() {
        return new OSSClientBuilder().build(oss.getEndpoint(), oss.getAccessKey(), oss.getSecretKey());
    }

    private void validateConfig() {
        if (!StringUtils.hasText(oss.getEndpoint())
                || !StringUtils.hasText(oss.getBucket())
                || !StringUtils.hasText(oss.getAccessKey())
                || !StringUtils.hasText(oss.getSecretKey())) {
            throw new BusinessException(
                    ErrorCode.INTERNAL_ERROR,
                    "OSS storage requires endpoint, bucket, access-key and secret-key");
        }
    }

    private static String extractExtension(String fileName) {
        if (!StringUtils.hasText(fileName) || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.'));
    }
}
