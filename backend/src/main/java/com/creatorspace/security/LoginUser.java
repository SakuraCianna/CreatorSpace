package com.creatorspace.security;

import java.util.List;

/**
 * 安全上下文中保存的已登录用户摘要。
 */
public record LoginUser(Long userId, String username, List<String> roles) {
}
