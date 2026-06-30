package com.creatorspace.module.user.controller;

import com.creatorspace.common.exception.BusinessException;
import com.creatorspace.common.result.ApiResponse;
import com.creatorspace.security.LoginUser;
import com.creatorspace.module.user.vo.UserPublicVO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
public class MeController {

    private final JdbcTemplate jdbcTemplate;

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

    public record UpdateProfileRequest(
            String nickname,
            String avatarUrl,
            String bio
    ) {
    }
}
