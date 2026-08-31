/**
 * 导入断点只读状态 API。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
package com.chenxiang.biotree.api.admin;

import com.chenxiang.biotree.api.common.ApiResponse;
import com.chenxiang.biotree.infrastructure.importdata.ImportCheckpointRepository;
import com.chenxiang.biotree.infrastructure.importdata.ImportProperties;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/import")
public class AdminImportController {

    private final ImportCheckpointRepository checkpointRepository;
    private final ImportProperties importProperties;

    public AdminImportController(
            ImportCheckpointRepository checkpointRepository, ImportProperties importProperties) {
        this.checkpointRepository = checkpointRepository;
        this.importProperties = importProperties;
    }

    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> status() {
        String jobKey = importProperties.getJobKey() == null ? "col" : importProperties.getJobKey();
        return checkpointRepository
                .find(jobKey)
                .map(cp -> ApiResponse.ok(Map.<String, Object>of(
                        "jobKey", cp.jobKey(),
                        "phase", cp.phase(),
                        "processedCount", cp.processedCount(),
                        "totalHint", cp.totalHint() == null ? 0 : cp.totalHint(),
                        "detail", cp.detailJson() == null ? "" : cp.detailJson(),
                        "running", true)))
                .orElseGet(() -> ApiResponse.ok(Map.of(
                        "jobKey", jobKey,
                        "phase", "IDLE",
                        "processedCount", 0,
                        "totalHint", 0,
                        "detail", "",
                        "running", false)));
    }
}
