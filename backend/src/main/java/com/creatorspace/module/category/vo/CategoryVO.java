package com.creatorspace.module.category.vo;

public record CategoryVO(
        Long id,
        String module,
        String name,
        String slug,
        String description,
        Integer sortOrder,
        Boolean enabled
) {
}
