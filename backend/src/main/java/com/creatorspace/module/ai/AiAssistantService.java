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
import java.util.function.Consumer;

@Service
public class AiAssistantService {

    private static final String DISABLED_NOTICE = "AI 助手未启用，请配置 AI_ENABLED=true 和模型参数后再生成建议。";
    private static final String SYSTEM_PROMPT = """
            你是 CreatorSpace 后台 AI 创作与运营助手。你只能提供建议，不能替管理员发布、删除、审核或修改内容。
            回答必须围绕个人主题博客、作品展示、灵感墙、评论审核和运营数据。不要输出密钥、Token、数据库密码或任何私密内容。
            请使用以下结构输出：
            【回复】
            用简洁中文说明你的判断。
            【建议】
            - SUMMARY：可采纳建议内容
            - TAG：可采纳建议内容
            - REVIEW_NOTE：可采纳建议内容
            - OPERATION_IDEA：可采纳建议内容
            可按任务需要替换类型为 TOPIC_IDEA、HOMEPAGE_RECOMMENDATION、RISK_HINT、WORKFLOW_STEP。
            """;

    private final JdbcTemplate jdbcTemplate;
    private final AiModelClient aiModelClient;
    private final boolean enabled;
    private final String provider;
    private final String modelName;

    public AiAssistantService(
            JdbcTemplate jdbcTemplate,
            AiModelClient aiModelClient,
            @Value("${app.ai.enabled:false}") boolean enabled,
            @Value("${app.ai.provider:local}") String provider,
            @Value("${app.ai.zhipu-model:local-rule-assistant}") String modelName
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.aiModelClient = aiModelClient;
        this.enabled = enabled;
        this.provider = provider == null || provider.isBlank() ? "local" : provider.trim();
        this.modelName = modelName == null || modelName.isBlank() ? "local-rule-assistant" : modelName.trim();
    }

    @Transactional
    public AiTaskVO createTask(AiTaskRequest request) {
        String taskType = normalize(request.taskType(), "任务类型不能为空");
        String targetType = normalizeNullable(request.targetType());
        String prompt = normalize(request.prompt(), "提示词不能为空");
        Long taskId = createTaskRow(taskType, targetType, request.targetId(), prompt);
        insertMessage(taskId, "USER", prompt);
        return runTask(taskId, taskType, targetType, request.targetId(), prompt, List.of(), null);
    }

    @Transactional
    public AiTaskVO continueTask(Long id, AiContinueRequest request) {
        String prompt = normalize(request.prompt(), "追问内容不能为空");
        AiTaskHeader task = taskHeader(id);
        insertMessage(id, "USER", prompt);
        return runTask(id, task.taskType(), task.targetType(), task.targetId(), prompt, conversationByTask(id), null);
    }

    @Transactional
    public AiTaskVO createWorkflow(AiWorkflowRequest request) {
        String workflowType = normalize(request.workflowType(), "工作流类型不能为空");
        int days = Math.min(90, Math.max(1, request.days() == null ? 7 : request.days()));
        WorkflowPrompt workflow = buildWorkflowPrompt(workflowType, days);
        Long taskId = createTaskRow(workflowType, "SITE", null, workflow.prompt());
        insertMessage(taskId, "SYSTEM", workflow.context());
        insertMessage(taskId, "USER", workflow.prompt());
        return runTask(taskId, workflowType, "SITE", null, workflow.prompt(), List.of(new AiModelClient.ChatMessage("SYSTEM", workflow.context())), workflow.context());
    }

    @Transactional
    public AiTaskVO createTaskStreaming(AiTaskRequest request, Consumer<String> onDelta) {
        String taskType = normalize(request.taskType(), "任务类型不能为空");
        String targetType = normalizeNullable(request.targetType());
        String prompt = normalize(request.prompt(), "提示词不能为空");
        Long taskId = createTaskRow(taskType, targetType, request.targetId(), prompt);
        insertMessage(taskId, "USER", prompt);
        return runTaskStreaming(taskId, taskType, targetType, request.targetId(), prompt, List.of(), null, onDelta);
    }

    @Transactional
    public AiTaskVO continueTaskStreaming(Long id, AiContinueRequest request, Consumer<String> onDelta) {
        String prompt = normalize(request.prompt(), "追问内容不能为空");
        AiTaskHeader task = taskHeader(id);
        insertMessage(id, "USER", prompt);
        return runTaskStreaming(id, task.taskType(), task.targetType(), task.targetId(), prompt, conversationByTask(id), null, onDelta);
    }

    @Transactional
    public AiTaskVO createWorkflowStreaming(AiWorkflowRequest request, Consumer<String> onDelta) {
        String workflowType = normalize(request.workflowType(), "工作流类型不能为空");
        int days = Math.min(90, Math.max(1, request.days() == null ? 7 : request.days()));
        WorkflowPrompt workflow = buildWorkflowPrompt(workflowType, days);
        Long taskId = createTaskRow(workflowType, "SITE", null, workflow.prompt());
        insertMessage(taskId, "SYSTEM", workflow.context());
        insertMessage(taskId, "USER", workflow.prompt());
        return runTaskStreaming(taskId, workflowType, "SITE", null, workflow.prompt(), List.of(new AiModelClient.ChatMessage("SYSTEM", workflow.context())), workflow.context(), onDelta);
    }

    public AiTaskVO taskById(Long id) {
        return taskById(id, null);
    }

    public PageResponse<AiTaskVO> tasks(long page, long pageSize) {
        Long total = jdbcTemplate.queryForObject("select count(*) from ai_agent_tasks", Long.class);
        List<AiTaskVO> records = jdbcTemplate.query("""
                        select id, task_type, target_type, target_id, prompt, status, provider, model_name, created_by, created_at, updated_at
                        from ai_agent_tasks
                        order by updated_at desc, id desc
                        limit ? offset ?
                        """,
                (rs, rowNum) -> toTask(rs, null),
                pageSize,
                (page - 1) * pageSize);
        return new PageResponse<>(records, page, pageSize, total == null ? 0 : total);
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

    private Long createTaskRow(String taskType, String targetType, Long targetId, String prompt) {
        Long taskId = jdbcTemplate.queryForObject("""
                        insert into ai_agent_tasks (task_type, target_type, target_id, prompt, status, provider, model_name, created_by)
                        values (?, ?, ?, ?, ?, ?, ?, ?)
                        returning id
                        """,
                Long.class,
                taskType,
                targetType,
                targetId,
                prompt,
                enabled ? "RUNNING" : "FAILED",
                provider,
                modelName,
                currentUserId());
        if (taskId == null) {
            throw BusinessException.badRequest("AI 任务创建失败");
        }
        return taskId;
    }

    private AiTaskVO runTask(Long taskId,
                             String taskType,
                             String targetType,
                             Long targetId,
                             String prompt,
                             List<AiModelClient.ChatMessage> history,
                             String providedContext) {
        if (!enabled) {
            insertMessage(taskId, "ASSISTANT", DISABLED_NOTICE);
            jdbcTemplate.update("update ai_agent_tasks set status = 'FAILED', updated_at = now() where id = ?", taskId);
            return taskById(taskId, DISABLED_NOTICE);
        }
        try {
            String context = providedContext == null ? buildTargetContext(targetType, targetId) : providedContext;
            String assistantMessage = generateAssistantMessage(taskType, prompt, context, history);
            insertMessage(taskId, "ASSISTANT", assistantMessage);
            for (SuggestionDraft draft : extractSuggestions(taskType, assistantMessage)) {
                jdbcTemplate.update("""
                                insert into ai_suggestions (task_id, target_type, target_id, suggestion_type, content, status)
                                values (?, ?, ?, ?, ?, 'PENDING')
                                """,
                        taskId,
                        targetType,
                        targetId,
                        draft.type(),
                        draft.content());
            }
            jdbcTemplate.update("update ai_agent_tasks set status = 'SUCCEEDED', updated_at = now() where id = ?", taskId);
            return taskById(taskId, null);
        } catch (BusinessException exception) {
            insertMessage(taskId, "ASSISTANT", exception.getMessage());
            jdbcTemplate.update("update ai_agent_tasks set status = 'FAILED', updated_at = now() where id = ?", taskId);
            return taskById(taskId, exception.getMessage());
        }
    }

    private AiTaskVO runTaskStreaming(Long taskId,
                                      String taskType,
                                      String targetType,
                                      Long targetId,
                                      String prompt,
                                      List<AiModelClient.ChatMessage> history,
                                      String providedContext,
                                      Consumer<String> onDelta) {
        if (!enabled) {
            insertMessage(taskId, "ASSISTANT", DISABLED_NOTICE);
            onDelta.accept(DISABLED_NOTICE);
            jdbcTemplate.update("update ai_agent_tasks set status = 'FAILED', updated_at = now() where id = ?", taskId);
            return taskById(taskId, DISABLED_NOTICE);
        }
        try {
            String context = providedContext == null ? buildTargetContext(targetType, targetId) : providedContext;
            String assistantMessage = generateAssistantMessageStreaming(taskType, prompt, context, history, onDelta);
            insertMessage(taskId, "ASSISTANT", assistantMessage);
            for (SuggestionDraft draft : extractSuggestions(taskType, assistantMessage)) {
                jdbcTemplate.update("""
                                insert into ai_suggestions (task_id, target_type, target_id, suggestion_type, content, status)
                                values (?, ?, ?, ?, ?, 'PENDING')
                                """,
                        taskId,
                        targetType,
                        targetId,
                        draft.type(),
                        draft.content());
            }
            jdbcTemplate.update("update ai_agent_tasks set status = 'SUCCEEDED', updated_at = now() where id = ?", taskId);
            return taskById(taskId, null);
        } catch (BusinessException exception) {
            insertMessage(taskId, "ASSISTANT", exception.getMessage());
            onDelta.accept(exception.getMessage());
            jdbcTemplate.update("update ai_agent_tasks set status = 'FAILED', updated_at = now() where id = ?", taskId);
            return taskById(taskId, exception.getMessage());
        }
    }
    private String generateAssistantMessage(String taskType, String prompt, String context, List<AiModelClient.ChatMessage> history) {
        if ("local".equalsIgnoreCase(provider) || !aiModelClient.supportsRemoteCall()) {
            return localAssistantMessage(generateLocalSuggestions(taskType, context, prompt));
        }
        return aiModelClient.complete(buildModelMessages(prompt, context, history));
    }

    private String generateAssistantMessageStreaming(String taskType, String prompt, String context, List<AiModelClient.ChatMessage> history, Consumer<String> onDelta) {
        if ("local".equalsIgnoreCase(provider) || !aiModelClient.supportsRemoteCall()) {
            String message = localAssistantMessage(generateLocalSuggestions(taskType, context, prompt));
            onDelta.accept(message);
            return message;
        }
        List<AiModelClient.ChatMessage> messages = buildModelMessages(prompt, context, history);
        return aiModelClient.completeStreaming(messages, onDelta);
    }

    private List<AiModelClient.ChatMessage> buildModelMessages(String prompt, String context, List<AiModelClient.ChatMessage> history) {
        List<AiModelClient.ChatMessage> messages = new ArrayList<>();
        messages.add(new AiModelClient.ChatMessage("SYSTEM", SYSTEM_PROMPT));
        if (context != null && !context.isBlank()) {
            messages.add(new AiModelClient.ChatMessage("SYSTEM", "当前可用业务上下文：\n" + context));
        }
        List<AiModelClient.ChatMessage> conversationHistory = history.stream()
                .filter(message -> !"SYSTEM".equalsIgnoreCase(message.role()))
                .limit(12)
                .toList();
        messages.addAll(conversationHistory);
        if (conversationHistory.isEmpty()) {
            messages.add(new AiModelClient.ChatMessage("USER", prompt));
        }
        return messages;
    }

    private List<SuggestionDraft> generateLocalSuggestions(String taskType, String context, String prompt) {
        String material = shorten((context == null || context.isBlank()) ? prompt : context + "\n" + prompt, 360);
        return switch (taskType) {
            case "SUMMARY" -> List.of(new SuggestionDraft("SUMMARY", "建议摘要：" + material + "。保留核心信息，控制在 80 到 120 字，避免替作者做最终发布决定。"));
            case "TAGS" -> List.of(new SuggestionDraft("TAG", "建议标签：创作记录、经验复盘、内容运营。请由管理员结合实际分类和现有标签确认。"));
            case "REVIEW" -> List.of(new SuggestionDraft("REVIEW_NOTE", "审核建议：内容可读性和主题关联度较高，但发布前建议人工核对事实、版权来源和是否包含隐私信息。"));
            case "OPERATION", "OPERATION_REPORT" -> List.of(
                    new SuggestionDraft("OPERATION_IDEA", "运营建议：优先复盘搜索词、热门内容和待审核队列，把有增长信号的主题加入本周推荐池。"),
                    new SuggestionDraft("WORKFLOW_STEP", "下一步：先处理待审核评论，再补齐热门文章摘要和首页推荐理由。")
            );
            case "TOPIC_IDEAS" -> List.of(new SuggestionDraft("TOPIC_IDEA", "选题建议：围绕最近高频搜索词制作 1 篇教程、1 篇经验复盘和 1 张灵感卡片。"));
            case "HOMEPAGE_RECOMMENDATIONS" -> List.of(new SuggestionDraft("HOMEPAGE_RECOMMENDATION", "首页推荐建议：选择访问高、互动高且仍公开可见的文章或作品，补充一句推荐理由后再上架。"));
            case "COMMENT_RISK_QUEUE" -> List.of(new SuggestionDraft("RISK_HINT", "评论审核建议：优先查看待审核、长文本、包含链接或语气明显攻击性的评论。"));
            default -> List.of(new SuggestionDraft("GENERAL", "建议：" + material + "。该建议仅供参考，需管理员确认后再采纳。"));
        };
    }

    private String localAssistantMessage(List<SuggestionDraft> drafts) {
        StringBuilder builder = new StringBuilder("【回复】\n已根据当前业务数据生成 ").append(drafts.size()).append(" 条建议，建议仍需管理员人工确认。\n【建议】");
        for (SuggestionDraft draft : drafts) {
            builder.append("\n- ").append(draft.type()).append("：").append(draft.content());
        }
        return builder.toString();
    }

    private List<SuggestionDraft> extractSuggestions(String taskType, String assistantMessage) {
        List<SuggestionDraft> drafts = new ArrayList<>();
        boolean inSuggestionBlock = false;
        for (String rawLine : assistantMessage.split("\\R")) {
            String line = rawLine.trim();
            if (line.contains("【建议】")) {
                inSuggestionBlock = true;
                continue;
            }
            if (!inSuggestionBlock || line.isBlank()) {
                continue;
            }
            line = line.replaceFirst("^[-*\\d.、\\s]+", "").trim();
            if (line.isBlank()) {
                continue;
            }
            String[] parts = line.split("[：:]", 2);
            String type = parts.length == 2 ? normalizeSuggestionType(parts[0], taskType) : defaultSuggestionType(taskType);
            String content = parts.length == 2 ? parts[1].trim() : line;
            if (!content.isBlank()) {
                drafts.add(new SuggestionDraft(type, content));
            }
        }
        if (drafts.isEmpty()) {
            drafts.add(new SuggestionDraft(defaultSuggestionType(taskType), shorten(assistantMessage, 900)));
        }
        return drafts.stream().limit(6).toList();
    }

    private String normalizeSuggestionType(String rawType, String taskType) {
        String normalized = rawType == null ? "" : rawType.trim().toUpperCase(Locale.ROOT).replace("建议", "").replace(" ", "_");
        return switch (normalized) {
            case "摘要", "SUMMARY" -> "SUMMARY";
            case "标签", "TAG", "TAGS" -> "TAG";
            case "审核", "REVIEW", "REVIEW_NOTE" -> "REVIEW_NOTE";
            case "运营", "OPERATION", "OPERATION_IDEA" -> "OPERATION_IDEA";
            case "选题", "TOPIC", "TOPIC_IDEA" -> "TOPIC_IDEA";
            case "首页", "推荐", "HOMEPAGE", "HOMEPAGE_RECOMMENDATION" -> "HOMEPAGE_RECOMMENDATION";
            case "风险", "RISK", "RISK_HINT" -> "RISK_HINT";
            case "步骤", "WORKFLOW", "WORKFLOW_STEP" -> "WORKFLOW_STEP";
            default -> defaultSuggestionType(taskType);
        };
    }

    private String defaultSuggestionType(String taskType) {
        return switch (taskType) {
            case "SUMMARY" -> "SUMMARY";
            case "TAGS" -> "TAG";
            case "REVIEW", "COMMENT_RISK_QUEUE" -> "REVIEW_NOTE";
            case "OPERATION", "OPERATION_REPORT" -> "OPERATION_IDEA";
            case "TOPIC_IDEAS" -> "TOPIC_IDEA";
            case "HOMEPAGE_RECOMMENDATIONS" -> "HOMEPAGE_RECOMMENDATION";
            default -> "GENERAL";
        };
    }

    private WorkflowPrompt buildWorkflowPrompt(String workflowType, int days) {
        String context = analyticsContext(days);
        String prompt = switch (workflowType) {
            case "OPERATION_REPORT" -> "请根据最近 " + days + " 天的数据生成一份后台运营日报，包含风险、机会和今日执行顺序。";
            case "TOPIC_IDEAS" -> "请根据最近 " + days + " 天的搜索词、热门内容和访问趋势提出 5 个选题建议。";
            case "HOMEPAGE_RECOMMENDATIONS" -> "请根据最近 " + days + " 天热门文章、热门作品和互动数据提出首页推荐建议。";
            case "COMMENT_RISK_QUEUE" -> "请根据待审核评论和近期互动数据提出评论审核优先级建议。";
            default -> throw BusinessException.badRequest("不支持的 AI 工作流类型");
        };
        return new WorkflowPrompt(prompt, context);
    }

    private String analyticsContext(int days) {
        String interval = days + " days";
        Long visits = scalar("select count(*) from visit_logs where created_at >= now() - cast(? as interval)", interval);
        Long searches = scalar("select count(*) from search_logs where created_at >= now() - cast(? as interval)", interval);
        Long pendingArticles = scalar("select count(*) from articles where status = 'PENDING_REVIEW'");
        Long pendingProjects = scalar("select count(*) from portfolio_projects where status = 'PENDING_REVIEW'");
        Long pendingComments = scalar("select count(*) from comments where status = 'PENDING'");
        return """
                数据窗口：最近 %d 天
                访问量：%d
                搜索次数：%d
                待审核文章：%d
                待审核作品：%d
                待审核评论：%d

                热门搜索词：
                %s

                热门文章：
                %s

                热门作品：
                %s

                待审核评论样本：
                %s
                """.formatted(days, visits, searches, pendingArticles, pendingProjects, pendingComments, topSearches(interval), topArticles(), topProjects(), pendingCommentSamples());
    }

    private Long scalar(String sql, Object... args) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class, args);
        return value == null ? 0 : value;
    }

    private String topSearches(String interval) {
        return String.join("\n", jdbcTemplate.query("""
                        select keyword || '（' || count(*) || ' 次）' as item
                        from search_logs
                        where created_at >= now() - cast(? as interval)
                        group by keyword
                        order by count(*) desc, max(created_at) desc
                        limit 8
                        """,
                (rs, rowNum) -> "- " + rs.getString("item"),
                interval));
    }

    private String topArticles() {
        return String.join("\n", jdbcTemplate.query("""
                        select a.title || '（浏览 ' || coalesce(cs.view_count, a.view_count, 0) || '，点赞 ' || coalesce(cs.like_count, a.like_count, 0) || '）' as item
                        from articles a
                        left join content_statistics cs on cs.target_type = 'ARTICLE' and cs.target_id = a.id
                        where a.status = 'PUBLISHED' and a.privacy_type = 'PUBLIC'
                        order by (coalesce(cs.view_count, a.view_count, 0) + coalesce(cs.like_count, a.like_count, 0) * 3 + coalesce(cs.comment_count, a.comment_count, 0) * 4) desc
                        limit 5
                        """,
                (rs, rowNum) -> "- " + rs.getString("item")));
    }

    private String topProjects() {
        return String.join("\n", jdbcTemplate.query("""
                        select p.title || '（浏览 ' || coalesce(cs.view_count, 0) || '，点赞 ' || coalesce(cs.like_count, 0) || '）' as item
                        from portfolio_projects p
                        left join content_statistics cs on cs.target_type = 'PROJECT' and cs.target_id = p.id
                        where p.status = 'VISIBLE'
                        order by (coalesce(cs.view_count, 0) + coalesce(cs.like_count, 0) * 3 + coalesce(cs.favorite_count, 0) * 4) desc
                        limit 5
                        """,
                (rs, rowNum) -> "- " + rs.getString("item")));
    }

    private String pendingCommentSamples() {
        return String.join("\n", jdbcTemplate.query("""
                        select '评论 ID ' || id || '：' || left(regexp_replace(content, '\\s+', ' ', 'g'), 120) as item
                        from comments
                        where status = 'PENDING'
                        order by created_at asc
                        limit 5
                        """,
                (rs, rowNum) -> "- " + rs.getString("item")));
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

    private AiTaskHeader taskHeader(Long id) {
        AiTaskHeader task = jdbcTemplate.query("""
                        select task_type, target_type, target_id
                        from ai_agent_tasks
                        where id = ?
                        """,
                rs -> rs.next() ? new AiTaskHeader(rs.getString("task_type"), rs.getString("target_type"), nullableLong(rs, "target_id")) : null,
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
                        select title, coalesce(summary, '') as summary, left(content_markdown, 1600) as content_markdown, status, privacy_type
                        from articles
                        where id = ?
                        """,
                rs -> {
                    if (!rs.next()) {
                        throw BusinessException.notFound("文章不存在");
                    }
                    if (!"PUBLIC".equals(rs.getString("privacy_type"))) {
                        throw BusinessException.forbidden("AI 不读取非公开文章内容");
                    }
                    return "标题：" + rs.getString("title") + "\n状态：" + rs.getString("status") + "\n摘要：" + rs.getString("summary") + "\n正文：" + rs.getString("content_markdown");
                },
                id);
    }

    private String projectContext(Long id) {
        return jdbcTemplate.query("""
                        select title, coalesce(description, '') as description, left(content_markdown, 1600) as content_markdown, status
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
                    return "标题：" + rs.getString("title") + "\n状态：" + rs.getString("status") + "\n描述：" + rs.getString("description") + "\n正文：" + rs.getString("content_markdown");
                },
                id);
    }

    private String commentContext(Long id) {
        return jdbcTemplate.query("""
                        select left(content, 1000) as content, status
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

    private List<AiModelClient.ChatMessage> conversationByTask(Long taskId) {
        return jdbcTemplate.query("""
                        select role, content
                        from ai_agent_messages
                        where task_id = ?
                        order by created_at asc, id asc
                        """,
                (rs, rowNum) -> new AiModelClient.ChatMessage(rs.getString("role"), rs.getString("content")),
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

    private record AiTaskHeader(String taskType, String targetType, Long targetId) {
    }

    private record WorkflowPrompt(String prompt, String context) {
    }

    public record AiTaskRequest(String taskType, String targetType, Long targetId, String prompt) {
    }

    public record AiContinueRequest(String prompt) {
    }

    public record AiWorkflowRequest(String workflowType, Integer days) {
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
