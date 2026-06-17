package com.creatorspace.module.auth.vo;

/**
 * 管理员登录成功后的访问令牌响应。
 */
public record AuthTokenVO(String accessToken, UserSummaryVO user) {
}
