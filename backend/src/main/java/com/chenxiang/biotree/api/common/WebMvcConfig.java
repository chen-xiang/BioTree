/**
 * Web MVC 拦截器注册。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
package com.chenxiang.biotree.api.common;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final PublicCacheInterceptor publicCacheInterceptor;

    public WebMvcConfig(PublicCacheInterceptor publicCacheInterceptor) {
        this.publicCacheInterceptor = publicCacheInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(publicCacheInterceptor).addPathPatterns("/api/taxa/**");
    }
}
