package com.creatorspace.module.health.controller;

import com.creatorspace.common.result.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * 提供应用健康检查接口。
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    // 返回应用健康状态。
    @GetMapping
    public ApiResponse<Map<String, Object>> health() {
        return ApiResponse.ok(Map.of(
                "application", "CreatorSpace",
                "status", "UP",
                "timestamp", OffsetDateTime.now().toString()
        ));
    }
}
