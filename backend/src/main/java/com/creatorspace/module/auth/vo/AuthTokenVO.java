package com.creatorspace.module.auth.vo;

/**
 * 登录成功后的访问令牌和刷新令牌响应。
 */
public record AuthTokenVO(
        String accessToken,
        String refreshToken,
        long expiresIn,
        UserSummaryVO user
) {
}
