package com.creatorspace.module.content;

import com.creatorspace.module.site.SiteCacheService;
import com.creatorspace.testsupport.PostgresIntegrationTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ContentApiIntegrationTests extends PostgresIntegrationTestSupport {

    private static final byte[] PNG_BYTES = new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x00
    };

    @Container
    private static final PostgreSQLContainer POSTGRES = createPostgres("creatorspace_content_test");

    @Container
    private static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    // 注册内容接口集成测试所需的临时 PostgreSQL 配置。
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

    @BeforeEach
    void clearSiteCache() {
        redisTemplate.delete(List.of(
                SiteCacheService.SITE_CONFIG_KEY,
                SiteCacheService.CURRENT_THEME_KEY,
                SiteCacheService.THEME_LIST_KEY
        ));
    }

    // 验证普通用户注册、创作文章、提交审核和管理员通过后的公开闭环。
    @Test
    void registeredUserCanCreateArticleAndAdminCanApproveIt() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", "creator-reader",
                                "password", "reader-secret"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.username", is("creator-reader")))
                .andExpect(jsonPath("$.data.roles[0]", is("USER")));

        String userToken = loginAsUser("creator-reader", "reader-secret");
        String adminToken = loginAsAdmin();
        long categoryId = createCategory(adminToken, "ARTICLE", "技术札记", "tech-notes");
        long tagId = createTag(adminToken, "Spring Boot 测试标签", "spring-boot-test-tag");

        String createArticleResponse = mockMvc.perform(post("/api/creator/articles")
                        .header("Authorization", bearer(userToken))
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
                .andExpect(jsonPath("$.data.recommended", is(false)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long articleId = dataId(createArticleResponse);

        mockMvc.perform(get("/api/articles")
                        .param("keyword", "第一篇公开文章"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records", empty()));

        mockMvc.perform(put("/api/creator/articles/{id}/submit", articleId)
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("PENDING_REVIEW")));

        mockMvc.perform(put("/api/admin/articles/{id}/approve", articleId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("PUBLISHED")));

        mockMvc.perform(get("/api/articles")
                        .param("keyword", "第一篇公开文章"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records", hasSize(1)))
                .andExpect(jsonPath("$.data.records[0].title", is("第一篇公开文章")))
                .andExpect(jsonPath("$.data.records[0].category.name", is("技术札记")))
                .andExpect(jsonPath("$.data.records[0].tags[0].name", is("Spring Boot 测试标签")));

        mockMvc.perform(get("/api/articles/slug/{slug}", "first-public-article"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contentMarkdown", is("## 正文\nCreatorSpace 第一阶段内容闭环。")));

        mockMvc.perform(get("/api/search")
                        .param("keyword", "tech-notes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[?(@.type == 'ARTICLE' && @.title == '第一篇公开文章')]").exists());
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

    // 验证公开文章详情页上一篇/下一篇由后端按公开列表排序计算, 不依赖前端分页窗口。
    @Test
    void publicArticleNeighborsFollowPublicListOrder() throws Exception {
        mockMvc.perform(get("/api/articles/slug/{slug}/neighbors", "creator-theme-blog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.previousArticle.slug", is("redis-site-config-cache")))
                .andExpect(jsonPath("$.data.nextArticle.slug", is("home-content-room")))
                .andExpect(jsonPath("$.data.previousArticle.contentMarkdown").doesNotExist())
                .andExpect(jsonPath("$.data.nextArticle.contentMarkdown").doesNotExist());
    }

    // 验证后台文章列表、更新、发布、撤回、推荐置顶和删除闭环。
    @Test
    void adminCanManageArticleLifecycle() throws Exception {
        String token = loginAsAdmin();
        long categoryId = createCategory(token, "ARTICLE", "后台文章分类", "admin-article-category");
        long tagId = createTag(token, "后台文章标签", "admin-article-tag");

        String createResponse = mockMvc.perform(post("/api/admin/articles")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "title", "后台文章生命周期",
                                "slug", "admin-article-lifecycle",
                                "summary", "创建后会被更新",
                                "contentMarkdown", "草稿正文",
                                "categoryId", categoryId,
                                "tagIds", new long[]{tagId},
                                "privacyType", "PUBLIC"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("DRAFT")))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long articleId = dataId(createResponse);

        mockMvc.perform(put("/api/admin/articles/{id}", articleId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "title", "后台文章生命周期已更新",
                                "slug", "admin-article-lifecycle-updated",
                                "summary", "更新后的摘要",
                                "contentMarkdown", "更新后的 Markdown",
                                "categoryId", categoryId,
                                "tagIds", new long[]{tagId},
                                "privacyType", "PUBLIC"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slug", is("admin-article-lifecycle-updated")))
                .andExpect(jsonPath("$.data.tags[0].name", is("后台文章标签")));

        mockMvc.perform(get("/api/admin/articles/{id}", articleId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contentMarkdown", is("更新后的 Markdown")));

        mockMvc.perform(get("/api/admin/articles")
                        .header("Authorization", bearer(token))
                        .param("keyword", "生命周期已更新")
                        .param("status", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records", hasSize(1)))
                .andExpect(jsonPath("$.data.records[0].title", is("后台文章生命周期已更新")));

        mockMvc.perform(put("/api/admin/articles/{id}/top", articleId)
                        .header("Authorization", bearer(token))
                        .param("enabled", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.top", is(true)));

        mockMvc.perform(put("/api/admin/articles/{id}/recommend", articleId)
                        .header("Authorization", bearer(token))
                        .param("enabled", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recommended", is(true)));

        mockMvc.perform(put("/api/admin/articles/{id}/publish", articleId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("PUBLISHED")));

        mockMvc.perform(get("/api/articles/slug/{slug}", "admin-article-lifecycle-updated"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title", is("后台文章生命周期已更新")));

        mockMvc.perform(put("/api/admin/articles/{id}/unpublish", articleId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("DRAFT")));

        mockMvc.perform(get("/api/articles/slug/{slug}", "admin-article-lifecycle-updated"))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/admin/articles/{id}", articleId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/articles/{id}", articleId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
    }

    // 验证普通用户创建作品、提交审核和管理员通过后游客可读取。
    @Test
    void registeredUserCanCreateProjectAndAdminCanApproveIt() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", "project-creator",
                                "password", "project-secret"
                        ))))
                .andExpect(status().isOk());
        String userToken = loginAsUser("project-creator", "project-secret");
        String adminToken = loginAsAdmin();
        long tagId = createTag(adminToken, "唯一展厅标签", "unique-gallery-tag");

        String createResponse = mockMvc.perform(post("/api/creator/projects")
                        .header("Authorization", bearer(userToken))
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
                .andExpect(jsonPath("$.data.status", is("DRAFT")))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long projectId = dataId(createResponse);

        mockMvc.perform(get("/api/projects")
                        .param("keyword", "CreatorSpace CMS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records", empty()));

        mockMvc.perform(put("/api/creator/projects/{id}/submit", projectId)
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("PENDING_REVIEW")));

        mockMvc.perform(put("/api/admin/projects/{id}/approve", projectId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("VISIBLE")));

        mockMvc.perform(get("/api/projects")
                        .param("keyword", "CreatorSpace CMS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records", hasSize(1)))
                .andExpect(jsonPath("$.data.records[0].title", is("CreatorSpace CMS")))
                .andExpect(jsonPath("$.data.records[0].recommended", is(false)))
                .andExpect(jsonPath("$.data.records[0].tags[0].name", is("唯一展厅标签")))
                .andExpect(jsonPath("$.data.records[0].techStack[0]", is("Vue 3")));

        mockMvc.perform(get("/api/projects/slug/{slug}", "creatorspace-cms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title", is("CreatorSpace CMS")))
                .andExpect(jsonPath("$.data.contentMarkdown", is("项目说明")));

        mockMvc.perform(get("/api/search")
                        .param("keyword", "unique-gallery-tag"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[?(@.type == 'PROJECT' && @.title == 'CreatorSpace CMS')]").exists());
    }

    // 验证公开作品详情返回截图、里程碑、资源链接和过程记录。
    @Test
    void publicProjectDetailIncludesExtendedShowcaseData() throws Exception {
        mockMvc.perform(get("/api/projects/slug/{slug}", "creator-center-workbench"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.screenshots", not(empty())))
                .andExpect(jsonPath("$.data.screenshots[0].imageUrl", not("")))
                .andExpect(jsonPath("$.data.screenshots[0].caption", not("")))
                .andExpect(jsonPath("$.data.milestones", not(empty())))
                .andExpect(jsonPath("$.data.milestones[0].title", not("")))
                .andExpect(jsonPath("$.data.milestones[0].description", not("")))
                .andExpect(jsonPath("$.data.resources", not(empty())))
                .andExpect(jsonPath("$.data.resources[0].label", not("")))
                .andExpect(jsonPath("$.data.resources[0].url", not("")))
                .andExpect(jsonPath("$.data.processNotes", not(empty())))
                .andExpect(jsonPath("$.data.processNotes[0].phase", not("")))
                .andExpect(jsonPath("$.data.processNotes[0].body", not("")));
    }

    // 验证普通用户可以上传创作资源，并对公开内容点赞和收藏。
    @Test
    void registeredUserCanUploadCreatorFilesAndInteractWithPublicContent() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", "asset-creator",
                                "password", "asset-secret"
                        ))))
                .andExpect(status().isOk());
        String userToken = loginAsUser("asset-creator", "asset-secret");
        String adminToken = loginAsAdmin();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "creator-cover.png",
                "image/png",
                PNG_BYTES
        );
        mockMvc.perform(multipart("/api/creator/files/upload")
                        .file(file)
                        .param("module", "PROJECT")
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.module", is("PROJECT")))
                .andExpect(jsonPath("$.data.publicUrl", not("")));

        mockMvc.perform(get("/api/creator/files")
                        .header("Authorization", bearer(userToken))
                        .param("module", "PROJECT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].originalName", is("creator-cover.png")));

        MockMultipartFile pdf = new MockMultipartFile(
                "file",
                "creator-note.pdf",
                "application/pdf",
                "%PDF-1.4\n".getBytes(StandardCharsets.UTF_8)
        );
        mockMvc.perform(multipart("/api/creator/files/upload")
                        .file(pdf)
                        .param("module", "OTHER")
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("普通用户只能上传图片资源")));

        String createArticleResponse = mockMvc.perform(post("/api/admin/articles")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "title", "互动目标文章",
                                "slug", "interaction-target-article",
                                "summary", "用于验证点赞和收藏",
                                "contentMarkdown", "## 互动\n这是一篇公开文章。",
                                "privacyType", "PUBLIC"
                        ))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long articleId = dataId(createArticleResponse);
        mockMvc.perform(put("/api/admin/articles/{id}/approve", articleId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/me/likes")
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "targetType", "ARTICLE",
                                "targetId", articleId
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.targetType", is("ARTICLE")))
                .andExpect(jsonPath("$.data.targetId", is((int) articleId)));

        mockMvc.perform(post("/api/me/favorites")
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "targetType", "ARTICLE",
                                "targetId", articleId
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.targetType", is("ARTICLE")));

        mockMvc.perform(get("/api/me/favorites")
                        .header("Authorization", bearer(userToken))
                        .param("targetType", "ARTICLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records", hasSize(1)));

        mockMvc.perform(delete("/api/me/likes")
                        .header("Authorization", bearer(userToken))
                        .param("targetType", "ARTICLE")
                        .param("targetId", String.valueOf(articleId)))
                .andExpect(status().isOk());
    }

    // 验证后台作品列表、更新、展示状态、推荐和删除闭环。
    @Test
    void adminCanManageProjectLifecycle() throws Exception {
        String token = loginAsAdmin();
        long tagId = createTag(token, "后台作品标签", "admin-project-tag");

        String createResponse = mockMvc.perform(post("/api/admin/projects")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "title", "后台作品生命周期",
                                "slug", "admin-project-lifecycle",
                                "description", "创建后会被更新",
                                "projectType", "WEB_APP",
                                "techStack", new String[]{"Vue 3"},
                                "contentMarkdown", "作品草稿",
                                "tagIds", new long[]{tagId},
                                "recommended", true
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("DRAFT")))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long projectId = dataId(createResponse);

        mockMvc.perform(put("/api/admin/projects/{id}", projectId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "title", "后台作品生命周期已更新",
                                "slug", "admin-project-lifecycle-updated",
                                "description", "更新后的作品描述",
                                "projectType", "WEB_APP",
                                "techStack", new String[]{"Vue 3", "Spring Boot"},
                                "contentMarkdown", "更新后的作品详情",
                                "tagIds", new long[]{tagId},
                                "recommended", false
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slug", is("admin-project-lifecycle-updated")))
                .andExpect(jsonPath("$.data.techStack[1]", is("Spring Boot")))
                .andExpect(jsonPath("$.data.recommended", is(false)));

        mockMvc.perform(get("/api/admin/projects")
                        .header("Authorization", bearer(token))
                        .param("keyword", "生命周期已更新")
                        .param("status", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records", hasSize(1)))
                .andExpect(jsonPath("$.data.records[0].title", is("后台作品生命周期已更新")));

        mockMvc.perform(put("/api/admin/projects/{id}/status", projectId)
                        .header("Authorization", bearer(token))
                        .param("status", "HIDDEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("HIDDEN")));

        mockMvc.perform(get("/api/projects/slug/{slug}", "admin-project-lifecycle-updated"))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/admin/projects/{id}/status", projectId)
                        .header("Authorization", bearer(token))
                        .param("status", "VISIBLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("VISIBLE")));

        mockMvc.perform(put("/api/admin/projects/{id}/recommend", projectId)
                        .header("Authorization", bearer(token))
                        .param("enabled", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recommended", is(true)));

        mockMvc.perform(delete("/api/admin/projects/{id}", projectId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/projects/{id}", projectId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
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
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("WWW-Authenticate"))
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("请先登录后再访问")));

        mockMvc.perform(get("/api/categories")
                        .param("module", "ARTICLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    // 验证普通用户访问后台接口时返回明确 JSON 权限错误。
    @Test
    void regularUserGetsJsonForbiddenWhenUsingAdminApis() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", "regular-admin-denied",
                                "password", "regular-secret"
                        ))))
                .andExpect(status().isOk());
        String userToken = loginAsUser("regular-admin-denied", "regular-secret");

        mockMvc.perform(get("/api/admin/dashboard/overview")
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("当前账号没有后台权限或登录状态不匹配")));
    }

    // 验证本地开发 CORS 使用来源模式全开放，并覆盖整个后端路径。
    @Test
    void localCorsAllowsAnyOriginPatternForBackendPaths() throws Exception {
        mockMvc.perform(options("/api/admin/auth/login")
                        .header("Origin", "http://random.local.test:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://random.local.test:5173"));

        mockMvc.perform(options("/uploads/example.png")
                        .header("Origin", "http://assets.local.test:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://assets.local.test:5173"));
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

    // 验证文章封面和内容 URL 标识会提前拒绝不安全输入。
    @Test
    void adminArticleApiRejectsUnsafeCoverUrlAndInvalidSlug() throws Exception {
        String token = loginAsAdmin();

        mockMvc.perform(post("/api/admin/articles")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "title", "不安全封面文章",
                                "slug", "unsafe-cover-article",
                                "summary", "应拒绝 javascript scheme",
                                "contentMarkdown", "正文",
                                "coverUrl", "javascript:alert(1)",
                                "privacyType", "PUBLIC"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("文章封面只允许 http、https 或站内上传地址")));

        mockMvc.perform(post("/api/admin/articles")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "title", "错误标识文章",
                                "slug", "bad/slug",
                                "summary", "应拒绝斜杠",
                                "contentMarkdown", "正文",
                                "privacyType", "PUBLIC"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("文章标识只允许小写字母、数字和短横线")));
    }

    // 验证作品 URL 标识拒绝会破坏路由的特殊字符。
    @Test
    void adminProjectApiRejectsInvalidSlug() throws Exception {
        String token = loginAsAdmin();

        mockMvc.perform(post("/api/admin/projects")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "title", "错误标识作品",
                                "slug", "bad project",
                                "description", "应拒绝空格",
                                "projectType", "WEB_APP",
                                "techStack", new String[]{"Vue 3"}
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("作品标识只允许小写字母、数字和短横线")));
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

    // 验证普通用户登录、评论提交和管理员审核后公开展示的完整链路。
    @Test
    void registeredUserCanLoginAndCommentAfterAdminApproval() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", "commenter",
                                "password", "commenter-secret"
                        ))))
                .andExpect(status().isOk());
        String userToken = loginAsUser("commenter", "commenter-secret");
        String adminToken = loginAsAdmin();

        String createArticleResponse = mockMvc.perform(post("/api/admin/articles")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "title", "评论闭环文章",
                                "slug", "comment-flow-article",
                                "summary", "用于验证评论审核",
                                "contentMarkdown", "## 评论\n等待读者反馈。",
                                "privacyType", "PUBLIC"
                        ))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long articleId = dataId(createArticleResponse);
        mockMvc.perform(put("/api/admin/articles/{id}/publish", articleId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());

        String commentResponse = mockMvc.perform(post("/api/comments")
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "targetType", "ARTICLE",
                                "targetId", articleId,
                                "content", "这篇文章的结构很清楚。"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("PENDING")))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long commentId = dataId(commentResponse);

        mockMvc.perform(get("/api/comments")
                        .param("targetType", "ARTICLE")
                        .param("targetId", String.valueOf(articleId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records", empty()));

        mockMvc.perform(put("/api/admin/comments/{id}/approve", commentId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("APPROVED")));

        mockMvc.perform(get("/api/comments")
                        .param("targetType", "ARTICLE")
                        .param("targetId", String.valueOf(articleId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records", hasSize(1)))
                .andExpect(jsonPath("$.data.records[0].content", is("这篇文章的结构很清楚。")));
    }

    // 验证回复只有审核通过后才进入公开回复数，驳回已通过回复会回退计数。
    @Test
    void replyCountOnlyIncludesApprovedReplies() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", "reply-reader",
                                "password", "reply-reader-secret"
                        ))))
                .andExpect(status().isOk());
        String userToken = loginAsUser("reply-reader", "reply-reader-secret");
        String adminToken = loginAsAdmin();

        String createArticleResponse = mockMvc.perform(post("/api/admin/articles")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "title", "回复计数文章",
                                "slug", "reply-count-article",
                                "summary", "用于验证评论回复计数",
                                "contentMarkdown", "## 回复计数\n只统计通过审核的回复。",
                                "privacyType", "PUBLIC"
                        ))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long articleId = dataId(createArticleResponse);
        mockMvc.perform(put("/api/admin/articles/{id}/publish", articleId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());

        String parentResponse = mockMvc.perform(post("/api/comments")
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "targetType", "ARTICLE",
                                "targetId", articleId,
                                "content", "这是一条主评论。"
                        ))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long parentId = dataId(parentResponse);
        mockMvc.perform(put("/api/admin/comments/{id}/approve", parentId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());

        String replyResponse = mockMvc.perform(post("/api/comments")
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "targetType", "ARTICLE",
                                "targetId", articleId,
                                "parentId", parentId,
                                "content", "这是一条等待审核的回复。"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("PENDING")))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long replyId = dataId(replyResponse);

        mockMvc.perform(get("/api/comments")
                        .param("targetType", "ARTICLE")
                        .param("targetId", String.valueOf(articleId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records", hasSize(1)))
                .andExpect(jsonPath("$.data.records[0].replyCount", is(0)));

        mockMvc.perform(put("/api/admin/comments/{id}/approve", replyId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/comments")
                        .param("targetType", "ARTICLE")
                        .param("targetId", String.valueOf(articleId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records", hasSize(2)))
                .andExpect(jsonPath("$.data.records[0].replyCount", is(1)));

        mockMvc.perform(put("/api/admin/comments/{id}/reject", parentId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/comments")
                        .param("targetType", "ARTICLE")
                        .param("targetId", String.valueOf(articleId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records", empty()));

        mockMvc.perform(put("/api/admin/comments/{id}/approve", parentId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/comments")
                        .param("targetType", "ARTICLE")
                        .param("targetId", String.valueOf(articleId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records", hasSize(2)))
                .andExpect(jsonPath("$.data.records[0].replyCount", is(1)));

        mockMvc.perform(put("/api/admin/comments/{id}/reject", replyId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/comments")
                        .param("targetType", "ARTICLE")
                        .param("targetId", String.valueOf(articleId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records", hasSize(1)))
                .andExpect(jsonPath("$.data.records[0].replyCount", is(0)));
    }

    // 验证后台灵感 CRUD 与本地文件上传资源列表。
    @Test
    void adminCanManageInspirationsAndUploadFiles() throws Exception {
        String token = loginAsAdmin();
        long tagId = createTag(token, "灵感测试标签", "inspiration-test-tag");

        String createResponse = mockMvc.perform(post("/api/admin/inspirations")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "title", "后台灵感卡片",
                                "content", "一条来自后台的灵感。",
                                "cardType", "TEXT",
                                "sourceUrl", "https://example.com/inspiration",
                                "color", "#6ea8ff",
                                "isPublic", false,
                                "sortOrder", 12,
                                "tagIds", new long[]{tagId}
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isPublic", is(false)))
                .andExpect(jsonPath("$.data.tags[0].name", is("灵感测试标签")))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long inspirationId = dataId(createResponse);

        mockMvc.perform(put("/api/admin/inspirations/{id}", inspirationId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "title", "后台灵感卡片",
                                "content", "公开后的灵感。",
                                "cardType", "TEXT",
                                "sourceUrl", "https://example.com/inspiration",
                                "color", "#6ea8ff",
                                "isPublic", true,
                                "sortOrder", 12,
                                "tagIds", new long[]{tagId}
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isPublic", is(true)));

        mockMvc.perform(get("/api/inspirations")
                        .param("keyword", "inspiration-test-tag"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].title", is("后台灵感卡片")));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "cover.png",
                "image/png",
                PNG_BYTES
        );
        mockMvc.perform(multipart("/api/admin/files/upload")
                        .file(file)
                        .param("module", "INSPIRATION")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.module", is("INSPIRATION")))
                .andExpect(jsonPath("$.data.publicUrl", not("")));

        mockMvc.perform(get("/api/admin/files")
                        .header("Authorization", bearer(token))
                        .param("module", "INSPIRATION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].originalName", is("cover.png")));
    }

    // 验证公开灵感墙返回关联内容, 且后台支持草图和参考资料两种独立类型。
    @Test
    void inspirationsExposeRelationsAndSupportSketchAndReferenceTypes() throws Exception {
        String token = loginAsAdmin();
        long tagId = createTag(token, "灵感类型标签", "inspiration-type-tag");

        mockMvc.perform(get("/api/inspirations")
                        .param("keyword", "创作中心信息架构"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].relations", not(empty())))
                .andExpect(jsonPath("$.data.records[0].relations[0].targetType", is("ARTICLE")))
                .andExpect(jsonPath("$.data.records[0].relations[0].targetTitle", not("")))
                .andExpect(jsonPath("$.data.records[0].relations[0].targetSlug", is("creator-theme-blog")));

        mockMvc.perform(post("/api/admin/inspirations")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "title", "草图灵感卡片",
                                "content", "界面草图说明。",
                                "cardType", "SKETCH",
                                "color", "#22c55e",
                                "isPublic", true,
                                "sortOrder", 21,
                                "tagIds", new long[]{tagId}
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cardType", is("SKETCH")));

        mockMvc.perform(post("/api/admin/inspirations")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "title", "参考资料灵感卡片",
                                "content", "公开阅读资料。",
                                "cardType", "REFERENCE",
                                "sourceUrl", "https://example.com/reference",
                                "color", "#0ea5e9",
                                "isPublic", true,
                                "sortOrder", 22,
                                "tagIds", new long[]{tagId}
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cardType", is("REFERENCE")));

        mockMvc.perform(get("/api/inspirations")
                        .param("type", "SKETCH")
                        .param("keyword", "草图灵感卡片"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].cardType", is("SKETCH")));

        mockMvc.perform(get("/api/inspirations")
                        .param("type", "REFERENCE")
                        .param("keyword", "参考资料灵感卡片"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].cardType", is("REFERENCE")));
    }

    // 验证上传接口拒绝伪造 Content-Type 的可执行扩展名。
    @Test
    void adminFileUploadRejectsSpoofedExecutableExtension() throws Exception {
        String token = loginAsAdmin();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "evil.html",
                "image/png",
                "<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/admin/files/upload")
                        .file(file)
                        .param("module", "OTHER")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("文件扩展名与类型不匹配")));
    }

    // 验证上传接口拒绝扩展名和 MIME 合法但文件头不匹配的伪造图片。
    @Test
    void adminFileUploadRejectsMismatchedFileSignature() throws Exception {
        String token = loginAsAdmin();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "fake.png",
                "image/png",
                "not a real png".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/admin/files/upload")
                        .file(file)
                        .param("module", "OTHER")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("文件内容与类型不匹配")));
    }

    // 验证管理员可以更新并切换主题，公开当前主题接口同步返回最新配置。
    @Test
    void adminCanUpdateAndSwitchCurrentTheme() throws Exception {
        String token = loginAsAdmin();
        long themeId = firstThemeId(token);

        mockMvc.perform(get("/api/themes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", not(empty())))
                .andExpect(jsonPath("$.data[0].themeName", not("")))
                .andExpect(jsonPath("$.data[0].id").doesNotExist())
                .andExpect(jsonPath("$.data[0].createdAt").doesNotExist())
                .andExpect(jsonPath("$.data[0].updatedAt").doesNotExist())
                .andExpect(jsonPath("$.data[0].config.creatorMode").doesNotExist());
        assertThat(Boolean.TRUE.equals(redisTemplate.hasKey(SiteCacheService.THEME_LIST_KEY))).isTrue();

        mockMvc.perform(get("/api/theme/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.themeName", not("")));
        assertThat(Boolean.TRUE.equals(redisTemplate.hasKey(SiteCacheService.CURRENT_THEME_KEY))).isTrue();

        mockMvc.perform(put("/api/admin/themes/{id}", themeId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "themeName", "test-material-blue",
                                "displayName", "Test Material Blue",
                                "primaryColor", "#1a73e8",
                                "backgroundType", "color",
                                "backgroundImage", "/uploads/demo/theme-gallery-night.webp",
                                "fontFamily", "Inter, system-ui, sans-serif",
                                "cardStyle", "material",
                                "layoutType", "editorial",
                                "config", Map.of(
                                        "density", "focused",
                                        "motion", "soft"
                                )
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.themeName", is("test-material-blue")))
                .andExpect(jsonPath("$.data.primaryColor", is("#1a73e8")))
                .andExpect(jsonPath("$.data.config.density", is("focused")));
        assertThat(Boolean.TRUE.equals(redisTemplate.hasKey(SiteCacheService.CURRENT_THEME_KEY))).isFalse();
        assertThat(Boolean.TRUE.equals(redisTemplate.hasKey(SiteCacheService.THEME_LIST_KEY))).isFalse();

        mockMvc.perform(put("/api/admin/themes/{id}/switch", themeId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active", is(true)));

        mockMvc.perform(get("/api/theme/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.themeName", is("test-material-blue")))
                .andExpect(jsonPath("$.data.config.motion", is("soft")));
        assertThat(Boolean.TRUE.equals(redisTemplate.hasKey(SiteCacheService.CURRENT_THEME_KEY))).isTrue();
    }

    // 验证主题后台拒绝 javascript 等不安全资源地址。
    @Test
    void adminThemeApiRejectsUnsafeBackgroundUrl() throws Exception {
        String token = loginAsAdmin();
        long themeId = firstThemeId(token);

        mockMvc.perform(put("/api/admin/themes/{id}", themeId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "themeName", "unsafe-theme",
                                "displayName", "Unsafe Theme",
                                "primaryColor", "#1a73e8",
                                "backgroundType", "image",
                                "backgroundImage", "javascript:alert(1)",
                                "fontFamily", "Inter, system-ui, sans-serif",
                                "cardStyle", "material",
                                "layoutType", "editorial",
                                "config", Map.of("density", "focused")
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("主题背景地址只允许 http、https 或 /uploads/ 路径")));
    }

    // 验证管理员保存站点设置后，公开站点配置接口能读取最新身份、导航、页面和 JSON 配置。
    @Test
    void adminCanUpdateSiteSettingsAndPublicConfigReflectsChanges() throws Exception {
        String token = loginAsAdmin();
        mockMvc.perform(get("/api/site/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data['site.identity'].name", not("")))
                .andExpect(jsonPath("$.data['site.creatorPolicy']").doesNotExist())
                .andExpect(jsonPath("$.data['page.home'].title", not("")))
                .andExpect(jsonPath("$.data['home.contentBlocks'][0].blockKey", is("home.creatorHero")))
                .andExpect(jsonPath("$.data['home.contentBlocks'][0].config.cta", is("/creator")));
        assertThat(Boolean.TRUE.equals(redisTemplate.hasKey(SiteCacheService.SITE_CONFIG_KEY))).isTrue();

        String settingsResponse = mockMvc.perform(get("/api/admin/site/settings")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.navigationItems", not(empty())))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode settings = objectMapper.readTree(settingsResponse).path("data");
        JsonNode firstNavigation = settings.path("navigationItems").get(0);

        mockMvc.perform(put("/api/admin/site/settings")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "profile", Map.of(
                                        "profileKey", "test-profile",
                                        "displayName", "CreatorSpace Test Lab",
                                        "headline", "主题博客与创意展厅",
                                        "avatarUrl", "/uploads/demo/avatar.webp",
                                         "bio", "测试公开站点资料会从后台同步。",
                                         "contactEmail", "creator@example.com",
                                         "location", "Shanghai",
                                                "profileJson", Map.of(
                                                        "signature", "material-cms",
                                                        "focus", new String[]{"主题配置", "内容展示"},
                                                        "experienceSummary", "公开经历摘要用于关于页时间线。",
                                                        "experiences", new Object[]{
                                                                Map.of(
                                                                        "period", "2024 - 2025",
                                                                        "title", "内容系统搭建",
                                                                        "description", "把文章、作品和灵感拆成可审核的数据流。"
                                                                )
                                                        },
                                                        "resumeUrl", "https://example.com/resume.pdf",
                                                        "resumeLabel", "查看简历",
                                                        "privateToken", "should-not-be-public"
                                                )
                                 ),
                                "configs", new Object[]{
                                        Map.of(
                                                "configKey", "site.identity",
                                                "configValue", Map.of(
                                                        "name", "CreatorSpace Test Lab",
                                                        "slogan", "后台可配置的个人主题空间"
                                                ),
                                                "description", "站点身份配置"
                                        )
                                },
                                "navigationItems", new Object[]{
                                        Map.of(
                                                "id", firstNavigation.path("id").asLong(),
                                                "label", "实验室",
                                                "path", "/lab",
                                                "icon", "flask",
                                                "groupName", "primary",
                                                "sortOrder", 1,
                                                "visible", true,
                                                "extraJson", Map.of("source", "test")
                                        )
                                },
                                "socialLinks", new Object[]{
                                        Map.of(
                                                "platform", "GitHub",
                                                "label", "Code Lab",
                                                "url", "https://github.com/example/creator-space",
                                                "icon", "github",
                                                "sortOrder", 1,
                                                "visible", true
                                        ),
                                        Map.of(
                                                "platform", "Mail",
                                                "label", "Email Lab",
                                                "url", "mailto:creator@example.com",
                                                "icon", "mail",
                                                "sortOrder", 2,
                                                "visible", true
                                        )
                                },
                                "pages", new Object[]{
                                        Map.of(
                                                "pageKey", "about",
                                                "title", "关于测试创作者",
                                                "slug", "about",
                                                "seoTitle", "关于 CreatorSpace Test Lab",
                                                "seoDescription", "从后台维护关于页 SEO。",
                                                "contentJson", Map.of("sections", new String[]{"bio", "contact"}),
                                                "layoutJson", Map.of("density", "editorial"),
                                                "status", "PUBLISHED"
                                        )
                                }
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profile.displayName", is("CreatorSpace Test Lab")))
                .andExpect(jsonPath("$.data.navigationItems[0].label", is("实验室")))
                .andExpect(jsonPath("$.data.navigationItems[0].extraJson.source", is("test")));
        assertThat(Boolean.TRUE.equals(redisTemplate.hasKey(SiteCacheService.SITE_CONFIG_KEY))).isFalse();

        mockMvc.perform(get("/api/site/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data['site.profile.active'].displayName", is("CreatorSpace Test Lab")))
                .andExpect(jsonPath("$.data['site.profile.active'].contactEmail").doesNotExist())
                .andExpect(jsonPath("$.data['site.profile.active'].profileJson.signature", is("material-cms")))
                .andExpect(jsonPath("$.data['site.profile.active'].profileJson.focus[0]", is("主题配置")))
                .andExpect(jsonPath("$.data['site.profile.active'].profileJson.experienceSummary", is("公开经历摘要用于关于页时间线。")))
                .andExpect(jsonPath("$.data['site.profile.active'].profileJson.experiences[0].title", is("内容系统搭建")))
                .andExpect(jsonPath("$.data['site.profile.active'].profileJson.resumeUrl", is("https://example.com/resume.pdf")))
                .andExpect(jsonPath("$.data['site.profile.active'].profileJson.resumeLabel", is("查看简历")))
                .andExpect(jsonPath("$.data['site.profile.active'].profileJson.privateToken").doesNotExist())
                .andExpect(jsonPath("$.data['site.identity'].name", is("CreatorSpace Test Lab")))
                .andExpect(jsonPath("$.data['site.navigationItems'][0].label", is("实验室")))
                .andExpect(jsonPath("$.data['site.navigationItems'][0].path", is("/lab")))
                .andExpect(jsonPath("$.data['site.socialLinks'][0].label", is("Code Lab")))
                .andExpect(jsonPath("$.data['site.socialLinks'][1].url", is("mailto:creator@example.com")))
                .andExpect(jsonPath("$.data['page.home'].title", not("")))
                .andExpect(jsonPath("$.data['home.contentBlocks'][0].blockKey", is("home.creatorHero")))
                .andExpect(jsonPath("$.data['page.about'].title", is("关于测试创作者")));
        assertThat(Boolean.TRUE.equals(redisTemplate.hasKey(SiteCacheService.SITE_CONFIG_KEY))).isTrue();
    }

    // 验证公开首页可读取访问统计摘要, 但不会暴露后台操作明细。
    @Test
    void publicSiteStatisticsSummaryIsReadableWithoutAdminToken() throws Exception {
        mockMvc.perform(get("/api/site/statistics/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalPv").isNumber())
                .andExpect(jsonPath("$.data.totalUv").isNumber())
                .andExpect(jsonPath("$.data.todayPv").isNumber())
                .andExpect(jsonPath("$.data.todayUv").isNumber())
                .andExpect(jsonPath("$.data.contentViews").isNumber())
                .andExpect(jsonPath("$.data.operationLogs").doesNotExist());
    }

    // 验证后台导航配置拒绝协议相对 URL，避免把 //evil.example 当作站内路径。
    @Test
    void adminSiteSettingsRejectsProtocolRelativeNavigationPath() throws Exception {
        String token = loginAsAdmin();

        mockMvc.perform(put("/api/admin/site/settings")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "navigationItems", new Object[]{
                                        Map.of(
                                                "label", "外部陷阱",
                                                "path", "//evil.example",
                                                "icon", "info",
                                                "groupName", "primary",
                                                "sortOrder", 99,
                                                "visible", true,
                                                "extraJson", Map.of("source", "test")
                                        )
                                }
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("导航地址只允许站内路径或 http/https 地址")));
    }

    // 验证社交链接只接受安全的公开链接或邮箱链接，避免脚本协议进入公开配置。
    @Test
    void adminSiteSettingsRejectsUnsafeSocialLinkProtocol() throws Exception {
        String token = loginAsAdmin();

        mockMvc.perform(put("/api/admin/site/settings")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "socialLinks", new Object[]{
                                        Map.of(
                                                "platform", "unsafe",
                                                "label", "Unsafe",
                                                "url", "javascript:alert(1)",
                                                "icon", "link",
                                                "sortOrder", 99,
                                                "visible", true
                                        )
                                }
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("社交链接只允许 http、https 或 mailto 地址")));

        mockMvc.perform(put("/api/admin/site/settings")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "socialLinks", new Object[]{
                                        Map.of(
                                                "platform", "malformed",
                                                "label", "Malformed",
                                                "url", "https:example.com",
                                                "icon", "link",
                                                "sortOrder", 100,
                                                "visible", true
                                        )
                                }
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("社交链接只允许 http、https 或 mailto 地址")));
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

    // 登录普通用户并返回访问令牌。
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

    // 读取第一条主题 ID。
    private long firstThemeId(String token) throws Exception {
        String response = mockMvc.perform(get("/api/admin/themes")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", not(empty())))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode first = objectMapper.readTree(response).path("data").get(0);
        assertThat(first.path("id").isNumber()).isTrue();
        return first.path("id").asLong();
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
