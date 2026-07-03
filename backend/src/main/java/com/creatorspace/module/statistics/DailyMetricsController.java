package com.creatorspace.module.statistics;

import com.creatorspace.common.result.ApiResponse;
import com.creatorspace.common.result.PageResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Validated
@RestController
@RequestMapping("/api/admin/daily-metrics")
public class DailyMetricsController {

    private final DailyMetricsAggregationService aggregationService;

    public DailyMetricsController(DailyMetricsAggregationService aggregationService) {
        this.aggregationService = aggregationService;
    }

    @PostMapping("/aggregate")
    public ApiResponse<DailyMetricsAggregationService.AggregationResult> aggregate(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDate targetDate = date == null ? LocalDate.now().minusDays(1) : date;
        return ApiResponse.ok(aggregationService.aggregate(targetDate));
    }

    @GetMapping
    public ApiResponse<PageResponse<DailyMetricsAggregationService.DailyMetricVO>> list(
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String metricKey,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize
    ) {
        if (startDate.isAfter(endDate)) {
            throw new ResponseStatusException(BAD_REQUEST, "startDate must be before or equal to endDate");
        }
        return ApiResponse.ok(aggregationService.list(startDate, endDate, metricKey, page, pageSize));
    }
}
