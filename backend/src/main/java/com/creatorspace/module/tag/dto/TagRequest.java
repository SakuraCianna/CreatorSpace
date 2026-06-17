package com.creatorspace.module.tag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 后台创建标签的请求参数。
 */
public record TagRequest(
        @NotBlank @Size(max = 80) String name,
        @NotBlank @Size(max = 120) String slug,
        @Size(max = 32) String color,
        Integer weight
) {
}
