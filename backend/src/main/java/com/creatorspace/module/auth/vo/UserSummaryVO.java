package com.creatorspace.module.auth.vo;

import java.util.List;

/**
 * 返回给前端的安全用户摘要，不包含密码哈希等敏感字段。
 */
public record UserSummaryVO(Long id, String username, List<String> roles) {
}
