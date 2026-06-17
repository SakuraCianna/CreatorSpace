package com.creatorspace.module.category.vo;

/**
 * 返回给前端的分类视图对象。
 */
public record CategoryVO(Long id, String module, String name, String slug, String description, Integer sortOrder) {
}
