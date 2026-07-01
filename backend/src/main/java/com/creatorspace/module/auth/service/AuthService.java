package com.creatorspace.module.auth.service;

import com.creatorspace.module.auth.dto.LoginRequest;
import com.creatorspace.module.auth.dto.RegisterRequest;
import com.creatorspace.module.auth.dto.ResetPasswordRequest;
import com.creatorspace.module.auth.vo.AuthTokenVO;
import com.creatorspace.module.auth.vo.UserSummaryVO;

public interface AuthService {

    UserSummaryVO register(RegisterRequest request);

    AuthTokenVO login(LoginRequest request);

    AuthTokenVO loginAdmin(LoginRequest request);

    AuthTokenVO refresh(String refreshToken);

    void logout(String refreshToken);

    void sendVerificationCode(String email, String purpose);

    void resetPassword(ResetPasswordRequest request);
}
