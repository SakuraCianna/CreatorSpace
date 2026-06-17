package com.creatorspace.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 携带 HTTP 状态码的业务异常，用于统一错误响应。
 */
public class BusinessException extends RuntimeException {

    private final HttpStatus status;

    // 保存业务错误对应的 HTTP 状态码。
    public BusinessException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    // 获取 HTTP 状态码。
    public HttpStatus getStatus() {
        return status;
    }

    // 创建请求参数错误异常。
    public static BusinessException badRequest(String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, message);
    }

    // 创建未认证异常。
    public static BusinessException unauthorized(String message) {
        return new BusinessException(HttpStatus.UNAUTHORIZED, message);
    }

    // 创建无权限异常。
    public static BusinessException forbidden(String message) {
        return new BusinessException(HttpStatus.FORBIDDEN, message);
    }

    // 创建资源不存在异常。
    public static BusinessException notFound(String message) {
        return new BusinessException(HttpStatus.NOT_FOUND, message);
    }

    // 创建资源冲突异常。
    public static BusinessException conflict(String message) {
        return new BusinessException(HttpStatus.CONFLICT, message);
    }
}
