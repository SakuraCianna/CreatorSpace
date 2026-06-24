package com.creatorspace.module.search;

import com.creatorspace.security.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class SearchLogService {

    private final JdbcTemplate jdbcTemplate;

    public SearchLogService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void record(String keyword, int resultCount, HttpServletRequest request, LoginUser loginUser) {
        if (keyword == null || keyword.isBlank()) {
            return;
        }
        try {
            jdbcTemplate.update("""
                            insert into search_logs (keyword, result_count, user_id, ip_address, user_agent)
                            values (?, ?, ?, cast(? as inet), ?)
                            """,
                    keyword.trim(),
                    resultCount,
                    loginUser == null ? null : loginUser.userId(),
                    request.getRemoteAddr(),
                    request.getHeader("User-Agent"));
        } catch (Exception ignored) {
            // Search results should still be returned when analytics logging fails.
        }
    }
}
