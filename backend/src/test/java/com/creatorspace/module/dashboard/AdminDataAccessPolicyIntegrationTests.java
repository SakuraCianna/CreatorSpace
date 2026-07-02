package com.creatorspace.module.dashboard;

import com.creatorspace.testsupport.PostgresIntegrationTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItems;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AdminDataAccessPolicyIntegrationTests extends PostgresIntegrationTestSupport {

    @Container
    private static final PostgreSQLContainer POSTGRES = createPostgres("creatorspace_admin_data_policy_test");

    @Container
    private static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, POSTGRES);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("spring.data.redis.password", () -> "");
        registry.add("app.storage.local-root", () -> "target/test-uploads");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void adminDataApisRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/overview"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("请先登录后再访问")));

        mockMvc.perform(get("/api/admin/articles"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("请先登录后再访问")));
    }

    @Test
    void adminDataApisRejectRegularUsers() throws Exception {
        registerUser("p0-admin-policy-user", "regular-secret");
        String userToken = loginAsUser("p0-admin-policy-user", "regular-secret");

        mockMvc.perform(get("/api/admin/dashboard/overview")
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("当前账号没有后台权限或登录状态不匹配")));

        mockMvc.perform(get("/api/admin/articles")
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("当前账号没有后台权限或登录状态不匹配")));
    }

    @Test
    void adminsCanAccessAdminDataApis() throws Exception {
        String adminToken = loginAsAdmin();

        mockMvc.perform(get("/api/admin/dashboard/overview")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.metrics").isArray());

        mockMvc.perform(get("/api/admin/articles")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.page", is(1)))
                .andExpect(jsonPath("$.data.pageSize", is(20)));
    }

    @Test
    void adminPagedDataApisRejectInvalidPagination() throws Exception {
        String adminToken = loginAsAdmin();

        mockMvc.perform(get("/api/admin/articles")
                        .header("Authorization", bearer(adminToken))
                        .param("page", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));

        mockMvc.perform(get("/api/admin/comments")
                        .header("Authorization", bearer(adminToken))
                        .param("pageSize", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));

        mockMvc.perform(get("/api/admin/files")
                        .header("Authorization", bearer(adminToken))
                        .param("pageSize", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    void dashboardOverviewReturnsRealOperationalData() throws Exception {
        String adminToken = loginAsAdmin();
        seedDashboardOverviewSentinels();

        mockMvc.perform(get("/api/admin/dashboard/overview")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.metrics.length()", is(13)))
                .andExpect(jsonPath("$.data.metrics[*].label", hasItems(
                        "文章总数",
                        "作品总数",
                        "灵感总数",
                        "留言总数",
                        "文件总数",
                        "待审核文章",
                        "待审核作品",
                        "待审核评论",
                        "总访问量",
                        "总搜索次数",
                        "总点赞数",
                        "总收藏数"
                )))
                .andExpect(jsonPath("$.data.visitTrend.length()", is(7)))
                .andExpect(jsonPath("$.data.searchTrend.length()", is(7)))
                .andExpect(jsonPath("$.data.visitTrend[6].pv", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.data.searchTrend[6].pv", greaterThanOrEqualTo(12)))
                .andExpect(jsonPath("$.data.hotArticles[0].slug", is("p1-dashboard-hot-article")))
                .andExpect(jsonPath("$.data.hotProjects[0].slug", is("p1-dashboard-hot-project")))
                .andExpect(jsonPath("$.data.hotSearchKeywords[0].keyword", is("p1-dashboard-keyword")))
                .andExpect(jsonPath("$.data.hotSearchKeywords[0].count", greaterThanOrEqualTo(12)))
                .andExpect(jsonPath("$.data.recentActivities[0].operation", is("P1_DASHBOARD_SENTINEL")))
                .andExpect(jsonPath("$.data.recentActivities[0].module", is("DASHBOARD_TEST")));
    }
    private void seedDashboardOverviewSentinels() {
        jdbcTemplate.update("delete from search_logs where keyword in ('p1-dashboard-keyword', 'p1-dashboard-other')");
        jdbcTemplate.update("delete from visit_logs where path like '/p1-dashboard/%'");
        jdbcTemplate.update("delete from operation_logs where operation = 'P1_DASHBOARD_SENTINEL'");
        jdbcTemplate.update("""
                delete from content_statistics
                where target_type = 'ARTICLE'
                  and target_id in (select id from articles where slug like 'p1-dashboard-%')
                """);
        jdbcTemplate.update("""
                delete from content_statistics
                where target_type = 'PROJECT'
                  and target_id in (select id from portfolio_projects where slug like 'p1-dashboard-%')
                """);
        jdbcTemplate.update("delete from articles where slug like 'p1-dashboard-%'");
        jdbcTemplate.update("delete from portfolio_projects where slug like 'p1-dashboard-%'");

        Long hotArticleId = jdbcTemplate.queryForObject("""
                        insert into articles (title, slug, summary, content_markdown, status, privacy_type, view_count, like_count, comment_count, is_top, is_recommend, publish_time)
                        values ('P1 dashboard hot article', 'p1-dashboard-hot-article', 'real statistics article', 'body', 'PUBLISHED', 'PUBLIC', 1, 1, 1, true, true, now())
                        returning id
                        """,
                Long.class);
        Long viewOnlyArticleId = jdbcTemplate.queryForObject("""
                        insert into articles (title, slug, summary, content_markdown, status, privacy_type, view_count, like_count, comment_count, publish_time)
                        values ('P1 dashboard view only article', 'p1-dashboard-view-only-article', 'high views low engagement', 'body', 'PUBLISHED', 'PUBLIC', 1, 0, 0, now())
                        returning id
                        """,
                Long.class);
        Long privateArticleId = jdbcTemplate.queryForObject("""
                        insert into articles (title, slug, summary, content_markdown, status, privacy_type, view_count, like_count, comment_count, publish_time)
                        values ('P1 dashboard private article', 'p1-dashboard-private-article', 'should not enter hot list', 'body', 'PUBLISHED', 'SELF', 9999999, 9999999, 9999999, now())
                        returning id
                        """,
                Long.class);
        Long hotProjectId = jdbcTemplate.queryForObject("""
                        insert into portfolio_projects (title, slug, description, project_type, tech_stack, status, is_recommend)
                        values ('P1 dashboard hot project', 'p1-dashboard-hot-project', 'real statistics project', 'WEB_APP', '[]'::jsonb, 'VISIBLE', true)
                        returning id
                        """,
                Long.class);
        Long viewOnlyProjectId = jdbcTemplate.queryForObject("""
                        insert into portfolio_projects (title, slug, description, project_type, tech_stack, status, is_recommend)
                        values ('P1 dashboard view only project', 'p1-dashboard-view-only-project', 'high views low engagement', 'WEB_APP', '[]'::jsonb, 'VISIBLE', false)
                        returning id
                        """,
                Long.class);
        Long hiddenProjectId = jdbcTemplate.queryForObject("""
                        insert into portfolio_projects (title, slug, description, project_type, tech_stack, status, is_recommend)
                        values ('P1 dashboard hidden project', 'p1-dashboard-hidden-project', 'should not enter hot list', 'WEB_APP', '[]'::jsonb, 'HIDDEN', true)
                        returning id
                        """,
                Long.class);

        jdbcTemplate.update("""
                        insert into content_statistics (target_type, target_id, view_count, like_count, favorite_count, comment_count, last_viewed_at)
                        values ('ARTICLE', ?, 500000, 300000, 0, 300000, now())
                        """,
                hotArticleId);
        jdbcTemplate.update("""
                        insert into content_statistics (target_type, target_id, view_count, like_count, favorite_count, comment_count, last_viewed_at)
                        values ('ARTICLE', ?, 1800000, 0, 0, 0, now())
                        """,
                viewOnlyArticleId);
        jdbcTemplate.update("""
                        insert into content_statistics (target_type, target_id, view_count, like_count, favorite_count, comment_count, last_viewed_at)
                        values ('ARTICLE', ?, 9999999, 9999999, 0, 9999999, now())
                        """,
                privateArticleId);
        jdbcTemplate.update("""
                        insert into content_statistics (target_type, target_id, view_count, like_count, favorite_count, comment_count, last_viewed_at)
                        values ('PROJECT', ?, 500000, 250000, 300000, 0, now())
                        """,
                hotProjectId);
        jdbcTemplate.update("""
                        insert into content_statistics (target_type, target_id, view_count, like_count, favorite_count, comment_count, last_viewed_at)
                        values ('PROJECT', ?, 1800000, 0, 0, 0, now())
                        """,
                viewOnlyProjectId);
        jdbcTemplate.update("""
                        insert into content_statistics (target_type, target_id, view_count, like_count, favorite_count, comment_count, last_viewed_at)
                        values ('PROJECT', ?, 9999999, 9999999, 9999999, 0, now())
                        """,
                hiddenProjectId);

        jdbcTemplate.update("""
                        insert into search_logs (keyword, result_count, created_at)
                        select 'p1-dashboard-keyword', 3, now()
                        from generate_series(1, 12)
                        """);
        jdbcTemplate.update("insert into search_logs (keyword, result_count, created_at) values ('p1-dashboard-other', 1, now())");
        jdbcTemplate.update("""
                        insert into visit_logs (path, target_type, target_id, created_at)
                        values ('/p1-dashboard/article', 'ARTICLE', ?, now()),
                               ('/p1-dashboard/project', 'PROJECT', ?, now())
                        """,
                hotArticleId, hotProjectId);
        jdbcTemplate.update("""
                        insert into operation_logs (operation, module, target_type, target_id, request_method, request_path, created_at)
                        values ('P1_DASHBOARD_SENTINEL', 'DASHBOARD_TEST', 'ARTICLE', ?, 'POST', '/api/admin/dashboard/overview-test', now())
                        """,
                hotArticleId);
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

    private void registerUser(String username, String password) throws Exception {
        String email = username.replaceAll("[^a-zA-Z0-9]", "") + "@qq.com";
        redisTemplate.opsForValue().set("email:code:value:REGISTER:" + email, "123456");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", username,
                                "password", password,
                                "email", email,
                                "verificationCode", "123456"
                        ))))
                .andExpect(status().isOk());
    }

    private String loginAsUser(String username, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", username,
                                "password", password
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
