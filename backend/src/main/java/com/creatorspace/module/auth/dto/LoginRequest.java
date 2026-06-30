package com.creatorspace.module.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 管理员登录请求参数。
 */
public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password,
        String hcaptchaToken
) {
}
