/**
 * 公开只读 API 的短 TTL Cache-Control。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
package com.chenxiang.biotree.api.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class PublicCacheInterceptor implements HandlerInterceptor {

    private final int maxAgeSeconds;

    public PublicCacheInterceptor(@Value("${app.cache.public-max-age-seconds:60}") int maxAgeSeconds) {
        this.maxAgeSeconds = maxAgeSeconds;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (maxAgeSeconds <= 0) {
            return true;
        }
        if (!HttpMethod.GET.matches(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        if (path.startsWith("/api/taxa") && !path.startsWith("/api/admin")) {
            response.setHeader("Cache-Control", "public, max-age=" + maxAgeSeconds);
        }
        return true;
    }
}
