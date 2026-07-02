package com.creatorspace.module.notification;

import org.springframework.jdbc.core.JdbcTemplate;

public final class NotificationHelper {

    private NotificationHelper() {}

    public static void insert(
            JdbcTemplate jdbcTemplate,
            long userId,
            String type,
            String title,
            String content,
            String targetType,
            Long targetId,
            Long actorId,
            String actorName
    ) {
        jdbcTemplate.update("""
                        insert into notifications (user_id, type, title, content, target_type, target_id, actor_id, actor_name)
                        values (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                userId, type, title, content, targetType, targetId, actorId, actorName);
    }
}
