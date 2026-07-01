package com.creatorspace.module.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 游客注册请求参数。
 */
public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 64) String username,
        @NotBlank @Size(min = 6, max = 72) String password,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 6, max = 6) String verificationCode
) {
}
