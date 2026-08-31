/**
 * OSS 存储服务单元测试（不发起真实网络请求）。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
package com.chenxiang.biotree.infrastructure.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.chenxiang.biotree.api.common.BusinessException;
import org.junit.jupiter.api.Test;

class OssFileStorageServiceTest {

    @Test
    void resolveUrlShouldJoinPublicBase() {
        StorageProperties.Oss oss = new StorageProperties.Oss();
        oss.setEndpoint("https://oss-cn-hangzhou.aliyuncs.com");
        oss.setBucket("biotree");
        oss.setAccessKey("ak");
        oss.setSecretKey("sk");
        oss.setPublicBaseUrl("https://cdn.example.com");
        OssFileStorageService service = new OssFileStorageService(oss);
        assertEquals("https://cdn.example.com/taxa/a.png", service.resolveUrl("taxa/a.png"));
    }

    @Test
    void missingConfigShouldFailFast() {
        StorageProperties.Oss oss = new StorageProperties.Oss();
        assertThrows(BusinessException.class, () -> new OssFileStorageService(oss));
    }
}
