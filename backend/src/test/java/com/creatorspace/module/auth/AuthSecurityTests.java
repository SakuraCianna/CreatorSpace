package com.creatorspace.module.auth;

import com.creatorspace.config.SecurityConfig;
import com.creatorspace.module.audit.OperationLogService;
import com.creatorspace.module.auth.controller.AuthController;
import com.creatorspace.module.auth.service.AuthService;
import com.creatorspace.security.JwtAuthenticationFilter;
import com.creatorspace.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AuthSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private OperationLogService operationLogService;

    @Test
    void sendRegisterCodeDoesNotRequireLogin() throws Exception {
        mockMvc.perform(post("/api/auth/send-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "register-user@qq.com",
                                  "hcaptchaToken": "captcha-token"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        verify(authService).sendVerificationCode("register-user@qq.com", "captcha-token", "REGISTER");
    }
}
