/**
 * 公开统计摘要 API。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
package com.chenxiang.biotree.api.stats;

import com.chenxiang.biotree.api.common.ApiResponse;
import com.chenxiang.biotree.application.StatsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/summary")
    public ApiResponse<StatsSummaryDto> summary() {
        return ApiResponse.ok(statsService.summary());
    }
}
