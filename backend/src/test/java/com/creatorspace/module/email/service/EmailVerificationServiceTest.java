package com.creatorspace.module.email.service;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailVerificationServiceTest {

    @Test
    void sendCode_generatesSixDigitNumericCode() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        EmailService emailService = mock(EmailService.class);

        when(redisTemplate.hasKey("email:code:limit:REGISTER:receiver@qq.com")).thenReturn(false);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        EmailVerificationService verificationService = new EmailVerificationService(redisTemplate, emailService);
        verificationService.sendCode("receiver@qq.com", "REGISTER");

        var codeCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(emailService).sendVerificationCode(eq("receiver@qq.com"), codeCaptor.capture(), eq("REGISTER"));
        assertTrue(codeCaptor.getValue().matches("\\d{6}"));
        verify(valueOperations).set("email:code:limit:REGISTER:receiver@qq.com", "1", 60, TimeUnit.SECONDS);
        verify(valueOperations).set(
                eq("email:code:value:REGISTER:receiver@qq.com"),
                eq(codeCaptor.getValue()),
                eq(10L),
                eq(TimeUnit.MINUTES));
    }
}
