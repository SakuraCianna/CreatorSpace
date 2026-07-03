package com.creatorspace.module.email.service;

import com.creatorspace.common.exception.BusinessException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailServiceTest {

    @Test
    void sendVerificationCodeUsesBeautifiedHtmlContent() throws Exception {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        Environment environment = mock(Environment.class);
        MimeMessage message = new MimeMessage((Session) null);

        when(environment.getProperty("spring.mail.username", "noreply@qq.com")).thenReturn("sender@qq.com");
        when(mailSender.createMimeMessage()).thenReturn(message);

        EmailService emailService = new EmailService(mailSender, environment);
        emailService.sendVerificationCode("receiver@qq.com", "123456", "REGISTER");
        String html = emailService.buildVerificationCodeHtml("123456", "REGISTER");

        verify(mailSender).send(message);
        assertEquals("CreatorSpace 注册验证码", message.getSubject());
        assertEquals("sender@qq.com", message.getFrom()[0].toString());
        assertTrue(html.contains("CreatorSpace"));
        assertTrue(html.contains("123456"));
        assertTrue(html.contains("Verification Code"));
    }

    @Test
    void sendVerificationCodeWrapsMailSenderFailure() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        Environment environment = mock(Environment.class);
        MimeMessage message = new MimeMessage((Session) null);

        when(environment.getProperty("spring.mail.username", "noreply@qq.com")).thenReturn("sender@qq.com");
        when(mailSender.createMimeMessage()).thenReturn(message);
        doThrow(new IllegalStateException("smtp down")).when(mailSender).send(any(MimeMessage.class));

        EmailService emailService = new EmailService(mailSender, environment);
        BusinessException exception = assertThrows(BusinessException.class,
                () -> emailService.sendVerificationCode("receiver@qq.com", "123456", "REGISTER"));

        assertTrue(exception.getMessage().contains("smtp down"));
    }
}