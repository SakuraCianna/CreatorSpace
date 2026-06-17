package com.creatorspace.module.auth.service;

import com.creatorspace.module.auth.dto.LoginRequest;
import com.creatorspace.module.auth.dto.RegisterRequest;
import com.creatorspace.module.auth.vo.AuthTokenVO;
import com.creatorspace.module.auth.vo.UserSummaryVO;

/**
 * 认证和注册业务接口。
 */
public interface AuthService {

    // 注册普通用户并返回安全用户信息。
    UserSummaryVO register(RegisterRequest request);

    // 校验管理员身份并签发访问令牌。
    AuthTokenVO loginAdmin(LoginRequest request);
}
