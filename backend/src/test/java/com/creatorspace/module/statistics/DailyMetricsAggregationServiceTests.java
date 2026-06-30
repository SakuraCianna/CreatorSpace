package com.creatorspace.module.statistics;

import com.creatorspace.common.cache.RedisJsonCacheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DailyMetricsAggregationServiceTests {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final RedisJsonCacheService redisCacheService = mock(RedisJsonCacheService.class);
    private final DailyMetricsAggregationService service = new DailyMetricsAggregationService(
            jdbcTemplate,
            new ObjectMapper(),
            redisCacheService,
            "Asia/Shanghai"
    );

    @Test
    void aggregateWritesDailyCountsAndHotContentSnapshots() throws Exception {
        LocalDate metricDate = LocalDate.of(2026, 6, 29);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(metricDate)))
                .thenReturn(11L, 7L, 5L, 3L, 2L, 1L, 4L);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
                .thenAnswer(invocation -> {
                    RowMapper<?> mapper = invocation.getArgument(1);
                    ResultSet rs = mock(ResultSet.class);
                    when(rs.getString("target_type")).thenReturn("ARTICLE");
                    when(rs.getLong("target_id")).thenReturn(9L);
                    when(rs.getString("title")).thenReturn("Hot item");
                    when(rs.getString("slug")).thenReturn("hot-item");
                    when(rs.getLong("view_count")).thenReturn(100L);
                    when(rs.getLong("like_count")).thenReturn(20L);
                    when(rs.getLong("comment_count")).thenReturn(4L);
                    when(rs.getLong("favorite_count")).thenReturn(3L);
                    when(rs.getLong("hot_score")).thenReturn(176L);
                    return List.of(mapper.mapRow(rs, 0));
                });

        DailyMetricsAggregationService.AggregationResult result = service.aggregate(metricDate);

        assertThat(result.metricDate()).isEqualTo(metricDate);
        assertThat(result.metrics()).extracting(DailyMetricsAggregationService.DailyMetricVO::metricKey)
                .containsExactly(
                        DailyMetricKeys.SITE_PV,
                        DailyMetricKeys.SITE_UV,
                        DailyMetricKeys.SEARCH_COUNT,
                        DailyMetricKeys.COMMENT_COUNT,
                        DailyMetricKeys.LIKE_COUNT,
                        DailyMetricKeys.FAVORITE_COUNT,
                        DailyMetricKeys.OPERATION_COUNT,
                        DailyMetricKeys.HOT_ARTICLES,
                        DailyMetricKeys.HOT_PROJECTS
                );

        ArgumentCaptor<Object> metricKeyCaptor = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<Object> metricValueCaptor = ArgumentCaptor.forClass(Object.class);
        verify(jdbcTemplate, org.mockito.Mockito.times(9)).update(
                anyString(),
                eq(metricDate),
                metricKeyCaptor.capture(),
                metricValueCaptor.capture(),
                anyString()
        );
        assertThat(metricKeyCaptor.getAllValues()).contains(DailyMetricKeys.SITE_PV, DailyMetricKeys.HOT_ARTICLES);
        assertThat(metricValueCaptor.getAllValues()).contains(11L, 1L);
        verify(redisCacheService).evict("creatorspace:cache:dashboard:overview");
    }
}