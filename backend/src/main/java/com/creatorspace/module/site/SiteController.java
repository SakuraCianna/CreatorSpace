package com.creatorspace.module.site;

import com.creatorspace.common.result.ApiResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 公开站点配置接口，让前台页面优先从后台配置读取个人空间信息。
 */
@RestController
public class SiteController {

    private static final Set<String> PUBLIC_THEME_CONFIG_KEYS = Set.of(
            "accentColor",
            "accentSoftColor",
            "backgroundColor",
            "density",
            "inkColor",
            "mood",
            "motion",
            "mutedColor",
            "surfaceColor",
            "tagline"
    );
    private static final Set<String> PUBLIC_PROFILE_JSON_KEYS = Set.of(
            "bio",
            "career",
            "education",
            "experience",
            "experienceSummary",
            "experiences",
            "focus",
            "heroTitleSuffix",
            "resumeLabel",
            "resumeUrl",
            "signature"
    );

    private final JdbcTemplate jdbcTemplate;
    private final SiteCacheService siteCacheService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 通过 JdbcTemplate 读取高弹性的 JSONB 配置，避免为早期配置项过早建复杂服务层。
    public SiteController(JdbcTemplate jdbcTemplate, SiteCacheService siteCacheService) {
        this.jdbcTemplate = jdbcTemplate;
        this.siteCacheService = siteCacheService;
    }

    // 返回站点身份、导航、首页推荐和关于页内容。
    @GetMapping("/api/site/config")
    public ApiResponse<Map<String, Object>> config() {
        Map<String, Object> cached = siteCacheService
                .read(SiteCacheService.SITE_CONFIG_KEY, new TypeReference<Map<String, Object>>() {
                })
                .orElse(null);
        if (cached != null) {
            return ApiResponse.ok(cached);
        }
        Map<String, Object> configs = loadConfig();
        siteCacheService.write(SiteCacheService.SITE_CONFIG_KEY, configs);
        return ApiResponse.ok(configs);
    }

    // 返回首页可以公开展示的访问统计摘要，不暴露后台操作日志和管理指标。
    @GetMapping("/api/site/statistics/summary")
    public ApiResponse<SiteStatisticsSummaryVO> statisticsSummary() {
        return ApiResponse.ok(new SiteStatisticsSummaryVO(
                singleLong("select count(*) from visit_logs"),
                singleLong("select count(distinct ip_address) from visit_logs"),
                singleLong("select count(*) from visit_logs where created_at::date = current_date"),
                singleLong("select count(distinct ip_address) from visit_logs where created_at::date = current_date"),
                singleLong("select coalesce(sum(view_count), 0) from content_statistics")
        ));
    }

    // 返回当前启用主题，供前台和后台预览保持同一套主题来源。
    @GetMapping("/api/theme/current")
    public ApiResponse<ThemeConfigVO> currentTheme() {
        ThemeConfigVO cached = siteCacheService
                .read(SiteCacheService.CURRENT_THEME_KEY, ThemeConfigVO.class)
                .orElse(null);
        if (cached != null) {
            return ApiResponse.ok(cached);
        }
        ThemeConfigVO theme = loadCurrentTheme();
        siteCacheService.write(SiteCacheService.CURRENT_THEME_KEY, theme);
        return ApiResponse.ok(theme);
    }

    // 返回公开主题列表，首页主题展示区只读使用，不暴露后台写操作。
    @GetMapping("/api/themes")
    public ApiResponse<List<PublicThemeVO>> themes() {
        List<PublicThemeVO> cached = siteCacheService
                .read(SiteCacheService.THEME_LIST_KEY, new TypeReference<List<PublicThemeVO>>() {
                })
                .orElse(null);
        if (cached != null) {
            return ApiResponse.ok(cached);
        }
        List<PublicThemeVO> themes = loadThemes();
        siteCacheService.write(SiteCacheService.THEME_LIST_KEY, themes);
        return ApiResponse.ok(themes);
    }

    private Map<String, Object> loadConfig() {
        Map<String, Object> configs = new LinkedHashMap<>();
        jdbcTemplate.query("""
                        select config_key, config_value::text
                        from site_configs
                        where config_key in ('site.identity')
                        order by config_key
                        """,
                rs -> {
                    configs.put(rs.getString("config_key"), readJson(rs.getString("config_value")));
                });
        configs.put("site.profile.active", activeProfile());
        configs.put("site.navigationItems", navigationItems());
        configs.put("site.socialLinks", socialLinks());
        configs.put("page.home", pageConfig("home"));
        configs.put("page.about", pageConfig("about"));
        configs.put("home.contentBlocks", contentBlocks("HOME"));
        return configs;
    }

    private ThemeConfigVO loadCurrentTheme() {
        return jdbcTemplate.query("""
                        select theme_name,
                               display_name,
                               primary_color,
                               background_type,
                               background_image,
                               font_family,
                               card_style,
                               layout_type,
                               config_json::text
                        from theme_configs
                        where is_active = true
                        order by id
                        limit 1
                        """,
                (rs, rowNum) -> new ThemeConfigVO(
                        rs.getString("theme_name"),
                        rs.getString("display_name"),
                        rs.getString("primary_color"),
                        rs.getString("background_type"),
                        rs.getString("background_image"),
                        rs.getString("font_family"),
                        rs.getString("card_style"),
                        rs.getString("layout_type"),
                        publicThemeConfig(readJson(rs.getString("config_json")))
                )).stream().findFirst().orElse(null);
    }

    private List<PublicThemeVO> loadThemes() {
        return jdbcTemplate.query("""
                        select theme_name,
                               display_name,
                               primary_color,
                               background_type,
                               background_image,
                               font_family,
                               card_style,
                               layout_type,
                               is_active,
                               config_json::text
                        from theme_configs
                        order by is_active desc, id
                        """,
                (rs, rowNum) -> new PublicThemeVO(
                        rs.getString("theme_name"),
                        rs.getString("display_name"),
                        rs.getString("primary_color"),
                        rs.getString("background_type"),
                        rs.getString("background_image"),
                        rs.getString("font_family"),
                        rs.getString("card_style"),
                        rs.getString("layout_type"),
                        rs.getBoolean("is_active"),
                        publicThemeConfig(readJson(rs.getString("config_json")))
                ));
    }

    // 安全解析 JSONB 文本。
    private Object readJson(String value) {
        try {
            return objectMapper.readValue(value == null ? "{}" : value, Object.class);
        } catch (Exception exception) {
            return Map.of();
        }
    }

    private long singleLong(String sql) {
        Long total = jdbcTemplate.queryForObject(sql, Long.class);
        return total == null ? 0 : total;
    }

    private Map<String, Object> publicThemeConfig(Object value) {
        if (!(value instanceof Map<?, ?> source)) {
            return Map.of();
        }
        Map<String, Object> config = new LinkedHashMap<>();
        PUBLIC_THEME_CONFIG_KEYS.forEach(key -> {
            if (source.containsKey(key)) {
                config.put(key, source.get(key));
            }
        });
        return config;
    }

    private Map<String, Object> publicProfileJson(Object value) {
        if (!(value instanceof Map<?, ?> source)) {
            return Map.of();
        }
        Map<String, Object> config = new LinkedHashMap<>();
        PUBLIC_PROFILE_JSON_KEYS.forEach(key -> {
            if (source.containsKey(key)) {
                config.put(key, source.get(key));
            }
        });
        Map<String, Object> resume = publicResumeConfig(source.get("resume"));
        if (!resume.isEmpty()) {
            config.put("resume", resume);
        }
        return config;
    }

    private Map<String, Object> publicResumeConfig(Object value) {
        if (!(value instanceof Map<?, ?> source)) {
            return Map.of();
        }
        Map<String, Object> config = new LinkedHashMap<>();
        for (String key : List.of("url", "href", "label")) {
            if (source.containsKey(key)) {
                config.put(key, source.get(key));
            }
        }
        return config;
    }

    private Object activeProfile() {
        return jdbcTemplate.query("""
                        select display_name,
                               headline,
                               avatar_url,
                               bio,
                               contact_email,
                               location,
                               profile_json::text
                        from site_profiles
                        where is_active = true
                        order by updated_at desc
                        limit 1
                        """,
                (rs, rowNum) -> {
                    Map<String, Object> profile = new LinkedHashMap<>();
                    profile.put("displayName", rs.getString("display_name"));
                    profile.put("headline", rs.getString("headline"));
                    profile.put("avatarUrl", rs.getString("avatar_url"));
                    profile.put("bio", rs.getString("bio"));
                    profile.put("location", rs.getString("location"));
                    profile.put("profileJson", publicProfileJson(readJson(rs.getString("profile_json"))));
                    return profile;
                }).stream().findFirst().orElse(Map.of());
    }

    private Object socialLinks() {
        List<Map<String, Object>> links = jdbcTemplate.query("""
                        select platform, label, url, icon, sort_order
                        from social_links
                        where visible = true
                        order by sort_order, id
                        """,
                (rs, rowNum) -> {
                    Map<String, Object> link = new LinkedHashMap<>();
                    link.put("platform", rs.getString("platform"));
                    link.put("label", rs.getString("label"));
                    link.put("url", rs.getString("url"));
                    link.put("icon", rs.getString("icon"));
                    link.put("sortOrder", rs.getInt("sort_order"));
                    return link;
                });
        return links;
    }

    private Object navigationItems() {
        List<Map<String, Object>> items = jdbcTemplate.query("""
                        select label, path, icon, group_name, sort_order
                        from navigation_items
                        where visible = true
                        order by sort_order, id
                        """,
                (rs, rowNum) -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("label", rs.getString("label"));
                    item.put("path", rs.getString("path"));
                    item.put("icon", rs.getString("icon"));
                    item.put("groupName", rs.getString("group_name"));
                    item.put("sortOrder", rs.getInt("sort_order"));
                    return item;
                });
        return items;
    }

    private Object pageConfig(String pageKey) {
        return jdbcTemplate.query("""
                        select title,
                               slug,
                               seo_title,
                               seo_description,
                               content_json::text,
                               layout_json::text,
                               status
                        from page_configs
                        where page_key = ?
                          and status = 'PUBLISHED'
                        limit 1
                        """,
                (rs, rowNum) -> {
                    Map<String, Object> page = new LinkedHashMap<>();
                    page.put("title", rs.getString("title"));
                    page.put("slug", rs.getString("slug"));
                    page.put("seoTitle", rs.getString("seo_title"));
                    page.put("seoDescription", rs.getString("seo_description"));
                    page.put("contentJson", readJson(rs.getString("content_json")));
                    page.put("layoutJson", readJson(rs.getString("layout_json")));
                    page.put("status", rs.getString("status"));
                    return page;
                },
                pageKey).stream().findFirst().orElse(Map.of());
    }

    private Object contentBlocks(String ownerType) {
        return jdbcTemplate.query("""
                        select block_key,
                               block_type,
                               title,
                               body,
                               config_json::text,
                               sort_order
                        from content_blocks
                        where owner_type = ?
                          and visible = true
                        order by sort_order, id
                        """,
                (rs, rowNum) -> {
                    Map<String, Object> block = new LinkedHashMap<>();
                    block.put("blockKey", rs.getString("block_key"));
                    block.put("blockType", rs.getString("block_type"));
                    block.put("title", rs.getString("title"));
                    block.put("body", rs.getString("body"));
                    block.put("config", readJson(rs.getString("config_json")));
                    block.put("sortOrder", rs.getInt("sort_order"));
                    return block;
                },
                ownerType);
    }

    public record ThemeConfigVO(
            String themeName,
            String displayName,
            String primaryColor,
            String backgroundType,
            String backgroundImage,
            String fontFamily,
            String cardStyle,
            String layoutType,
            Object config
    ) {
    }

    public record PublicThemeVO(
            String themeName,
            String displayName,
            String primaryColor,
            String backgroundType,
            String backgroundImage,
            String fontFamily,
            String cardStyle,
            String layoutType,
            boolean active,
            Object config
    ) {
    }

    public record SiteStatisticsSummaryVO(
            Long totalPv,
            Long totalUv,
            Long todayPv,
            Long todayUv,
            Long contentViews
    ) {
    }
}
