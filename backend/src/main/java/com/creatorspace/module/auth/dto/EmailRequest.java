package com.creatorspace.module.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailRequest(
        @NotBlank @Email String email,
        String hcaptchaToken
) {
}
