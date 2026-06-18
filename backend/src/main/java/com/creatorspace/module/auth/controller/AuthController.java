package com.creatorspace.module.auth.controller;

import com.creatorspace.common.result.ApiResponse;
import com.creatorspace.module.auth.dto.LoginRequest;
import com.creatorspace.module.auth.dto.RegisterRequest;
import com.creatorspace.module.auth.service.AuthService;
import com.creatorspace.module.auth.vo.AuthTokenVO;
import com.creatorspace.module.auth.vo.UserSummaryVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 处理用户注册、用户登录和后台管理员登录接口。
 */
@RestController
public class AuthController {

    private final AuthService authService;

    // 通过构造器注入认证服务，Controller 只负责参数和响应封装。
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // 注册普通用户并返回安全用户信息。
    @PostMapping("/api/auth/register")
    public ApiResponse<UserSummaryVO> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(authService.register(request));
    }

    // 校验普通用户身份并签发访问令牌。
    @PostMapping("/api/auth/login")
    public ApiResponse<AuthTokenVO> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    // 校验管理员身份并签发访问令牌。
    @PostMapping("/api/admin/auth/login")
    public ApiResponse<AuthTokenVO> loginAdmin(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.loginAdmin(request));
    }
}
