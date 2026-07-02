package com.creatorspace.module.search;

import com.creatorspace.security.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class SearchLogServiceTests {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final SearchLogService searchLogService = new SearchLogService(jdbcTemplate);

    @Test
    void recordWritesKeywordResultCountUserIdAndRequestContext() {
        org.mockito.Mockito.when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        org.mockito.Mockito.when(request.getHeader("User-Agent")).thenReturn("Search Agent");

        searchLogService.record("  creator  ", 3, request, new LoginUser(9L, "admin", List.of("ADMIN")));

        verify(jdbcTemplate).update(
                any(String.class),
                eq("creator"),
                eq(3),
                eq(9L),
                eq("127.0.0.1"),
                eq("Search Agent")
        );
    }

    @Test
    void recordSkipsBlankKeyword() {
        searchLogService.record("   ", 0, request, null);

        verify(jdbcTemplate, never()).update(any(String.class), org.mockito.ArgumentMatchers.<Object[]>any());
    }

    @Test
    void recordDoesNotPropagateDatabaseFailure() {
        org.mockito.Mockito.when(request.getRemoteAddr()).thenReturn("not-an-ip");
        doThrow(new RuntimeException("database unavailable"))
                .when(jdbcTemplate)
                .update(any(String.class), any(), any(), any(), any(), any());

        assertThatCode(() -> searchLogService.record("creator", 1, request, null))
                .doesNotThrowAnyException();
    }
}
