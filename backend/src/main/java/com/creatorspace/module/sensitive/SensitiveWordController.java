package com.creatorspace.module.sensitive;

import com.creatorspace.common.exception.BusinessException;
import com.creatorspace.common.result.ApiResponse;
import com.creatorspace.common.result.PageResponse;
import com.creatorspace.security.LoginUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

@Validated
@RestController
public class SensitiveWordController {

    private static final Set<String> MATCH_TYPES = Set.of("EXACT", "CONTAINS", "REGEX");
    private static final Set<String> SEVERITIES = Set.of("REVIEW", "REJECT", "MASK");

    private final JdbcTemplate jdbcTemplate;

    public SensitiveWordController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/api/admin/sensitive-words")
    public ApiResponse<PageResponse<SensitiveWordVO>> list(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize
    ) {
        long offset = (page - 1) * pageSize;
        Long total = jdbcTemplate.queryForObject(
                "select count(*) from sensitive_words",
                Long.class);
        List<SensitiveWordVO> records = jdbcTemplate.query("""
                        select s.id, s.word, s.match_type, s.severity, s.enabled, s.created_at
                        from sensitive_words s
                        order by s.created_at desc, s.id desc
                        limit ? offset ?
                        """,
                (rs, rowNum) -> new SensitiveWordVO(
                        rs.getLong("id"),
                        rs.getString("word"),
                        rs.getString("match_type"),
                        rs.getString("severity"),
                        rs.getBoolean("enabled"),
                        rs.getObject("created_at", OffsetDateTime.class)
                ),
                pageSize,
                offset);
        return ApiResponse.ok(new PageResponse<>(records, page, pageSize, total == null ? 0 : total));
    }

    @Transactional(rollbackFor = Exception.class)
    @PostMapping("/api/admin/sensitive-words")
    public ApiResponse<SensitiveWordVO> create(
            @AuthenticationPrincipal LoginUser loginUser,
            @Valid @RequestBody SensitiveWordRequest request
    ) {
        String matchType = normalizeMatchType(request.matchType());
        String severity = normalizeSeverity(request.severity());
        try {
            Long id = jdbcTemplate.queryForObject("""
                            insert into sensitive_words (word, match_type, severity, enabled, created_by)
                            values (?, ?, ?, ?, ?)
                            returning id
                            """,
                    Long.class,
                    request.word().trim(),
                    matchType,
                    severity,
                    request.enabled(),
                    loginUser.userId());
            return ApiResponse.ok(getWord(id));
        } catch (DataIntegrityViolationException e) {
            throw BusinessException.badRequest("敏感词已存在");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @PutMapping("/api/admin/sensitive-words/{id}")
    public ApiResponse<SensitiveWordVO> update(
            @PathVariable Long id,
            @Valid @RequestBody SensitiveWordRequest request
    ) {
        String matchType = normalizeMatchType(request.matchType());
        String severity = normalizeSeverity(request.severity());
        int affected = jdbcTemplate.update("""
                        update sensitive_words
                        set word = ?, match_type = ?, severity = ?, enabled = ?, updated_at = now()
                        where id = ?
                        """,
                request.word().trim(),
                matchType,
                severity,
                request.enabled(),
                id);
        if (affected == 0) {
            throw BusinessException.notFound("敏感词不存在");
        }
        return ApiResponse.ok(getWord(id));
    }

    @Transactional(rollbackFor = Exception.class)
    @DeleteMapping("/api/admin/sensitive-words/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        int affected = jdbcTemplate.update("delete from sensitive_words where id = ?", id);
        if (affected == 0) {
            throw BusinessException.notFound("敏感词不存在");
        }
        return ApiResponse.ok(null);
    }

    @Transactional(rollbackFor = Exception.class)
    @PutMapping("/api/admin/sensitive-words/{id}/toggle")
    public ApiResponse<SensitiveWordVO> toggle(@PathVariable Long id) {
        Boolean current = jdbcTemplate.query("select enabled from sensitive_words where id = ?",
                (rs, rowNum) -> rs.getBoolean("enabled"),
                id).stream().findFirst().orElseThrow(() -> BusinessException.notFound("敏感词不存在"));
        jdbcTemplate.update("update sensitive_words set enabled = ?, updated_at = now() where id = ?",
                !current, id);
        return ApiResponse.ok(getWord(id));
    }

    private SensitiveWordVO getWord(Long id) {
        return jdbcTemplate.query("""
                        select s.id, s.word, s.match_type, s.severity, s.enabled, s.created_at
                        from sensitive_words s
                        where s.id = ?
                        """,
                (rs, rowNum) -> new SensitiveWordVO(
                        rs.getLong("id"),
                        rs.getString("word"),
                        rs.getString("match_type"),
                        rs.getString("severity"),
                        rs.getBoolean("enabled"),
                        rs.getObject("created_at", OffsetDateTime.class)
                ),
                id).stream().findFirst().orElseThrow(() -> BusinessException.notFound("敏感词不存在"));
    }

    private String normalizeMatchType(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase();
        if (!MATCH_TYPES.contains(normalized)) {
            throw BusinessException.badRequest("匹配方式不合法");
        }
        return normalized;
    }

    private String normalizeSeverity(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase();
        if (!SEVERITIES.contains(normalized)) {
            throw BusinessException.badRequest("处理级别不合法");
        }
        return normalized;
    }

    public record SensitiveWordRequest(
            @NotBlank @Size(max = 120) String word,
            @NotBlank String matchType,
            @NotBlank String severity,
            @NotNull Boolean enabled
    ) {
    }

    public record SensitiveWordVO(
            Long id,
            String word,
            String matchType,
            String severity,
            Boolean enabled,
            OffsetDateTime createdAt
    ) {
    }
}
