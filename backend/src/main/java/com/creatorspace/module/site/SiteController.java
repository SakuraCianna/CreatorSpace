package com.creatorspace.module.site;

import com.creatorspace.common.result.ApiResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 公开站点配置接口，让前台页面优先从后台配置读取个人空间信息。
 */
@RestController
public class SiteController {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 通过 JdbcTemplate 读取高弹性的 JSONB 配置，避免为早期配置项过早建复杂服务层。
    public SiteController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // 返回站点身份、导航、首页推荐和关于页内容。
    @GetMapping("/api/site/config")
    public ApiResponse<Map<String, JsonNode>> config() {
        Map<String, JsonNode> configs = new LinkedHashMap<>();
        jdbcTemplate.query("""
                        select config_key, config_value::text
                        from site_configs
                        where config_key like 'site.%'
                           or config_key like 'demo.site.%'
                           or config_key like 'demo.home.%'
                           or config_key like 'page.%'
                        order by config_key
                        """,
                rs -> {
                    configs.put(rs.getString("config_key"), readJson(rs.getString("config_value")));
                });
        configs.put("site.profile.active", activeProfile());
        configs.put("site.socialLinks", socialLinks());
        configs.put("page.about", pageConfig("about"));
        return ApiResponse.ok(configs);
    }

    // 返回当前启用主题，供前台和后台预览保持同一套主题来源。
    @GetMapping("/api/theme/current")
    public ApiResponse<ThemeConfigVO> currentTheme() {
        ThemeConfigVO theme = jdbcTemplate.query("""
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
                        readJson(rs.getString("config_json"))
                )).stream().findFirst().orElse(null);
        return ApiResponse.ok(theme);
    }

    // 安全解析 JSONB 文本。
    private JsonNode readJson(String value) {
        try {
            return objectMapper.readTree(value == null ? "{}" : value);
        } catch (Exception exception) {
            return objectMapper.createObjectNode();
        }
    }

    private JsonNode activeProfile() {
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
                    profile.put("contactEmail", rs.getString("contact_email"));
                    profile.put("location", rs.getString("location"));
                    profile.put("profileJson", readJson(rs.getString("profile_json")));
                    return (JsonNode) objectMapper.valueToTree(profile);
                }).stream().findFirst().orElse(objectMapper.createObjectNode());
    }

    private JsonNode socialLinks() {
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
        return objectMapper.valueToTree(links);
    }

    private JsonNode pageConfig(String pageKey) {
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
                    return (JsonNode) objectMapper.valueToTree(page);
                },
                pageKey).stream().findFirst().orElse(objectMapper.createObjectNode());
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
            JsonNode config
    ) {
    }
}
