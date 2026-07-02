package com.creatorspace.module.email.service;

import com.creatorspace.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmailServiceTest {

    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final Environment environment = mock(Environment.class);

    @Test
    void sendVerificationCodeUsesConfiguredSenderAndPurposeSubject() {
        when(environment.getProperty("spring.mail.username", "noreply@qq.com")).thenReturn("sender@qq.com");
        EmailService service = new EmailService(mailSender, environment);

        service.sendVerificationCode("user@qq.com", "123456", "REGISTER");
        service.sendVerificationCode("user@qq.com", "654321", "RESET_PASSWORD");
        service.sendVerificationCode("user@qq.com", "111111", "LOGIN");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(3)).send(captor.capture());
        assertEquals("sender@qq.com", captor.getAllValues().getFirst().getFrom());
        assertArrayEquals(new String[]{"user@qq.com"}, captor.getAllValues().getFirst().getTo());
        assertTrue(captor.getAllValues().get(0).getText().contains("123456"));
        assertNotEquals(captor.getAllValues().get(0).getSubject(), captor.getAllValues().get(1).getSubject());
        assertNotNull(captor.getAllValues().get(2).getSubject());
    }

    @Test
    void sendVerificationCodeWrapsMailSenderFailure() {
        when(environment.getProperty("spring.mail.username", "noreply@qq.com")).thenReturn("sender@qq.com");
        doThrow(new IllegalStateException("smtp down")).when(mailSender).send(any(SimpleMailMessage.class));
        EmailService service = new EmailService(mailSender, environment);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.sendVerificationCode("user@qq.com", "123456", "REGISTER"));

        assertTrue(exception.getMessage().contains("smtp down"));
    }
}
