package com.creatorspace.module.statistics;

import com.creatorspace.common.cache.RedisJsonCacheService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VisitLogServiceTests {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final RedisJsonCacheService redisCacheService = mock(RedisJsonCacheService.class);
    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final VisitLogService visitLogService = new VisitLogService(jdbcTemplate, redisCacheService);

    @Test
    void firstVisitWritesRedisDedupeLogAndAllowsViewCountIncrement() {
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getRequestURI()).thenReturn("/api/articles/slug/hello");
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0 Windows Chrome/120");
        when(request.getHeader("Referer")).thenReturn("/articles");
        when(redisCacheService.setIfAbsent(any(String.class), eq(Duration.ofMinutes(5)))).thenReturn(true);

        boolean shouldIncrement = visitLogService.recordContentVisit("ARTICLE", 7L, request);

        assertThat(shouldIncrement).isTrue();
        verify(jdbcTemplate, never()).queryForObject(any(String.class), eq(Long.class), eq("ARTICLE"), eq(7L), eq("127.0.0.1"));
        verify(jdbcTemplate).update(
                any(String.class),
                eq("/api/articles/slug/hello"),
                eq("ARTICLE"),
                eq(7L),
                eq(null),
                eq("127.0.0.1"),
                eq("Mozilla/5.0 Windows Chrome/120"),
                eq("/articles"),
                eq("DESKTOP"),
                eq("Chrome"),
                eq("Windows")
        );
    }

    @Test
    void redisRepeatedVisitStillWritesLogButSkipsViewCountIncrement() {
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getRequestURI()).thenReturn("/api/projects/slug/demo");
        when(redisCacheService.setIfAbsent(any(String.class), eq(Duration.ofMinutes(5)))).thenReturn(false);

        boolean shouldIncrement = visitLogService.recordContentVisit("PROJECT", 9L, request);

        assertThat(shouldIncrement).isFalse();
        verify(jdbcTemplate, never()).queryForObject(any(String.class), eq(Long.class), eq("PROJECT"), eq(9L), eq("127.0.0.1"));
        verify(jdbcTemplate).update(any(String.class), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void redisFailureFallsBackToDatabaseDedupe() {
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getRequestURI()).thenReturn("/api/articles/slug/hello");
        doThrow(new RuntimeException("redis unavailable"))
                .when(redisCacheService)
                .setIfAbsent(any(String.class), eq(Duration.ofMinutes(5)));
        when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class), eq("ARTICLE"), eq(7L), eq("127.0.0.1")))
                .thenReturn(0L);

        boolean shouldIncrement = visitLogService.recordContentVisit("ARTICLE", 7L, request);

        assertThat(shouldIncrement).isTrue();
    }

    @Test
    void loggingFailureDoesNotBlockContentRead() {
        when(request.getRemoteAddr()).thenReturn("invalid-ip");
        when(request.getRequestURI()).thenReturn("/api/articles/slug/hello");
        when(redisCacheService.setIfAbsent(any(String.class), eq(Duration.ofMinutes(5)))).thenReturn(true);
        doThrow(new RuntimeException("database unavailable"))
                .when(jdbcTemplate)
                .update(any(String.class), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());

        assertThatCode(() -> visitLogService.recordContentVisit("ARTICLE", 7L, request))
                .doesNotThrowAnyException();
    }
}
