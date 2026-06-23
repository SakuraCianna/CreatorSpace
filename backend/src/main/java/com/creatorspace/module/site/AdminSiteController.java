package com.creatorspace.module.site;

import com.creatorspace.common.exception.BusinessException;
import com.creatorspace.common.result.ApiResponse;
import com.creatorspace.security.LoginUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * 后台主题和站点设置接口，让 CMS 可以维护公开站点的主题、身份、导航和页面配置。
 */
@RestController
public class AdminSiteController {

    private static final String KEY_PATTERN = "^[a-zA-Z0-9._-]{2,120}$";
    private static final String SLUG_PATTERN = "^[a-z0-9]+(?:-[a-z0-9]+)*$";
    private static final String THEME_NAME_PATTERN = "^[a-z0-9]+(?:[-_][a-z0-9]+)*$";

    private final JdbcTemplate jdbcTemplate;
    private final SiteCacheService siteCacheService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AdminSiteController(JdbcTemplate jdbcTemplate, SiteCacheService siteCacheService) {
        this.jdbcTemplate = jdbcTemplate;
        this.siteCacheService = siteCacheService;
    }

    // 查询全部主题，后台用于编辑和切换当前主题。
    @GetMapping("/api/admin/themes")
    public ApiResponse<List<AdminThemeVO>> listThemes() {
        return ApiResponse.ok(themes());
    }

    // 更新主题基础信息和扩展 JSON。
    @Transactional
    @PutMapping("/api/admin/themes/{id}")
    public ApiResponse<AdminThemeVO> updateTheme(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable Long id,
            @Valid @RequestBody ThemeUpdateRequest request
    ) {
        assertThemeExists(id);
        ensureThemeNameAvailable(id, request.themeName());
        validateAssetUrl(request.backgroundImage(), "主题背景地址只允许 http、https 或 /uploads/ 路径");

        jdbcTemplate.update("""
                        update theme_configs
                        set theme_name = ?,
                            display_name = ?,
                            primary_color = ?,
                            background_type = ?,
                            background_image = ?,
                            font_family = ?,
                            card_style = ?,
                            layout_type = ?,
                            config_json = cast(? as jsonb),
                            updated_at = now()
                        where id = ?
                        """,
                cleanRequired(request.themeName()),
                cleanRequired(request.displayName()),
                cleanRequired(request.primaryColor()),
                cleanRequired(request.backgroundType()),
                clean(request.backgroundImage()),
                clean(request.fontFamily()),
                cleanRequired(request.cardStyle()),
                cleanRequired(request.layoutType()),
                json(request.config()),
                id
        );
        logOperation(loginUser.userId(), "更新主题", "THEME", "THEME", id, Map.of("themeName", request.themeName()));
        evictAfterCommit(siteCacheService::evictThemes);
        return ApiResponse.ok(themeById(id));
    }

    // 切换当前启用主题，事务内先清空再启用，保证唯一激活约束不冲突。
    @Transactional
    @PutMapping("/api/admin/themes/{id}/switch")
    public ApiResponse<AdminThemeVO> switchTheme(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable Long id
    ) {
        assertThemeExists(id);
        jdbcTemplate.update("update theme_configs set is_active = false, updated_at = now() where is_active = true");
        jdbcTemplate.update("update theme_configs set is_active = true, updated_at = now() where id = ?", id);
        logOperation(loginUser.userId(), "切换主题", "THEME", "THEME", id, Map.of("themeId", id));
        evictAfterCommit(siteCacheService::evictThemes);
        return ApiResponse.ok(themeById(id));
    }

    // 查询后台站点设置工作台所需的配置集合。
    @GetMapping("/api/admin/site/settings")
    public ApiResponse<SiteSettingsVO> settings() {
        return ApiResponse.ok(siteSettings());
    }

    // 更新站点身份、导航、社交链接、页面 SEO 和站点 JSON 配置。
    @Transactional
    @PutMapping("/api/admin/site/settings")
    public ApiResponse<SiteSettingsVO> updateSettings(
            @AuthenticationPrincipal LoginUser loginUser,
            @Valid @RequestBody SiteSettingsRequest request
    ) {
        if (request.profile() != null) {
            upsertProfile(request.profile());
        }
        if (request.configs() != null) {
            request.configs().forEach(this::upsertSiteConfig);
        }
        if (request.navigationItems() != null) {
            request.navigationItems().forEach(this::upsertNavigationItem);
        }
        if (request.socialLinks() != null) {
            request.socialLinks().forEach(this::upsertSocialLink);
        }
        if (request.pages() != null) {
            request.pages().forEach(this::upsertPageConfig);
        }
        logOperation(loginUser.userId(), "更新站点设置", "SITE", "SITE", null, Map.of("sections", request.changedSections()));
        evictAfterCommit(siteCacheService::evictSiteConfig);
        return ApiResponse.ok(siteSettings());
    }

    private void evictAfterCommit(Runnable eviction) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            eviction.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                eviction.run();
            }
        });
    }

    private List<AdminThemeVO> themes() {
        return jdbcTemplate.query("""
                        select id,
                               theme_name,
                               display_name,
                               primary_color,
                               background_type,
                               background_image,
                               font_family,
                               card_style,
                               layout_type,
                               is_active,
                               config_json::text,
                               created_at::text as created_at,
                               updated_at::text as updated_at
                        from theme_configs
                        order by is_active desc, id
                        """,
                (rs, rowNum) -> new AdminThemeVO(
                        rs.getLong("id"),
                        rs.getString("theme_name"),
                        rs.getString("display_name"),
                        rs.getString("primary_color"),
                        rs.getString("background_type"),
                        rs.getString("background_image"),
                        rs.getString("font_family"),
                        rs.getString("card_style"),
                        rs.getString("layout_type"),
                        rs.getBoolean("is_active"),
                        readJson(rs.getString("config_json")),
                        rs.getString("created_at"),
                        rs.getString("updated_at")
                ));
    }

    private AdminThemeVO themeById(Long id) {
        return themes()
                .stream()
                .filter(theme -> theme.id().equals(id))
                .findFirst()
                .orElseThrow(() -> BusinessException.notFound("主题不存在"));
    }

    private SiteSettingsVO siteSettings() {
        return new SiteSettingsVO(
                activeProfile(),
                navigationItems(),
                socialLinks(),
                pageConfigs(),
                siteConfigEntries()
        );
    }

    private SiteProfileVO activeProfile() {
        return jdbcTemplate.query("""
                        select id,
                               profile_key,
                               display_name,
                               headline,
                               avatar_url,
                               bio,
                               contact_email,
                               location,
                               profile_json::text,
                               is_active,
                               updated_at::text as updated_at
                        from site_profiles
                        order by is_active desc, updated_at desc, id
                        limit 1
                        """,
                (rs, rowNum) -> new SiteProfileVO(
                        rs.getLong("id"),
                        rs.getString("profile_key"),
                        rs.getString("display_name"),
                        rs.getString("headline"),
                        rs.getString("avatar_url"),
                        rs.getString("bio"),
                        rs.getString("contact_email"),
                        rs.getString("location"),
                        readJson(rs.getString("profile_json")),
                        rs.getBoolean("is_active"),
                        rs.getString("updated_at")
                )).stream().findFirst().orElse(null);
    }

    private List<NavigationItemVO> navigationItems() {
        return jdbcTemplate.query("""
                        select id, label, path, icon, group_name, sort_order, visible, extra_json::text
                        from navigation_items
                        order by sort_order, id
                        """,
                (rs, rowNum) -> new NavigationItemVO(
                        rs.getLong("id"),
                        rs.getString("label"),
                        rs.getString("path"),
                        rs.getString("icon"),
                        rs.getString("group_name"),
                        rs.getInt("sort_order"),
                        rs.getBoolean("visible"),
                        readJson(rs.getString("extra_json"))
                ));
    }

    private List<SocialLinkVO> socialLinks() {
        return jdbcTemplate.query("""
                        select id, platform, label, url, icon, sort_order, visible
                        from social_links
                        order by sort_order, id
                        """,
                (rs, rowNum) -> new SocialLinkVO(
                        rs.getLong("id"),
                        rs.getString("platform"),
                        rs.getString("label"),
                        rs.getString("url"),
                        rs.getString("icon"),
                        rs.getInt("sort_order"),
                        rs.getBoolean("visible")
                ));
    }

    private List<PageConfigVO> pageConfigs() {
        return jdbcTemplate.query("""
                        select id,
                               page_key,
                               title,
                               slug,
                               seo_title,
                               seo_description,
                               content_json::text,
                               layout_json::text,
                               status
                        from page_configs
                        order by page_key
                        """,
                (rs, rowNum) -> new PageConfigVO(
                        rs.getLong("id"),
                        rs.getString("page_key"),
                        rs.getString("title"),
                        rs.getString("slug"),
                        rs.getString("seo_title"),
                        rs.getString("seo_description"),
                        readJson(rs.getString("content_json")),
                        readJson(rs.getString("layout_json")),
                        rs.getString("status")
                ));
    }

    private List<SiteConfigEntryVO> siteConfigEntries() {
        return jdbcTemplate.query("""
                        select id, config_key, config_value::text, description
                        from site_configs
                        order by config_key
                        """,
                (rs, rowNum) -> new SiteConfigEntryVO(
                        rs.getLong("id"),
                        rs.getString("config_key"),
                        readJson(rs.getString("config_value")),
                        rs.getString("description")
                ));
    }

    private void upsertProfile(SiteProfileRequest profile) {
        validateAssetUrl(profile.avatarUrl(), "头像地址只允许 http、https 或 /uploads/ 路径");
        jdbcTemplate.update("update site_profiles set is_active = false, updated_at = now() where is_active = true");
        int updated = jdbcTemplate.update("""
                        update site_profiles
                        set display_name = ?,
                            headline = ?,
                            avatar_url = ?,
                            bio = ?,
                            contact_email = ?,
                            location = ?,
                            profile_json = cast(? as jsonb),
                            is_active = true,
                            updated_at = now()
                        where profile_key = ?
                        """,
                cleanRequired(profile.displayName()),
                clean(profile.headline()),
                clean(profile.avatarUrl()),
                clean(profile.bio()),
                clean(profile.contactEmail()),
                clean(profile.location()),
                    json(profile.profileJson()),
                cleanRequired(profile.profileKey())
        );
        if (updated == 0) {
            jdbcTemplate.update("""
                            insert into site_profiles (
                                profile_key, display_name, headline, avatar_url, bio, contact_email,
                                location, profile_json, is_active
                            )
                            values (?, ?, ?, ?, ?, ?, ?, cast(? as jsonb), true)
                            """,
                    cleanRequired(profile.profileKey()),
                    cleanRequired(profile.displayName()),
                    clean(profile.headline()),
                    clean(profile.avatarUrl()),
                    clean(profile.bio()),
                    clean(profile.contactEmail()),
                    clean(profile.location()),
                    json(profile.profileJson())
            );
        }
    }

    private void upsertSiteConfig(SiteConfigEntryRequest config) {
        int updated = jdbcTemplate.update("""
                        update site_configs
                        set config_value = cast(? as jsonb),
                            description = ?,
                            updated_at = now()
                        where config_key = ?
                        """,
                json(config.configValue()),
                clean(config.description()),
                cleanRequired(config.configKey())
        );
        if (updated == 0) {
            jdbcTemplate.update("""
                            insert into site_configs (config_key, config_value, description)
                            values (?, cast(? as jsonb), ?)
                            """,
                    cleanRequired(config.configKey()),
                    json(config.configValue()),
                    clean(config.description())
            );
        }
    }

    private void upsertNavigationItem(NavigationItemRequest item) {
        validatePath(item.path());
        if (item.id() != null && exists("navigation_items", item.id())) {
            jdbcTemplate.update("""
                            update navigation_items
                            set label = ?,
                                path = ?,
                                icon = ?,
                                group_name = ?,
                                sort_order = ?,
                                visible = ?,
                                extra_json = cast(? as jsonb),
                                updated_at = now()
                            where id = ?
                            """,
                    cleanRequired(item.label()),
                    cleanRequired(item.path()),
                    clean(item.icon()),
                    cleanRequired(item.groupName()),
                    item.sortOrder(),
                    item.visible(),
                    json(item.extraJson()),
                    item.id()
            );
            return;
        }
        jdbcTemplate.update("""
                        insert into navigation_items (label, path, icon, group_name, sort_order, visible, extra_json)
                        values (?, ?, ?, ?, ?, ?, cast(? as jsonb))
                        """,
                cleanRequired(item.label()),
                cleanRequired(item.path()),
                clean(item.icon()),
                cleanRequired(item.groupName()),
                item.sortOrder(),
                item.visible(),
                json(item.extraJson())
        );
    }

    private void upsertSocialLink(SocialLinkRequest link) {
        validateSocialUrl(link.url());
        if (link.id() != null && exists("social_links", link.id())) {
            jdbcTemplate.update("""
                            update social_links
                            set platform = ?,
                                label = ?,
                                url = ?,
                                icon = ?,
                                sort_order = ?,
                                visible = ?,
                                updated_at = now()
                            where id = ?
                            """,
                    cleanRequired(link.platform()),
                    cleanRequired(link.label()),
                    cleanRequired(link.url()),
                    clean(link.icon()),
                    link.sortOrder(),
                    link.visible(),
                    link.id()
            );
            return;
        }
        jdbcTemplate.update("""
                        insert into social_links (platform, label, url, icon, sort_order, visible)
                        values (?, ?, ?, ?, ?, ?)
                        """,
                cleanRequired(link.platform()),
                cleanRequired(link.label()),
                cleanRequired(link.url()),
                clean(link.icon()),
                link.sortOrder(),
                link.visible()
        );
    }

    private void upsertPageConfig(PageConfigRequest page) {
        if (page.id() != null && exists("page_configs", page.id())) {
            jdbcTemplate.update("""
                            update page_configs
                            set page_key = ?,
                                title = ?,
                                slug = ?,
                                seo_title = ?,
                                seo_description = ?,
                                content_json = cast(? as jsonb),
                                layout_json = cast(? as jsonb),
                                status = ?,
                                updated_at = now()
                            where id = ?
                            """,
                    cleanRequired(page.pageKey()),
                    cleanRequired(page.title()),
                    cleanRequired(page.slug()),
                    clean(page.seoTitle()),
                    clean(page.seoDescription()),
                    json(page.contentJson()),
                    json(page.layoutJson()),
                    cleanRequired(page.status()),
                    page.id()
            );
            return;
        }
        jdbcTemplate.update("""
                        insert into page_configs (
                            page_key, title, slug, seo_title, seo_description, content_json, layout_json, status
                        )
                        values (?, ?, ?, ?, ?, cast(? as jsonb), cast(? as jsonb), ?)
                        on conflict (page_key) do update
                        set title = excluded.title,
                            slug = excluded.slug,
                            seo_title = excluded.seo_title,
                            seo_description = excluded.seo_description,
                            content_json = excluded.content_json,
                            layout_json = excluded.layout_json,
                            status = excluded.status,
                            updated_at = now()
                        """,
                cleanRequired(page.pageKey()),
                cleanRequired(page.title()),
                cleanRequired(page.slug()),
                clean(page.seoTitle()),
                clean(page.seoDescription()),
                json(page.contentJson()),
                json(page.layoutJson()),
                cleanRequired(page.status())
        );
    }

    private void assertThemeExists(Long id) {
        if (!exists("theme_configs", id)) {
            throw BusinessException.notFound("主题不存在");
        }
    }

    private void ensureThemeNameAvailable(Long id, String themeName) {
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from theme_configs where theme_name = ? and id <> ?",
                Long.class,
                cleanRequired(themeName),
                id
        );
        if (count != null && count > 0) {
            throw BusinessException.conflict("主题标识已存在");
        }
    }

    private boolean exists(String tableName, Long id) {
        Long count = jdbcTemplate.queryForObject("select count(*) from " + tableName + " where id = ?", Long.class, id);
        return count != null && count > 0;
    }

    private void validateAssetUrl(String value, String message) {
        String cleaned = clean(value);
        if (cleaned == null || cleaned.startsWith("/uploads/")) {
            return;
        }
        validateExternalUrl(cleaned, message);
    }

    private void validateExternalUrl(String value, String message) {
        String cleaned = clean(value);
        if (cleaned == null) {
            return;
        }
        try {
            String scheme = URI.create(cleaned).getScheme();
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                throw BusinessException.badRequest(message);
            }
        } catch (IllegalArgumentException exception) {
            throw BusinessException.badRequest(message);
        }
    }

    private void validateSocialUrl(String value) {
        String cleaned = cleanRequired(value);
        try {
            URI uri = URI.create(cleaned);
            String scheme = uri.getScheme();
            String schemeSpecificPart = uri.getSchemeSpecificPart();
            if ("mailto".equalsIgnoreCase(scheme)
                    && schemeSpecificPart != null
                    && !schemeSpecificPart.isBlank()) {
                return;
            }
            if (("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    && uri.getHost() != null
                    && !uri.getHost().isBlank()) {
                return;
            }
        } catch (IllegalArgumentException exception) {
            throw BusinessException.badRequest("社交链接只允许 http、https 或 mailto 地址");
        }
        throw BusinessException.badRequest("社交链接只允许 http、https 或 mailto 地址");
    }

    private void validatePath(String value) {
        String cleaned = cleanRequired(value);
        if (cleaned.startsWith("//")) {
            throw BusinessException.badRequest("导航地址只允许站内路径或 http/https 地址");
        }
        if (cleaned.startsWith("/")) {
            return;
        }
        validateExternalUrl(cleaned, "导航地址只允许站内路径或 http/https 地址");
    }

    private Object readJson(String value) {
        try {
            return objectMapper.readValue(value == null ? "{}" : value, Object.class);
        } catch (Exception exception) {
            return Map.of();
        }
    }

    private String json(Object value) {
        try {
            if (value == null) {
                return "{}";
            }
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw BusinessException.badRequest("JSON 配置不合法");
        }
    }

    private String clean(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String cleanRequired(String value) {
        String cleaned = clean(value);
        if (cleaned == null) {
            throw BusinessException.badRequest("必填字段不能为空");
        }
        return cleaned;
    }

    private void logOperation(Long operatorId, String operation, String module, String targetType, Long targetId, Map<String, ?> detail) {
        jdbcTemplate.update("""
                        insert into operation_logs (operator_id, operation, module, target_type, target_id, detail_json)
                        values (?, ?, ?, ?, ?, cast(? as jsonb))
                        """,
                operatorId,
                operation,
                module,
                targetType,
                targetId,
                json(detail == null ? Map.of() : detail)
        );
    }

    public record AdminThemeVO(
            Long id,
            String themeName,
            String displayName,
            String primaryColor,
            String backgroundType,
            String backgroundImage,
            String fontFamily,
            String cardStyle,
            String layoutType,
            boolean active,
            Object config,
            String createdAt,
            String updatedAt
    ) {
    }

    public record ThemeUpdateRequest(
            @NotBlank
            @Size(max = 80)
            @Pattern(regexp = THEME_NAME_PATTERN)
            String themeName,

            @NotBlank
            @Size(max = 120)
            String displayName,

            @NotBlank
            @Size(max = 32)
            @Pattern(regexp = "^#[0-9a-fA-F]{6}([0-9a-fA-F]{2})?$")
            String primaryColor,

            @NotBlank
            @Size(max = 40)
            String backgroundType,

            @Size(max = 500)
            String backgroundImage,

            @Size(max = 120)
            String fontFamily,

            @NotBlank
            @Size(max = 40)
            String cardStyle,

            @NotBlank
            @Size(max = 40)
            String layoutType,

            Map<String, Object> config
    ) {
    }

    public record SiteSettingsVO(
            SiteProfileVO profile,
            List<NavigationItemVO> navigationItems,
            List<SocialLinkVO> socialLinks,
            List<PageConfigVO> pages,
            List<SiteConfigEntryVO> configs
    ) {
    }

    public record SiteProfileVO(
            Long id,
            String profileKey,
            String displayName,
            String headline,
            String avatarUrl,
            String bio,
            String contactEmail,
            String location,
            Object profileJson,
            boolean active,
            String updatedAt
    ) {
    }

    public record NavigationItemVO(
            Long id,
            String label,
            String path,
            String icon,
            String groupName,
            Integer sortOrder,
            boolean visible,
            Object extraJson
    ) {
    }

    public record SocialLinkVO(
            Long id,
            String platform,
            String label,
            String url,
            String icon,
            Integer sortOrder,
            boolean visible
    ) {
    }

    public record PageConfigVO(
            Long id,
            String pageKey,
            String title,
            String slug,
            String seoTitle,
            String seoDescription,
            Object contentJson,
            Object layoutJson,
            String status
    ) {
    }

    public record SiteConfigEntryVO(
            Long id,
            String configKey,
            Object configValue,
            String description
    ) {
    }

    public record SiteSettingsRequest(
            @Valid SiteProfileRequest profile,
            List<@Valid NavigationItemRequest> navigationItems,
            List<@Valid SocialLinkRequest> socialLinks,
            List<@Valid PageConfigRequest> pages,
            List<@Valid SiteConfigEntryRequest> configs
    ) {
        public List<String> changedSections() {
            List<String> sections = new java.util.ArrayList<>();
            if (profile != null) {
                sections.add("profile");
            }
            if (navigationItems != null) {
                sections.add("navigation");
            }
            if (socialLinks != null) {
                sections.add("social");
            }
            if (pages != null) {
                sections.add("pages");
            }
            if (configs != null) {
                sections.add("configs");
            }
            return sections;
        }
    }

    public record SiteProfileRequest(
            @NotBlank
            @Size(max = 80)
            @Pattern(regexp = KEY_PATTERN)
            String profileKey,

            @NotBlank
            @Size(max = 120)
            String displayName,

            @Size(max = 180)
            String headline,

            @Size(max = 500)
            String avatarUrl,

            @Size(max = 5000)
            String bio,

            @Size(max = 180)
            String contactEmail,

            @Size(max = 120)
            String location,

            Map<String, Object> profileJson
    ) {
    }

    public record SiteConfigEntryRequest(
            Long id,

            @NotBlank
            @Size(max = 120)
            @Pattern(regexp = KEY_PATTERN)
            String configKey,

            Map<String, Object> configValue,

            @Size(max = 500)
            String description
    ) {
    }

    public record NavigationItemRequest(
            Long id,

            @NotBlank
            @Size(max = 80)
            String label,

            @NotBlank
            @Size(max = 180)
            String path,

            @Size(max = 80)
            String icon,

            @NotBlank
            @Size(max = 80)
            String groupName,

            Integer sortOrder,
            boolean visible,
            Map<String, Object> extraJson
    ) {
    }

    public record SocialLinkRequest(
            Long id,

            @NotBlank
            @Size(max = 80)
            String platform,

            @NotBlank
            @Size(max = 120)
            String label,

            @NotBlank
            @Size(max = 500)
            String url,

            @Size(max = 80)
            String icon,

            Integer sortOrder,
            boolean visible
    ) {
    }

    public record PageConfigRequest(
            Long id,

            @NotBlank
            @Size(max = 120)
            @Pattern(regexp = KEY_PATTERN)
            String pageKey,

            @NotBlank
            @Size(max = 180)
            String title,

            @NotBlank
            @Size(max = 180)
            @Pattern(regexp = SLUG_PATTERN)
            String slug,

            @Size(max = 180)
            String seoTitle,

            @Size(max = 1000)
            String seoDescription,

            Map<String, Object> contentJson,
            Map<String, Object> layoutJson,

            @NotBlank
            @Pattern(regexp = "^(DRAFT|PUBLISHED|ARCHIVED)$")
            String status
    ) {
    }
}
