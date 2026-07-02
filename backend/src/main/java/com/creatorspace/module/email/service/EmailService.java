package com.creatorspace.module.email.service;

import com.creatorspace.common.exception.BusinessException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String from;

    public EmailService(JavaMailSender mailSender,
                        org.springframework.core.env.Environment env) {
        this.mailSender = mailSender;
        this.from = env.getProperty("spring.mail.username", "noreply@qq.com");
    }

    public void sendVerificationCode(String to, String code, String purpose) {
        String subject = switch (purpose) {
            case "REGISTER" -> "CreatorSpace 注册验证码";
            case "RESET_PASSWORD" -> "CreatorSpace 重置密码验证码";
            default -> "CreatorSpace 验证码";
        };
        String text = """
                您好！
                
                您的验证码为：%s
                
                验证码有效期为 10 分钟，请勿泄露给他人。
                如果不是您本人操作，请忽略此邮件。
                
                CreatorSpace 团队
                """.formatted(code);
        send(to, subject, text);
    }

    private void send(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
        } catch (Exception e) {
            throw BusinessException.badRequest("邮件发送失败: " + e.getMessage());
        }
    }
}
