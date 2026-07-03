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
import java.util.concurrent.atomic.AtomicBoolean;

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

    public List<String> generateHotTopics() {
        if (!enabled) {
            return List.of("开启AI获取推荐", "分享你的开发经验", "IT职场心得");
        }
        
        String context = "当前热门文章：\n" + topArticles() + "\n近期热门搜索：\n" + topSearches("7 days");
        String prompt = "你是CreatorSpace的AI创作助手。请根据上述近期平台热门文章和搜索词，为创作者随机推荐3到5个能够吸引流量的、具体的创作热点或灵感话题。每行一个话题，不需要序号，不需要任何前言或解释。只输出话题本身。";
        
        try {
            String response;
            if ("local".equalsIgnoreCase(provider) || !aiModelClient.supportsRemoteCall()) {
                response = "如何使用大模型提升开发效率\nIT疑难杂症排查指南\n开源项目实战解析\n我的职业成长复盘";
            } else {
                List<AiModelClient.ChatMessage> messages = new ArrayList<>();
                messages.add(new AiModelClient.ChatMessage("SYSTEM", prompt));
                messages.add(new AiModelClient.ChatMessage("USER", context));
                response = aiModelClient.complete(messages);
            }
            
            return java.util.Arrays.stream(response.split("\\R"))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .map(s -> s.replaceFirst("^\\d+\\.\\s*", "").replaceFirst("^[-*]\\s*", ""))
                    .limit(5)
                    .toList();
        } catch (Exception e) {
            return List.of("如何写出爆款文章", "技术进阶路线分享", "独立开发者的经验谈");
        }
    }

    public CreatorAiResponse generateCreatorText(CreatorAiRequest request) {
        String mode = normalizeCreatorMode(request.mode());
        String prompt = trimToNull(request.prompt());
        String title = trimToNull(request.title());
        String context = trimToNull(request.context());
        String selection = trimToNull(request.selection());
        if (prompt == null && title == null && context == null && selection == null) {
            throw BusinessException.badRequest("请先输入标题、正文或 AI 创作要求");
        }

        try {
            return new CreatorAiResponse(mode, aiModelClient.complete(buildCreatorMessages(mode, prompt, title, context, selection)), null);
        } catch (Exception exception) {
            return new CreatorAiResponse(
                    mode,
                    localCreatorText(mode, prompt, title, context, selection),
                    null
            );
        }
    }

    public CreatorAiResponse generateCreatorTextStreaming(CreatorAiRequest request, Consumer<String> onDelta) {
        String mode = normalizeCreatorMode(request.mode());
        String prompt = trimToNull(request.prompt());
        String title = trimToNull(request.title());
        String context = trimToNull(request.context());
        String selection = trimToNull(request.selection());
        if (prompt == null && title == null && context == null && selection == null) {
            throw BusinessException.badRequest("请先输入标题、正文或 AI 创作要求");
        }

        AtomicBoolean streamStarted = new AtomicBoolean(false);
        Consumer<String> guardedDelta = delta -> {
            streamStarted.set(true);
            onDelta.accept(delta);
        };
        try {
            String text = aiModelClient.completeStreaming(buildCreatorMessages(mode, prompt, title, context, selection), guardedDelta);
            return new CreatorAiResponse(mode, text, null);
        } catch (Exception exception) {
            if (streamStarted.get()) {
                throw exception;
            }
            String text = localCreatorText(mode, prompt, title, context, selection);
            streamLocalText(text, onDelta);
            return new CreatorAiResponse(mode, text, null);
        }
    }

    public String generateText(String prompt, String context) {
        return generateCreatorText(new CreatorAiRequest("CUSTOM", null, prompt, context, null)).text();
    }

    private List<AiModelClient.ChatMessage> buildCreatorMessages(String mode, String prompt, String title, String context, String selection) {
        List<AiModelClient.ChatMessage> messages = new ArrayList<>();
        messages.add(new AiModelClient.ChatMessage("SYSTEM", "你是 CreatorSpace 普通用户创建博客时的 AI 创作助手。你只帮助作者构思、续写、润色、摘要和整理资料线索，不替用户发布、不承诺事实正确、不编造引用来源。输出必须简洁、可直接放进博客编辑器，优先使用 Markdown。"));
        StringBuilder user = new StringBuilder();
        user.append("任务模式：").append(mode).append('\n');
        user.append("输出要求：").append(creatorModeInstruction(mode)).append("\n\n");
        if (title != null) {
            user.append("当前标题：\n").append(title).append("\n\n");
        }
        if (selection != null) {
            user.append("用户当前选中的片段：\n").append(shorten(selection, 1800)).append("\n\n");
        }
        if (context != null) {
            user.append("当前草稿上下文：\n").append(shorten(context, 6000)).append("\n\n");
        }
        if (prompt != null) {
            user.append("用户补充要求：\n").append(prompt).append('\n');
        }
        messages.add(new AiModelClient.ChatMessage("USER", user.toString().trim()));
        return messages;
    }

    private String creatorModeInstruction(String mode) {
        return switch (mode) {
            case "OUTLINE" -> "生成一份博客大纲，包含标题、导语方向、3 到 6 个二级标题和每节要点。";
            case "CONTINUE" -> "基于当前草稿自然续写 2 到 4 段，不重复已有内容，保持作者原有语气。";
            case "POLISH" -> "润色选中片段或全文片段，保留原意，改善表达、结构和可读性，只输出润色后的正文。";
            case "SUMMARY" -> "生成 80 到 140 字文章摘要，适合作为发布设置中的摘要字段。";
            case "TITLE" -> "生成 5 个可选博客标题，每行一个，不要解释。";
            case "TAGS" -> "生成 5 到 8 个短标签，每行一个，不带 #，避免太宽泛。";
            case "CODE" -> "按用户要求生成代码片段或技术示例，使用 Markdown 代码块，并补充必要说明。";
            case "RESEARCH" -> "整理资料检索方向、关键词和需要核验的问题，不编造具体论文、链接或作者。";
            default -> "按用户要求生成可直接放入博客编辑器的 Markdown 内容。";
        };
    }

    private String localCreatorText(String mode, String prompt, String title, String context, String selection) {
        String topic = firstNonBlank(title, prompt, "这篇博客");
        String material = firstNonBlank(selection, context, prompt, topic);
        String brief = shorten(material, 180);
        return switch (mode) {
            case "OUTLINE" -> "# " + topic + "\n\n## 开篇：为什么这个话题值得写\n- 交代问题背景和读者会获得什么。\n- 用一个具体场景引出正文。\n\n## 核心问题\n- 梳理目前遇到的关键挑战。\n- 点出容易被忽略的细节。\n\n## 解决思路\n- 拆成步骤、方法或经验清单。\n- 每一步配一个简短示例。\n\n## 实践复盘\n- 说明哪些做法有效，哪些需要调整。\n- 补充踩坑、权衡和后续计划。\n\n## 结尾\n- 总结最重要的收获。\n- 给读者一个可执行的下一步。";
            case "CONTINUE" -> "接下来可以继续从一个更具体的场景切入：\n\n如果把上面的思路放到真实创作流程里，最重要的不是一次性写完，而是先把问题拆清楚。先确定读者是谁、他们卡在哪里，再决定用教程、复盘还是清单来组织内容。\n\n在写作过程中，可以把每一节都压成一个明确的小结论：这一段解决什么问题、用了什么方法、留下什么经验。这样文章不会只是材料堆叠，而会形成一条可以跟着走的路径。";
            case "POLISH" -> "润色建议稿：\n\n" + brief + "\n\n可以进一步把表达压得更清楚：先说明背景，再给出判断，最后落到具体做法。句子尽量短一些，关键概念前后保持同一个称呼，让读者不用反复猜测上下文。";
            case "SUMMARY" -> "本文围绕“" + topic + "”展开，结合当前创作内容梳理背景、关键问题和实践思路，帮助读者更清晰地理解方法、取舍与后续可执行的改进方向。";
            case "TITLE" -> """
                    %s：一次完整的实践复盘
                    从问题到方案：%s 的写作记录
                    %s 背后的关键思路
                    写给创作者的 %s 入门指南
                    如何把 %s 落到真实项目里
                    """.formatted(topic, topic, topic, topic, topic).trim();
            case "TAGS" -> "创作复盘\n实践记录\n经验总结\n方法论\n项目思考\n内容创作";
            case "CODE" -> "```ts\n// 根据你的需求补充更具体的输入后，AI 可以生成完整示例。\nfunction draftIdea(title: string, context: string) {\n  return `${title}: ${context}`\n}\n```\n\n可以在提示里说明语言、框架、输入输出和边界条件，生成结果会更贴近正文。";
            case "RESEARCH" -> "## 检索方向\n- 关键词：" + topic + "、实践复盘、案例分析、常见问题。\n- 优先核验：定义是否准确、数据是否有来源、案例是否可公开引用。\n- 可补充材料：官方文档、技术博客、项目 README、论文摘要或真实使用记录。\n\n## 写作提醒\n- 不确定的结论先写成观察或假设。\n- 引用外部资料时保留来源链接和访问时间。";
            default -> "## " + topic + "\n\n" + brief + "\n\n可以从背景、问题、方法和复盘四个角度继续展开。先写清楚为什么要做，再说明怎么做，最后总结这次创作或实践带来的启发。";
        };
    }

    private void streamLocalText(String text, Consumer<String> onDelta) {
        int chunkSize = 24;
        for (int index = 0; index < text.length(); index += chunkSize) {
            onDelta.accept(text.substring(index, Math.min(index + chunkSize, text.length())));
        }
    }

    private String normalizeCreatorMode(String value) {
        String mode = normalizeNullable(value);
        if (mode == null) {
            return "CUSTOM";
        }
        return switch (mode) {
            case "OUTLINE", "CONTINUE", "POLISH", "SUMMARY", "TITLE", "TAGS", "CODE", "RESEARCH", "CUSTOM" -> mode;
            default -> "CUSTOM";
        };
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }
    public PageResponse<AiTaskVO> tasks(String status, long page, long pageSize) {
        String normalizedStatus = normalizeNullable(status);
        List<Object> params = new ArrayList<>();
        StringBuilder where = new StringBuilder("where 1 = 1");
        if (normalizedStatus != null && !"ALL".equals(normalizedStatus)) {
            where.append(" and status = ?");
            params.add(normalizedStatus);
        }
        Long total = jdbcTemplate.queryForObject("select count(*) from ai_agent_tasks " + where, Long.class, params.toArray());
        List<Object> listParams = new ArrayList<>(params);
        listParams.add(pageSize);
        listParams.add((page - 1) * pageSize);
        List<AiTaskVO> records = jdbcTemplate.query("""
                        select id, task_type, target_type, target_id, prompt, status, provider, model_name, created_by, created_at, updated_at
                        from ai_agent_tasks
                        %s
                        order by updated_at desc, id desc
                        limit ? offset ?
                        """.formatted(where),
                (rs, rowNum) -> toTask(rs, null),
                listParams.toArray());
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
        if (!usesRemoteModel()) {
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

    private boolean usesRemoteModel() {
        return !"local".equalsIgnoreCase(provider) && aiModelClient.supportsRemoteCall();
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
                    String status = rs.getString("status");
                    if (!"PUBLIC".equals(rs.getString("privacy_type"))) {
                        throw BusinessException.forbidden("AI 不读取非公开文章内容");
                    }
                    if (usesRemoteModel() && !"PUBLISHED".equals(status)) {
                        throw BusinessException.forbidden("远程 AI 不读取未发布文章内容，请切换 AI_PROVIDER=local 后再辅助草稿或待审核内容。");
                    }
                    return "标题：" + rs.getString("title") + "\n状态：" + status + "\n摘要：" + rs.getString("summary") + "\n正文：" + rs.getString("content_markdown");
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
                    String status = rs.getString("status");
                    if ("HIDDEN".equals(status)) {
                        throw BusinessException.forbidden("AI 不读取隐藏作品内容");
                    }
                    if (usesRemoteModel() && !"VISIBLE".equals(status)) {
                        throw BusinessException.forbidden("远程 AI 不读取非可见作品内容，请切换 AI_PROVIDER=local 后再辅助草稿或待审核内容。");
                    }
                    return "标题：" + rs.getString("title") + "\n状态：" + status + "\n描述：" + rs.getString("description") + "\n正文：" + rs.getString("content_markdown");
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

    public record CreatorAiRequest(String mode, String title, String prompt, String context, String selection) {
    }

    public record CreatorAiResponse(String mode, String text, String notice) {
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
