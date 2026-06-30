package com.creatorspace.module.audit;

import com.creatorspace.security.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AdminOperationAuditInterceptor implements HandlerInterceptor {

    private static final Pattern ID_IN_PATH = Pattern.compile("/(\\d+)(?:/|$)");

    private final OperationLogService operationLogService;

    public AdminOperationAuditInterceptor(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception exception) {
        if (exception != null || response.getStatus() >= 400 || !shouldRecord(request)) {
            return;
        }
        AuditTarget target = resolveTarget(request);
        operationLogService.record(currentUserId(), target.operation(), target.module(), target.targetType(), target.targetId(), request,
                Map.of("path", request.getRequestURI(), "method", request.getMethod()));
    }

    private boolean shouldRecord(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null || !path.startsWith("/api/admin/") || path.startsWith("/api/admin/auth/")) {
            return false;
        }
        return switch (request.getMethod().toUpperCase(Locale.ROOT)) {
            case "POST", "PUT", "PATCH", "DELETE" -> true;
            default -> false;
        };
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
            return loginUser.userId();
        }
        return null;
    }

    private AuditTarget resolveTarget(HttpServletRequest request) {
        String path = request.getRequestURI();
        Long targetId = extractId(path);
        if (path.contains("/articles")) {
            return new AuditTarget(articleOperation(request), "ARTICLE", "ARTICLE", targetId);
        }
        if (path.contains("/projects")) {
            return new AuditTarget(projectOperation(request), "PROJECT", "PROJECT", targetId);
        }
        if (path.contains("/comments")) {
            return new AuditTarget(commentOperation(path), "COMMENT", "COMMENT", targetId);
        }
        if (path.contains("/files/upload")) {
            return new AuditTarget("\u4e0a\u4f20\u6587\u4ef6", "FILE", "FILE", targetId);
        }
        if (path.contains("/themes")) {
            return new AuditTarget(path.endsWith("/switch") ? "\u5207\u6362\u4e3b\u9898" : "\u66f4\u65b0\u4e3b\u9898", "THEME", "THEME", targetId);
        }
        if (path.contains("/site/settings")) {
            return new AuditTarget("\u66f4\u65b0\u7ad9\u70b9\u8bbe\u7f6e", "SITE", "SITE", null);
        }
        if (path.contains("/inspirations")) {
            return new AuditTarget(genericOperation(request, "\u7075\u611f"), "INSPIRATION", "INSPIRATION", targetId);
        }
        if (path.contains("/guestbook")) {
            return new AuditTarget(genericOperation(request, "\u7559\u8a00"), "GUESTBOOK", "GUESTBOOK", targetId);
        }
        if (path.contains("/categories")) {
            return new AuditTarget(genericOperation(request, "\u5206\u7c7b"), "CATEGORY", "CATEGORY", targetId);
        }
        if (path.contains("/tags")) {
            return new AuditTarget(genericOperation(request, "\u6807\u7b7e"), "TAG", "TAG", targetId);
        }
        if (path.contains("/ai/suggestions") && path.endsWith("/adopt")) {
            return new AuditTarget("\u91c7\u7eb3AI\u5efa\u8bae", "AI", "AI_SUGGESTION", targetId);
        }
        if (path.contains("/ai/suggestions") && path.endsWith("/ignore")) {
            return new AuditTarget("\u5ffd\u7565AI\u5efa\u8bae", "AI", "AI_SUGGESTION", targetId);
        }
        if (path.contains("/ai/tasks")) {
            return new AuditTarget("\u521b\u5efaAI\u4efb\u52a1", "AI", "AI_TASK", targetId);
        }
        if (path.contains("/ai/workflows")) {
            return new AuditTarget("\u521b\u5efaAI\u5de5\u4f5c\u6d41", "AI", "AI_WORKFLOW", targetId);
        }
        return new AuditTarget(genericOperation(request, "\u540e\u53f0\u6570\u636e"), "ADMIN", "ADMIN", targetId);
    }

    private String articleOperation(HttpServletRequest request) {
        String path = request.getRequestURI();
        if ("POST".equalsIgnoreCase(request.getMethod())) {
            return "\u521b\u5efa\u6587\u7ae0";
        }
        if ("DELETE".equalsIgnoreCase(request.getMethod())) {
            return "\u5220\u9664\u6587\u7ae0";
        }
        if (path.endsWith("/publish")) {
            return "\u53d1\u5e03\u6587\u7ae0";
        }
        if (path.endsWith("/unpublish")) {
            return "\u64a4\u56de\u6587\u7ae0";
        }
        if (path.endsWith("/approve")) {
            return "\u5ba1\u6838\u901a\u8fc7\u6587\u7ae0";
        }
        if (path.endsWith("/reject")) {
            return "\u9a73\u56de\u6587\u7ae0";
        }
        if (path.endsWith("/top")) {
            return "\u5207\u6362\u6587\u7ae0\u7f6e\u9876";
        }
        if (path.endsWith("/recommend")) {
            return "\u5207\u6362\u6587\u7ae0\u63a8\u8350";
        }
        return "\u66f4\u65b0\u6587\u7ae0";
    }

    private String projectOperation(HttpServletRequest request) {
        String path = request.getRequestURI();
        if ("POST".equalsIgnoreCase(request.getMethod())) {
            return "\u521b\u5efa\u4f5c\u54c1";
        }
        if ("DELETE".equalsIgnoreCase(request.getMethod())) {
            return "\u5220\u9664\u4f5c\u54c1";
        }
        if (path.endsWith("/approve")) {
            return "\u5ba1\u6838\u901a\u8fc7\u4f5c\u54c1";
        }
        if (path.endsWith("/reject")) {
            return "\u9a73\u56de\u4f5c\u54c1";
        }
        if (path.endsWith("/recommend")) {
            return "\u5207\u6362\u4f5c\u54c1\u63a8\u8350";
        }
        if (path.endsWith("/status")) {
            return "\u66f4\u65b0\u4f5c\u54c1\u5c55\u793a\u72b6\u6001";
        }
        return "\u66f4\u65b0\u4f5c\u54c1";
    }

    private String commentOperation(String path) {
        if (path.endsWith("/approve")) {
            return "\u5ba1\u6838\u901a\u8fc7\u8bc4\u8bba";
        }
        if (path.endsWith("/reject")) {
            return "\u9a73\u56de\u8bc4\u8bba";
        }
        return "\u66f4\u65b0\u8bc4\u8bba";
    }

    private String genericOperation(HttpServletRequest request, String name) {
        return switch (request.getMethod().toUpperCase(Locale.ROOT)) {
            case "POST" -> "\u521b\u5efa" + name;
            case "DELETE" -> "\u5220\u9664" + name;
            default -> "\u66f4\u65b0" + name;
        };
    }

    private Long extractId(String path) {
        Matcher matcher = ID_IN_PATH.matcher(path == null ? "" : path);
        Long lastId = null;
        while (matcher.find()) {
            lastId = Long.valueOf(matcher.group(1));
        }
        return lastId;
    }

    private record AuditTarget(String operation, String module, String targetType, Long targetId) {
    }
}