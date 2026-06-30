package com.creatorspace.module.dashboard;

import com.creatorspace.common.cache.CacheKeys;
import com.creatorspace.common.cache.RedisJsonCacheService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
class DashboardControllerTests {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final RedisJsonCacheService redisCacheService = mock(RedisJsonCacheService.class);
    private final DashboardController controller = new DashboardController(jdbcTemplate, redisCacheService);

    @Test
    void trendCanReadDailyMetricsSnapshot() throws Exception {
        when(redisCacheService.read(CacheKeys.DASHBOARD_OVERVIEW, DashboardController.DashboardOverviewVO.class))
                .thenReturn(Optional.empty());
        when(redisCacheService.read(eq(CacheKeys.HOT_ARTICLES), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenReturn(Optional.of(List.of()));
        when(redisCacheService.read(eq(CacheKeys.HOT_PROJECTS), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenReturn(Optional.of(List.of()));
        when(redisCacheService.read(eq(CacheKeys.HOT_SEARCH_KEYWORDS), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenReturn(Optional.of(List.of()));
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(0L);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq("site.pv")))
                .thenAnswer(invocation -> {
                    RowMapper<?> mapper = invocation.getArgument(1);
                    ResultSet rs = mock(ResultSet.class);
                    when(rs.getObject("metric_date", LocalDate.class)).thenReturn(LocalDate.of(2026, 6, 29));
                    when(rs.getLong("pv")).thenReturn(12L);
                    return List.of(mapper.mapRow(rs, 0));
                });
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq("search.count")))
                .thenAnswer(invocation -> {
                    RowMapper<?> mapper = invocation.getArgument(1);
                    ResultSet rs = mock(ResultSet.class);
                    when(rs.getObject("metric_date", LocalDate.class)).thenReturn(LocalDate.of(2026, 6, 29));
                    when(rs.getLong("pv")).thenReturn(5L);
                    return List.of(mapper.mapRow(rs, 0));
                });
        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
                .thenReturn(List.of());

        DashboardController.DashboardOverviewVO overview = controller.overview().data();

        assertThat(overview.visitTrend()).containsExactly(new DashboardController.TrendPointVO(LocalDate.of(2026, 6, 29), 12L));
        assertThat(overview.searchTrend()).containsExactly(new DashboardController.TrendPointVO(LocalDate.of(2026, 6, 29), 5L));
    }
    @Test
    void overviewReturnsCachedSnapshotWithoutQueryingDatabase() {
        DashboardController.DashboardOverviewVO cached = new DashboardController.DashboardOverviewVO(
                List.of(new DashboardController.MetricVO("文章总数", "12", "cached")),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
        when(redisCacheService.read(CacheKeys.DASHBOARD_OVERVIEW, DashboardController.DashboardOverviewVO.class))
                .thenReturn(Optional.of(cached));

        assertThat(controller.overview().data()).isSameAs(cached);
        verifyNoInteractions(jdbcTemplate);
    }
}
