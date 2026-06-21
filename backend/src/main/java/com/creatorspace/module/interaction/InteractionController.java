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

    private static final Set<String> LIKE_TARGETS = Set.of("ARTICLE", "PROJECT", "COMMENT", "INSPIRATION");
    private static final Set<String> FAVORITE_TARGETS = Set.of("ARTICLE", "PROJECT", "INSPIRATION");

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
        ensureVisibleTarget(targetType, request.targetId());
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

    // 登录用户收藏公开内容。
    @Transactional(rollbackFor = Exception.class)
    @PostMapping("/api/me/favorites")
    public ApiResponse<InteractionVO> favorite(
            @AuthenticationPrincipal LoginUser loginUser,
            @Valid @RequestBody InteractionRequest request
    ) {
        String targetType = normalizeTargetType(request.targetType(), FAVORITE_TARGETS);
        ensureVisibleTarget(targetType, request.targetId());
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

    private void ensureVisibleTarget(String targetType, Long targetId) {
        Long count = switch (targetType) {
            case "ARTICLE" -> jdbcTemplate.queryForObject("""
                            select count(*)
                            from articles
                            where id = ?
                              and status = 'PUBLISHED'
                              and privacy_type = 'PUBLIC'
                            """,
                    Long.class,
                    targetId);
            case "PROJECT" -> jdbcTemplate.queryForObject("""
                            select count(*)
                            from portfolio_projects
                            where id = ?
                              and status = 'VISIBLE'
                            """,
                    Long.class,
                    targetId);
            case "INSPIRATION" -> jdbcTemplate.queryForObject("""
                            select count(*)
                            from inspiration_cards
                            where id = ?
                              and is_public = true
                            """,
                    Long.class,
                    targetId);
            case "COMMENT" -> jdbcTemplate.queryForObject("""
                            select count(*)
                            from comments
                            where id = ?
                              and status = 'APPROVED'
                            """,
                    Long.class,
                    targetId);
            default -> 0L;
        };
        if (count == null || count == 0) {
            throw BusinessException.notFound("互动目标不存在或不可见");
        }
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
}
