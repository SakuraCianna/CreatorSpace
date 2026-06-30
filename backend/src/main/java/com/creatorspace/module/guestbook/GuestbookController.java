package com.creatorspace.module.guestbook;

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
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 留言板公开接口和管理员审核接口。
 */
@Validated
@RestController
public class GuestbookController {

    private static final Set<String> STATUSES = Set.of("PENDING", "APPROVED", "REJECTED", "SPAM");

    private final JdbcTemplate jdbcTemplate;

    public GuestbookController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // 查询公开留言列表，只返回已审核通过的内容。
    @GetMapping("/api/guestbook")
    public ApiResponse<PageResponse<GuestbookVO>> listPublic(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize
    ) {
        long offset = (page - 1) * pageSize;
        Long total = jdbcTemplate.queryForObject(
                "select count(*) from guestbook_entries where status = 'APPROVED'",
                Long.class);
        List<GuestbookVO> records = jdbcTemplate.query("""
                        select g.id,
                               g.user_id,
                               g.display_name,
                               g.content,
                               g.status,
                               g.like_count,
                               g.created_at
                        from guestbook_entries g
                        where g.status = 'APPROVED'
                        order by g.created_at desc, g.id desc
                        limit ? offset ?
                        """,
                (rs, rowNum) -> new GuestbookVO(
                        rs.getLong("id"),
                        rs.getObject("user_id") == null ? null : rs.getLong("user_id"),
                        rs.getString("display_name"),
                        rs.getString("content"),
                        rs.getString("status"),
                        rs.getLong("like_count"),
                        rs.getObject("created_at", OffsetDateTime.class)
                ),
                pageSize,
                offset);
        return ApiResponse.ok(new PageResponse<>(records, page, pageSize, total == null ? 0 : total));
    }

    // 登录用户提交留言。
    @Transactional(rollbackFor = Exception.class)
    @PostMapping("/api/guestbook")
    public ApiResponse<GuestbookVO> create(
            @AuthenticationPrincipal LoginUser loginUser,
            @Valid @RequestBody GuestbookRequest request,
            HttpServletRequest servletRequest
    ) {
        String content = ensureContentAllowed(request.content());
        checkGuestbookSpam(loginUser.userId());
        Long id = jdbcTemplate.queryForObject("""
                        insert into guestbook_entries (
                            user_id, display_name, content, status,
                            ip_address, user_agent
                        )
                        values (?, ?, ?, 'PENDING', cast(? as inet), ?)
                        returning id
                        """,
                Long.class,
                loginUser.userId(),
                loginUser.username(),
                content,
                servletRequest.getRemoteAddr(),
                servletRequest.getHeader("User-Agent"));
        return ApiResponse.ok(getEntry(id));
    }

    // 登录用户删除自己的留言。
    @Transactional(rollbackFor = Exception.class)
    @DeleteMapping("/api/guestbook/{id}")
    public ApiResponse<Void> deleteMine(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable Long id
    ) {
        int affected = jdbcTemplate.update(
                "delete from guestbook_entries where id = ? and user_id = ?",
                id, loginUser.userId());
        if (affected == 0) {
            throw BusinessException.notFound("留言不存在或无权删除");
        }
        return ApiResponse.ok(null);
    }

    // 登录用户编辑自己的留言，修改后重新进入审核队列。
    @Transactional(rollbackFor = Exception.class)
    @PutMapping("/api/guestbook/{id}")
    public ApiResponse<GuestbookVO> updateMine(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable Long id,
            @Valid @RequestBody GuestbookUpdateRequest request
    ) {
        String status = jdbcTemplate.query("""
                        select status from guestbook_entries
                        where id = ? and user_id = ?
                        """,
                (rs, rowNum) -> rs.getString("status"),
                id, loginUser.userId()).stream().findFirst()
                .orElseThrow(() -> BusinessException.notFound("留言不存在或无权编辑"));
        if (!"APPROVED".equals(status) && !"PENDING".equals(status)) {
            throw BusinessException.badRequest("当前状态不能编辑");
        }
        String maskedContent = ensureContentAllowed(request.content());
        jdbcTemplate.update("""
                        update guestbook_entries
                        set content = ?, status = 'PENDING', updated_at = now()
                        where id = ? and user_id = ?
                        """,
                maskedContent, id, loginUser.userId());
        return ApiResponse.ok(getEntry(id));
    }

    // 管理员批量审核通过留言。
    @Transactional(rollbackFor = Exception.class)
    @PutMapping("/api/admin/guestbook/batch-approve")
    public ApiResponse<Void> batchApprove(@RequestBody IdListRequest request) {
        for (Long entryId : request.ids()) {
            try {
                updateStatus(entryId, "APPROVED");
            } catch (Exception ignored) {
            }
        }
        return ApiResponse.ok(null);
    }

    // 管理员批量驳回留言。
    @Transactional(rollbackFor = Exception.class)
    @PutMapping("/api/admin/guestbook/batch-reject")
    public ApiResponse<Void> batchReject(@RequestBody IdListRequest request) {
        for (Long entryId : request.ids()) {
            try {
                updateStatus(entryId, "REJECTED");
            } catch (Exception ignored) {
            }
        }
        return ApiResponse.ok(null);
    }

    // 管理员查看全部留言。
    @GetMapping("/api/admin/guestbook")
    public ApiResponse<PageResponse<GuestbookVO>> listAdmin(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize
    ) {
        String normalizedStatus = status == null || status.isBlank() ? "" : status.trim().toUpperCase();
        if (!normalizedStatus.isEmpty() && !STATUSES.contains(normalizedStatus)) {
            throw BusinessException.badRequest("留言状态不合法");
        }
        String where = normalizedStatus.isEmpty() ? "where 1 = 1" : "where g.status = ?";
        Object[] countParams = normalizedStatus.isEmpty() ? new Object[]{} : new Object[]{normalizedStatus};
        Long total = jdbcTemplate.queryForObject(
                "select count(*) from guestbook_entries g " + where,
                Long.class,
                countParams);

        List<Object> params = new java.util.ArrayList<>();
        if (!normalizedStatus.isEmpty()) {
            params.add(normalizedStatus);
        }
        params.add(pageSize);
        params.add((page - 1) * pageSize);

        List<GuestbookVO> records = jdbcTemplate.query("""
                        select g.id,
                               g.user_id,
                               g.display_name,
                               g.content,
                               g.status,
                               g.like_count,
                               g.created_at
                        from guestbook_entries g
                        %s
                        order by g.created_at desc, g.id desc
                        limit ? offset ?
                        """.formatted(where),
                (rs, rowNum) -> new GuestbookVO(
                        rs.getLong("id"),
                        rs.getObject("user_id") == null ? null : rs.getLong("user_id"),
                        rs.getString("display_name"),
                        rs.getString("content"),
                        rs.getString("status"),
                        rs.getLong("like_count"),
                        rs.getObject("created_at", OffsetDateTime.class)
                ),
                params.toArray());
        return ApiResponse.ok(new PageResponse<>(records, page, pageSize, total == null ? 0 : total));
    }

    // 管理员审核通过留言。
    @Transactional(rollbackFor = Exception.class)
    @PutMapping("/api/admin/guestbook/{id}/approve")
    public ApiResponse<GuestbookVO> approve(@PathVariable Long id) {
        updateStatus(id, "APPROVED");
        return ApiResponse.ok(getEntry(id));
    }

    // 管理员驳回留言。
    @Transactional(rollbackFor = Exception.class)
    @PutMapping("/api/admin/guestbook/{id}/reject")
    public ApiResponse<GuestbookVO> reject(@PathVariable Long id) {
        updateStatus(id, "REJECTED");
        return ApiResponse.ok(getEntry(id));
    }

    private void checkGuestbookSpam(Long userId) {
        Long recent = jdbcTemplate.queryForObject("""
                        select count(*) from guestbook_entries
                        where user_id = ?
                          and created_at > ? - interval '1 minute'
                        """,
                Long.class, userId, java.time.OffsetDateTime.now());
        if (recent != null && recent >= 5) {
            throw BusinessException.tooManyRequests("留言过于频繁，请稍后再试");
        }
    }

    private String ensureContentAllowed(String content) {
        String result = content.trim();
        List<String> words = jdbcTemplate.query("""
                        select word, match_type, severity
                        from sensitive_words
                        where enabled = true
                        order by id
                        """,
                (rs, rowNum) -> rs.getString("word") + "|" + rs.getString("match_type") + "|" + rs.getString("severity"));
        for (String entry : words) {
            String[] parts = entry.split("\\|", 3);
            String word = parts[0];
            String matchType = parts[1];
            String severity = parts[2];
            if (!matchesSensitiveWord(result, word, matchType)) {
                continue;
            }
            switch (severity) {
                case "REJECT" -> throw BusinessException.badRequest("留言包含敏感内容");
                case "MASK" -> result = maskContent(result, word, matchType);
                case "REVIEW" -> { /* PENDING status handles this */ }
            }
        }
        return result;
    }

    private boolean matchesSensitiveWord(String content, String word, String matchType) {
        return switch (matchType) {
            case "EXACT" -> content.equalsIgnoreCase(word);
            case "REGEX" -> {
                try {
                    yield Pattern.compile(word, Pattern.CASE_INSENSITIVE).matcher(content).find();
                } catch (PatternSyntaxException e) {
                    yield true;
                }
            }
            default -> content.toLowerCase().contains(word.toLowerCase());
        };
    }

    private String maskContent(String content, String word, String matchType) {
        return switch (matchType) {
            case "EXACT" -> content.replaceAll("(?i)\\Q" + word + "\\E", "***");
            case "REGEX" -> {
                try {
                    yield Pattern.compile(word, Pattern.CASE_INSENSITIVE).matcher(content).replaceAll("***");
                } catch (PatternSyntaxException e) {
                    yield content;
                }
            }
            default -> content.replaceAll("(?i)" + Pattern.quote(word), "***");
        };
    }

    private void updateStatus(Long id, String status) {
        int affected = jdbcTemplate.update(
                "update guestbook_entries set status = ?, updated_at = now() where id = ?",
                status, id);
        if (affected == 0) {
            throw BusinessException.notFound("留言不存在");
        }
    }

    private GuestbookVO getEntry(Long id) {
        return jdbcTemplate.query("""
                        select g.id,
                               g.user_id,
                               g.display_name,
                               g.content,
                               g.status,
                               g.like_count,
                               g.created_at
                        from guestbook_entries g
                        where g.id = ?
                        """,
                (rs, rowNum) -> new GuestbookVO(
                        rs.getLong("id"),
                        rs.getObject("user_id") == null ? null : rs.getLong("user_id"),
                        rs.getString("display_name"),
                        rs.getString("content"),
                        rs.getString("status"),
                        rs.getLong("like_count"),
                        rs.getObject("created_at", OffsetDateTime.class)
                ),
                id).stream().findFirst().orElseThrow(() -> BusinessException.notFound("留言不存在"));
    }

    public record GuestbookRequest(
            @NotBlank @Size(min = 2, max = 2000) String content
    ) {
    }

    public record GuestbookUpdateRequest(
            @NotBlank @Size(min = 2, max = 2000) String content
    ) {
    }

    public record GuestbookVO(
            Long id,
            Long userId,
            String displayName,
            String content,
            String status,
            Long likeCount,
            OffsetDateTime createdAt
    ) {
    }

    public record IdListRequest(
            @NotNull List<Long> ids
    ) {
    }
}
