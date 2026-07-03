package com.creatorspace.module.email.service;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.security.SecureRandom;
import java.util.Properties;

class EmailServiceSmokeTest {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Test
    void sendsBeautifiedVerificationEmailWhenExplicitlyEnabled() {
        Assumptions.assumeTrue(Boolean.getBoolean("mail.smoke.enabled"));

        String username = requiredSetting("MAIL_USERNAME");
        String password = requiredSetting("MAIL_PASSWORD");
        String host = setting("MAIL_HOST", "smtp.qq.com");
        int port = Integer.parseInt(setting("MAIL_PORT", "587"));
        String target = System.getProperty("mail.smoke.to", "754515922@qq.com");

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);
        mailSender.setDefaultEncoding("UTF-8");

        Properties properties = mailSender.getJavaMailProperties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.starttls.required", "true");

        EmailService emailService = new EmailService(mailSender, mailEnvironment(username));
        emailService.sendVerificationCode(target, sixDigitCode(), "REGISTER");
    }

    private String sixDigitCode() {
        return String.valueOf(100000 + RANDOM.nextInt(900000));
    }

    private Environment mailEnvironment(String username) {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getSystemProperties().put("spring.mail.username", username);
        return environment;
    }

    private String requiredSetting(String name) {
        String value = setting(name, "");
        Assumptions.assumeTrue(!value.isBlank(), name + " is required for mail smoke test");
        return value;
    }

    private String setting(String name, String fallback) {
        String propertyValue = System.getProperty(name);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue;
        }
        String envValue = System.getenv(name);
        return envValue == null || envValue.isBlank() ? fallback : envValue;
    }
}
