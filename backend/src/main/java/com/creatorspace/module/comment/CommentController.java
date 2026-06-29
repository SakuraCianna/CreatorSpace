package com.creatorspace.module.comment;

import com.creatorspace.common.exception.BusinessException;
import com.creatorspace.common.result.ApiResponse;
import com.creatorspace.common.result.PageResponse;
import com.creatorspace.security.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 评论提交、公开展示和后台审核接口。
 */
@Validated
@RestController
public class CommentController {

    private static final Set<String> TARGET_TYPES = Set.of("ARTICLE", "PROJECT", "MESSAGE");
    private static final Set<String> STATUSES = Set.of("PENDING", "APPROVED", "REJECTED", "SPAM");
    private static final int MAX_DEPTH = 4;

    private final JdbcTemplate jdbcTemplate;

    public CommentController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // 查询公开评论，只返回已审核通过的内容。
    @GetMapping("/api/comments")
    public ApiResponse<PageResponse<CommentVO>> listPublic(
            @RequestParam String targetType,
            @RequestParam Long targetId,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize
    ) {
        String type = normalizeTargetType(targetType);
        long offset = (page - 1) * pageSize;
        Long total = jdbcTemplate.queryForObject("""
                        with recursive visible_comments as (
                            select root.*
                            from comments root
                            where root.target_type = ?
                              and root.target_id = ?
                              and root.status = 'APPROVED'
                              and root.parent_id is null
                            union all
                            select child.*
                            from comments child
                            join visible_comments parent on parent.id = child.parent_id
                            where child.target_type = ?
                              and child.target_id = ?
                              and child.status = 'APPROVED'
                        )
                        select count(*)
                        from visible_comments
                        """,
                Long.class,
                type,
                targetId,
                type,
                targetId);
        List<CommentVO> records = jdbcTemplate.query("""
                        with recursive visible_comments as (
                            select root.*
                            from comments root
                            where root.target_type = ?
                              and root.target_id = ?
                              and root.status = 'APPROVED'
                              and root.parent_id is null
                            union all
                            select child.*
                            from comments child
                            join visible_comments parent on parent.id = child.parent_id
                            where child.target_type = ?
                              and child.target_id = ?
                              and child.status = 'APPROVED'
                        )
                        select c.id,
                               c.target_type,
                               c.target_id,
                               c.parent_id,
                               c.root_id,
                               c.user_id,
                               u.username,
                               c.content,
                               c.status,
                               c.depth,
                               c.reply_count,
                               c.like_count,
                               c.created_at
                        from visible_comments c
                        join users u on u.id = c.user_id
                        order by coalesce(c.root_id, c.id) asc, c.depth asc, c.created_at asc, c.id asc
                        limit ? offset ?
                        """,
                (rs, rowNum) -> toComment(rs),
                type,
                targetId,
                type,
                targetId,
                pageSize,
                offset);
        return ApiResponse.ok(new PageResponse<>(records, page, pageSize, total == null ? 0 : total));
    }

    // 登录用户提交评论，默认进入审核队列。
    @Transactional(rollbackFor = Exception.class)
    @PostMapping("/api/comments")
    public ApiResponse<CommentVO> create(
            @AuthenticationPrincipal LoginUser loginUser,
            @Valid @RequestBody CommentRequest request,
            HttpServletRequest servletRequest
    ) {
        String type = normalizeTargetType(request.targetType());
        ensureTargetExists(type, request.targetId(), loginUser.userId());
        ParentComment parent = parentComment(type, request.targetId(), request.parentId());
        ensureContentAllowed(request.content());
        checkSpam(loginUser.userId());

        Long id = jdbcTemplate.queryForObject("""
                        insert into comments (
                            target_type,
                            target_id,
                            parent_id,
                            root_id,
                            user_id,
                            reply_to_user_id,
                            content,
                            status,
                            depth,
                            ip_address,
                            user_agent
                        )
                        values (?, ?, ?, ?, ?, ?, ?, 'PENDING', ?, cast(? as inet), ?)
                        returning id
                        """,
                Long.class,
                type,
                request.targetId(),
                parent == null ? null : parent.id(),
                parent == null ? null : parent.rootId(),
                loginUser.userId(),
                parent == null ? null : parent.userId(),
                request.content().trim(),
                parent == null ? 0 : parent.depth() + 1,
                servletRequest.getRemoteAddr(),
                servletRequest.getHeader("User-Agent"));
        return ApiResponse.ok(getComment(id));
    }

    // 管理员查看评论审核队列。
    @GetMapping("/api/admin/comments")
    public ApiResponse<PageResponse<CommentVO>> listAdmin(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String targetType,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize
    ) {
        String normalizedStatus = status == null || status.isBlank() ? "" : status.trim().toUpperCase();
        String normalizedType = targetType == null || targetType.isBlank() ? "" : normalizeTargetType(targetType);
        if (!normalizedStatus.isEmpty() && !STATUSES.contains(normalizedStatus)) {
            throw BusinessException.badRequest("评论状态不合法");
        }
        StringBuilder where = new StringBuilder("where 1 = 1");
        List<Object> params = new java.util.ArrayList<>();
        if (!normalizedStatus.isEmpty()) {
            where.append(" and c.status = ?");
            params.add(normalizedStatus);
        }
        if (!normalizedType.isEmpty()) {
            where.append(" and c.target_type = ?");
            params.add(normalizedType);
        }

        Long total = jdbcTemplate.queryForObject("select count(*) from comments c " + where,
                Long.class,
                params.toArray());
        List<Object> listParams = new java.util.ArrayList<>(params);
        listParams.add(pageSize);
        listParams.add((page - 1) * pageSize);
        List<CommentVO> records = jdbcTemplate.query("""
                        select c.id,
                               c.target_type,
                               c.target_id,
                               c.parent_id,
                               c.root_id,
                               c.user_id,
                               u.username,
                               c.content,
                               c.status,
                               c.depth,
                               c.reply_count,
                               c.like_count,
                               c.created_at
                        from comments c
                        join users u on u.id = c.user_id
                        %s
                        order by c.created_at desc, c.id desc
                        limit ? offset ?
                        """.formatted(where),
                (rs, rowNum) -> toComment(rs),
                listParams.toArray());
        return ApiResponse.ok(new PageResponse<>(records, page, pageSize, total == null ? 0 : total));
    }

    // 管理员审核通过评论。
    @Transactional(rollbackFor = Exception.class)
    @PutMapping("/api/admin/comments/{id}/approve")
    public ApiResponse<CommentVO> approve(@PathVariable Long id) {
        updateReviewStatus(id, "APPROVED");
        return ApiResponse.ok(getComment(id));
    }

    // 管理员驳回评论。
    @Transactional(rollbackFor = Exception.class)
    @PutMapping("/api/admin/comments/{id}/reject")
    public ApiResponse<CommentVO> reject(@PathVariable Long id) {
        updateReviewStatus(id, "REJECTED");
        return ApiResponse.ok(getComment(id));
    }

    private CommentVO getComment(Long id) {
        return jdbcTemplate.query("""
                        select c.id,
                               c.target_type,
                               c.target_id,
                               c.parent_id,
                               c.root_id,
                               c.user_id,
                               u.username,
                               c.content,
                               c.status,
                               c.depth,
                               c.reply_count,
                               c.like_count,
                               c.created_at
                        from comments c
                        join users u on u.id = c.user_id
                        where c.id = ?
                        """,
                (rs, rowNum) -> toComment(rs),
                id).stream().findFirst().orElseThrow(() -> BusinessException.notFound("评论不存在"));
    }

    private void updateReviewStatus(Long id, String status) {
        CommentStatus current = jdbcTemplate.query("""
                        select id, parent_id, status
                        from comments
                        where id = ?
                        for update
                        """,
                (rs, rowNum) -> new CommentStatus(
                        rs.getLong("id"),
                        rs.getObject("parent_id") == null ? null : rs.getLong("parent_id"),
                        rs.getString("status")
                ),
                id).stream().findFirst().orElseThrow(() -> BusinessException.notFound("评论不存在"));

        int affected = jdbcTemplate.update("update comments set status = ?, updated_at = now() where id = ?", status, id);
        if (affected == 0) {
            throw BusinessException.notFound("评论不存在");
        }
        if (current.parentId() == null || current.status().equals(status)) {
            return;
        }
        if ("APPROVED".equals(status)) {
            jdbcTemplate.update("update comments set reply_count = reply_count + 1, updated_at = now() where id = ?", current.parentId());
        } else if ("APPROVED".equals(current.status())) {
            jdbcTemplate.update("""
                            update comments
                            set reply_count = greatest(reply_count - 1, 0),
                                updated_at = now()
                            where id = ?
                            """,
                    current.parentId());
        }
    }

    private ParentComment parentComment(String type, Long targetId, Long parentId) {
        if (parentId == null) {
            return null;
        }
        ParentComment parent = jdbcTemplate.query("""
                        select id, root_id, user_id, depth
                        from comments
                        where id = ?
                          and target_type = ?
                          and target_id = ?
                          and status = 'APPROVED'
                        """,
                (rs, rowNum) -> new ParentComment(
                        rs.getLong("id"),
                        rs.getObject("root_id") == null ? rs.getLong("id") : rs.getLong("root_id"),
                        rs.getLong("user_id"),
                        rs.getInt("depth")
                ),
                parentId,
                type,
                targetId).stream().findFirst().orElseThrow(() -> BusinessException.badRequest("回复的评论不存在或未通过审核"));
        if (parent.depth() >= MAX_DEPTH) {
            throw BusinessException.badRequest("评论回复层级过深");
        }
        return parent;
    }

    private void ensureTargetExists(String type, Long targetId, Long userId) {
        boolean exists = switch (type) {
            case "ARTICLE" -> {
                String privacyType = jdbcTemplate.query("""
                                select privacy_type from articles
                                where id = ? and status = 'PUBLISHED'
                                """,
                        (rs, rowNum) -> rs.getString("privacy_type"),
                        targetId).stream().findFirst().orElse(null);
                if (privacyType == null) {
                    yield false;
                }
                yield com.creatorspace.common.util.ArticlePermissionHelper.hasPermission(
                        jdbcTemplate, privacyType, targetId, userId);
            }
            case "PROJECT" -> {
                Long count = jdbcTemplate.queryForObject("""
                                select count(*) from portfolio_projects
                                where id = ? and status = 'VISIBLE'
                                """,
                        Long.class, targetId);
                yield count != null && count > 0;
            }
            case "MESSAGE" -> true;
            default -> false;
        };
        if (!exists) {
            throw BusinessException.notFound("评论目标不存在或不可见");
        }
    }

    private void ensureContentAllowed(String content) {
        String normalized = content.trim();
        jdbcTemplate.query("""
                        select word, match_type, severity
                        from sensitive_words
                        where enabled = true
                        order by id
                        """,
                rs -> {
                    if (matchesSensitiveWord(normalized, rs.getString("word"), rs.getString("match_type"))
                            && "REJECT".equals(rs.getString("severity"))) {
                        throw BusinessException.badRequest("评论包含敏感内容");
                    }
                });
    }

    private void checkSpam(Long userId) {
        Long recent = jdbcTemplate.queryForObject("""
                        select count(*) from comments
                        where user_id = ?
                          and created_at > ? - interval '1 minute'
                        """,
                Long.class, userId, java.time.OffsetDateTime.now());
        if (recent != null && recent >= 5) {
            throw BusinessException.tooManyRequests("评论过于频繁，请稍后再试");
        }
    }

    private boolean matchesSensitiveWord(String content, String word, String matchType) {
        return switch (matchType) {
            case "EXACT" -> content.equalsIgnoreCase(word);
            case "REGEX" -> matchesRegex(content, word);
            default -> content.toLowerCase().contains(word.toLowerCase());
        };
    }

    private boolean matchesRegex(String content, String pattern) {
        try {
            return Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(content).find();
        } catch (PatternSyntaxException exception) {
            return false;
        }
    }

    private String normalizeTargetType(String value) {
        String type = value == null ? "" : value.trim().toUpperCase();
        if (!TARGET_TYPES.contains(type)) {
            throw BusinessException.badRequest("评论目标类型不合法");
        }
        return type;
    }

    private CommentVO toComment(ResultSet rs) throws SQLException {
        return new CommentVO(
                rs.getLong("id"),
                rs.getString("target_type"),
                rs.getLong("target_id"),
                rs.getObject("parent_id") == null ? null : rs.getLong("parent_id"),
                rs.getObject("root_id") == null ? null : rs.getLong("root_id"),
                rs.getLong("user_id"),
                rs.getString("username"),
                rs.getString("content"),
                rs.getString("status"),
                rs.getInt("depth"),
                rs.getLong("reply_count"),
                rs.getLong("like_count"),
                rs.getObject("created_at", OffsetDateTime.class)
        );
    }

    public record CommentRequest(
            @NotBlank String targetType,
            @NotNull Long targetId,
            Long parentId,
            @NotBlank @Size(min = 2, max = 1000) String content
    ) {
    }

    public record CommentVO(
            Long id,
            String targetType,
            Long targetId,
            Long parentId,
            Long rootId,
            Long userId,
            String username,
            String content,
            String status,
            Integer depth,
            Long replyCount,
            Long likeCount,
            OffsetDateTime createdAt
    ) {
    }

    private record ParentComment(Long id, Long rootId, Long userId, Integer depth) {
    }

    private record CommentStatus(Long id, Long parentId, String status) {
    }
}
