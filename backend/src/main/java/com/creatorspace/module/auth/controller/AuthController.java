package com.creatorspace.module.auth.controller;

import com.creatorspace.common.result.ApiResponse;
import com.creatorspace.module.auth.dto.EmailRequest;
import com.creatorspace.module.auth.dto.LoginRequest;
import com.creatorspace.module.auth.dto.RefreshRequest;
import com.creatorspace.module.auth.dto.RegisterRequest;
import com.creatorspace.module.auth.dto.ResetPasswordRequest;
import com.creatorspace.module.auth.service.AuthService;
import com.creatorspace.module.auth.vo.AuthTokenVO;
import com.creatorspace.module.auth.vo.UserSummaryVO;
import jakarta.validation.Valid;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 处理用户注册、用户登录和后台管理员登录接口。
 */
@RestController
public class AuthController {

    private final AuthService authService;
    private final Environment environment;

    public AuthController(AuthService authService, Environment environment) {
        this.authService = authService;
        this.environment = environment;
    }

    @GetMapping("/api/auth/diagnose-mail")
    public ApiResponse<Map<String, Object>> diagnoseMail() {
        String host = environment.getProperty("spring.mail.host", "NOT FOUND");
        String port = environment.getProperty("spring.mail.port", "NOT FOUND");
        String username = environment.getProperty("spring.mail.username", "NOT FOUND");
        String password = environment.getProperty("spring.mail.password", "");
        boolean hasPassword = password != null && !password.isEmpty() && !password.equals("NOT FOUND");
        return ApiResponse.ok(Map.of(
            "host", host,
            "port", port,
            "username", username,
            "passwordConfigured", hasPassword,
            "passwordLength", hasPassword ? password.length() : 0
        ));
    }

    @PostMapping("/api/auth/register")
    public ApiResponse<UserSummaryVO> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(authService.register(request));
    }

    @PostMapping("/api/auth/login")
    public ApiResponse<AuthTokenVO> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @PostMapping("/api/admin/auth/login")
    public ApiResponse<AuthTokenVO> loginAdmin(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.loginAdmin(request));
    }

    @PostMapping("/api/auth/refresh")
    public ApiResponse<AuthTokenVO> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.ok(authService.refresh(request.refreshToken()));
    }

    @PostMapping("/api/auth/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
        return ApiResponse.ok(null);
    }

    @PostMapping("/api/auth/send-code")
    public ApiResponse<Void> sendCode(@Valid @RequestBody EmailRequest request) {
        authService.sendVerificationCode(request.email(), request.hcaptchaToken(), "REGISTER");
        return ApiResponse.ok(null);
    }

    @PostMapping("/api/auth/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody EmailRequest request) {
        authService.sendVerificationCode(request.email(), request.hcaptchaToken(), "RESET_PASSWORD");
        return ApiResponse.ok(null);
    }

    @PostMapping("/api/auth/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.ok(null);
    }
}
