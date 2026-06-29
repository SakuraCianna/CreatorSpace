package com.creatorspace.module.statistics;

import com.creatorspace.common.cache.CacheKeys;
import com.creatorspace.common.cache.RedisJsonCacheService;
import com.creatorspace.security.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

@Service
public class VisitLogService {

    private static final Duration VISIT_DEDUPE_TTL = Duration.ofMinutes(5);

    private final JdbcTemplate jdbcTemplate;
    private final RedisJsonCacheService redisCacheService;

    public VisitLogService(JdbcTemplate jdbcTemplate, RedisJsonCacheService redisCacheService) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisCacheService = redisCacheService;
    }

    public boolean recordContentVisit(String targetType, Long targetId, HttpServletRequest request) {
        String ipAddress = request.getRemoteAddr();
        Long userId = currentUserId();
        boolean repeatedRecently = hasRecentVisit(targetType, targetId, ipAddress);
        try {
            String userAgent = request.getHeader("User-Agent");
            jdbcTemplate.update("""
                            insert into visit_logs (
                                path, target_type, target_id, user_id, ip_address, user_agent, referer,
                                device_type, browser, operating_system
                            )
                            values (?, ?, ?, ?, cast(? as inet), ?, ?, ?, ?, ?)
                            """,
                    request.getRequestURI(),
                    targetType,
                    targetId,
                    userId,
                    ipAddress,
                    userAgent,
                    request.getHeader("Referer"),
                    deviceType(userAgent),
                    browser(userAgent),
                    operatingSystem(userAgent));
        } catch (Exception ignored) {
            // Visit analytics must not block public content reads.
        }
        return !repeatedRecently;
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser loginUser)) {
            return null;
        }
        return loginUser.userId();
    }

    private boolean hasRecentVisit(String targetType, Long targetId, String ipAddress) {
        String dedupeKey = visitDedupeKey(targetType, targetId, ipAddress);
        try {
            return !redisCacheService.setIfAbsent(dedupeKey, VISIT_DEDUPE_TTL);
        } catch (RuntimeException ignored) {
            // Redis is an optimization. Fall back to database dedupe when it is unavailable.
        }
        try {
            Long count = jdbcTemplate.queryForObject("""
                            select count(*)
                            from visit_logs
                            where target_type = ?
                              and target_id = ?
                              and ip_address = cast(? as inet)
                              and created_at >= now() - interval '5 minutes'
                            """,
                    Long.class,
                    targetType,
                    targetId,
                    ipAddress);
            return count != null && count > 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    private String visitDedupeKey(String targetType, Long targetId, String ipAddress) {
        return CacheKeys.VISIT_DEDUPE_PREFIX + sha256Hex(targetType + ":" + targetId + ":" + normalize(ipAddress));
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private String deviceType(String userAgent) {
        String normalized = normalize(userAgent);
        if (normalized.contains("mobile") || normalized.contains("android") || normalized.contains("iphone")) {
            return "MOBILE";
        }
        if (normalized.contains("ipad") || normalized.contains("tablet")) {
            return "TABLET";
        }
        return "DESKTOP";
    }

    private String browser(String userAgent) {
        String normalized = normalize(userAgent);
        if (normalized.contains("edg/")) {
            return "Edge";
        }
        if (normalized.contains("chrome/")) {
            return "Chrome";
        }
        if (normalized.contains("firefox/")) {
            return "Firefox";
        }
        if (normalized.contains("safari/")) {
            return "Safari";
        }
        return "Unknown";
    }

    private String operatingSystem(String userAgent) {
        String normalized = normalize(userAgent);
        if (normalized.contains("windows")) {
            return "Windows";
        }
        if (normalized.contains("android")) {
            return "Android";
        }
        if (normalized.contains("iphone") || normalized.contains("ipad") || normalized.contains("ios")) {
            return "iOS";
        }
        if (normalized.contains("mac os") || normalized.contains("macintosh")) {
            return "macOS";
        }
        if (normalized.contains("linux")) {
            return "Linux";
        }
        return "Unknown";
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase();
    }
}
