package com.creatorspace.module.statistics;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VisitLogServiceTests {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final VisitLogService visitLogService = new VisitLogService(jdbcTemplate);

    @Test
    void firstVisitWritesLogAndAllowsViewCountIncrement() {
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getRequestURI()).thenReturn("/api/articles/slug/hello");
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0 Windows Chrome/120");
        when(request.getHeader("Referer")).thenReturn("/articles");
        when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class), eq("ARTICLE"), eq(7L), eq("127.0.0.1")))
                .thenReturn(0L);

        boolean shouldIncrement = visitLogService.recordContentVisit("ARTICLE", 7L, request);

        assertThat(shouldIncrement).isTrue();
        verify(jdbcTemplate).update(
                any(String.class),
                eq("/api/articles/slug/hello"),
                eq("ARTICLE"),
                eq(7L),
                eq("127.0.0.1"),
                eq("Mozilla/5.0 Windows Chrome/120"),
                eq("/articles"),
                eq("DESKTOP"),
                eq("Chrome"),
                eq("Windows")
        );
    }

    @Test
    void recentVisitStillWritesLogButSkipsViewCountIncrement() {
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getRequestURI()).thenReturn("/api/projects/slug/demo");
        when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class), eq("PROJECT"), eq(9L), eq("127.0.0.1")))
                .thenReturn(1L);

        boolean shouldIncrement = visitLogService.recordContentVisit("PROJECT", 9L, request);

        assertThat(shouldIncrement).isFalse();
        verify(jdbcTemplate).update(any(String.class), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void loggingFailureDoesNotBlockContentRead() {
        when(request.getRemoteAddr()).thenReturn("invalid-ip");
        when(request.getRequestURI()).thenReturn("/api/articles/slug/hello");
        doThrow(new RuntimeException("database unavailable"))
                .when(jdbcTemplate)
                .update(any(String.class), any(), any(), any(), any(), any(), any(), any(), any(), any());

        assertThatCode(() -> visitLogService.recordContentVisit("ARTICLE", 7L, request))
                .doesNotThrowAnyException();
    }
}
