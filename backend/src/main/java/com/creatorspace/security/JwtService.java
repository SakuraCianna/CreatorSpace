package com.creatorspace.security;

import com.creatorspace.common.exception.BusinessException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 负责生成和校验后台访问令牌。
 */
@Service
public class JwtService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String JWT_ALGORITHM = "HS256";
    private static final int MIN_SECRET_LENGTH = 32;
    private static final String LOCAL_FALLBACK_SECRET = "creator-space-local-development-secret-not-for-production";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Clock clock;
    private final String secret;
    private final long accessTokenExpireMinutes;

    // 启动时校验 JWT 密钥强度，本地环境允许占位值以减少配置阻塞。
    public JwtService(
            @Value("${app.security.jwt-secret}") String secret,
            @Value("${app.security.jwt-access-token-expire-minutes}") long accessTokenExpireMinutes,
            @Value("${app.env:local}") String appEnv
    ) {
        this.clock = Clock.systemUTC();
        this.secret = resolveSigningSecret(secret, appEnv);
        this.accessTokenExpireMinutes = accessTokenExpireMinutes;
    }

    // 根据登录用户生成访问令牌。
    public String createAccessToken(LoginUser loginUser) {
        Instant now = Instant.now(clock);
        Map<String, Object> header = Map.of(
                "alg", JWT_ALGORITHM,
                "typ", "JWT"
        );
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", loginUser.userId().toString());
        payload.put("username", loginUser.username());
        payload.put("roles", loginUser.roles());
        payload.put("iat", now.getEpochSecond());
        payload.put("exp", now.plusSeconds(accessTokenExpireMinutes * 60).getEpochSecond());

        String unsignedToken = base64UrlJson(header) + "." + base64UrlJson(payload);
        return unsignedToken + "." + sign(unsignedToken);
    }

    // 解析并校验访问令牌。
    public LoginUser parseAccessToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw BusinessException.unauthorized("无效的登录凭证");
            }
            String unsignedToken = parts[0] + "." + parts[1];
            if (!constantTimeEquals(sign(unsignedToken), parts[2])) {
                throw BusinessException.unauthorized("无效的登录凭证");
            }

            Map<String, Object> payload = objectMapper.readValue(
                    Base64.getUrlDecoder().decode(parts[1]),
                    new TypeReference<>() {
                    }
            );
            long expiresAt = ((Number) payload.get("exp")).longValue();
            if (Instant.now(clock).getEpochSecond() >= expiresAt) {
                throw BusinessException.unauthorized("登录状态已过期");
            }

            Long userId = Long.valueOf(payload.get("sub").toString());
            String username = payload.get("username").toString();
            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) payload.get("roles");
            return new LoginUser(userId, username, roles);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw BusinessException.unauthorized("无效的登录凭证");
        }
    }

    // 将对象序列化为 Base64URL JSON。
    private String base64UrlJson(Object value) {
        try {
            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (Exception exception) {
            throw new IllegalStateException("JWT JSON serialization failed", exception);
        }
    }

    // 对令牌内容进行 HMAC 签名。
    private String sign(String content) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("JWT signing failed", exception);
        }
    }

    // 使用常量时间比较字符串。
    private boolean constantTimeEquals(String left, String right) {
        return java.security.MessageDigest.isEqual(
                left.getBytes(StandardCharsets.UTF_8),
                right.getBytes(StandardCharsets.UTF_8)
        );
    }

    // 本地开发可使用兜底密钥，非本地环境仍强制要求显式强密钥。
    private String resolveSigningSecret(String configuredSecret, String appEnv) {
        String normalizedSecret = configuredSecret == null ? "" : configuredSecret.trim();
        if (normalizedSecret.length() >= MIN_SECRET_LENGTH) {
            return normalizedSecret;
        }
        if (isLocalLikeEnv(appEnv)) {
            return LOCAL_FALLBACK_SECRET;
        }
        throw new IllegalStateException("JWT_SECRET must be configured with at least 32 characters outside local development");
    }

    private boolean isLocalLikeEnv(String appEnv) {
        String normalizedEnv = appEnv == null ? "local" : appEnv.trim().toLowerCase();
        return "local".equals(normalizedEnv)
                || "dev".equals(normalizedEnv)
                || "development".equals(normalizedEnv)
                || "test".equals(normalizedEnv);
    }
}
