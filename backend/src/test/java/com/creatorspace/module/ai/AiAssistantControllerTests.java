package com.creatorspace.module.ai;

import com.creatorspace.testsupport.PostgresIntegrationTestSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AiAssistantControllerTests extends PostgresIntegrationTestSupport {

    @Container
    private static final PostgreSQLContainer POSTGRES = createPostgres("creatorspace_ai_assistant_test");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, POSTGRES);
        registry.add("app.ai.enabled", () -> "false");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void aiAdminApisRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/admin/ai/suggestions"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    void disabledAiCreatesFailedTaskWithClearNotice() throws Exception {
        String token = loginAsAdmin();

        mockMvc.perform(post("/api/admin/ai/tasks")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "taskType", "SUMMARY",
                                "targetType", "ARTICLE",
                                "prompt", "请帮我生成一段文章摘要"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("FAILED")))
                .andExpect(jsonPath("$.data.notice", containsString("AI 助手未启用")))
                .andExpect(jsonPath("$.data.messages[1].content", containsString("AI 助手未启用")));

        Long failedTasks = jdbcTemplate.queryForObject(
                "select count(*) from ai_agent_tasks where task_type = 'SUMMARY' and status = 'FAILED'",
                Long.class);
        assertThat(failedTasks).isGreaterThanOrEqualTo(1L);
    }

    @Test
    void disabledAiCreatesWorkflowTaskWithClearNotice() throws Exception {
        String token = loginAsAdmin();

        mockMvc.perform(post("/api/admin/ai/workflows")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "workflowType", "OPERATION_REPORT",
                                "days", 7
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.taskType", is("OPERATION_REPORT")))
                .andExpect(jsonPath("$.data.targetType", is("SITE")))
                .andExpect(jsonPath("$.data.status", is("FAILED")))
                .andExpect(jsonPath("$.data.notice", containsString("AI 助手未启用")))
                .andExpect(jsonPath("$.data.messages[0].role", is("SYSTEM")))
                .andExpect(jsonPath("$.data.messages[1].role", is("USER")))
                .andExpect(jsonPath("$.data.messages[2].content", containsString("AI 助手未启用")));

        Integer workflowCount = jdbcTemplate.queryForObject("""
                        select count(*)
                        from ai_agent_tasks
                        where task_type = 'OPERATION_REPORT'
                          and target_type = 'SITE'
                          and status = 'FAILED'
                        """,
                Integer.class);
        assertThat(workflowCount).isGreaterThanOrEqualTo(1);
    }
    @Test
    void adminCanAdoptSuggestionAndWriteOperationLog() throws Exception {
        String token = loginAsAdmin();
        Long suggestionId = jdbcTemplate.queryForObject("""
                        insert into ai_suggestions (target_type, target_id, suggestion_type, content, status)
                        values ('ARTICLE', 1, 'SUMMARY', '建议补充更清晰的摘要。', 'PENDING')
                        returning id
                        """,
                Long.class);

        mockMvc.perform(put("/api/admin/ai/suggestions/{id}/adopt", suggestionId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("ADOPTED")))
                .andExpect(jsonPath("$.data.adoptedBy").isNumber());

        Integer logCount = jdbcTemplate.queryForObject("""
                        select count(*)
                        from operation_logs
                        where module = 'AI'
                          and target_type = 'AI_SUGGESTION'
                          and target_id = ?
                          and operation = '采纳AI建议'
                        """,
                Integer.class,
                suggestionId);
        assertThat(logCount).isEqualTo(1);
    }

    private String loginAsAdmin() throws Exception {
        String response = mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", ADMIN_USERNAME,
                                "password", ADMIN_PASSWORD
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.accessToken", not("")))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).path("data").path("accessToken").asText();
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
