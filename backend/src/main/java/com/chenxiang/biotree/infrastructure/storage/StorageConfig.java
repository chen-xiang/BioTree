/**
 * 按配置装配 Local 或 OSS 存储实现。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
package com.chenxiang.biotree.infrastructure.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageConfig {

    private static final Logger log = LoggerFactory.getLogger(StorageConfig.class);

    @Bean
    public StorageService storageService(StorageProperties properties) {
        String type = properties.getType() == null ? "local" : properties.getType().trim().toLowerCase();
        if ("oss".equals(type)) {
            log.info("Using OSS file storage");
            return new OssFileStorageService(properties.getOss());
        }
        log.info("Using local file storage basePath={}", properties.getLocal().getBasePath());
        return new LocalFileStorageService(properties.getLocal());
    }
}
