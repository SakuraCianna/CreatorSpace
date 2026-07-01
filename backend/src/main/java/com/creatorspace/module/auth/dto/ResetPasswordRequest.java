package com.creatorspace.module.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 6, max = 6) String verificationCode,
        @NotBlank @Size(min = 6, max = 72) String newPassword
) {
}
