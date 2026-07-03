package com.creatorspace.module.user.vo;

/**
 * 用户公开主页视图对象。
 */
public record UserPublicVO(
        Long id,
        String username,
        String nickname,
        String avatarUrl,
        String bio,
        BlogThemeVO blogTheme,
        long articleCount,
        long followerCount,
        long followingCount,
        long friendCount
) {
}
