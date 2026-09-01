/**
 * 健康与就绪探测用的公开接口（骨架）。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 * Updated: 2026-09-01 仅 Web 进程加载
 */
package com.chenxiang.biotree.api;

import com.chenxiang.biotree.api.common.ApiResponse;
import com.chenxiang.biotree.api.common.WebOnly;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@WebOnly
@RestController
@RequestMapping("/api")
public class SystemController {

    @GetMapping("/health")
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.ok(Map.of("status", "UP"));
    }
}
