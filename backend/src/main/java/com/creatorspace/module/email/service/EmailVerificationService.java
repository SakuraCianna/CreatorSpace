package com.creatorspace.module.email.service;

import com.creatorspace.common.exception.BusinessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Service
public class EmailVerificationService {

    private static final int CODE_LENGTH = 6;
    private static final long CODE_EXPIRE_MINUTES = 10;
    private static final long RESEND_SECONDS = 60;

    private static final String REDIS_KEY_LIMIT_PREFIX = "email:code:limit:";
    private static final String REDIS_KEY_VALUE_PREFIX = "email:code:value:";

    private final StringRedisTemplate redisTemplate;
    private final EmailService emailService;
    private final SecureRandom random = new SecureRandom();

    public EmailVerificationService(StringRedisTemplate redisTemplate, EmailService emailService) {
        this.redisTemplate = redisTemplate;
        this.emailService = emailService;
    }

    public void sendCode(String email, String purpose) {
        if (!email.endsWith("@qq.com")) {
            throw BusinessException.badRequest("仅支持 QQ 邮箱注册");
        }

        String limitKey = REDIS_KEY_LIMIT_PREFIX + purpose + ":" + email;
        Boolean hasLimit = redisTemplate.hasKey(limitKey);
        if (Boolean.TRUE.equals(hasLimit)) {
            throw BusinessException.badRequest("请 60 秒后再试");
        }

        String code = generateCode();
        String valueKey = REDIS_KEY_VALUE_PREFIX + purpose + ":" + email;

        // Save rate limit flag
        redisTemplate.opsForValue().set(limitKey, "1", RESEND_SECONDS, TimeUnit.SECONDS);
        // Save verification code
        redisTemplate.opsForValue().set(valueKey, code, CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);

        emailService.sendVerificationCode(email, code, purpose);
    }

    public void verifyCode(String email, String code, String purpose) {
        String valueKey = REDIS_KEY_VALUE_PREFIX + purpose + ":" + email;
        String storedCode = redisTemplate.opsForValue().get(valueKey);

        if (storedCode == null || !storedCode.equals(code)) {
            throw BusinessException.badRequest("验证码无效或已过期");
        }

        // Delete code after successful verification to prevent reuse
        redisTemplate.delete(valueKey);
    }

    private String generateCode() {
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }
}
