package com.creatorspace.module.notification;

import com.creatorspace.common.result.ApiResponse;
import com.creatorspace.common.result.PageResponse;
import com.creatorspace.security.LoginUser;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class NotificationController {

    private final JdbcTemplate jdbcTemplate;

    public NotificationController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public record NotificationVO(
            Long id,
            String type,
            String title,
            String content,
            String targetType,
            Long targetId,
            Long actorId,
            String actorName,
            Boolean isRead,
            OffsetDateTime createdAt
    ) {}

    public record UnreadCountVO(long count) {}

    // 登录用户查看自己的通知列表。
    @GetMapping("/api/me/notifications")
    public ApiResponse<PageResponse<NotificationVO>> listNotifications(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize
    ) {
        Long total = jdbcTemplate.queryForObject(
                "select count(*) from notifications where user_id = ?",
                Long.class, loginUser.userId());

        List<NotificationVO> records = jdbcTemplate.query("""
                        select id, type, title, content, target_type, target_id,
                               actor_id, actor_name, is_read, created_at
                        from notifications
                        where user_id = ?
                        order by created_at desc, id desc
                        limit ? offset ?
                        """,
                (rs, rowNum) -> new NotificationVO(
                        rs.getLong("id"),
                        rs.getString("type"),
                        rs.getString("title"),
                        rs.getString("content"),
                        rs.getString("target_type"),
                        rs.getObject("target_id", Long.class),
                        rs.getObject("actor_id", Long.class),
                        rs.getString("actor_name"),
                        rs.getBoolean("is_read"),
                        rs.getObject("created_at", OffsetDateTime.class)
                ),
                loginUser.userId(), pageSize, (page - 1) * pageSize);

        return ApiResponse.ok(new PageResponse<>(records, page, pageSize, total == null ? 0 : total));
    }

    // 登录用户查看未读通知数。
    @GetMapping("/api/me/notifications/unread-count")
    public ApiResponse<UnreadCountVO> unreadCount(
            @AuthenticationPrincipal LoginUser loginUser
    ) {
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from notifications where user_id = ? and is_read = false",
                Long.class, loginUser.userId());
        return ApiResponse.ok(new UnreadCountVO(count == null ? 0 : count));
    }

    // 标记单条通知为已读。
    @Transactional(rollbackFor = Exception.class)
    @PutMapping("/api/me/notifications/{id}/read")
    public ApiResponse<Void> markRead(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable Long id
    ) {
        jdbcTemplate.update(
                "update notifications set is_read = true where id = ? and user_id = ?",
                id, loginUser.userId());
        return ApiResponse.ok(null);
    }

    // 标记所有通知为已读。
    @Transactional(rollbackFor = Exception.class)
    @PutMapping("/api/me/notifications/read-all")
    public ApiResponse<Void> markAllRead(
            @AuthenticationPrincipal LoginUser loginUser
    ) {
        jdbcTemplate.update(
                "update notifications set is_read = true where user_id = ? and is_read = false",
                loginUser.userId());
        return ApiResponse.ok(null);
    }

    // 删除一条通知。
    @Transactional(rollbackFor = Exception.class)
    @DeleteMapping("/api/me/notifications/{id}")
    public ApiResponse<Void> deleteNotification(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable Long id
    ) {
        jdbcTemplate.update(
                "delete from notifications where id = ? and user_id = ?",
                id, loginUser.userId());
        return ApiResponse.ok(null);
    }

    // 管理员查看待审核概览。
    @GetMapping("/api/admin/pending-review")
    public ApiResponse<Map<String, Object>> pendingReview(
            @AuthenticationPrincipal LoginUser loginUser
    ) {
        Long pendingComments = jdbcTemplate.queryForObject(
                "select count(*) from comments where status = 'PENDING'",
                Long.class);
        Long pendingGuestbook = jdbcTemplate.queryForObject(
                "select count(*) from guestbook_entries where status = 'PENDING'",
                Long.class);
        Long pendingArticles = jdbcTemplate.queryForObject(
                "select count(*) from articles where status = 'PENDING_REVIEW'",
                Long.class);
        Long pendingProjects = jdbcTemplate.queryForObject(
                "select count(*) from portfolio_projects where status = 'PENDING_REVIEW'",
                Long.class);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("pendingComments", pendingComments == null ? 0 : pendingComments);
        result.put("pendingGuestbook", pendingGuestbook == null ? 0 : pendingGuestbook);
        result.put("pendingArticles", pendingArticles == null ? 0 : pendingArticles);
        result.put("pendingProjects", pendingProjects == null ? 0 : pendingProjects);
        long total = (pendingComments == null ? 0 : pendingComments)
                   + (pendingGuestbook == null ? 0 : pendingGuestbook)
                   + (pendingArticles == null ? 0 : pendingArticles)
                   + (pendingProjects == null ? 0 : pendingProjects);
        result.put("total", total);
        return ApiResponse.ok(result);
    }
}
