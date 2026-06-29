package com.creatorspace.module.interaction;

import com.creatorspace.common.exception.BusinessException;
import com.creatorspace.common.result.ApiResponse;
import com.creatorspace.common.result.PageResponse;
import com.creatorspace.security.LoginUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 登录用户点赞和收藏接口，负责把普通用户互动能力从后台 CMS 中独立出来。
 */
@Validated
@RestController
public class InteractionController {

    private static final Set<String> LIKE_TARGETS = Set.of("ARTICLE", "PROJECT", "INSPIRATION", "MESSAGE");
    private static final Set<String> FAVORITE_TARGETS = Set.of("ARTICLE", "PROJECT", "INSPIRATION");
    private static final Set<String> REACTION_TYPES = Set.of("LIKE", "THANKS", "INSIGHTFUL");

    private final JdbcTemplate jdbcTemplate;

    public InteractionController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // 登录用户点赞公开内容。
    @Transactional(rollbackFor = Exception.class)
    @PostMapping("/api/me/likes")
    public ApiResponse<InteractionVO> like(
            @AuthenticationPrincipal LoginUser loginUser,
            @Valid @RequestBody InteractionRequest request
    ) {
        String targetType = normalizeTargetType(request.targetType(), LIKE_TARGETS);
        ensureVisibleTarget(targetType, request.targetId(), loginUser.userId());
        int inserted = jdbcTemplate.update("""
                        insert into like_records (target_type, target_id, user_id)
                        values (?, ?, ?)
                        on conflict do nothing
                        """,
                targetType,
                request.targetId(),
                loginUser.userId());
        if (inserted > 0) {
            adjustCounters(targetType, request.targetId(), "like_count", 1);
        }
        return ApiResponse.ok(getInteraction("like_records", targetType, request.targetId(), loginUser.userId()));
    }

    // 登录用户取消点赞。
    @Transactional(rollbackFor = Exception.class)
    @DeleteMapping("/api/me/likes")
    public ApiResponse<Void> unlike(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestParam String targetType,
            @RequestParam Long targetId
    ) {
        String normalizedType = normalizeTargetType(targetType, LIKE_TARGETS);
        int deleted = jdbcTemplate.update("""
                        delete from like_records
                        where target_type = ?
                          and target_id = ?
                          and user_id = ?
                        """,
                normalizedType,
                targetId,
                loginUser.userId());
        if (deleted > 0) {
            adjustCounters(normalizedType, targetId, "like_count", -1);
        }
        return ApiResponse.ok(null);
    }

    // 登录用户查看自己的点赞记录。
    @GetMapping("/api/me/likes")
    public ApiResponse<PageResponse<InteractionVO>> listLikes(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestParam(required = false) String targetType,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize
    ) {
        return ApiResponse.ok(listInteractions("like_records", loginUser.userId(), targetType, LIKE_TARGETS, page, pageSize));
    }

    // 登录用户查询是否已点赞。
    @GetMapping("/api/me/likes/status")
    public ApiResponse<StatusVO> likeStatus(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestParam String targetType,
            @RequestParam Long targetId
    ) {
        String normalizedType = normalizeTargetType(targetType, LIKE_TARGETS);
        boolean liked = Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "select exists(select 1 from like_records where target_type = ? and target_id = ? and user_id = ?)",
                Boolean.class,
                normalizedType,
                targetId,
                loginUser.userId()));
        return ApiResponse.ok(new StatusVO(liked));
    }

    // 登录用户收藏公开内容。
    @Transactional(rollbackFor = Exception.class)
    @PostMapping("/api/me/favorites")
    public ApiResponse<InteractionVO> favorite(
            @AuthenticationPrincipal LoginUser loginUser,
            @Valid @RequestBody InteractionRequest request
    ) {
        String targetType = normalizeTargetType(request.targetType(), FAVORITE_TARGETS);
        ensureVisibleTarget(targetType, request.targetId(), loginUser.userId());
        int inserted = jdbcTemplate.update("""
                        insert into favorite_records (target_type, target_id, user_id)
                        values (?, ?, ?)
                        on conflict do nothing
                        """,
                targetType,
                request.targetId(),
                loginUser.userId());
        if (inserted > 0) {
            adjustCounters(targetType, request.targetId(), "favorite_count", 1);
        }
        return ApiResponse.ok(getInteraction("favorite_records", targetType, request.targetId(), loginUser.userId()));
    }

    // 登录用户取消收藏。
    @Transactional(rollbackFor = Exception.class)
    @DeleteMapping("/api/me/favorites")
    public ApiResponse<Void> unfavorite(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestParam String targetType,
            @RequestParam Long targetId
    ) {
        String normalizedType = normalizeTargetType(targetType, FAVORITE_TARGETS);
        int deleted = jdbcTemplate.update("""
                        delete from favorite_records
                        where target_type = ?
                          and target_id = ?
                          and user_id = ?
                        """,
                normalizedType,
                targetId,
                loginUser.userId());
        if (deleted > 0) {
            adjustCounters(normalizedType, targetId, "favorite_count", -1);
        }
        return ApiResponse.ok(null);
    }

    // 登录用户查看自己的收藏记录。
    @GetMapping("/api/me/favorites")
    public ApiResponse<PageResponse<InteractionVO>> listFavorites(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestParam(required = false) String targetType,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize
    ) {
        return ApiResponse.ok(listInteractions("favorite_records", loginUser.userId(), targetType, FAVORITE_TARGETS, page, pageSize));
    }

    // 登录用户查询是否已收藏。
    @GetMapping("/api/me/favorites/status")
    public ApiResponse<StatusVO> favoriteStatus(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestParam String targetType,
            @RequestParam Long targetId
    ) {
        String normalizedType = normalizeTargetType(targetType, FAVORITE_TARGETS);
        boolean favorited = Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "select exists(select 1 from favorite_records where target_type = ? and target_id = ? and user_id = ?)",
                Boolean.class,
                normalizedType,
                targetId,
                loginUser.userId()));
        return ApiResponse.ok(new StatusVO(favorited));
    }

    // 对评论添加反应（LIKE / THANKS / INSIGHTFUL）。
    @Transactional(rollbackFor = Exception.class)
    @PostMapping("/api/me/comment-reactions")
    public ApiResponse<CommentReactionVO> react(
            @AuthenticationPrincipal LoginUser loginUser,
            @Valid @RequestBody CommentReactionRequest request
    ) {
        if (!REACTION_TYPES.contains(request.type())) {
            throw BusinessException.badRequest("评论反应类型不合法");
        }
        Long count = jdbcTemplate.queryForObject("""
                        select count(*) from comments
                        where id = ? and status = 'APPROVED'
                        """,
                Long.class, request.commentId());
        if (count == null || count == 0) {
            throw BusinessException.notFound("评论不存在或不可见");
        }
        Long id = jdbcTemplate.queryForObject("""
                        insert into comment_reactions (comment_id, user_id, reaction_type)
                        values (?, ?, ?)
                        on conflict (comment_id, user_id, reaction_type)
                        do update set created_at = now()
                        returning id
                        """,
                Long.class,
                request.commentId(),
                loginUser.userId(),
                request.type());
        if ("LIKE".equals(request.type())) {
            jdbcTemplate.update("""
                            update comments
                            set like_count = (select count(*) from comment_reactions
                                              where comment_id = ? and reaction_type = 'LIKE'),
                                updated_at = now()
                            where id = ?
                            """,
                    request.commentId(),
                    request.commentId());
        }
        return ApiResponse.ok(getCommentReaction(id));
    }

    // 取消对评论的反应。
    @Transactional(rollbackFor = Exception.class)
    @DeleteMapping("/api/me/comment-reactions")
    public ApiResponse<Void> unreact(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestParam Long commentId,
            @RequestParam(defaultValue = "LIKE") String type
    ) {
        if (!REACTION_TYPES.contains(type)) {
            throw BusinessException.badRequest("评论反应类型不合法");
        }
        jdbcTemplate.update("""
                        delete from comment_reactions
                        where comment_id = ? and user_id = ? and reaction_type = ?
                        """,
                commentId, loginUser.userId(), type);
        if ("LIKE".equals(type)) {
            jdbcTemplate.update("""
                            update comments
                            set like_count = (select count(*) from comment_reactions
                                              where comment_id = ? and reaction_type = 'LIKE'),
                                updated_at = now()
                            where id = ?
                            """,
                    commentId,
                    commentId);
        }
        return ApiResponse.ok(null);
    }

    // 查询用户对某评论的反应状态。
    @GetMapping("/api/me/comment-reactions/status")
    public ApiResponse<StatusVO> reactionStatus(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestParam Long commentId
    ) {
        String type = jdbcTemplate.query("""
                        select reaction_type from comment_reactions
                        where comment_id = ? and user_id = ?
                        """,
                (rs, rowNum) -> rs.getString("reaction_type"),
                commentId, loginUser.userId()).stream().findFirst().orElse(null);
        return ApiResponse.ok(new StatusVO(type != null));
    }

    // 批量查询用户对多个评论的点赞状态。
    @GetMapping("/api/me/comment-reactions/batch-status")
    public ApiResponse<java.util.Map<Long, Boolean>> batchReactionStatus(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestParam java.util.List<Long> commentIds
    ) {
        if (commentIds.isEmpty()) {
            return ApiResponse.ok(java.util.Map.of());
        }
        String placeholders = String.join(",", commentIds.stream().map(id -> "?").toList());
        java.util.Map<Long, Boolean> result = new java.util.LinkedHashMap<>();
        commentIds.forEach(id -> result.put(id, false));
        jdbcTemplate.query("""
                        select comment_id from comment_reactions
                        where comment_id in (%s) and user_id = ? and reaction_type = 'LIKE'
                        """.formatted(placeholders),
                (rs, rowNum) -> rs.getLong("comment_id"),
                commentIds.toArray()).forEach(id -> result.put(id, true));
        return ApiResponse.ok(result);
    }

    private PageResponse<InteractionVO> listInteractions(
            String tableName,
            Long userId,
            String targetType,
            Set<String> allowedTypes,
            long page,
            long pageSize
    ) {
        String normalizedType = targetType == null || targetType.isBlank() ? "" : normalizeTargetType(targetType, allowedTypes);
        String where = normalizedType.isEmpty() ? "where user_id = ?" : "where user_id = ? and target_type = ?";
        Object[] countParams = normalizedType.isEmpty() ? new Object[]{userId} : new Object[]{userId, normalizedType};
        Long total = jdbcTemplate.queryForObject("select count(*) from " + tableName + " " + where, Long.class, countParams);

        List<Object> params = new java.util.ArrayList<>();
        params.add(userId);
        if (!normalizedType.isEmpty()) {
            params.add(normalizedType);
        }
        params.add(pageSize);
        params.add((page - 1) * pageSize);
        List<InteractionVO> records = jdbcTemplate.query("""
                        select id, target_type, target_id, created_at
                        from %s
                        %s
                        order by created_at desc, id desc
                        limit ? offset ?
                        """.formatted(tableName, where),
                (rs, rowNum) -> new InteractionVO(
                        rs.getLong("id"),
                        rs.getString("target_type"),
                        rs.getLong("target_id"),
                        rs.getObject("created_at", OffsetDateTime.class)
                ),
                params.toArray());
        return new PageResponse<>(records, page, pageSize, total == null ? 0 : total);
    }

    private InteractionVO getInteraction(String tableName, String targetType, Long targetId, Long userId) {
        return jdbcTemplate.query("""
                        select id, target_type, target_id, created_at
                        from %s
                        where target_type = ?
                          and target_id = ?
                          and user_id = ?
                        """.formatted(tableName),
                (rs, rowNum) -> new InteractionVO(
                        rs.getLong("id"),
                        rs.getString("target_type"),
                        rs.getLong("target_id"),
                        rs.getObject("created_at", OffsetDateTime.class)
                ),
                targetType,
                targetId,
                userId).stream().findFirst().orElseThrow(() -> BusinessException.notFound("互动记录不存在"));
    }

    private String normalizeTargetType(String value, Set<String> allowedTypes) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!allowedTypes.contains(normalized)) {
            throw BusinessException.badRequest("互动目标类型不合法");
        }
        return normalized;
    }

    private void ensureVisibleTarget(String targetType, Long targetId, Long userId) {
        boolean visible = switch (targetType) {
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
            case "INSPIRATION" -> {
                Long count = jdbcTemplate.queryForObject("""
                                select count(*) from inspiration_cards
                                where id = ? and is_public = true
                                """,
                        Long.class, targetId);
                yield count != null && count > 0;
            }
            case "COMMENT" -> {
                Long count = jdbcTemplate.queryForObject("""
                                select count(*) from comments
                                where id = ? and status = 'APPROVED'
                                """,
                        Long.class, targetId);
                yield count != null && count > 0;
            }
            case "MESSAGE" -> {
                Long count = jdbcTemplate.queryForObject("""
                                select count(*) from guestbook_entries
                                where id = ? and status = 'APPROVED'
                                """,
                        Long.class, targetId);
                yield count != null && count > 0;
            }
            default -> false;
        };
        if (!visible) {
            throw BusinessException.notFound("互动目标不存在或不可见");
        }
    }

    private CommentReactionVO getCommentReaction(Long id) {
        return jdbcTemplate.query("""
                        select cr.id, cr.comment_id, cr.user_id, cr.reaction_type, cr.created_at
                        from comment_reactions cr
                        where cr.id = ?
                        """,
                (rs, rowNum) -> new CommentReactionVO(
                        rs.getLong("id"),
                        rs.getLong("comment_id"),
                        rs.getLong("user_id"),
                        rs.getString("reaction_type"),
                        rs.getObject("created_at", OffsetDateTime.class)
                ),
                id).stream().findFirst().orElseThrow(() -> BusinessException.notFound("评论反应不存在"));
    }

    private void adjustCounters(String targetType, Long targetId, String counterColumn, int delta) {
        if ("COMMENT".equals(targetType) && "like_count".equals(counterColumn)) {
            jdbcTemplate.update("""
                            update comments
                            set like_count = greatest(like_count + ?, 0),
                                updated_at = now()
                            where id = ?
                            """,
                    delta,
                    targetId);
            return;
        }
        jdbcTemplate.update("""
                        insert into content_statistics (target_type, target_id, %s)
                        values (?, ?, greatest(?, 0))
                        on conflict (target_type, target_id) do update
                        set %s = greatest(content_statistics.%s + ?, 0),
                            updated_at = now()
                        """.formatted(counterColumn, counterColumn, counterColumn),
                targetType,
                targetId,
                delta,
                delta);
        if ("ARTICLE".equals(targetType) && "like_count".equals(counterColumn)) {
            jdbcTemplate.update("update articles set like_count = greatest(like_count + ?, 0), updated_at = now() where id = ?", delta, targetId);
        }
    }

    public record InteractionRequest(
            @NotBlank String targetType,
            @NotNull Long targetId
    ) {
    }

    public record InteractionVO(
            Long id,
            String targetType,
            Long targetId,
            OffsetDateTime createdAt
    ) {
    }

    public record StatusVO(
            boolean status
    ) {
    }

    public record CommentReactionRequest(
            @NotNull Long commentId,
            @NotBlank String type
    ) {
    }

    public record CommentReactionVO(
            Long id,
            Long commentId,
            Long userId,
            String type,
            OffsetDateTime createdAt
    ) {
    }
}
