package com.creatorspace.module.content;

import com.creatorspace.testsupport.PostgresIntegrationTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ContentApiIntegrationTests extends PostgresIntegrationTestSupport {

    @Container
    private static final PostgreSQLContainer POSTGRES = createPostgres("creatorspace_content_test");

    // 注册内容接口集成测试所需的临时 PostgreSQL 配置。
    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, POSTGRES);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 验证注册和文章发布的完整闭环。
    @Test
    void visitorCanRegisterAndAdminCanPublishArticleWithCategoryAndTags() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", "reader",
                                "password", "reader-secret"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.username", is("reader")))
                .andExpect(jsonPath("$.data.roles[0]", is("USER")));

        String token = loginAsAdmin();
        long categoryId = createCategory(token, "ARTICLE", "技术札记", "tech-notes");
        long tagId = createTag(token, "Spring Boot", "spring-boot");

        String createArticleResponse = mockMvc.perform(post("/api/admin/articles")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "title", "第一篇公开文章",
                                "slug", "first-public-article",
                                "summary", "第一阶段公开文章摘要",
                                "contentMarkdown", "## 正文\nCreatorSpace 第一阶段内容闭环。",
                                "categoryId", categoryId,
                                "tagIds", new long[]{tagId},
                                "privacyType", "PUBLIC"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("DRAFT")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long articleId = dataId(createArticleResponse);

        mockMvc.perform(put("/api/admin/articles/{id}/publish", articleId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("PUBLISHED")));

        mockMvc.perform(get("/api/articles")
                        .param("keyword", "第一篇公开文章"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records", hasSize(1)))
                .andExpect(jsonPath("$.data.records[0].title", is("第一篇公开文章")))
                .andExpect(jsonPath("$.data.records[0].category.name", is("技术札记")))
                .andExpect(jsonPath("$.data.records[0].tags[0].name", is("Spring Boot")));

        mockMvc.perform(get("/api/articles/slug/{slug}", "first-public-article"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contentMarkdown", is("## 正文\nCreatorSpace 第一阶段内容闭环。")));

        mockMvc.perform(get("/api/search")
                        .param("keyword", "tech-notes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].type", is("ARTICLE")))
                .andExpect(jsonPath("$.data.records[0].title", is("第一篇公开文章")));
    }

    // 验证公开文章列表不会泄露草稿或私密文章。
    @Test
    void publicArticleListDoesNotExposeDraftOrPrivateArticles() throws Exception {
        String token = loginAsAdmin();

        mockMvc.perform(post("/api/admin/articles")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "title", "不应公开检索哨兵草稿",
                                "slug", "draft-article",
                                "summary", "不应出现在前台",
                                "contentMarkdown", "草稿",
                                "privacyType", "PUBLIC"
                        ))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/admin/articles")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "title", "不应公开检索哨兵私密文章",
                                "slug", "private-article",
                                "summary", "不应出现在前台",
                                "contentMarkdown", "私密",
                                "privacyType", "SELF"
                        ))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/articles")
                        .param("keyword", "不应公开检索哨兵"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records", empty()));
    }

    // 验证管理员创建作品后游客可读取公开作品列表。
    @Test
    void adminCanCreateProjectAndVisitorCanReadVisibleProjectList() throws Exception {
        String token = loginAsAdmin();
        long tagId = createTag(token, "唯一展厅标签", "unique-gallery-tag");

        mockMvc.perform(post("/api/admin/projects")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "title", "CreatorSpace CMS",
                                "slug", "creatorspace-cms",
                                "description", "个人内容系统后台",
                                "projectType", "WEB_APP",
                                "techStack", new String[]{"Vue 3", "Spring Boot"},
                                "contentMarkdown", "项目说明",
                                "tagIds", new long[]{tagId},
                                "recommended", true
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("VISIBLE")));

        mockMvc.perform(get("/api/projects")
                        .param("keyword", "CreatorSpace CMS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records", hasSize(1)))
                .andExpect(jsonPath("$.data.records[0].title", is("CreatorSpace CMS")))
                .andExpect(jsonPath("$.data.records[0].tags[0].name", is("唯一展厅标签")))
                .andExpect(jsonPath("$.data.records[0].techStack[0]", is("Vue 3")));

        mockMvc.perform(get("/api/projects/slug/{slug}", "creatorspace-cms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title", is("CreatorSpace CMS")))
                .andExpect(jsonPath("$.data.contentMarkdown", is("项目说明")));

        mockMvc.perform(get("/api/search")
                        .param("keyword", "unique-gallery-tag"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].type", is("PROJECT")))
                .andExpect(jsonPath("$.data.records[0].title", is("CreatorSpace CMS")));
    }

    // 验证后台创建作品时拒绝不安全外链。
    @Test
    void adminProjectApiRejectsUnsafeExternalUrls() throws Exception {
        String token = loginAsAdmin();

        mockMvc.perform(post("/api/admin/projects")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "title", "不安全外链作品",
                                "slug", "unsafe-link-project",
                                "description", "应拒绝 javascript scheme",
                                "projectType", "WEB_APP",
                                "techStack", new String[]{"Vue 3"},
                                "githubUrl", "javascript:alert(1)"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("GitHub 链接只允许 http 或 https 地址")));
    }

    // 验证公开作品详情不会泄露隐藏作品。
    @Test
    void publicProjectDetailDoesNotExposeHiddenProject() throws Exception {
        jdbcTemplate.update("""
                insert into portfolio_projects (
                    title,
                    slug,
                    description,
                    project_type,
                    tech_stack,
                    content_markdown,
                    status
                )
                values (?, ?, ?, ?, cast(? as jsonb), ?, ?)
                """,
                "隐藏作品",
                "hidden-project",
                "不应公开",
                "WEB_APP",
                "[]",
                "隐藏内容",
                "HIDDEN");

        mockMvc.perform(get("/api/projects/slug/{slug}", "hidden-project"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)));
    }

    // 验证匿名用户不能访问后台内容接口。
    @Test
    void anonymousUserCannotUseAdminContentApis() throws Exception {
        mockMvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "module", "ARTICLE",
                                "name", "未授权",
                                "slug", "anonymous"
                        ))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/categories")
                        .param("module", "ARTICLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    // 验证公开列表拒绝非法分页参数。
    @Test
    void publicListsRejectInvalidPagination() throws Exception {
        mockMvc.perform(get("/api/articles")
                        .param("page", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));

        mockMvc.perform(get("/api/projects")
                        .param("pageSize", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    // 验证内容创建会提前拒绝无效关联资源。
    @Test
    void adminContentApisRejectInvalidRelations() throws Exception {
        String token = loginAsAdmin();
        long projectCategoryId = createCategory(token, "PROJECT", "项目分类", "project-category");

        mockMvc.perform(post("/api/admin/articles")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "title", "错误分类文章",
                                "slug", "invalid-category-article",
                                "summary", "分类模块错误",
                                "contentMarkdown", "正文",
                                "categoryId", projectCategoryId,
                                "privacyType", "PUBLIC"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("文章分类不属于文章模块")));

        mockMvc.perform(post("/api/admin/articles")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "title", "错误标签文章",
                                "slug", "invalid-tag-article",
                                "summary", "标签不存在",
                                "contentMarkdown", "正文",
                                "tagIds", new long[]{999_999L},
                                "privacyType", "PUBLIC"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("标签不存在")));

        mockMvc.perform(post("/api/admin/projects")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "title", "错误标签作品",
                                "slug", "invalid-tag-project",
                                "description", "标签不存在",
                                "projectType", "WEB_APP",
                                "techStack", new String[]{"Spring Boot"},
                                "tagIds", new long[]{999_999L}
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("标签不存在")));
    }

    // 验证禁用管理员不会获得后台访问令牌。
    @Test
    void disabledAdminCannotLogin() throws Exception {
        jdbcTemplate.update("update users set status = 'DISABLED' where username = ?", ADMIN_USERNAME);
        try {
            mockMvc.perform(post("/api/admin/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of(
                                    "username", ADMIN_USERNAME,
                                    "password", ADMIN_PASSWORD
                            ))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success", is(false)));
        } finally {
            jdbcTemplate.update("update users set status = 'ACTIVE' where username = ?", ADMIN_USERNAME);
        }
    }

    // 登录管理员并返回访问令牌。
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
                .andExpect(jsonPath("$.data.user.roles[0]", is("ADMIN")))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).path("data").path("accessToken").asText();
    }

    // 创建测试分类并返回主键。
    private long createCategory(String token, String module, String name, String slug) throws Exception {
        String response = mockMvc.perform(post("/api/admin/categories")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "module", module,
                                "name", name,
                                "slug", slug
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return dataId(response);
    }

    // 创建测试标签并返回主键。
    private long createTag(String token, String name, String slug) throws Exception {
        String response = mockMvc.perform(post("/api/admin/tags")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "name", name,
                                "slug", slug,
                                "color", "#6750a4"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return dataId(response);
    }

    // 从接口响应中读取 data.id。
    private long dataId(String response) throws Exception {
        JsonNode data = objectMapper.readTree(response).path("data");
        assertThat(data.path("id").isNumber()).isTrue();
        return data.path("id").asLong();
    }

    // 将测试对象序列化为 JSON。
    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    // 拼接 Bearer 认证头。
    private String bearer(String token) {
        return "Bearer " + token;
    }
}
