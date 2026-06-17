package com.creatorspace.common.result;

/**
 * 统一封装后端接口返回结构，保持前后端响应格式稳定。
 */
public record ApiResponse<T>(boolean success, T data, String message) {

    // 返回成功响应。
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, "OK");
    }

    // 返回失败响应。
    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(false, null, message);
    }
}
