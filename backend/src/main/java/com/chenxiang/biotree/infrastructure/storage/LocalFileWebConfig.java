/**
 * 本地文件对外访问映射（/files/**）。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 * Updated: 2026-09-01 仅 Web 进程映射 /files/**
 */
package com.chenxiang.biotree.infrastructure.storage;

import java.nio.file.Path;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@ConditionalOnWebApplication
public class LocalFileWebConfig implements WebMvcConfigurer {

    private final StorageProperties storageProperties;

    public LocalFileWebConfig(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        if (!"local".equalsIgnoreCase(storageProperties.getType())) {
            return;
        }
        String location = Path.of(storageProperties.getLocal().getBasePath())
                .toAbsolutePath()
                .normalize()
                .toUri()
                .toString();
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        registry.addResourceHandler("/files/**").addResourceLocations(location);
    }
}
