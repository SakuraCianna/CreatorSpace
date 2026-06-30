package com.creatorspace.module.ai;

import com.creatorspace.common.exception.BusinessException;
import com.creatorspace.common.result.PageResponse;
import com.creatorspace.security.LoginUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class AiAssistantService {

    private static final String DISABLED_NOTICE = "AI 助手未启用，请配置 AI_ENABLED=true 和模型参数后再生成建议。";

    private final JdbcTemplate jdbcTemplate;
    private final boolean enabled;
    private final String provider;
    private final String modelName;

    public AiAssistantService(
            JdbcTemplate jdbcTemplate,
            @Value("${app.ai.enabled:false}") boolean enabled,
            @Value("${app.ai.provider:local}") String provider,
            @Value("${app.ai.zhipu-model:local-rule-assistant}") String modelName
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.enabled = enabled;
        this.provider = provider == null || provider.isBlank() ? "local" : provider.trim();
        this.modelName = modelName == null || modelName.isBlank() ? "local-rule-assistant" : modelName.trim();
    }

    @Transactional
    public AiTaskVO createTask(AiTaskRequest request) {
        String taskType = normalize(request.taskType(), "任务类型不能为空");
        String targetType = normalizeNullable(request.targetType());
        String prompt = normalize(request.prompt(), "提示词不能为空");
        Long operatorId = currentUserId();
        String initialStatus = enabled ? "RUNNING" : "FAILED";
        Long taskId = jdbcTemplate.queryForObject("""
                        insert into ai_agent_tasks (task_type, target_type, target_id, prompt, status, provider, model_name, created_by)
                        values (?, ?, ?, ?, ?, ?, ?, ?)
                        returning id
                        """,
                Long.class,
                taskType,
                targetType,
                request.targetId(),
                prompt,
                initialStatus,
                provider,
                modelName,
                operatorId);
        if (taskId == null) {
            throw BusinessException.badRequest("AI 任务创建失败");
        }
        insertMessage(taskId, "USER", prompt);
        if (!enabled) {
            insertMessage(taskId, "ASSISTANT", DISABLED_NOTICE);
            return taskById(taskId, DISABLED_NOTICE);
        }

        String context = buildTargetContext(targetType, request.targetId());
        List<SuggestionDraft> drafts = generateSuggestions(taskType, targetType, request.targetId(), prompt, context);
        String assistantMessage = assistantMessage(drafts);
        insertMessage(taskId, "ASSISTANT", assistantMessage);
        for (SuggestionDraft draft : drafts) {
            jdbcTemplate.update("""
                            insert into ai_suggestions (task_id, target_type, target_id, suggestion_type, content, status)
                            values (?, ?, ?, ?, ?, 'PENDING')
                            """,
                    taskId,
                    targetType,
                    request.targetId(),
                    draft.type(),
                    draft.content());
        }
        jdbcTemplate.update("update ai_agent_tasks set status = 'SUCCEEDED', updated_at = now() where id = ?", taskId);
        return taskById(taskId, null);
    }

    public AiTaskVO taskById(Long id) {
        return taskById(id, null);
    }

    public PageResponse<AiSuggestionVO> suggestions(String status, long page, long pageSize) {
        String normalizedStatus = normalizeNullable(status);
        List<Object> params = new ArrayList<>();
        StringBuilder where = new StringBuilder("where 1 = 1");
        if (normalizedStatus != null && !"ALL".equals(normalizedStatus)) {
            where.append(" and status = ?");
            params.add(normalizedStatus);
        }
        Long total = jdbcTemplate.queryForObject("select count(*) from ai_suggestions " + where, Long.class, params.toArray());
        List<Object> listParams = new ArrayList<>(params);
        listParams.add(pageSize);
        listParams.add((page - 1) * pageSize);
        List<AiSuggestionVO> records = jdbcTemplate.query("""
                        select id, task_id, target_type, target_id, suggestion_type, content, status, adopted_by, adopted_at, created_at
                        from ai_suggestions
                        %s
                        order by created_at desc, id desc
                        limit ? offset ?
                        """.formatted(where),
                (rs, rowNum) -> toSuggestion(rs),
                listParams.toArray());
        return new PageResponse<>(records, page, pageSize, total == null ? 0 : total);
    }

    @Transactional
    public AiSuggestionVO adopt(Long id) {
        return changeSuggestionStatus(id, "ADOPTED");
    }

    @Transactional
    public AiSuggestionVO ignore(Long id) {
        return changeSuggestionStatus(id, "REJECTED");
    }

    private AiTaskVO taskById(Long id, String notice) {
        AiTaskVO task = jdbcTemplate.query("""
                        select id, task_type, target_type, target_id, prompt, status, provider, model_name, created_by, created_at, updated_at
                        from ai_agent_tasks
                        where id = ?
                        """,
                rs -> rs.next() ? toTask(rs, notice) : null,
                id);
        if (task == null) {
            throw BusinessException.notFound("AI 任务不存在");
        }
        return task;
    }

    private AiSuggestionVO changeSuggestionStatus(Long id, String status) {
        AiSuggestionVO current = suggestionById(id);
        if (!"PENDING".equals(current.status())) {
            throw BusinessException.conflict("AI 建议已处理，不能重复操作");
        }
        Long operatorId = currentUserId();
        jdbcTemplate.update("""
                        update ai_suggestions
                        set status = ?, adopted_by = ?, adopted_at = now()
                        where id = ?
                        """,
                status,
                operatorId,
                id);
        return suggestionById(id);
    }

    private AiSuggestionVO suggestionById(Long id) {
        AiSuggestionVO suggestion = jdbcTemplate.query("""
                        select id, task_id, target_type, target_id, suggestion_type, content, status, adopted_by, adopted_at, created_at
                        from ai_suggestions
                        where id = ?
                        """,
                rs -> rs.next() ? toSuggestion(rs) : null,
                id);
        if (suggestion == null) {
            throw BusinessException.notFound("AI 建议不存在");
        }
        return suggestion;
    }

    private void insertMessage(Long taskId, String role, String content) {
        jdbcTemplate.update("""
                        insert into ai_agent_messages (task_id, role, content, token_count)
                        values (?, ?, ?, ?)
                        """,
                taskId,
                role,
                content,
                Math.max(1, content.length() / 4));
    }

    private String buildTargetContext(String targetType, Long targetId) {
        if (targetType == null || targetId == null) {
            return "";
        }
        return switch (targetType) {
            case "ARTICLE" -> articleContext(targetId);
            case "PROJECT" -> projectContext(targetId);
            case "COMMENT" -> commentContext(targetId);
            default -> "";
        };
    }

    private String articleContext(Long id) {
        return jdbcTemplate.query("""
                        select title, coalesce(summary, '') as summary, left(content_markdown, 1200) as content_markdown, status, privacy_type
                        from articles
                        where id = ?
                        """,
                rs -> {
                    if (!rs.next()) {
                        throw BusinessException.notFound("文章不存在");
                    }
                    String privacy = rs.getString("privacy_type");
                    if (!"PUBLIC".equals(privacy)) {
                        throw BusinessException.forbidden("AI 不读取非公开文章内容");
                    }
                    return "标题：" + rs.getString("title") + "\n摘要：" + rs.getString("summary") + "\n正文：" + rs.getString("content_markdown");
                },
                id);
    }

    private String projectContext(Long id) {
        return jdbcTemplate.query("""
                        select title, coalesce(description, '') as description, left(content_markdown, 1200) as content_markdown, status
                        from portfolio_projects
                        where id = ?
                        """,
                rs -> {
                    if (!rs.next()) {
                        throw BusinessException.notFound("作品不存在");
                    }
                    if ("HIDDEN".equals(rs.getString("status"))) {
                        throw BusinessException.forbidden("AI 不读取隐藏作品内容");
                    }
                    return "标题：" + rs.getString("title") + "\n描述：" + rs.getString("description") + "\n正文：" + rs.getString("content_markdown");
                },
                id);
    }

    private String commentContext(Long id) {
        return jdbcTemplate.query("""
                        select left(content, 800) as content, status
                        from comments
                        where id = ?
                        """,
                rs -> {
                    if (!rs.next()) {
                        throw BusinessException.notFound("评论不存在");
                    }
                    return "评论内容：" + rs.getString("content") + "\n状态：" + rs.getString("status");
                },
                id);
    }

    private List<SuggestionDraft> generateSuggestions(String taskType, String targetType, Long targetId, String prompt, String context) {
        String material = shorten((context == null || context.isBlank()) ? prompt : context + "\n" + prompt, 260);
        return switch (taskType) {
            case "SUMMARY" -> List.of(new SuggestionDraft("SUMMARY", "建议摘要：" + material + "。保留核心信息，控制在 80 到 120 字，避免替作者做最终发布决定。"));
            case "TAGS" -> List.of(new SuggestionDraft("TAG", "建议标签：创作记录、经验复盘、内容运营。请由管理员结合实际分类和现有标签确认。"));
            case "REVIEW" -> List.of(new SuggestionDraft("REVIEW_NOTE", "审核建议：内容可读性和主题关联度较高，但发布前建议人工核对事实、版权来源和是否包含隐私信息。"));
            case "OPERATION" -> List.of(new SuggestionDraft("OPERATION_IDEA", "运营建议：可把该内容纳入本周推荐池，并结合搜索词、访问趋势和互动数据判断是否放到首页入口。"));
            default -> List.of(new SuggestionDraft("GENERAL", "建议：" + material + "。该建议仅供参考，需管理员确认后再采纳。"));
        };
    }

    private String assistantMessage(List<SuggestionDraft> drafts) {
        StringBuilder builder = new StringBuilder("已生成 ").append(drafts.size()).append(" 条可采纳建议：");
        for (SuggestionDraft draft : drafts) {
            builder.append("\n- ").append(draft.type()).append("：").append(draft.content());
        }
        return builder.toString();
    }

    private AiTaskVO toTask(ResultSet rs, String notice) throws SQLException {
        Long id = rs.getLong("id");
        return new AiTaskVO(
                id,
                rs.getString("task_type"),
                rs.getString("target_type"),
                nullableLong(rs, "target_id"),
                rs.getString("prompt"),
                rs.getString("status"),
                rs.getString("provider"),
                rs.getString("model_name"),
                nullableLong(rs, "created_by"),
                rs.getObject("created_at", OffsetDateTime.class),
                rs.getObject("updated_at", OffsetDateTime.class),
                messagesByTask(id),
                suggestionsByTask(id),
                notice
        );
    }

    private List<AiMessageVO> messagesByTask(Long taskId) {
        return jdbcTemplate.query("""
                        select id, task_id, role, content, token_count, created_at
                        from ai_agent_messages
                        where task_id = ?
                        order by created_at asc, id asc
                        """,
                (rs, rowNum) -> new AiMessageVO(
                        rs.getLong("id"),
                        rs.getLong("task_id"),
                        rs.getString("role"),
                        rs.getString("content"),
                        rs.getObject("token_count") == null ? null : rs.getInt("token_count"),
                        rs.getObject("created_at", OffsetDateTime.class)
                ),
                taskId);
    }

    private List<AiSuggestionVO> suggestionsByTask(Long taskId) {
        return jdbcTemplate.query("""
                        select id, task_id, target_type, target_id, suggestion_type, content, status, adopted_by, adopted_at, created_at
                        from ai_suggestions
                        where task_id = ?
                        order by created_at asc, id asc
                        """,
                (rs, rowNum) -> toSuggestion(rs),
                taskId);
    }

    private AiSuggestionVO toSuggestion(ResultSet rs) throws SQLException {
        return new AiSuggestionVO(
                rs.getLong("id"),
                nullableLong(rs, "task_id"),
                rs.getString("target_type"),
                nullableLong(rs, "target_id"),
                rs.getString("suggestion_type"),
                rs.getString("content"),
                rs.getString("status"),
                nullableLong(rs, "adopted_by"),
                rs.getObject("adopted_at", OffsetDateTime.class),
                rs.getObject("created_at", OffsetDateTime.class)
        );
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
            return loginUser.userId();
        }
        return null;
    }

    private Long nullableLong(ResultSet rs, String column) throws SQLException {
        Object value = rs.getObject(column);
        return value == null ? null : rs.getLong(column);
    }

    private String normalize(String value, String message) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            throw BusinessException.badRequest(message);
        }
        return normalized;
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String shorten(String value, int maxLength) {
        String normalized = value == null ? "" : value.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...";
    }

    private record SuggestionDraft(String type, String content) {
    }

    public record AiTaskRequest(String taskType, String targetType, Long targetId, String prompt) {
    }

    public record AiTaskVO(
            Long id,
            String taskType,
            String targetType,
            Long targetId,
            String prompt,
            String status,
            String provider,
            String modelName,
            Long createdBy,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            List<AiMessageVO> messages,
            List<AiSuggestionVO> suggestions,
            String notice
    ) {
    }

    public record AiMessageVO(Long id, Long taskId, String role, String content, Integer tokenCount, OffsetDateTime createdAt) {
    }

    public record AiSuggestionVO(
            Long id,
            Long taskId,
            String targetType,
            Long targetId,
            String suggestionType,
            String content,
            String status,
            Long adoptedBy,
            OffsetDateTime adoptedAt,
            OffsetDateTime createdAt
    ) {
    }
}