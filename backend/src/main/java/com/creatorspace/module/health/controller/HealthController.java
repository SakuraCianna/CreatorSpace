package com.creatorspace.module.health.controller;

import com.creatorspace.common.result.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public ApiResponse<Map<String, Object>> health() {
        return ApiResponse.ok(Map.of(
                "application", "CreatorSpace",
                "status", "UP",
                "timestamp", OffsetDateTime.now().toString()
        ));
    }
}
