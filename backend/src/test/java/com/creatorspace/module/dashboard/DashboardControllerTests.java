package com.creatorspace.module.dashboard;

import com.creatorspace.common.cache.CacheKeys;
import com.creatorspace.common.cache.RedisJsonCacheService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DashboardControllerTests {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final RedisJsonCacheService redisCacheService = mock(RedisJsonCacheService.class);
    private final DashboardController controller = new DashboardController(jdbcTemplate, redisCacheService);

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