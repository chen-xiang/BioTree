/**
 * 文件存储统一接口（Local / OSS 可切换）。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
package com.chenxiang.biotree.infrastructure.storage;

import java.io.InputStream;

public interface StorageService {

    /**
     * 保存文件并返回存储键。
     */
    String store(String suggestedFileName, String contentType, InputStream content, long sizeBytes);

    /**
     * 删除指定存储键对应的对象。
     */
    void delete(String storageKey);

    /**
     * 生成可访问 URL（本地为相对/受控路径，OSS 为对象 URL）。
     */
    String resolveUrl(String storageKey);
}
