package com.creatorspace.module.email.service;

import com.creatorspace.common.exception.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;

@Service
public class EmailVerificationService {

    private static final int CODE_LENGTH = 6;
    private static final long CODE_EXPIRE_MINUTES = 10;
    private static final long RESEND_SECONDS = 60;

    private final JdbcTemplate jdbcTemplate;
    private final EmailService emailService;
    private final SecureRandom random = new SecureRandom();

    public EmailVerificationService(JdbcTemplate jdbcTemplate, EmailService emailService) {
        this.jdbcTemplate = jdbcTemplate;
        this.emailService = emailService;
    }

    public void sendCode(String email, String purpose) {
        if (!email.endsWith("@qq.com")) {
            throw BusinessException.badRequest("仅支持 QQ 邮箱注册");
        }
        OffsetDateTime recent = jdbcTemplate.queryForObject("""
                        select created_at from email_verification_codes
                        where email = ? and purpose = ? and used_at is null and expires_at > now()
                        order by created_at desc limit 1
                        """,
                OffsetDateTime.class,
                email, purpose);
        if (recent != null && recent.plusSeconds(RESEND_SECONDS).isAfter(OffsetDateTime.now())) {
            throw BusinessException.badRequest("请 60 秒后再试");
        }
        String code = generateCode();
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime expiresAt = now.plusMinutes(CODE_EXPIRE_MINUTES);
        jdbcTemplate.update("""
                        insert into email_verification_codes (email, code, purpose, created_at, expires_at)
                        values (?, ?, ?, ?, ?)
                        """,
                email, code, purpose, now, expiresAt);
        emailService.sendVerificationCode(email, code, purpose);
    }

    public void verifyCode(String email, String code, String purpose) {
        Integer count = jdbcTemplate.queryForObject("""
                        select count(*) from email_verification_codes
                        where email = ? and code = ? and purpose = ? and used_at is null and expires_at > now()
                        """,
                Integer.class, email, code, purpose);
        if (count == null || count == 0) {
            throw BusinessException.badRequest("验证码无效或已过期");
        }
        jdbcTemplate.update("""
                        update email_verification_codes set used_at = now()
                        where email = ? and code = ? and purpose = ? and used_at is null
                        """,
                email, code, purpose);
    }

    private String generateCode() {
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }
}
