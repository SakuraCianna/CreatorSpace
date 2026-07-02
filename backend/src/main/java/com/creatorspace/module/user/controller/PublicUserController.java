package com.creatorspace.module.user.controller;

import com.creatorspace.common.exception.BusinessException;
import com.creatorspace.common.result.ApiResponse;
import com.creatorspace.common.result.PageResponse;
import com.creatorspace.module.article.vo.ArticleVO;
import com.creatorspace.module.category.service.CategoryService;
import com.creatorspace.module.category.vo.CategoryVO;
import com.creatorspace.module.tag.service.TagService;
import com.creatorspace.module.tag.vo.TagVO;
import com.creatorspace.module.user.vo.UserPublicVO;
import com.creatorspace.security.LoginUser;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Validated
@RestController
public class PublicUserController {

    private final JdbcTemplate jdbcTemplate;
    private final CategoryService categoryService;
    private final TagService tagService;

    public PublicUserController(JdbcTemplate jdbcTemplate, CategoryService categoryService, TagService tagService) {
        this.jdbcTemplate = jdbcTemplate;
        this.categoryService = categoryService;
        this.tagService = tagService;
    }

    @GetMapping("/api/users/{userId}")
    public ApiResponse<UserPublicVO> getProfile(
            @PathVariable Long userId,
            Authentication authentication
    ) {
        Long currentUserId = null;
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
            currentUserId = loginUser.userId();
        }
        boolean isOwner = currentUserId != null && currentUserId.equals(userId);
        boolean isFriend = !isOwner && currentUserId != null && isMutualFollow(currentUserId, userId);
        String articleCountSql;
        if (isOwner) {
            articleCountSql = "(select count(*) from articles a where a.created_by = u.id and a.status in ('PUBLISHED', 'PRIVATE')) as article_count";
        } else if (isFriend) {
            articleCountSql = "(select count(*) from articles a where a.created_by = u.id and a.status = 'PUBLISHED' and a.privacy_type in ('PUBLIC', 'FRIENDS')) as article_count";
        } else {
            articleCountSql = "(select count(*) from articles a where a.created_by = u.id and a.status = 'PUBLISHED' and a.privacy_type = 'PUBLIC') as article_count";
        }
        List<UserPublicVO> profiles = jdbcTemplate.query("""
                        select u.id, u.username, u.nickname, u.avatar_url, u.bio,
                               %s,
                               (select count(*) from user_follows f where f.followee_id = u.id) as follower_count,
                               (select count(*) from user_follows f where f.follower_id = u.id) as following_count,
                               (select count(*) from (
                                   select f1.follower_id from user_follows f1 where f1.followee_id = u.id
                                   intersect
                                   select f2.followee_id from user_follows f2 where f2.follower_id = u.id
                               ) mutual) as friend_count
                        from users u
                        where u.id = ?
                        """.formatted(articleCountSql),
                (rs, rowNum) -> toPublicVO(rs),
                userId);
        if (profiles.isEmpty()) {
            throw BusinessException.notFound("用户不存在");
        }
        return ApiResponse.ok(profiles.getFirst());
    }

    @GetMapping("/api/users/{userId}/articles")
    public ApiResponse<PageResponse<ArticleVO>> listArticles(
            @PathVariable Long userId,
            Authentication authentication,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize
    ) {
        Long currentUserId = null;
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
            currentUserId = loginUser.userId();
        }
        boolean isOwner = currentUserId != null && currentUserId.equals(userId);
        boolean isFriend = !isOwner && currentUserId != null && isMutualFollow(currentUserId, userId);
        long offset = (page - 1) * pageSize;
        String countSql;
        String listSql;
        Object[] countParams;
        Object[] listParams;
        if (isOwner) {
            countSql = "select count(*) from articles where created_by = ? and status in ('PUBLISHED', 'PRIVATE')";
            countParams = new Object[]{userId};
            listSql = """
                    select a.* from articles a
                    where a.created_by = ? and a.status in ('PUBLISHED', 'PRIVATE')
                    order by a.is_top desc, a.publish_time desc, a.id desc
                    limit ? offset ?
                    """;
            listParams = new Object[]{userId, pageSize, offset};
        } else if (isFriend) {
            countSql = "select count(*) from articles where created_by = ? and status = 'PUBLISHED' and privacy_type in ('PUBLIC', 'FRIENDS')";
            countParams = new Object[]{userId};
            listSql = """
                    select a.* from articles a
                    where a.created_by = ? and a.status = 'PUBLISHED' and a.privacy_type in ('PUBLIC', 'FRIENDS')
                    order by a.is_top desc, a.publish_time desc, a.id desc
                    limit ? offset ?
                    """;
            listParams = new Object[]{userId, pageSize, offset};
        } else {
            countSql = "select count(*) from articles where created_by = ? and status = 'PUBLISHED' and privacy_type = 'PUBLIC'";
            countParams = new Object[]{userId};
            listSql = """
                    select a.* from articles a
                    where a.created_by = ? and a.status = 'PUBLISHED' and a.privacy_type = 'PUBLIC'
                    order by a.is_top desc, a.publish_time desc, a.id desc
                    limit ? offset ?
                    """;
            listParams = new Object[]{userId, pageSize, offset};
        }
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, countParams);
        List<ArticleVO> records = jdbcTemplate.query(listSql, (rs, rowNum) -> toArticleVO(rs), listParams);
        return ApiResponse.ok(new PageResponse<>(records, page, pageSize, total == null ? 0 : total));
    }

    @GetMapping("/api/users/{userId}/followers")
    public ApiResponse<PageResponse<FollowUserVO>> listFollowers(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize
    ) {
        long offset = (page - 1) * pageSize;
        Long total = jdbcTemplate.queryForObject(
                "select count(*) from user_follows where followee_id = ?",
                Long.class, userId);
        List<FollowUserVO> records = jdbcTemplate.query("""
                        select u.id, u.username, u.nickname, u.avatar_url, u.bio
                        from user_follows f
                        join users u on u.id = f.follower_id
                        where f.followee_id = ?
                        order by f.created_at desc
                        limit ? offset ?
                        """,
                (rs, rowNum) -> toFollowUserVO(rs),
                userId, pageSize, offset);
        return ApiResponse.ok(new PageResponse<>(records, page, pageSize, total == null ? 0 : total));
    }

    @GetMapping("/api/users/{userId}/following")
    public ApiResponse<PageResponse<FollowUserVO>> listFollowing(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize
    ) {
        long offset = (page - 1) * pageSize;
        Long total = jdbcTemplate.queryForObject(
                "select count(*) from user_follows where follower_id = ?",
                Long.class, userId);
        List<FollowUserVO> records = jdbcTemplate.query("""
                        select u.id, u.username, u.nickname, u.avatar_url, u.bio
                        from user_follows f
                        join users u on u.id = f.followee_id
                        where f.follower_id = ?
                        order by f.created_at desc
                        limit ? offset ?
                        """,
                (rs, rowNum) -> toFollowUserVO(rs),
                userId, pageSize, offset);
        return ApiResponse.ok(new PageResponse<>(records, page, pageSize, total == null ? 0 : total));
    }

    @GetMapping("/api/users/{userId}/friends")
    public ApiResponse<PageResponse<FollowUserVO>> listFriends(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize
    ) {
        long offset = (page - 1) * pageSize;
        Long total = jdbcTemplate.queryForObject("""
                        select count(*) from (
                            select f1.follower_id from user_follows f1 where f1.followee_id = ?
                            intersect
                            select f2.followee_id from user_follows f2 where f2.follower_id = ?
                        ) mutual
                        """,
                Long.class, userId, userId);
        List<FollowUserVO> records = jdbcTemplate.query("""
                        select u.id, u.username, u.nickname, u.avatar_url, u.bio
                        from (
                            select f1.follower_id as uid from user_follows f1 where f1.followee_id = ?
                            intersect
                            select f2.followee_id from user_follows f2 where f2.follower_id = ?
                        ) mutual
                        join users u on u.id = mutual.uid
                        order by u.id
                        limit ? offset ?
                        """,
                (rs, rowNum) -> toFollowUserVO(rs),
                userId, userId, pageSize, offset);
        return ApiResponse.ok(new PageResponse<>(records, page, pageSize, total == null ? 0 : total));
    }

    private boolean isMutualFollow(Long userId, Long otherUserId) {
        if (userId == null || otherUserId == null) return false;
        Long count = jdbcTemplate.queryForObject("""
                        select count(*) from user_follows f1
                        join user_follows f2 on f1.follower_id = f2.followee_id and f1.followee_id = f2.follower_id
                        where f1.follower_id = ? and f1.followee_id = ?
                        """,
                Long.class, userId, otherUserId);
        return count != null && count > 0;
    }

    private UserPublicVO toPublicVO(ResultSet rs) throws SQLException {
        Long id = rs.getObject("id", Long.class);
        if (id == null) return null;
        return new UserPublicVO(
                id,
                rs.getString("username"),
                rs.getString("nickname"),
                rs.getString("avatar_url"),
                rs.getString("bio"),
                rs.getLong("article_count"),
                rs.getLong("follower_count"),
                rs.getLong("following_count"),
                rs.getLong("friend_count")
        );
    }

    private FollowUserVO toFollowUserVO(ResultSet rs) throws SQLException {
        return new FollowUserVO(
                rs.getObject("id", Long.class),
                rs.getString("username"),
                rs.getString("nickname"),
                rs.getString("avatar_url"),
                rs.getString("bio")
        );
    }

    private ArticleVO toArticleVO(ResultSet rs) throws SQLException {
        long articleId = rs.getLong("id");
        Long authorId = rs.getObject("created_by", Long.class);
        String authorName = null;
        String authorAvatar = null;
        String authorBio = null;
        if (authorId != null) {
            List<AuthorInfo> authors = jdbcTemplate.query(
                    "select username, avatar_url, bio from users where id = ?",
                    (r, n) -> new AuthorInfo(r.getString("username"), r.getString("avatar_url"), r.getString("bio")),
                    authorId);
            if (!authors.isEmpty()) {
                authorName = authors.getFirst().username();
                authorAvatar = authors.getFirst().avatarUrl();
                authorBio = authors.getFirst().bio();
            }
        }
        CategoryVO category = rs.getObject("category_id", Long.class) == null
                ? null : categoryService.findById(rs.getObject("category_id", Long.class));
        List<Long> tagIds = jdbcTemplate.query(
                "select tag_id from article_tags where article_id = ?",
                (r, n) -> r.getLong("tag_id"),
                articleId);
        List<TagVO> tags = tagIds.isEmpty() ? Collections.emptyList() : tagService.listByIds(tagIds);
        return new ArticleVO(
                articleId,
                rs.getString("title"),
                rs.getString("slug"),
                rs.getString("summary"),
                null,
                rs.getString("cover_url"),
                rs.getString("status"),
                rs.getString("privacy_type"),
                rs.getObject("view_count", Long.class),
                rs.getObject("like_count", Long.class),
                rs.getObject("comment_count", Long.class),
                rs.getObject("is_top", Boolean.class),
                rs.getObject("is_recommend", Boolean.class),
                category,
                tags,
                rs.getObject("publish_time", OffsetDateTime.class),
                authorId,
                authorName,
                authorAvatar,
                authorBio,
                rs.getObject("submitted_at", OffsetDateTime.class),
                rs.getObject("reviewed_at", OffsetDateTime.class),
                rs.getString("review_note")
        );
    }

    private record AuthorInfo(String username, String avatarUrl, String bio) {
    }

    public record FollowUserVO(
            Long id,
            String username,
            String nickname,
            String avatarUrl,
            String bio
    ) {
    }
}
