package com.creatorspace.module.search;

import com.creatorspace.module.site.SiteCacheService;
import com.creatorspace.testsupport.PostgresIntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class SearchEnhancementIntegrationTests extends PostgresIntegrationTestSupport {

    private static final String KEYWORD = "p1-search-enhanced-sentinel";

    @Container
    private static final PostgreSQLContainer POSTGRES = createPostgres("creatorspace_search_enhancement_test");

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

    @BeforeEach
    void seedEnhancedSearchSentinels() {
        redisTemplate.delete(List.of(
                SiteCacheService.SITE_CONFIG_KEY,
                SiteCacheService.CURRENT_THEME_KEY,
                SiteCacheService.THEME_LIST_KEY
        ));
        jdbcTemplate.update("delete from search_logs where keyword = ?", KEYWORD);
        jdbcTemplate.update("delete from articles where slug like 'p1-search-enhanced-%'");
        jdbcTemplate.update("delete from portfolio_projects where slug like 'p1-search-enhanced-%'");
        jdbcTemplate.update("delete from inspiration_cards where title like 'P1 enhanced search %'");
        jdbcTemplate.update("delete from page_configs where slug like 'p1-search-enhanced-%'");
        jdbcTemplate.update("delete from categories where slug like 'p1-search-enhanced-%'");
        jdbcTemplate.update("delete from tags where slug like 'p1-search-enhanced-%'");

        jdbcTemplate.update("""
                        insert into articles (title, slug, summary, content_markdown, status, privacy_type, publish_time)
                        values (?, ?, ?, ?, 'PUBLISHED', 'PUBLIC', now())
                        """,
                "P1 enhanced search article " + KEYWORD,
                "p1-search-enhanced-article",
                "Article summary " + KEYWORD,
                "Article body " + KEYWORD);
        jdbcTemplate.update("""
                        insert into portfolio_projects (title, slug, description, project_type, tech_stack, status)
                        values (?, ?, ?, 'WEB_APP', ?::jsonb, 'VISIBLE')
                        """,
                "P1 enhanced search project " + KEYWORD,
                "p1-search-enhanced-project",
                "Project description " + KEYWORD,
                "[\"Vue\", \"Spring Boot\"]");
        jdbcTemplate.update("""
                        insert into inspiration_cards (title, content, card_type, is_public)
                        values (?, ?, 'TEXT', true)
                        """,
                "P1 enhanced search inspiration " + KEYWORD,
                "Inspiration content " + KEYWORD);
        jdbcTemplate.update("""
                        insert into tags (name, slug, color, weight)
                        values (?, ?, '#315bff', 9)
                        """,
                "P1 enhanced search tag " + KEYWORD,
                "p1-search-enhanced-tag");
        jdbcTemplate.update("""
                        insert into categories (module, name, slug, description, enabled)
                        values ('ARTICLE', ?, ?, ?, true)
                        """,
                "P1 enhanced search category " + KEYWORD,
                "p1-search-enhanced-category",
                "Category description " + KEYWORD);
        jdbcTemplate.update("""
                        insert into page_configs (page_key, title, slug, seo_title, seo_description, content_json, status)
                        values (?, ?, ?, ?, ?, ?::jsonb, 'PUBLISHED')
                        """,
                "p1-search-enhanced-page",
                "P1 enhanced search page " + KEYWORD,
                "p1-search-enhanced-page",
                "Enhanced search page",
                "Page description " + KEYWORD,
                "{\"body\":\"" + KEYWORD + "\"}");
    }

    @Test
    void publicSearchCoversArticlesProjectsInspirationsTagsCategoriesAndPages() throws Exception {
        mockMvc.perform(get("/api/search").param("keyword", KEYWORD).param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total", is(6)))
                .andExpect(jsonPath("$.data.records[*].type", hasItem("ARTICLE")))
                .andExpect(jsonPath("$.data.records[*].type", hasItem("PROJECT")))
                .andExpect(jsonPath("$.data.records[*].type", hasItem("INSPIRATION")))
                .andExpect(jsonPath("$.data.records[*].type", hasItem("TAG")))
                .andExpect(jsonPath("$.data.records[*].type", hasItem("CATEGORY")))
                .andExpect(jsonPath("$.data.records[*].type", hasItem("PAGE")));
    }

    @Test
    void publicSearchCanFilterByType() throws Exception {
        mockMvc.perform(get("/api/search")
                        .param("keyword", KEYWORD)
                        .param("type", "tag"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total", is(1)))
                .andExpect(jsonPath("$.data.records[0].type", is("TAG")))
                .andExpect(jsonPath("$.data.records[0].slug", is("p1-search-enhanced-tag")));
    }

    @Test
    void publicSearchRejectsUnsupportedTypeAndSort() throws Exception {
        mockMvc.perform(get("/api/search")
                        .param("keyword", KEYWORD)
                        .param("type", "SECRET"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));

        mockMvc.perform(get("/api/search")
                        .param("keyword", KEYWORD)
                        .param("sort", "unsafe"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }
}
