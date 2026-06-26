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

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class SearchSecurityIntegrationTests extends PostgresIntegrationTestSupport {

    private static final String SECRET_KEYWORD = "p0-search-secret-sentinel";
    private static final String PUBLIC_KEYWORD = "p0-search-public-sentinel";

    @Container
    private static final PostgreSQLContainer POSTGRES = createPostgres("creatorspace_search_security_test");

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
    void seedSearchSecuritySentinels() {
        redisTemplate.delete(List.of(
                SiteCacheService.SITE_CONFIG_KEY,
                SiteCacheService.CURRENT_THEME_KEY,
                SiteCacheService.THEME_LIST_KEY
        ));
        jdbcTemplate.update("delete from search_logs where keyword in (?, ?)", SECRET_KEYWORD, PUBLIC_KEYWORD);
        jdbcTemplate.update("delete from articles where slug like 'p0-search-%'");
        jdbcTemplate.update("delete from portfolio_projects where slug like 'p0-search-%'");
        jdbcTemplate.update("delete from inspiration_cards where title like 'P0 search %'");

        insertArticle("P0 search public article " + PUBLIC_KEYWORD, "p0-search-public-article", "PUBLISHED", "PUBLIC");
        insertArticle("P0 search draft article " + SECRET_KEYWORD, "p0-search-draft-article", "DRAFT", "PUBLIC");
        insertArticle("P0 search pending article " + SECRET_KEYWORD, "p0-search-pending-article", "PENDING_REVIEW", "PUBLIC");
        insertArticle("P0 search private article " + SECRET_KEYWORD, "p0-search-private-article", "PUBLISHED", "SELF");
        insertArticle("P0 search rejected article " + SECRET_KEYWORD, "p0-search-rejected-article", "REJECTED", "PUBLIC");

        insertProject("P0 search public project " + PUBLIC_KEYWORD, "p0-search-public-project", "VISIBLE");
        insertProject("P0 search draft project " + SECRET_KEYWORD, "p0-search-draft-project", "DRAFT");
        insertProject("P0 search pending project " + SECRET_KEYWORD, "p0-search-pending-project", "PENDING_REVIEW");
        insertProject("P0 search hidden project " + SECRET_KEYWORD, "p0-search-hidden-project", "HIDDEN");
        insertProject("P0 search rejected project " + SECRET_KEYWORD, "p0-search-rejected-project", "REJECTED");

        insertInspiration("P0 search public inspiration " + PUBLIC_KEYWORD, true);
        insertInspiration("P0 search private inspiration " + SECRET_KEYWORD, false);
    }

    @Test
    void publicSearchReturnsOnlyPublicVisibleContent() throws Exception {
        mockMvc.perform(get("/api/search").param("keyword", PUBLIC_KEYWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records", hasSize(3)))
                .andExpect(jsonPath("$.data.total", is(3)));
    }

    @Test
    void publicSearchDoesNotExposeDraftPrivatePendingRejectedOrHiddenContent() throws Exception {
        mockMvc.perform(get("/api/search").param("keyword", SECRET_KEYWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records", empty()))
                .andExpect(jsonPath("$.data.total", is(0)));
    }

    private void insertArticle(String title, String slug, String status, String privacyType) {
        jdbcTemplate.update("""
                        insert into articles (title, slug, summary, content_markdown, status, privacy_type, publish_time)
                        values (?, ?, ?, ?, ?, ?, now())
                        """,
                title,
                slug,
                "Search boundary summary " + title,
                "Search boundary body " + title,
                status,
                privacyType);
    }

    private void insertProject(String title, String slug, String status) {
        jdbcTemplate.update("""
                        insert into portfolio_projects (title, slug, description, project_type, status)
                        values (?, ?, ?, ?, ?)
                        """,
                title,
                slug,
                "Search boundary project " + title,
                "WEB_APP",
                status);
    }

    private void insertInspiration(String title, boolean isPublic) {
        jdbcTemplate.update("""
                        insert into inspiration_cards (title, content, card_type, is_public)
                        values (?, ?, ?, ?)
                        """,
                title,
                "Search boundary inspiration " + title,
                "TEXT",
                isPublic);
    }
}
