package com.creatorspace.module.visibility;

import com.creatorspace.common.exception.BusinessException;
import com.creatorspace.common.result.ApiResponse;
import com.creatorspace.security.LoginUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@Validated
@RestController
public class ArticleVisibilityController {

    private static final Set<String> RULE_TYPES = Set.of("ALLOW", "DENY");

    private final JdbcTemplate jdbcTemplate;

    public ArticleVisibilityController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/api/creator/articles/{articleId}/visibility-users")
    public ApiResponse<List<VisibilityUserVO>> list(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable Long articleId
    ) {
        ensureOwner(articleId, loginUser.userId());
        List<VisibilityUserVO> records = jdbcTemplate.query("""
                        select v.user_id, u.username, v.rule_type, v.created_at
                        from article_visibility_users v
                        join users u on u.id = v.user_id
                        where v.article_id = ?
                        order by v.rule_type, u.username
                        """,
                (rs, rowNum) -> new VisibilityUserVO(
                        rs.getLong("user_id"),
                        rs.getString("username"),
                        rs.getString("rule_type"),
                        rs.getObject("created_at", java.time.OffsetDateTime.class)
                ),
                articleId);
        return ApiResponse.ok(records);
    }

    @Transactional(rollbackFor = Exception.class)
    @PostMapping("/api/creator/articles/{articleId}/visibility-users")
    public ApiResponse<VisibilityUserVO> add(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable Long articleId,
            @Valid @RequestBody VisibilityUserRequest request
    ) {
        ensureOwner(articleId, loginUser.userId());
        String ruleType = normalizeRuleType(request.ruleType());
        Long userExists = jdbcTemplate.queryForObject("select count(*) from users where id = ?", Long.class, request.userId());
        if (userExists == null || userExists == 0) {
            throw BusinessException.notFound("用户不存在");
        }
        if (loginUser.userId().equals(request.userId())) {
            throw BusinessException.badRequest("不能对自己设置可见性规则");
        }
        try {
            jdbcTemplate.update("""
                            insert into article_visibility_users (article_id, user_id, rule_type)
                            values (?, ?, ?)
                            on conflict (article_id, user_id) do update
                            set rule_type = excluded.rule_type,
                                created_at = now()
                            """,
                    articleId, request.userId(), ruleType);
            return ApiResponse.ok(getUser(articleId, request.userId()));
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw BusinessException.badRequest("可见性规则已存在");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @DeleteMapping("/api/creator/articles/{articleId}/visibility-users/{userId}")
    public ApiResponse<Void> remove(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable Long articleId,
            @PathVariable Long userId
    ) {
        ensureOwner(articleId, loginUser.userId());
        int affected = jdbcTemplate.update("""
                        delete from article_visibility_users
                        where article_id = ? and user_id = ?
                        """,
                articleId, userId);
        if (affected == 0) {
            throw BusinessException.notFound("可见性规则不存在");
        }
        return ApiResponse.ok(null);
    }

    private void ensureOwner(Long articleId, Long userId) {
        Long ownerId = jdbcTemplate.queryForObject(
                "select created_by from articles where id = ?",
                Long.class, articleId);
        if (ownerId == null) {
            throw BusinessException.notFound("文章不存在");
        }
        if (!ownerId.equals(userId)) {
            throw BusinessException.badRequest("只能管理自己的文章");
        }
    }

    private VisibilityUserVO getUser(Long articleId, Long userId) {
        return jdbcTemplate.query("""
                        select v.user_id, u.username, v.rule_type, v.created_at
                        from article_visibility_users v
                        join users u on u.id = v.user_id
                        where v.article_id = ? and v.user_id = ?
                        """,
                (rs, rowNum) -> new VisibilityUserVO(
                        rs.getLong("user_id"),
                        rs.getString("username"),
                        rs.getString("rule_type"),
                        rs.getObject("created_at", java.time.OffsetDateTime.class)
                ),
                articleId, userId).stream().findFirst()
                .orElseThrow(() -> BusinessException.notFound("可见性规则不存在"));
    }

    private String normalizeRuleType(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase();
        if (!RULE_TYPES.contains(normalized)) {
            throw BusinessException.badRequest("规则类型不合法");
        }
        return normalized;
    }

    public record VisibilityUserRequest(
            @NotNull Long userId,
            @NotBlank String ruleType
    ) {
    }

    public record VisibilityUserVO(
            Long userId,
            String username,
            String ruleType,
            java.time.OffsetDateTime createdAt
    ) {
    }
}
