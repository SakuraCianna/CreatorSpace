package com.creatorspace.common;

import com.creatorspace.common.exception.BusinessException;
import com.creatorspace.common.result.ApiResponse;
import com.creatorspace.common.result.PageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CommonBasicsTest {

    @Test
    void apiResponseWrapsSuccessAndFailure() {
        ApiResponse<String> ok = ApiResponse.ok("data");
        ApiResponse<String> fail = ApiResponse.fail("bad");

        assertTrue(ok.success());
        assertEquals("data", ok.data());
        assertEquals("OK", ok.message());
        assertFalse(fail.success());
        assertNull(fail.data());
        assertEquals("bad", fail.message());
    }

    @Test
    void pageResponseCarriesPaginationFields() {
        PageResponse<String> page = new PageResponse<>(List.of("a", "b"), 2, 10, 25);

        assertEquals(List.of("a", "b"), page.records());
        assertEquals(2, page.page());
        assertEquals(10, page.pageSize());
        assertEquals(25, page.total());
    }

    @Test
    void businessExceptionFactoriesCarryHttpStatus() {
        assertException(BusinessException.badRequest("bad"), HttpStatus.BAD_REQUEST, "bad");
        assertException(BusinessException.unauthorized("login"), HttpStatus.UNAUTHORIZED, "login");
        assertException(BusinessException.forbidden("deny"), HttpStatus.FORBIDDEN, "deny");
        assertException(BusinessException.notFound("missing"), HttpStatus.NOT_FOUND, "missing");
        assertException(BusinessException.conflict("conflict"), HttpStatus.CONFLICT, "conflict");
        assertException(BusinessException.tooManyRequests("slow"), HttpStatus.TOO_MANY_REQUESTS, "slow");
    }

    private void assertException(BusinessException exception, HttpStatus status, String message) {
        assertEquals(status, exception.getStatus());
        assertEquals(message, exception.getMessage());
    }
}
