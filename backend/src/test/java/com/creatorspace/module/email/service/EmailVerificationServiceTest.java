package com.creatorspace.module.email.service;

import com.creatorspace.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EmailVerificationServiceTest {

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
    private final EmailService emailService = mock(EmailService.class);
    private EmailVerificationService service;

    @BeforeEach
    void setUp() {
        service = new EmailVerificationService(redisTemplate, emailService);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void sendCodeStoresRateLimitAndVerificationCode() {
        when(redisTemplate.hasKey("email:code:limit:REGISTER:user@qq.com")).thenReturn(false);

        service.sendCode("user@qq.com", "REGISTER");

        verify(valueOperations).set("email:code:limit:REGISTER:user@qq.com", "1", 60L, TimeUnit.SECONDS);
        verify(valueOperations).set(eq("email:code:value:REGISTER:user@qq.com"), argThat(code -> code.matches("\\d{6}")), eq(10L), eq(TimeUnit.MINUTES));
        verify(emailService).sendVerificationCode(eq("user@qq.com"), argThat(code -> code.matches("\\d{6}")), eq("REGISTER"));
    }

    @Test
    void sendCodeRejectsNonQqEmailAndRateLimitedEmail() {
        assertThrows(BusinessException.class, () -> service.sendCode("user@example.com", "REGISTER"));

        when(redisTemplate.hasKey("email:code:limit:REGISTER:user@qq.com")).thenReturn(true);
        BusinessException exception = assertThrows(BusinessException.class, () -> service.sendCode("user@qq.com", "REGISTER"));

        assertEquals("请 60 秒后再试", exception.getMessage());
        verify(emailService, never()).sendVerificationCode(anyString(), anyString(), anyString());
    }

    @Test
    void verifyCodeDeletesCodeAfterSuccessAndRejectsMismatch() {
        when(valueOperations.get("email:code:value:REGISTER:user@qq.com")).thenReturn("123456");

        service.verifyCode("user@qq.com", "123456", "REGISTER");

        verify(redisTemplate).delete("email:code:value:REGISTER:user@qq.com");

        when(valueOperations.get("email:code:value:REGISTER:user@qq.com")).thenReturn("654321");
        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.verifyCode("user@qq.com", "123456", "REGISTER"));
        assertEquals("验证码无效或已过期", exception.getMessage());
    }
}
