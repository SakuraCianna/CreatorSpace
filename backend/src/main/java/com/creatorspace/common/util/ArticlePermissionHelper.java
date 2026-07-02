package com.creatorspace.common.util;

import org.springframework.jdbc.core.JdbcTemplate;

public class ArticlePermissionHelper {

    public static boolean canAccess(JdbcTemplate jdbcTemplate, Long articleId, Long userId) {
        String privacyType = jdbcTemplate.query("""
                        select a.privacy_type
                        from articles a
                        where a.id = ? and a.status in ('PUBLISHED', 'PRIVATE')
                        """,
                (rs, rowNum) -> rs.getString("privacy_type"),
                articleId).stream().findFirst().orElse(null);
        if (privacyType == null) {
            return false;
        }
        return hasPermission(jdbcTemplate, privacyType, articleId, userId);
    }

    public static boolean hasPermission(JdbcTemplate jdbcTemplate, String privacyType, Long articleId, Long userId) {
        return switch (privacyType) {
            case "PUBLIC" -> true;
            case "SELF" -> userId != null && isOwner(jdbcTemplate, articleId, userId);
            case "FRIENDS" -> userId != null && (isOwner(jdbcTemplate, articleId, userId)
                    || isFriend(jdbcTemplate, userId, getAuthorId(jdbcTemplate, articleId)));
            case "SELECTED_FRIENDS" -> userId != null && isAllowed(jdbcTemplate, articleId, userId);
            case "EXCLUDED_FRIENDS" -> userId != null
                    && !isDenied(jdbcTemplate, articleId, userId)
                    && (isOwner(jdbcTemplate, articleId, userId)
                    || isFriend(jdbcTemplate, userId, getAuthorId(jdbcTemplate, articleId)));
            default -> false;
        };
    }

    private static boolean isOwner(JdbcTemplate jdbcTemplate, Long articleId, Long userId) {
        Long authorId = getAuthorId(jdbcTemplate, articleId);
        return authorId != null && authorId.equals(userId);
    }

    private static Long getAuthorId(JdbcTemplate jdbcTemplate, Long articleId) {
        return jdbcTemplate.query(
                        "select created_by from articles where id = ?",
                        (rs, rowNum) -> rs.getLong("created_by"),
                        articleId).stream().findFirst().orElse(null);
    }

    private static boolean isFriend(JdbcTemplate jdbcTemplate, Long userId, Long otherUserId) {
        if (userId == null || otherUserId == null) return false;
        Long count = jdbcTemplate.queryForObject("""
                        select count(*) from user_follows f1
                        join user_follows f2 on f1.follower_id = f2.followee_id and f1.followee_id = f2.follower_id
                        where f1.follower_id = ? and f1.followee_id = ?
                        """,
                Long.class, userId, otherUserId);
        return count != null && count > 0;
    }

    private static boolean isAllowed(JdbcTemplate jdbcTemplate, Long articleId, Long userId) {
        Long count = jdbcTemplate.queryForObject("""
                        select count(*) from article_visibility_users
                        where article_id = ? and user_id = ? and rule_type = 'ALLOW'
                        """,
                Long.class, articleId, userId);
        return count != null && count > 0;
    }

    private static boolean isDenied(JdbcTemplate jdbcTemplate, Long articleId, Long userId) {
        Long count = jdbcTemplate.queryForObject("""
                        select count(*) from article_visibility_users
                        where article_id = ? and user_id = ? and rule_type = 'DENY'
                        """,
                Long.class, articleId, userId);
        return count != null && count > 0;
    }
}
