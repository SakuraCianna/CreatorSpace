package com.creatorspace.common.result;

import java.util.List;

/**
 * 统一封装分页列表响应，避免各模块重复定义分页字段。
 */
public record PageResponse<T>(
        List<T> records,
        long page,
        long pageSize,
        long total
) {
}
