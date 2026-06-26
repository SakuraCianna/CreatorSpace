package com.creatorspace.module.audit;

import com.creatorspace.security.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminOperationAuditInterceptorTests {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void recordsSuccessfulAdminWriteRequest() {
        OperationLogService operationLogService = mock(OperationLogService.class);
        AdminOperationAuditInterceptor interceptor = new AdminOperationAuditInterceptor(operationLogService);
        HttpServletRequest request = request("PUT", "/api/admin/articles/42/publish");
        HttpServletResponse response = response(200);
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(
                new LoginUser(9L, "admin", List.of("ADMIN")), null));

        interceptor.afterCompletion(request, response, null, null);

        verify(operationLogService).record(
                eq(9L),
                eq("\u53d1\u5e03\u6587\u7ae0"),
                eq("ARTICLE"),
                eq("ARTICLE"),
                eq(42L),
                eq(request),
                any(Map.class)
        );
    }

    @Test
    void skipsAdminReadRequestAndFailedWriteRequest() {
        OperationLogService operationLogService = mock(OperationLogService.class);
        AdminOperationAuditInterceptor interceptor = new AdminOperationAuditInterceptor(operationLogService);

        interceptor.afterCompletion(request("GET", "/api/admin/articles"), response(200), null, null);
        interceptor.afterCompletion(request("PUT", "/api/admin/articles/42/publish"), response(400), null, null);

        verify(operationLogService, never()).record(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void resolvesThemeAndSiteOperations() {
        OperationLogService operationLogService = mock(OperationLogService.class);
        AdminOperationAuditInterceptor interceptor = new AdminOperationAuditInterceptor(operationLogService);
        HttpServletRequest themeRequest = request("PUT", "/api/admin/themes/3/switch");
        HttpServletRequest siteRequest = request("PUT", "/api/admin/site/settings");

        interceptor.afterCompletion(themeRequest, response(200), null, null);
        interceptor.afterCompletion(siteRequest, response(200), null, null);

        verify(operationLogService).record(any(), eq("\u5207\u6362\u4e3b\u9898"), eq("THEME"), eq("THEME"), eq(3L), eq(themeRequest), any(Map.class));
        verify(operationLogService).record(any(), eq("\u66f4\u65b0\u7ad9\u70b9\u8bbe\u7f6e"), eq("SITE"), eq("SITE"), eq(null), eq(siteRequest), any(Map.class));
    }

    private HttpServletRequest request(String method, String path) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn(method);
        when(request.getRequestURI()).thenReturn(path);
        return request;
    }

    private HttpServletResponse response(int status) {
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getStatus()).thenReturn(status);
        return response;
    }
}