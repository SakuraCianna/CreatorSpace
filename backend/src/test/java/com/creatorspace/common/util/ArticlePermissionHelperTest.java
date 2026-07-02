package com.creatorspace.common.util;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ArticlePermissionHelperTest {

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void canAccessReturnsFalseWhenArticleIsMissingAndTrueForPublic() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(1L))).thenReturn(List.of());
        assertFalse(ArticlePermissionHelper.canAccess(jdbcTemplate, 1L, null));

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(2L))).thenAnswer(invocation -> {
            RowMapper mapper = invocation.getArgument(1);
            ResultSet rs = mock(ResultSet.class);
            when(rs.getString("privacy_type")).thenReturn("PUBLIC");
            return List.of(mapper.mapRow(rs, 0));
        });
        assertTrue(ArticlePermissionHelper.canAccess(jdbcTemplate, 2L, null));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void hasPermissionHandlesOwnerFriendsSelectedAndDeniedRules() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.query(eq("select created_by from articles where id = ?"), any(RowMapper.class), eq(10L)))
                .thenAnswer(invocation -> {
                    RowMapper mapper = invocation.getArgument(1);
                    ResultSet rs = mock(ResultSet.class);
                    when(rs.getLong("created_by")).thenReturn(7L);
                    return List.of(mapper.mapRow(rs, 0));
                });
        when(jdbcTemplate.queryForObject(contains("user_follows"), eq(Long.class), eq(9L), eq(7L))).thenReturn(1L);
        when(jdbcTemplate.queryForObject(contains("rule_type = 'ALLOW'"), eq(Long.class), eq(10L), eq(9L))).thenReturn(1L);
        when(jdbcTemplate.queryForObject(contains("rule_type = 'DENY'"), eq(Long.class), eq(10L), eq(9L))).thenReturn(0L);

        assertTrue(ArticlePermissionHelper.hasPermission(jdbcTemplate, "SELF", 10L, 7L));
        assertFalse(ArticlePermissionHelper.hasPermission(jdbcTemplate, "SELF", 10L, 9L));
        assertTrue(ArticlePermissionHelper.hasPermission(jdbcTemplate, "FRIENDS", 10L, 9L));
        assertTrue(ArticlePermissionHelper.hasPermission(jdbcTemplate, "SELECTED_FRIENDS", 10L, 9L));
        assertTrue(ArticlePermissionHelper.hasPermission(jdbcTemplate, "EXCLUDED_FRIENDS", 10L, 9L));

        when(jdbcTemplate.queryForObject(contains("rule_type = 'DENY'"), eq(Long.class), eq(10L), eq(9L))).thenReturn(1L);
        assertFalse(ArticlePermissionHelper.hasPermission(jdbcTemplate, "EXCLUDED_FRIENDS", 10L, 9L));
        assertFalse(ArticlePermissionHelper.hasPermission(jdbcTemplate, "UNKNOWN", 10L, 9L));
    }
}
