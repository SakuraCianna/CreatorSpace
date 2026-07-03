package com.creatorspace.module.user.controller;

import com.creatorspace.common.exception.BusinessException;
import com.creatorspace.common.result.ApiResponse;
import com.creatorspace.security.LoginUser;
import com.creatorspace.module.user.vo.BlogThemeVO;
import com.creatorspace.module.user.vo.UserPublicVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@RestController
public class MeController {

    private static final Pattern HEX_COLOR = Pattern.compile("^#[0-9a-fA-F]{6}$");
    private static final Set<String> FONT_PRESETS = Set.of("literary-serif", "neo-grotesk", "rounded-sans", "mono-editor");
    private static final Set<String> CANVAS_TYPES = Set.of("soft-paper", "linen", "gradient", "image");
    private static final Set<String> LAYOUT_STYLES = Set.of("editorial", "notebook", "gallery");
    private static final Set<String> BLOCK_STYLES = Set.of("quiet", "ink", "carded");

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MeController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/api/me/profile")
    public ApiResponse<UserPublicVO> getMyProfile(@AuthenticationPrincipal LoginUser loginUser) {
        var profiles = jdbcTemplate.query("""
                        select u.id, u.username, u.nickname, u.avatar_url, u.bio,
                               (select count(*) from articles a where a.created_by = u.id and a.status = 'PUBLISHED' and a.privacy_type = 'PUBLIC') as article_count,
                               (select count(*) from user_follows f where f.followee_id = u.id) as follower_count,
                               (select count(*) from user_follows f where f.follower_id = u.id) as following_count,
                               (select count(*) from (
                                   select f1.follower_id from user_follows f1 where f1.followee_id = u.id
                                   intersect
                                   select f2.followee_id from user_follows f2 where f2.follower_id = u.id
                               ) mutual) as friend_count
                        from users u
                        where u.id = ?
                        """,
                (rs, rowNum) -> {
                    Long id = rs.getObject("id", Long.class);
                    if (id == null) return null;
                    return new UserPublicVO(
                            id,
                            rs.getString("username"),
                            rs.getString("nickname"),
                            rs.getString("avatar_url"),
                            rs.getString("bio"),
                            blogTheme(id),
                            rs.getLong("article_count"),
                            rs.getLong("follower_count"),
                            rs.getLong("following_count"),
                            rs.getLong("friend_count")
                    );
                },
                loginUser.userId());
        if (profiles.isEmpty()) {
            throw BusinessException.notFound("用户不存在");
        }
        return ApiResponse.ok(profiles.getFirst());
    }

    @GetMapping("/api/me/blog-theme")
    public ApiResponse<BlogThemeVO> getMyBlogTheme(@AuthenticationPrincipal LoginUser loginUser) {
        return ApiResponse.ok(blogTheme(loginUser.userId()));
    }

    @Transactional(rollbackFor = Exception.class)
    @PutMapping("/api/me/blog-theme")
    public ApiResponse<BlogThemeVO> updateMyBlogTheme(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestBody BlogThemeRequest request
    ) {
        BlogThemeVO theme = sanitizeTheme(request);
        jdbcTemplate.update("""
                        insert into user_blog_themes (
                            user_id,
                            display_name,
                            font_preset,
                            accent_color,
                            title_color,
                            body_color,
                            canvas_type,
                            canvas_color,
                            canvas_image,
                            paper_color,
                            layout_style,
                            block_style,
                            config_json
                        )
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, cast(? as jsonb))
                        on conflict (user_id) do update set
                            display_name = excluded.display_name,
                            font_preset = excluded.font_preset,
                            accent_color = excluded.accent_color,
                            title_color = excluded.title_color,
                            body_color = excluded.body_color,
                            canvas_type = excluded.canvas_type,
                            canvas_color = excluded.canvas_color,
                            canvas_image = excluded.canvas_image,
                            paper_color = excluded.paper_color,
                            layout_style = excluded.layout_style,
                            block_style = excluded.block_style,
                            config_json = excluded.config_json,
                            updated_at = now()
                        """,
                loginUser.userId(),
                theme.displayName(),
                theme.fontPreset(),
                theme.accentColor(),
                theme.titleColor(),
                theme.bodyColor(),
                theme.canvasType(),
                theme.canvasColor(),
                theme.canvasImage(),
                theme.paperColor(),
                theme.layoutStyle(),
                theme.blockStyle(),
                json(Map.of())
        );
        return ApiResponse.ok(blogTheme(loginUser.userId()));
    }

    @Transactional(rollbackFor = Exception.class)
    @PutMapping("/api/me/profile")
    public ApiResponse<UserPublicVO> updateMyProfile(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestBody UpdateProfileRequest request
    ) {
        jdbcTemplate.update(
                "update users set nickname = ?, avatar_url = ?, bio = ?, updated_at = now() where id = ?",
                request.nickname(),
                request.avatarUrl(),
                request.bio(),
                loginUser.userId()
        );
        return getMyProfile(loginUser);
    }

    @Transactional(rollbackFor = Exception.class)
    @PutMapping("/api/me/password")
    public ApiResponse<Void> updateMyPassword(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestBody UpdatePasswordRequest request,
            org.springframework.security.crypto.password.PasswordEncoder passwordEncoder
    ) {
        String currentEncoded = jdbcTemplate.queryForObject(
                "select password from users where id = ?",
                String.class,
                loginUser.userId()
        );
        if (currentEncoded == null || !passwordEncoder.matches(request.oldPassword(), currentEncoded)) {
            throw BusinessException.badRequest("原密码不正确");
        }
        jdbcTemplate.update(
                "update users set password = ?, updated_at = now() where id = ?",
                passwordEncoder.encode(request.newPassword()),
                loginUser.userId()
        );
        return ApiResponse.ok(null);
    }

    public record UpdateProfileRequest(
            String nickname,
            String avatarUrl,
            String bio
    ) {
    }

    public record UpdatePasswordRequest(
            String oldPassword,
            String newPassword
    ) {
    }

    public record BlogThemeRequest(
            String displayName,
            String fontPreset,
            String accentColor,
            String titleColor,
            String bodyColor,
            String canvasType,
            String canvasColor,
            String canvasImage,
            String paperColor,
            String layoutStyle,
            String blockStyle
    ) {
    }

    private BlogThemeVO sanitizeTheme(BlogThemeRequest request) {
        if (request == null) {
            throw BusinessException.badRequest("博客外观配置不能为空");
        }
        String canvasType = enumValue(request.canvasType(), CANVAS_TYPES, "soft-paper");
        String canvasImage = clean(request.canvasImage());
        if ("image".equals(canvasType)) {
            validateUploadPath(canvasImage, "画布图片只允许使用站内上传文件");
        } else {
            canvasImage = null;
        }
        return new BlogThemeVO(
                truncate(defaultString(request.displayName(), "我的主题"), 80),
                enumValue(request.fontPreset(), FONT_PRESETS, "literary-serif"),
                colorValue(request.accentColor(), "#2563eb"),
                colorValue(request.titleColor(), "#111827"),
                colorValue(request.bodyColor(), "#374151"),
                canvasType,
                colorValue(request.canvasColor(), "#f8fafc"),
                canvasImage,
                colorValue(request.paperColor(), "#ffffff"),
                enumValue(request.layoutStyle(), LAYOUT_STYLES, "editorial"),
                enumValue(request.blockStyle(), BLOCK_STYLES, "quiet")
        );
    }

    private BlogThemeVO blogTheme(Long userId) {
        var themes = jdbcTemplate.query("""
                        select display_name,
                               font_preset,
                               accent_color,
                               title_color,
                               body_color,
                               canvas_type,
                               canvas_color,
                               canvas_image,
                               paper_color,
                               layout_style,
                               block_style
                        from user_blog_themes
                        where user_id = ?
                        """,
                (rs, rowNum) -> new BlogThemeVO(
                        rs.getString("display_name"),
                        rs.getString("font_preset"),
                        rs.getString("accent_color"),
                        rs.getString("title_color"),
                        rs.getString("body_color"),
                        rs.getString("canvas_type"),
                        rs.getString("canvas_color"),
                        rs.getString("canvas_image"),
                        rs.getString("paper_color"),
                        rs.getString("layout_style"),
                        rs.getString("block_style")
                ),
                userId);
        return themes.isEmpty() ? defaultBlogTheme() : themes.getFirst();
    }

    private BlogThemeVO defaultBlogTheme() {
        return new BlogThemeVO(
                "我的主题",
                "literary-serif",
                "#2563eb",
                "#111827",
                "#374151",
                "soft-paper",
                "#f8fafc",
                null,
                "#ffffff",
                "editorial",
                "quiet"
        );
    }

    private String enumValue(String value, Set<String> allowed, String fallback) {
        String normalized = clean(value);
        return allowed.contains(normalized) ? normalized : fallback;
    }

    private String colorValue(String value, String fallback) {
        String color = clean(value);
        return HEX_COLOR.matcher(color).matches() ? color : fallback;
    }

    private String defaultString(String value, String fallback) {
        String clean = clean(value);
        return clean == null ? fallback : clean;
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String clean = value.trim();
        return clean.isEmpty() ? null : clean;
    }

    private String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }

    private void validateUploadPath(String value, String message) {
        if (value == null || !value.startsWith("/uploads/")) {
            throw BusinessException.badRequest(message);
        }
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (Exception ignored) {
            return "{}";
        }
    }
}
